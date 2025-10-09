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

    // ✅ Nouvelle config : vitesse de propagation
    public final ForgeConfigSpec.DoubleValue GAS_SPREAD_SPEED;

    public GasConfig(ForgeConfigSpec.Builder b) {
        b.push("gas_bim");

        GAS_ENABLED_STACK = b.comment("Taille max de stack pour la bombe à gaz activée.")
                .defineInRange("enabled_stack", 16, 1, 64);

        GAS_DISABLED_STACK = b.comment("Taille max de stack pour la coque vide (gaz désactivé).")
                .defineInRange("disabled_stack", 16, 1, 64);

        VITESSE_PROJECTILE = b.comment("Multiplicateur de vitesse initiale du projectile de gaz.")
                .defineInRange("projectile_speed_mult", 0.85D, 0.1D, 10.0D);

        POIDS_PROJECTILE = b.comment("Coefficient de gravité du projectile (poids).")
                .defineInRange("projectile_weight", 1.0D, 0.1D, 10.0D);

        GAS_IMPACT_HEARTS = b.comment("Dégâts à l'impact avant la libération du gaz.")
                .defineInRange("impact_hearts", 1.0D, 0.0D, 50.0D);

        GAS_DMG_BYPASS_ARMOR = b.comment("Si vrai, le gaz ignore l'armure.")
                .define("bypass_armor", true);

        GAS_EXPLODE_AFTER_TICKS = b.comment("Délai avant que le nuage de gaz commence à se former (ticks).")
                .defineInRange("explode_after_ticks", 60, 5, 20 * 60);

        GAS_LIFETIME_TICKS = b.comment("Durée de vie totale du nuage de gaz (ticks).")
                .defineInRange("lifetime_ticks", 200, 20, 20 * 60);

        GAS_PICKUP_AFTER_TICKS = b.comment("Délai avant qu’une bombe posée puisse être ramassée (ticks).")
                .defineInRange("pickup_after_ticks", 20, 0, 20 * 60);

        RADIUS = b.comment("Rayon maximal du nuage de gaz (en blocs).")
                .defineInRange("radius", 45, 5, 128);

        GAS_DAMAGE_HEARTH = b.comment("Dégâts par seconde (en cœurs) du gaz.")
                .defineInRange("damage_hearts_per_second", 1.0D, 0.0D, 50.0D);

        GAS_STORM_DAMAGE_HEARTH = b.comment("Dégâts par seconde sous la pluie.")
                .defineInRange("storm_damage_hearts_per_second", 2.5D, 0.0D, 100.0D);

        RESTITUTION_GROUND = b.defineInRange("restitution_ground", 0.35D, 0.0D, 1.0D);
        FRICTION_GROUND = b.defineInRange("friction_ground", 0.55D, 0.0D, 1.0D);
        RESTITUTION_WALL = b.defineInRange("restitution_wall", 0.35D, 0.0D, 1.0D);
        FRICTION_WALL = b.defineInRange("friction_wall", 0.75D, 0.0D, 1.0D);
        MAX_BOUNCE_UP = b.defineInRange("max_bounce_up", 0.18D, 0.0D, 1.0D);
        STOP_EPS = b.defineInRange("stop_eps", 0.04D, 0.0D, 0.5D);

        GAS_MIN_HEIGHT = b.defineInRange("min_height", 1.0D, 0.0D, 64.0D);
        GAS_MAX_HEIGHT = b.defineInRange("max_height", 3.0D, 0.5D, 64.0D);

        GAS_SPREAD_SPEED = b.comment("Vitesse de propagation du nuage de gaz (1.0 = normal).")
                .defineInRange("spread_speed", 1.0D, 0.1D, 10.0D);

        b.pop();
    }
}
