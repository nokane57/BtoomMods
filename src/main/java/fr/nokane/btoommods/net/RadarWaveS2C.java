package fr.nokane.btoommods.net;

import fr.nokane.btoommods.client.effects.RadarWaveEffect;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * 🌊 Paquet client : déclenche l’effet visuel de l’onde radar (particules vertes)
 */
public class RadarWaveS2C {
    private final double x, y, z;
    private final int radius, duration;

    public RadarWaveS2C(double x, double y, double z, int radius, int duration) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.duration = duration;
    }

    public static void encode(RadarWaveS2C msg, PacketBuffer buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeInt(msg.radius);
        buf.writeInt(msg.duration);
    }

    public static RadarWaveS2C decode(PacketBuffer buf) {
        return new RadarWaveS2C(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void handle(RadarWaveS2C msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 👁️ Visible uniquement côté client scanneur
            RadarWaveEffect.trigger(msg.x, msg.y, msg.z, msg.radius, msg.duration);
        });
        ctx.get().setPacketHandled(true);
    }
}
