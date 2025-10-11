package fr.nokane.btoommods.config.cracker;

import net.minecraftforge.common.ForgeConfigSpec;

public class CrackerConfig {

    public final ForgeConfigSpec.IntValue STACK;
    public final ForgeConfigSpec.DoubleValue VITESSE_PROJECTILE;
    public final ForgeConfigSpec.DoubleValue POIDS_PROJECTILE;

    // --- Explosion et dégâts ---
    public final ForgeConfigSpec.DoubleValue EXPLOSION_STRENGTH;
    public final ForgeConfigSpec.DoubleValue RADIUS;
    public final ForgeConfigSpec.BooleanValue BREAK_BLOCK;
    public final ForgeConfigSpec.DoubleValue DIRECT_HIT_MULTIPLIER;
    public final ForgeConfigSpec.DoubleValue EPICENTER_DAMAGE;

    // 🆕 Options supplémentaires
    public final ForgeConfigSpec.BooleanValue NO_ITEM_DESTROY;
    public final ForgeConfigSpec.DoubleValue BREAK_BLOCK_RADIUS;

    // --- Divers ---
    public final ForgeConfigSpec.IntValue LIFETIME_TICKS;
    public final ForgeConfigSpec.IntValue COOLDOWN_TICKS;

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
        ).defineInRange("radius", 5.0D, 0.0D, 64.0D);

        BREAK_BLOCK = b.comment(
                "FR: Si vrai, l'explosion détruit les blocs.",
                "EN: If true, explosion breaks blocks."
        ).define("break_blocks", true);

        DIRECT_HIT_MULTIPLIER = b.comment(
                "FR: Multiplicateur de dégâts pour un impact direct sur une entité.",
                "EN: Damage multiplier for direct hit on an entity."
        ).defineInRange("direct_hit_multiplier", 2.0D, 0.0D, 10.0D);

        EPICENTER_DAMAGE = b.comment(
                "FR: Dégâts (en cœurs) infligés à l’épicentre de l’explosion du Cracker BIM.",
                "EN: Damage (in hearts) dealt at the explosion epicenter."
        ).defineInRange("epicenter_damage", 8.0D, 0.0D, 100.0D);

        // 🆕 Rayon pour la casse de blocs (indépendant du rayon de dégâts)
        BREAK_BLOCK_RADIUS = b.comment(
                "FR: Rayon maximum pour la casse de blocs (indépendant du rayon de dégâts).",
                "EN: Maximum block destruction radius (independent from damage radius)."
        ).defineInRange("break_block_radius", 3.5D, 0.5D, 32.0D);

        // 🆕 Empêche la destruction des items
        NO_ITEM_DESTROY = b.comment(
                "FR: Si vrai, les explosions du Cracker BIM ne détruisent jamais les items drop.",
                "EN: If true, Cracker BIM explosions never destroy dropped items."
        ).define("no_item_destroy", true);

        LIFETIME_TICKS = b.comment(
                "FR: Durée de vie de l'entité projectile avant disparition (ticks).",
                "EN: Lifetime of the projectile entity before despawn (ticks)."
        ).defineInRange("lifetime_ticks", 200, 20, 20 * 60 * 10);

        COOLDOWN_TICKS = b.comment(
                "FR: Délai (en ticks) entre deux tirs du Cracker BIM (20 = 1 seconde).",
                "EN: Cooldown in ticks between two Cracker BIM throws (20 = 1 second)."
        ).defineInRange("cooldown_ticks", 20, 0, 200);

        b.pop();
    }
}
