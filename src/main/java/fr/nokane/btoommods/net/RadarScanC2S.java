package fr.nokane.btoommods.net;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.radar.RadarStorage;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Supplier;

public class RadarScanC2S {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<ActiveRadarWave> ACTIVE_WAVES = new ArrayList<>();

    public static void encode(RadarScanC2S msg, net.minecraft.network.PacketBuffer buf) {}
    public static RadarScanC2S decode(net.minecraft.network.PacketBuffer buf) { return new RadarScanC2S(); }

    public static void handle(RadarScanC2S msg, Supplier<net.minecraftforge.fml.network.NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) return;

            // ❌ Empêcher les spectateurs d'utiliser le radar
            if (player.isSpectator()) {
                LOGGER.info("[RADAR] Player {} cannot use radar in spectator mode", player.getName().getString());
                return;
            }

            ServerWorld world = player.getLevel();
            int totalRadars = RadarStorage.get(player);
            if (totalRadars <= 0) return;

            int base = ModConfigs.RADAR.RADAR_BASE_RADIUS.get();
            int per = ModConfigs.RADAR.RADAR_EXTRA_PER_ITEM.get();
            int radius = base + totalRadars * per;
            int duration = ModConfigs.RADAR.RADAR_WAVE_DURATION.get() + totalRadars * 5;
            int glowTicks = ModConfigs.RADAR.RADAR_GLOW_TICKS.get();
            boolean ignoreSneak = ModConfigs.RADAR.RADAR_IGNORE_SNEAK.get();

            int baseGlowRange = ModConfigs.RADAR.RADAR_GLOW_VISIBLE_RANGE.get();
            int messageActivation = ModConfigs.RADAR.ACTIVATION_MESSAGE.get();
            int glowRange = baseGlowRange + (totalRadars - 1) * per;

            LOGGER.info("[RADAR] Player {} scanning with {} radars - Radius: {}, Glow: [0-{}], Message activation: {}+",
                    player.getName().getString(), totalRadars, radius, glowRange, messageActivation);

            SoundUtils.playWorldSound(world, player.getX(), player.getY(), player.getZ(),
                    fr.nokane.btoommods.sound.ModSounds.SONAR_ITEM.get(),
                    SoundUtils.VOL_SONAR, 1.0F);

            Net.CH.send(PacketDistributor.PLAYER.with(() -> player),
                    new RadarWaveS2C(player.getX(), player.getY(), player.getZ(), radius, duration));

            ACTIVE_WAVES.add(new ActiveRadarWave(world, player, radius, duration, glowTicks,
                    ignoreSneak, glowRange, messageActivation));
        });
        ctx.get().setPacketHandled(true);
    }

    static { MinecraftForge.EVENT_BUS.register(RadarScanC2S.class); }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ACTIVE_WAVES.isEmpty()) return;
        ACTIVE_WAVES.removeIf(w -> !w.tick());
    }

    private static class ActiveRadarWave {
        private final ServerWorld world;
        private final ServerPlayerEntity origin;
        private final int maxRadius, duration, glowTicks;
        private final boolean ignoreSneak;
        private final int glowRange;
        private final int messageActivation;
        private int age = 0;

        private final Map<Integer, Boolean> entityGlowState = new HashMap<>();
        private final Set<Integer> messagedEntities = new HashSet<>();

        ActiveRadarWave(ServerWorld w, ServerPlayerEntity o, int r, int d, int g, boolean s,
                        int glowR, int msgActivation) {
            world = w;
            origin = o;
            maxRadius = r;
            duration = d;
            glowTicks = g;
            ignoreSneak = s;
            glowRange = glowR;
            messageActivation = msgActivation;

            LOGGER.info("[RADAR WAVE] Created - MaxRadius: {}, GlowRange: [0-{}], Message activation: {}+",
                    maxRadius, glowRange, messageActivation);
        }

        boolean tick() {
            if (origin == null || !origin.isAlive() || ++age > duration) return false;

            double progress = (double) age / duration;
            double currentRadius = progress * maxRadius;
            double radiusSq = currentRadius * currentRadius;
            double glowRangeSq = glowRange * glowRange;

            LOGGER.debug("[RADAR WAVE] Tick {}/{}, currentRadius={}, glowRange={}, messageActivation={}",
                    age, duration, (int)currentRadius, glowRange, messageActivation);

            // === Détection des joueurs ===
            for (ServerPlayerEntity target : world.players()) {
                if (target == null || target.isSpectator()) continue;
                if (ignoreSneak && target.isCrouching() && target != origin) continue;

                double distSq = origin.distanceToSqr(target);
                double dist = Math.sqrt(distSq);

                if (distSq > radiusSq) continue;

                LOGGER.debug("[RADAR WAVE] Target {} at distance {} - GlowRange: {}, MessageActivation: {}",
                        target.getName().getString(), (int)dist, glowRange, messageActivation);

                boolean wasGlowing = entityGlowState.getOrDefault(target.getId(), false);
                boolean inGlowRange = dist <= glowRange;
                boolean inMessageRange = dist >= messageActivation;

                // 💚 Glow visible (0 - glowRange)
                if (!wasGlowing && inGlowRange) {
                    LOGGER.info("[RADAR SERVER] Applying glow to {} at {} blocks (glow range: {})",
                            target.getName().getString(), (int)dist, glowRange);

                    try {
                        Net.CH.send(PacketDistributor.PLAYER.with(() -> origin),
                                new RadarGlowS2C(glowTicks, new int[]{target.getId()}));

                        if (target != origin) {
                            Net.CH.send(PacketDistributor.PLAYER.with(() -> target),
                                    new RadarGlowS2C(glowTicks, new int[]{origin.getId()}));
                        }
                        entityGlowState.put(target.getId(), true);
                    } catch (Exception e) {
                        LOGGER.error("[RADAR SERVER] Error sending glow packet", e);
                    }
                }

                // 💬 Messages (distance >= messageActivation)
                if (inMessageRange && !messagedEntities.contains(target.getId())) {
                    int currentDist = (int)dist;

                    LOGGER.info("[RADAR SERVER] Player {} detected {} at {} blocks (message activation: {})",
                            origin.getName().getString(), target.getName().getString(),
                            currentDist, messageActivation);

                    try {
                        RadarMessageS2C scannerMsg = new RadarMessageS2C(
                                RadarMessageS2C.MessageType.DETECTED_PLAYER,
                                target.getName().getString(),
                                currentDist,
                                (int) target.getX(),
                                (int) target.getY(),
                                (int) target.getZ()
                        );

                        LOGGER.info("[RADAR SERVER] Sending DETECTED_PLAYER to scanner");
                        Net.CH.send(PacketDistributor.PLAYER.with(() -> origin), scannerMsg);

                        if (target != origin) {
                            RadarMessageS2C alertMsg = new RadarMessageS2C(
                                    RadarMessageS2C.MessageType.ALERT_TARGET,
                                    origin.getName().getString(),
                                    (int) origin.getX(),
                                    (int) origin.getY(),
                                    (int) origin.getZ(),
                                    0
                            );

                            LOGGER.info("[RADAR SERVER] Sending ALERT_TARGET to target");
                            Net.CH.send(PacketDistributor.PLAYER.with(() -> target), alertMsg);
                        }

                        messagedEntities.add(target.getId());

                    } catch (Exception e) {
                        LOGGER.error("[RADAR SERVER] Error sending message packets", e);
                    }
                }
            }

            // === Détection objets radar ===
            double minY = Math.max(0, origin.getY() - currentRadius);
            double maxY = Math.min(256, origin.getY() + currentRadius);
            AxisAlignedBB box = new AxisAlignedBB(
                    origin.getX() - currentRadius, minY, origin.getZ() - currentRadius,
                    origin.getX() + currentRadius, maxY, origin.getZ() + currentRadius
            );

            for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, box)) {
                if (!item.isAlive() || item.getItem().isEmpty()) continue;
                if (item.getItem().getItem() != ModItems.RADAR_ITEM.get()) continue;

                double distSq = origin.distanceToSqr(item);
                double dist = Math.sqrt(distSq);

                boolean wasGlowing = entityGlowState.getOrDefault(item.getId(), false);
                boolean inGlowRange = dist <= glowRange;
                boolean inMessageRange = dist >= messageActivation;

                if (!wasGlowing && inGlowRange) {
                    LOGGER.info("[RADAR SERVER] Applying glow to radar item at {} blocks", (int)dist);
                    Net.CH.send(PacketDistributor.PLAYER.with(() -> origin),
                            new RadarGlowS2C(glowTicks, new int[]{item.getId()}));
                    entityGlowState.put(item.getId(), true);
                }

                if (inMessageRange && !messagedEntities.contains(item.getId())) {
                    int currentDist = (int)dist;

                    LOGGER.info("[RADAR SERVER] Sending DETECTED_OBJECT message at {} blocks (activation: {})",
                            currentDist, messageActivation);

                    try {
                        Net.CH.send(PacketDistributor.PLAYER.with(() -> origin),
                                new RadarMessageS2C(
                                        RadarMessageS2C.MessageType.DETECTED_OBJECT,
                                        currentDist,
                                        (int) item.getX(),
                                        (int) item.getY(),
                                        (int) item.getZ()
                                ));

                        messagedEntities.add(item.getId());

                    } catch (Exception e) {
                        LOGGER.error("[RADAR SERVER] Error sending object message", e);
                    }
                }
            }

            return true;
        }
    }
}