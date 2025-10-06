package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.*;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.network.IPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

/**
 * Projectile du Cracker BIM :
 * - Subit la gravité (poids configurable)
 * - Vitesse ajustée par le poids
 * - Explosion immédiate à l'impact sans rebond
 */
public class CrackerBimEntity extends ProjectileItemEntity {

    private static final double GROUND_EPS = 0.02;
    private boolean hasExploded = false;

    public CrackerBimEntity(EntityType<? extends CrackerBimEntity> type, World world) {
        super(type, world);
    }

    public CrackerBimEntity(World world, LivingEntity owner) {
        super(ModEntities.CRACKER_BIM.get(), owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return fr.nokane.btoommods.item.ModItems.CRACKER_BIM.get();
    }

    @Override
    public void tick() {
        super.tick();

        // Gravité ajustée par le poids
        if (!this.isNoGravity()) {
            Vector3d vel = this.getDeltaMovement();
            double poids = ModConfigs.CRACKER.POIDS_PROJECTILE.get();
            this.setDeltaMovement(vel.x, vel.y - (0.04D * poids), vel.z);
        }

        // Empêche enfoncement dans le sol
        BlockPos pos = this.blockPosition();
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (!shape.isEmpty()) {
            double topY = pos.getY() + shape.max(Direction.Axis.Y);
            if (this.getY() < topY + GROUND_EPS) {
                this.setPos(this.getX(), topY + GROUND_EPS, this.getZ());
            }
        }

        // Sécurité : auto-suppression après un certain temps
        if (!level.isClientSide && this.tickCount > ModConfigs.CRACKER.LIFETIME_TICKS.get()) {
            explode();
            this.remove();
        }
    }

    @Override
    protected void onHit(RayTraceResult hit) {
        super.onHit(hit);
        if (level.isClientSide || hasExploded) return;
        hasExploded = true;

        explode();
        this.remove();
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult hit) {
        super.onHitEntity(hit);
        if (level.isClientSide || hasExploded) return;
        hasExploded = true;

        explode();
        this.remove();
    }

    /** 💥 Explosion propre et configurable */
    /** 💥 Explosion propre et configurable */
    private void explode() {
        if (level.isClientSide) return;

        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        float strength = ModConfigs.CRACKER.EXPLOSION_STRENGTH.get().floatValue();
        boolean breakBlocks = ModConfigs.CRACKER.BREAK_BLOCK.get();

        // Explosion sans feu, mode configurable
        level.explode(
                this.getOwner(),         // entité responsable
                x, y, z,                 // position
                strength,                // puissance
                false,                   // causesFire (ici non)
                breakBlocks ? Explosion.Mode.BREAK : Explosion.Mode.NONE
        );
    }


    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
