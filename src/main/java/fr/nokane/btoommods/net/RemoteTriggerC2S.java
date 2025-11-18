package fr.nokane.btoommods.net;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.item.RemoteBimEntity;
import fr.nokane.btoommods.item.RemoteBraceletItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Paquet C2S envoyé quand le joueur appuie sur une touche 1-8
 * en tenant le bracelet Remote en main secondaire
 */
public class RemoteTriggerC2S {

    private final int slot; // 1-8

    public RemoteTriggerC2S(int slot) {
        this.slot = slot;
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
            ServerPlayerEntity player = ctx.getSender();
            if (player == null) return;

            // ✅ Vérifie que le joueur possède un bracelet Remote
            ItemStack bracelet = RemoteBraceletItem.findBraceletInInventory(player);
            if (bracelet.isEmpty()) {
                return;
            }

            // ✅ Récupère l'UUID du bracelet
            UUID braceletUUID = RemoteBraceletItem.getBraceletUUID(bracelet);
            if (braceletUUID == null) {
                return;
            }

            ServerWorld world = player.getLevel();
            int triggerRadius = ModConfigs.REMOTE.REMOTE_TRIGGER_RADIUS.get();

            AxisAlignedBB box = new AxisAlignedBB(
                    player.getX() - triggerRadius,
                    player.getY() - triggerRadius,
                    player.getZ() - triggerRadius,
                    player.getX() + triggerRadius,
                    player.getY() + triggerRadius,
                    player.getZ() + triggerRadius
            );

            // ✅ Trouve tous les Remote BIMs affiliés à CE bracelet dans le slot demandé
            for (RemoteBimEntity remoteBim : world.getEntitiesOfClass(RemoteBimEntity.class, box)) {
                // ✅ Vérifie que le Remote BIM appartient au même bracelet
                if (braceletUUID.equals(remoteBim.getBraceletUUID())) {
                    // Vérifie que c'est le bon slot
                    if (remoteBim.getSlot() == msg.slot) {
                        // Déclenche l'explosion
                        remoteBim.detonateNow();
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}