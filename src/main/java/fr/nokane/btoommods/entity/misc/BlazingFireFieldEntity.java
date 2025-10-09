package fr.nokane.btoommods.entity.misc;

import fr.nokane.btoommods.config.ModConfigs;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.*;

/**
 * Champ de feu du Blazing BIM :
 * - Suit le relief du terrain
 * - Descend si le bloc en dessous est cassé
 * - Inflige des dégâts verticaux
 */
public class BlazingFireFieldEntity extends Entity {

    private int age;
    private static final DamageSource BLAZING_FLAME =
            (new DamageSource("blazing_flame")).setIsFire();

    private final List<BlockPos> columns = new ArrayList<>();
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

        // 🔥 Le feu s’ajuste même côté client (pour suivre les blocs cassés)
        adjustFirePositions();

        if (level.isClientSide) {
            spawnParticles();
            return;
        }

        // ⏳ Durée de vie
        age++;
        int lifetime = ModConfigs.BLAZING.BLAZING_FIRE_LIFETIME.get();
        if (age >= lifetime) {
            remove();
            return;
        }

        // 💥 Dégâts
        applyFireDamage();
    }

    /** 🔥 Ajuste la hauteur du feu si le bloc du dessous disparaît */
    private void adjustFirePositions() {
        for (int i = 0; i < columns.size(); i++) {
            BlockPos p = columns.get(i);
            BlockPos below = p.below();

            // si le bloc sous le feu devient vide, on descend
            if (level.isEmptyBlock(below) && p.getY() > 0) {
                columns.set(i, below);
            }
        }
    }

    /** 💥 Dégâts verticaux (bloqués seulement par les blocs pleins) */
    private void applyFireDamage() {
        float hearts = ModConfigs.BLAZING.BLAZING_FIRE_DMG_INSIDE_HEARTS.get().floatValue();
        int burnDuration = ModConfigs.BLAZING.BLAZING_BURN_DURATION.get();

        Set<LivingEntity> victims = new HashSet<>();

        for (BlockPos base : columns) {
            for (int dy = 0; dy <= MAX_DAMAGE_HEIGHT; dy++) {
                BlockPos pos = base.above(dy);

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

    /** ✅ Détermine si un bloc bloque les dégâts */
    private boolean isFullSolid(BlockPos pos) {
        if (!level.isLoaded(pos)) return true;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;

        String name = state.getBlock().getRegistryName() != null
                ? state.getBlock().getRegistryName().getPath()
                : "";

        // les blocs décoratifs ne bloquent pas le feu
        if (name.contains("leaves") || name.contains("grass") || name.contains("carpet") ||
                name.contains("moss") || name.contains("vine") || name.contains("log") ||
                name.contains("planks") || name.contains("bush") || name.contains("flower") ||
                name.contains("snow"))
            return false;

        VoxelShape shape = state.getCollisionShape(level, pos);
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) >= 1.0;
    }

    /** ⚒️ Construit la croix de feu alignée avec le sol */
    private void buildColumns() {
        columns.clear();
        int length = ModConfigs.BLAZING.BLAZING_FIRE_LENGTH.get();
        int width = Math.max(1, ModConfigs.BLAZING.BLAZING_FIRE_WIDTH.get());
        BlockPos center = this.blockPosition();
        Set<Long> seen = new HashSet<>();

        for (int dx = -length; dx <= length; dx++) {
            for (int w = -(width - 1) / 2; w <= width / 2; w++) {
                BlockPos base = new BlockPos(center.getX() + dx, center.getY(), center.getZ() + w);
                BlockPos ground = findSafeSurface(base);
                if (seen.add(ground.asLong())) columns.add(ground);
            }
        }

        for (int dz = -length; dz <= length; dz++) {
            for (int w = -(width - 1) / 2; w <= width / 2; w++) {
                BlockPos base = new BlockPos(center.getX() + w, center.getY(), center.getZ() + dz);
                BlockPos ground = findSafeSurface(base);
                if (seen.add(ground.asLong())) columns.add(ground);
            }
        }
    }

    /** 🔽 Trouve une surface où poser le feu sans passer sous le sol */
    private BlockPos findSafeSurface(BlockPos start) {
        BlockPos.Mutable pos = new BlockPos.Mutable(start.getX(), start.getY(), start.getZ());
        int minY = 0;

        for (int i = 0; i < 32 && pos.getY() > minY; i++) {
            BlockState state = level.getBlockState(pos);
            if (isSurfaceBlock(state)) return pos.above();
            pos.move(Direction.DOWN);
        }

        return start;
    }

    /** ✅ Bloc acceptable pour poser le feu */
    private boolean isSurfaceBlock(BlockState state) {
        if (state.isAir()) return false;
        String name = state.getBlock().getRegistryName() != null
                ? state.getBlock().getRegistryName().getPath()
                : "";

        return name.contains("leaves") || name.contains("grass") || name.contains("snow") ||
                name.contains("carpet") || name.contains("moss") || name.contains("vine") ||
                name.contains("log") || name.contains("planks") || name.contains("bush") ||
                name.contains("flower") || name.contains("dirt") || name.contains("stone");
    }

    /** 🔥 Particules visuelles */
    /** 🔥 Particules visuelles avec hauteur précise selon le bloc */
    private void spawnParticles() {
        Random r = this.level.random;

        for (int i = 0; i < columns.size(); i++) {
            BlockPos p = columns.get(i);

            // 💨 Ajustement dynamique (si le bloc en dessous est cassé)
            BlockPos below = p.below();
            if (level.isEmptyBlock(below) && p.getY() > 0) {
                columns.set(i, below);
                p = below;
            }

            // 🔹 Récupère la hauteur réelle du bloc
            BlockState state = level.getBlockState(p.below());
            double shapeTop = 1.0;
            if (!state.isAir()) {
                VoxelShape shape = state.getCollisionShape(level, p.below());
                if (!shape.isEmpty()) {
                    shapeTop = shape.max(Direction.Axis.Y);
                }
            }

            // 🔥 Position de la flamme : pile au sommet du bloc
            double x = p.getX() + 0.5;
            double y = p.below().getY() + shapeTop + 0.01; // juste au-dessus du bloc réel
            double z = p.getZ() + 0.5;

            if (r.nextFloat() < 0.85F)
                level.addParticle(net.minecraft.particles.ParticleTypes.FLAME, x, y, z, 0, 0.01, 0);
            if (r.nextFloat() < 0.30F)
                level.addParticle(net.minecraft.particles.ParticleTypes.SMOKE, x, y, z, 0, 0.02, 0);
        }
    }


    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        this.age = nbt.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        nbt.putInt("Age", this.age);
    }

    @Override
    public net.minecraft.network.IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
