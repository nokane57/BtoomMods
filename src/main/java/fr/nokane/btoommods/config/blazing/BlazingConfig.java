package fr.nokane.btoommods.config.blazing;

import net.minecraftforge.common.ForgeConfigSpec;

public class BlazingConfig {

    public final ForgeConfigSpec.IntValue STACK;
    public final ForgeConfigSpec.DoubleValue VITESSE_PROJECTILE;
    public final ForgeConfigSpec.DoubleValue POIDS_PROJECTILE;
    public final ForgeConfigSpec.IntValue BLAZING_FIRE_LENGTH;
    public final ForgeConfigSpec.IntValue BLAZING_FIRE_WIDTH;
    public final ForgeConfigSpec.IntValue BLAZING_FIRE_LIFETIME;
    public final ForgeConfigSpec.DoubleValue BLAZING_FIRE_DMG_INSIDE_HEARTS;
    public final ForgeConfigSpec.DoubleValue BLAZING_BURN_DMG_HEARTS;
    public final ForgeConfigSpec.IntValue BLAZING_BURN_DURATION;
    public final ForgeConfigSpec.BooleanValue FIRE_DAMAGE_THROUGH_BLOCKS;

    public BlazingConfig(ForgeConfigSpec.Builder b) {
        b.push("blazing_bim");

        STACK = b.comment(
                "FR: Taille maximale de stack pour la bombe Blazing.",
                "EN: Maximum stack size for the Blazing bomb."
        ).defineInRange("stack", 16, 1, 64);

        VITESSE_PROJECTILE = b.comment(
                "FR: Multiplicateur de vitesse du projectile Blazing.",
                "EN: Speed multiplier for the Blazing projectile."
        ).defineInRange("projectile_speed_mult", 1.25D, 0.1D, 10.0D);

        POIDS_PROJECTILE = b.comment(
                "FR: Coefficient de poids (affecte la gravité et le rebond).",
                "EN: Weight factor (affects gravity and bounce)."
        ).defineInRange("projectile_weight", 1.0D, 0.1D, 10.0D);

        BLAZING_FIRE_LENGTH = b.comment(
                "FR: Longueur du tapis de feu (en blocs).",
                "EN: Length of the fire strip (in blocks)."
        ).defineInRange("fire_length", 8, 1, 64);

        BLAZING_FIRE_WIDTH = b.comment(
                "FR: Largeur du tapis de feu (nombre de bandes).",
                "EN: Width of the fire strip (number of lanes)."
        ).defineInRange("fire_width", 1, 1, 8);

        BLAZING_FIRE_LIFETIME = b.comment(
                "FR: Durée pendant laquelle le feu persiste (ticks).",
                "EN: Duration the ground fire persists (in ticks)."
        ).defineInRange("fire_lifetime", 6000, 20, 20 * 60 * 30);

        BLAZING_FIRE_DMG_INSIDE_HEARTS = b.comment(
                "FR: Dégâts (en CŒURS) par seconde pour les entités dans la zone de feu.",
                "EN: Damage (in HEARTS) per second for entities inside the fire zone."
        ).defineInRange("fire_inside_hearts_per_second", 1.5D, 0.0D, 50.0D);

        BLAZING_BURN_DMG_HEARTS = b.comment(
                "FR: Dégâts du statut 'brûlure' appliqué aux entités (en cœurs par tick).",
                "EN: Damage of the burning effect applied to entities (in hearts per tick)."
        ).defineInRange("burn_damage_hearts_per_tick", 0.5D, 0.0D, 10.0D);

        BLAZING_BURN_DURATION = b.comment(
                "FR: Durée de l'effet de brûlure appliqué (ticks).",
                "EN: Duration of the applied burning effect (ticks)."
        ).defineInRange("burn_duration_ticks", 40, 10, 20 * 60 * 5);

        FIRE_DAMAGE_THROUGH_BLOCKS = b.comment(
                "FR: Si vrai, le feu inflige des dégâts jusqu’à 5 blocs au-dessus même à travers les blocs solides.",
                "EN: If true, fire damages entities up to 5 blocks above even through solid blocks."
        ).define("fire_damage_through_blocks", true);

        b.pop();
    }
}
