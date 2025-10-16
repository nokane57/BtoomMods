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
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 🎯 HUD du Timer BIM :
 * - Affiche un compte à rebours fluide (avec décimales).
 * - Priorité : projectile > item au sol > inventaire/main.
 * - Reste fluide pendant les transitions (main ↔ drop ↔ tir).
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class TimerHudOverlay extends AbstractGui {

    private static final ResourceLocation HUD_TEXTURE =
            new ResourceLocation(Btoommods.MOD_ID, "textures/gui/timer_hud.png");

    private static double lastRemaining = -1;
    private static long lastTick = -1;
    private static long lostSinceTick = -1;
    private static boolean justExploded = false;
    private static long explosionTick = 0;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        long gameTick = mc.level.getGameTime();
        double radius = ModConfigs.TIMER.HUD_RADIUS.get();

        Entity currentEntity = detectActiveEntity(mc, radius);
        double remaining = detectRemainingSeconds(mc, currentEntity);

        // 💥 Explosion → cache le HUD immédiatement
        if (remaining <= 0.0 &&
                (currentEntity instanceof TimerBimProjectileEntity || currentEntity instanceof ItemEntity)) {
            justExploded = true;
            explosionTick = gameTick;
            lastRemaining = -1;
            lostSinceTick = -1;
            return;
        }

        // 🚫 Cache 3 ticks après explosion
        if (justExploded && gameTick - explosionTick < 3) return;
        justExploded = false;

        // 🕓 Simulation fluide client-side pendant le drop (si pas encore mis à jour serveur)
        if (currentEntity instanceof ItemEntity && remaining > 0 && lastRemaining > 0) {
            if (lastTick > 0) {
                long deltaTicks = gameTick - lastTick;
                remaining = Math.max(0.0, lastRemaining - (deltaTicks / 20.0));
            }
        }

        // 🎯 Si on vient de perdre la référence, conserve 0.5 s la dernière valeur
        if (remaining < 0 && lastRemaining > 0 &&
                (lostSinceTick == -1 || gameTick - lostSinceTick <= 10)) {
            remaining = Math.max(0.0, lastRemaining - (gameTick - lostSinceTick) / 20.0);
        } else if (remaining < 0) {
            lastRemaining = -1;
            lostSinceTick = -1;
            return;
        }

        if (currentEntity != null) lostSinceTick = -1;
        else if (lostSinceTick == -1) lostSinceTick = gameTick;

        // ✅ Sauvegarde dernière valeur connue
        lastRemaining = remaining;
        lastTick = gameTick;

        // --- Dessin du HUD ---
        int screenW = mc.getWindow().getGuiScaledWidth();
        int x = screenW - 64 - 5;
        int y = 5;

        MatrixStack matrix = event.getMatrixStack();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        mc.getTextureManager().bind(HUD_TEXTURE);
        blit(matrix, x, y, 0, 0, 64, 64, 64, 64);

        // 🔢 Texte précis
        String text = (remaining <= 0.05) ? "0.0" : String.format("%.1f", remaining);

        // 🔥 Rouge clignotant pour les 3 dernières secondes
        int color;
        if (remaining <= 3.0) {
            float blink = (float) ((System.currentTimeMillis() / 200L) % 2);
            color = blink < 1 ? 0xFFFF0000 : 0xFF550000;
        } else {
            color = 0x00FF00;
        }

        FontRenderer font = mc.font;
        int textX = x + 32 - font.width(text) / 2;
        int textY = y + 24;
        font.draw(matrix, text, textX, textY, color);
    }

    /** 🔍 Détection du timer actif (projectile > item > inventaire) */
    private static Entity detectActiveEntity(Minecraft mc, double radius) {
        // 1️⃣ Projectile actif
        TimerBimProjectileEntity proj = mc.level.getEntitiesOfClass(
                        TimerBimProjectileEntity.class,
                        mc.player.getBoundingBox().inflate(radius))
                .stream()
                .filter(p -> p.isAlive() && !p.hasExplodedClientSide() && p.getRemainingTicks() > 0)
                .findFirst().orElse(null);
        if (proj != null) return proj;

        // 2️⃣ Item actif au sol
        ItemEntity itemEntity = mc.level.getEntitiesOfClass(
                        ItemEntity.class,
                        mc.player.getBoundingBox().inflate(radius),
                        e -> e.isAlive() && e.getItem().getItem() instanceof TimerBimItem)
                .stream()
                .filter(e -> e.getItem().getOrCreateTag().getBoolean(TimerBimItem.NBT_ACTIVE))
                .findFirst().orElse(null);
        if (itemEntity != null) return itemEntity;

        // 3️⃣ Timer actif dans l’inventaire
        for (ItemStack stack : mc.player.inventory.items) {
            if (stack.getItem() instanceof TimerBimItem) {
                boolean active = stack.getOrCreateTag().getBoolean(TimerBimItem.NBT_ACTIVE);
                int ticks = stack.getOrCreateTag().getInt(TimerBimItem.NBT_REMAINING);
                if (active && ticks > 0) return mc.player;
            }
        }

        return null;
    }

    /** ⏱️ Retourne le temps restant précis */
    private static double detectRemainingSeconds(Minecraft mc, Entity entity) {
        if (entity == null) return -1;
        int ticks = 0;

        if (entity instanceof TimerBimProjectileEntity) {
            TimerBimProjectileEntity proj = (TimerBimProjectileEntity) entity;
            if (proj.hasExplodedClientSide()) return -1;
            ticks = proj.getRemainingTicks();
        } else if (entity instanceof ItemEntity) {
            ItemEntity item = (ItemEntity) entity;
            ItemStack stack = item.getItem();
            CompoundNBT tag = stack.getTag();

            // 🔁 Lecture prioritaire du NBT réel
            if (tag != null && tag.contains(TimerBimItem.NBT_REMAINING)) {
                ticks = tag.getInt(TimerBimItem.NBT_REMAINING);
            } else if (item.getPersistentData().contains("RemainingTicks")) {
                ticks = item.getPersistentData().getInt("RemainingTicks");
            } else if (lastRemaining > 0) {
                ticks = (int) Math.ceil(lastRemaining * 20);
            }
        } else if (entity == mc.player) {
            for (ItemStack stack : mc.player.inventory.items) {
                if (stack.getItem() instanceof TimerBimItem) {
                    boolean active = stack.getOrCreateTag().getBoolean(TimerBimItem.NBT_ACTIVE);
                    if (active) {
                        ticks = stack.getOrCreateTag().getInt(TimerBimItem.NBT_REMAINING);
                        break;
                    }
                }
            }
        }

        if (ticks <= 0) return -1;
        return ticks / 20.0;
    }
}
