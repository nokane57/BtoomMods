package fr.nokane.btoommods.entity.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.DamageSource;
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

        if (level.isClientSide) return;

        BlockPos pos = this.blockPosition();
        BlockState bs = level.getBlockState(pos);

        // Détonation sur feuilles/lianes
        if (bs.is(Blocks.VINE) || bs.is(BlockTags.LEAVES)) {
            explodeAndDiscard();
            return;
        }

        // Durée de vie max (config)
        int life = ModConfigs.CRACKER.LIFETIME_TICKS.get();
        if (this.tickCount > life) {
            explodeAndDiscard();
        }
    }

    @Override
    protected void onHit(RayTraceResult hit) {
        super.onHit(hit);
        if (level.isClientSide) return;

        if (hit.getType() == RayTraceResult.Type.BLOCK) {
            BlockRayTraceResult br = (BlockRayTraceResult) hit;
            if (isSoftBlock(level.getBlockState(br.getBlockPos()), br.getBlockPos())) return;
        }

        explodeAndDiscard();
    }

    @Override
    protected void onHitEntity(EntityRayTraceResult hit) {
        super.onHitEntity(hit);
        if (!level.isClientSide) explodeAndDiscard();
    }

    private boolean isSoftBlock(BlockState s, BlockPos pos) {
        if (s.isAir(level, pos)) return true;
        if (s.getMaterial().isLiquid()) return true;
        if (s.getMaterial().isReplaceable()) return true;
        if (s.is(Blocks.TALL_GRASS) || s.is(Blocks.GRASS) || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN)) return true;
        if (s.is(BlockTags.FLOWERS) || s.is(Blocks.SWEET_BERRY_BUSH)) return true;
        return false;
    }

    private void explodeAndDiscard() {
        if (level.isClientSide) {
            remove();
            return;
        }

        // Récupération depuis config
        float radius = (float) ModConfigs.CRACKER.RADIUS.get().doubleValue();
        float epicenterHearts = 8.0F; // configurable plus tard
        float blockBlast = (float) ModConfigs.CRACKER.EXPLOSION_STRENGTH.get().doubleValue();
        boolean breakBlocks = ModConfigs.CRACKER.BREAK_BLOCK.get();

        Vector3d pos = this.position();

        Explosion.Mode vanillaMode = breakBlocks ? Explosion.Mode.BREAK : Explosion.Mode.NONE;
        Explosion boom = this.level.explode(this, pos.x, pos.y, pos.z,
                blockBlast, false, vanillaMode);

        // Dégâts aux entités proches
        AxisAlignedBB area = new AxisAlignedBB(
                pos.x - radius, pos.y - radius, pos.z - radius,
                pos.x + radius, pos.y + radius, pos.z + radius
        );

        List<LivingEntity> victims = this.level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && e.isPickable());

        for (LivingEntity e : victims) {
            double dist = e.position().distanceTo(pos);
            if (dist > radius) continue;
            double factor = Math.max(0.0, 1.0 - dist / radius);
            float dmgHP = epicenterHearts * (float) factor * 2.0F;
            e.hurt(DamageSource.explosion(boom), dmgHP);
        }

        remove();
    }

    @Override
    public net.minecraft.network.IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
