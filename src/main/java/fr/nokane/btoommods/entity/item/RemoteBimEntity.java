package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.net.GlowS2C;
import fr.nokane.btoommods.net.Net;
import fr.nokane.btoommods.net.RemoteOwnerMarkerS2C;
import net.minecraft.entity.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
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
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.*;

public class RemoteBimEntity extends ProjectileItemEntity {

    private static final DataParameter<Integer> SLOT_ID = EntityDataManager.defineId(RemoteBimEntity.class, DataSerializers.INT);
    private static final DataParameter<Boolean> STUCK = EntityDataManager.defineId(RemoteBimEntity.class, DataSerializers.BOOLEAN);
    private static final double SNEAK_RECALL_RADIUS = 1.5D;
    private static final int SNEAK_RECALL_MIN_AGE = 10;

    private int ownerMarkerCooldown = 0;

    public RemoteBimEntity(EntityType<? extends RemoteBimEntity> type, World level) {
        super(type, level);
    }

    public RemoteBimEntity(EntityType<? extends RemoteBimEntity> type, World level, LivingEntity owner) {
        super(type, owner, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SLOT_ID, 1);
        this.entityData.define(STUCK, false);
    }

    @Override
    public Item getDefaultItem() {
        return ModItems.REMOTE_BIM.get();
    }

    public int getSlot() { return Math.max(1, Math.min(8, this.entityData.get(SLOT_ID))); }
    public void setSlot(int s) { this.entityData.set(SLOT_ID, Math.max(1, Math.min(8, s))); }

    public boolean isStuck() { return this.entityData.get(STUCK); }
    private void setStuck(boolean b) { this.entityData.set(STUCK, b); }

    @Override
    protected float getGravity() {
        double poids = Math.max(0.1, ModConfigs.REMOTE.POIDS_PROJECTILE.get());
        return (float)(0.03D / poids);
    }

    @Override
    public boolean isNoGravity() {
        return isStuck() || super.isNoGravity();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        // ✅ Force le rendu jusqu’à ~400 blocs (400 * 400 = 160000)
        // ou même plus si tu veux le voir à l’infini
        return dist < 400 * 400;
    }



    // --- ces trois méthodes NE doivent PAS avoir @Override en 1.16.5 ---
    public boolean shouldRenderName() {
        return false;
    }

    public boolean isCurrentlyGlowing() {
        return false;
    }

    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (isStuck()) {
            this.setDeltaMovement(Vector3d.ZERO);
            this.noPhysics = true;
            this.setInvisible(false);
            this.setPos(this.getX(), this.getY(), this.getZ());

            if (!level.isClientSide) {
                Entity owner = this.getOwner();
                if (owner instanceof ServerPlayerEntity) {
                    ServerPlayerEntity sp = (ServerPlayerEntity) owner;
                    if (ownerMarkerCooldown-- <= 0) {
                        ownerMarkerCooldown = 40;
                        int slot = getSlot();
                        int color = getServerSlotColor(slot);

                        // ✅ Glow visible uniquement pour le propriétaire
                        Net.toPlayer(sp, new GlowS2C(60, new int[]{this.getId()}, color));

                        // ✅ Si le joueur est loin, afficher un repère de particules
                        double distSqr = sp.distanceToSqr(this);
                        if (distSqr > 50 * 50) {
                            Net.toPlayer(sp, new RemoteOwnerMarkerS2C(this.getX(), this.getY() + 0.2, this.getZ()));
                        }
                    }
                }
                tryOwnerSneakPickup();
            }
            return;
        }

        if (!level.isClientSide) {
            if (tryOwnerSneakPickup()) return;
            int lifetime = ModConfigs.REMOTE.REMOTE_LIFETIME_TICKS.get();
            if (this.tickCount > lifetime) this.remove();
        }
    }

    private int getServerSlotColor(int slot) {
        int[] colors = {
                0xFF3C3C, 0xFF8800, 0xFFD700, 0x00FF00,
                0x00FFFF, 0x0066FF, 0xAA00FF, 0xFF00AA
        };
        int base = colors[(slot - 1) % colors.length];
        return (slot << 24) | (base & 0xFFFFFF);
    }

    @Override
    protected void onHit(RayTraceResult hit) {
        if (level.isClientSide) return;

        if (hit.getType() == RayTraceResult.Type.BLOCK) {
            BlockRayTraceResult br = (BlockRayTraceResult) hit;
            Direction face = br.getDirection();
            Vector3d loc = br.getLocation();
            double eps = 0.02D;
            this.setPos(loc.x + face.getStepX() * eps, loc.y + face.getStepY() * eps, loc.z + face.getStepZ() * eps);
            this.setDeltaMovement(Vector3d.ZERO);
            this.noPhysics = true;
            this.setStuck(true);
        } else if (hit.getType() == RayTraceResult.Type.ENTITY) {
            onHitEntity((EntityRayTraceResult) hit);
        }
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult result) {
        if (level.isClientSide) return;
        Vector3d loc = result.getLocation();
        this.setPos(loc.x, loc.y, loc.z);
        this.setDeltaMovement(Vector3d.ZERO);
        this.noPhysics = true;
        this.setStuck(true);
    }

    public void detonateNow() {
        if (this.removed || !(level instanceof ServerWorld)) return;
        ServerWorld sw = (ServerWorld) level;
        BlockPos center = this.blockPosition();

        double radius = ModConfigs.REMOTE.REMOTE_RADIUS.get();
        boolean breakBlocks = ModConfigs.REMOTE.BREAK_BLOCKS.get();
        boolean noItemDestroy = ModConfigs.REMOTE.REMOTE_NO_ITEM_DESTROY.get();
        double blockBreakRadius = ModConfigs.REMOTE.REMOTE_BREAK_RADIUS.get();

        sw.playSound(null, center, SoundEvents.GENERIC_EXPLODE, SoundCategory.BLOCKS,
                0.7F, 0.9F + sw.random.nextFloat() * 0.2F);
        sw.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);

        if (breakBlocks) {
            int r = (int) Math.ceil(blockBreakRadius);
            BlockPos.Mutable pos = new BlockPos.Mutable();
            for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
                pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                double d = Math.sqrt(x * x + y * y + z * z);
                if (d <= blockBreakRadius && !sw.isEmptyBlock(pos)
                        && sw.getBlockState(pos).getExplosionResistance(sw, pos, null) < 200.0F) {
                    sw.destroyBlock(pos, true);
                }
            }
        }

        AxisAlignedBB aabb = new AxisAlignedBB(getX() - radius, getY() - radius, getZ() - radius,
                getX() + radius, getY() + radius, getZ() + radius);

        LivingEntity ownerLE = null;
        if (this.getOwner() instanceof LivingEntity)
            ownerLE = (LivingEntity) this.getOwner();

        DamageSource src = DamageSource.explosion(ownerLE);

        List<Entity> list = sw.getEntities(this, aabb, new java.util.function.Predicate<Entity>() {
            @Override
            public boolean test(Entity e) {
                return e.isAlive() && (!(e instanceof ItemEntity) || !noItemDestroy);
            }
        });

        for (Entity e : list) {
            if (e instanceof LivingEntity) {
                double d = Math.sqrt(e.distanceToSqr(this));
                if (d > radius) continue;
                float dmg = (float) (8.0 * (1.0 - d / radius));
                ((LivingEntity) e).hurt(src, dmg * 2.0F);
            }
        }

        this.remove();
    }

    private boolean tryOwnerSneakPickup() {
        Entity owner = this.getOwner();
        if (!(owner instanceof ServerPlayerEntity)) return false;
        ServerPlayerEntity sp = (ServerPlayerEntity) owner;
        if (!sp.isShiftKeyDown() || this.tickCount < SNEAK_RECALL_MIN_AGE) return false;
        if (this.distanceToSqr(sp) > SNEAK_RECALL_RADIUS * SNEAK_RECALL_RADIUS) return false;

        ItemStack give = this.getItem().isEmpty() ? new ItemStack(getDefaultItem()) : this.getItem().copy();
        if (!sp.addItem(give)) sp.drop(give, false);
        this.remove();
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Slot", getSlot());
        nbt.putBoolean("Stuck", isStuck());
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        setSlot(nbt.getInt("Slot"));
        setStuck(nbt.getBoolean("Stuck"));
    }

    public void assignSlotAuto() {
        if (!(level instanceof ServerWorld)) { setSlot(1); return; }
        ServerWorld sw = (ServerWorld) level;
        UUID me = (getOwner() != null ? getOwner().getUUID() : new UUID(0, 0));
        int scan = ModConfigs.REMOTE.REMOTE_SCAN_RADIUS.get();

        AxisAlignedBB box = new AxisAlignedBB(getX() - scan, getY() - scan, getZ() - scan,
                getX() + scan, getY() + scan, getZ() + scan);

        Set<Integer> taken = new HashSet<>();
        for (RemoteBimEntity e : sw.getEntitiesOfClass(RemoteBimEntity.class, box)) {
            Entity eo = e.getOwner();
            if (eo != null && eo.getUUID().equals(me)) {
                taken.add(e.getSlot());
            }
        }

        for (int s = 1; s <= 8; s++) {
            if (!taken.contains(s)) {
                setSlot(s);
                return;
            }
        }
        setSlot(1);
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
