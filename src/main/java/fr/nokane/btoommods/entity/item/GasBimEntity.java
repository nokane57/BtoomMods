// fr/nokane/btoommods/entity/item/GasBimEntity.java
package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class GasBimEntity extends ProjectileItemEntity {
    private int ticksFromLaunch = 0;
    private int ticksOnGround   = 0;
    private boolean droppedDisabled = false;
    private boolean hasTouchedGround = false;

    public GasBimEntity(EntityType<? extends GasBimEntity> type, World level) { super(type, level); }
    public GasBimEntity(EntityType<? extends GasBimEntity> type, World level, LivingEntity owner) { super(type, owner, level); }

    @Override protected Item getDefaultItem() { return ModItems.GAS_BIM.get(); }

    @Override
    public void tick() {
        super.tick();
        ticksFromLaunch++;

        // explosion forcée après X ticks
        if (!level.isClientSide && ticksFromLaunch >= ModConfigs.COMMON.GAS_EXPLODE_AFTER_TICKS.get()) {
            explodeGas();
            return;
        }

        // gestion posé au sol
        if (this.isOnGround()) {
            hasTouchedGround = true;
            ticksOnGround++;
            // petit frottement
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.0, 0.6));
            // drop item désactivé 1s après posé
            if (!level.isClientSide
                    && !droppedDisabled
                    && ticksOnGround >= ModConfigs.COMMON.GAS_PICKUP_AFTER_TICKS.get()) {
                droppedDisabled = true;
                ItemEntity drop = new ItemEntity(level, this.getX(), this.getY(), this.getZ(),
                        ModItems.GAS_BIM_DISABLED.get().getDefaultInstance());
                level.addFreshEntity(drop);
            }
        }
    }

    @Override
    protected void onHit(RayTraceResult hit) {
        super.onHit(hit);
        if (level.isClientSide) return;

        if (hit.getType() == RayTraceResult.Type.BLOCK) {
            // léger rebond
            Vector3d v = this.getDeltaMovement();
            this.setDeltaMovement(v.x * 0.25, 0.25, v.z * 0.25);
            level.playSound(null, this.blockPosition(), SoundEvents.SLIME_BLOCK_STEP, SoundCategory.PLAYERS, 0.4F, 1.4F);
        }
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult hit) {
        super.onHitEntity(hit);
        if (level.isClientSide) return;

        if (hit.getEntity() instanceof LivingEntity) {
            hit.getEntity().hurt(DamageSource.thrown(this, this.getOwner()), 2.0F); // 1 cœur
        }
        // petit rebond vers le sol
        Vector3d v = this.getDeltaMovement();
        this.setDeltaMovement(v.x * 0.25, -0.1, v.z * 0.25);
        level.playSound(null, this.blockPosition(), SoundEvents.SLIME_BLOCK_STEP, SoundCategory.PLAYERS, 0.4F, 1.3F);
    }

    private void explodeGas() {
        if (level.isClientSide) return;

        // spawn le champ de gaz
        fr.nokane.btoommods.entity.misc.GasCloudFieldEntity field = ModEntities.GAS_CLOUD_FIELD.get().create(level);
        if (field != null) {
            field.setPos(this.getX(), this.getY(), this.getZ());
            level.addFreshEntity(field);
        }
        this.remove();
    }

    @Override
    public net.minecraft.network.IPacket<?> getAddEntityPacket() {
        return net.minecraftforge.fml.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
