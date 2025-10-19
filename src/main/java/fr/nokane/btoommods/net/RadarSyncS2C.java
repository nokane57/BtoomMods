// fr/nokane/btoommods/net/RadarSyncS2C.java
package fr.nokane.btoommods.net;

import fr.nokane.btoommods.radar.RadarStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class RadarSyncS2C {
    private final int count;

    public RadarSyncS2C(int count) {
        this.count = count;
    }

    public static void encode(RadarSyncS2C msg, PacketBuffer buf) {
        buf.writeInt(msg.count);
    }

    public static RadarSyncS2C decode(PacketBuffer buf) {
        return new RadarSyncS2C(buf.readInt());
    }

    public static void handle(RadarSyncS2C msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(msg));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(RadarSyncS2C msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            RadarStorage.set(mc.player, msg.count);
        }
    }
}
