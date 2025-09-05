package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.item.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.network.IPacket;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.List;

public class CrackerBimEntity extends ProjectileItemEntity {

    public CrackerBimEntity(EntityType<? extends CrackerBimEntity> type, World level) {
        super(type, level);
    }

    public CrackerBimEntity(World level, LivingEntity owner) {
        super(ModEntities.CRACKER_BIM.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CRACKER_BIM.get();
    }

    @Override
    public void tick() {
        super.tick();

        // Déclencher si on "traverse" des lianes/feuilles (on est dedans)
        BlockPos pos = this.blockPosition();
        BlockState bs = level.getBlockState(pos);
        if (bs.is(Blocks.VINE) || bs.is(BlockTags.LEAVES)) {
            explodeAndDiscard();
            return;
        }

        // Portée maximale (config)
        int life = ModConfigs.COMMON.lifetimeTicks.get();
        if (this.tickCount > life) {
            explodeAndDiscard();
        }
    }

    @Override
    protected void onHit(RayTraceResult hit) {
        super.onHit(hit);
        // Appelé pour tout type de hit (bloc/entité), on décide au besoin
        if (hit.getType() == RayTraceResult.Type.BLOCK) {
            BlockRayTraceResult br = (BlockRayTraceResult) hit;
            BlockPos bp = br.getBlockPos();
            BlockState hs = level.getBlockState(bp);
            if (isSoftBlock(hs, bp)) {
                // On ignore les petits blocs (hautes herbes, fleurs, air/remplaçable/liquide)
                return;
            }
        }
        explodeAndDiscard();
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult hit) {
        super.onHitEntity(hit);
        explodeAndDiscard();
    }

    @Override
    protected void onHitBlock(BlockRayTraceResult hit) {
        super.onHitBlock(hit);
        BlockPos bp = hit.getBlockPos();
        BlockState hs = level.getBlockState(bp);
        if (!isSoftBlock(hs, bp)) {
            explodeAndDiscard();
        }
        // sinon on ignore (hautes herbes/fleurs/etc.)
    }

    private boolean isSoftBlock(BlockState s, BlockPos pos) {
        // Air, liquides, matériaux remplaçables -> on ignore
        if (s.isAir(level, pos)) return true;
        if (s.getMaterial().isLiquid()) return true;
        if (s.getMaterial().isReplaceable()) return true;

        // Herbes hautes & végétation légère
        if (s.is(Blocks.TALL_GRASS) || s.is(Blocks.GRASS) || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN)) return true;
        if (s.is(BlockTags.FLOWERS)) return true;
        if (s.is(Blocks.SWEET_BERRY_BUSH)) return true;

        // On déclenche normalement sur feuilles/lianes (géré ailleurs), donc ici: false
        return false;
    }

    private void explodeAndDiscard() {
        if (this.level.isClientSide) {
            this.remove();
            return;
        }

        // Charger les valeurs depuis la config
        float radius = ModConfigs.COMMON.radius.get().floatValue();
        float epicenterHearts = ModConfigs.COMMON.epicenterHearts.get().floatValue();
        float blockBlast = ModConfigs.COMMON.blockBlast.get().floatValue();
        boolean causesFire = ModConfigs.COMMON.causesFire.get();
        ModConfigs.ExplosionMode mode = ModConfigs.COMMON.explosionMode.get();

        Vector3d p = this.position();

        // 1) Explosion vanilla (optionnelle selon le mode)
        Explosion.Mode vanillaMode = (mode == ModConfigs.ExplosionMode.BREAK) ? Explosion.Mode.BREAK : Explosion.Mode.NONE;
        Explosion boom = this.level.explode(
                this, p.x, p.y, p.z,
                blockBlast,
                causesFire,
                vanillaMode
        );

        // 2) Dégâts personnalisés aux entités : 8 cœurs au centre (par défaut), décroissance linéaire jusqu’à radius
        AxisAlignedBB aabb = new AxisAlignedBB(
                p.x - radius, p.y - radius, p.z - radius,
                p.x + radius, p.y + radius, p.z + radius
        );
        List<LivingEntity> victims = this.level.getEntitiesOfClass(
                LivingEntity.class, aabb, e -> e.isAlive() && e.isPickable()
        );

        for (LivingEntity e : victims) {
            double dist = e.position().distanceTo(p);
            if (dist > radius) continue;
            double factor = Math.max(0.0, 1.0 - (dist / radius)); // linéaire
            float dmgHP = (float) (epicenterHearts * factor * 2.0F); // 1 cœur = 2 HP
            e.hurt(DamageSource.explosion(boom), dmgHP);
        }

        // 3) Son (serveur -> répliqué aux joueurs proches)
        this.level.playSound(
                (PlayerEntity) null,
                p.x, p.y, p.z,
                SoundEvents.GENERIC_EXPLODE,
                SoundCategory.PLAYERS,
                1.0F, 1.0F
        );

        this.remove();
    }

    // Paquet de spawn réseau
    @Override
    public net.minecraft.network.IPacket<?> getAddEntityPacket() {
        return net.minecraftforge.fml.network.NetworkHooks.getEntitySpawningPacket(this);
    }

}
