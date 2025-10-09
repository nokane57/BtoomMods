package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import net.minecraft.entity.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.network.IPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.List;

/**
 * 💣 Cracker BIM — Projectile explosif :
 * - Explosion instantanée à l’impact
 * - Dégâts directs + dégâts d’épicentre configurables
 * - Dégâts de zone atténués
 * - Casse les blocs (optionnel)
 * - Ne détruit jamais les items si configuré
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

        // Suppression après un certain temps
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
        explode();

        // 💢 Dégâts directs boostés
        Entity target = hit.getEntity();
        if (target.isAlive() && target instanceof LivingEntity) {
            double mult = ModConfigs.CRACKER.DIRECT_HIT_MULTIPLIER.get();
            float baseDmg = ModConfigs.CRACKER.EXPLOSION_STRENGTH.get().floatValue() * 2.0F;
            float impactDmg = (float) (baseDmg * mult);

            DamageSource dmgSource = new DamageSource("explosion.cracker_bim")
                    .setExplosion().setProjectile();
            target.hurt(dmgSource, impactDmg);
        }
        this.remove();
    }

    /** 💥 Explosion avec options configurables */
    private void explode() {
        if (level.isClientSide || !(level instanceof ServerWorld)) return;

        ServerWorld sw = (ServerWorld) level;
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        float strength = ModConfigs.CRACKER.EXPLOSION_STRENGTH.get().floatValue();
        double radius = ModConfigs.CRACKER.RADIUS.get();
        boolean breakBlocks = ModConfigs.CRACKER.BREAK_BLOCK.get();
        double epicenterHearts = ModConfigs.CRACKER.EPICENTER_DAMAGE.get();
        boolean noItemDestroy = ModConfigs.CRACKER.NO_ITEM_DESTROY.get();
        double blockBreakRadius = ModConfigs.CRACKER.BREAK_BLOCK_RADIUS.get();

        // 💥 Explosion visuelle + son
        sw.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundCategory.BLOCKS,
                0.7F, 0.9F + sw.random.nextFloat() * 0.2F);
        sw.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);

        // ✅ Casse les blocs manuellement si activé
        if (breakBlocks) {
            int blockRadius = (int) Math.ceil(blockBreakRadius);
            BlockPos.Mutable pos = new BlockPos.Mutable();
            BlockPos center = this.blockPosition();

            for (int dx = -blockRadius; dx <= blockRadius; dx++) {
                for (int dy = -blockRadius; dy <= blockRadius; dy++) {
                    for (int dz = -blockRadius; dz <= blockRadius; dz++) {
                        pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (dist <= blockBreakRadius && !sw.isEmptyBlock(pos)) {
                            if (sw.getBlockState(pos).getExplosionResistance(sw, pos, null) < 200.0F) {
                                sw.destroyBlock(pos, true); // drop les items
                            }
                        }
                    }
                }
            }
        }

        // 🧨 Dégâts de zone (ignore les items si configuré)
        AxisAlignedBB area = new AxisAlignedBB(x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius);
        List<Entity> nearby = sw.getEntities(this, area,
                e -> e.isAlive() && (!(e instanceof ItemEntity) || !noItemDestroy));

        for (Entity e : nearby) {
            if (e instanceof LivingEntity) {
                LivingEntity le = (LivingEntity) e;
                double distSqr = this.distanceToSqr(le);
                if (distSqr <= radius * radius) {
                    double dist = Math.sqrt(distSqr);
                    double factor = Math.max(0.0, 1.0 - (dist / radius));
                    float maxDamage = (float) (epicenterHearts * 2.0);
                    float damage = (float) (maxDamage * factor);
                    le.hurt(new DamageSource("explosion.cracker_bim").setExplosion(), damage);
                }
            }
        }
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
