package fr.nokane.btoommods.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.item.TimerBimProjectileEntity;
import fr.nokane.btoommods.net.Net;
import fr.nokane.btoommods.net.TimerItemSyncS2C;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.List;

public class TimerBimItem extends Item {

    public static final String NBT_ACTIVE = "Active";
    public static final String NBT_HAS_STARTED = "HasStarted";
    public static final String NBT_REMAINING = "RemainingTicks";
    public static final String NBT_JUST_EXPLODED = "JustExploded";

    public TimerBimItem(Properties props) { super(props); }

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
        float velocity = (float) (1.7f * power * ModConfigs.TIMER.VITESSE_PROJECTILE.get());

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

        // Consomme si le joueur est mort
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

        // Initialisation si manquante
        if (!tag.contains(NBT_REMAINING))
            tag.putInt(NBT_REMAINING, getMaxTicks());
        if (!tag.contains(NBT_HAS_STARTED))
            tag.putBoolean(NBT_HAS_STARTED, false);
        if (!tag.contains(NBT_ACTIVE))
            tag.putBoolean(NBT_ACTIVE, false);

        boolean active = tag.getBoolean(NBT_ACTIVE);
        boolean started = tag.getBoolean(NBT_HAS_STARTED);
        int remaining = tag.getInt(NBT_REMAINING);

        // 🟡 Si en pause : ne tick pas, garde RemainingTicks tel quel
        if (started && !active) {
            // Rien à faire, le timer reste figé
            return false;
        }

        // 🔴 Si actif : tick normalement
        if (active && remaining > 0) {
            tickTimer(stack, world, entity, entity.getX(), entity.getY(), entity.getZ(), false, entity);
        }

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

                if (holder instanceof PlayerEntity) {
                    // ⚡ Explosion dans l’inventaire : consomme uniquement celui qui a explosé
                    boolean consumed = applyInventoryExplosion((PlayerEntity) holder, stack);

                    if (!consumed && stack.getCount() > 0) {
                        stack.shrink(1);
                    }
                } else {
                    TimerBimProjectileEntity.safeExplosionWorld(world, x, y, z);
                    if (shrinkOnExplode && stack.getCount() > 0) stack.shrink(1);
                    else if (entityToRemove != null) entityToRemove.remove();
                }
            }
            return true;
        }

        // Sync toutes les secondes
        if (!world.isClientSide && entityToRemove != null && remaining % 20 == 0) {
            Net.CH.send(PacketDistributor.TRACKING_ENTITY.with(() -> entityToRemove),
                    new TimerItemSyncS2C(entityToRemove.getId(), remaining));
        }
        return false;
    }

    /** Explosion dans l'inventaire : consomme UNIQUEMENT le timer explosé, marque "JustExploded" */
    private static boolean applyInventoryExplosion(PlayerEntity player, ItemStack explodedStack) {
        World world = player.level;
        if (!(world instanceof ServerWorld)) return false;
        ServerWorld sw = (ServerWorld) world;

        double radius = ModConfigs.TIMER.INVENTORY_EXPLOSION_RADIUS.get();
        double dmgEpic = ModConfigs.TIMER.MAX_DAMAGE_AT_EPICENTER.get();
        double dmgOuter = ModConfigs.TIMER.INVENTORY_EXPLOSION_DAMAGE.get();

        double x = player.getX();
        double y = player.getY() + player.getBbHeight() * 0.5;
        double z = player.getZ();

        // ✅ Étape 1 : Marque ce timer comme "JustExploded"
        CompoundNBT explodedTag = explodedStack.getOrCreateTag();
        explodedTag.putBoolean(NBT_JUST_EXPLODED, true);
        explodedStack.setTag(explodedTag);

        // Supprime seulement ce stack du slot concerné
        for (int i = 0; i < player.inventory.items.size(); i++) {
            ItemStack s = player.inventory.items.get(i);
            if (s == explodedStack) {
                s.shrink(s.getCount());
                player.inventory.items.set(i, ItemStack.EMPTY);
                player.inventory.setChanged();
                break;
            }
        }

        // 💥 Étape 2 : Explosion et dégâts
        sw.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0F, 1.0F);
        sw.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 8, 0.3, 0.3, 0.3, 0.02);

        AxisAlignedBB area = new AxisAlignedBB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        List<LivingEntity> victims = sw.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive());

        for (LivingEntity e : victims) {
            double dx = e.getX() - x;
            double dy = (e.getY() + e.getBbHeight() * 0.5) - y;
            double dz = e.getZ() - z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > radius) continue;

            double t = Math.min(1.0, dist / radius);
            double dmg = dmgEpic - (dmgEpic - dmgOuter) * t;
            e.hurt(DamageSource.explosion(player), (float) dmg);
        }

        return true;
    }

    public static void toggleTimer(ItemStack stack) {
        CompoundNBT tag = stack.getOrCreateTag();
        boolean active = tag.getBoolean(NBT_ACTIVE);
        boolean hasStarted = tag.getBoolean(NBT_HAS_STARTED);
        int remaining = tag.contains(NBT_REMAINING) ? tag.getInt(NBT_REMAINING) : getMaxTicks();

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
