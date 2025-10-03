package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.item.TimerBimItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class TimerBimProjectileEntity extends ProjectileItemEntity {

    // ---- Synced data ----
    private static final DataParameter<Boolean> DATA_ACTIVE =
            EntityDataManager.defineId(TimerBimProjectileEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DATA_STARTED =
            EntityDataManager.defineId(TimerBimProjectileEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> DATA_REMAINING =
            EntityDataManager.defineId(TimerBimProjectileEntity.class, DataSerializers.INT);

    // ---- Constantes fixes ----
    private static final double GROUND_EPS = 0.02;
    private static final double MIN_BOUNCE_UP = 0.01;
    private static final double WALL_VERTICAL_POP = 0.02;
    private static final double ENTITY_VERTICAL_POP = 0.02;
    private static final double POP_SPEED_GATE = 0.3;

    // ---- Locaux ----
    private boolean hadFirstBounce = false;
    private int pickupDelay = 20; // ticks avant ramassage

    public TimerBimProjectileEntity(EntityType<? extends TimerBimProjectileEntity> type, World world) {
        super(type, world);
        this.noPhysics = false;
    }

    public TimerBimProjectileEntity(World world, LivingEntity owner) {
        super(ModEntities.TIMER_BIM_PROJECTILE.get(), owner, world);
        this.noPhysics = false;
    }

    // ---- NBT initial pour le timer ----
    public void setTimerState(boolean active, boolean hasStarted, int remaining) {
        this.entityData.set(DATA_ACTIVE, active);
        this.entityData.set(DATA_STARTED, hasStarted);
        this.entityData.set(DATA_REMAINING, hasStarted ? Math.max(0, remaining) : TimerBimItem.getMaxTicks());
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.TIMER_BIM.get();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ACTIVE, false);
        this.entityData.define(DATA_STARTED, false);
        this.entityData.define(DATA_REMAINING, TimerBimItem.getMaxTicks());
    }

    // ---- Tick principal ----
    @Override
    public void tick() {
        // 1) Raytrace manuel
        RayTraceResult hitResult = ProjectileHelper.getHitResult(this, this::canHitEntity);
        if (hitResult.getType() != RayTraceResult.Type.MISS) {
            this.onHit(hitResult);
        }

        // 2) Tick vanilla
        super.tick();

        // 3) Gravité
        if (!this.isNoGravity()) {
            Vector3d m = this.getDeltaMovement();
            this.setDeltaMovement(m.x, m.y - 0.04, m.z);
        }

        // 4) Anti-clip
        BlockPos pos = this.blockPosition();
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (!shape.isEmpty()) {
            double topY = pos.getY() + shape.max(Direction.Axis.Y);
            if (this.getY() < topY + GROUND_EPS) {
                this.setPos(this.getX(), topY + GROUND_EPS, this.getZ());
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.0, 0.6));
            }
        }

        // 5) Timer
        if (!level.isClientSide && isActive() && getRemainingTicks() > 0) {
            int remaining = getRemainingTicks() - 1;
            this.entityData.set(DATA_REMAINING, remaining);
            if (remaining <= 0) {
                explodeWithDamage();
                this.remove();
                return;
            }
        }

        // 6) Pickup delay
        if (pickupDelay > 0) pickupDelay--;

        // 7) Air friction
        Vector3d motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.scale(0.98));
    }

    // ---- Collision générique ----
    @Override
    protected void onHit(RayTraceResult hit) {
        if (hit.getType() == RayTraceResult.Type.ENTITY) {
            this.onHitEntity((EntityRayTraceResult) hit);
            return;
        }
        if (level.isClientSide) return;
        if (hit.getType() != RayTraceResult.Type.BLOCK) return;

        // Gestion des blocs
        BlockRayTraceResult br = (BlockRayTraceResult) hit;
        Direction face = br.getDirection();
        BlockPos bpos = br.getBlockPos();
        Vector3d loc = br.getLocation();

        double restitutionGround = ModConfigs.COMMON.TIMER_RESTITUTION_GROUND.get();
        double frictionGround = ModConfigs.COMMON.TIMER_FRICTION_GROUND.get();
        double restitutionWall = ModConfigs.COMMON.TIMER_RESTITUTION_WALL.get();
        double frictionWall = ModConfigs.COMMON.TIMER_FRICTION_WALL.get();
        double maxBounceUp = ModConfigs.COMMON.TIMER_MAX_BOUNCE_UP.get();
        double stopEps = ModConfigs.COMMON.TIMER_STOP_EPS.get();

        switch (face) {
            case UP: {
                double topY = topYOf(bpos);
                this.setPos(loc.x, Math.max(loc.y, topY) + GROUND_EPS, loc.z);

                Vector3d v = this.getDeltaMovement();
                double newVx = v.x * frictionGround;
                double newVz = v.z * frictionGround;

                double newVy = Math.abs(v.y) * restitutionGround;
                newVy = Math.max(newVy, MIN_BOUNCE_UP);
                newVy = Math.min(newVy, maxBounceUp);

                double horiz = Math.hypot(newVx, newVz);
                if (newVy < stopEps && horiz < 0.05) {
                    this.setDeltaMovement(0.0, 0.0, 0.0);
                    this.fallDistance = 0.0F;
                    break;
                }

                this.setDeltaMovement(newVx, newVy, newVz);
                this.fallDistance = 0.0F;
                break;
            }
            case NORTH:
            case SOUTH: {
                Vector3d v = this.getDeltaMovement();
                double addPop = (v.length() > POP_SPEED_GATE) ? WALL_VERTICAL_POP : 0.0;
                double newVx = v.x * frictionWall;
                double newVy = Math.max(v.y * 0.25, 0.0) + addPop;
                newVy = Math.min(newVy, maxBounceUp * 0.6);
                double newVz = -v.z * restitutionWall;
                this.setDeltaMovement(newVx, newVy, newVz);
                break;
            }
            case EAST:
            case WEST: {
                Vector3d v = this.getDeltaMovement();
                double addPop = (v.length() > POP_SPEED_GATE) ? WALL_VERTICAL_POP : 0.0;
                double newVx = -v.x * restitutionWall;
                double newVy = Math.max(v.y * 0.25, 0.0) + addPop;
                newVy = Math.min(newVy, maxBounceUp * 0.6);
                double newVz = v.z * frictionWall;
                this.setDeltaMovement(newVx, newVy, newVz);
                break;
            }
        }

        if (!hadFirstBounce) {
            hadFirstBounce = true;
            pickupDelay = 20;
        }
    }

    // ---- Collision entités ----
    @Override
    protected void onHitEntity(EntityRayTraceResult hit) {
        Entity target = hit.getEntity();
        if (!level.isClientSide && target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) target;

            // ✅ dégâts configurables à l’impact
            float dmg = (float)(ModConfigs.COMMON.TIMER_IMPACT_HEARTS.get() * 2.0);
            target.hurt(new IndirectEntityDamageSource("timer_bim", this, this.getOwner()).setProjectile(), dmg);

            // ✅ rebond avec configs TIMER
            Vector3d v = this.getDeltaMovement();
            double restitution = ModConfigs.COMMON.TIMER_RESTITUTION_WALL.get();
            double maxBounce   = ModConfigs.COMMON.TIMER_MAX_BOUNCE_UP.get();

            Vector3d rebound = new Vector3d(
                    -v.x * restitution,
                    Math.min(Math.abs(v.y) * 0.4 + ENTITY_VERTICAL_POP, maxBounce),
                    -v.z * restitution
            );

            this.setDeltaMovement(rebound);
            this.hasImpulse = true;

            level.playSound(null, this.blockPosition(),
                    SoundEvents.SLIME_BLOCK_HIT, SoundCategory.PLAYERS, 0.6F, 1.0F);

            if (!hadFirstBounce) {
                hadFirstBounce = true;
                pickupDelay = 20;
            }
        }
    }

    private double topYOf(BlockPos pos) {
        VoxelShape s = level.getBlockState(pos).getCollisionShape(level, pos);
        double add = s.isEmpty() ? 1.0 : s.max(Direction.Axis.Y);
        return pos.getY() + add;
    }

    private void explodeWithDamage() {
        TimerBimItem.explodeAndConsume(level, getX(), getY(), getZ());

        double radius = 7.0;
        level.getEntities(this, this.getBoundingBox().inflate(radius)).forEach(e -> {
            double dist = this.distanceTo(e);
            if (dist <= radius) {
                double factor = 1.0 - (dist / radius);
                float damage = (float) (18.0 * factor);
                if (damage > 0) {
                    e.hurt(DamageSource.explosion((Explosion) null), damage);
                }
            }
        });
    }

    // ---- Pickup ----
    @Override
    public void playerTouch(PlayerEntity player) {
        if (level.isClientSide) return;
        if (!hadFirstBounce || pickupDelay > 0) return;

        ItemStack stack = this.getItem();
        stack = stack.isEmpty() ? new ItemStack(ModItems.TIMER_BIM.get()) : stack.copy();

        CompoundNBT tag = stack.getOrCreateTag();
        tag.putBoolean(TimerBimItem.NBT_HAS_STARTED, this.hasStarted());
        tag.putBoolean(TimerBimItem.NBT_ACTIVE, this.isActive());
        tag.putInt(TimerBimItem.NBT_REMAINING, Math.max(0, this.getRemainingTicks()));
        stack.setTag(tag);

        if (player.addItem(stack)) {
            this.remove();
        }
    }

    // ---- Save/Load ----
    @Override
    public void readAdditionalSaveData(CompoundNBT tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_ACTIVE, tag.getBoolean("Active"));
        this.entityData.set(DATA_STARTED, tag.getBoolean("HasStarted"));
        this.entityData.set(DATA_REMAINING, tag.getInt("Remaining"));
        this.hadFirstBounce = tag.getBoolean("FirstBounce");
        this.pickupDelay = tag.getInt("PickupDelay");
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Active", this.isActive());
        tag.putBoolean("HasStarted", this.hasStarted());
        tag.putInt("Remaining", this.getRemainingTicks());
        tag.putBoolean("FirstBounce", this.hadFirstBounce);
        tag.putInt("PickupDelay", this.pickupDelay);
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // ---- Getters ----
    public boolean isActive() { return this.entityData.get(DATA_ACTIVE); }
    public boolean hasStarted() { return this.entityData.get(DATA_STARTED); }
    public int getRemainingTicks() { return this.entityData.get(DATA_REMAINING); }
}
