package fr.nokane.btoommods.client;

import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.entity.item.CrackerBimEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.SpriteRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        RenderingRegistry.registerEntityRenderingHandler(
                ModEntities.CRACKER_BIM.get(),
                manager -> new SpriteRenderer<CrackerBimEntity>(manager, Minecraft.getInstance().getItemRenderer())
        );

    }

    // (facultatif) si tu veux enregistrer des loaders/models additionnels
    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent e) {}
}
