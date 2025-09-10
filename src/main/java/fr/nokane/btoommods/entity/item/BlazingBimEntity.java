// fr/nokane/btoommods/entity/item/BlazingBimEntity.java
package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.network.IPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.fml.network.NetworkHooks;

public class BlazingBimEntity extends ProjectileItemEntity {
    public BlazingBimEntity(EntityType<? extends BlazingBimEntity> type, World level) { super(type, level); }
    public BlazingBimEntity(World level, LivingEntity owner) { super(ModEntities.BLAZING_BIM.get(), owner, level); }

    @Override protected Item getDefaultItem() { return ModItems.BLAZING_BIM.get(); }

    @Override
    protected void onHit(RayTraceResult hit) {
        super.onHit(hit);
        if (level.isClientSide) { this.remove(); return; }

        BlockPos center = (hit.getType() == RayTraceResult.Type.BLOCK)
                ? ((BlockRayTraceResult) hit).getBlockPos().relative(((BlockRayTraceResult) hit).getDirection())
                : new BlockPos(this.position());

        // Aligne le centre sur le sol pour éviter un champ trop haut/bas
        int y = level.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ());
        center = new BlockPos(center.getX(), y, center.getZ());

        fr.nokane.btoommods.entity.misc.BlazingFireFieldEntity field =
                ModEntities.BLAZING_FIRE_FIELD.get().create(level);
        if (field != null) {
            field.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
            level.addFreshEntity(field);
        }
        this.remove();
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
