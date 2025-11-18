package fr.nokane.btoommods.net;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.item.RemoteBimEntity;
import fr.nokane.btoommods.item.RemoteBraceletItem;
import fr.nokane.btoommods.item.RemoteBraceletManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class RemoteQuerySlotsC2S {

    public static void encode(RemoteQuerySlotsC2S m, PacketBuffer buf) {}
    public static RemoteQuerySlotsC2S decode(PacketBuffer buf) { return new RemoteQuerySlotsC2S(); }

    public static void handle(RemoteQuerySlotsC2S msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayerEntity sp = ctx.getSender();
            if (sp == null) return;

            // ✅ UTILISE LE BRACELET ACTIF DU JOUEUR
            ItemStack bracelet = RemoteBraceletManager.getActiveBracelet(sp);
            if (bracelet.isEmpty()) {
                // Pas de bracelet actif = aucun Remote BIM visible
                Net.toPlayer(sp, new RemoteSlotsS2C(0));
                return;
            }

            // ✅ Récupère l'UUID du bracelet ACTIF
            UUID braceletUUID = RemoteBraceletItem.getBraceletUUID(bracelet);
            if (braceletUUID == null) {
                Net.toPlayer(sp, new RemoteSlotsS2C(0));
                return;
            }

            ServerWorld sw = sp.getLevel();

            // ✅ Lecture du rayon de scan dans la config
            int scan = ModConfigs.REMOTE.REMOTE_SCAN_RADIUS.get();
            AxisAlignedBB box = sp.getBoundingBox().inflate(scan);

            int mask = 0;
            // ✅ Parcourt tous les Remote BIMs affiliés au bracelet ACTIF
            for (RemoteBimEntity e : sw.getEntitiesOfClass(RemoteBimEntity.class, box)) {
                if (braceletUUID.equals(e.getBraceletUUID())) {
                    int s = e.getSlot();
                    if (s >= 1 && s <= 8) {
                        mask |= (1 << (s - 1));
                    }
                }
            }

            Net.toPlayer(sp, new RemoteSlotsS2C(mask));
        });
        ctx.setPacketHandled(true);
    }
}