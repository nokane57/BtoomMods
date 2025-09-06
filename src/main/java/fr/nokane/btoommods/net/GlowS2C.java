// fr/nokane/btoommods/net/GlowS2C.java
package fr.nokane.btoommods.net;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class GlowS2C {
    private final int glowTicks;
    private final int[] ids;

    public GlowS2C(int glowTicks, int[] ids){
        this.glowTicks = glowTicks;
        this.ids = ids;
    }

    public static void encode(GlowS2C m, PacketBuffer b){
        b.writeVarInt(m.glowTicks);
        b.writeVarInt(m.ids.length);
        for (int id : m.ids) b.writeVarInt(id);
    }

    public static GlowS2C decode(PacketBuffer b){
        int ticks = b.readVarInt();
        int n = b.readVarInt();
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) ids[i] = b.readVarInt();
        return new GlowS2C(ticks, ids);
    }

    public static void handle(GlowS2C msg, Supplier<NetworkEvent.Context> ctx){
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> fr.nokane.btoommods.client.GlowClient.apply(msg.ids, msg.glowTicks));
        c.setPacketHandled(true);
    }
}
