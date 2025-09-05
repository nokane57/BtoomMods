package fr.nokane.btoommods.entity.misc;

import fr.nokane.btoommods.config.ModConfigs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.HashSet;
import java.util.Set;

public class BlazingFireFieldEntity extends Entity {
    private int age;

    // Source de dégâts custom (aucun registre requis)
    private static final DamageSource BLAZING_FLAME =
            (new DamageSource("blazing_flame")).setIsFire();

    public BlazingFireFieldEntity(EntityType<? extends BlazingFireFieldEntity> type, World level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() { }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) return;

        age++;
        if (age >= ModConfigs.COMMON.BLAZING_FIRE_LIFETIME.get()) {
            this.remove();
            return;
        }

        // Config
        final int len         = ModConfigs.COMMON.BLAZING_FIRE_LENGTH.get();
        final int width       = Math.max(1, ModConfigs.COMMON.BLAZING_FIRE_WIDTH.get());
        final float insideHearts = ModConfigs.COMMON.BLAZING_FIRE_DMG_INSIDE_HEARTS.get().floatValue();
        final int burnDuration   = ModConfigs.COMMON.BLAZING_BURN_DURATION.get();

        // AABBs : croix X/Z
        BlockPos c = this.blockPosition();
        double minY = c.getY();
        double maxY = c.getY() + 2.0D;
        double halfW = 0.5D * width;

        AxisAlignedBB bandX = new AxisAlignedBB(
                c.getX() - len, minY, c.getZ() - halfW,
                c.getX() + len + 1, maxY, c.getZ() + halfW
        );
        AxisAlignedBB bandZ = new AxisAlignedBB(
                c.getX() - halfW, minY, c.getZ() - len,
                c.getX() + halfW, maxY, c.getZ() + len + 1
        );

        Set<LivingEntity> victims = new HashSet<>();
        victims.addAll(level.getEntitiesOfClass(LivingEntity.class, bandX, LivingEntity::isAlive));
        victims.addAll(level.getEntitiesOfClass(LivingEntity.class, bandZ, LivingEntity::isAlive));

        for (LivingEntity e : victims) {
            // Dégâts "dans la zone" : 2 coeurs/tick par défaut
            e.hurt(BLAZING_FLAME, insideHearts * 2.0F);

            // Feu visuel + brûle après être sorti (vanilla inflige ses propres dégâts)
            int sec = Math.max(1, burnDuration / 20);
            e.setSecondsOnFire(sec);
        }
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
