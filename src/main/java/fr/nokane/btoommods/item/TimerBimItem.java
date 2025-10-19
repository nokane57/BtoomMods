package fr.nokane.btoommods.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.item.TimerBimProjectileEntity;
import fr.nokane.btoommods.net.Net;
import fr.nokane.btoommods.net.TimerItemSyncS2C;
import net.minecraft.entity.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.List;

/**
 * ⏱️ Timer BIM corrigé :
 * - Explosion consommée dans l’inventaire
 * - Dégâts configurables en inventaire
 * - Rayon et dégâts max réglables
 * - Pas de réinitialisation du timer après mort
 */
public class TimerBimItem extends Item {

    public static final String NBT_ACTIVE = "Active";
    public static final String NBT_HAS_STARTED = "HasStarted";
    public static final String NBT_REMAINING = "RemainingTicks";

    public TimerBimItem(Properties props) {
        super(props);
    }

    public static int getMaxSeconds() { return ModConfigs.TIMER.DEFAULT_SECONDS.get(); }
    public static int getMaxTicks()   { return getMaxSeconds() * 20; }

    @Override public UseAction getUseAnimation(ItemStack stack) { return UseAction.BOW; }
    @Override public int getUseDuration(ItemStack stack) { return 72000; }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return ActionResult.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, World world, LivingEntity living, int timeLeft) {
        if (world.isClientSide || !(living instanceof PlayerEntity)) return;

        PlayerEntity player = (PlayerEntity) living;
        int used = this.getUseDuration(stack) - timeLeft;
        float power = Math.min(1.0f, used / 20.0f);
        float velocity = (float)(1.7f * power * ModConfigs.TIMER.VITESSE_PROJECTILE.get());

        CompoundNBT tag = stack.getOrCreateTag();
        boolean active     = tag.getBoolean(NBT_ACTIVE);
        boolean hasStarted = tag.getBoolean(NBT_HAS_STARTED);
        int remaining      = tag.contains(NBT_REMAINING) ? tag.getInt(NBT_REMAINING) : getMaxTicks();

        TimerBimProjectileEntity proj = new TimerBimProjectileEntity(world, player);
        proj.setItem(stack.copy());
        proj.setTimerState(active, hasStarted, remaining);
        proj.shootFromRotation(player, player.xRot, player.yRot, 0.0f, velocity, 0.9f);
        world.addFreshEntity(proj);

        int cooldown = ModConfigs.TIMER.COOLDOWN_TICKS.get();
        player.getCooldowns().addCooldown(this, cooldown);

        if (!player.abilities.instabuild) stack.shrink(1);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClientSide) return;

        // 🛑 Si le joueur meurt, consomme immédiatement la Timer active
        if (entity instanceof PlayerEntity && !entity.isAlive()) {
            CompoundNBT tag = stack.getOrCreateTag();
            if (tag.getBoolean(NBT_ACTIVE)) stack.shrink(1);
            return;
        }

        tickTimer(stack, world, entity, entity.getX(), entity.getY(), entity.getZ(), true, null);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        World world = entity.level;
        if (world.isClientSide) return false;

        CompoundNBT tag = stack.getOrCreateTag();
        if (!tag.contains(NBT_REMAINING))
            tag.putInt(NBT_REMAINING, getMaxTicks());
        else if (tag.getInt(NBT_REMAINING) <= 0)
            tag.putInt(NBT_REMAINING, getMaxTicks());

        if (!tag.contains(NBT_HAS_STARTED)) tag.putBoolean(NBT_HAS_STARTED, false);
        if (!tag.contains(NBT_ACTIVE)) tag.putBoolean(NBT_ACTIVE, false);

        boolean active = tag.getBoolean(NBT_ACTIVE);
        boolean started = tag.getBoolean(NBT_HAS_STARTED);
        int remaining = tag.getInt(NBT_REMAINING);

        if (started && remaining > 0) {
            tickTimer(stack, world, entity, entity.getX(), entity.getY(), entity.getZ(), false, entity);
            return false;
        }

        if (!active) return false;
        tickTimer(stack, world, entity, entity.getX(), entity.getY(), entity.getZ(), false, entity);
        return false;
    }

    private boolean tickTimer(ItemStack stack, World world, Entity holder,
                              double x, double y, double z,
                              boolean shrinkOnExplode, ItemEntity entityToRemove) {

        CompoundNBT tag = stack.getOrCreateTag();
        boolean active = tag.getBoolean(NBT_ACTIVE);
        if (!active) return false;

        int remaining = tag.contains(NBT_REMAINING) ? tag.getInt(NBT_REMAINING) : getMaxTicks();
        if (remaining <= 0) return false;

        remaining--;
        tag.putInt(NBT_REMAINING, remaining);

        if (remaining <= 0) {
            if (!world.isClientSide) {
                tag.putBoolean(NBT_ACTIVE, false);
                tag.putBoolean(NBT_HAS_STARTED, false);
                tag.putInt(NBT_REMAINING, 0);
                stack.setTag(tag);

                // Explosion d'inventaire ou normale selon le contexte
                if (holder instanceof PlayerEntity) {
                    PlayerEntity player = (PlayerEntity) holder;
                    double dmg = ModConfigs.TIMER.INVENTORY_EXPLOSION_DAMAGE.get();
                    double radius = ModConfigs.TIMER.INVENTORY_EXPLOSION_RADIUS.get();
                    double maxDmg = ModConfigs.TIMER.MAX_DAMAGE_AT_EPICENTER.get();
                    applyInventoryExplosion(world, player, dmg, radius, maxDmg);
                    stack.shrink(1);
                } else {
                    safeExplosion(world, x, y, z);
                    if (shrinkOnExplode && stack.getCount() > 0) stack.shrink(1);
                    else if (entityToRemove != null) entityToRemove.remove();
                }
            }
            return true;
        }

        // 🔁 Synchronisation toutes les secondes
        if (!world.isClientSide && entityToRemove != null && remaining % 20 == 0) {
            Net.CH.send(PacketDistributor.TRACKING_ENTITY.with(() -> entityToRemove),
                    new TimerItemSyncS2C(entityToRemove.getId(), remaining));
        }

        return false;
    }

    /** 💥 Explosion dans l’inventaire : dégâts progressifs, pas de casse de blocs */
    private static void applyInventoryExplosion(World world, PlayerEntity player,
                                                double baseDamage, double radius, double maxDamageEpicenter) {
        if (!(world instanceof ServerWorld)) return;
        ServerWorld sw = (ServerWorld) world;

        double x = player.getX();
        double y = player.getY() + player.getBbHeight() * 0.5;
        double z = player.getZ();

        // 🔊 Effet visuel/sonore
        sw.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0F, 1.0F);
        sw.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 8, 0.3, 0.3, 0.3, 0.02);

        // 💀 Dégâts dégressifs sur rayon
        List<LivingEntity> victims = sw.getEntitiesOfClass(LivingEntity.class,
                new AxisAlignedBB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius),
                e -> e.isAlive());

        for (LivingEntity e : victims) {
            double dx = e.getX() - x;
            double dy = (e.getY() + e.getBbHeight() * 0.5) - y;
            double dz = e.getZ() - z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > radius) continue;

            double ratio = 1.0 - (dist / radius);
            double dmg = baseDamage + (maxDamageEpicenter - baseDamage) * ratio;

            // Le porteur prend uniquement le baseDamage
            if (e == player) dmg = baseDamage;

            e.hurt(DamageSource.explosion(player), (float) dmg);
        }
    }

    /** 💣 Explosion standard du projectile (au sol/en l’air) */
    public static void safeExplosion(World world, double x, double y, double z) {
        if (!(world instanceof ServerWorld)) return;
        ServerWorld sw = (ServerWorld) world;

        boolean breakBlocks = ModConfigs.TIMER.BREAK_BLOCKS.get();
        boolean fire = ModConfigs.TIMER.CAUSES_FIRE.get();
        boolean noItemDestroy = ModConfigs.TIMER.NO_ITEM_DESTROY.get();

        double blockRadius = ModConfigs.TIMER.BREAK_BLOCK_RADIUS.get();
        double radius = ModConfigs.TIMER.EXPLOSION_RADIUS.get();
        double visualRadius = ModConfigs.TIMER.EXPLOSION_VISUAL_RADIUS.get();
        float power = ModConfigs.TIMER.EXPLOSION_STRENGTH.get().floatValue();

        BlockPos center = new BlockPos(x, y, z);

        sw.playSound(null, center, SoundEvents.GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        sw.sendParticles(ParticleTypes.EXPLOSION, x, y, z, (int)(visualRadius * 4),
                visualRadius / 2, visualRadius / 2, visualRadius / 2, 0.1);
        sw.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0, 0, 0);

        Explosion explosion = new Explosion(world, null, null, null, x, y, z, power, fire,
                breakBlocks ? Explosion.Mode.DESTROY : Explosion.Mode.NONE);
        explosion.explode();
        explosion.finalizeExplosion(true);

        // 🔧 Protection et effets secondaires
        if (noItemDestroy) {
            AxisAlignedBB area = new AxisAlignedBB(x-radius, y-radius, z-radius, x+radius, y+radius, z+radius);
            List<ItemEntity> items = sw.getEntitiesOfClass(ItemEntity.class, area);
            for (ItemEntity it : items) {
                it.setInvulnerable(true);
                Vector3d dir = it.position().subtract(x,y,z).normalize().scale(0.25);
                it.setDeltaMovement(it.getDeltaMovement().add(dir));
            }
        }
    }

    /** 🔁 Active / désactive manuellement le minuteur */
    public static void toggleTimer(ItemStack stack) {
        CompoundNBT tag = stack.getOrCreateTag();
        boolean active = tag.getBoolean(NBT_ACTIVE);
        boolean hasStarted = tag.getBoolean(NBT_HAS_STARTED);
        int remaining = tag.contains(NBT_REMAINING)
                ? tag.getInt(NBT_REMAINING)
                : getMaxTicks();

        if (!hasStarted && !active) {
            tag.putBoolean(NBT_ACTIVE, true);
            tag.putBoolean(NBT_HAS_STARTED, true);
            tag.putInt(NBT_REMAINING, getMaxTicks());
        } else if (active) {
            tag.putBoolean(NBT_ACTIVE, false);
            tag.putBoolean(NBT_HAS_STARTED, true);
        } else {
            tag.putBoolean(NBT_ACTIVE, true);
            tag.putBoolean(NBT_HAS_STARTED, true);
        }

        tag.putInt(NBT_REMAINING, Math.max(0, remaining));
        stack.setTag(tag);
    }
}
