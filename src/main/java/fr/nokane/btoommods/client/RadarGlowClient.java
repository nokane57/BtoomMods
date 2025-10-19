package fr.nokane.btoommods.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.*;

/**
 * 🟢 RadarGlowClient :
 * - Glow VERT clair pour tous les joueurs détectés
 * - Inclut le joueur scanneur lui-même (visible sur son écran)
 * - Pas de particules
 * - Équipe unique pour éviter tout conflit avec les Remote BIM
 */
public final class RadarGlowClient {

    private static final Map<Integer, Long> GLOW_UNTIL = new HashMap<>();
    private static final String TEAM_NAME = "gl_radar_team"; // Nom unique pour éviter conflit

    private RadarGlowClient() {}

    public static void install() {
        if (FMLEnvironment.dist.isClient()) {
            MinecraftForge.EVENT_BUS.register(new RadarGlowClient());
        }
    }

    public static void apply(int[] ids, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long now = mc.level.getGameTime();
        long until = now + ticks;

        Scoreboard board = mc.level.getScoreboard();

        // 🟢 Crée ou met à jour la team radar (couleur verte forcée)
        ScorePlayerTeam team = board.getPlayerTeam(TEAM_NAME);
        if (team == null) {
            team = board.addPlayerTeam(TEAM_NAME);
        }
        team.setColor(TextFormatting.GREEN);
        team.setSeeFriendlyInvisibles(false);
        team.setAllowFriendlyFire(true);
        team.setNameTagVisibility(ScorePlayerTeam.Visible.ALWAYS);
        team.setCollisionRule(ScorePlayerTeam.CollisionRule.NEVER);

        // 🔁 Applique le glow à toutes les entités détectées
        for (int id : ids) {
            Entity e = mc.level.getEntity(id);
            if (e == null || e.removed) continue;

            e.setGlowing(true);

            // ✅ Retire toute autre team avant d’ajouter à celle du radar
            ScorePlayerTeam curTeam = board.getPlayersTeam(e.getScoreboardName());
            if (curTeam != null && curTeam != team) {
                board.removePlayerFromTeam(e.getScoreboardName(), curTeam);
            }

            board.addPlayerToTeam(e.getScoreboardName(), team);
            GLOW_UNTIL.put(id, until);
        }

        // ✳️ Force le glowing local du joueur (pour qu’il se voie lui-même)
        PlayerEntity self = mc.player;
        if (self != null) {
            self.setGlowing(true);
            board.addPlayerToTeam(self.getScoreboardName(), team);
            GLOW_UNTIL.put(self.getId(), until);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long now = mc.level.getGameTime();
        Iterator<Map.Entry<Integer, Long>> it = GLOW_UNTIL.entrySet().iterator();
        Scoreboard board = mc.level.getScoreboard();

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

            // 🔚 Fin du glow radar
            if (now >= expire) {
                ent.setGlowing(false);

                String name = ent.getScoreboardName();
                ScorePlayerTeam curTeam = board.getPlayersTeam(name);
                if (curTeam != null && TEAM_NAME.equals(curTeam.getName())) {
                    board.removePlayerFromTeam(name, curTeam);
                }

                it.remove();
            }
        }
    }
}
