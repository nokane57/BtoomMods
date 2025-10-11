package fr.nokane.btoommods.net;

import fr.nokane.btoommods.client.GlowClient;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.function.Supplier;

public class GlowS2C {
    private final int glowTicks;
    private final int[] ids;
    private final int colorARGB; // AARRGGBB (alpha conservé)

    public GlowS2C(int glowTicks, int[] ids, int colorARGB) {
        this.glowTicks = glowTicks;
        this.ids = ids;
        this.colorARGB = colorARGB;
    }

    public static void encode(GlowS2C m, PacketBuffer b) {
        b.writeVarInt(m.glowTicks);
        b.writeVarInt(m.ids.length);
        for (int id : m.ids) b.writeVarInt(id);
        b.writeInt(m.colorARGB); // ✅ conserve l'alpha
    }

    public static GlowS2C decode(PacketBuffer b) {
        int ticks = b.readVarInt();
        int n = b.readVarInt();
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) ids[i] = b.readVarInt();
        int colorARGB = b.readInt(); // ✅ lit l'alpha
        return new GlowS2C(ticks, ids, colorARGB);
    }

    public static void handle(GlowS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> GlowClient.apply(msg.ids, msg.glowTicks, msg.colorARGB));
        c.setPacketHandled(true);
    }
}
