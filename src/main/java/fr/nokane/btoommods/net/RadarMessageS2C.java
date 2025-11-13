package fr.nokane.btoommods.net;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * Packet pour envoyer des messages du radar
 */
public class RadarMessageS2C {

    private static final Logger LOGGER = LogManager.getLogger();

    public enum MessageType {
        DETECTED_PLAYER,
        ALERT_TARGET,
        DETECTED_OBJECT
    }

    private final MessageType type;
    private final String param1;
    private final int param2, param3, param4, param5;

    // Pour DETECTED_PLAYER: playerName, distance, x, y, z
    public RadarMessageS2C(MessageType type, String param1, int param2, int param3, int param4, int param5) {
        this.type = type;
        this.param1 = param1;
        this.param2 = param2;
        this.param3 = param3;
        this.param4 = param4;
        this.param5 = param5;
    }

    // Pour DETECTED_OBJECT: distance, x, y, z
    public RadarMessageS2C(MessageType type, int param2, int param3, int param4, int param5) {
        this(type, "", param2, param3, param4, param5);
    }

    public static void encode(RadarMessageS2C msg, PacketBuffer buf) {
        LOGGER.debug("[RADAR] Encoding packet: type={}, param1={}, param2={}, param3={}, param4={}, param5={}",
                msg.type, msg.param1, msg.param2, msg.param3, msg.param4, msg.param5);
        buf.writeEnum(msg.type);
        buf.writeUtf(msg.param1, 32767); // Ajout d'une limite de taille
        buf.writeInt(msg.param2);
        buf.writeInt(msg.param3);
        buf.writeInt(msg.param4);
        buf.writeInt(msg.param5);
    }

    public static RadarMessageS2C decode(PacketBuffer buf) {
        RadarMessageS2C msg = new RadarMessageS2C(
                buf.readEnum(MessageType.class),
                buf.readUtf(32767), // Même limite de taille
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
        LOGGER.debug("[RADAR] Decoded packet: type={}, param1={}, param2={}, param3={}, param4={}, param5={}",
                msg.type, msg.param1, msg.param2, msg.param3, msg.param4, msg.param5);
        return msg;
    }

    public static void handle(RadarMessageS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        LOGGER.info("[RADAR] Handling packet - Side: {}, Type: {}",
                context.getDirection().getReceptionSide(), msg.type);

        // S'assurer que le packet est traité côté client
        context.enqueueWork(() -> {
            try {
                LOGGER.info("[RADAR] Enqueuing work for message type: {}", msg.type);
                handleClient(msg);
            } catch (Exception e) {
                LOGGER.error("[RADAR] Error handling client message", e);
            }
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(RadarMessageS2C msg) {
        LOGGER.info("[RADAR CLIENT] Processing message: type={}, param1={}", msg.type, msg.param1);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            LOGGER.warn("[RADAR CLIENT] Player is null, cannot display message");
            return;
        }

        StringTextComponent text;

        switch (msg.type) {
            case DETECTED_PLAYER:
                text = new StringTextComponent(
                        String.format("RADAR > %s detected at %d blocks [%d, %d, %d]",
                                msg.param1, msg.param2, msg.param3, msg.param4, msg.param5)
                );
                text.withStyle(TextFormatting.AQUA);
                LOGGER.info("[RADAR CLIENT] Displaying DETECTED_PLAYER: {} at {} blocks",
                        msg.param1, msg.param2);
                break;

            case ALERT_TARGET:
                text = new StringTextComponent(
                        String.format("ALERT > You were detected by %s [%d, %d, %d]",
                                msg.param1, msg.param2, msg.param3, msg.param4)
                );
                text.withStyle(TextFormatting.RED);
                LOGGER.info("[RADAR CLIENT] Displaying ALERT_TARGET from {}", msg.param1);
                break;

            case DETECTED_OBJECT:
                text = new StringTextComponent(
                        String.format("RADAR > Radar item detected at %d blocks [%d, %d, %d]",
                                msg.param2, msg.param3, msg.param4, msg.param5)
                );
                text.withStyle(TextFormatting.GRAY);
                LOGGER.info("[RADAR CLIENT] Displaying DETECTED_OBJECT at {} blocks", msg.param2);
                break;

            default:
                LOGGER.warn("[RADAR CLIENT] Unknown message type: {}", msg.type);
                return;
        }

        mc.player.sendMessage(text, Util.NIL_UUID);
        LOGGER.info("[RADAR CLIENT] Message successfully sent to player chat");
    }
}