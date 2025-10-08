// fr/nokane/btoommods/entity/misc/GasCloudFieldEntity.java
package fr.nokane.btoommods.entity.misc;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.particle.ModParticles;
import fr.nokane.btoommods.sound.ModSounds;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.HashSet;
import java.util.Random;

public class GasCloudFieldEntity extends Entity {

    private int age;
    private boolean droppedShell = false;
    private boolean spawnSoundPlayed = false;

    private static final int HURT_PERIOD_TICKS = 10;

    private double radius = 1.0;     // s’étend jusqu’à RADIUS
    private double alphaFactor = 1.0; // sert à atténuer le visuel et les dégâts en fin de vie

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

        // ▶️ Son à l'apparition (une seule fois, côté serveur → entendu par tous)
        if (!spawnSoundPlayed && !level.isClientSide) {
            spawnSoundPlayed = true;
            SoundUtils.playWorldSound(level, getX(), getY(), getZ(), ModSounds.GAS_ITEM.get());
        }

        // Durée de vie = délai d'apparition du nuage + 200 (comme avant)
        final int lifetime = ModConfigs.GAS.GAS_EXPLODE_AFTER_TICKS.get() + 200;
        double lifeProgress = Math.min((double) age / lifetime, 1.0);

        // 🎛️ Expansion → plateau → dissipation (lisser visuel + dégâts)
        int baseRadius = ModConfigs.GAS.RADIUS.get();
        if (lifeProgress < 0.25) {
            radius = baseRadius * (lifeProgress / 0.25); // 0 → R en 25% du temps
            alphaFactor = 1.0;
        } else if (lifeProgress < 0.75) {
            radius = baseRadius;
            alphaFactor = 1.0;
        } else {
            double t = (lifeProgress - 0.75) / 0.25; // 0 → 1
            double inv = 1.0 - t;
            radius = baseRadius * (0.6 + 0.4 * inv); // réduit un peu le disque
            alphaFactor = inv;                       // fade out
        }

        // Suit le relief local
        stayAttachedToGround();

        if (level.isClientSide) {
            spawnParticles(); // visuel seulement
            return;
        }

        // Dégâts (toutes les 0.5s)
        if (age % HURT_PERIOD_TICKS == 0) {
            applyGasDamage();
        }

        // Fin de vie
        if (age > lifetime) {
            dropDisabledShellOnce();
            remove();
        }
    }

    /** Calage de la hauteur du centre sur le relief (moyenne locale) */
    private void stayAttachedToGround() {
        int checkRadius = (int) Math.min(4, radius);
        double totalY = 0.0;
        int samples = 0;

        BlockPos center = this.blockPosition();
        for (int dx = -checkRadius; dx <= checkRadius; dx++) {
            for (int dz = -checkRadius; dz <= checkRadius; dz++) {
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
            this.setPos(this.getX(), avgGroundY + 0.75, this.getZ());
        }
    }

    private double findGroundY(BlockPos pos) {
        for (int y = pos.getY(); y >= 0; y--) {
            BlockPos p = new BlockPos(pos.getX(), y, pos.getZ());
            VoxelShape shape = level.getBlockState(p).getCollisionShape(level, p);
            if (!shape.isEmpty()) {
                return y + shape.max(Direction.Axis.Y);
            }
        }
        return Double.NaN;
    }

    /** Dégâts appliqués sur tout le volume du gaz (hauteur + largeur) */
    private void applyGasDamage() {
        boolean raining = level.isRainingAt(this.blockPosition());
        double heartsPerSec = raining
                ? ModConfigs.GAS.GAS_STORM_DAMAGE_HEARTH.get()
                : ModConfigs.GAS.GAS_DAMAGE_HEARTH.get();

        // atténuation en fin de vie
        heartsPerSec *= alphaFactor;

        // dégâts en HP pour ce tick
        float dmgHP = (float) (heartsPerSec * 2.0 * HURT_PERIOD_TICKS / 20.0);

        // baseY = sol moyen sous le centre du gaz
        double groundY = findGroundY(this.blockPosition());
        if (Double.isNaN(groundY)) groundY = this.getY();

        // récupère les hauteurs configurées du gaz
        double minH = ModConfigs.GAS.GAS_MIN_HEIGHT.get();
        double maxH = ModConfigs.GAS.GAS_MAX_HEIGHT.get();

        // étend vers le haut ET vers le bas (pour terrain vallonné)
        double minY = groundY - minH;
        double maxY = groundY + maxH;
        double margin = 0.5;

        AxisAlignedBB aabb = new AxisAlignedBB(
                getX() - radius - margin, minY,
                getZ() - radius - margin,
                getX() + radius + margin, maxY,
                getZ() + radius + margin
        );

        DamageSource gas = new DamageSource("gas_bim");
        if (ModConfigs.GAS.GAS_DMG_BYPASS_ARMOR.get()) {
            gas = gas.bypassArmor();
        }

        for (LivingEntity e : new HashSet<>(level.getEntitiesOfClass(LivingEntity.class, aabb, LivingEntity::isAlive))) {
            // distance horizontale seulement
            double dx = e.getX() - this.getX();
            double dz = e.getZ() - this.getZ();
            if ((dx * dx + dz * dz) <= (radius * radius)) {
                e.hurt(gas, dmgHP);
            }
        }
    }


    /** Visuel : particules légères posées au sol dans le disque */
    private void spawnParticles() {
        Random r = this.level.random;
        int samples = (int) ((70 + radius * 10) * alphaFactor);

        for (int i = 0; i < samples; i++) {
            double angle = r.nextDouble() * Math.PI * 2.0;
            double dist  = r.nextDouble() * radius;
            double px = this.getX() + Math.cos(angle) * dist;
            double pz = this.getZ() + Math.sin(angle) * dist;

            double groundY = findGroundY(new BlockPos(px, this.getY(), pz));
            double py = Double.isNaN(groundY) ? this.getY() : groundY + 0.70;

            double vy = (r.nextDouble() - 0.5) * 0.002 * alphaFactor;
            level.addParticle(ModParticles.GAS_CLOUD.get(), px, py, pz, 0.0, vy, 0.0);
        }
    }

    private void dropDisabledShellOnce() {
        if (droppedShell || level.isClientSide) return;
        ItemEntity drop = new ItemEntity(level, this.getX(), this.getY(), this.getZ(),
                ModItems.GAS_BIM_DISABLED.get().getDefaultInstance());
        level.addFreshEntity(drop);
        droppedShell = true;
    }

    @Override protected void readAdditionalSaveData(CompoundNBT nbt) {
        age = nbt.getInt("Age");
        radius = nbt.getDouble("Radius");
    }

    @Override protected void addAdditionalSaveData(CompoundNBT nbt) {
        nbt.putInt("Age", age);
        nbt.putDouble("Radius", radius);
    }

    @Override public net.minecraft.network.IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override public boolean isPickable() { return false; }
}
