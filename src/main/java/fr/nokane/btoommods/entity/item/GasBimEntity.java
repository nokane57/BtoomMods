package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.Direction;
import net.minecraft.util.IndirectEntityDamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class GasBimEntity extends ProjectileItemEntity {
    private int  ticksFromLaunch = 0;
    private long lastGroundHitGameTime = -1;

    private static final double GROUND_EPS          = 0.02;
    private static final double RESTITUTION_GROUND  = 0.35;
    private static final double FRICTION_GROUND     = 0.55;
    private static final double RESTITUTION_WALL    = 0.35;
    private static final double FRICTION_WALL       = 0.75;
    private static final double MIN_BOUNCE_UP       = 0.02;
    private static final double MAX_BOUNCE_UP       = 0.18;
    private static final double STOP_EPS            = 0.04;

    private static final double WALL_VERTICAL_POP   = 0.04;
    private static final double ENTITY_VERTICAL_POP = 0.03;
    private static final double POP_SPEED_GATE      = 0.25;

    public GasBimEntity(EntityType<? extends GasBimEntity> type, World level) { super(type, level); }
    public GasBimEntity(EntityType<? extends GasBimEntity> type, World level, LivingEntity owner) { super(type, owner, level); }

    @Override protected Item getDefaultItem() { return ModItems.GAS_BIM.get(); }

    @Override
    public void tick() {
        super.tick();
        ticksFromLaunch++;

        if (!level.isClientSide) {
            if (ticksFromLaunch >= ModConfigs.COMMON.GAS_EXPLODE_AFTER_TICKS.get() && isGrounded()) {
                explodeGas();
                return;
            }
        }

        // anti clip dans les collision shapes (dalles etc.)
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
        if (hit.getType() != RayTraceResult.Type.BLOCK) return;

        BlockRayTraceResult br = (BlockRayTraceResult) hit;
        Direction face = br.getDirection();
        BlockPos bpos = br.getBlockPos();
        Vector3d loc = br.getLocation();

        switch (face) {
            case UP: {
                double topY = topYOf(bpos);
                this.setPos(loc.x, Math.max(loc.y, topY) + GROUND_EPS, loc.z);

                Vector3d v = this.getDeltaMovement();
                double newVx = v.x * FRICTION_GROUND;
                double newVz = v.z * FRICTION_GROUND;

                double newVy = Math.abs(v.y) * RESTITUTION_GROUND;
                newVy = Math.max(newVy, MIN_BOUNCE_UP);
                newVy = Math.min(newVy, MAX_BOUNCE_UP);

                double horiz = Math.hypot(newVx, newVz);
                if (newVy < STOP_EPS && horiz < 0.05) {
                    this.setDeltaMovement(0.0, 0.0, 0.0);
                    this.fallDistance = 0.0F;
                    lastGroundHitGameTime = level.getGameTime();
                    break;
                }

                this.setDeltaMovement(newVx, newVy, newVz);
                this.fallDistance = 0.0F;
                lastGroundHitGameTime = level.getGameTime();
                level.playSound(null, this.blockPosition(),
                        SoundEvents.SLIME_BLOCK_STEP, SoundCategory.PLAYERS, 0.35F, 1.10F);
                break;
            }
            case DOWN: {
                Vector3d v = this.getDeltaMovement();
                this.setDeltaMovement(v.x * 0.7, -Math.abs(v.y) * 0.3, v.z * 0.7);
                break;
            }
            case NORTH:
            case SOUTH: {
                Vector3d v = this.getDeltaMovement();
                double addPop = (v.length() > POP_SPEED_GATE) ? WALL_VERTICAL_POP : 0.0;
                double newVx = v.x * FRICTION_WALL;
                double newVy = Math.max(v.y * 0.25, 0.0) + addPop;
                newVy = Math.min(newVy, MAX_BOUNCE_UP * 0.6);
                double newVz = -v.z * RESTITUTION_WALL;
                this.setDeltaMovement(newVx, newVy, newVz);
                level.playSound(null, this.blockPosition(),
                        SoundEvents.SLIME_BLOCK_STEP, SoundCategory.PLAYERS, 0.30F, 1.05F);
                break;
            }
            case EAST:
            case WEST: {
                Vector3d v = this.getDeltaMovement();
                double addPop = (v.length() > POP_SPEED_GATE) ? WALL_VERTICAL_POP : 0.0;
                double newVx = -v.x * RESTITUTION_WALL;
                double newVy = Math.max(v.y * 0.25, 0.0) + addPop;
                newVy = Math.min(newVy, MAX_BOUNCE_UP * 0.6);
                double newVz = v.z * FRICTION_WALL;
                this.setDeltaMovement(newVx, newVy, newVz);
                level.playSound(null, this.blockPosition(),
                        SoundEvents.SLIME_BLOCK_STEP, SoundCategory.PLAYERS, 0.30F, 1.05F);
                break;
            }
        }
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult hit) {
        // rebond
        Vector3d v = this.getDeltaMovement();
        double addPop = (v.length() > POP_SPEED_GATE) ? ENTITY_VERTICAL_POP : 0.0;
        this.setDeltaMovement(v.x * 0.6, Math.max(v.y * 0.2, 0.0) + addPop, v.z * 0.6);
        level.playSound(null, this.blockPosition(),
                SoundEvents.SLIME_BLOCK_STEP, SoundCategory.PLAYERS, 0.30F, 1.05F);

        // dégâts d'impact (serveur)
        if (!level.isClientSide && hit.getEntity() instanceof LivingEntity) {
            float hearts = ModConfigs.COMMON.GAS_IMPACT_HEARTS.get().floatValue();
            if (hearts > 0f) {
                LivingEntity tgt = (LivingEntity) hit.getEntity();
                // source indirecte pour attribuer au tireur si présent
                net.minecraft.util.DamageSource src =
                        (this.getOwner() != null)
                                ? new IndirectEntityDamageSource("gas_bim_hit", this, this.getOwner()).setProjectile()
                                : new net.minecraft.util.DamageSource("gas_bim_hit").setProjectile();
                tgt.hurt(src, hearts * 2.0F);
            }
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
