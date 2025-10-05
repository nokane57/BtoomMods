package fr.nokane.btoommods.net.client;

import fr.nokane.btoommods.net.TimerToggledS2C;
import fr.nokane.btoommods.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;

/**
 * Exécuté uniquement côté client — permet de jouer les sons sans crash serveur.
 */
public class TimerToggledS2CHandler {

    public static void playSound(TimerToggledS2C.Action action) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        switch (action) {
            case ACTIVATED:
                mc.getSoundManager().play(SimpleSound.forUI(ModSounds.PULL_ITEM.get(), 1.0F));
                break;
            case DEACTIVATED:
                mc.getSoundManager().play(SimpleSound.forUI(ModSounds.PULL_ITEM.get(), 0.8F));
                break;
        }
    }
}
