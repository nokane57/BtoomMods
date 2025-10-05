package fr.nokane.btoommods.config.radar;

import net.minecraftforge.common.ForgeConfigSpec;

public class RadarConfig {

    public final ForgeConfigSpec.IntValue STACK;
    public final ForgeConfigSpec.IntValue RADAR_BASE_RADIUS;
    public final ForgeConfigSpec.IntValue RADAR_EXTRA_PER_ITEM;
    public final ForgeConfigSpec.IntValue RADAR_GLOW_TICKS;
    public final ForgeConfigSpec.IntValue RADAR_ACTIVE_WINDOW;
    public final ForgeConfigSpec.IntValue RADAR_COOLDOWN_TICKS;

    public RadarConfig(ForgeConfigSpec.Builder b) {
        b.push("radar");

        STACK = b.comment(
                "FR: Taille de stack de l'item Radar (implant).",
                "EN: Stack size for Radar item (implant)."
        ).defineInRange("stack", 64, 1, 64);

        RADAR_BASE_RADIUS = b.comment(
                "FR: Portée de base du balayage radar (en blocs).",
                "EN: Base scan radius (in blocks)."
        ).defineInRange("base_radius", 50, 1, 256);

        RADAR_EXTRA_PER_ITEM = b.comment(
                "FR: Bonus de portée par item Radar supplémentaire (booster).",
                "EN: Extra scan range per additional Radar item."
        ).defineInRange("extra_per_item", 5, 0, 64);

        RADAR_GLOW_TICKS = b.comment(
                "FR: Durée de l'effet Glow (ticks) sur les joueurs détectés.",
                "EN: Glow duration (ticks) applied to detected players."
        ).defineInRange("glow_ticks", 60, 1, 20 * 30);

        RADAR_ACTIVE_WINDOW = b.comment(
                "FR: Fenêtre d'activité (ticks) pendant laquelle un joueur est considéré actif.",
                "EN: Activity window (ticks) in which a player is considered active."
        ).defineInRange("active_window_ticks", 100, 1, 20 * 60);

        RADAR_COOLDOWN_TICKS = b.comment(
                "FR: Délai minimal entre deux scans radar (ticks).",
                "EN: Minimum cooldown (ticks) between radar scans."
        ).defineInRange("cooldown_ticks", 60, 0, 20 * 60);

        b.pop();
    }
}
