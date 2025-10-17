package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
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
    private int fuseTicks;

    public GasBimEntity(EntityType<? extends GasBimEntity> type, World world) {
        super(type, world);
        this.fuseTicks = Math.max(1, ModConfigs.GAS.GAS_EXPLODE_AFTER_TICKS.get());
    }

    public GasBimEntity(World world, LivingEntity owner) {
        super(ModEntities.GAS_BIM.get(), owner, world);
        this.fuseTicks = Math.max(1, ModConfigs.GAS.GAS_EXPLODE_AFTER_TICKS.get());
    }

    @Override
    protected Item getDefaultItem() { return ModItems.GAS_BIM.get(); }

    @Override
    public void tick() {
        super.tick();

        // Gravité
        if (!this.isNoGravity()) {
            Vector3d m = this.getDeltaMovement();
            double weight = Math.max(0.1, ModConfigs.GAS.POIDS_PROJECTILE.get());
            this.setDeltaMovement(m.x, m.y - 0.04D * weight, m.z);
        }

        // Anti-enfoncement simple
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

        // Fuse → explosion
        if (fuseTicks > 0) {
            fuseTicks--;
            if (fuseTicks == 0) {
                explodeGas();
                return;
            }
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

        double restitutionGround = ModConfigs.GAS.RESTITUTION_GROUND.get();
        double frictionGround    = ModConfigs.GAS.FRICTION_GROUND.get();
        double restitutionWall   = ModConfigs.GAS.RESTITUTION_WALL.get();
        double frictionWall      = ModConfigs.GAS.FRICTION_WALL.get();
        double maxBounceUp       = ModConfigs.GAS.MAX_BOUNCE_UP.get();
        double stopEps           = ModConfigs.GAS.STOP_EPS.get();

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
                if (this.getDeltaMovement().lengthSqr() > 0.04) SoundUtils.playRebound(level, getX(), getY(), getZ());
                break;
            }
            case NORTH: case SOUTH: case EAST: case WEST: {
                double pop = 0.05;
                Vector3d newV = new Vector3d(
                        face.getAxis() == Direction.Axis.X ? -v.x * restitutionWall : v.x * frictionWall,
                        Math.min(Math.max(v.y * 0.25, 0.0) + pop, maxBounceUp * 0.6),
                        face.getAxis() == Direction.Axis.Z ? -v.z * restitutionWall : v.z * frictionWall
                );
                this.setDeltaMovement(newV);
                if (this.getDeltaMovement().lengthSqr() > 0.04) SoundUtils.playRebound(level, getX(), getY(), getZ());
                break;
            }
        }
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult hit) {
        if (level.isClientSide) return;
        if (hit.getEntity() instanceof LivingEntity) {
            float dmg = (float) (ModConfigs.GAS.GAS_IMPACT_HEARTS.get() * 2.0);
            hit.getEntity().hurt(new IndirectEntityDamageSource("gas_bim", this, this.getOwner()).setProjectile(), dmg);

            Vector3d v = this.getDeltaMovement();
            this.setDeltaMovement(-v.x * 0.3, 0.1, -v.z * 0.3);
            SoundUtils.playRebound(level, getX(), getY(), getZ());
        }
    }

    private void explodeGas() {
        if (level.isClientSide) return;

        fr.nokane.btoommods.entity.misc.GasCloudFieldEntity field =
                ModEntities.GAS_CLOUD_FIELD.get().create(level);
        if (field == null) return;

        // Position exacte de l'explosion (épicentre)
        final double x = this.getX();
        final double y = this.getY();
        final double z = this.getZ();

        // Heuristique simple : si posé / quasi immobile verticalement → sol, sinon air
        boolean airborne = !(this.isOnGround() || Math.abs(this.getDeltaMovement().y) < 0.01);

        // *** IMPORTANT : setter AVANT addFreshEntity (pour que le client reçoive les DataParameters dès le spawn)
        field.setAirborne(airborne);
        field.setExplosionOrigin(y);
        field.setPos(x, y, z);

        level.addFreshEntity(field);
        SoundUtils.playGas();
        this.remove();
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
