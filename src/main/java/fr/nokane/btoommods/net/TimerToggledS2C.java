package fr.nokane.btoommods.net;

import fr.nokane.btoommods.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class TimerToggledS2C {
    public enum Action { ACTIVATED, DEACTIVATED }
    private final Action action;
    public TimerToggledS2C(Action action) { this.action = action; }

    public static void encode(TimerToggledS2C msg, PacketBuffer buf) { buf.writeEnum(msg.action); }
    public static TimerToggledS2C decode(PacketBuffer buf) { return new TimerToggledS2C(buf.readEnum(Action.class)); }

    public static void handle(TimerToggledS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> handleClient(msg));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(TimerToggledS2C msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;

        switch (msg.action) {
            case ACTIVATED:
                mc.getSoundManager().play(SimpleSound.forUI(ModSounds.PI_ITEM.get(), 1.0F));
                break;
            case DEACTIVATED:
                mc.getSoundManager().play(SimpleSound.forUI(ModSounds.PULL_ITEM.get(), 1.0F));
                break;
        }
    }
}
