package fr.nokane.btoommods.client;

import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.entity.item.GasBimEntity;
import fr.nokane.btoommods.net.Net;
import fr.nokane.btoommods.net.RadarScanC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.SpriteRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    private static KeyBinding RADAR_KEY;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        // Renders d'entités
        RenderingRegistry.registerEntityRenderingHandler(
                ModEntities.CRACKER_BIM.get(),
                mgr -> new SpriteRenderer<>(mgr, Minecraft.getInstance().getItemRenderer())
        );
        RenderingRegistry.registerEntityRenderingHandler(
                ModEntities.BLAZING_BIM.get(),
                mgr -> new SpriteRenderer<>(mgr, Minecraft.getInstance().getItemRenderer())
        );
        RenderingRegistry.registerEntityRenderingHandler(
                ModEntities.BLAZING_FIRE_FIELD.get(),
                EmptyRenderer::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
                ModEntities.GAS_BIM.get(),
                mgr -> new SpriteRenderer<GasBimEntity>(mgr, Minecraft.getInstance().getItemRenderer())
        );
        RenderingRegistry.registerEntityRenderingHandler(
                ModEntities.GAS_CLOUD_FIELD.get(),
                EmptyRenderer::new
        );

        // Touche du radar
        RADAR_KEY = new KeyBinding("key.btoommods.radar", GLFW.GLFW_KEY_R, "key.categories.gameplay");
        ClientRegistry.registerKeyBinding(RADAR_KEY);

        GlowClient.install();

        // Tick client (bus général, pas le bus MOD)
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::onClientTick);
    }

    private static void onClientTick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;

        // Si pas en monde (menu, chargement…), on ne fait rien
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Déclenchement : envoie un C2S au serveur
        while (RADAR_KEY.consumeClick()) {
            Net.CH.sendToServer(new RadarScanC2S());
        }
    }
}
