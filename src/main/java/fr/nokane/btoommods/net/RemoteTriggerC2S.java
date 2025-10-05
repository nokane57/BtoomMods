package fr.nokane.btoommods.net;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.item.RemoteBimEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class RemoteTriggerC2S {
    private final int slot; // 1..8

    public RemoteTriggerC2S(int slot) {
        this.slot = Math.max(1, Math.min(8, slot));
    }

    public static void encode(RemoteTriggerC2S msg, PacketBuffer buf) {
        buf.writeVarInt(msg.slot);
    }

    public static RemoteTriggerC2S decode(PacketBuffer buf) {
        return new RemoteTriggerC2S(buf.readVarInt());
    }

    public static void handle(RemoteTriggerC2S msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayerEntity sp = ctx.getSender();
            if (sp == null) return;

            ServerWorld sw = sp.getLevel();

            // ✅ Lire dans la config REMOTE
            int triggerRadius = ModConfigs.REMOTE.REMOTE_TRIGGER_RADIUS.get();

            AxisAlignedBB box = sp.getBoundingBox().inflate(triggerRadius);

            sw.getEntitiesOfClass(RemoteBimEntity.class, box, e ->
                    e.getOwner() != null
                            && e.getOwner().getUUID().equals(sp.getUUID())
                            && e.getSlot() == msg.slot
            ).forEach(RemoteBimEntity::detonateNow);
        });
        ctx.setPacketHandled(true);
    }
}
