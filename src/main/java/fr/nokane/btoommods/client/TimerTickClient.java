package fr.nokane.btoommods.client;

import fr.nokane.btoommods.entity.item.TimerBimProjectileEntity;
import fr.nokane.btoommods.item.TimerBimItem;
import fr.nokane.btoommods.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 🎧 TimerTickClient — Gestion du son du Timer BIM :
 * - Bip par seconde, même pendant un drop.
 * - Synchronisé avec le HUD.
 * - Ne spamme plus à la transition inventaire → sol.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class TimerTickClient {

    private static int lastSeconds = -1;
    private static long lastGameTick = 0;
    private static long lostSinceTick = -1;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        long currentTick = mc.level.getGameTime();
        Entity entity = detectActiveTimerEntity(mc);
        int secs = detectRemainingSeconds(mc, entity);

        // 🔄 Garde le son pendant 0.5s après drop
        if (secs < 0 && lastSeconds > 0 && (lostSinceTick == -1 || currentTick - lostSinceTick <= 10)) {
            secs = lastSeconds - 1;
        } else if (secs < 0) {
            lastSeconds = -1;
            lostSinceTick = -1;
            return;
        }

        if (entity != null) lostSinceTick = -1;
        else if (lostSinceTick == -1) lostSinceTick = currentTick;

        // 🎵 Bip à chaque nouvelle seconde
        if (lastSeconds != -1 && secs < lastSeconds && secs >= 0) {
            if ((currentTick - lastGameTick) >= 18) {
                mc.getSoundManager().play(SimpleSound.forUI(ModSounds.PI_ITEM.get(), 1.0F));
                lastGameTick = currentTick;
            }
        }

        lastSeconds = secs;
    }

    private static Entity detectActiveTimerEntity(Minecraft mc) {
        double radius = 10.0;

        TimerBimProjectileEntity proj = mc.level.getEntitiesOfClass(
                        TimerBimProjectileEntity.class,
                        mc.player.getBoundingBox().inflate(radius))
                .stream().filter(p -> p.isAlive() && !p.hasExplodedClientSide() && p.getRemainingTicks() > 0)
                .findFirst().orElse(null);
        if (proj != null) return proj;

        ItemEntity item = mc.level.getEntitiesOfClass(
                        ItemEntity.class,
                        mc.player.getBoundingBox().inflate(radius),
                        e -> e.isAlive() && e.getItem().getItem() instanceof TimerBimItem)
                .stream().filter(e -> e.getItem().getOrCreateTag().getBoolean(TimerBimItem.NBT_ACTIVE))
                .findFirst().orElse(null);
        if (item != null) return item;

        for (ItemStack stack : mc.player.inventory.items) {
            if (stack.getItem() instanceof TimerBimItem) {
                boolean active = stack.getOrCreateTag().getBoolean(TimerBimItem.NBT_ACTIVE);
                int ticks = stack.getOrCreateTag().getInt(TimerBimItem.NBT_REMAINING);
                if (active && ticks > 0) return mc.player;
            }
        }

        return null;
    }

    private static int detectRemainingSeconds(Minecraft mc, Entity entity) {
        if (entity == null) return -1;
        int ticks = 0;

        if (entity instanceof TimerBimProjectileEntity)
            ticks = ((TimerBimProjectileEntity) entity).getRemainingTicks();
        else if (entity instanceof ItemEntity)
            ticks = ((ItemEntity) entity).getPersistentData().getInt("RemainingTicks");
        else if (entity == mc.player) {
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
        return (int) Math.floor(ticks / 20.0);
    }
}
