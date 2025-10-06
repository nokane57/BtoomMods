package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.item.TimerBimItem;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.Direction;
import net.minecraft.util.IndirectEntityDamageSource;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class TimerBimProjectileEntity extends ProjectileItemEntity {

    private static final DataParameter<Boolean> DATA_ACTIVE =
            EntityDataManager.defineId(TimerBimProjectileEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DATA_STARTED =
            EntityDataManager.defineId(TimerBimProjectileEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> DATA_REMAINING =
            EntityDataManager.defineId(TimerBimProjectileEntity.class, DataSerializers.INT);

    private static final double GROUND_EPS = 0.02;

    private boolean hadFirstBounce = false;
    private int pickupDelay = 20;
    private long lastGroundHitTime = -1;
    private boolean explodedClientSide = false;

    public TimerBimProjectileEntity(EntityType<? extends TimerBimProjectileEntity> type, World world) {
        super(type, world);
    }

    public TimerBimProjectileEntity(World world, LivingEntity owner) {
        super(ModEntities.TIMER_BIM_PROJECTILE.get(), owner, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ACTIVE, false);
        this.entityData.define(DATA_STARTED, false);
        this.entityData.define(DATA_REMAINING, TimerBimItem.getMaxTicks());
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.TIMER_BIM.get();
    }

    public void setTimerState(boolean active, boolean hasStarted, int remaining) {
        this.entityData.set(DATA_ACTIVE, active);
        this.entityData.set(DATA_STARTED, hasStarted);
        this.entityData.set(DATA_REMAINING, hasStarted ? Math.max(0, remaining) : TimerBimItem.getMaxTicks());
    }

    @Override
    public void tick() {
        super.tick();

        // Gravité
        if (!this.isNoGravity()) {
            Vector3d m = this.getDeltaMovement();
            this.setDeltaMovement(m.x, m.y - ModConfigs.TIMER.POIDS_PROJECTILE.get(), m.z);
        }

        // Correction au sol
        BlockPos pos = this.blockPosition();
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (!shape.isEmpty()) {
            double topY = pos.getY() + shape.max(Direction.Axis.Y);
            if (this.getY() < topY + GROUND_EPS) {
                this.setPos(this.getX(), topY + GROUND_EPS, this.getZ());
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.0, 0.6));
            }
        }

        // Gestion du timer
        if (!level.isClientSide && isActive() && getRemainingTicks() > 0) {
            int remaining = getRemainingTicks() - 1;
            this.entityData.set(DATA_REMAINING, remaining);

            if (remaining <= 0) {
                TimerBimItem.explodeAndConsume(level, getX(), getY(), getZ());
                this.entityData.set(DATA_REMAINING, 0);
                this.entityData.set(DATA_ACTIVE, false);
                this.remove();
                return;
            }
        }

        // Client : détecte explosion pour le HUD
        if (level.isClientSide && !explodedClientSide && getRemainingTicks() <= 0) {
            explodedClientSide = true;
        }

        if (pickupDelay > 0) pickupDelay--;
    }

    public boolean hasExplodedClientSide() {
        return explodedClientSide || getRemainingTicks() <= 0 || !isAlive();
    }

    @Override
    protected void onHit(RayTraceResult hit) {
        if (hit.getType() == RayTraceResult.Type.ENTITY) {
            this.onHitEntity((EntityRayTraceResult) hit);
            return;
        }
        if (hit.getType() != RayTraceResult.Type.BLOCK || level.isClientSide) return;

        BlockRayTraceResult br = (BlockRayTraceResult) hit;
        Direction face = br.getDirection();
        BlockPos bpos = br.getBlockPos();
        Vector3d loc = br.getLocation();

        Vector3d v = this.getDeltaMovement();
        double restitutionGround = ModConfigs.TIMER.RESTITUTION_GROUND.get();
        double frictionGround = ModConfigs.TIMER.FRICTION_GROUND.get();
        double restitutionWall = 0.45;
        double frictionWall = 0.75;
        double maxBounceUp = 0.3;
        double stopEps = 0.04;
        double popSpeedGate = 0.25;
        double wallVerticalPop = 0.05;

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
            case SOUTH:
            case EAST:
            case WEST: {
                double addPop = (v.length() > popSpeedGate) ? wallVerticalPop : 0.0;
                Vector3d newV = new Vector3d(
                        face.getAxis() == Direction.Axis.X ? -v.x * restitutionWall : v.x * frictionWall,
                        Math.min(Math.max(v.y * 0.25, 0.0) + addPop, maxBounceUp * 0.6),
                        face.getAxis() == Direction.Axis.Z ? -v.z * restitutionWall : v.z * frictionWall
                );
                this.setDeltaMovement(newV);
                SoundUtils.playRebound(level, getX(), getY(), getZ());
                break;
            }
        }

        if (!hadFirstBounce) {
            hadFirstBounce = true;
            pickupDelay = 20;
        }
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult hit) {
        Entity target = hit.getEntity();
        if (!level.isClientSide && target instanceof LivingEntity) {
            float dmg = (float) (ModConfigs.TIMER.IMPACT_HEARTS.get() * 2.0);
            target.hurt(new IndirectEntityDamageSource("timer_bim", this, this.getOwner()).setProjectile(), dmg);
            SoundUtils.playRebound(level, getX(), getY(), getZ());
        }
    }

    @Override
    public void playerTouch(PlayerEntity player) {
        if (level.isClientSide) return;
        if (!hadFirstBounce || pickupDelay > 0 || !isGrounded()) return;

        ItemStack stack = new ItemStack(ModItems.TIMER_BIM.get());
        CompoundNBT tag = stack.getOrCreateTag();
        tag.putBoolean(TimerBimItem.NBT_ACTIVE, this.isActive());
        tag.putBoolean(TimerBimItem.NBT_HAS_STARTED, this.hasStarted());
        tag.putInt(TimerBimItem.NBT_REMAINING, Math.max(0, this.getRemainingTicks()));
        stack.setTag(tag);

        if (player.addItem(stack)) this.remove();
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

    public boolean isActive() { return this.entityData.get(DATA_ACTIVE); }
    public boolean hasStarted() { return this.entityData.get(DATA_STARTED); }
    public int getRemainingTicks() { return this.entityData.get(DATA_REMAINING); }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
