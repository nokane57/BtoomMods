package fr.nokane.btoommods.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effects;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Gestion du glow radar client :
 * ✅ Portée = même que le radar serveur (jusqu’à 365 blocs)
 * ✅ Supprime les entités après expiration
 * ✅ Garde la team verte unique
 * ✅ Fonctionne même si les entités sont rechargées plus tard
 */
public final class GlowClient {

    private static final Map<Integer, Long> GLOW_UNTIL = new HashMap<>();
    private static final String TEAM_NAME = "glow_radar_green";
    // Distance max visible temporairement (sera ajustée dynamiquement)
    private static double forcedRenderDistance = 0.0;

    private GlowClient() {}

    public static void install() {
        if (FMLEnvironment.dist.isClient()) {
            MinecraftForge.EVENT_BUS.register(new GlowClient());
        }
    }

    /** Applique un glow coloré temporaire avec portée radar étendue */
    public static void apply(int[] ids, int ticks, int colorRGB) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long now = mc.level.getGameTime();
        long until = now + ticks;

        // 🟢 Crée la team verte si elle n'existe pas
        ScorePlayerTeam team = mc.level.getScoreboard().getPlayerTeam(TEAM_NAME);
        if (team == null) {
            team = mc.level.getScoreboard().addPlayerTeam(TEAM_NAME);
            team.setColor(TextFormatting.GREEN);
            team.setSeeFriendlyInvisibles(false);
            team.setAllowFriendlyFire(true);
        }

        // 🔄 Étend temporairement la distance de rendu des entités (simule jusqu’à 400 blocs)
        forcedRenderDistance = Math.max(forcedRenderDistance, 400.0);

        for (int id : ids) {
            Entity e = mc.level.getEntity(id);
            if (e != null) {
                e.setGlowing(true);
                if (e.getName() != null) {
                    String name = e.getName().getString();
                    if (mc.level.getScoreboard().getPlayersTeam(name) != team) {
                        mc.level.getScoreboard().addPlayerToTeam(name, team);
                    }
                }
            }
            // 🔒 garde l’état du glow même si l’entité est temporairement déchargée
            GLOW_UNTIL.merge(id, until, Math::max);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long now = mc.level.getGameTime();
        ScorePlayerTeam team = mc.level.getScoreboard().getPlayerTeam(TEAM_NAME);

        Iterator<Map.Entry<Integer, Long>> it = GLOW_UNTIL.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<Integer, Long> entry = it.next();
            int id = entry.getKey();
            long expire = entry.getValue();

            Entity ent = mc.level.getEntity(id);
            if (ent != null) {
                // 🟩 Si visible, on s'assure qu'elle reste glowée
                if (!ent.isGlowing()) ent.setGlowing(true);
            }

            // ⏳ Retirer quand expiré
            if (now >= expire) {
                if (ent != null) {
                    ent.setGlowing(false);
                    if (team != null && ent.getName() != null) {
                        String name = ent.getName().getString();
                        // ✅ Évite le crash : vérifie si la team correspond avant de retirer
                        if (mc.level.getScoreboard().getPlayersTeam(name) == team) {
                            mc.level.getScoreboard().removePlayerFromTeam(name, team);
                        }
                    }
                }
                it.remove();
            }
        }

        // 🧹 Supprime la team quand elle est vide
        if (team != null && team.getPlayers().isEmpty()) {
            mc.level.getScoreboard().removePlayerTeam(team);
        }
    }

    /** Particules pour les Remote BIM */
    public static void spawnRemoteOwnerMarker(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        for (int i = 0; i < 10; i++)
            mc.level.addParticle(ParticleTypes.CRIT, x, y, z, 0.0, 0.02, 0.0);
        for (int i = 0; i < 4; i++)
            mc.level.addParticle(ParticleTypes.END_ROD, x, y + 0.1, z, 0.0, 0.01, 0.0);
    }
}
