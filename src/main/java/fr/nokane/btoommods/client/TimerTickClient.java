package fr.nokane.btoommods.client;

import fr.nokane.btoommods.entity.item.TimerBimProjectileEntity;
import fr.nokane.btoommods.item.TimerBimItem;
import fr.nokane.btoommods.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gère le son du Timer BIM côté client :
 * - Le son du timer actif est entendu même si on change de slot
 * - Priorité : projectile > sol > inventaire/main
 * - Un seul "tic-tac" joué par seconde
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class TimerTickClient {

    private static int lastSeconds = -1;
    private static long lastTickTime = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        int secs = detectActiveTimerSeconds(mc);
        if (secs < 0) {
            lastSeconds = -1;
            return;
        }

        // 1 son par seconde max
        long now = System.currentTimeMillis();
        if (now - lastTickTime < 900) return;

        if (lastSeconds == -1) {
            lastSeconds = secs;
            lastTickTime = now;
            return;
        }

        if (secs < lastSeconds && secs >= 0) {
            mc.getSoundManager().play(SimpleSound.forUI(ModSounds.PI_ITEM.get(), 1.0F));
            lastTickTime = now;
        }

        lastSeconds = secs;
    }

    /** 🔍 Détecte le timer actif prioritaire autour ou tenu */
    public static int detectActiveTimerSeconds(Minecraft mc) {
        double radius = 10.0;

        // 1️⃣ Projectile prioritaire
        TimerBimProjectileEntity proj = mc.level.getEntitiesOfClass(
                        TimerBimProjectileEntity.class,
                        mc.player.getBoundingBox().inflate(radius)
                ).stream().filter(p -> p.isAlive() && !p.hasExplodedClientSide() && p.getRemainingTicks() > 0)
                .findFirst().orElse(null);
        if (proj != null)
            return (int) Math.ceil(proj.getRemainingTicks() / 20.0);

        // 2️⃣ Timer au sol actif
        ItemEntity itemEntity = mc.level.getEntitiesOfClass(
                        ItemEntity.class,
                        mc.player.getBoundingBox().inflate(radius),
                        e -> e.isAlive() && e.getItem().getItem() instanceof TimerBimItem
                ).stream()
                .filter(e -> e.getItem().getOrCreateTag().getBoolean(TimerBimItem.NBT_ACTIVE))
                .findFirst().orElse(null);

        if (itemEntity != null) {
            int syncedTicks = itemEntity.getPersistentData().getInt("RemainingTicks");
            if (syncedTicks > 0)
                return (int) Math.ceil(syncedTicks / 20.0);
        }

        // 3️⃣ Timer actif dans la main
        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof TimerBimItem) {
            boolean active = held.getOrCreateTag().getBoolean(TimerBimItem.NBT_ACTIVE);
            if (active) {
                int s = TimerBimItem.getDisplaySeconds(held);
                if (s > 0) return s;
            }
        }

        // 4️⃣ Timer actif dans l'inventaire
        for (ItemStack stack : mc.player.inventory.items) {
            if (stack.getItem() instanceof TimerBimItem) {
                boolean active = stack.getOrCreateTag().getBoolean(TimerBimItem.NBT_ACTIVE);
                int ticks = stack.getOrCreateTag().getInt(TimerBimItem.NBT_REMAINING);
                if (active && ticks > 0) {
                    return (int) Math.ceil(ticks / 20.0);
                }
            }
        }

        return -1;
    }
}
