package fr.nokane.btoommods.net;

import fr.nokane.btoommods.client.screen.RemoteBraceletScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class RemoteSlotsS2C {
    private final int mask;
    public RemoteSlotsS2C(int mask){ this.mask = mask; }

    public static void encode(RemoteSlotsS2C m, PacketBuffer buf){ buf.writeVarInt(m.mask); }
    public static RemoteSlotsS2C decode(PacketBuffer buf){ return new RemoteSlotsS2C(buf.readVarInt()); }

    public static void handle(RemoteSlotsS2C msg, Supplier<NetworkEvent.Context> ctxSup){
        NetworkEvent.Context ctx = ctxSup.get();
        if (!ctx.getDirection().getReceptionSide().isClient()) { ctx.setPacketHandled(true); return; }

        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            if (mc.screen instanceof RemoteBraceletScreen) {
                ((RemoteBraceletScreen) mc.screen).applySlotMask(msg.mask);
            }
        });
        ctx.setPacketHandled(true);
    }
}
