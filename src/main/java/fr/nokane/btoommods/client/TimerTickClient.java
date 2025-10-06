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

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class TimerTickClient {

    private static int lastSeconds = -1;

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

        if (lastSeconds == -1) {
            lastSeconds = secs;
            return;
        }

        if (secs < lastSeconds && secs >= 0) {
            mc.getSoundManager().play(SimpleSound.forUI(ModSounds.PI_ITEM.get(), 1.0F));
        }

        lastSeconds = secs;
    }

    /** Détection centralisée du timer actif (main, inventaire, sol, projectile) */
    public static int detectActiveTimerSeconds(Minecraft mc) {
        int secs = -1;

        // Main
        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof TimerBimItem) {
            int s = TimerBimItem.getDisplaySeconds(held);
            if (s > 0) return s;
        }

        // Projectile
        TimerBimProjectileEntity proj = mc.level.getEntitiesOfClass(
                        TimerBimProjectileEntity.class,
                        mc.player.getBoundingBox().inflate(10.0)
                ).stream().filter(p -> p.isAlive() && !p.hasExplodedClientSide())
                .findFirst().orElse(null);

        if (proj != null) {
            int s = (int) Math.ceil(proj.getRemainingTicks() / 20.0);
            if (s > 0) return s;
        }

        // Item au sol
        ItemEntity itemEntity = mc.level.getEntitiesOfClass(
                ItemEntity.class,
                mc.player.getBoundingBox().inflate(10.0),
                e -> e.getItem().getItem() instanceof TimerBimItem
        ).stream().filter(ItemEntity::isAlive).findFirst().orElse(null);

        if (itemEntity != null) {
            int syncedTicks = itemEntity.getPersistentData().getInt("RemainingTicks");
            int s = syncedTicks > 0 ? (int) Math.ceil(syncedTicks / 20.0)
                    : TimerBimItem.getDisplaySeconds(itemEntity.getItem());
            if (s > 0) return s;
        }

        // Inventaire (synchro)
        int invTicks = mc.player.getPersistentData().getInt("TimerInventoryTicks");
        if (invTicks > 0)
            return (int) Math.ceil(invTicks / 20.0);

        return secs;
    }
}
