package fr.nokane.btoommods.net;

import fr.nokane.btoommods.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;

/**
 * Gère la lecture des sons côté client
 * quand le joueur active ou désactive un Timer BIM.
 */
public class TimerToggledS2CHandler {

    public static void playSound(TimerToggledS2C.Action action) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;

        switch (action) {
            case ACTIVATED:
                mc.getSoundManager().play(SimpleSound.forUI(ModSounds.PI_ITEM.get(), 1.0F));
                break;

            case DEACTIVATED:
                mc.getSoundManager().play(SimpleSound.forUI(ModSounds.PULL_ITEM.get(), 1.0F));
                break;
        }
    }
}
