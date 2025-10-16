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

        if (!world.isClientSide)
            player.getPersistentData().putInt("timer_cooldown", cooldown);

        if (!player.abilities.instabuild) stack.shrink(1);
    }

    public static int getDisplaySeconds(ItemStack stack) {
        CompoundNBT tag = stack.getOrCreateTag();
        if (!tag.contains(NBT_REMAINING)) tag.putInt(NBT_REMAINING, getMaxTicks());
        int ticks = tag.getInt(NBT_REMAINING);
        if (ticks <= 0) return -1;
        return Math.max(0, Math.min(getMaxSeconds(), (int)Math.ceil(ticks / 20.0)));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClientSide) return;
        tickTimer(stack, world, entity, entity.getX(), entity.getY(), entity.getZ(), true, null);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        World world = entity.level;
        if (world.isClientSide) return false;

        CompoundNBT tag = stack.getOrCreateTag();
        if (!tag.contains(NBT_REMAINING)) tag.putInt(NBT_REMAINING, getMaxTicks());
        if (!tag.contains(NBT_HAS_STARTED)) tag.putBoolean(NBT_HAS_STARTED, false);
        if (!tag.contains(NBT_ACTIVE)) tag.putBoolean(NBT_ACTIVE, false);

        boolean active = tag.getBoolean(NBT_ACTIVE);
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

        int before = remaining;
        remaining--;
        tag.putInt(NBT_REMAINING, remaining);

        // 💥 Explosion quand fini
        if (remaining <= 0) {
            if (!world.isClientSide) {
                // Synchro HUD 0
                if (entityToRemove != null) {
                    Net.CH.send(PacketDistributor.TRACKING_ENTITY.with(() -> entityToRemove),
                            new TimerItemSyncS2C(entityToRemove.getId(), 0));
                } else if (holder instanceof PlayerEntity) {
                    PlayerEntity p = (PlayerEntity) holder;
                    Net.CH.send(PacketDistributor.PLAYER.with(() -> (net.minecraft.entity.player.ServerPlayerEntity)p),
                            new TimerItemSyncS2C(-1, 0));
                }

                // Explosion finale
                safeExplosion(world, x, y, z);

                if (shrinkOnExplode && stack.getCount() > 0) stack.shrink(1);
                else if (entityToRemove != null) entityToRemove.remove();
            }
            return true;
        }

        // 🔁 Synchro chaque seconde
        int secBefore = (int)Math.ceil(before / 20.0);
        int secNow = (int)Math.ceil(remaining / 20.0);
        if (!world.isClientSide && secNow != secBefore) {
            if (entityToRemove != null) {
                Net.CH.send(PacketDistributor.TRACKING_ENTITY.with(() -> entityToRemove),
                        new TimerItemSyncS2C(entityToRemove.getId(), remaining));
            } else if (holder instanceof PlayerEntity) {
                PlayerEntity p = (PlayerEntity) holder;
                Net.CH.send(PacketDistributor.PLAYER.with(() -> (net.minecraft.entity.player.ServerPlayerEntity)p),
                        new TimerItemSyncS2C(-1, remaining));
            }
        }

        return false;
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
    }

    // ==========================================================
    // 💥 Explosion sécurisée (ne détruit jamais les items)
    // ==========================================================
    public static void safeExplosion(World world, double x, double y, double z) {
        if (!(world instanceof ServerWorld)) return;
        ServerWorld sw = (ServerWorld) world;

        boolean breakBlocks = ModConfigs.TIMER.BREAK_BLOCKS.get();
        boolean fire = ModConfigs.TIMER.CAUSES_FIRE.get();
        boolean noItemDestroy = ModConfigs.TIMER.NO_ITEM_DESTROY.get();
        double blockRadius = ModConfigs.TIMER.BREAK_BLOCK_RADIUS.get();
        double radius = ModConfigs.TIMER.EXPLOSION_RADIUS.get();
        float power = ModConfigs.TIMER.EXPLOSION_STRENGTH.get().floatValue();

        BlockPos center = new BlockPos(x, y, z);

        // 💥 Effet visuel
        sw.playSound(null, center, SoundEvents.GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.8F, 1.0F);
        sw.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0, 0, 0);

        // 🧱 Casse manuelle des blocs
        if (breakBlocks) {
            int r = (int) Math.ceil(blockRadius);
            BlockPos.Mutable pos = new BlockPos.Mutable();
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                        if (dist <= blockRadius && !sw.isEmptyBlock(pos)) {
                            sw.destroyBlock(pos, true);
                        }
                    }
                }
            }
        }

        // 💀 Dégâts entités (ignore ItemEntity)
        AxisAlignedBB area = new AxisAlignedBB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        List<Entity> entities = sw.getEntities((Entity) null, area, e -> e.isAlive() && !(e instanceof ItemEntity));
        for (Entity e : entities) {
            if (e instanceof LivingEntity) {
                LivingEntity le = (LivingEntity)e;
                double dist = Math.sqrt(le.distanceToSqr(x, y, z));
                if (dist <= radius) {
                    float dmg = (float)(8.0 * (1.0 - dist / radius));
                    le.hurt(DamageSource.explosion((Explosion) null), dmg * 2.0F);
                }
            }
        }

        // 🔥 Feu optionnel
        if (fire) {
            BlockPos.Mutable bp = new BlockPos.Mutable();
            int fr = (int)Math.ceil(radius / 2);
            for (int dx = -fr; dx <= fr; dx++) {
                for (int dz = -fr; dz <= fr; dz++) {
                    bp.set(center.getX() + dx, center.getY(), center.getZ() + dz);
                    if (sw.isEmptyBlock(bp) && sw.getBlockState(bp.below()).isSolidRender(sw, bp.below())) {
                        sw.setBlock(bp, net.minecraft.block.Blocks.FIRE.defaultBlockState(), 11);
                    }
                }
            }
        }

        // 🪙 Protection des items drop
        if (noItemDestroy) {
            List<ItemEntity> items = sw.getEntitiesOfClass(ItemEntity.class, area);
            for (ItemEntity it : items) {
                it.setInvulnerable(true);
                Vector3d dir = it.position().subtract(x, y, z).normalize().scale(0.25);
                it.setDeltaMovement(it.getDeltaMovement().add(dir));
            }
        }
    }
}
