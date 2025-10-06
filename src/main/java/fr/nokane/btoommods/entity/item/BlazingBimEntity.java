package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.item.ModItems;
import net.minecraft.entity.*;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.network.IPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.fml.network.NetworkHooks;

/**
 * Projectile du Blazing BIM :
 * - Subit la gravité (poids configurable)
 * - Crée un champ de feu à l'impact
 * - Aucun rebond ni effet physique secondaire
 */
public class BlazingBimEntity extends ProjectileItemEntity {

    private static final double GROUND_EPS = 0.02;
    private boolean hasImpacted = false;

    public BlazingBimEntity(EntityType<? extends BlazingBimEntity> type, World level) {
        super(type, level);
    }

    public BlazingBimEntity(World level, LivingEntity owner) {
        super(ModEntities.BLAZING_BIM.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BLAZING_BIM.get();
    }

    @Override
    public void tick() {
        super.tick();

        // Appliquer la gravité avec coefficient de poids
        if (!this.isNoGravity()) {
            Vector3d vel = this.getDeltaMovement();
            double poids = ModConfigs.BLAZING.POIDS_PROJECTILE.get();
            this.setDeltaMovement(vel.x, vel.y - (0.04D * poids), vel.z);
        }

        // Correction de position si au sol (évite enfoncement visuel)
        BlockPos pos = this.blockPosition();
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (!shape.isEmpty()) {
            double topY = pos.getY() + shape.max(Direction.Axis.Y);
            if (this.getY() < topY + GROUND_EPS) {
                this.setPos(this.getX(), topY + GROUND_EPS, this.getZ());
            }
        }

        // Sécurité : si bug ou durée trop longue, auto-suppression
        if (!level.isClientSide && this.tickCount > 200) {
            spawnFireField();
            this.remove();
        }
    }

    @Override
    protected void onHit(RayTraceResult hit) {
        super.onHit(hit);

        if (level.isClientSide || hasImpacted) return;
        hasImpacted = true;

        spawnFireField();
        this.remove();
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult hit) {
        super.onHitEntity(hit);

        if (level.isClientSide || hasImpacted) return;
        hasImpacted = true;

        spawnFireField();
        this.remove();
    }

    /** 🔥 Génère le champ de feu au point d’impact */
    private void spawnFireField() {
        if (level.isClientSide) return;

        BlockPos pos = this.blockPosition();
        int y = level.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        BlockPos center = new BlockPos(pos.getX(), y, pos.getZ());

        fr.nokane.btoommods.entity.misc.BlazingFireFieldEntity field =
                ModEntities.BLAZING_FIRE_FIELD.get().create(level);

        if (field != null) {
            field.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
            level.addFreshEntity(field);
        }
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
