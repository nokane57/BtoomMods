package fr.nokane.btoommods.client.hud;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import fr.nokane.btoommods.Btoommods;
import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.radar.RadarStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Btoommods.MOD_ID, value = Dist.CLIENT)
public final class RadarHudOverlay extends AbstractGui {

    private RadarHudOverlay() {}

    @SubscribeEvent
    public static void onRenderHud(RenderGameOverlayEvent.Post e) {
        if (e.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int count = RadarStorage.get(mc.player);

        // Debug : afficher toujours
        // mc.font.drawShadow(e.getMatrixStack(), "RadarCount=" + count, 10, 10, 0xFFFFFF);

        if (count <= 0) return;

        int base = ModConfigs.RADAR.RADAR_BASE_RADIUS.get();
        int extra = ModConfigs.RADAR.RADAR_EXTRA_PER_ITEM.get();
        int radius = base + Math.max(0, count - 1) * extra;

        MatrixStack ms = e.getMatrixStack();
        int x = 8;
        int y = 8;

        ItemStack radarIcon = new ItemStack(ModItems.RADAR_ITEM.get());
        RenderSystem.enableBlend();
        mc.getItemRenderer().renderAndDecorateItem(radarIcon, x, y);

        mc.font.drawShadow(ms, "x" + count, x + 18, y + 4, 0x55FF55);
        mc.font.drawShadow(ms, "Portée : " + radius + " blocs", x, y + 20, 0x55FF55);
    }
}
