// fr/nokane/btoommods/net/Net.java
package fr.nokane.btoommods.net;

import fr.nokane.btoommods.Btoommods;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;

public final class Net {
    private static final String PROTOCOL = "1";

    // Le canal est créé quand la classe est chargée (pendant common setup)
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Btoommods.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );

    private static int id = 0;

    public static void registerMessages() {
        CH.registerMessage(id++, RadarScanC2S.class,
                RadarScanC2S::encode, RadarScanC2S::decode, RadarScanC2S::handle);

        CH.registerMessage(id++, GlowS2C.class,
                GlowS2C::encode, GlowS2C::decode, GlowS2C::handle);

        CH.registerMessage(id++, RemoteTriggerC2S.class, RemoteTriggerC2S::encode, RemoteTriggerC2S::decode, RemoteTriggerC2S::handle);
        CH.registerMessage(id++, RemoteOwnerMarkerS2C.class, RemoteOwnerMarkerS2C::encode, RemoteOwnerMarkerS2C::decode, RemoteOwnerMarkerS2C::handle);
        CH.registerMessage(id++, RemoteQuerySlotsC2S.class, RemoteQuerySlotsC2S::encode, RemoteQuerySlotsC2S::decode, RemoteQuerySlotsC2S::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CH.registerMessage(id++, RemoteSlotsS2C.class, RemoteSlotsS2C::encode, RemoteSlotsS2C::decode, RemoteSlotsS2C::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void toPlayer(net.minecraft.entity.player.ServerPlayerEntity sp, Object msg){
        CH.send(net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> sp), msg);
    }

    private Net() {}
}
