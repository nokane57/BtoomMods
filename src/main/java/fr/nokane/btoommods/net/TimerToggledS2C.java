package fr.nokane.btoommods.net;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class TimerToggledS2C {

    public enum Action { ACTIVATED, DEACTIVATED }

    private final Action action;

    public TimerToggledS2C(Action action) {
        this.action = action;
    }

    public static void encode(TimerToggledS2C msg, PacketBuffer buf) {
        buf.writeEnum(msg.action);
    }

    public static TimerToggledS2C decode(PacketBuffer buf) {
        return new TimerToggledS2C(buf.readEnum(Action.class));
    }

    public static void handle(TimerToggledS2C msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // On ne fait rien côté serveur
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
                fr.nokane.btoommods.net.client.TimerToggledS2CHandler.playSound(msg.action);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
