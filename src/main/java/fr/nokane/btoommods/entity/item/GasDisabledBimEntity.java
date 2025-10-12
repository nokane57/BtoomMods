package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.*;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.network.IPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.IndirectEntityDamageSource;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

/**
 * 💨 GasDisabledBimEntity
 * - Projectile du BIM gaz "vide"
 * - Poids / rebonds identiques au Timer BIM
 * - Ne crée PAS de nuage de gaz
 * - Se dissipe automatiquement après quelques secondes
 */
public class GasDisabledBimEntity extends ProjectileItemEntity {

    private static final double GROUND_EPS = 0.02;
    private long lastGroundHitTime = -1;
    private int ticksSinceLaunch = 0;
    private int fuseTicks = 60; // 💥 Disparition auto après 3 secondes (60 ticks)

    public GasDisabledBimEntity(EntityType<? extends GasDisabledBimEntity> type, World world) {
        super(type, world);
    }

    public GasDisabledBimEntity(World world, LivingEntity owner) {
        super(ModEntities.GAS_BIM_DISABLED.get(), owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.GAS_BIM_DISABLED.get();
    }

    @Override
    public void tick() {
        super.tick();
        ticksSinceLaunch++;

        if (!this.isNoGravity()) {
            Vector3d m = this.getDeltaMovement();
            this.setDeltaMovement(m.x, m.y - ModConfigs.GAS_DISABLED.POIDS_PROJECTILE.get() * 0.04D, m.z);
        }

        BlockPos pos = this.blockPosition();
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (!shape.isEmpty()) {
            double topY = pos.getY() + shape.max(Direction.Axis.Y);
            if (this.getY() < topY + GROUND_EPS) {
                this.setPos(this.getX(), topY + GROUND_EPS, this.getZ());
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.0, 0.6));
            }
        }

        if (level.isClientSide) return;

        // ⏲️ Disparition auto
        if (fuseTicks-- <= 0) {
            level.levelEvent(2001, this.blockPosition(), 0); // petite fumée
            this.remove();
            return;
        }

        if (ticksSinceLaunch >= 100 && isGrounded()) {
            level.levelEvent(2001, this.blockPosition(), 0);
            this.remove();
        }
    }

    @Override
    protected void onHit(RayTraceResult hit) {
        if (level.isClientSide) return;

        if (hit.getType() == RayTraceResult.Type.ENTITY) {
            this.onHitEntity((EntityRayTraceResult) hit);
            return;
        }

        if (hit.getType() != RayTraceResult.Type.BLOCK) return;

        BlockRayTraceResult br = (BlockRayTraceResult) hit;
        Direction face = br.getDirection();
        BlockPos bpos = br.getBlockPos();
        Vector3d loc = br.getLocation();

        double restitutionGround = ModConfigs.GAS_DISABLED.RESTITUTION_GROUND.get();
        double frictionGround = ModConfigs.GAS_DISABLED.FRICTION_GROUND.get();
        double restitutionWall = ModConfigs.GAS_DISABLED.RESTITUTION_WALL.get();
        double frictionWall = ModConfigs.GAS_DISABLED.FRICTION_WALL.get();
        double maxBounceUp = ModConfigs.GAS_DISABLED.MAX_BOUNCE_UP.get();
        double stopEps = ModConfigs.GAS_DISABLED.STOP_EPS.get();

        Vector3d v = this.getDeltaMovement();

        switch (face) {
            case UP: {
                double topY = bpos.getY() + level.getBlockState(bpos).getCollisionShape(level, bpos).max(Direction.Axis.Y);
                this.setPos(loc.x, Math.max(loc.y, topY) + GROUND_EPS, loc.z);

                double newVx = v.x * frictionGround;
                double newVz = v.z * frictionGround;
                double newVy = Math.abs(v.y) * restitutionGround;
                newVy = Math.min(Math.max(newVy, 0.02), maxBounceUp);

                double horiz = Math.hypot(newVx, newVz);
                if (newVy < stopEps && horiz < 0.05) {
                    this.setDeltaMovement(0.0, 0.0, 0.0);
                    lastGroundHitTime = level.getGameTime();
                    break;
                }

                this.setDeltaMovement(newVx, newVy, newVz);
                lastGroundHitTime = level.getGameTime();
                SoundUtils.playRebound(level, getX(), getY(), getZ());
                break;
            }
            default: {
                Vector3d newV = new Vector3d(
                        face.getAxis() == Direction.Axis.X ? -v.x * restitutionWall : v.x * frictionWall,
                        Math.min(Math.max(v.y * 0.25, 0.0) + 0.05, maxBounceUp * 0.6),
                        face.getAxis() == Direction.Axis.Z ? -v.z * restitutionWall : v.z * frictionWall
                );
                this.setDeltaMovement(newV);
                SoundUtils.playRebound(level, getX(), getY(), getZ());
                break;
            }
        }
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult hit) {
        Entity target = hit.getEntity();
        if (!level.isClientSide && target instanceof LivingEntity) {
            float dmg = (float) (ModConfigs.GAS_DISABLED.GAS_IMPACT_HEARTS.get() * 2.0);
            target.hurt(new IndirectEntityDamageSource("gas_bim_disabled", this, this.getOwner()).setProjectile(), dmg);

            Vector3d v = this.getDeltaMovement();
            this.setDeltaMovement(-v.x * 0.3, 0.1, -v.z * 0.3);
            SoundUtils.playRebound(level, getX(), getY(), getZ());
        }
    }

    private boolean isGrounded() {
        long now = level.getGameTime();
        if (now - lastGroundHitTime <= 2) return true;
        BlockPos below = this.blockPosition().below();
        double topY = below.getY() + level.getBlockState(below)
                .getCollisionShape(level, below).max(Direction.Axis.Y);
        boolean nearSurface = (this.getY() - topY) <= 0.06;
        boolean slowY = Math.abs(this.getDeltaMovement().y) < 0.08;
        return nearSurface && slowY;
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
