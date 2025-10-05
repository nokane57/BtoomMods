package fr.nokane.btoommods.net;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.item.RemoteBimEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class RemoteQuerySlotsC2S {

    public static void encode(RemoteQuerySlotsC2S m, PacketBuffer buf) {}
    public static RemoteQuerySlotsC2S decode(PacketBuffer buf) { return new RemoteQuerySlotsC2S(); }

    public static void handle(RemoteQuerySlotsC2S msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayerEntity sp = ctx.getSender();
            if (sp == null) return;

            ServerWorld sw = sp.getLevel();

            // ✅ Lecture correcte dans la config REMOTE
            int scan = ModConfigs.REMOTE.REMOTE_SCAN_RADIUS.get();
            AxisAlignedBB box = sp.getBoundingBox().inflate(scan);

            int mask = 0;
            for (RemoteBimEntity e : sw.getEntitiesOfClass(RemoteBimEntity.class, box,
                    ent -> ent.getOwner() != null && ent.getOwner().getUUID().equals(sp.getUUID()))) {
                int s = e.getSlot();
                if (s >= 1 && s <= 8)
                    mask |= (1 << (s - 1));
            }

            Net.toPlayer(sp, new RemoteSlotsS2C(mask));
        });
        ctx.setPacketHandled(true);
    }
}
