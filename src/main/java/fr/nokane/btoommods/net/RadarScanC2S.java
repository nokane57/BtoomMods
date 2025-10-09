package fr.nokane.btoommods.net;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.radar.RadarCapability;
import fr.nokane.btoommods.radar.RadarData;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RadarScanC2S {

    public static void encode(RadarScanC2S msg, net.minecraft.network.PacketBuffer buf) {}
    public static RadarScanC2S decode(net.minecraft.network.PacketBuffer buf) { return new RadarScanC2S(); }

    public static void handle(RadarScanC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {

            ServerPlayerEntity player = context.getSender();
            if (player == null) return;

            player.getCapability(RadarCapability.CAP).ifPresent(data -> {
                if (!data.hasImplant()) return;

                ServerWorld world = player.getLevel();
                long now = world.getGameTime();

                // 🕒 cooldown individuel
                if (now < data.getCooldownUntil()) return;
                data.setCooldownUntil(now + ModConfigs.RADAR.RADAR_COOLDOWN_TICKS.get());

                // 🛰️ Vérifie nombre réel de radars dans l’inventaire (sécurité)
                int boosters = player.inventory.items.stream()
                        .filter(s -> !s.isEmpty() && s.getItem() == ModItems.RADAR_ITEM.get())
                        .mapToInt(ItemStack::getCount)
                        .sum();

                // 📡 Rayon de détection
                int baseRadius = ModConfigs.RADAR.RADAR_BASE_RADIUS.get();
                int extraPerItem = ModConfigs.RADAR.RADAR_EXTRA_PER_ITEM.get();

                // ✅ 1 radar = baseRadius ; chaque radar supplémentaire = +extraPerItem
                int radius = baseRadius + Math.max(0, boosters - 1) * extraPerItem;
                double radiusSq = radius * radius;

                int glowTicks = ModConfigs.RADAR.RADAR_GLOW_TICKS.get();
                int activeWindow = ModConfigs.RADAR.RADAR_ACTIVE_WINDOW.get();

                final double EPS_DIST = 0.0125;
                final double EPS_VEL = 0.0125;

                List<ServerPlayerEntity> detectedPlayers = new ArrayList<>();

                // 🔁 Utilise uniquement les joueurs dans le même monde
                for (ServerPlayerEntity other : world.players()) {
                    if (other == null || other.isSpectator()) continue;
                    if (player.distanceToSqr(other) > radiusSq) continue;

                    boolean isScanner = other == player;

                    // 👀 sneaks non détectés sauf si scanneur
                    if (other.isCrouching() && !isScanner) continue;

                    // 💤 AFK check
                    boolean movingNow = isMovingForwardBackward(other, EPS_DIST, EPS_VEL);
                    boolean recentlyActive = other.getCapability(RadarCapability.CAP)
                            .map(RadarData::getLastMoveTick)
                            .map(t -> now - t <= activeWindow)
                            .orElse(false);

                    if (!(movingNow || recentlyActive) && !isScanner) continue;

                    detectedPlayers.add(other);
                }

                // 🔊 Son du scan
                SoundUtils.playWorldSound(world,
                        player.getX(), player.getY(), player.getZ(),
                        fr.nokane.btoommods.sound.ModSounds.SONAR_ITEM.get(),
                        SoundUtils.VOL_SONAR, 1.0F);

                // ✨ Le scanneur se voit toujours lui-même
                if (!detectedPlayers.contains(player)) detectedPlayers.add(player);

                // 1️⃣ Envoi au scanneur (voit tout)
                int[] idsForScanner = detectedPlayers.stream()
                        .mapToInt(ServerPlayerEntity::getId)
                        .toArray();
                Net.CH.send(PacketDistributor.PLAYER.with(() -> player),
                        new GlowS2C(glowTicks, idsForScanner, 0x00FF00));

                // 2️⃣ Envoi aux cibles (voient le scanneur)
                for (ServerPlayerEntity target : detectedPlayers) {
                    if (target == player) continue;
                    Net.CH.send(PacketDistributor.PLAYER.with(() -> target),
                            new GlowS2C(glowTicks, new int[]{player.getId()}, 0x00FF00));
                }

                // 🧭 Debug console serveur
                System.out.printf("[RADAR] %s -> radius=%d, boosters=%d, detected=%d%n",
                        player.getName().getString(), radius, boosters, detectedPlayers.size());
            });
        });
        context.setPacketHandled(true);
    }

    /** Vérifie si le joueur bouge vers l’avant ou l’arrière */
    private static boolean isMovingForwardBackward(ServerPlayerEntity p, double epsDist, double epsVel) {
        Vector3d look = p.getLookAngle();
        double lx = look.x, lz = look.z;
        double ll = Math.hypot(lx, lz);
        if (ll < 1.0E-6) { lx = 0; lz = 0; ll = 1.0; }

        double dx = p.getX() - p.xOld;
        double dz = p.getZ() - p.zOld;
        double alongDist = (dx * lx + dz * lz) / ll;

        Vector3d v = p.getDeltaMovement();
        double alongVel = (v.x * lx + v.z * lz) / ll;

        return Math.abs(alongDist) > epsDist || Math.abs(alongVel) > epsVel;
    }
}
