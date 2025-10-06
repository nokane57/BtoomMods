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

public class GasBimEntity extends ProjectileItemEntity {

    private static final double GROUND_EPS = 0.02;
    private long lastGroundHitTime = -1;
    private int ticksSinceLaunch = 0;

    public GasBimEntity(EntityType<? extends GasBimEntity> type, World world) {
        super(type, world);
    }

    public GasBimEntity(World world, LivingEntity owner) {
        super(ModEntities.GAS_BIM.get(), owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.GAS_BIM.get();
    }

    @Override
    public void tick() {
        super.tick();

        ticksSinceLaunch++;

        // 🎯 Gravité identique au Timer BIM
        if (!this.isNoGravity()) {
            Vector3d m = this.getDeltaMovement();
            this.setDeltaMovement(m.x, m.y - ModConfigs.TIMER.POIDS_PROJECTILE.get(), m.z);
        }

        // 🔧 Anti-enfoncement
        BlockPos pos = this.blockPosition();
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (!shape.isEmpty()) {
            double topY = pos.getY() + shape.max(Direction.Axis.Y);
            if (this.getY() < topY + GROUND_EPS) {
                this.setPos(this.getX(), topY + GROUND_EPS, this.getZ());
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.0, 0.6));
            }
        }

        // 💨 Explosion du gaz après un délai
        if (!level.isClientSide && ticksSinceLaunch >= ModConfigs.GAS.GAS_EXPLODE_AFTER_TICKS.get() && isGrounded()) {
            explodeGas();
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

        // 🔩 Rebond identique au Timer BIM
        double restitutionGround = ModConfigs.TIMER.RESTITUTION_GROUND.get();
        double frictionGround = ModConfigs.TIMER.FRICTION_GROUND.get();
        double restitutionWall = ModConfigs.TIMER.RESTITUTION_WALL.get();
        double frictionWall = ModConfigs.TIMER.FRICTION_WALL.get();
        double maxBounceUp = ModConfigs.TIMER.MAX_BOUNCE_UP.get();
        double stopEps = ModConfigs.TIMER.STOP_EPS.get();

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
                    this.fallDistance = 0.0F;
                    lastGroundHitTime = level.getGameTime();
                    break;
                }

                this.setDeltaMovement(newVx, newVy, newVz);
                this.fallDistance = 0.0F;
                lastGroundHitTime = level.getGameTime();
                SoundUtils.playRebound(level, getX(), getY(), getZ());
                break;
            }
            case NORTH:
            case SOUTH: {
                this.setDeltaMovement(v.x * frictionWall, Math.abs(v.y) * 0.25, -v.z * restitutionWall);
                SoundUtils.playRebound(level, getX(), getY(), getZ());
                break;
            }
            case EAST:
            case WEST: {
                this.setDeltaMovement(-v.x * restitutionWall, Math.abs(v.y) * 0.25, v.z * frictionWall);
                SoundUtils.playRebound(level, getX(), getY(), getZ());
                break;
            }
        }
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult hit) {
        Entity target = hit.getEntity();
        if (!level.isClientSide && target instanceof LivingEntity) {
            float dmg = (float) (ModConfigs.GAS.GAS_IMPACT_HEARTS.get() * 2.0);
            target.hurt(new IndirectEntityDamageSource("gas_bim", this, this.getOwner()).setProjectile(), dmg);
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

    private void explodeGas() {
        if (level.isClientSide) return;

        fr.nokane.btoommods.entity.misc.GasCloudFieldEntity field =
                ModEntities.GAS_CLOUD_FIELD.get().create(level);
        if (field != null) {
            field.setPos(this.getX(), this.getY() + 0.05, this.getZ());
            level.addFreshEntity(field);
        }
        this.remove();
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
