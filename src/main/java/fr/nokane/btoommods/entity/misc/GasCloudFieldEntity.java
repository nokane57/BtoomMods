package fr.nokane.btoommods.entity.misc;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.particle.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.HashSet;
import java.util.Random;

public class GasCloudFieldEntity extends Entity {

    private int age;
    private boolean droppedShell = false;
    private static final int HURT_PERIOD_TICKS = 10;

    public GasCloudFieldEntity(EntityType<? extends GasCloudFieldEntity> type, World level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        age++;

        // === Maintien sur le terrain ===
        stayAttachedToGround();

        int radius = ModConfigs.GAS.RADIUS.get();
        double dmgHearts = ModConfigs.GAS.GAS_DAMAGE_HEARTH.get();
        double dmgStormHearts = ModConfigs.GAS.GAS_STORM_DAMAGE_HEARTH.get();

        if (level.isClientSide) {
            spawnParticles(radius);
            return;
        }

        // === Dégâts ===
        if (age % HURT_PERIOD_TICKS == 0) {
            boolean isRaining = level.isRainingAt(this.blockPosition());
            double hearts = isRaining ? dmgStormHearts : dmgHearts;
            float dmgHP = (float) (hearts * 2.0 * HURT_PERIOD_TICKS / 20.0);

            double minY = this.getY() + ModConfigs.GAS.GAS_MIN_HEIGHT.get();
            double maxY = this.getY() + ModConfigs.GAS.GAS_MAX_HEIGHT.get();

            AxisAlignedBB aabb = new AxisAlignedBB(
                    getX() - radius, minY, getZ() - radius,
                    getX() + radius, maxY, getZ() + radius
            );

            DamageSource gas = new DamageSource("gas_bim");
            if (ModConfigs.GAS.GAS_DMG_BYPASS_ARMOR.get())
                gas = gas.bypassArmor();

            for (LivingEntity e : new HashSet<>(level.getEntitiesOfClass(LivingEntity.class, aabb, LivingEntity::isAlive))) {
                e.hurt(gas, dmgHP);
            }
        }

        // === Fin de vie ===
        if (age > ModConfigs.GAS.GAS_EXPLODE_AFTER_TICKS.get() + 200) {
            dropDisabledShellOnce();
            remove();
        }
    }

    /**
     * Fait en sorte que le gaz colle au relief du terrain
     * — il ne flotte plus dans les airs.
     */
    private void stayAttachedToGround() {
        // on scanne autour pour trouver la hauteur moyenne du sol sous le gaz
        int radius = 4;
        double totalY = 0.0;
        int samples = 0;

        BlockPos center = this.blockPosition();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos check = center.offset(dx, 0, dz);
                double groundY = findGroundY(check);
                if (!Double.isNaN(groundY)) {
                    totalY += groundY;
                    samples++;
                }
            }
        }

        if (samples > 0) {
            double avgGroundY = totalY / samples;
            double targetY = avgGroundY + 0.75; // un peu au-dessus du sol
            this.setPos(this.getX(), targetY, this.getZ());
        }
    }

    /**
     * Retourne la hauteur Y du sol sous une position donnée.
     */
    private double findGroundY(BlockPos pos) {
        // Descend jusqu’à trouver un bloc solide
        for (int y = pos.getY(); y >= 0; y--) {
            BlockPos p = new BlockPos(pos.getX(), y, pos.getZ());
            VoxelShape shape = level.getBlockState(p).getCollisionShape(level, p);
            if (!shape.isEmpty()) {
                return y + shape.max(Direction.Axis.Y);
            }
        }
        // rien trouvé (vide, au-dessus du vide)
        return Double.NaN;
    }

    private void spawnParticles(int radius) {
        Random r = this.level.random;
        BlockPos c = this.blockPosition();
        int samples = 80 + radius * 4;

        for (int i = 0; i < samples; i++) {
            double ang = r.nextDouble() * Math.PI * 2.0;
            double rad = r.nextDouble() * radius;
            double px = c.getX() + 0.5 + Math.cos(ang) * rad;
            double pz = c.getZ() + 0.5 + Math.sin(ang) * rad;

            // on aligne aussi la hauteur de la particule sur le relief
            double groundY = findGroundY(new BlockPos(px, this.getY(), pz));
            double py = (Double.isNaN(groundY) ? this.getY() : groundY + 0.75);

            level.addParticle(ModParticles.GAS_CLOUD.get(), px, py, pz, 0.0, 0.002, 0.0);
        }
    }

    private void dropDisabledShellOnce() {
        if (droppedShell || level.isClientSide) return;
        ItemEntity drop = new ItemEntity(level, this.getX(), this.getY(), this.getZ(),
                ModItems.GAS_BIM_DISABLED.get().getDefaultInstance());
        level.addFreshEntity(drop);
        droppedShell = true;
    }

    @Override protected void readAdditionalSaveData(CompoundNBT nbt) { age = nbt.getInt("Age"); }
    @Override protected void addAdditionalSaveData(CompoundNBT nbt) { nbt.putInt("Age", age); }
    @Override public net.minecraft.network.IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean isPickable() { return false; }
}
