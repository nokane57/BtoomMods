package fr.nokane.btoommods.net;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.radar.RadarCapability;
import fr.nokane.btoommods.radar.RadarData;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RadarScanC2S {
    public static void encode(RadarScanC2S m, PacketBuffer b) {}

    public static RadarScanC2S decode(PacketBuffer b) {
        return new RadarScanC2S();
    }

    public static void handle(RadarScanC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get(); c.enqueueWork(() -> {
            ServerPlayerEntity sp = c.getSender(); if (sp == null) return;
            sp.getCapability(RadarCapability.CAP).ifPresent(data -> {
                if (!data.hasImplant()) return; long now = sp.level.getGameTime();
                // anti-spam
                if (now < data.getCooldownUntil()) return;
                data.setCooldownUntil(now + ModConfigs.COMMON.RADAR_COOLDOWN_SECONDS.get());
                int base = ModConfigs.COMMON.RADAR_BASE_RADIUS.get();
                int extraPer = ModConfigs.COMMON.RADAR_EXTRA_PER_ITEM.get();
                int radius = base + data.getBoosters() * extraPer;
                int activeWindow = ModConfigs.COMMON.RADAR_ACTIVE_WINDOW_SECONDS.get();
                int glowTicks = ModConfigs.COMMON.RADAR_GLOW_SECONDS.get();
                List<Integer> foundIds = new ArrayList<>();
                for (ServerPlayerEntity other : sp.getServer().getPlayerList().getPlayers()) {
                    if (other == sp || other.level != sp.level) continue;
                    if (sp.distanceToSqr(other) > (double)(radius * radius)) continue;
                    long lastMove = other.getCapability(RadarCapability.CAP)
                            .map(RadarData::getLastMoveTick)
                            .orElse(0L);
                    if (now - lastMove <= activeWindow) {
                        foundIds.add(other.getId());
                        // renvoyer à l’émetteur // réciproque : le joueur scanné voit l’émetteur
                        Net.CH.send(PacketDistributor.PLAYER.with(() -> other), new GlowS2C(glowTicks, new int[]{ sp.getId() })); } }
                if (!foundIds.isEmpty()) { Net.CH.send(PacketDistributor.PLAYER.with(() -> sp), new GlowS2C(glowTicks, foundIds.stream().mapToInt(i -> i).toArray())); } }); }); c.setPacketHandled(true); } }