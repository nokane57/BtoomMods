package fr.nokane.btoommods.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.*;

public final class GlowClient {

    private static final Map<Integer, Long> GLOW_UNTIL = new HashMap<>();
    private static final Map<Integer, String> ENTITY_TEAM = new HashMap<>();

    private static final String TEAM_PREFIX = "gls_"; // court = safe
    private GlowClient() {}

    public static void install() {
        if (FMLEnvironment.dist.isClient()) {
            MinecraftForge.EVENT_BUS.register(new GlowClient());
        }
    }

    /**
     * Applique un glow coloré par slot (1–8).
     * Le slot est encodé dans les 3 bits faibles de colorARGB côté RemoteBimEntity.
     */
    public static void apply(int[] ids, int ticks, int colorARGB) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long now = mc.level.getGameTime();
        long until = now + ticks;

        // 🧩 slot est encodé par RemoteBimEntity (8 couleurs)
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
            if (e != null) {
                e.setGlowing(true);
                String sbName = e.getScoreboardName();
                ScorePlayerTeam cur = mc.level.getScoreboard().getPlayersTeam(sbName);
                if (cur != team) {
                    if (cur != null)
                        mc.level.getScoreboard().removePlayerFromTeam(sbName, cur);
                    mc.level.getScoreboard().addPlayerToTeam(sbName, team);
                }
                ENTITY_TEAM.put(id, teamName);
            }
            GLOW_UNTIL.merge(id, until, Math::max);
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
            if (ent != null && !ent.isGlowing()) ent.setGlowing(true);

            if (now >= expire) {
                if (ent != null) {
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
                }
                it.remove();
            }
        }
    }

    public static void spawnRemoteOwnerMarker(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (int i = 0; i < 10; i++)
            mc.level.addParticle(ParticleTypes.CRIT, x, y, z, 0.0, 0.02, 0.0);
        for (int i = 0; i < 4; i++)
            mc.level.addParticle(ParticleTypes.END_ROD, x, y + 0.1, z, 0.0, 0.01, 0.0);
    }

    // ---------- Table de correspondance slot → couleur vanilla ----------
    private static final Map<Integer, TextFormatting> SLOT_COLOR = new HashMap<>();
    static {
        SLOT_COLOR.put(1, TextFormatting.RED);
        SLOT_COLOR.put(2, TextFormatting.YELLOW);
        SLOT_COLOR.put(3, TextFormatting.GREEN);
        SLOT_COLOR.put(4, TextFormatting.AQUA);
        SLOT_COLOR.put(5, TextFormatting.LIGHT_PURPLE);
        SLOT_COLOR.put(6, TextFormatting.LIGHT_PURPLE);
        SLOT_COLOR.put(7, TextFormatting.RED);
        SLOT_COLOR.put(8, TextFormatting.DARK_AQUA);
    }
}
