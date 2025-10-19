package fr.nokane.btoommods.radar;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;

/**
 * Gestion du stockage du nombre de radars dans le HUD du joueur.
 */
public class RadarStorage {

    private static final String TAG_RADAR_COUNT = "btoommods_radar_count";

    public static int get(PlayerEntity player) {
        CompoundNBT persisted = player.getPersistentData().getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        return persisted.getInt(TAG_RADAR_COUNT);
    }

    public static void set(PlayerEntity player, int count) {
        count = Math.max(0, count);
        CompoundNBT data = player.getPersistentData();
        CompoundNBT persisted = data.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        persisted.putInt(TAG_RADAR_COUNT, count);
        data.put(PlayerEntity.PERSISTED_NBT_TAG, persisted);
    }

    public static void add(PlayerEntity player, int amount) {
        if (amount <= 0) return;
        set(player, get(player) + amount);
    }

    public static void remove(PlayerEntity player, int amount) {
        if (amount <= 0) return;
        set(player, Math.max(0, get(player) - amount));
    }
}
