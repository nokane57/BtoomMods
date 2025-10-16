package fr.nokane.btoommods.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.*;

public final class GlowClient {

    private static final Map<Integer, Long> GLOW_UNTIL = new HashMap<>();
    private static final Map<Integer, String> ENTITY_TEAM = new HashMap<>();
    private static final String TEAM_PREFIX = "gls_";

    private GlowClient() {}

    public static void install() {
        if (FMLEnvironment.dist.isClient()) {
            MinecraftForge.EVENT_BUS.register(new GlowClient());
        }
    }

    /**
     * 💡 Active un glow très visible, avec particules d’aura.
     */
    public static void apply(int[] ids, int ticks, int colorARGB) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long now = mc.level.getGameTime();
        long until = now + ticks;

        int slot = ((colorARGB >> 24) & 0xFF);
        if (slot < 1 || slot > 8) slot = 1;

        TextFormatting fmt = SLOT_COLOR.getOrDefault(slot, TextFormatting.WHITE);
        String teamName = TEAM_PREFIX + slot;

        ScorePlayerTeam team = mc.level.getScoreboard().getPlayerTeam(teamName);
        if (team == null) {
            team = mc.level.getScoreboard().addPlayerTeam(teamName);
            team.setColor(fmt);
            team.setSeeFriendlyInvisibles(false);
            team.setAllowFriendlyFire(true);
        }

        for (int id : ids) {
            Entity e = mc.level.getEntity(id);
            if (e == null || e.removed) continue;

            // ✅ vrai effet de glowing (visible à distance)
            e.setGlowing(true);

            // ✅ couleur via scoreboard
            String sbName = e.getScoreboardName();
            ScorePlayerTeam cur = mc.level.getScoreboard().getPlayersTeam(sbName);
            if (cur != team) {
                if (cur != null)
                    mc.level.getScoreboard().removePlayerFromTeam(sbName, cur);
                mc.level.getScoreboard().addPlayerToTeam(sbName, team);
            }
            ENTITY_TEAM.put(id, teamName);
            GLOW_UNTIL.merge(id, until, Math::max);

            // 🌟 Aura de particules (autour du projectile)
            Vector3d pos = e.position();
            for (int i = 0; i < 8; i++) {
                double spread = 0.25 + mc.level.random.nextDouble() * 0.2;
                mc.level.addParticle(ParticleTypes.END_ROD,
                        pos.x + (mc.level.random.nextDouble() - 0.5) * spread,
                        pos.y + 0.1 + (mc.level.random.nextDouble() - 0.5) * spread,
                        pos.z + (mc.level.random.nextDouble() - 0.5) * spread,
                        0, 0.01 + mc.level.random.nextDouble() * 0.02, 0);
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long now = mc.level.getGameTime();
        Iterator<Map.Entry<Integer, Long>> it = GLOW_UNTIL.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<Integer, Long> entry = it.next();
            int id = entry.getKey();
            long expire = entry.getValue();

            Entity ent = mc.level.getEntity(id);
            if (ent == null || ent.removed) {
                it.remove();
                continue;
            }

            // 🔥 Maintien du glow visible
            if (!ent.isGlowing()) ent.setGlowing(true);

            // 💫 Halo visible à grande distance
            double dist = (mc.player != null) ? mc.player.distanceToSqr(ent) : 0.0;
            if (dist < 600 * 600) { // visible jusqu’à 600 blocs
                // pulsation douce : varie la quantité selon le temps
                int pulse = (int) ((System.currentTimeMillis() / 200) % 6);
                for (int i = 0; i < pulse + 2; i++) {
                    double offset = 0.2 + mc.level.random.nextDouble() * 0.15;
                    mc.level.addParticle(ParticleTypes.END_ROD,
                            ent.getX() + (mc.level.random.nextDouble() - 0.5) * offset,
                            ent.getY() + 0.1,
                            ent.getZ() + (mc.level.random.nextDouble() - 0.5) * offset,
                            0.0, 0.02, 0.0);
                }

                // ✨ éclats blancs discrets
                if (mc.level.random.nextFloat() < 0.2F) {
                    mc.level.addParticle(ParticleTypes.CRIT,
                            ent.getX(), ent.getY() + 0.05, ent.getZ(),
                            0.0, 0.03, 0.0);
                }
            }

            // 🧹 expiration
            if (now >= expire) {
                ent.setGlowing(false);
                String teamName = ENTITY_TEAM.remove(id);
                if (teamName != null) {
                    ScorePlayerTeam team = mc.level.getScoreboard().getPlayerTeam(teamName);
                    if (team != null) {
                        String sbName = ent.getScoreboardName();
                        if (mc.level.getScoreboard().getPlayersTeam(sbName) == team)
                            mc.level.getScoreboard().removePlayerFromTeam(sbName, team);
                    }
                }
                it.remove();
            }
        }
    }

    public static void spawnRemoteOwnerMarker(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (int i = 0; i < 12; i++) {
            double spread = 0.3;
            mc.level.addParticle(ParticleTypes.END_ROD,
                    x + (mc.level.random.nextDouble() - 0.5) * spread,
                    y + 0.1,
                    z + (mc.level.random.nextDouble() - 0.5) * spread,
                    0, 0.02, 0);
        }
        for (int i = 0; i < 5; i++) {
            mc.level.addParticle(ParticleTypes.CRIT,
                    x, y + 0.1, z,
                    0.0, 0.03, 0.0);
        }
    }

    // ---------- Couleurs de slot ----------
    private static final Map<Integer, TextFormatting> SLOT_COLOR = new HashMap<>();
    static {
        SLOT_COLOR.put(1, TextFormatting.RED);
        SLOT_COLOR.put(2, TextFormatting.YELLOW);
        SLOT_COLOR.put(3, TextFormatting.GREEN);
        SLOT_COLOR.put(4, TextFormatting.AQUA);
        SLOT_COLOR.put(5, TextFormatting.LIGHT_PURPLE);
        SLOT_COLOR.put(6, TextFormatting.BLUE);
        SLOT_COLOR.put(7, TextFormatting.GOLD);
        SLOT_COLOR.put(8, TextFormatting.DARK_AQUA);
    }
}
