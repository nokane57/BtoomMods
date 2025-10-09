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
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.*;

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

        // Construction initiale du tapis
        if (!initialized) {
            buildColumns();
            initialized = true;
        }

        if (level.isClientSide) {
            spawnParticles();
            return;
        }

        // ⏳ Durée de vie configurée
        age++;
        int lifetime = ModConfigs.BLAZING.BLAZING_FIRE_LIFETIME.get();
        if (age >= lifetime) {
            remove();
            return;
        }

        // 🔥 Suivi du terrain : le feu descend si le bloc sous lui disparaît
        adjustFirePositions();

        // 💥 Dégâts
        applyFireDamage();
    }

    /** 🔥 Ajuste la hauteur du feu si le sol disparaît */
    private void adjustFirePositions() {
        for (int i = 0; i < columns.size(); i++) {
            BlockPos p = columns.get(i);
            BlockPos below = p.below();

            // Si le bloc du dessous est vide, on descend
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

    private boolean isFullSolid(BlockPos pos) {
        if (!level.isLoaded(pos)) return true;
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level, pos);
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) >= 1.0;
    }

    /** ⚒️ Construit la croix de feu alignée avec le sol (corrigée) */
    private void buildColumns() {
        columns.clear();
        int length = ModConfigs.BLAZING.BLAZING_FIRE_LENGTH.get();
        int width = Math.max(1, ModConfigs.BLAZING.BLAZING_FIRE_WIDTH.get());
        BlockPos center = this.blockPosition();
        Set<Long> seen = new HashSet<>();

        for (int dx = -length; dx <= length; dx++) {
            for (int w = -(width - 1) / 2; w <= width / 2; w++) {
                BlockPos base = new BlockPos(center.getX() + dx, center.getY(), center.getZ() + w);
                BlockPos ground = findGroundBelow(base);
                if (seen.add(ground.asLong())) columns.add(ground);
            }
        }

        for (int dz = -length; dz <= length; dz++) {
            for (int w = -(width - 1) / 2; w <= width / 2; w++) {
                BlockPos base = new BlockPos(center.getX() + w, center.getY(), center.getZ() + dz);
                BlockPos ground = findGroundBelow(base);
                if (seen.add(ground.asLong())) columns.add(ground);
            }
        }
    }

    /** 🔽 Trouve le premier bloc solide en descendant depuis une position */
    private BlockPos findGroundBelow(BlockPos start) {
        BlockPos.Mutable pos = new BlockPos.Mutable(start.getX(), start.getY(), start.getZ());
        for (int i = 0; i < 32; i++) { // sécurité
            if (!level.isEmptyBlock(pos) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() == false)
                return pos.above(); // place le feu au-dessus du bloc trouvé
            pos.move(Direction.DOWN);
        }
        return start; // si rien trouvé, garde la position d’origine
    }


    /** 🔥 Particules visuelles */
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
