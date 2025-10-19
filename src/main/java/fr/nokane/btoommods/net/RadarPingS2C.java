package fr.nokane.btoommods.net;

import fr.nokane.btoommods.client.effects.RadarPingRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class RadarPingS2C {
    private final double x, y, z;
    private final boolean isPlayer;

    public RadarPingS2C(double x, double y, double z, boolean isPlayer) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.isPlayer = isPlayer;
    }

    public static void encode(RadarPingS2C msg, PacketBuffer buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeBoolean(msg.isPlayer);
    }

    public static RadarPingS2C decode(PacketBuffer buf) {
        return new RadarPingS2C(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readBoolean());
    }

    public static void handle(RadarPingS2C msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                Minecraft.getInstance().execute(() ->
                        RadarPingRenderer.addPing(msg.x, msg.y, msg.z, msg.isPlayer)
                )
        );
        ctx.get().setPacketHandled(true);
    }
}
