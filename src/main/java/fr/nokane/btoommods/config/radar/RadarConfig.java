package fr.nokane.btoommods.config.radar;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * ⚙️ Configuration du système radar (implant + scan progressif)
 */
public class RadarConfig {

    public final ForgeConfigSpec.IntValue STACK;
    public final ForgeConfigSpec.IntValue RADAR_BASE_RADIUS;
    public final ForgeConfigSpec.IntValue RADAR_EXTRA_PER_ITEM;
    public final ForgeConfigSpec.IntValue RADAR_GLOW_TICKS;
    public final ForgeConfigSpec.IntValue RADAR_ACTIVE_WINDOW;
    public final ForgeConfigSpec.IntValue RADAR_COOLDOWN_TICKS;

    public final ForgeConfigSpec.IntValue RADAR_WAVE_DURATION;
    public final ForgeConfigSpec.BooleanValue RADAR_IGNORE_SNEAK;
    public final ForgeConfigSpec.BooleanValue RADAR_REQUIRE_MOVEMENT;

    // 🆕 Distances de visibilité et de message (BASE - augmentent avec le nombre de radars)
    public final ForgeConfigSpec.IntValue RADAR_GLOW_VISIBLE_RANGE;
    public final ForgeConfigSpec.IntValue ACTIVATION_MESSAGE;

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
                "    S'applique au rayon total ET à la portée de glow.",
                "EN: Extra scan range per additional Radar item.",
                "    Applies to total radius AND glow range."
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

        RADAR_WAVE_DURATION = b.comment(
                "FR: Durée (en ticks) de la propagation complète de l'onde radar (effet + détection).",
                "EN: Total duration (ticks) of the radar wave propagation."
        ).defineInRange("wave_duration_ticks", 40, 10, 200);

        RADAR_IGNORE_SNEAK = b.comment(
                "FR: Si vrai, les joueurs accroupis ne sont pas détectés par le radar.",
                "EN: If true, sneaking players are ignored by radar scans."
        ).define("ignore_sneak", true);

        RADAR_REQUIRE_MOVEMENT = b.comment(
                "FR: Si vrai, seules les entités ayant bougé récemment sont détectées.",
                "EN: If true, only recently moving players are detected."
        ).define("require_movement", false);

        // 🆕 Distance maximale de visibilité du glowing (BASE, augmente avec radars)
        RADAR_GLOW_VISIBLE_RANGE = b.comment(
                "FR: Distance maximale de BASE (en blocs) à laquelle le Glow est visible.",
                "    Cette valeur augmente avec le nombre de radars.",
                "    Formule: glowRange = BASE + (nbRadars - 1) * extra_per_item",
                "    Zone glow: [0 - glowRange] → Effet lumineux",
                "EN: BASE maximum distance (blocks) at which glowing is visible.",
                "    This value increases with the number of radars owned.",
                "    Formula: glowRange = BASE + (nbRadars - 1) * extra_per_item",
                "    Glow zone: [0 - glowRange] → Visual glow effect"
        ).defineInRange("glow_visible_range", 35, 1, 512);

        // 🆕 Distance d'activation des messages (FIXE)
        ACTIVATION_MESSAGE = b.comment(
                "FR: Distance (en blocs) à partir de laquelle les messages de détection apparaissent.",
                "    Cette valeur est FIXE et ne change pas avec le nombre de radars.",
                "    Les messages apparaissent de cette distance jusqu'au rayon max du radar.",
                "    Exemple: Si 35, les messages apparaissent à partir de 35+ blocs",
                "    Zone message: [cette valeur - radarRadius] → Messages texte",
                "EN: Distance (blocks) from which detection messages start appearing.",
                "    This value is FIXED and does not change with number of radars.",
                "    Messages appear from this distance to radar's max radius.",
                "    Example: If 35, messages appear from 35+ blocks onwards",
                "    Message zone: [this value - radarRadius] → Text messages"
        ).defineInRange("activation_message", 35, 1, 512);

        b.pop();
    }
}