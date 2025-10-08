package fr.nokane.btoommods.config.gas;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration du gaz BIM :
 * - explosion différée, propagation horizontale au sol
 * - dégâts constants dans la zone visible (disque)
 * - rebonds et poids calqués sur le Timer BIM
 */
public class GasConfig {

    public final ForgeConfigSpec.IntValue GAS_ENABLED_STACK;
    public final ForgeConfigSpec.IntValue GAS_DISABLED_STACK;

    public final ForgeConfigSpec.DoubleValue VITESSE_PROJECTILE;
    public final ForgeConfigSpec.DoubleValue POIDS_PROJECTILE;

    public final ForgeConfigSpec.DoubleValue GAS_IMPACT_HEARTS;
    public final ForgeConfigSpec.BooleanValue GAS_DMG_BYPASS_ARMOR;

    public final ForgeConfigSpec.IntValue GAS_EXPLODE_AFTER_TICKS;
    public final ForgeConfigSpec.IntValue GAS_LIFETIME_TICKS;
    public final ForgeConfigSpec.IntValue GAS_PICKUP_AFTER_TICKS;

    public final ForgeConfigSpec.IntValue RADIUS;
    public final ForgeConfigSpec.DoubleValue GAS_DAMAGE_HEARTH;
    public final ForgeConfigSpec.DoubleValue GAS_STORM_DAMAGE_HEARTH;

    public final ForgeConfigSpec.DoubleValue RESTITUTION_GROUND;
    public final ForgeConfigSpec.DoubleValue FRICTION_GROUND;
    public final ForgeConfigSpec.DoubleValue RESTITUTION_WALL;
    public final ForgeConfigSpec.DoubleValue FRICTION_WALL;
    public final ForgeConfigSpec.DoubleValue MAX_BOUNCE_UP;
    public final ForgeConfigSpec.DoubleValue STOP_EPS;
    public final ForgeConfigSpec.DoubleValue GAS_MIN_HEIGHT;
    public final ForgeConfigSpec.DoubleValue GAS_MAX_HEIGHT;

    public GasConfig(ForgeConfigSpec.Builder b) {
        b.push("gas_bim");

        // === Stack et propriétés générales ===
        GAS_ENABLED_STACK = b.comment(
                "FR: Taille max de stack pour la bombe à gaz activée.",
                "EN: Max stack size for active gas bomb."
        ).defineInRange("enabled_stack", 16, 1, 64);

        GAS_DISABLED_STACK = b.comment(
                "FR: Taille max de stack pour la coque vide (gaz désactivé).",
                "EN: Max stack size for disabled gas shell."
        ).defineInRange("disabled_stack", 16, 1, 64);

        // === Physique du projectile ===
        VITESSE_PROJECTILE = b.comment(
                "FR: Multiplicateur de vitesse initiale du projectile de gaz.",
                "EN: Initial throw speed multiplier."
        ).defineInRange("projectile_speed_mult", 0.85D, 0.1D, 10.0D);

        POIDS_PROJECTILE = b.comment(
                "FR: Coefficient de gravité du projectile (poids).",
                "EN: Projectile gravity weight factor."
        ).defineInRange("projectile_weight", 1.0D, 0.1D, 10.0D);

        // === Dégâts ===
        GAS_IMPACT_HEARTS = b.comment(
                "FR: Dégâts en cœurs infligés à l’impact du projectile (avant le nuage).",
                "EN: Damage (in hearts) when hitting an entity before gas release."
        ).defineInRange("impact_hearts", 1.0D, 0.0D, 50.0D);

        GAS_DMG_BYPASS_ARMOR = b.comment(
                "FR: Si vrai, le gaz ignore l’armure des entités.",
                "EN: If true, gas ignores armor protection."
        ).define("bypass_armor", true);

        // === Durées ===
        GAS_EXPLODE_AFTER_TICKS = b.comment(
                "FR: Délai avant que le nuage de gaz commence à se former (ticks).",
                "EN: Delay before gas cloud spawns (in ticks)."
        ).defineInRange("explode_after_ticks", 60, 5, 20 * 60);

        GAS_LIFETIME_TICKS = b.comment(
                "FR: Durée de vie totale du nuage de gaz (ticks).",
                "EN: Total lifetime of the gas cloud entity (in ticks)."
        ).defineInRange("lifetime_ticks", 200, 20, 20 * 60);

        GAS_PICKUP_AFTER_TICKS = b.comment(
                "FR: Délai avant qu’une bombe posée puisse être ramassée (ticks).",
                "EN: Delay before gas bomb becomes pickable again (ticks)."
        ).defineInRange("pickup_after_ticks", 20, 0, 20 * 60);

        // === Zone et dégâts continus ===
        RADIUS = b.comment(
                "FR: Rayon maximal du nuage de gaz (en blocs).",
                "EN: Maximum horizontal spread radius (in blocks)."
        ).defineInRange("radius", 45, 5, 128);

        GAS_DAMAGE_HEARTH = b.comment(
                "FR: Dégâts par seconde (en cœurs) du gaz au sol.",
                "EN: Damage per second (in hearts) for entities inside the gas."
        ).defineInRange("damage_hearts_per_second", 1.0D, 0.0D, 50.0D);

        GAS_STORM_DAMAGE_HEARTH = b.comment(
                "FR: Dégâts par seconde (en cœurs) pendant la pluie.",
                "EN: Damage per second (in hearts) during rain."
        ).defineInRange("storm_damage_hearts_per_second", 2.5D, 0.0D, 100.0D);

        // === Physique du rebond (identique au Timer BIM) ===
        RESTITUTION_GROUND = b.comment(
                "FR: Coefficient de rebond vertical au sol.",
                "EN: Vertical restitution coefficient (ground)."
        ).defineInRange("restitution_ground", 0.35D, 0.0D, 1.0D);

        FRICTION_GROUND = b.comment(
                "FR: Friction horizontale au sol.",
                "EN: Horizontal friction on ground contact."
        ).defineInRange("friction_ground", 0.55D, 0.0D, 1.0D);

        RESTITUTION_WALL = b.comment(
                "FR: Coefficient de rebond contre les murs.",
                "EN: Restitution on wall collision."
        ).defineInRange("restitution_wall", 0.35D, 0.0D, 1.0D);

        FRICTION_WALL = b.comment(
                "FR: Friction horizontale contre les murs.",
                "EN: Horizontal friction on walls."
        ).defineInRange("friction_wall", 0.75D, 0.0D, 1.0D);

        MAX_BOUNCE_UP = b.comment(
                "FR: Vitesse verticale maximale après rebond.",
                "EN: Maximum upward velocity after bounce."
        ).defineInRange("max_bounce_up", 0.18D, 0.0D, 1.0D);

        STOP_EPS = b.comment(
                "FR: Tolérance d’arrêt du projectile (plus haut = s’arrête plus vite).",
                "EN: Stop epsilon — higher = stops sooner."
        ).defineInRange("stop_eps", 0.04D, 0.0D, 0.5D);

        GAS_MIN_HEIGHT = b.comment(
                "Hauteur en blocs sous le sol affectée par le gaz.",
                "Utile pour frapper les entités légèrement plus basses."
        ).defineInRange("min_height", 1.0D, 0.0D, 64.0D);

        GAS_MAX_HEIGHT = b.comment(
                "Hauteur en blocs au-dessus du sol affectée par le gaz.",
                "Détermine l'épaisseur verticale du nuage."
        ).defineInRange("max_height", 3.0D, 0.5D, 64.0D);

        b.pop();
    }
}
