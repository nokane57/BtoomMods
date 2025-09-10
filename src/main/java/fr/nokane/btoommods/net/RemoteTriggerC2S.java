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
    public RemoteTriggerC2S(int slot){ this.slot = Math.max(1, Math.min(8, slot)); }

    public static void encode(RemoteTriggerC2S m, PacketBuffer b){ b.writeVarInt(m.slot); }
    public static RemoteTriggerC2S decode(PacketBuffer b){ return new RemoteTriggerC2S(b.readVarInt()); }

    public static void handle(RemoteTriggerC2S msg, Supplier<NetworkEvent.Context> ctx){
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayerEntity sp = c.getSender();
            if (sp == null) return;

            ServerWorld sw = sp.getLevel();
            int triggerRadius = ModConfigs.COMMON.REMOTE_TRIGGER_RADIUS.get();

            AxisAlignedBB box = sp.getBoundingBox().inflate(triggerRadius);

            sw.getEntitiesOfClass(RemoteBimEntity.class, box, e ->
                    e.getOwner() != null
                            && e.getOwner().getUUID().equals(sp.getUUID())
                            && e.getSlot() == msg.slot
            ).forEach(RemoteBimEntity::detonateNow);
        });
        c.setPacketHandled(true);
    }
}
