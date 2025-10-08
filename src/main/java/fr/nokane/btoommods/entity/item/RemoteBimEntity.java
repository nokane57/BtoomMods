package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.net.Net;
import fr.nokane.btoommods.net.RemoteOwnerMarkerS2C;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

    public int getSlot() {
        return Math.max(1, Math.min(8, this.entityData.get(SLOT_ID)));
    }

    public void setSlot(int s) {
        this.entityData.set(SLOT_ID, Math.max(1, Math.min(8, s)));
    }

    public boolean isStuck() {
        return this.entityData.get(STUCK);
    }

    private void setStuck(boolean b) {
        this.entityData.set(STUCK, b);
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) return;

        if (tryOwnerSneakPickup()) return;

        // ✅ Gravité dépendant du poids
        if (!isStuck() && !this.isNoGravity()) {
            Vector3d v = this.getDeltaMovement();
            double poids = ModConfigs.REMOTE.POIDS_PROJECTILE.get();
            this.setDeltaMovement(v.x, v.y - (0.04D * poids), v.z);
        }

        // ⏳ Suppression après durée de vie (si non collée)
        int lifetime = ModConfigs.REMOTE.REMOTE_MARKER_COOLDOWN_TICKS.get();
        if (!isStuck() && lifetime > 0 && this.tickCount > lifetime) {
            this.remove();
            return;
        }

        if (isStuck()) {
            this.setDeltaMovement(Vector3d.ZERO);
            this.noPhysics = true;
            this.setInvisible(true);

            if (ownerMarkerCooldown-- <= 0) {
                ownerMarkerCooldown = ModConfigs.REMOTE.REMOTE_MARKER_COOLDOWN_TICKS.get();
                if (ownerMarkerCooldown <= 0) ownerMarkerCooldown = 20;
                if (getOwner() instanceof ServerPlayerEntity) {
                    ServerPlayerEntity sp = (ServerPlayerEntity) getOwner();
                    if (sp.connection != null && !sp.hasDisconnected()) {
                        Net.toPlayer(sp, new RemoteOwnerMarkerS2C(this.getX(), this.getY() + 0.05, this.getZ()));
                    }
                }
            }
            return;
        }

        // 💨 Vol libre
        this.setInvisible(false);
        Vector3d v = this.getDeltaMovement();
        this.setDeltaMovement(v.x * 0.99, v.y, v.z * 0.99);
    }

    @Override
    protected void onHit(RayTraceResult hit) {
        if (level.isClientSide) return;

        // ❌ Aucun rebond → colle directement à la surface
        if (hit.getType() == RayTraceResult.Type.BLOCK) {
            BlockRayTraceResult br = (BlockRayTraceResult) hit;
            Direction face = br.getDirection();
            Vector3d loc = br.getLocation();

            double offset = 0.02;
            this.setPos(loc.x + face.getStepX() * offset, loc.y + face.getStepY() * offset, loc.z + face.getStepZ() * offset);

            this.setDeltaMovement(Vector3d.ZERO);
            this.noPhysics = true;
            this.setStuck(true);
            this.setInvisible(true);

            SoundUtils.playRebound(level, getX(), getY(), getZ());
        }
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult hit) {
        if (level.isClientSide) return;

        // 💥 Désactivation du rebond : colle directement sur l'entité
        this.setDeltaMovement(Vector3d.ZERO);
        this.noPhysics = true;
        this.setStuck(true);
        this.setInvisible(true);

        SoundUtils.playRebound(level, getX(), getY(), getZ());
    }

    public void detonateNow() {
        if (this.removed || !(level instanceof ServerWorld)) return;

        ServerWorld sw = (ServerWorld) level;
        BlockPos center = this.blockPosition();

        double radius = ModConfigs.REMOTE.REMOTE_RADIUS.get();
        boolean breakBlocks = ModConfigs.REMOTE.BREAK_BLOCKS.get();
        boolean fire = ModConfigs.REMOTE.REMOTE_CAUSES_FIRE.get();

        float explosionPower = (float) Math.min(8.0, radius / 2.0);
        Explosion.Mode mode = breakBlocks ? Explosion.Mode.BREAK : Explosion.Mode.NONE;

        AxisAlignedBB area = new AxisAlignedBB(
                getX() - radius, getY() - radius, getZ() - radius,
                getX() + radius, getY() + radius, getZ() + radius
        );

        LivingEntity ownerLE = (getOwner() instanceof LivingEntity) ? (LivingEntity) getOwner() : null;
        DamageSource src = DamageSource.explosion(ownerLE);

        // 💥 Dégâts
        for (LivingEntity e : sw.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            double d = Math.sqrt(e.distanceToSqr(this));
            if (d > radius) continue;
            float dmg = (float) (8.0 * (1.0 - d / radius));
            e.hurt(src, dmg * 2.0F);
        }

        // 🎯 Explosion
        sw.explode(this, getX(), getY(), getZ(), explosionPower, fire, mode);
        sw.playSound(null, center, SoundEvents.GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.0F, 0.9F + sw.random.nextFloat() * 0.2F);

        // 🔊 Son PI pour le joueur le plus proche
        ServerPlayerEntity nearest = (ServerPlayerEntity) sw.getNearestPlayer(getX(), getY(), getZ(), radius * 2, false);
        if (nearest != null) {
            SoundUtils.playWorldSound(sw, nearest.getX(), nearest.getY(), nearest.getZ(),
                    fr.nokane.btoommods.sound.ModSounds.PI_ITEM.get(), 1.3F, 1.0F);
        }

        this.remove();
    }


    public void assignSlotAuto() {
        if (!(level instanceof ServerWorld)) { setSlot(1); return; }
        UUID me = (getOwner() != null ? getOwner().getUUID() : new UUID(0, 0));
        ServerWorld sw = (ServerWorld) level;

        int scan = ModConfigs.REMOTE.REMOTE_SCAN_RADIUS.get();
        AxisAlignedBB box = new AxisAlignedBB(getX() - scan, getY() - scan, getZ() - scan, getX() + scan, getY() + scan, getZ() + scan);

        Set<Integer> taken = new HashSet<Integer>();
        for (RemoteBimEntity e : sw.getEntitiesOfClass(RemoteBimEntity.class, box)) {
            if (e.getOwner() != null && e.getOwner().getUUID().equals(me)) {
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

    private boolean tryOwnerSneakPickup() {
        if (!(getOwner() instanceof ServerPlayerEntity)) return false;
        ServerPlayerEntity sp = (ServerPlayerEntity) getOwner();
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

    @Override
    public net.minecraft.network.IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
