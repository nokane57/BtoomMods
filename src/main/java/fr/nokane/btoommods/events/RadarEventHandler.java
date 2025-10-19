package fr.nokane.btoommods.events;

import fr.nokane.btoommods.Btoommods;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.net.Net;
import fr.nokane.btoommods.net.RadarSyncS2C;
import fr.nokane.btoommods.radar.RadarStorage;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = Btoommods.MOD_ID)
public final class RadarEventHandler {

    private static final String TAG_INIT = "btoommods_radar_init";

    private RadarEventHandler() {}

    // 1️⃣ Quand un joueur se connecte
    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinWorldEvent e) {
        if (!(e.getEntity() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity sp = (ServerPlayerEntity) e.getEntity();
        if (sp.level.isClientSide) return;

        boolean firstJoin = !sp.getPersistentData()
                .getCompound(PlayerEntity.PERSISTED_NBT_TAG)
                .getBoolean(TAG_INIT);

        if (firstJoin) {
            sp.getPersistentData()
                    .getCompound(PlayerEntity.PERSISTED_NBT_TAG)
                    .putBoolean(TAG_INIT, true);

            RadarStorage.set(sp, 1);
        }

        // 🔄 Sync HUD radar client ⇄ serveur
        Net.CH.send(PacketDistributor.PLAYER.with(() -> sp),
                new RadarSyncS2C(RadarStorage.get(sp)));
    }

    // 2️⃣ Ramassage → ajoute au HUD uniquement
    @SubscribeEvent
    public static void onPickupRadar(EntityItemPickupEvent e) {
        if (e.getItem().getItem().getItem() != ModItems.RADAR_ITEM.get()) return;

        PlayerEntity player = e.getPlayer();
        ItemStack stack = e.getItem().getItem();
        int amount = stack.getCount();

        RadarStorage.add(player, amount);

        // 🔄 Sync client
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity sp = (ServerPlayerEntity) player;
            Net.CH.send(PacketDistributor.PLAYER.with(() -> sp),
                    new RadarSyncS2C(RadarStorage.get(sp)));
        }

        e.getItem().remove();
        e.setCanceled(true);
    }

    // 3️⃣ Mort → drop tous les radars du HUD, puis redonne 1
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent e) {
        if (!(e.getEntityLiving() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity sp = (ServerPlayerEntity) e.getEntityLiving();

        int total = RadarStorage.get(sp);
        if (total > 0) {
            ItemStack dropStack = new ItemStack(ModItems.RADAR_ITEM.get(), total);
            sp.level.addFreshEntity(new ItemEntity(sp.level, sp.getX(), sp.getY(), sp.getZ(), dropStack));
        }

        // Remet le compteur à 1 automatiquement
        RadarStorage.set(sp, 1);

        // 🔄 Sync client (HUD mis à jour)
        Net.CH.send(PacketDistributor.PLAYER.with(() -> sp),
                new RadarSyncS2C(RadarStorage.get(sp)));
    }
}
