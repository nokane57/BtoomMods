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

/**
 * 🔴 Gère uniquement les effets Glow des Remote BIMs, projectiles, etc.
 * (Radar Glow → séparé dans RadarGlowClient)
 */
public final class GlowClient {

    private static final Map<Integer, Long> GLOW_UNTIL = new HashMap<>();
    private static final Map<Integer, String> ENTITY_TEAM = new HashMap<>();
    private static final String TEAM_PREFIX = "gl_remote_";

    private GlowClient() {}

    public static void install() {
        if (FMLEnvironment.dist.isClient()) {
            MinecraftForge.EVENT_BUS.register(new GlowClient());
        }
    }

    /**
     * Active un effet de glowing coloré avec une légère aura (pour Remotes / BIMs)
     */
    public static void apply(int[] ids, int ticks, int colorARGB) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long now = mc.level.getGameTime();
        long until = now + ticks;

        int slot = ((colorARGB >> 24) & 0xFF);
        if (slot < 1 || slot > 8) slot = 1;

        TextFormatting fmt = SLOT_COLOR.getOrDefault(slot, TextFormatting.RED);
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

            e.setGlowing(true);

            String sbName = e.getScoreboardName();
            ScorePlayerTeam cur = mc.level.getScoreboard().getPlayersTeam(sbName);
            if (cur != team) {
                if (cur != null)
                    mc.level.getScoreboard().removePlayerFromTeam(sbName, cur);
                mc.level.getScoreboard().addPlayerToTeam(sbName, team);
            }
            ENTITY_TEAM.put(id, teamName);
            GLOW_UNTIL.merge(id, until, Math::max);

            // 🌟 Aura de particules rouges / énergiques
            Vector3d pos = e.position();
            for (int i = 0; i < 6; i++) {
                double spread = 0.25 + mc.level.random.nextDouble() * 0.2;
                mc.level.addParticle(ParticleTypes.END_ROD,
                        pos.x + (mc.level.random.nextDouble() - 0.5) * spread,
                        pos.y + 0.1 + (mc.level.random.nextDouble() - 0.5) * spread,
                        pos.z + (mc.level.random.nextDouble() - 0.5) * spread,
                        0, 0.015 + mc.level.random.nextDouble() * 0.02, 0);
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

            if (!ent.isGlowing()) ent.setGlowing(true);

            double dist = (mc.player != null) ? mc.player.distanceToSqr(ent) : 0.0;
            if (dist < 600 * 600) {
                int pulse = (int) ((System.currentTimeMillis() / 250) % 6);
                for (int i = 0; i < pulse + 2; i++) {
                    double offset = 0.25 + mc.level.random.nextDouble() * 0.1;
                    mc.level.addParticle(ParticleTypes.END_ROD,
                            ent.getX() + (mc.level.random.nextDouble() - 0.5) * offset,
                            ent.getY() + 0.1,
                            ent.getZ() + (mc.level.random.nextDouble() - 0.5) * offset,
                            0.0, 0.015, 0.0);
                }
            }

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

    // ---------- Couleurs par slot (Remotes uniquement) ----------
    private static final Map<Integer, TextFormatting> SLOT_COLOR = new HashMap<>();
    static {
        SLOT_COLOR.put(1, TextFormatting.RED);
        SLOT_COLOR.put(2, TextFormatting.YELLOW);
        SLOT_COLOR.put(3, TextFormatting.LIGHT_PURPLE);
        SLOT_COLOR.put(4, TextFormatting.AQUA);
        SLOT_COLOR.put(5, TextFormatting.BLUE);
        SLOT_COLOR.put(6, TextFormatting.GOLD);
        SLOT_COLOR.put(7, TextFormatting.DARK_RED);
        SLOT_COLOR.put(8, TextFormatting.DARK_AQUA);
    }

    /**
     * 📍 Marqueur du propriétaire du Remote BIM
     * Affiche un effet visuel de particules blanches et lumineuses.
     */
    public static void spawnRemoteOwnerMarker(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // Petits éclats lumineux autour du point
        for (int i = 0; i < 10; i++) {
            double spread = 0.3;
            mc.level.addParticle(ParticleTypes.END_ROD,
                    x + (mc.level.random.nextDouble() - 0.5) * spread,
                    y + 0.1 + (mc.level.random.nextDouble() * 0.3),
                    z + (mc.level.random.nextDouble() - 0.5) * spread,
                    0, 0.02, 0);
        }

        // Quelques particules critiques pour un effet d’impact
        for (int i = 0; i < 4; i++) {
            mc.level.addParticle(ParticleTypes.CRIT,
                    x, y + 0.1, z,
                    0.0, 0.03, 0.0);
        }
    }
}
