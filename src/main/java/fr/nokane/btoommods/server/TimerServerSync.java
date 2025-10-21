package fr.nokane.btoommods.server;

import fr.nokane.btoommods.Btoommods;
import fr.nokane.btoommods.item.TimerBimItem;
import fr.nokane.btoommods.net.Net;
import fr.nokane.btoommods.net.TimerItemSyncS2C;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/** Envoie le restant des Timers actifs dans l'inventaire pour HUD. */
@Mod.EventBusSubscriber(modid = Btoommods.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TimerServerSync {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ServerLifecycleHooks.getCurrentServer().getTickCount() % 20 != 0) return;

        for (ServerPlayerEntity player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            for (ItemStack stack : player.inventory.items) {
                if (!(stack.getItem() instanceof TimerBimItem)) continue;
                if (!stack.hasTag()) continue;

                CompoundNBT tag = stack.getTag();
                if (tag == null || !tag.getBoolean(TimerBimItem.NBT_ACTIVE)) continue;

                int remaining = tag.getInt(TimerBimItem.NBT_REMAINING);
                if (remaining <= 0) continue;

                Net.CH.send(PacketDistributor.PLAYER.with(() -> player),
                        new TimerItemSyncS2C(-1, remaining));
            }
        }
    }
}
