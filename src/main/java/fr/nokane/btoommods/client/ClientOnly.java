package fr.nokane.btoommods.client;

import fr.nokane.btoommods.client.screen.RemoteBraceletScreen;
import net.minecraft.client.Minecraft;

/**
 * Classe contenant du code uniquement client.
 * Elle n'est jamais chargée côté serveur.
 */
public class ClientOnly {

    public static void openBraceletScreen() {
        Minecraft.getInstance().setScreen(new RemoteBraceletScreen());
    }
}
