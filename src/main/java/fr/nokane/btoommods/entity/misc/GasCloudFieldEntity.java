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
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.HashSet;

public class GasCloudFieldEntity extends Entity {
    private int age; // ticks
    private boolean droppedShell = false;

    // descente du nuage
    private static final double SINK_SPEED = 0.08;
    private static final double Y_OFFSET_ABOVE_GROUND = 0.75;

    // dégâts appliqués toutes les 10 ticks (≈ temps d'invulnérabilité vanilla)
    private static final int HURT_PERIOD_TICKS = 10;

    public GasCloudFieldEntity(EntityType<? extends GasCloudFieldEntity> type, World level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        age++;

        // fait “ramper” le nuage vers le sol
        settleTowardGround();

        final int step      = ModConfigs.COMMON.GAS_RING_STEP.get();
        final int maxR      = ModConfigs.COMMON.GAS_MAX_RADIUS.get();
        final int stepTicks = ModConfigs.COMMON.GAS_EXPAND_STEP_TICKS.get();

        int stage = Math.min(3, age / stepTicks + 1); // 1..3
        int currentRadius = Math.min(maxR, stage * step);

        if (level.isClientSide) {
            spawnParticles(currentRadius);
        } else {
            // ---- SERVEUR : dégâts ----
            if ((age % HURT_PERIOD_TICKS) == 0) {
                applyGasDamage(currentRadius, step);
            }

            // fin de vie du nuage
            if (age > stepTicks * 3 + 40) {
                dropDisabledShellOnce();
                this.remove();
            }
        }
    }

    /** Convertit (cœurs/s) -> HP par période de HURT_PERIOD_TICKS. */
    private static float hpPerPeriod(double heartsPerSecond) {
        // 1 cœur = 2 HP ; période = HURT_PERIOD_TICKS ; 20 ticks = 1s
        return (float)(heartsPerSecond * 2.0 * HURT_PERIOD_TICKS / 20.0);
    }

    private void applyGasDamage(int currentRadius, int step) {
        // Config en CŒURS / SECONDE
        final double innerHps = ModConfigs.COMMON.GAS_DMG_INNER_HPS.get();
        final double midHps   = ModConfigs.COMMON.GAS_DMG_MID_HPS.get();
        final double outerHps = ModConfigs.COMMON.GAS_DMG_OUTER_HPS.get();

        // Conversion en HP par application (toutes les 10 ticks)
        final float innerHp = hpPerPeriod(innerHps);
        final float midHp   = hpPerPeriod(midHps);
        final float outerHp = hpPerPeriod(outerHps);

        final boolean bypassArmor = ModConfigs.COMMON.GAS_DMG_BYPASS_ARMOR.get();

        BlockPos c = this.blockPosition();
        double yMin = this.getY() - 0.20;
        double yMax = this.getY() + ModConfigs.COMMON.GAS_DAMAGE_HEIGHT.get(); // ← hauteur configurable

        AxisAlignedBB aabb = new AxisAlignedBB(
                c.getX() - currentRadius, yMin, c.getZ() - currentRadius,
                c.getX() + currentRadius + 1, yMax, c.getZ() + currentRadius + 1
        );

        // seuils fixes des anneaux
        final double r1 = step;
        final double r2 = step * 2.0;
        final double r3 = step * 3.0;

        DamageSource base = new DamageSource("gas_bim");
        if (bypassArmor) base = base.bypassArmor();

        for (LivingEntity e : new HashSet<>(level.getEntitiesOfClass(LivingEntity.class, aabb, LivingEntity::isAlive))) {
            // distance horizontale
            double dx = e.getX() - (c.getX() + 0.5);
            double dz = e.getZ() - (c.getZ() + 0.5);
            double d  = Math.sqrt(dx*dx + dz*dz);

            // rien hors du nuage
            if (d > currentRadius) continue;

            // limite de hauteur au-dessus du sol
            if (ModConfigs.COMMON.GAS_GRAVITY_LIMIT.get()) {
                int above = blocksAboveGround(e);
                if (above > ModConfigs.COMMON.GAS_MAX_ABOVE_GROUND.get()) continue;
            }

            float hp = 0F;
            if (d <= r1) {
                hp = innerHp;
            } else if (d <= r2) {
                hp = midHp;
            } else if (d <= r3) {
                hp = outerHp;
            }

            if (hp > 0F) {
                e.hurt(base, hp);
            }
        }
    }

    /** Descend vers le haut du bloc solide juste dessous, sans traverser. */
    private void settleTowardGround() {
        BlockPos below = this.blockPosition().below();
        double topY = topYOf(below);
        double targetY = topY + Y_OFFSET_ABOVE_GROUND;

        if (this.getY() > targetY + 0.01) {
            double dy = Math.min(SINK_SPEED, this.getY() - targetY);
            this.setPos(this.getX(), this.getY() - dy, this.getZ());
        } else if (this.getY() < targetY) {
            this.setPos(this.getX(), targetY, this.getZ());
        }
    }

    private BlockPos snapColumnToTopSolid(BlockPos colXZ, int fromY) {
        Vector3d start = new Vector3d(colXZ.getX() + 0.5, fromY, colXZ.getZ() + 0.5);
        Vector3d end   = new Vector3d(colXZ.getX() + 0.5, fromY - 256, colXZ.getZ() + 0.5);
        RayTraceContext ctx = new RayTraceContext(
                start, end,
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE,
                this
        );
        RayTraceResult rt = level.clip(ctx);

        if (rt.getType() == RayTraceResult.Type.BLOCK) {
            BlockRayTraceResult br = (BlockRayTraceResult) rt;
            BlockPos hit = br.getBlockPos();
            Direction face = br.getDirection();
            BlockPos top = (face == Direction.UP) ? hit.above() : hit.relative(face);
            if (level.getBlockState(top.below()).isFaceSturdy(level, top.below(), Direction.UP)) {
                return top;
            }
        }

        int y = level.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, colXZ.getX(), colXZ.getZ());
        return new BlockPos(colXZ.getX(), y, colXZ.getZ());
    }

    private double topYOf(BlockPos pos) {
        VoxelShape s = level.getBlockState(pos).getCollisionShape(level, pos);
        double add = s.isEmpty() ? 1.0 : s.max(Direction.Axis.Y);
        return pos.getY() + add;
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
        int fromY = (int)Math.ceil(this.getY() + 16);

        for (int i = 0; i < samples; i++) {
            double ang = r.nextDouble() * Math.PI * 2.0;
            double rad = r.nextDouble() * radius;
            double px = c.getX() + 0.5 + Math.cos(ang) * rad;
            double pz = c.getZ() + 0.5 + Math.sin(ang) * rad;

            BlockPos ground = snapColumnToTopSolid(
                    new BlockPos(MathHelper.floor(px), fromY, MathHelper.floor(pz)), fromY);

            double py = ground.getY() + 0.02 + r.nextDouble() * 0.15;

            level.addParticle(ModParticles.GAS_CLOUD.get(), px, py, pz, 0.0, 0.003, 0.0);
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
    @Override public net.minecraft.network.IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean isPickable() { return false; }
}
