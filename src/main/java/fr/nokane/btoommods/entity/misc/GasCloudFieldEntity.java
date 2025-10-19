package fr.nokane.btoommods.entity.misc;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.particle.ModParticles;
import fr.nokane.btoommods.sound.ModSounds;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.HashSet;
import java.util.Random;

/**
 * Gaz BIM volumétrique :
 * - SOL : dôme collé au terrain (monte: max_height, descend: min_height)
 * - AIR : volume flottant centré sur l’épicentre (monte/descend sans clamp au sol)
 * - Expansion progressive + dissipation fluide
 * - Densité et dégâts synchronisés avec le visuel
 */
public class GasCloudFieldEntity extends Entity {

    private static final int HURT_PERIOD_TICKS = 10;

    // ---- Sync réseau (TRÈS IMPORTANT)
    private static final DataParameter<Boolean> DATA_AIRBORNE =
            EntityDataManager.defineId(GasCloudFieldEntity.class, DataSerializers.BOOLEAN);

    // Pas de DOUBLE dans les DataSerializers 1.16 → on passe par FLOAT
    private static final DataParameter<Float> DATA_ORIGIN_Y =
            EntityDataManager.defineId(GasCloudFieldEntity.class, DataSerializers.FLOAT);

    private int age;
    private boolean droppedShell = false;
    private boolean spawnSoundPlayed = false;

    // état dynamique
    private double currentRadius = 1.0;
    private double currentUp     = 1.0;
    private double currentDown   = 1.0;
    private double alphaFactor   = 1.0;
    private double spreadSpeed   = 1.0;

    // contexte
    private double explosionOriginY = Double.NaN; // côté logique, on garde en double
    private boolean airborne = false;

    public GasCloudFieldEntity(EntityType<? extends GasCloudFieldEntity> type, World level) {
        super(type, level);
        this.noPhysics = true;
        this.spreadSpeed = ModConfigs.GAS.GAS_SPREAD_SPEED.get();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_AIRBORNE, false);
        this.entityData.define(DATA_ORIGIN_Y, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        age++;

        // --- Lecture des valeurs synchronisées côté client
        if (level.isClientSide) {
            this.airborne = this.entityData.get(DATA_AIRBORNE);
            // On fait confiance à la valeur synchronisée (toujours l’épicentre exact)
            this.explosionOriginY = (double) this.entityData.get(DATA_ORIGIN_Y);
        }

        // Son d’apparition (serveur uniquement)
        if (!spawnSoundPlayed && !level.isClientSide) {
            spawnSoundPlayed = true;
            SoundUtils.playWorldSound(level, getX(), getY(), getZ(),
                    ModSounds.GAS_ITEM.get(), SoundUtils.VOL_GAS, 1.3F);
        }

        // Configs
        final int lifetime    = Math.max(20, ModConfigs.GAS.GAS_LIFETIME_TICKS.get());
        final double baseR    = Math.max(1.0,  ModConfigs.GAS.RADIUS.get());
        final double minHConf = Math.max(0.0,  ModConfigs.GAS.GAS_MIN_HEIGHT.get());
        final double maxHConf = Math.max(0.0,  ModConfigs.GAS.GAS_MAX_HEIGHT.get());
        final boolean bypass  = ModConfigs.GAS.GAS_DMG_BYPASS_ARMOR.get();

        // Progression vie (prise en compte spreadSpeed)
        double lifeProgress = Math.min((double) age / lifetime, 1.0);
        double t = Math.min(lifeProgress * Math.max(0.05, spreadSpeed), 1.0);

        // Expansion / plateau / dissipation
        if (t < 0.3) {
            double f = t / 0.3;
            currentRadius = baseR * f;
            currentUp     = maxHConf * f;
            currentDown   = minHConf * f;
            alphaFactor   = 1.0;
        } else if (t < 0.8) {
            currentRadius = baseR;
            currentUp     = maxHConf;
            currentDown   = minHConf;
            alphaFactor   = 1.0;
        } else {
            double u = (t - 0.8) / 0.2;
            double inv = 1.0 - u;
            currentRadius = baseR * (0.5 + 0.5 * inv);
            currentUp     = maxHConf * (0.5 + 0.5 * inv);
            currentDown   = minHConf * (0.5 + 0.5 * inv);
            alphaFactor   = Math.max(0.05, inv);
        }

        // Client → particules
        if (level.isClientSide) {
            if (airborne) spawnParticlesAirVolume();
            else          spawnParticlesGroundDome();
            return;
        }

        // Serveur → dégâts
        if (age % HURT_PERIOD_TICKS == 0) {
            double heartsPerSec = level.isRainingAt(this.blockPosition())
                    ? ModConfigs.GAS.GAS_STORM_DAMAGE_HEARTH.get()
                    : ModConfigs.GAS.GAS_DAMAGE_HEARTH.get();
            heartsPerSec *= Math.max(0.25, alphaFactor);
            float dmgHP = (float) (heartsPerSec * 2.0 * HURT_PERIOD_TICKS / 20.0);

            if (airborne) applyDamageAirVolume(dmgHP, bypass);
            else          applyDamageGroundDome(dmgHP, bypass);
        }

        // Fin de vie
        if (age > lifetime) {
            dropDisabledShellOnce();
            remove();
        }
    }

    /* ============================
       PARTICULES — AU SOL
       ============================ */
    private void spawnParticlesGroundDome() {
        Random r = this.level.random;
        int samples = (int) ((60 + currentRadius * 10) * Math.max(0.25, alphaFactor));

        final double cx = this.getX();
        final double cz = this.getZ();

        for (int i = 0; i < samples; i++) {
            double angle = r.nextDouble() * Math.PI * 2.0;
            double dist  = Math.sqrt(r.nextDouble()) * currentRadius;
            double px = cx + Math.cos(angle) * dist;
            double pz = cz + Math.sin(angle) * dist;

            double groundY = findGroundY(new BlockPos(px, this.getY(), pz));
            if (Double.isNaN(groundY)) groundY = this.getY();

            double ratio = dist / currentRadius;
            double localUp = currentUp * Math.sqrt(Math.max(0.0, 1.0 - ratio * ratio));

            double pyMin = groundY - currentDown;
            double pyMax = groundY + localUp;

            double py = pyMin + r.nextDouble() * Math.max(0.05, (pyMax - pyMin));
            double vy = (r.nextDouble() - 0.5) * 0.002 * Math.max(0.2, alphaFactor);
            vy -= 0.0004;

            level.addParticle(ModParticles.GAS_CLOUD.get(), px, py, pz, 0.0, vy, 0.0);
        }
    }

    /* ============================
       PARTICULES — DANS L’AIR
       ============================ */
    private void spawnParticlesAirVolume() {
        Random r = this.level.random;
        int samples = (int) ((60 + currentRadius * 10) * Math.max(0.25, alphaFactor));

        final double cx = this.getX();
        final double cz = this.getZ();
        // IMPORTANT : pas de fallback au sol → on utilise le Y synchronisé
        final double cy = this.explosionOriginY;

        for (int i = 0; i < samples; i++) {
            double angle = r.nextDouble() * Math.PI * 2.0;
            double dist  = Math.sqrt(r.nextDouble()) * currentRadius;
            double px = cx + Math.cos(angle) * dist;
            double pz = cz + Math.sin(angle) * dist;

            double ratio = dist / currentRadius;
            double localUp   = currentUp   * Math.sqrt(Math.max(0.0, 1.0 - ratio * ratio));
            double localDown = currentDown * Math.sqrt(Math.max(0.0, 1.0 - ratio * ratio));

            double pyMin = cy - localDown;
            double pyMax = cy + localUp;
            double py = pyMin + r.nextDouble() * Math.max(0.05, (pyMax - pyMin));

            double vy = -(0.00025 + r.nextDouble() * 0.0006);
            level.addParticle(ModParticles.GAS_CLOUD.get(), px, py, pz, 0.0, vy, 0.0);
        }
    }

    /* ============================
       DÉGÂTS — AU SOL
       ============================ */
    private void applyDamageGroundDome(float dmgHP, boolean bypassArmor) {
        final double cx = this.getX();
        final double cz = this.getZ();

        DamageSource gas = new DamageSource("gas_bim");
        if (bypassArmor) gas = gas.bypassArmor();

        AxisAlignedBB box = new AxisAlignedBB(
                cx - currentRadius - 1, this.getY() - currentDown - 2,
                cz - currentRadius - 1,
                cx + currentRadius + 1, this.getY() + currentUp + 2,
                cz + currentRadius + 1
        );

        for (LivingEntity e : new HashSet<LivingEntity>(
                level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive))) {

            double ex = e.getX(), ez = e.getZ();
            double dx = ex - cx, dz = ez - cz;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > currentRadius) continue;

            double groundY = findGroundY(new BlockPos(ex, e.getY(), ez));
            if (Double.isNaN(groundY)) continue;

            double ratio = dist / currentRadius;
            double localUp = currentUp * Math.sqrt(Math.max(0.0, 1.0 - ratio * ratio));

            double minY = groundY - currentDown;
            double maxY = groundY + localUp;

            double ey = e.getY() + e.getBbHeight() * 0.6;
            if (ey >= minY && ey <= maxY) e.hurt(gas, dmgHP);
        }
    }

    /* ============================
       DÉGÂTS — AIR
       ============================ */
    private void applyDamageAirVolume(float dmgHP, boolean bypassArmor) {
        final double cx = this.getX();
        final double cz = this.getZ();
        final double cy = this.explosionOriginY; // y synchro

        DamageSource gas = new DamageSource("gas_bim");
        if (bypassArmor) gas = gas.bypassArmor();

        AxisAlignedBB box = new AxisAlignedBB(
                cx - currentRadius - 1, cy - currentDown - 1,
                cz - currentRadius - 1,
                cx + currentRadius + 1, cy + currentUp + 1,
                cz + currentRadius + 1
        );

        for (LivingEntity e : new HashSet<LivingEntity>(
                level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive))) {

            double ex = e.getX(), ez = e.getZ();
            double dx = ex - cx, dz = ez - cz;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > currentRadius) continue;

            double ratio = dist / currentRadius;
            double localUp   = currentUp   * Math.sqrt(Math.max(0.0, 1.0 - ratio * ratio));
            double localDown = currentDown * Math.sqrt(Math.max(0.0, 1.0 - ratio * ratio));

            double minY = cy - localDown;
            double maxY = cy + localUp;

            double ey = e.getY() + e.getBbHeight() * 0.6;
            if (ey >= minY && ey <= maxY) e.hurt(gas, dmgHP);
        }
    }

    // Utils sol
    private double findGroundY(BlockPos pos) {
        int startY = Math.min(pos.getY(), level.getMaxBuildHeight() - 1);
        for (int y = startY; y >= 0; y--) {
            BlockPos p = new BlockPos(pos.getX(), y, pos.getZ());
            VoxelShape shape = level.getBlockState(p).getCollisionShape(level, p);
            if (!shape.isEmpty()) return y + shape.max(Direction.Axis.Y);
        }
        return Double.NaN;
    }

    private void dropDisabledShellOnce() {
        if (droppedShell || level.isClientSide) return;
        ItemEntity drop = new ItemEntity(level, this.getX(), this.getY(), this.getZ(),
                ModItems.GAS_BIM_DISABLED.get().getDefaultInstance());
        level.addFreshEntity(drop);
        droppedShell = true;
    }

    // Persistence
    @Override protected void readAdditionalSaveData(CompoundNBT nbt) {
        age              = nbt.getInt("Age");
        currentRadius    = nbt.getDouble("Radius");
        currentUp        = nbt.getDouble("Up");
        currentDown      = nbt.getDouble("Down");
        alphaFactor      = nbt.getDouble("AlphaFactor");
        spreadSpeed      = nbt.contains("SpreadSpeed") ? nbt.getDouble("SpreadSpeed") : spreadSpeed;
        explosionOriginY = nbt.getDouble("ExplosionOriginY");
        airborne         = nbt.getBoolean("Airborne");

        // Re-déposer dans le dataManager au chargement côté serveur
        if (!level.isClientSide) {
            this.entityData.set(DATA_AIRBORNE, airborne);
            this.entityData.set(DATA_ORIGIN_Y, (float) explosionOriginY);
        }
    }

    @Override protected void addAdditionalSaveData(CompoundNBT nbt) {
        nbt.putInt("Age", age);
        nbt.putDouble("Radius", currentRadius);
        nbt.putDouble("Up", currentUp);
        nbt.putDouble("Down", currentDown);
        nbt.putDouble("AlphaFactor", alphaFactor);
        nbt.putDouble("SpreadSpeed", spreadSpeed);
        nbt.putDouble("ExplosionOriginY", explosionOriginY);
        nbt.putBoolean("Airborne", airborne);
    }

    public void setExplosionOrigin(double y) {
        this.explosionOriginY = y;
        // On pousse immédiatement côté réseau
        this.entityData.set(DATA_ORIGIN_Y, (float) y);
    }

    public void setAirborne(boolean b) {
        this.airborne = b;
        this.entityData.set(DATA_AIRBORNE, b);
    }

    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean isPickable() { return false; }
}
