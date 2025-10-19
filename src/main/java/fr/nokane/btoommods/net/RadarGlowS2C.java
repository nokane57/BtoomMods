package fr.nokane.btoommods.net;

import fr.nokane.btoommods.client.RadarGlowClient;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 🟢 Paquet côté client : active le glow vert du radar
 */
public class RadarGlowS2C {
    private final int glowTicks;
    private final int[] ids;

    public RadarGlowS2C(int glowTicks, int[] ids) {
        this.glowTicks = glowTicks;
        this.ids = ids;
    }

    public static void encode(RadarGlowS2C msg, PacketBuffer buf) {
        buf.writeVarInt(msg.glowTicks);
        buf.writeVarInt(msg.ids.length);
        for (int id : msg.ids) buf.writeVarInt(id);
    }

    public static RadarGlowS2C decode(PacketBuffer buf) {
        int ticks = buf.readVarInt();
        int n = buf.readVarInt();
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) ids[i] = buf.readVarInt();
        return new RadarGlowS2C(ticks, ids);
    }

    public static void handle(RadarGlowS2C msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> RadarGlowClient.apply(msg.ids, msg.glowTicks));
        ctx.get().setPacketHandled(true);
    }
}
