package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.Direction;
import net.minecraft.util.IndirectEntityDamageSource;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class GasBimEntity extends ProjectileItemEntity {

    private int ticksFromLaunch = 0;
    private long lastGroundHitGameTime = -1;
    private static final double GROUND_EPS = 0.02;

    public GasBimEntity(EntityType<? extends GasBimEntity> type, World level) {
        super(type, level);
    }

    public GasBimEntity(EntityType<? extends GasBimEntity> type, World level, LivingEntity owner) {
        super(type, owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.GAS_BIM.get();
    }

    @Override
    public void tick() {
        super.tick();
        ticksFromLaunch++;

        if (!level.isClientSide) {
            // Explosion automatique si le gaz reste au sol trop longtemps
            if (ticksFromLaunch >= ModConfigs.GAS.GAS_EXPLODE_AFTER_TICKS.get() && isGrounded()) {
                explodeGas();
                return;
            }
        }

        // Anti-enfoncement dans les dalles/blocs minces
        BlockPos pos = this.blockPosition();
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (!shape.isEmpty()) {
            double topY = pos.getY() + shape.max(Direction.Axis.Y);
            if (this.getY() < topY + GROUND_EPS) {
                this.setPos(this.getX(), topY + GROUND_EPS, this.getZ());
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.0, 0.6));
            }
        }
    }

    @Override
    protected void onHit(RayTraceResult hit) {
        if (hit.getType() == RayTraceResult.Type.ENTITY) {
            this.onHitEntity((EntityRayTraceResult) hit);
            return;
        }

        if (hit.getType() != RayTraceResult.Type.BLOCK) return;

        BlockRayTraceResult br = (BlockRayTraceResult) hit;
        Direction face = br.getDirection();
        BlockPos bpos = br.getBlockPos();
        Vector3d loc = br.getLocation();

        // Paramètres physiques depuis config
        double restitutionGround = ModConfigs.GAS.RESTITUTION_GROUND.get();
        double frictionGround = ModConfigs.GAS.FRICTION_GROUND.get();
        double restitutionWall = ModConfigs.GAS.RESTITUTION_WALL.get();
        double frictionWall = ModConfigs.GAS.FRICTION_WALL.get();
        double maxBounceUp = ModConfigs.GAS.MAX_BOUNCE_UP.get();
        double stopEps = ModConfigs.GAS.STOP_EPS.get();

        double wallVerticalPop = 0.04;
        double popSpeedGate = 0.25;

        switch (face) {
            case UP: {
                double topY = topYOf(bpos);
                this.setPos(loc.x, Math.max(loc.y, topY) + GROUND_EPS, loc.z);

                Vector3d v = this.getDeltaMovement();
                double newVx = v.x * frictionGround;
                double newVz = v.z * frictionGround;
                double newVy = Math.abs(v.y) * restitutionGround;
                newVy = Math.max(newVy, 0.02);
                newVy = Math.min(newVy, maxBounceUp);

                double horiz = Math.hypot(newVx, newVz);
                if (newVy < stopEps && horiz < 0.05) {
                    this.setDeltaMovement(0.0, 0.0, 0.0);
                    this.fallDistance = 0.0F;
                    lastGroundHitGameTime = level.getGameTime();
                    break;
                }

                this.setDeltaMovement(newVx, newVy, newVz);
                this.fallDistance = 0.0F;
                lastGroundHitGameTime = level.getGameTime();

                // 🔊 Son de rebond
                SoundUtils.playRebound(level, getX(), getY(), getZ());
                break;
            }
            case NORTH:
            case SOUTH: {
                Vector3d v = this.getDeltaMovement();
                double addPop = (v.length() > popSpeedGate) ? wallVerticalPop : 0.0;
                double newVx = v.x * frictionWall;
                double newVy = Math.max(v.y * 0.25, 0.0) + addPop;
                newVy = Math.min(newVy, maxBounceUp * 0.6);
                double newVz = -v.z * restitutionWall;
                this.setDeltaMovement(newVx, newVy, newVz);

                SoundUtils.playRebound(level, getX(), getY(), getZ());
                break;
            }
            case EAST:
            case WEST: {
                Vector3d v = this.getDeltaMovement();
                double addPop = (v.length() > popSpeedGate) ? wallVerticalPop : 0.0;
                double newVx = -v.x * restitutionWall;
                double newVy = Math.max(v.y * 0.25, 0.0) + addPop;
                newVy = Math.min(newVy, maxBounceUp * 0.6);
                double newVz = v.z * frictionWall;
                this.setDeltaMovement(newVx, newVy, newVz);

                SoundUtils.playRebound(level, getX(), getY(), getZ());
                break;
            }
        }
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult hit) {
        Entity target = hit.getEntity();
        if (!level.isClientSide && target instanceof LivingEntity) {
            float dmgHearts = ModConfigs.GAS.GAS_IMPACT_HEARTS.get().floatValue();
            float dmgHP = dmgHearts * 2.0F;
            target.hurt(new IndirectEntityDamageSource("gas_bim", this, this.getOwner()), dmgHP);

            Vector3d v = this.getDeltaMovement();
            double restitution = ModConfigs.GAS.RESTITUTION_WALL.get();
            double maxBounce = ModConfigs.GAS.MAX_BOUNCE_UP.get();

            Vector3d rebound = new Vector3d(
                    -v.x * restitution,
                    Math.min(Math.abs(v.y) * 0.4 + 0.08, maxBounce),
                    -v.z * restitution
            );

            this.setDeltaMovement(rebound);
            this.hasImpulse = true;

            SoundUtils.playRebound(level, getX(), getY(), getZ());
        }
    }

    private boolean isGrounded() {
        long now = level.getGameTime();
        if (now - lastGroundHitGameTime <= 2) return true;
        BlockPos below = this.blockPosition().below();
        double topY = topYOf(below);
        boolean nearSurface = (this.getY() - topY) <= 0.06;
        boolean slowY = Math.abs(this.getDeltaMovement().y) < 0.08;
        return nearSurface && slowY;
    }

    private double topYOf(BlockPos pos) {
        VoxelShape s = level.getBlockState(pos).getCollisionShape(level, pos);
        double add = s.isEmpty() ? 1.0 : s.max(Direction.Axis.Y);
        return pos.getY() + add;
    }

    private void explodeGas() {
        if (level.isClientSide) return;

        BlockPos below = this.blockPosition().below();
        double y = Math.max(this.getY(), topYOf(below)) + 0.01;

        fr.nokane.btoommods.entity.misc.GasCloudFieldEntity field =
                ModEntities.GAS_CLOUD_FIELD.get().create(level);
        if (field != null) {
            field.setPos(this.getX(), y, this.getZ());
            level.addFreshEntity(field);
        }

        this.remove();
    }

    @Override
    public net.minecraft.network.IPacket<?> getAddEntityPacket() {
        return net.minecraftforge.fml.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
