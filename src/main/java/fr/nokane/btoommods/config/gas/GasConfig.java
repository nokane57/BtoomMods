package fr.nokane.btoommods.config.gas;

import net.minecraftforge.common.ForgeConfigSpec;

public class GasConfig {

    public final ForgeConfigSpec.IntValue GAS_ENABLED_STACK;
    public final ForgeConfigSpec.IntValue GAS_DISABLED_STACK;
    public final ForgeConfigSpec.DoubleValue VITESSE_PROJECTILE;
    public final ForgeConfigSpec.DoubleValue POIDS_PROJECTILE;

    public final ForgeConfigSpec.DoubleValue GAS_IMPACT_HEARTS;
    public final ForgeConfigSpec.BooleanValue GAS_DMG_BYPASS_ARMOR;
    public final ForgeConfigSpec.IntValue GAS_EXPLODE_AFTER_TICKS;
    public final ForgeConfigSpec.IntValue GAS_PICKUP_AFTER_TICKS;

    public final ForgeConfigSpec.IntValue RADIUS;
    public final ForgeConfigSpec.DoubleValue GAS_DAMAGE_HEARTH;
    public final ForgeConfigSpec.DoubleValue GAS_STORM_DAMAGE_HEARTH; // ✅ dégâts pendant la pluie
    public final ForgeConfigSpec.DoubleValue GAS_MIN_HEIGHT;
    public final ForgeConfigSpec.DoubleValue GAS_MAX_HEIGHT;

    public final ForgeConfigSpec.DoubleValue RESTITUTION_GROUND;
    public final ForgeConfigSpec.DoubleValue FRICTION_GROUND;
    public final ForgeConfigSpec.DoubleValue RESTITUTION_WALL;
    public final ForgeConfigSpec.DoubleValue FRICTION_WALL;
    public final ForgeConfigSpec.DoubleValue MAX_BOUNCE_UP;
    public final ForgeConfigSpec.DoubleValue STOP_EPS;

    public GasConfig(ForgeConfigSpec.Builder b) {
        b.push("gas_bim");

        GAS_ENABLED_STACK = b.comment(
                "FR: Taille maximale de stack de la bombe à gaz active.",
                "EN: Maximum stack size for the active gas bomb."
        ).defineInRange("enabled_stack", 16, 1, 64);

        GAS_DISABLED_STACK = b.comment(
                "FR: Taille maximale de stack de la coque vide (bombe à gaz désactivée).",
                "EN: Maximum stack size for the empty gas bomb shell."
        ).defineInRange("disabled_stack", 16, 1, 64);

        VITESSE_PROJECTILE = b.comment(
                "FR: Multiplicateur de vitesse du projectile de gaz.",
                "EN: Speed multiplier for the gas projectile."
        ).defineInRange("projectile_speed_mult", 0.85D, 0.1D, 10.0D);

        POIDS_PROJECTILE = b.comment(
                "FR: Coefficient de poids (influence la gravité et le rebond).",
                "EN: Weight factor (affects gravity and bounce)."
        ).defineInRange("projectile_weight", 1.0D, 0.1D, 10.0D);

        GAS_IMPACT_HEARTS = b.comment(
                "FR: Dégâts en CŒURS infligés à l’impact du projectile.",
                "EN: Damage in HEARTS dealt on impact with an entity."
        ).defineInRange("impact_hearts", 1.0D, 0.0D, 50.0D);

        GAS_DMG_BYPASS_ARMOR = b.comment(
                "FR: Si vrai, les dégâts de gaz ignorent l’armure.",
                "EN: If true, gas damage bypasses armor."
        ).define("bypass_armor", true);

        GAS_EXPLODE_AFTER_TICKS = b.comment(
                "FR: Délai avant création du nuage de gaz (ticks).",
                "EN: Delay before the gas cloud spawns (ticks)."
        ).defineInRange("explode_after_ticks", 60, 5, 20 * 60);

        GAS_PICKUP_AFTER_TICKS = b.comment(
                "FR: Délai avant que la bombe puisse être ramassée (ticks).",
                "EN: Delay before the bomb can be picked up (ticks)."
        ).defineInRange("pickup_after_ticks", 20, 0, 20 * 60);

        RADIUS = b.comment(
                "FR: Rayon maximal du nuage de gaz (en blocs).",
                "EN: Maximum radius of the gas cloud (in blocks)."
        ).defineInRange("radius", 45, 5, 128);

        GAS_DAMAGE_HEARTH = b.comment(
                "FR: Dégâts du gaz par seconde (en cœurs) quand il ne pleut pas.",
                "EN: Gas damage per second (in hearts) when not raining."
        ).defineInRange("damage_hearts_per_second", 1.0D, 0.0D, 50.0D);

        GAS_STORM_DAMAGE_HEARTH = b.comment(
                "FR: Dégâts du gaz par seconde (en cœurs) quand il PLEUT.",
                "EN: Gas damage per second (in hearts) during rain or storms."
        ).defineInRange("storm_damage_hearts_per_second", 2.5D, 0.0D, 100.0D);

        GAS_MIN_HEIGHT = b.comment(
                "FR: Hauteur minimale d’effet du nuage (blocs).",
                "EN: Minimum height where the gas starts affecting entities."
        ).defineInRange("min_height", 0.0D, 0.0D, 64.0D);

        GAS_MAX_HEIGHT = b.comment(
                "FR: Hauteur maximale d’effet du nuage (blocs).",
                "EN: Maximum height where the gas affects entities."
        ).defineInRange("max_height", 3.0D, 0.5D, 64.0D);

        RESTITUTION_GROUND = b.comment(
                "FR: Coefficient de rebond au sol.",
                "EN: Ground restitution coefficient."
        ).defineInRange("restitution_ground", 0.35D, 0.0D, 1.0D);

        FRICTION_GROUND = b.comment(
                "FR: Friction horizontale au sol.",
                "EN: Ground horizontal friction."
        ).defineInRange("friction_ground", 0.55D, 0.0D, 1.0D);

        RESTITUTION_WALL = b.comment(
                "FR: Coefficient de rebond contre les murs.",
                "EN: Wall restitution coefficient."
        ).defineInRange("restitution_wall", 0.35D, 0.0D, 1.0D);

        FRICTION_WALL = b.comment(
                "FR: Friction horizontale contre les murs.",
                "EN: Wall horizontal friction."
        ).defineInRange("friction_wall", 0.75D, 0.0D, 1.0D);

        MAX_BOUNCE_UP = b.comment(
                "FR: Rebond vertical maximal.",
                "EN: Maximum vertical bounce velocity."
        ).defineInRange("max_bounce_up", 0.18D, 0.0D, 1.0D);

        STOP_EPS = b.comment(
                "FR: Tolérance d’arrêt (plus haut = s’arrête plus vite).",
                "EN: Stop epsilon (higher = stops earlier)."
        ).defineInRange("stop_eps", 0.04D, 0.0D, 0.5D);

        b.pop();
    }
}
