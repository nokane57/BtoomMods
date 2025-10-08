package fr.nokane.btoommods.entity.misc;

import fr.nokane.btoommods.config.ModConfigs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.*;

/**
 * Champ de feu du Blazing BIM :
 * - Particules visibles sur tous les blocs (herbe, feuilles, neige, etc.)
 * - Dégâts verticaux jusqu’à 5 blocs
 * - Seuls les blocs pleins bloquent les dégâts
 */
public class BlazingFireFieldEntity extends Entity {

    private int age;
    private static final DamageSource BLAZING_FLAME =
            (new DamageSource("blazing_flame")).setIsFire();

    private final List<BlockPos> columns = new ArrayList<BlockPos>();
    private boolean initialized = false;
    private static final int MAX_DAMAGE_HEIGHT = 5;

    public BlazingFireFieldEntity(EntityType<? extends BlazingFireFieldEntity> type, World level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();

        if (!initialized) {
            buildColumns();
            initialized = true;
        }

        if (level.isClientSide) {
            spawnParticles();
            return;
        }

        age++;
        int lifetime = ModConfigs.BLAZING.BLAZING_FIRE_LIFETIME.get();
        if (age >= lifetime) {
            remove();
            return;
        }

        applyFireDamage();
    }

    /** 💥 Dégâts verticaux (bloqués seulement par les blocs pleins) */
    private void applyFireDamage() {
        float hearts = ModConfigs.BLAZING.BLAZING_FIRE_DMG_INSIDE_HEARTS.get().floatValue();
        int burnDuration = ModConfigs.BLAZING.BLAZING_BURN_DURATION.get();

        Set<LivingEntity> victims = new HashSet<LivingEntity>();

        for (BlockPos base : columns) {
            for (int dy = 0; dy <= MAX_DAMAGE_HEIGHT; dy++) {
                BlockPos pos = base.above(dy);

                // Seuls les blocs pleins bloquent les dégâts
                if (isFullSolid(pos)) break;

                AxisAlignedBB aabb = new AxisAlignedBB(
                        pos.getX() + 0.1, pos.getY(), pos.getZ() + 0.1,
                        pos.getX() + 0.9, pos.getY() + 1.6, pos.getZ() + 0.9
                );
                victims.addAll(level.getEntitiesOfClass(LivingEntity.class, aabb, LivingEntity::isAlive));
            }
        }

        for (LivingEntity e : victims) {
            e.hurt(BLAZING_FLAME, hearts * 2.0F);
            e.setSecondsOnFire(Math.max(1, burnDuration / 20));
        }
    }

    /** ✅ Retourne vrai seulement pour les blocs complètement solides (pleins) */
    private boolean isFullSolid(BlockPos pos) {
        if (!level.isLoaded(pos)) return true;

        net.minecraft.block.BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level, pos);
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) >= 1.0;
    }

    /** ⚒️ Construit la croix du feu */
    private void buildColumns() {
        columns.clear();
        int length = ModConfigs.BLAZING.BLAZING_FIRE_LENGTH.get();
        int width = Math.max(1, ModConfigs.BLAZING.BLAZING_FIRE_WIDTH.get());
        BlockPos center = this.blockPosition();

        int fromY = (int) Math.ceil(this.getY() + 16);
        Set<Long> seen = new HashSet<Long>();

        // Axe X
        for (int dx = -length; dx <= length; dx++) {
            for (int w = -(width - 1) / 2; w <= width / 2; w++) {
                BlockPos base = new BlockPos(center.getX() + dx, fromY, center.getZ() + w);
                BlockPos top = getVisibleSurface(base, fromY);
                if (seen.add(BlockPos.asLong(top.getX(), top.getY(), top.getZ())))
                    columns.add(top);
            }
        }

        // Axe Z
        for (int dz = -length; dz <= length; dz++) {
            for (int w = -(width - 1) / 2; w <= width / 2; w++) {
                BlockPos base = new BlockPos(center.getX() + w, fromY, center.getZ() + dz);
                BlockPos top = getVisibleSurface(base, fromY);
                if (seen.add(BlockPos.asLong(top.getX(), top.getY(), top.getZ())))
                    columns.add(top);
            }
        }
    }

    /** 🔥 Particules sur tous les blocs visibles (feuilles comprises) */
    private void spawnParticles() {
        Random r = this.level.random;
        for (BlockPos p : columns) {
            double x = p.getX() + 0.5;
            double y = p.getY() + 0.02;
            double z = p.getZ() + 0.5;

            if (r.nextFloat() < 0.85F)
                level.addParticle(net.minecraft.particles.ParticleTypes.FLAME, x, y, z, 0, 0.01, 0);
            if (r.nextFloat() < 0.30F)
                level.addParticle(net.minecraft.particles.ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
        }
    }

    /** 🔎 Trouve la surface visible (corrigée pour feuilles, neige, etc.) */
    private BlockPos getVisibleSurface(BlockPos colXZ, int fromY) {
        Vector3d start = new Vector3d(colXZ.getX() + 0.5, fromY, colXZ.getZ() + 0.5);
        Vector3d end = new Vector3d(colXZ.getX() + 0.5, fromY - 256, colXZ.getZ() + 0.5);

        RayTraceContext ctx = new RayTraceContext(start, end,
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE,
                this);

        RayTraceResult rt = level.clip(ctx);
        if (rt.getType() == RayTraceResult.Type.BLOCK) {
            BlockRayTraceResult br = (BlockRayTraceResult) rt;
            BlockPos hit = br.getBlockPos();
            Direction face = br.getDirection();

            // On se place juste au-dessus du bloc touché
            BlockPos top = (face == Direction.UP) ? hit.above() : hit.relative(face);

            // Vérifie si le bloc touché est une feuille / neige / carpette etc.
            net.minecraft.block.BlockState state = level.getBlockState(hit);
            String name = state.getBlock().getRegistryName() != null
                    ? state.getBlock().getRegistryName().getPath()
                    : "";

            // ✅ Le feu se pose sur tous les blocs, même décoratifs
            if (name.contains("leaves")
                    || name.contains("carpet")
                    || name.contains("snow")
                    || name.contains("grass")
                    || name.contains("moss")
                    || name.contains("vine")
                    || name.contains("bush")
                    || name.contains("flower")
                    || name.contains("log")
                    || name.contains("planks")) {
                return hit.above();
            }

            return top;
        }

        int y = level.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                colXZ.getX(), colXZ.getZ());
        return new BlockPos(colXZ.getX(), y, colXZ.getZ());
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) { this.age = nbt.getInt("Age"); }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) { nbt.putInt("Age", this.age); }

    @Override
    public net.minecraft.network.IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() { return false; }
}
