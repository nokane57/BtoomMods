package fr.nokane.btoommods.events;

import fr.nokane.btoommods.net.ConfigSyncS2C;
import fr.nokane.btoommods.net.Net;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gère la synchronisation de la configuration du serveur vers les clients
 * à chaque connexion d'un joueur.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ConfigSyncHandler {

    /**
     * Envoie la config du serveur au client dès qu'il se connecte
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();

            // Envoyer toutes les valeurs de config du serveur au client
            Net.toPlayer(player, new ConfigSyncS2C());
        }
    }
}