package fr.nokane.btoommods.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Gestion centralisée du volume et des sons du mod.
 * - Méthodes client: HUD/UI
 * - Méthodes serveur: world.playSound()
 */
public class SoundUtils {

    public static final float VOL_UI = 1.5F;
    public static final float VOL_WORLD = 1.3F;
    public static final float VOL_REBOND = 1.35F;
    public static final float VOL_SONAR = 1.4F;

    // --- CLIENT: sons d'interface ---
    @OnlyIn(Dist.CLIENT)
    public static void playUISound(SoundEvent sound, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getSoundManager() == null) return;
        mc.getSoundManager().play(SimpleSound.forUI(sound, VOL_UI, pitch));
    }

    @OnlyIn(Dist.CLIENT)
    public static void playUISound(SoundEvent sound) {
        playUISound(sound, 1.0F);
    }

    // --- SERVEUR & CLIENT: sons dans le monde ---
    public static void playWorldSound(World world, double x, double y, double z, SoundEvent sound) {
        playWorldSound(world, x, y, z, sound, VOL_WORLD, 1.0F);
    }

    public static void playWorldSound(World world, double x, double y, double z,
                                      SoundEvent sound, float volume, float pitch) {
        if (world == null || sound == null) return;
        world.playSound(null, x, y, z, sound, SoundCategory.PLAYERS, volume, pitch);
    }

    // --- Alias utiles ---
    public static void playRebound(World world, double x, double y, double z) {
        playWorldSound(world, x, y, z, ModSounds.REBOND_ITEM.get(),
                VOL_REBOND, 1.0F + world.random.nextFloat() * 0.2F);
    }

    @OnlyIn(Dist.CLIENT)
    public static void playClac() {
        playUISound(ModSounds.PULL_ITEM.get());
    }

    @OnlyIn(Dist.CLIENT)
    public static void playBip() {
        playUISound(ModSounds.PI_ITEM.get(), 1.05F);
    }

    @OnlyIn(Dist.CLIENT)
    public static void playSonar() {
        playUISound(ModSounds.SONAR_ITEM.get());
    }
}
