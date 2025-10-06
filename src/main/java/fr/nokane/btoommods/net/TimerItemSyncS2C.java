package fr.nokane.btoommods.net;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class TimerItemSyncS2C {

    private final int entityId;
    private final int remainingTicks;

    public TimerItemSyncS2C(int entityId, int remainingTicks) {
        this.entityId = entityId;
        this.remainingTicks = remainingTicks;
    }

    public static void encode(TimerItemSyncS2C msg, PacketBuffer buf) {
        buf.writeInt(msg.entityId);
        buf.writeInt(msg.remainingTicks);
    }

    public static TimerItemSyncS2C decode(PacketBuffer buf) {
        return new TimerItemSyncS2C(buf.readInt(), buf.readInt());
    }

    public static void handle(TimerItemSyncS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection().getReceptionSide() != LogicalSide.CLIENT) {
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> handleClient(msg));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(TimerItemSyncS2C msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;

        if (msg.entityId == -1) {
            if (mc.player != null) {
                if (msg.remainingTicks <= 0) {
                    mc.player.getPersistentData().remove("TimerInventoryTicks");
                } else {
                    mc.player.getPersistentData().putInt("TimerInventoryTicks", msg.remainingTicks);
                }
            }
            return;
        }

        Entity e = mc.level.getEntity(msg.entityId);
        if (e instanceof ItemEntity) {
            if (msg.remainingTicks <= 0) {
                e.getPersistentData().remove("RemainingTicks");
            } else {
                e.getPersistentData().putInt("RemainingTicks", msg.remainingTicks);
            }
        }
    }
}
