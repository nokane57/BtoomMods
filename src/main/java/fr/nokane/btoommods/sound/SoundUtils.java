package fr.nokane.btoommods.sound;

import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Gestion centralisée des sons du mod.
 * - Ne charge jamais de classes client sur le serveur.
 * - Utilise une sous-classe pour les sons d'interface côté client.
 */
public class SoundUtils {

    public static final float VOL_UI = 1.5F;
    public static final float VOL_WORLD = 1.3F;
    public static final float VOL_REBOND = 1.35F;
    public static final float VOL_SONAR = 1.4F;
    public static final float VOL_GAS = 1.35F;

    // --- Sons joués dans le monde (serveur ou client)
    public static void playWorldSound(World world, double x, double y, double z, SoundEvent sound) {
        playWorldSound(world, x, y, z, sound, VOL_WORLD, 1.0F);
    }

    public static void playWorldSound(World world, double x, double y, double z,
                                      SoundEvent sound, float volume, float pitch) {
        if (world == null || sound == null) return;
        world.playSound(null, x, y, z, sound, SoundCategory.PLAYERS, volume, pitch);
    }

    // --- Alias utiles pour le monde
    public static void playRebound(World world, double x, double y, double z) {
        playWorldSound(world, x, y, z, ModSounds.REBOND_ITEM.get(),
                VOL_REBOND, 1.0F + (world != null ? world.random.nextFloat() * 0.2F : 0));
    }

    // --- Méthodes client uniquement (UI) ---
    public static void playClac() {
        if (FMLEnvironment.dist == Dist.CLIENT) Client.playUISound(ModSounds.PULL_ITEM.get(), 1.0F);
    }

    public static void playBip() {
        if (FMLEnvironment.dist == Dist.CLIENT) Client.playUISound(ModSounds.PI_ITEM.get(), 1.05F);
    }

    public static void playSonar() {
        if (FMLEnvironment.dist == Dist.CLIENT) Client.playUISound(ModSounds.SONAR_ITEM.get(), 1.0F);
    }

    public static void playGas() {
        if (FMLEnvironment.dist == Dist.CLIENT) Client.playUISound(ModSounds.GAS_ITEM.get(), 1.05F);
    }

    public static void playFire() {
        if (FMLEnvironment.dist == Dist.CLIENT) Client.playUISound(ModSounds.FIRE_ITEM.get(), 1.05F);
    }

    // =====================================================================
    // ⬇️ Sous-classe interne client-only : n'est jamais chargée côté serveur
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
