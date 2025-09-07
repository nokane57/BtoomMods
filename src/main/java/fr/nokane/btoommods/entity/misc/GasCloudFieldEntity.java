// fr/nokane/btoommods/entity/misc/GasCloudFieldEntity.java
package fr.nokane.btoommods.entity.misc;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.particle.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

public class GasCloudFieldEntity extends Entity {
    private int age; // ticks
    private boolean droppedShell = false;

    private static final DamageSource GAS_DAMAGE =
            (new DamageSource("gas_bim")).bypassArmor();

    public GasCloudFieldEntity(EntityType<? extends GasCloudFieldEntity> type, World level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        age++;

        // fait "tomber" le nuage si pas de sol dessous
        settleTowardGround();

        final int step      = ModConfigs.COMMON.GAS_RING_STEP.get();           // 15
        final int maxR      = ModConfigs.COMMON.GAS_MAX_RADIUS.get();          // 45
        final int stepTicks = ModConfigs.COMMON.GAS_EXPAND_STEP_TICKS.get();   // ex: 40

        int stage = Math.min(3, age / stepTicks + 1); // 1..3
        int currentRadius = Math.min(maxR, stage * step);

        if (level.isClientSide) {
            spawnParticles(currentRadius);
            return;
        }

        // HP/tick à partir de cœurs/seconde (2/20 = 0.1)
        float hpTickInner = ModConfigs.COMMON.GAS_DMG_INNER_HPS.get().floatValue() * 0.1F;
        float hpTickMid   = ModConfigs.COMMON.GAS_DMG_MID_HPS.get().floatValue()   * 0.1F;
        float hpTickOuter = ModConfigs.COMMON.GAS_DMG_OUTER_HPS.get().floatValue() * 0.1F;

        BlockPos c = this.blockPosition();
        int yMin = c.getY() - 1;
        int yMax = c.getY() + 4;

        AxisAlignedBB aabb = new AxisAlignedBB(
                c.getX() - currentRadius, yMin, c.getZ() - currentRadius,
                c.getX() + currentRadius + 1, yMax, c.getZ() + currentRadius + 1
        );

        Set<LivingEntity> list = new HashSet<>(level.getEntitiesOfClass(LivingEntity.class, aabb, LivingEntity::isAlive));
        for (LivingEntity e : list) {
            double d = Math.sqrt(e.distanceToSqr(c.getX() + 0.5, e.getY(), c.getZ() + 0.5));

            if (ModConfigs.COMMON.GAS_GRAVITY_LIMIT.get()) {
                int above = blocksAboveGround(e);
                if (above > ModConfigs.COMMON.GAS_MAX_ABOVE_GROUND.get()) continue;
            }

            float hp = 0F;
            if (d <= step) hp = hpTickInner;        // 0–15
            else if (d <= 2*step) hp = hpTickMid;   // 15–30
            else if (d <= 3*step) hp = hpTickOuter; // 30–45

            if (hp > 0F) e.hurt(GAS_DAMAGE, hp);
        }

        // durée : 3 étapes + petite traîne
        if (age > stepTicks * 3 + 40) {
            dropDisabledShellOnce();
            this.remove();
        }
    }

    /** Fait descendre le centre du nuage s’il n’y a pas de support sous lui. */
    private void settleTowardGround() {
        BlockPos below = this.blockPosition().below();
        if (!hasSolidBelowWithin(below, 2)) {
            // pas de sol proche : descend doucement
            this.setPos(this.getX(), this.getY() - 0.08, this.getZ());
        } else {
            // empêche d'entrer dans le bloc
            BlockPos ground = findGroundBelow(this.blockPosition(), 64);
            if (ground != null) {
                double targetY = ground.getY() + 1.0;
                if (this.getY() < targetY) {
                    this.setPos(this.getX(), targetY + 0.01, this.getZ());
                }
            }
        }
    }

    private boolean hasSolidBelowWithin(BlockPos startBelow, int range) {
        for (int i = 0; i < range; i++) {
            BlockPos p = startBelow.below(i);
            if (level.getBlockState(p).getMaterial().isSolid()) return true;
        }
        return false;
    }

    private BlockPos findGroundBelow(BlockPos start, int maxDown) {
        for (int i = 0; i < maxDown; i++) {
            BlockPos p = start.below(i);
            if (level.getBlockState(p).getMaterial().isSolid()) return p;
        }
        return null;
    }

    private int blocksAboveGround(LivingEntity e) {
        BlockPos pos = e.blockPosition();
        for (int i = 0; i <= 32; i++) {
            BlockPos p = pos.below(i);
            if (level.getBlockState(p).getMaterial().isSolid()) return i;
        }
        return 999;
    }

    private void spawnParticles(int radius) {
        BlockPos c = this.blockPosition();
        java.util.Random r = this.level.random;

        int samples = 80 + radius * 4;
        for (int i = 0; i < samples; i++) {
            double ang = r.nextDouble() * Math.PI * 2.0;
            double rad = r.nextDouble() * radius;
            double x = c.getX() + 0.5 + Math.cos(ang) * rad;
            double z = c.getZ() + 0.5 + Math.sin(ang) * rad;
            double y = this.getY() + 0.2 + r.nextDouble() * 0.5;

            level.addParticle(ModParticles.GAS_CLOUD.get(), x, y, z, 0.0, 0.003, 0.0);
        }
    }

    private void dropDisabledShellOnce() {
        if (droppedShell || level.isClientSide) return;
        ItemEntity drop = new ItemEntity(level, this.getX(), this.getY(), this.getZ(),
                ModItems.GAS_BIM_DISABLED.get().getDefaultInstance());
        level.addFreshEntity(drop);
        droppedShell = true;
    }

    @Override protected void readAdditionalSaveData(CompoundNBT nbt) { age = nbt.getInt("Age"); }
    @Override protected void addAdditionalSaveData(CompoundNBT nbt) { nbt.putInt("Age", age); }
    @Override public net.minecraft.network.IPacket<?> getAddEntityPacket() {
        return net.minecraftforge.fml.network.NetworkHooks.getEntitySpawningPacket(this);
    }
    @Override public boolean isPickable() { return false; }
}
