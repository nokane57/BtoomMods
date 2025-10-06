package fr.nokane.btoommods.net;

import fr.nokane.btoommods.item.TimerBimItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class TimerKeyC2S {

    public enum Action {
        TOGGLE
    }

    private final Action action;

    public TimerKeyC2S(Action action) {
        this.action = action;
    }

    public static void encode(TimerKeyC2S msg, net.minecraft.network.PacketBuffer buf) {
        buf.writeEnum(msg.action);
    }

    public static TimerKeyC2S decode(net.minecraft.network.PacketBuffer buf) {
        return new TimerKeyC2S(buf.readEnum(Action.class));
    }

    public static void handle(TimerKeyC2S msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) return;

            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof TimerBimItem)) return;

            if (msg.action == Action.TOGGLE) {
                CompoundNBT tag = held.getOrCreateTag();
                boolean wasActive = tag.getBoolean(TimerBimItem.NBT_ACTIVE);

                // ⏸ On inverse l’état du timer
                TimerBimItem.toggleTimer(held);
                boolean isNowActive = tag.getBoolean(TimerBimItem.NBT_ACTIVE);

                // 🔊 Joue le son côté client
                TimerToggledS2C.Action soundAction = isNowActive
                        ? TimerToggledS2C.Action.ACTIVATED
                        : TimerToggledS2C.Action.DEACTIVATED;

                Net.CH.sendTo(
                        new TimerToggledS2C(soundAction),
                        player.connection.connection,
                        NetworkDirection.PLAY_TO_CLIENT
                );

                // 🧹 Force la mise à jour HUD côté client
                if (!isNowActive) {
                    // Si désactivé → envoi d’un message 0 tick → HUD masqué
                    Net.CH.sendTo(
                            new fr.nokane.btoommods.net.TimerItemSyncS2C(-1, 0),
                            player.connection.connection,
                            NetworkDirection.PLAY_TO_CLIENT
                    );
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
