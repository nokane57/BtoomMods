package fr.nokane.btoommods.sound;

import fr.nokane.btoommods.config.ModConfigs;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Gestion centralisée des sons du mod.
 * - Sépare les sons "monde" et "interface".
 * - Compatible client/serveur (aucune classe client chargée côté serveur).
 */
public class SoundUtils {

    // Volumes par défaut (peuvent être modulés)
    public static final float VOL_UI = 1.5F;
    public static final float VOL_WORLD = 1.3F;
    public static final float VOL_REBOND = 1.35F;
    public static final float VOL_SONAR = 1.4F;
    public static final float VOL_GAS = 1.35F;

    // --- 🔊 Sons joués dans le monde (serveur ou client) ---

    public static void playWorldSound(World world, double x, double y, double z, SoundEvent sound) {
        playWorldSound(world, x, y, z, sound, VOL_WORLD, 1.0F);
    }

    public static void playWorldSound(World world, double x, double y, double z,
                                      SoundEvent sound, float volume, float pitch) {
        if (world == null || sound == null) return;
        world.playSound(null, x, y, z, sound, SoundCategory.PLAYERS, volume, pitch);
    }

    // --- 🎯 Alias utiles ---

    public static void playRebound(World world, double x, double y, double z) {
        if (world == null) return;
        playWorldSound(world, x, y, z,
                ModSounds.REBOND_ITEM.get(),
                VOL_REBOND,
                1.0F + (world.random != null ? world.random.nextFloat() * 0.2F : 0));
    }

    /** 🔥 Son d’allumage du feu (Blazing BIM) */
    public static void playFire(World world, double x, double y, double z) {
        float vol = (float) (0.6F * ModConfigs.BLAZING.BLAZING_FIRE_LENGTH.get() / 8.0F);
        playWorldSound(world, x, y, z, ModSounds.FIRE_ITEM.get(), vol, 1.0F);
    }

    /** 💥 Explosion atténuée (Blazing BIM) */
    public static void playExplosion(World world, double x, double y, double z) {
        world.playSound(null, x, y, z,
                net.minecraft.util.SoundEvents.GENERIC_EXPLODE,
                SoundCategory.BLOCKS,
                0.4F, // volume plus faible
                1.0F);
    }

    // --- 🖥️ Méthodes client uniquement (interface UI) ---

    public static void playClac() {
        if (FMLEnvironment.dist == Dist.CLIENT)
            Client.playUISound(ModSounds.PULL_ITEM.get(), 1.0F);
    }

    public static void playBip() {
        if (FMLEnvironment.dist == Dist.CLIENT)
            Client.playUISound(ModSounds.PI_ITEM.get(), 1.05F);
    }

    public static void playSonar() {
        if (FMLEnvironment.dist == Dist.CLIENT)
            Client.playUISound(ModSounds.SONAR_ITEM.get(), 1.0F);
    }

    public static void playGas() {
        if (FMLEnvironment.dist == Dist.CLIENT)
            Client.playUISound(ModSounds.GAS_ITEM.get(), 1.05F);
    }

    // =====================================================================
    // ⬇️ Sous-classe interne client-only : jamais chargée côté serveur
    // =====================================================================
    @OnlyIn(Dist.CLIENT)
    private static class Client {
        private static void playUISound(SoundEvent sound, float pitch) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.getSoundManager() == null || sound == null) return;
            mc.getSoundManager().play(
                    net.minecraft.client.audio.SimpleSound.forUI(sound, VOL_UI, pitch)
            );
        }
    }
}
