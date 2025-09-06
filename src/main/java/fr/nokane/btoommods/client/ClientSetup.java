package fr.nokane.btoommods.client;

import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.entity.item.GasBimEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.SpriteRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    // fr/nokane/btoommods/client/ClientSetup.java
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        RenderingRegistry.registerEntityRenderingHandler(
                ModEntities.CRACKER_BIM.get(),
                m -> new SpriteRenderer<>(m, Minecraft.getInstance().getItemRenderer())
        );
        RenderingRegistry.registerEntityRenderingHandler(
                ModEntities.BLAZING_BIM.get(),
                m -> new SpriteRenderer<>(m, Minecraft.getInstance().getItemRenderer())
        );
        RenderingRegistry.registerEntityRenderingHandler(
                ModEntities.BLAZING_FIRE_FIELD.get(),
                EmptyRenderer::new // ci-dessous
        );
        RenderingRegistry.registerEntityRenderingHandler(
                ModEntities.GAS_BIM.get(),
                manager -> new SpriteRenderer<GasBimEntity>(manager, Minecraft.getInstance().getItemRenderer())
        );
        RenderingRegistry.registerEntityRenderingHandler(
                ModEntities.GAS_CLOUD_FIELD.get(),
                EmptyRenderer::new // rendu vide, uniquement particules coté entity.tick()
        );
    }
}
