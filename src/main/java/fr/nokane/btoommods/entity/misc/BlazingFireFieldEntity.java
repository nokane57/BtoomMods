package fr.nokane.btoommods.entity.misc;

import fr.nokane.btoommods.config.ModConfigs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.*;

/**
 * Entité temporaire représentant un champ de feu en croix.
 * Elle inflige des dégâts et applique le feu aux entités à proximité.
 */
public class BlazingFireFieldEntity extends Entity {
    private int age;
    private static final DamageSource BLAZING_FLAME = (new DamageSource("blazing_flame")).setIsFire();

    /** Colonnes (X,Z,Y) qui composent la croix du champ de feu */
    private final List<BlockPos> columns = new ArrayList<>();
    private boolean initialized = false;

    public BlazingFireFieldEntity(EntityType<? extends BlazingFireFieldEntity> type, World level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();

        if (!initialized) {
            buildColumns(); // construit la géométrie du champ (une seule fois)
            initialized = true;
        }

        // --- Client : uniquement les particules ---
        if (level.isClientSide) {
            spawnParticles();
            return;
        }

        // --- Serveur : logique de dégâts ---
        age++;
        int lifetime = ModConfigs.BLAZING.BLAZING_FIRE_LIFETIME.get();
        if (age >= lifetime) {
            remove();
            return;
        }

        float insideHearts = ModConfigs.BLAZING.BLAZING_FIRE_DMG_INSIDE_HEARTS.get().floatValue();
        int burnDuration = ModConfigs.BLAZING.BLAZING_BURN_DURATION.get();

        // On tape les entités sur chaque colonne (au sol)
        Set<LivingEntity> victims = new HashSet<>();
        for (BlockPos p : columns) {
            AxisAlignedBB aabb = new AxisAlignedBB(
                    p.getX() + 0.1, p.getY(), p.getZ() + 0.1,
                    p.getX() + 0.9, p.getY() + 1.6, p.getZ() + 0.9
            );
            victims.addAll(level.getEntitiesOfClass(LivingEntity.class, aabb, LivingEntity::isAlive));
        }

        for (LivingEntity e : victims) {
            e.hurt(BLAZING_FLAME, insideHearts * 2.0F); // 1 cœur = 2 HP
            e.setSecondsOnFire(Math.max(1, burnDuration / 20));
        }
    }

    /** Construit la croix de feu alignée au sol. */
    private void buildColumns() {
        columns.clear();

        int length = ModConfigs.BLAZING.BLAZING_FIRE_LENGTH.get();
        int width  = Math.max(1, ModConfigs.BLAZING.BLAZING_FIRE_WIDTH.get());
        BlockPos center = this.blockPosition();

        int fromY = (int) Math.ceil(this.getY() + 16);
        Set<Long> seen = new HashSet<>();

        // --- Bras X ---
        for (int dx = -length; dx <= length; dx++) {
            for (int w = -(width - 1) / 2; w <= width / 2; w++) {
                BlockPos base = new BlockPos(center.getX() + dx, fromY, center.getZ() + w);
                BlockPos top = snapColumnToTopSolid(base, fromY);
                long key = BlockPos.asLong(top.getX(), top.getY(), top.getZ());
                if (seen.add(key)) columns.add(top);
            }
        }

        // --- Bras Z ---
        for (int dz = -length; dz <= length; dz++) {
            for (int w = -(width - 1) / 2; w <= width / 2; w++) {
                BlockPos base = new BlockPos(center.getX() + w, fromY, center.getZ() + dz);
                BlockPos top = snapColumnToTopSolid(base, fromY);
                long key = BlockPos.asLong(top.getX(), top.getY(), top.getZ());
                if (seen.add(key)) columns.add(top);
            }
        }
    }

    /** Génère des particules visuelles sur chaque colonne. */
    private void spawnParticles() {
        Random r = this.level.random;
        for (BlockPos p : columns) {
            double x = p.getX() + 0.5;
            double y = p.getY() + 0.01;
            double z = p.getZ() + 0.5;

            if (r.nextFloat() < 0.85F)
                level.addParticle(net.minecraft.particles.ParticleTypes.FLAME, x, y, z, 0, 0.01, 0);
            if (r.nextFloat() < 0.30F)
                level.addParticle(net.minecraft.particles.ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.02, 0);
        }
    }

    /** Raycast vertical ↓ + fallback heightmap : renvoie le bloc au-dessus du sol solide. */
    private BlockPos snapColumnToTopSolid(BlockPos colXZ, int fromY) {
        Vector3d start = new Vector3d(colXZ.getX() + 0.5, fromY, colXZ.getZ() + 0.5);
        Vector3d end   = new Vector3d(colXZ.getX() + 0.5, fromY - 256, colXZ.getZ() + 0.5);

        RayTraceContext ctx = new RayTraceContext(
                start, end,
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE,
                this
        );
        RayTraceResult rt = level.clip(ctx);

        if (rt.getType() == RayTraceResult.Type.BLOCK) {
            BlockRayTraceResult br = (BlockRayTraceResult) rt;
            BlockPos hit = br.getBlockPos();
            Direction face = br.getDirection();
            BlockPos top = (face == Direction.UP) ? hit.above() : hit.relative(face);
            if (level.getBlockState(top.below()).isFaceSturdy(level, top.below(), Direction.UP)) {
                return top;
            }
        }

        int y = level.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, colXZ.getX(), colXZ.getZ());
        return new BlockPos(colXZ.getX(), y, colXZ.getZ());
    }

    @Override protected void readAdditionalSaveData(CompoundNBT nbt) { this.age = nbt.getInt("Age"); }
    @Override protected void addAdditionalSaveData(CompoundNBT nbt)   { nbt.putInt("Age", this.age); }

    @Override
    public net.minecraft.network.IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
