package fr.nokane.btoommods.net;

import fr.nokane.btoommods.Btoommods;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;

public final class Net {
    private static final String PROTOCOL = "1";
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

        CH.registerMessage(id++, RemoteTriggerC2S.class,
                RemoteTriggerC2S::encode, RemoteTriggerC2S::decode, RemoteTriggerC2S::handle);

        CH.registerMessage(id++, RemoteOwnerMarkerS2C.class,
                RemoteOwnerMarkerS2C::encode, RemoteOwnerMarkerS2C::decode, RemoteOwnerMarkerS2C::handle);

        CH.registerMessage(id++, RemoteQuerySlotsC2S.class,
                RemoteQuerySlotsC2S::encode, RemoteQuerySlotsC2S::decode, RemoteQuerySlotsC2S::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CH.registerMessage(id++, RemoteSlotsS2C.class,
                RemoteSlotsS2C::encode, RemoteSlotsS2C::decode, RemoteSlotsS2C::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CH.registerMessage(id++, TimerKeyC2S.class,
                TimerKeyC2S::encode, TimerKeyC2S::decode, TimerKeyC2S::handle);

        CH.registerMessage(id++, TimerToggledS2C.class,
                TimerToggledS2C::encode, TimerToggledS2C::decode, TimerToggledS2C::handle);

        CH.registerMessage(id++, TimerItemSyncS2C.class,
                TimerItemSyncS2C::encode, TimerItemSyncS2C::decode, TimerItemSyncS2C::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CH.registerMessage(id++, RadarWaveS2C.class,
                RadarWaveS2C::encode, RadarWaveS2C::decode, RadarWaveS2C::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CH.registerMessage(id++, RadarGlowS2C.class,
                RadarGlowS2C::encode, RadarGlowS2C::decode, RadarGlowS2C::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CH.registerMessage(id++, RadarPingS2C.class,
                RadarPingS2C::encode, RadarPingS2C::decode, RadarPingS2C::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // ✅ Ajout du packet de synchronisation radar
        CH.registerMessage(id++, RadarSyncS2C.class,
                RadarSyncS2C::encode, RadarSyncS2C::decode, RadarSyncS2C::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void toPlayer(net.minecraft.entity.player.ServerPlayerEntity sp, Object msg) {
        CH.send(net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> sp), msg);
    }

    private Net() {}
}
