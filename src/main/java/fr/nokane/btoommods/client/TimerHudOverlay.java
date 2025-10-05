package fr.nokane.btoommods.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import fr.nokane.btoommods.Btoommods;
import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.item.TimerBimProjectileEntity;
import fr.nokane.btoommods.item.TimerBimItem;
import fr.nokane.btoommods.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class TimerHudOverlay extends AbstractGui {

    private static final ResourceLocation HUD_TEXTURE =
            new ResourceLocation(Btoommods.MOD_ID, "textures/gui/timer_hud.png");

    // --- Pour suivre le dernier affichage de secondes ---
    private static int lastDisplayedSeconds = -1;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int secs = -1;

        // --- Cas 1 : joueur tient un Timer BIM ---
        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof TimerBimItem) {
            secs = TimerBimItem.getDisplaySeconds(held);
        }

        // --- Cas 2 : projectile Timer BIM proche ---
        if (secs < 0) {
            double radius = ModConfigs.TIMER.HUD_RADIUS.get();
            Entity nearestProj = mc.level.getEntities(mc.player,
                            mc.player.getBoundingBox().inflate(radius),
                            e -> e instanceof TimerBimProjectileEntity)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (nearestProj instanceof TimerBimProjectileEntity) {
                TimerBimProjectileEntity proj = (TimerBimProjectileEntity) nearestProj;
                secs = (int) Math.ceil(proj.getRemainingTicks() / 20.0);
            }
        }

        // --- Cas 3 : item Timer BIM actif au sol ---
        if (secs < 0) {
            double radius = ModConfigs.TIMER.HUD_RADIUS.get();
            Entity nearestItem = mc.level.getEntities(mc.player,
                            mc.player.getBoundingBox().inflate(radius),
                            e -> e instanceof ItemEntity &&
                                    ((ItemEntity)e).getItem().getItem() instanceof TimerBimItem)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (nearestItem instanceof ItemEntity) {
                ItemStack stack = ((ItemEntity) nearestItem).getItem();
                secs = TimerBimItem.getDisplaySeconds(stack);
            }
        }

        // --- Rien à afficher ---
        if (secs < 0) {
            lastDisplayedSeconds = -1;
            return;
        }

        // --- Joue le "PI" à chaque changement de seconde ---
        if (secs != lastDisplayedSeconds && secs > 0) {
            mc.getSoundManager().play(SimpleSound.forUI(ModSounds.PI_ITEM.get(), 1.0F));
            lastDisplayedSeconds = secs;
        }

        // --- Placement du HUD ---
        int screenW = mc.getWindow().getGuiScaledWidth();
        int x = screenW - 64 - 5;
        int y = 5;

        MatrixStack matrix = event.getMatrixStack();

        // --- Dessine la texture du timer ---
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        mc.getTextureManager().bind(HUD_TEXTURE);
        blit(matrix, x, y, 0, 0, 64, 64, 64, 64);

        // --- Affiche les secondes ---
        String text = String.format("%02d", secs);
        int color = 0x00FF00; // vert constant

        FontRenderer font = mc.font;
        int textX = x + 32 - font.width(text) / 2;
        int textY = y + 24;

        font.draw(matrix, text, textX, textY, color);
    }
}
