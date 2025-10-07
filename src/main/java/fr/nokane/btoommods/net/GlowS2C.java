package fr.nokane.btoommods.net;

import fr.nokane.btoommods.client.GlowClient;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class GlowS2C {
    private final int glowTicks;
    private final int[] ids;
    private final int color; // ex. 0x00FF00 pour vert

    public GlowS2C(int glowTicks, int[] ids, int color) {
        this.glowTicks = glowTicks;
        this.ids = ids;
        this.color = color;
    }

    public static void encode(GlowS2C m, PacketBuffer b) {
        b.writeVarInt(m.glowTicks);
        b.writeVarInt(m.color);
        b.writeVarInt(m.ids.length);
        for (int id : m.ids) b.writeVarInt(id);
    }

    public static GlowS2C decode(PacketBuffer b) {
        int ticks = b.readVarInt();
        int color = b.readVarInt();
        int n = b.readVarInt();
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) ids[i] = b.readVarInt();
        return new GlowS2C(ticks, ids, color);
    }

    public static void handle(GlowS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> GlowClient.apply(msg.ids, msg.glowTicks, msg.color));
        c.setPacketHandled(true);
    }
}
