package fr.nokane.btoommods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.util.SoundEvent;

public class ClientProxy {
    /**
     * Joue un son UI côté client, safe pour éviter tout crash côté serveur.
     */
    public static void playUISound(SoundEvent sound, float volume) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            mc.getSoundManager().play(SimpleSound.forUI(sound, volume));
        }
    }
}
