package fr.nokane.btoommods.net;

import fr.nokane.btoommods.client.GlowClient;
import fr.nokane.btoommods.client.RadarGlowClient;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.function.Supplier;

public class GlowS2C {
    private final int glowTicks;
    private final int[] ids;
    private final int colorARGB;
    private final boolean radar; // ✅ nouveau flag

    public GlowS2C(int glowTicks, int[] ids, int colorARGB) {
        this(glowTicks, ids, colorARGB, false);
    }

    public GlowS2C(int glowTicks, int[] ids, int colorARGB, boolean radar) {
        this.glowTicks = glowTicks;
        this.ids = ids;
        this.colorARGB = colorARGB;
        this.radar = radar;
    }

    public static void encode(GlowS2C m, PacketBuffer b) {
        b.writeVarInt(m.glowTicks);
        b.writeVarInt(m.ids.length);
        for (int id : m.ids) b.writeVarInt(id);
        b.writeInt(m.colorARGB);
        b.writeBoolean(m.radar);
    }

    public static GlowS2C decode(PacketBuffer b) {
        int ticks = b.readVarInt();
        int n = b.readVarInt();
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) ids[i] = b.readVarInt();
        int colorARGB = b.readInt();
        boolean radar = b.readBoolean();
        return new GlowS2C(ticks, ids, colorARGB, radar);
    }

    public static void handle(GlowS2C msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (msg.radar)
                RadarGlowClient.apply(msg.ids, msg.glowTicks);
            else
                GlowClient.apply(msg.ids, msg.glowTicks, msg.colorARGB);
        });
        ctx.get().setPacketHandled(true);
    }
}
