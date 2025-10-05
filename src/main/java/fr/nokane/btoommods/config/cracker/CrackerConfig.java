package fr.nokane.btoommods.config.cracker;

import net.minecraftforge.common.ForgeConfigSpec;

public class CrackerConfig {

    public final ForgeConfigSpec.IntValue STACK;
    public final ForgeConfigSpec.DoubleValue VITESSE_PROJECTILE;
    public final ForgeConfigSpec.DoubleValue POIDS_PROJECTILE;

    // --- Explosion et dégâts ---
    public final ForgeConfigSpec.DoubleValue EXPLOSION_STRENGTH;
    public final ForgeConfigSpec.DoubleValue RADIUS; // ✅ ajouté
    public final ForgeConfigSpec.BooleanValue BREAK_BLOCK;

    // --- Divers ---
    public final ForgeConfigSpec.IntValue LIFETIME_TICKS;

    public CrackerConfig(ForgeConfigSpec.Builder b) {
        b.push("cracker_bim");

        STACK = b.comment(
                "FR: Taille maximale de stack de l'item Cracker BIM.",
                "EN: Maximum stack size for Cracker BIM item."
        ).defineInRange("stack", 16, 1, 64);

        VITESSE_PROJECTILE = b.comment(
                "FR: Multiplicateur de vitesse du projectile Cracker.",
                "EN: Projectile speed multiplier."
        ).defineInRange("projectile_speed_mult", 1.25D, 0.1D, 10.0D);

        POIDS_PROJECTILE = b.comment(
                "FR: Coefficient de poids (affecte la gravité et le rebond).",
                "EN: Weight factor (affects gravity and bounce)."
        ).defineInRange("projectile_weight", 1.0D, 0.1D, 10.0D);

        EXPLOSION_STRENGTH = b.comment(
                "FR: Force de l'explosion (TNT = 4.0 par défaut).",
                "EN: Explosion strength (TNT ≈ 4.0)."
        ).defineInRange("explosion_strength", 4.0D, 0.0D, 20.0D);

        RADIUS = b.comment(
                "FR: Rayon d'effet de l'explosion du Cracker BIM (en blocs).",
                "EN: Explosion radius (in blocks)."
        ).defineInRange("radius", 5.0D, 0.0D, 64.0D); // ✅ valeur par défaut 5 blocs

        BREAK_BLOCK = b.comment(
                "FR: Si vrai, l'explosion détruit les blocs.",
                "EN: If true, explosion breaks blocks."
        ).define("break_blocks", true);

        LIFETIME_TICKS = b.comment(
                "FR: Durée de vie de l'entité projectile avant disparition (ticks).",
                "EN: Lifetime of the projectile entity before despawn (ticks)."
        ).defineInRange("lifetime_ticks", 200, 20, 20 * 60 * 10);

        b.pop();
    }
}
