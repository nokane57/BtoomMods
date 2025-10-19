package fr.nokane.btoommods.net;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.radar.RadarStorage;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.*;
import java.util.function.Supplier;

public class RadarScanC2S {

    private static final List<ActiveRadarWave> ACTIVE_WAVES = new ArrayList<>();

    public static void encode(RadarScanC2S msg, net.minecraft.network.PacketBuffer buf) {}
    public static RadarScanC2S decode(net.minecraft.network.PacketBuffer buf) { return new RadarScanC2S(); }

    public static void handle(RadarScanC2S msg, Supplier<net.minecraftforge.fml.network.NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) return;

            ServerWorld world = player.getLevel();
            int totalRadars = RadarStorage.get(player);
            if (totalRadars <= 0) return;

            int base = ModConfigs.RADAR.RADAR_BASE_RADIUS.get();
            int per = ModConfigs.RADAR.RADAR_EXTRA_PER_ITEM.get();
            int radius = base + totalRadars * per;
            int duration = ModConfigs.RADAR.RADAR_WAVE_DURATION.get() + totalRadars * 5;
            int glowTicks = ModConfigs.RADAR.RADAR_GLOW_TICKS.get();
            boolean ignoreSneak = ModConfigs.RADAR.RADAR_IGNORE_SNEAK.get();

            SoundUtils.playWorldSound(world, player.getX(), player.getY(), player.getZ(),
                    fr.nokane.btoommods.sound.ModSounds.SONAR_ITEM.get(),
                    SoundUtils.VOL_SONAR, 1.0F);

            Net.CH.send(PacketDistributor.PLAYER.with(() -> player),
                    new RadarWaveS2C(player.getX(), player.getY(), player.getZ(), radius, duration));

            ACTIVE_WAVES.add(new ActiveRadarWave(world, player, radius, duration, glowTicks, ignoreSneak));
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
        private int age = 0;

        // État : glowing / message déjà envoyé
        private final Map<Integer, Boolean> entityGlowState = new HashMap<>();
        private final Set<Integer> messagedEntities = new HashSet<>();

        ActiveRadarWave(ServerWorld w, ServerPlayerEntity o, int r, int d, int g, boolean s) {
            world = w;
            origin = o;
            maxRadius = r;
            duration = d;
            glowTicks = g;
            ignoreSneak = s;
        }

        boolean tick() {
            if (origin == null || !origin.isAlive() || ++age > duration) return false;

            double progress = (double) age / duration;
            double currentRadius = progress * maxRadius;
            double radiusSq = currentRadius * currentRadius;

            double glowRange = ModConfigs.RADAR.RADAR_GLOW_VISIBLE_RANGE.get();
            double messageRange = ModConfigs.RADAR.RADAR_MESSAGE_RANGE.get();
            double glowRangeSq = glowRange * glowRange;
            double messageRangeSq = messageRange * messageRange;

            // === Détection des joueurs ===
            for (ServerPlayerEntity target : world.players()) {
                if (target == null || target.isSpectator()) continue;
                if (ignoreSneak && target.isCrouching() && target != origin) continue;

                double distSq = origin.distanceToSqr(target);
                if (distSq > radiusSq) continue;

                boolean wasGlowing = entityGlowState.getOrDefault(target.getId(), false);
                boolean shouldGlow = distSq <= glowRangeSq;

                // 💚 Glow visible
                if (!wasGlowing && shouldGlow) {
                    Net.CH.send(PacketDistributor.PLAYER.with(() -> origin),
                            new RadarGlowS2C(glowTicks, new int[]{target.getId()}));
                    if (target != origin)
                        Net.CH.send(PacketDistributor.PLAYER.with(() -> target),
                                new RadarGlowS2C(glowTicks, new int[]{origin.getId()}));
                    entityGlowState.put(target.getId(), true);
                }

                // 💬 Message unique au-delà du glow
                else if (!shouldGlow && distSq <= messageRangeSq && !messagedEntities.contains(target.getId())) {
                    double dist = Math.sqrt(distSq);
                    origin.sendMessage(
                            new TranslationTextComponent("message.btoommods.radar.detected_player",
                                    target.getName().getString(), (int) dist,
                                    (int) target.getX(), (int) target.getY(), (int) target.getZ())
                                    .withStyle(TextFormatting.AQUA),
                            Util.NIL_UUID);
                    target.sendMessage(
                            new TranslationTextComponent("message.btoommods.radar.alert_target",
                                    origin.getName().getString(),
                                    (int) origin.getX(), (int) origin.getY(), (int) origin.getZ())
                                    .withStyle(TextFormatting.RED),
                            Util.NIL_UUID);
                    messagedEntities.add(target.getId());
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
                boolean wasGlowing = entityGlowState.getOrDefault(item.getId(), false);
                boolean shouldGlow = distSq <= glowRangeSq;

                if (!wasGlowing && shouldGlow) {
                    Net.CH.send(PacketDistributor.PLAYER.with(() -> origin),
                            new RadarGlowS2C(glowTicks, new int[]{item.getId()}));
                    entityGlowState.put(item.getId(), true);
                }
                // 💬 Message unique pour chaque objet radar
                else if (!shouldGlow && distSq <= messageRangeSq && !messagedEntities.contains(item.getId())) {
                    double dist = Math.sqrt(distSq);
                    origin.sendMessage(
                            new TranslationTextComponent("message.btoommods.radar.detected_object",
                                    (int) dist, (int) item.getX(), (int) item.getY(), (int) item.getZ())
                                    .withStyle(TextFormatting.GRAY),
                            Util.NIL_UUID);
                    messagedEntities.add(item.getId());
                }
            }

            return true;
        }
    }
}
