package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.item.ModItems;
import net.minecraft.entity.*;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.network.IPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
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

        // Gravité selon poids
        if (!this.isNoGravity()) {
            Vector3d vel = this.getDeltaMovement();
            double poids = ModConfigs.BLAZING.POIDS_PROJECTILE.get();
            this.setDeltaMovement(vel.x, vel.y - (0.04D * poids), vel.z);
        }

        // Correction d'enfoncement
        BlockPos pos = this.blockPosition();
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (!shape.isEmpty()) {
            double topY = pos.getY() + shape.max(Direction.Axis.Y);
            if (this.getY() < topY + GROUND_EPS) {
                this.setPos(this.getX(), topY + GROUND_EPS, this.getZ());
            }
        }

        // Auto-suppression de sécurité
        if (!level.isClientSide && this.tickCount > 200) {
            spawnFireField();
            this.remove();
        }
    }

    @Override
    protected void onHit(RayTraceResult hit) {
        if (level.isClientSide || hasImpacted) return;
        hasImpacted = true;
        spawnFireField();
        this.remove();
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult hit) {
        if (level.isClientSide || hasImpacted) return;
        hasImpacted = true;
        spawnFireField();
        this.remove();
    }

    /** 🔥 Crée le champ de feu au point d’impact sans remonter à la surface */
    private void spawnFireField() {
        if (level.isClientSide) return;

        BlockPos impact = this.blockPosition();

        fr.nokane.btoommods.entity.misc.BlazingFireFieldEntity field =
                ModEntities.BLAZING_FIRE_FIELD.get().create(level);

        if (field != null) {
            field.setPos(impact.getX() + 0.5, impact.getY(), impact.getZ() + 0.5);
            level.addFreshEntity(field);

            // 🔊 Explosion douce
            level.playSound(null, impact, SoundEvents.GENERIC_EXPLODE,
                    SoundCategory.BLOCKS, 0.4F, 1.0F);

            // 🔥 Son d’activation du feu
            fr.nokane.btoommods.sound.SoundUtils.playWorldSound(
                    level,
                    impact.getX() + 0.5,
                    impact.getY(),
                    impact.getZ() + 0.5,
                    fr.nokane.btoommods.sound.ModSounds.FIRE_ITEM.get(),
                    0.6F,
                    1.0F
            );
        }
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
