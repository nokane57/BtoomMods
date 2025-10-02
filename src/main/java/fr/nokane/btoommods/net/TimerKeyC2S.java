package fr.nokane.btoommods.net;

import fr.nokane.btoommods.item.TimerBimItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
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

    public static void encode(TimerKeyC2S msg, PacketBuffer buf) {
        buf.writeEnum(msg.action);
    }

    public static TimerKeyC2S decode(PacketBuffer buf) {
        return new TimerKeyC2S(buf.readEnum(Action.class));
    }

    public static void handle(TimerKeyC2S msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) return;

            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof TimerBimItem)) return;

            if (msg.action == Action.TOGGLE) {
                TimerBimItem.toggleTimer(held);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

