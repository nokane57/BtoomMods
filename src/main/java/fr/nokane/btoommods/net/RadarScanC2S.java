package fr.nokane.btoommods.net;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.radar.RadarCapability;
import fr.nokane.btoommods.radar.RadarData;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RadarScanC2S {

    public static void encode(RadarScanC2S msg, PacketBuffer buf) {}
    public static RadarScanC2S decode(PacketBuffer buf) { return new RadarScanC2S(); }

    public static void handle(RadarScanC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayerEntity player = context.getSender();
            if (player == null) return;

            player.getCapability(RadarCapability.CAP).ifPresent(data -> {
                if (!data.hasImplant()) return;

                ServerWorld world = player.getLevel();
                long now = world.getGameTime();

                // 🕒 Anti-spam radar
                if (now < data.getCooldownUntil()) return;
                data.setCooldownUntil(now + ModConfigs.RADAR.RADAR_COOLDOWN_TICKS.get());

                // 📡 Rayon de détection
                int baseRadius = ModConfigs.RADAR.RADAR_BASE_RADIUS.get();
                int extraPerItem = ModConfigs.RADAR.RADAR_EXTRA_PER_ITEM.get();
                int radius = baseRadius + data.getBoosters() * extraPerItem;
                double radiusSq = radius * radius;

                int glowTicks = ModConfigs.RADAR.RADAR_GLOW_TICKS.get();
                int activeWindow = ModConfigs.RADAR.RADAR_ACTIVE_WINDOW.get();

                final double EPS_DIST = 0.0125;
                final double EPS_VEL = 0.0125;

                List<Integer> foundIds = new ArrayList<>();

                for (ServerPlayerEntity other : world.getServer().getPlayerList().getPlayers()) {
                    if (other == player || other.level != player.level) continue;
                    if (player.distanceToSqr(other) > radiusSq) continue;
                    if (other.isCrouching()) continue;

                    boolean movingNow = isMovingForwardBackward(other, EPS_DIST, EPS_VEL);
                    boolean recentlyActive = other.getCapability(RadarCapability.CAP)
                            .map(RadarData::getLastMoveTick)
                            .map(t -> now - t <= activeWindow)
                            .orElse(false);

                    if (!(movingNow || recentlyActive)) continue;

                    foundIds.add(other.getId());

                    // 🔊 Sonar pour les deux joueurs
                    SoundUtils.playWorldSound(world,
                            player.getX(), player.getY(), player.getZ(),
                            fr.nokane.btoommods.sound.ModSounds.SONAR_ITEM.get(),
                            SoundUtils.VOL_SONAR, 1.0F);

                    SoundUtils.playWorldSound(world,
                            other.getX(), other.getY(), other.getZ(),
                            fr.nokane.btoommods.sound.ModSounds.SONAR_ITEM.get(),
                            SoundUtils.VOL_SONAR, 1.0F);

                    // ✨ Glow sur la cible détectée
                    Net.CH.send(PacketDistributor.PLAYER.with(() -> other),
                            new GlowS2C(glowTicks, new int[]{player.getId()}));
                }

                // ✨ Glow sur les cibles détectées côté joueur
                if (!foundIds.isEmpty()) {
                    Net.CH.send(PacketDistributor.PLAYER.with(() -> player),
                            new GlowS2C(glowTicks, foundIds.stream().mapToInt(i -> i).toArray()));
                }
            });
        });
        context.setPacketHandled(true);
    }

    /** Détecte si un joueur avance/recul le long de son axe de regard. */
    private static boolean isMovingForwardBackward(ServerPlayerEntity p, double epsDist, double epsVel) {
        Vector3d look = p.getLookAngle();
        double lx = look.x, lz = look.z;
        double ll = Math.hypot(lx, lz);
        if (ll < 1.0E-6) {
            lx = 0;
            lz = 0;
            ll = 1.0;
        }

        double dx = p.getX() - p.xOld;
        double dz = p.getZ() - p.zOld;
        double alongDist = (dx * lx + dz * lz) / ll;

        Vector3d v = p.getDeltaMovement();
        double alongVel = (v.x * lx + v.z * lz) / ll;

        return Math.abs(alongDist) > epsDist || Math.abs(alongVel) > epsVel;
    }
}
