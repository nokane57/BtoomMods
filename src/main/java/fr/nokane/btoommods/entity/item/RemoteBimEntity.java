package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.config.ModConfigs.ExplosionMode;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.net.Net;
import fr.nokane.btoommods.net.RemoteOwnerMarkerS2C;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class RemoteBimEntity extends ProjectileItemEntity {

    private static final DataParameter<Integer> SLOT_ID = EntityDataManager.defineId(RemoteBimEntity.class, DataSerializers.INT);
    private static final DataParameter<Boolean> STUCK   = EntityDataManager.defineId(RemoteBimEntity.class, DataSerializers.BOOLEAN);
    private static final double SNEAK_RECALL_RADIUS = 1.5D; // distance max (blocs)
    private static final int    SNEAK_RECALL_MIN_AGE = 10;  // évite le pickup instantané (ticks)

    private static final double RESTITUTION_ENTITY = 0.35; // rebond sur entités
    private int ownerMarkerCooldown = 0; // ticks

    public RemoteBimEntity(EntityType<? extends RemoteBimEntity> type, World level){ super(type, level); }
    public RemoteBimEntity(EntityType<? extends RemoteBimEntity> type, World level, LivingEntity owner){ super(type, owner, level); }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SLOT_ID, 1);
        this.entityData.define(STUCK, false);
    }

    @Override public Item getDefaultItem(){ return ModItems.REMOTE_BIM.get(); }

    public int  getSlot(){ return Math.max(1, Math.min(8, this.entityData.get(SLOT_ID))); }
    public void setSlot(int s){ this.entityData.set(SLOT_ID, Math.max(1, Math.min(8, s))); }
    public boolean isStuck(){ return this.entityData.get(STUCK); }
    private  void setStuck(boolean b){ this.entityData.set(STUCK, b); }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) return;

        if (tryOwnerSneakPickup()) return;
        // Lifetime (config) : n'expire que si non collée (par défaut 0 = infini)
        int lifetime = ModConfigs.COMMON.REMOTE_LIFETIME_TICKS.get();
        if (!isStuck() && lifetime > 0 && this.tickCount > lifetime) {
            this.remove();
            return;
        }

        if (isStuck()){
            this.setDeltaMovement(Vector3d.ZERO);
            this.noPhysics = true;
            this.setInvisible(true);

            if (ownerMarkerCooldown-- <= 0){
                ownerMarkerCooldown = ModConfigs.COMMON.REMOTE_MARKER_COOLDOWN_TICKS.get();
                if (ownerMarkerCooldown <= 0) ownerMarkerCooldown = 20;
                if (getOwner() instanceof ServerPlayerEntity){
                    ServerPlayerEntity sp = (ServerPlayerEntity) getOwner();
                    if (sp.connection != null && !sp.hasDisconnected()) {
                        Net.toPlayer(sp, new RemoteOwnerMarkerS2C(this.getX(), this.getY() + 0.05, this.getZ()));
                    }
                }
            }
            return;
        }

        // en vol : visible, petite friction
        this.setInvisible(false);
        Vector3d v = this.getDeltaMovement();
        this.setDeltaMovement(v.x * 0.99, v.y, v.z * 0.99);
    }

    @Override
    protected void onHit(RayTraceResult hit) {
        super.onHit(hit);
        if (level.isClientSide) return;

        if (hit.getType() == RayTraceResult.Type.BLOCK){
            BlockRayTraceResult br = (BlockRayTraceResult) hit;
            Direction face = br.getDirection();
            Vector3d loc = br.getLocation();
            double off = 0.01;
            this.setPos(loc.x + face.getStepX()*off, loc.y + face.getStepY()*off, loc.z + face.getStepZ()*off);

            this.setDeltaMovement(Vector3d.ZERO);
            this.noPhysics = true;
            this.setStuck(true);
            this.setInvisible(true);

            level.playSound(null, this.blockPosition(), SoundEvents.SLIME_BLOCK_STEP, SoundCategory.PLAYERS, 0.5F, 1.2F);
        }
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult hit) {
        super.onHitEntity(hit);
        if (level.isClientSide) return;

        Vector3d v = this.getDeltaMovement();
        this.setDeltaMovement(v.x * 0.5, Math.max(0.08, -v.y * RESTITUTION_ENTITY) + 0.08, v.z * 0.5);
        level.playSound(null, this.blockPosition(), SoundEvents.SLIME_BLOCK_STEP, SoundCategory.PLAYERS, 0.4F, 1.1F);
    }

    /* === Détonation (suit remote_bim) === */
    public void detonateNow(){
        if (this.removed) return;
        if (!(level instanceof ServerWorld)) return;

        ServerWorld sw = (ServerWorld) level;
        BlockPos center = this.blockPosition();

        // ---- lecture config remote_bim
        double radiusBlocks = ModConfigs.COMMON.REMOTE_RADIUS.get();
        double heartsAtCenter = ModConfigs.COMMON.REMOTE_EPICENTER_HEARTS.get();
        float hpAtCenter = (float)(heartsAtCenter * 2.0); // cœurs -> HP
        float blockBlast = ModConfigs.COMMON.REMOTE_BLOCK_BLAST.get().floatValue();
        boolean causesFire = ModConfigs.COMMON.REMOTE_CAUSES_FIRE.get();
        ExplosionMode modeCfg = ModConfigs.COMMON.REMOTE_EXPLOSION_MODE.get();
        boolean manualBreak = ModConfigs.COMMON.REMOTE_MANUAL_BREAK_ENABLED.get();
        int manualMax = ModConfigs.COMMON.REMOTE_MANUAL_BREAK_MAX_BLOCKS.get();

        net.minecraft.world.Explosion.Mode blockMode =
                (modeCfg == ExplosionMode.BREAK) ? net.minecraft.world.Explosion.Mode.BREAK
                        : net.minecraft.world.Explosion.Mode.NONE;

        // ---- dégâts entités (profil linéaire centre -> rayon)
        if (radiusBlocks > 0.0) {
            AxisAlignedBB box = new AxisAlignedBB(
                    getX()-radiusBlocks, getY()-radiusBlocks, getZ()-radiusBlocks,
                    getX()+radiusBlocks, getY()+radiusBlocks, getZ()+radiusBlocks
            );
            LivingEntity ownerLE = (getOwner() instanceof LivingEntity) ? (LivingEntity) getOwner() : null;
            DamageSource src = DamageSource.explosion(ownerLE);

            List<LivingEntity> list = sw.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);
            for (LivingEntity e : list){
                double d = Math.sqrt(e.distanceToSqr(this));
                if (d > radiusBlocks) continue;
                float dmg = (float)Math.max(0.0, hpAtCenter * (1.0 - d / radiusBlocks));
                if (dmg > 0f) e.hurt(src, dmg);
            }
        }

        // ---- petit "nettoyage" manuel optionnel
        if (manualBreak && manualMax > 0) {
            Random r = sw.random;
            int broken = 0, attempts = Math.max(manualMax * 4, manualMax + 16);
            while (broken < manualMax && attempts-- > 0){
                int dx = r.nextInt(7) - 3;
                int dz = r.nextInt(7) - 3;
                BlockPos p = center.offset(dx, 0, dz);
                BlockPos.Mutable m = new BlockPos.Mutable();
                for (int i=0;i<6;i++){
                    m.set(p.getX(), p.getY()+i, p.getZ());
                    if (sw.isEmptyBlock(m) && !sw.isEmptyBlock(m.below())){
                        p = m.below();
                        break;
                    }
                }
                float hardness = sw.getBlockState(p).getDestroySpeed(sw, p);
                if (hardness >= 0 && hardness <= 3.0f && !sw.isEmptyBlock(p)){
                    if (sw.destroyBlock(p, true)) broken++;
                }
            }
        }

        // ---- feedback + explosion vanilla suivant config
        sw.levelEvent(2001, center, net.minecraft.block.Block.getId(sw.getBlockState(center)));
        sw.playSound(null, center, SoundEvents.GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.0F, 0.9F + sw.random.nextFloat()*0.2F);

        if (blockBlast > 0f || blockMode != net.minecraft.world.Explosion.Mode.NONE || causesFire) {
            sw.explode(this, getX(), getY(), getZ(), blockBlast, causesFire, blockMode);
        } else {
            // pas d'explosion vanilla (comportement par défaut antérieur)
            sw.explode(this, getX(), getY(), getZ(), 0.0F, false, net.minecraft.world.Explosion.Mode.NONE);
        }

        this.remove();
    }

    /* === Slot 1..8 unique par joueur — scan borné/configurable === */
    public void assignSlotAuto(){
        if (!(level instanceof ServerWorld)) { setSlot(1); return; }
        UUID me = this.getOwner() != null ? this.getOwner().getUUID() : new UUID(0,0);
        ServerWorld sw = (ServerWorld) level;

        int scan = ModConfigs.COMMON.REMOTE_SCAN_RADIUS.get();
        AxisAlignedBB search = new AxisAlignedBB(
                this.getX() - scan, this.getY() - scan, this.getZ() - scan,
                this.getX() + scan, this.getY() + scan, this.getZ() + scan
        );

        java.util.Set<Integer> taken = new java.util.HashSet<>();
        sw.getEntitiesOfClass(RemoteBimEntity.class, search).stream()
                .filter(e -> e.getOwner()!=null && e.getOwner().getUUID().equals(me))
                .map(RemoteBimEntity::getSlot)
                .forEach(taken::add);

        for (int s=1; s<=8; s++){
            if (!taken.contains(s)){ setSlot(s); return; }
        }
        setSlot(1);
    }

    /** Tente de rendre l'item au propriétaire s'il est accroupi et au contact. */
    private boolean tryOwnerSneakPickup() {
        if (!(getOwner() instanceof ServerPlayerEntity)) return false;
        ServerPlayerEntity sp = (ServerPlayerEntity) getOwner();

        if (!sp.isShiftKeyDown()) return false;                  // doit être accroupi
        if (this.tickCount < SNEAK_RECALL_MIN_AGE) return false; // anti-réaspiration instantanée

        double max2 = SNEAK_RECALL_RADIUS * SNEAK_RECALL_RADIUS;
        if (this.distanceToSqr(sp) > max2) return false;         // trop loin

        // Rendre EXACTEMENT l'item tiré (NBT inclus)
        ItemStack toGive = this.getItem().copy();
        if (toGive.isEmpty()) toGive = new ItemStack(this.getDefaultItem());

        if (!sp.addItem(toGive)) {
            sp.drop(toGive, false); // inventaire plein → drop au sol
        }

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
