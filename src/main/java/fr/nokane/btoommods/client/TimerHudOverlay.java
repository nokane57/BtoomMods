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
 * - Affiche le compte à rebours du timer actif OU en pause
 * - Le projectile est prioritaire
 * - Disparaît quand aucun timer n’est actif ni visible
 * - Réapparaît quand le timer revient (main / sol)
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class TimerHudOverlay extends AbstractGui {

    private static final ResourceLocation HUD_TEXTURE =
            new ResourceLocation(Btoommods.MOD_ID, "textures/gui/timer_hud.png");

    private static int lastDisplayedSecs = -1;
    private static boolean hideHud = false;
    private static ItemStack lastTimerStack = ItemStack.EMPTY;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int secs = detectTimerSeconds(mc);

        // 💣 Si plus de timer actif → cacher HUD
        if (secs <= 0) {
            hideHud = true;
            lastDisplayedSecs = -1;
            return;
        }

        if (hideHud) return;

        lastDisplayedSecs = secs;

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

    /**
     * 🔍 Détection du timer actif ou en pause à afficher :
     * 1. Projectile actif (prioritaire)
     * 2. Timer en main (actif ou en pause)
     * 3. Timer au sol (actif ou en pause)
     * 4. Timer dans l’inventaire (en pause)
     */
    private static int detectTimerSeconds(Minecraft mc) {
        double radius = ModConfigs.TIMER.HUD_RADIUS.get();

        // 1️⃣ Projectile prioritaire
        TimerBimProjectileEntity proj = mc.level.getEntitiesOfClass(
                        TimerBimProjectileEntity.class,
                        mc.player.getBoundingBox().inflate(radius))
                .stream()
                .filter(p -> p.isAlive() && !p.hasExplodedClientSide() && p.getRemainingTicks() > 0)
                .findFirst()
                .orElse(null);

        if (proj != null) {
            hideHud = false;
            lastTimerStack = ItemStack.EMPTY;
            return (int) Math.ceil(proj.getRemainingTicks() / 20.0);
        }

        // 2️⃣ Timer en main
        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof TimerBimItem) {
            int ticks = held.getOrCreateTag().getInt(TimerBimItem.NBT_REMAINING);
            boolean started = held.getOrCreateTag().getBoolean(TimerBimItem.NBT_HAS_STARTED);

            if (started && ticks > 0) {
                hideHud = false;
                lastTimerStack = held;
                return (int) Math.ceil(ticks / 20.0);
            }
        }

        // 🔄 Si on change de slot (et l’ancien timer existe encore)
        if (!(mc.player.getMainHandItem().getItem() instanceof TimerBimItem) && !lastTimerStack.isEmpty()) {
            boolean started = lastTimerStack.getOrCreateTag().getBoolean(TimerBimItem.NBT_HAS_STARTED);
            int ticks = lastTimerStack.getOrCreateTag().getInt(TimerBimItem.NBT_REMAINING);
            if (started && ticks > 0) {
                // HUD temporairement caché jusqu’à ce qu’on reprenne ce timer
                hideHud = true;
                return -1;
            }
        }

        // 3️⃣ Timer au sol (actif ou en pause)
        ItemEntity nearestItem = mc.level.getEntitiesOfClass(
                        ItemEntity.class,
                        mc.player.getBoundingBox().inflate(radius),
                        e -> e.isAlive() && e.getItem().getItem() instanceof TimerBimItem)
                .stream().findFirst().orElse(null);

        if (nearestItem != null) {
            ItemStack stack = nearestItem.getItem();
            int syncedTicks = nearestItem.getPersistentData().getInt("RemainingTicks");
            boolean started = stack.getOrCreateTag().getBoolean(TimerBimItem.NBT_HAS_STARTED);
            int ticks = syncedTicks > 0 ? syncedTicks : stack.getOrCreateTag().getInt(TimerBimItem.NBT_REMAINING);

            if (started && ticks > 0) {
                hideHud = false;
                lastTimerStack = ItemStack.EMPTY;
                return (int) Math.ceil(ticks / 20.0);
            }
        }

        // 4️⃣ Timer inventaire (pause)
        for (ItemStack stack : mc.player.inventory.items) {
            if (stack.getItem() instanceof TimerBimItem) {
                boolean started = stack.getOrCreateTag().getBoolean(TimerBimItem.NBT_HAS_STARTED);
                int ticks = stack.getOrCreateTag().getInt(TimerBimItem.NBT_REMAINING);
                if (started && ticks > 0) {
                    hideHud = false;
                    lastTimerStack = stack;
                    return (int) Math.ceil(ticks / 20.0);
                }
            }
        }

        // 🔚 Rien d’actif → cacher HUD
        return -1;
    }
}
