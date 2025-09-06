// fr/nokane/btoommods/entity/misc/BlazingFireFieldEntity.java
package fr.nokane.btoommods.entity.misc;

import fr.nokane.btoommods.config.ModConfigs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.HashSet;
import java.util.Set;

public class BlazingFireFieldEntity extends Entity {
    private int age;
    private static final DamageSource BLAZING_FLAME = (new DamageSource("blazing_flame")).setIsFire();

    public BlazingFireFieldEntity(EntityType<? extends BlazingFireFieldEntity> type, World level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();

        // client: visuel
        if (level.isClientSide) { spawnParticles(); return; }

        // serveur: logique
        age++;
        if (age >= ModConfigs.COMMON.BLAZING_FIRE_LIFETIME.get()) { this.remove(); return; }

        int len = ModConfigs.COMMON.BLAZING_FIRE_LENGTH.get();
        int width = Math.max(1, ModConfigs.COMMON.BLAZING_FIRE_WIDTH.get());
        float insideHearts = ModConfigs.COMMON.BLAZING_FIRE_DMG_INSIDE_HEARTS.get().floatValue();
        int burnDuration = ModConfigs.COMMON.BLAZING_BURN_DURATION.get();

        BlockPos c = this.blockPosition();
        double minY = c.getY();
        double maxY = c.getY() + 2.0;
        double halfW = 0.5 * width;

        AxisAlignedBB bandX = new AxisAlignedBB(c.getX() - len, minY, c.getZ() - halfW, c.getX() + len + 1, maxY, c.getZ() + halfW);
        AxisAlignedBB bandZ = new AxisAlignedBB(c.getX() - halfW, minY, c.getZ() - len, c.getX() + halfW,     maxY, c.getZ() + len + 1);

        Set<LivingEntity> victims = new HashSet<>();
        victims.addAll(level.getEntitiesOfClass(LivingEntity.class, bandX, LivingEntity::isAlive));
        victims.addAll(level.getEntitiesOfClass(LivingEntity.class, bandZ, LivingEntity::isAlive));

        for (LivingEntity e : victims) {
            e.hurt(BLAZING_FLAME, insideHearts * 2.0F);
            e.setSecondsOnFire(Math.max(1, burnDuration / 20));
        }
    }

    private void spawnParticles() {
        int len = ModConfigs.COMMON.BLAZING_FIRE_LENGTH.get();
        int width = Math.max(1, ModConfigs.COMMON.BLAZING_FIRE_WIDTH.get());
        BlockPos c = this.blockPosition();
        double y = c.getY() + 0.1;
        java.util.Random r = this.level.random;

        // bras X
        for (int dx = -len; dx <= len; dx++) {
            for (int w = -(width-1)/2; w <= width/2; w++) {
                double x = c.getX() + dx + 0.5;
                double z = c.getZ() + w  + 0.5;
                if (r.nextFloat() < 0.85F) level.addParticle(net.minecraft.particles.ParticleTypes.FLAME, x, y, z, 0, 0.01, 0);
                if (r.nextFloat() < 0.30F) level.addParticle(net.minecraft.particles.ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.02, 0);
            }
        }
        // bras Z
        for (int dz = -len; dz <= len; dz++) {
            for (int w = -(width-1)/2; w <= width/2; w++) {
                double x = c.getX() + w  + 0.5;
                double z = c.getZ() + dz + 0.5;
                if (r.nextFloat() < 0.85F) level.addParticle(net.minecraft.particles.ParticleTypes.FLAME, x, y, z, 0, 0.01, 0);
                if (r.nextFloat() < 0.30F) level.addParticle(net.minecraft.particles.ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.02, 0);
            }
        }
    }

    @Override protected void readAdditionalSaveData(CompoundNBT nbt) { this.age = nbt.getInt("Age"); }
    @Override protected void addAdditionalSaveData(CompoundNBT nbt) { nbt.putInt("Age", this.age); }
    @Override public net.minecraft.network.IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean isPickable() { return false; }
}
