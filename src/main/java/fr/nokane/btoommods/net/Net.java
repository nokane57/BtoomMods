// fr/nokane/btoommods/net/Net.java
package fr.nokane.btoommods.net;

import fr.nokane.btoommods.Btoommods;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

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
    }

    private Net() {}
}
