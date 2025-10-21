package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.item.TimerBimItem;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.List;

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

        // ✅ Gravité depuis ta config FR
        if (!this.isNoGravity()) {
            Vector3d motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, motion.y - ModConfigs.TIMER.POIDS_PROJECTILE.get(), motion.z);
        }

        // Correction surface / sol
        BlockPos pos = this.blockPosition();
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (!shape.isEmpty()) {
            double topY = pos.getY() + shape.max(Direction.Axis.Y);
            if (this.getY() < topY + GROUND_EPS) {
                this.setPos(this.getX(), topY + GROUND_EPS, this.getZ());
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.0, 0.6));
            }
        }

        // Décompte serveur
        if (!level.isClientSide && isActive() && getRemainingTicks() > 0) {
            int remaining = getRemainingTicks() - 1;
            this.entityData.set(DATA_REMAINING, remaining);

            if (remaining <= 0) {
                safeExplosionWorld(level, getX(), getY(), getZ());
                this.entityData.set(DATA_REMAINING, 0);
                this.entityData.set(DATA_ACTIVE, false);
                this.remove();
                return;
            }
        }

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
        double restitutionWall = ModConfigs.TIMER.RESTITUTION_WALL.get();
        double frictionWall = ModConfigs.TIMER.FRICTION_WALL.get();
        double maxBounceUp = ModConfigs.TIMER.MAX_BOUNCE_UP.get();
        double stopEps = ModConfigs.TIMER.STOP_EPS.get();
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

                if (this.getDeltaMovement().lengthSqr() > 0.04) {
                    SoundUtils.playRebound(level, getX(), getY(), getZ());
                }
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

                if (this.getDeltaMovement().lengthSqr() > 0.04) {
                    SoundUtils.playRebound(level, getX(), getY(), getZ());
                }
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

        if (!level.isClientSide) {
            // ✅ utilise le champ IMPACT_HEARTS (dégâts d’impact direct)
            if (target instanceof LivingEntity) {
                float dmg = (float) (double) ModConfigs.TIMER.IMPACT_HEARTS.get();
                target.hurt(new IndirectEntityDamageSource("timer_bim", this, this.getOwner()).setProjectile(), dmg);
            }

            double restitution = ModConfigs.TIMER.RESTITUTION_ENTITY.get();
            Vector3d motion = this.getDeltaMovement();
            Vector3d normal = this.position().subtract(target.position()).normalize();
            double dot = motion.dot(normal);
            Vector3d reflected = motion.subtract(normal.scale(2 * dot)).scale(restitution);
            this.setDeltaMovement(reflected);
            this.hasImpulse = true;
            this.yRot += (this.random.nextFloat() - 0.5f) * 20f;

            if (this.getDeltaMovement().lengthSqr() > 0.04) {
                SoundUtils.playRebound(level, getX(), getY(), getZ());
            }
        }
    }

    @Override
    public void playerTouch(PlayerEntity player) {
        if (level.isClientSide) return;
        if (!hadFirstBounce || pickupDelay > 0 || !isGrounded()) return;

        ItemStack stack = new ItemStack(ModItems.TIMER_BIM.get());
        CompoundNBT tag = stack.getOrCreateTag();
        tag.putBoolean(TimerBimItem.NBT_ACTIVE, this.isActive());
        tag.putBoolean(TimerBimItem.NBT_HAS_STARTED, true);
        tag.putInt(TimerBimItem.NBT_REMAINING, Math.max(0, this.getRemainingTicks()));
        stack.setTag(tag);

        if (player.addItem(stack)) {
            this.remove();
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

    public boolean isActive() { return this.entityData.get(DATA_ACTIVE); }
    public boolean hasStarted() { return this.entityData.get(DATA_STARTED); }
    public int getRemainingTicks() { return this.entityData.get(DATA_REMAINING); }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    /** Explosion monde standard + dégâts custom. */
    public static void safeExplosionWorld(World world, double x, double y, double z) {
        if (!(world instanceof ServerWorld)) return;
        ServerWorld sw = (ServerWorld) world;

        boolean breakBlocks = ModConfigs.TIMER.BREAK_BLOCKS.get();
        boolean fire = ModConfigs.TIMER.CAUSES_FIRE.get();
        boolean noItemDestroy = ModConfigs.TIMER.NO_ITEM_DESTROY.get();

        double dmgEpic = ModConfigs.TIMER.MAX_DAMAGE_AT_EPICENTER.get();
        double dmgOuter = ModConfigs.TIMER.INVENTORY_EXPLOSION_DAMAGE.get();
        double radius = ModConfigs.TIMER.EXPLOSION_RADIUS.get();
        double visualRad = ModConfigs.TIMER.EXPLOSION_VISUAL_RADIUS.get();
        float power = (float) (double) ModConfigs.TIMER.EXPLOSION_STRENGTH.get();

        BlockPos center = new BlockPos(x, y, z);
        sw.playSound(null, center, SoundEvents.GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        sw.sendParticles(ParticleTypes.EXPLOSION, x, y, z, (int)(visualRad * 4), visualRad / 2, visualRad / 2, visualRad / 2, 0.1);
        sw.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0, 0, 0);

        Explosion explosion = new Explosion(world, null, null, null, x, y, z, power, fire,
                breakBlocks ? Explosion.Mode.DESTROY : Explosion.Mode.NONE);
        explosion.explode();
        explosion.finalizeExplosion(true);

        AxisAlignedBB aabb = new AxisAlignedBB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        List<LivingEntity> victims = sw.getEntitiesOfClass(LivingEntity.class, aabb, e -> e.isAlive());
        for (LivingEntity e : victims) {
            double dx = e.getX() - x;
            double dy = (e.getY() + e.getBbHeight() * 0.5) - y;
            double dz = e.getZ() - z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > radius) continue;

            double t = Math.min(1.0, dist / radius);
            double dmg = dmgEpic + (dmgOuter - dmgEpic) * t;
            e.hurt(DamageSource.explosion((Explosion) null), (float) dmg);
        }

        if (noItemDestroy) {
            AxisAlignedBB area = new AxisAlignedBB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
            List<ItemEntity> items = sw.getEntitiesOfClass(ItemEntity.class, area);
            for (ItemEntity it : items) {
                it.setInvulnerable(true);
                Vector3d dir = it.position().subtract(x, y, z).normalize().scale(0.25);
                it.setDeltaMovement(it.getDeltaMovement().add(dir));
            }
        }
    }
}
