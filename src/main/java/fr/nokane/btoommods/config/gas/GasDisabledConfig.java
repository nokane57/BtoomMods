package fr.nokane.btoommods.config.gas;

import net.minecraftforge.common.ForgeConfigSpec;

public class GasDisabledConfig {

    public final ForgeConfigSpec.IntValue GAS_DISABLED_STACK;
    public final ForgeConfigSpec.DoubleValue VITESSE_PROJECTILE;
    public final ForgeConfigSpec.DoubleValue POIDS_PROJECTILE;

    public final ForgeConfigSpec.DoubleValue GAS_IMPACT_HEARTS;
    public final ForgeConfigSpec.DoubleValue RESTITUTION_GROUND;
    public final ForgeConfigSpec.DoubleValue FRICTION_GROUND;
    public final ForgeConfigSpec.DoubleValue RESTITUTION_WALL;
    public final ForgeConfigSpec.DoubleValue FRICTION_WALL;
    public final ForgeConfigSpec.DoubleValue MAX_BOUNCE_UP;
    public final ForgeConfigSpec.DoubleValue STOP_EPS;

    public GasDisabledConfig(ForgeConfigSpec.Builder b) {
        b.push("gas_bim_disabled");

        GAS_DISABLED_STACK = b.comment("Taille max de stack pour la coque vide (gaz désactivé).")
                .defineInRange("disabled_stack", 16, 1, 64);

        VITESSE_PROJECTILE = b.comment("Multiplicateur de vitesse initiale du projectile de gaz.")
                .defineInRange("projectile_speed_mult", 0.85D, 0.1D, 10.0D);

        POIDS_PROJECTILE = b.comment("Coefficient de gravité du projectile (poids).")
                .defineInRange("projectile_weight", 1.0D, 0.1D, 10.0D);

        GAS_IMPACT_HEARTS = b.comment("Dégâts à l'impact avant la libération du gaz.")
                .defineInRange("impact_hearts", 1.0D, 0.0D, 50.0D);

        RESTITUTION_GROUND = b.defineInRange("restitution_ground", 0.35D, 0.0D, 1.0D);
        FRICTION_GROUND = b.defineInRange("friction_ground", 0.55D, 0.0D, 1.0D);
        RESTITUTION_WALL = b.defineInRange("restitution_wall", 0.35D, 0.0D, 1.0D);
        FRICTION_WALL = b.defineInRange("friction_wall", 0.75D, 0.0D, 1.0D);
        MAX_BOUNCE_UP = b.defineInRange("max_bounce_up", 0.18D, 0.0D, 1.0D);
        STOP_EPS = b.defineInRange("stop_eps", 0.04D, 0.0D, 0.5D);

        b.pop();
    }
}
