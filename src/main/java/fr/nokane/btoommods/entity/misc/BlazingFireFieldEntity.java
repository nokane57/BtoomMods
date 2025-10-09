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
 * - Suit le relief du terrain + redescend si le support disparaît
 * - Dégâts verticaux (5 blocs) ; peut ignorer les blocs selon la config
 */
public class BlazingFireFieldEntity extends Entity {

    private int age;
    private static final DamageSource BLAZING_FLAME =
            (new DamageSource("blazing_flame")).setIsFire();

    private final List<BlockPos> columns = new ArrayList<>();
    private boolean initialized = false;

    private static final int MAX_DAMAGE_HEIGHT = 5;   // hauteur d’effet
    private static final int PARTICLE_OFFSET_EPS = 0; // pour éviter les accumulations

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

        // 🔧 ajuste la position des colonnes (client + serveur)
        adjustFirePositions();

        if (level.isClientSide) {
            spawnParticles();
            return;
        }

        // ⏳ durée de vie
        age++;
        if (age >= ModConfigs.BLAZING.BLAZING_FIRE_LIFETIME.get()) {
            remove();
            return;
        }

        // 💥 dégâts
        applyFireDamage();
    }

    /** 🔥 Descend tant que le bloc support n’a pas de collision (ou est vide) */
    private void adjustFirePositions() {
        for (int i = 0; i < columns.size(); i++) {
            BlockPos p = columns.get(i);

            // boucle pour rattraper plusieurs blocs cassés d’un coup
            while (p.getY() > 0) {
                BlockPos below = p.below();
                BlockState stateBelow = level.getBlockState(below);
                VoxelShape shape = stateBelow.getCollisionShape(level, below);

                // si pas de collision → on descend
                if (stateBelow.isAir() || shape.isEmpty()) {
                    p = below;
                    continue;
                }
                break;
            }

            columns.set(i, p);
        }
    }

    /** 💥 Dégâts verticaux – traversent ou non les blocs selon la config */
    private void applyFireDamage() {
        final boolean throughBlocks = ModConfigs.BLAZING.FIRE_DAMAGE_THROUGH_BLOCKS.get();
        final float hearts = ModConfigs.BLAZING.BLAZING_FIRE_DMG_INSIDE_HEARTS.get().floatValue();
        final int burnDuration = ModConfigs.BLAZING.BLAZING_BURN_DURATION.get();

        Set<LivingEntity> victims = new HashSet<>();

        for (BlockPos base : columns) {
            for (int dy = 0; dy <= MAX_DAMAGE_HEIGHT; dy++) {
                BlockPos pos = base.above(dy);

                // si on ne traverse pas les blocs → on stoppe à la 1ère collision pleine
                if (!throughBlocks && isFullSolid(pos)) break;

                AxisAlignedBB aabb = new AxisAlignedBB(
                        pos.getX() + 0.1, pos.getY(), pos.getZ() + 0.1,
                        pos.getX() + 0.9, pos.getY() + 1.6, pos.getZ() + 0.9
                );
                victims.addAll(level.getEntitiesOfClass(LivingEntity.class, aabb, LivingEntity::isAlive));
            }
        }

        for (LivingEntity e : victims) {
            e.hurt(BLAZING_FLAME, hearts * 2.0F); // 1 cœur = 2 HP
            e.setSecondsOnFire(Math.max(1, burnDuration / 20));
        }
    }

    /** ✅ "plein" = collision shape non vide et hauteur >= 1 bloc */
    private boolean isFullSolid(BlockPos pos) {
        if (!level.isLoaded(pos)) return true;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;

        // certains blocs décoratifs ne doivent pas bloquer
        String name = state.getBlock().getRegistryName() != null
                ? state.getBlock().getRegistryName().getPath()
                : "";
        if (name.contains("leaves") || name.contains("grass") || name.contains("carpet") ||
                name.contains("moss") || name.contains("vine") || name.contains("log") ||
                name.contains("planks") || name.contains("bush") || name.contains("flower") ||
                name.contains("snow"))
            return false;

        VoxelShape shape = state.getCollisionShape(level, pos);
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) >= 1.0;
    }

    /** ⚒️ Construit la croix de feu alignée avec le sol (trouve un support “solide”) */
    private void buildColumns() {
        columns.clear();
        int length = ModConfigs.BLAZING.BLAZING_FIRE_LENGTH.get();
        int width = Math.max(1, ModConfigs.BLAZING.BLAZING_FIRE_WIDTH.get());
        BlockPos center = this.blockPosition();
        Set<Long> seen = new HashSet<>();

        // Axe X
        for (int dx = -length; dx <= length; dx++) {
            for (int w = -(width - 1) / 2; w <= width / 2; w++) {
                BlockPos base = new BlockPos(center.getX() + dx, center.getY(), center.getZ() + w);
                BlockPos ground = findSafeSurface(base);
                if (seen.add(ground.asLong())) columns.add(ground);
            }
        }

        // Axe Z
        for (int dz = -length; dz <= length; dz++) {
            for (int w = -(width - 1) / 2; w <= width / 2; w++) {
                BlockPos base = new BlockPos(center.getX() + w, center.getY(), center.getZ() + dz);
                BlockPos ground = findSafeSurface(base);
                if (seen.add(ground.asLong())) columns.add(ground);
            }
        }
    }

    /** 🔽 Trouve une surface où poser le feu (premier bloc avec collision) */
    private BlockPos findSafeSurface(BlockPos start) {
        BlockPos.Mutable pos = new BlockPos.Mutable(start.getX(), start.getY(), start.getZ());
        for (int i = 0; i < 32 && pos.getY() > 0; i++) {
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (!state.isAir() && !shape.isEmpty()) {
                return pos.above(); // poser juste au-dessus du support réel
            }
            pos.move(Direction.DOWN);
        }
        return start;
    }

    /** 🔥 Particules visuelles posées exactement au sommet du bloc support */
    private void spawnParticles() {
        Random r = this.level.random;

        for (int i = 0; i < columns.size(); i++) {
            BlockPos p = columns.get(i);

            // recalcule la position (au cas où ça bouge ce tick)
            BlockPos below = p.below();
            BlockState state = level.getBlockState(below);
            double top = below.getY() + 1.0;
            VoxelShape shape = state.getCollisionShape(level, below);
            if (!state.isAir() && !shape.isEmpty()) {
                top = below.getY() + shape.max(Direction.Axis.Y);
            }

            double x = p.getX() + 0.5;
            double y = top + 0.01;
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
    public boolean isPickable() { return false; }
}
