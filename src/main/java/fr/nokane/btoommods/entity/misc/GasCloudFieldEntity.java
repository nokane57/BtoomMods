// fr/nokane/btoommods/entity/misc/GasCloudFieldEntity.java
package fr.nokane.btoommods.entity.misc;

import fr.nokane.btoommods.config.ModConfigs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

public class GasCloudFieldEntity extends Entity {
    private int age; // ticks
    private static final DamageSource GAS_DAMAGE = (new DamageSource("gas_bim")).bypassArmor(); // gaz : traverse l’armure

    public GasCloudFieldEntity(EntityType<? extends GasCloudFieldEntity> type, World level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        age++;

        final int step = ModConfigs.COMMON.GAS_RING_STEP.get();           // 15
        final int maxR = ModConfigs.COMMON.GAS_MAX_RADIUS.get();          // 45
        final int stepTicks = ModConfigs.COMMON.GAS_EXPAND_STEP_TICKS.get(); // 40 (2s)
        int stage = Math.min(3, age / stepTicks + 1); // 1,2,3

        int currentRadius = Math.min(maxR, stage * step);

        if (level.isClientSide) {
            spawnParticles(currentRadius);
            return;
        }

        // Dégâts par anneau (HP/tick = cœurs/sec * 0.1F)
        float hpTickInner = ModConfigs.COMMON.GAS_DMG_INNER_HPS.get().floatValue() * 0.1F;
        float hpTickMid   = ModConfigs.COMMON.GAS_DMG_MID_HPS.get().floatValue()   * 0.1F;
        float hpTickOuter = ModConfigs.COMMON.GAS_DMG_OUTER_HPS.get().floatValue() * 0.1F;

        BlockPos c = this.blockPosition();
        int yMin = c.getY() - 1;
        int yMax = c.getY() + 4; // hauteur modeste

        AxisAlignedBB aabb = new AxisAlignedBB(
                c.getX() - currentRadius, yMin, c.getZ() - currentRadius,
                c.getX() + currentRadius + 1, yMax, c.getZ() + currentRadius + 1
        );

        Set<LivingEntity> list = new HashSet<>(level.getEntitiesOfClass(LivingEntity.class, aabb, LivingEntity::isAlive));
        for (LivingEntity e : list) {
            double d2 = e.distanceToSqr(c.getX() + 0.5, e.getY(), c.getZ() + 0.5);
            double d  = Math.sqrt(d2);

            // Option gravité : ne pas toucher au-dessus de X blocs du sol
            if (ModConfigs.COMMON.GAS_GRAVITY_LIMIT.get()) {
                int above = blocksAboveGround(e);
                if (above > ModConfigs.COMMON.GAS_MAX_ABOVE_GROUND.get()) continue;
            }

            float hp = 0F;
            if (d <= step) hp = hpTickInner;                 // 0–15
            else if (d <= 2*step) hp = hpTickMid;           // 15–30
            else if (d <= 3*step) hp = hpTickOuter;         // 30–45

            if (hp > 0F) e.hurt(GAS_DAMAGE, hp);
        }

        // durée max = 3 étapes * 2s par défaut + petite persistance => 6s ~ 8s
        if (age > stepTicks * 3 + 40) this.remove();
    }

    /** Compte approximativement combien de blocs l’entité est au-dessus d’un bloc plein. */
    private int blocksAboveGround(LivingEntity e) {
        BlockPos pos = e.blockPosition();
        for (int i = 0; i <= 32; i++) {
            BlockPos p = pos.below(i);
            if (level.getBlockState(p).getMaterial().isSolid()) return i;
        }
        return 999;
    }

    private void spawnParticles(int radius) {
        // Nuage jaune : on “parseme” des particules dans le disque
        BlockPos c = this.blockPosition();
        java.util.Random r = this.level.random;

        int samples = 80 + radius * 4; // plus c’est grand, plus de particules
        for (int i = 0; i < samples; i++) {
            double ang = r.nextDouble() * Math.PI * 2.0;
            double rad = r.nextDouble() * radius;
            double x = c.getX() + 0.5 + Math.cos(ang) * rad;
            double z = c.getZ() + 0.5 + Math.sin(ang) * rad;
            double y = c.getY() + 0.2 + r.nextDouble() * 0.5;

            // nuage : on combine CLOUD et DUST (jaune) pour rendu “gaz”
            level.addParticle(ParticleTypes.CLOUD, x, y, z, 0.0, 0.003, 0.0);
            // petite variation de couleur ? en 1.16 on reste simple : plus de CLOUD suffit
        }
    }

    @Override protected void readAdditionalSaveData(CompoundNBT nbt) { age = nbt.getInt("Age"); }
    @Override protected void addAdditionalSaveData(CompoundNBT nbt) { nbt.putInt("Age", age); }
    @Override public net.minecraft.network.IPacket<?> getAddEntityPacket() {
        return net.minecraftforge.fml.network.NetworkHooks.getEntitySpawningPacket(this);
    }
    @Override public boolean isPickable() { return false; }
}
