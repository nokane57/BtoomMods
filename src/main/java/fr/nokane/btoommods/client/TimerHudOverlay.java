package fr.nokane.btoommods.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import fr.nokane.btoommods.Btoommods;
import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.item.TimerBimProjectileEntity;
import fr.nokane.btoommods.item.TimerBimItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * HUD du Timer BIM :
 * - Affiche le compte à rebours du timer actif OU le plus proche
 * - Priorité : projectile > item au sol > en main (actif ou en pause)
 * - Reste visible si le joueur tient une timer en pause mais qu’un timer actif est proche
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class TimerHudOverlay extends AbstractGui {

    private static final ResourceLocation HUD_TEXTURE =
            new ResourceLocation(Btoommods.MOD_ID, "textures/gui/timer_hud.png");

    private static boolean hideHud = false;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int secs = detectTimerSeconds(mc);

        if (secs <= 0) {
            hideHud = true;
            return;
        }

        hideHud = false;

        // --- Dessin du HUD ---
        int screenW = mc.getWindow().getGuiScaledWidth();
        int x = screenW - 64 - 5;
        int y = 5;

        MatrixStack matrix = event.getMatrixStack();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        mc.getTextureManager().bind(HUD_TEXTURE);
        blit(matrix, x, y, 0, 0, 64, 64, 64, 64);

        String text = String.format("%02d", Math.max(0, secs));
        int color = (secs <= 3) ? 0xFF0000 : 0x00FF00;

        FontRenderer font = mc.font;
        int textX = x + 32 - font.width(text) / 2;
        int textY = y + 24;
        font.draw(matrix, text, textX, textY, color);
    }

    /** 🔍 Détecte le timer actif le plus pertinent pour le HUD */
    private static int detectTimerSeconds(Minecraft mc) {
        double radius = ModConfigs.TIMER.HUD_RADIUS.get();

        // 1️⃣ Projectile prioritaire
        TimerBimProjectileEntity proj = mc.level.getEntitiesOfClass(
                        TimerBimProjectileEntity.class,
                        mc.player.getBoundingBox().inflate(radius)
                ).stream()
                .filter(p -> p.isAlive() && !p.hasExplodedClientSide() && p.getRemainingTicks() > 0)
                .findFirst()
                .orElse(null);

        if (proj != null)
            return (int) Math.ceil(proj.getRemainingTicks() / 20.0);

        // 2️⃣ Timer au sol actif
        ItemEntity nearestItem = mc.level.getEntitiesOfClass(
                        ItemEntity.class,
                        mc.player.getBoundingBox().inflate(radius),
                        e -> e.isAlive() && e.getItem().getItem() instanceof TimerBimItem
                ).stream()
                .filter(e -> e.getItem().getOrCreateTag().getBoolean(TimerBimItem.NBT_ACTIVE))
                .findFirst()
                .orElse(null);

        if (nearestItem != null) {
            int syncedTicks = nearestItem.getPersistentData().getInt("RemainingTicks");
            if (syncedTicks > 0)
                return (int) Math.ceil(syncedTicks / 20.0);
        }

        // 3️⃣ Timer en main (affiché même si en pause)
        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof TimerBimItem) {
            int ticks = held.getOrCreateTag().getInt(TimerBimItem.NBT_REMAINING);
            if (ticks > 0)
                return (int) Math.ceil(ticks / 20.0);
        }

        // 🔚 Aucun timer actif détecté
        return -1;
    }
}
