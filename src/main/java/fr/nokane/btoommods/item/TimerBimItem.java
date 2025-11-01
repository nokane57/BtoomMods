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
        boolean active   = tag.getBoolean(NBT_ACTIVE);
        boolean started  = tag.getBoolean(NBT_HAS_STARTED);
        int remaining    = tag.contains(NBT_REMAINING) ? tag.getInt(NBT_REMAINING) : getMaxTicks();

        TimerBimProjectileEntity proj = new TimerBimProjectileEntity(world, player);
        proj.setItem(stack.copy());
        proj.setTimerState(active, started, remaining);
        proj.shootFromRotation(player, player.xRot, player.yRot, 0.0f, velocity, 0.9f);
        world.addFreshEntity(proj);

        player.getCooldowns().addCooldown(this, ModConfigs.TIMER.COOLDOWN_TICKS.get());
        if (!player.abilities.instabuild) stack.shrink(1);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClientSide) return;

        // Si le joueur meurt avec un timer actif → le consomme
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
        // Init NBT (Forge 1.16.5)
        if (!tag.contains(NBT_REMAINING))   tag.putInt(NBT_REMAINING, getMaxTicks());
        if (!tag.contains(NBT_HAS_STARTED)) tag.putBoolean(NBT_HAS_STARTED, false);
        if (!tag.contains(NBT_ACTIVE))      tag.putBoolean(NBT_ACTIVE, false);

        boolean active  = tag.getBoolean(NBT_ACTIVE);
        boolean started = tag.getBoolean(NBT_HAS_STARTED);
        int remaining   = tag.getInt(NBT_REMAINING);

        // Pause : ne pas décrémenter
        if (started && !active) return false;

        if (active && remaining > 0) {
            tickTimer(stack, world, entity, entity.getX(), entity.getY(), entity.getZ(), false, entity);
        }
        return false;
    }

    private boolean tickTimer(ItemStack stack, World world, Entity holder,
                              double x, double y, double z,
                              boolean shrinkOnExplode, ItemEntity entityToRemove) {

        CompoundNBT tag = stack.getOrCreateTag();
        if (!tag.getBoolean(NBT_ACTIVE)) return false;

        int remaining = tag.getInt(NBT_REMAINING);
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
                    applyInventoryExplosion((PlayerEntity) holder, stack);
                } else {
                    TimerBimProjectileEntity.safeExplosionItem(world, x, y, z);
                    if (entityToRemove != null) entityToRemove.remove();
                }

                if (shrinkOnExplode && stack.getCount() > 0) stack.shrink(1);
            }
            return true;
        }

        // Sync HUD côté client pour un item droppé (toutes les secondes)
        if (!world.isClientSide && entityToRemove != null && remaining % 20 == 0) {
            Net.CH.send(PacketDistributor.TRACKING_ENTITY.with(() -> entityToRemove),
                    new TimerItemSyncS2C(entityToRemove.getId(), remaining));
        }
        return false;
    }

    /** Explosion dans l’inventaire (dégâts en demi-cœurs / HP) avec décroissance linéaire. */
    private static void applyInventoryExplosion(PlayerEntity player, ItemStack explodedStack) {
        World world = player.level;
        if (!(world instanceof ServerWorld)) return;
        ServerWorld sw = (ServerWorld) world;

        double radius = ModConfigs.TIMER.INVENTORY_RADIUS.get();
        double dmgEpic = ModConfigs.TIMER.INVENTORY_EPICENTER_DAMAGE.get();
        double dmgOuter = ModConfigs.TIMER.INVENTORY_RADIUS_DAMAGE.get();

        double x = player.getX();
        double y = player.getY() + player.getBbHeight() * 0.5;
        double z = player.getZ();

        // Supprime uniquement le stack qui a explosé
        for (int i = 0; i < player.inventory.items.size(); i++) {
            ItemStack s = player.inventory.items.get(i);
            if (s == explodedStack) {
                s.shrink(s.getCount());
                player.inventory.items.set(i, ItemStack.EMPTY);
                player.inventory.setChanged();
                break;
            }
        }

        // Effets visuels
        sw.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundCategory.PLAYERS, 1f, 1f);
        sw.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 8, 0.3, 0.3, 0.3, 0.02);

        AxisAlignedBB area = new AxisAlignedBB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        List<LivingEntity> victims = sw.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive());

        for (LivingEntity e : victims) {
            double dist = Math.sqrt(e.distanceToSqr(x, y, z));
            if (dist > radius) continue;

            double t = Math.min(1.0, Math.max(0.0, dist / radius));
            float damage = (float) (dmgEpic * (1.0 - t) + dmgOuter * t); // décroissance linéaire
            e.hurt(DamageSource.explosion(player), damage);
        }
    }

    public static void toggleTimer(ItemStack stack) {
        CompoundNBT tag = stack.getOrCreateTag();
        boolean active   = tag.getBoolean(NBT_ACTIVE);
        boolean started  = tag.getBoolean(NBT_HAS_STARTED);

        if (!started && !active) {
            tag.putBoolean(NBT_ACTIVE, true);
            tag.putBoolean(NBT_HAS_STARTED, true);
            tag.putInt(NBT_REMAINING, getMaxTicks());
        } else {
            tag.putBoolean(NBT_ACTIVE, !active);
            tag.putBoolean(NBT_HAS_STARTED, true);
        }
        stack.setTag(tag);
    }
}
