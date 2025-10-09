package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.*;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.network.IPacket;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.List;

/**
 * 💣 Cracker BIM — Projectile explosif :
 * - Subit la gravité
 * - Explosion immédiate à l’impact
 * - Dégâts directs + dégâts d’épicentre configurables
 * - Dégâts de zone atténués selon la distance
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

        // Empêche l’enfoncement dans le sol
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
        if (level.isClientSide || hasExploded) return;
        hasExploded = true;

        explode();
        this.remove();
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult hit) {
        if (level.isClientSide || hasExploded) return;
        hasExploded = true;

        // 💥 Explosion centrée sur l'impact
        explode();

        // 💢 Dégâts directs boostés (configurable)
        Entity target = hit.getEntity();
        if (target.isAlive()) {
            double mult = ModConfigs.CRACKER.DIRECT_HIT_MULTIPLIER.get();
            float baseDmg = ModConfigs.CRACKER.EXPLOSION_STRENGTH.get().floatValue() * 2.0F;
            float impactDmg = (float) (baseDmg * mult);

            DamageSource dmgSource = new DamageSource("explosion.cracker_bim")
                    .setExplosion()
                    .setProjectile();

            target.hurt(dmgSource, impactDmg);
        }

        this.remove();
    }

    /** 💥 Explosion propre + dégâts de zone progressifs */
    private void explode() {
        if (level.isClientSide) return;

        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        float strength = ModConfigs.CRACKER.EXPLOSION_STRENGTH.get().floatValue();
        double radius = ModConfigs.CRACKER.RADIUS.get();
        boolean breakBlocks = ModConfigs.CRACKER.BREAK_BLOCK.get();
        double epicenterHearts = ModConfigs.CRACKER.EPICENTER_DAMAGE.get();

        // Explosion visuelle et sonore
        level.explode(
                this.getOwner(),
                x, y, z,
                strength,
                false,
                breakBlocks ? Explosion.Mode.BREAK : Explosion.Mode.NONE
        );

        // 🧨 Dégâts de zone progressifs
        AxisAlignedBB area = new AxisAlignedBB(
                x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius
        );

        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area);
        for (LivingEntity e : nearby) {
            double distSqr = this.distanceToSqr(e);
            if (distSqr <= radius * radius) {
                double dist = Math.sqrt(distSqr);
                double factor = 1.0 - (dist / radius); // Diminution linéaire
                factor = Math.max(0.0, factor);

                // 💥 Dégâts à l’épicentre (1 cœur = 2 HP)
                float maxDamage = (float) (epicenterHearts * 2.0);
                float damage = (float) (maxDamage * factor);

                DamageSource src = new DamageSource("explosion.cracker_bim").setExplosion();
                e.hurt(src, damage);
            }
        }

    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
