// fr/nokane/btoommods/net/RadarScanC2S.java
package fr.nokane.btoommods.net;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.radar.RadarCapability;
import fr.nokane.btoommods.radar.RadarData;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RadarScanC2S {
    public static void encode(RadarScanC2S m, PacketBuffer b) {}
    public static RadarScanC2S decode(PacketBuffer b) { return new RadarScanC2S(); }

    public static void handle(RadarScanC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayerEntity sp = c.getSender();
            if (sp == null) return;

            sp.getCapability(RadarCapability.CAP).ifPresent(data -> {
                if (!data.hasImplant()) return;

                long now = sp.level.getGameTime();

                // anti-spam
                if (now < data.getCooldownUntil()) return;
                data.setCooldownUntil(now + ModConfigs.COMMON.RADAR_COOLDOWN_TICKS.get());

                int base = ModConfigs.COMMON.RADAR_BASE_RADIUS.get();
                int extraPer = ModConfigs.COMMON.RADAR_EXTRA_PER_ITEM.get();
                int radius = base + data.getBoosters() * extraPer;
                double radiusSq = (double) radius * (double) radius;

                int glowTicks = ModConfigs.COMMON.RADAR_GLOW_TICKS.get();
                int activeWindow = ModConfigs.COMMON.RADAR_ACTIVE_WINDOW.get();

                // Seuils “avance/recul” (même logique que côté RadarEvents)
                final double EPS_DIST = 0.0125; // ~1.25 cm/tick
                final double EPS_VEL  = 0.0125;

                List<Integer> foundIds = new ArrayList<>();

                for (ServerPlayerEntity other : sp.getServer().getPlayerList().getPlayers()) {
                    if (other == sp || other.level != sp.level) continue;
                    if (sp.distanceToSqr(other) > radiusSq) continue;

                    // invisible au radar si accroupi
                    if (other.isCrouching()) continue;

                    // Détection immédiate si l’autre AVANCE/RECULE maintenant…
                    boolean movingNow = isMovingForwardBackward(other, EPS_DIST, EPS_VEL);

                    // …sinon fallback sur la fenêtre d’activité
                    boolean recentlyActive = other.getCapability(RadarCapability.CAP)
                            .map(RadarData::getLastMoveTick)
                            .map(t -> now - t <= activeWindow)
                            .orElse(false);

                    if (!(movingNow || recentlyActive)) continue;

                    foundIds.add(other.getId());

                    // réciproque : la cible voit aussi le scanneur en glow
                    Net.CH.send(PacketDistributor.PLAYER.with(() -> other),
                            new GlowS2C(glowTicks, new int[]{ sp.getId() }));
                }

                if (!foundIds.isEmpty()) {
                    Net.CH.send(PacketDistributor.PLAYER.with(() -> sp),
                            new GlowS2C(glowTicks, foundIds.stream().mapToInt(i -> i).toArray()));
                }
            });
        });
        c.setPacketHandled(true);
    }

    /** Vrai si le joueur avance/recul *maintenant* le long de son axe de regard. */
    private static boolean isMovingForwardBackward(ServerPlayerEntity p, double epsDist, double epsVel) {
        // axe avant/arrière (regard) horizontal
        Vector3d look = p.getLookAngle();
        double lx = look.x, lz = look.z;
        double ll = Math.hypot(lx, lz);
        if (ll < 1.0E-6) { lx = 0; lz = 0; ll = 1.0; }

        // déplacement depuis le tick précédent
        double dx = p.getX() - p.xOld;
        double dz = p.getZ() - p.zOld;
        double alongDist = (dx * lx + dz * lz) / ll;

        // vitesse actuelle
        Vector3d v = p.getDeltaMovement();
        double alongVel  = (v.x * lx + v.z * lz) / ll;

        return Math.abs(alongDist) > epsDist || Math.abs(alongVel) > epsVel;
    }
}
