// fr/nokane/btoommods/entity/item/BlazingBimEntity.java
package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.entity.ModEntities;

import fr.nokane.btoommods.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.network.IPacket;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

public class BlazingBimEntity extends ProjectileItemEntity {
    public BlazingBimEntity(EntityType<? extends BlazingBimEntity> type, World level) { super(type, level); }
    public BlazingBimEntity(EntityType<? extends BlazingBimEntity> type, World level, LivingEntity owner) { super(type, owner, level); }

    @Override protected Item getDefaultItem() { return ModItems.BLAZING_BIM.get(); }

    @Override
    protected void onHit(RayTraceResult hit) {
        super.onHit(hit);
        if (level.isClientSide) { this.remove(); return; }

        BlockPos center;
        if (hit.getType() == RayTraceResult.Type.BLOCK) {
            BlockRayTraceResult b = (BlockRayTraceResult) hit;
            center = b.getBlockPos().relative(b.getDirection());
        } else {
            center = new BlockPos(this.position());
        }

        this.remove();
    }

    @Override
    public net.minecraft.network.IPacket<?> getAddEntityPacket() {
        return net.minecraftforge.fml.network.NetworkHooks.getEntitySpawningPacket(this);
    }

}
