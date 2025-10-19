package fr.nokane.btoommods.client;

import fr.nokane.btoommods.Btoommods;
import fr.nokane.btoommods.client.effects.RadarWaveEffect;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.net.*;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.SpriteRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 🎮 Configuration client :
 * - Renders
 * - Keybindings
 * - Tick listeners
 * - Glow management
 * - Onde radar visuelle
 */
@Mod.EventBusSubscriber(modid = Btoommods.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    private static KeyBinding RADAR_KEY;
    private static KeyBinding REMOTE_GUI_KEY;
    private static KeyBinding TIMER_TOGGLE_KEY;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        // === Renders ===
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.CRACKER_BIM.get(),
                mgr -> new SpriteRenderer<>(mgr, Minecraft.getInstance().getItemRenderer()));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.BLAZING_BIM.get(),
                mgr -> new SpriteRenderer<>(mgr, Minecraft.getInstance().getItemRenderer()));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.BLAZING_FIRE_FIELD.get(), EmptyRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.GAS_BIM.get(),
                mgr -> new SpriteRenderer<>(mgr, Minecraft.getInstance().getItemRenderer()));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.GAS_BIM_DISABLED.get(),
                mgr -> new SpriteRenderer<>(mgr, Minecraft.getInstance().getItemRenderer()));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.GAS_CLOUD_FIELD.get(), EmptyRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.REMOTE_BIM.get(),
                mgr -> new SpriteRenderer<>(mgr, Minecraft.getInstance().getItemRenderer()));
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.TIMER_BIM_PROJECTILE.get(),
                mgr -> new SpriteRenderer<>(mgr, Minecraft.getInstance().getItemRenderer()));

        // === Keybindings ===
        RADAR_KEY = new KeyBinding("key.btoommods.radar", GLFW.GLFW_KEY_R, "key.categories.btoommods");
        REMOTE_GUI_KEY = new KeyBinding("key.btoommods.remote_bracelet", GLFW.GLFW_KEY_B, "key.categories.btoommods");
        TIMER_TOGGLE_KEY = new KeyBinding("key.btoommods.timer_toggle", GLFW.GLFW_KEY_G, "key.categories.btoommods");

        ClientRegistry.registerKeyBinding(RADAR_KEY);
        ClientRegistry.registerKeyBinding(REMOTE_GUI_KEY);
        ClientRegistry.registerKeyBinding(TIMER_TOGGLE_KEY);

        // === Install Glow systems ===
        GlowClient.install();       // 🔴 Remotes / BIMs
        RadarGlowClient.install();  // 🟢 Radar
        RadarWaveEffect.install();  // 🌊 Onde radar (particules)

        // === Tick listener ===
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::onClientTick);
    }

    private static void onClientTick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // === Radar ===
        while (RADAR_KEY.consumeClick()) {
            Net.CH.sendToServer(new RadarScanC2S());

            // 🌊 Onde radar visuelle côté client (preview instantanée)
            double x = mc.player.getX();
            double y = mc.player.getY() + mc.player.getBbHeight() * 0.5;
            double z = mc.player.getZ();
            RadarWaveEffect.trigger(x, y, z, 80, 40); // rayon / durée par défaut
            SoundUtils.playClac();
        }

        // === Bracelet GUI ===
        while (REMOTE_GUI_KEY.consumeClick()) {
            mc.setScreen(new fr.nokane.btoommods.client.screen.RemoteBraceletScreen());
        }

        // === Timer toggle ===
        while (TIMER_TOGGLE_KEY.consumeClick()) {
            Net.CH.sendToServer(new TimerKeyC2S(TimerKeyC2S.Action.TOGGLE));
        }

        // === Remote bracelet (touches 1–8) ===
        ItemStack off = mc.player.getOffhandItem();
        if (!off.isEmpty() && off.getItem() instanceof fr.nokane.btoommods.item.RemoteBraceletItem) {
            long window = mc.getWindow().getWindow();

            for (int key = GLFW.GLFW_KEY_1; key <= GLFW.GLFW_KEY_8; key++) {
                if (GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS) {
                    int slot = (key - GLFW.GLFW_KEY_1) + 1;
                    Net.CH.sendToServer(new RemoteTriggerC2S(slot));
                    SoundUtils.playClac();
                }
            }

            for (int key = GLFW.GLFW_KEY_KP_1; key <= GLFW.GLFW_KEY_KP_8; key++) {
                if (GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS) {
                    int slot = (key - GLFW.GLFW_KEY_KP_1) + 1;
                    Net.CH.sendToServer(new RemoteTriggerC2S(slot));
                    SoundUtils.playClac();
                }
            }
        }
    }
}
