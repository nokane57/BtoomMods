package fr.nokane.btoommods.net;

import fr.nokane.btoommods.client.GlowClient;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class RemoteOwnerMarkerS2C {
    private final double x, y, z;
    public RemoteOwnerMarkerS2C(double x, double y, double z){ this.x = x; this.y = y; this.z = z; }

    public static void encode(RemoteOwnerMarkerS2C m, PacketBuffer b){
        b.writeDouble(m.x); b.writeDouble(m.y); b.writeDouble(m.z);
    }
    public static RemoteOwnerMarkerS2C decode(PacketBuffer b){
        return new RemoteOwnerMarkerS2C(b.readDouble(), b.readDouble(), b.readDouble());
    }

    public static void handle(RemoteOwnerMarkerS2C msg, Supplier<NetworkEvent.Context> ctxSup){
        NetworkEvent.Context ctx = ctxSup.get();
        if (ctx.getDirection().getReceptionSide().isClient()){
            ctx.enqueueWork(() -> GlowClient.spawnRemoteOwnerMarker(msg.x, msg.y, msg.z));
        }
        ctx.setPacketHandled(true);
    }
}
