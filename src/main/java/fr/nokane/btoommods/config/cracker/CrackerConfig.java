package fr.nokane.btoommods.config.cracker;

import net.minecraftforge.common.ForgeConfigSpec;

public class CrackerConfig {

    public final ForgeConfigSpec.IntValue STACK;
    public final ForgeConfigSpec.DoubleValue VITESSE_PROJECTILE;
    public final ForgeConfigSpec.DoubleValue POIDS_PROJECTILE;

    // --- Explosion et dégâts ---
    public final ForgeConfigSpec.DoubleValue EXPLOSION_STRENGTH;
    public final ForgeConfigSpec.DoubleValue RADIUS;
    public final ForgeConfigSpec.DoubleValue VISUAL_RADIUS;
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

        STACK = b.comment("FR: Taille maximale de stack de l'item Cracker BIM.",
                        "EN: Maximum stack size for Cracker BIM item.")
                .defineInRange("stack", 16, 1, 64);

        VITESSE_PROJECTILE = b.comment("FR: Multiplicateur de vitesse du projectile Cracker.",
                        "EN: Projectile speed multiplier.")
                .defineInRange("projectile_speed_mult", 1.25D, 0.1D, 10.0D);

        POIDS_PROJECTILE = b.comment("FR: Coefficient de poids (affecte la gravité et le rebond).",
                        "EN: Weight factor (affects gravity).")
                .defineInRange("projectile_weight", 1.0D, 0.1D, 10.0D);

        EXPLOSION_STRENGTH = b.comment("FR: Force physique de l'explosion (TNT = 4.0 par défaut).",
                        "EN: Explosion strength (TNT ≈ 4.0).")
                .defineInRange("explosion_strength", 2.0D, 0.0D, 20.0D); // 🔽 réduit dégâts directs

        RADIUS = b.comment("FR: Rayon d'effet réel (dégâts) du Cracker BIM.",
                        "EN: Real damage radius (in blocks).")
                .defineInRange("radius", 8.0D, 0.0D, 64.0D); // 🔼 zone plus grande

        VISUAL_RADIUS = b.comment("FR: Rayon purement visuel pour les particules d’explosion.",
                        "EN: Visual-only explosion radius for particle effects.")
                .defineInRange("visual_radius", 10.0D, 1.0D, 64.0D);

        BREAK_BLOCK = b.comment("FR: Si vrai, l'explosion détruit les blocs.",
                        "EN: If true, explosion breaks blocks.")
                .define("break_blocks", true);

        DIRECT_HIT_MULTIPLIER = b.comment("FR: Multiplicateur de dégâts pour un impact direct.",
                        "EN: Damage multiplier for direct hits.")
                .defineInRange("direct_hit_multiplier", 1.5D, 0.0D, 10.0D); // 🔽 impact direct réduit

        EPICENTER_DAMAGE = b.comment("FR: Dégâts max (en cœurs) à l’épicentre de l’explosion.",
                        "EN: Max damage (in hearts) at explosion epicenter.")
                .defineInRange("epicenter_damage", 4.0D, 0.0D, 100.0D); // 🔽 dégâts divisés par 2

        BREAK_BLOCK_RADIUS = b.comment("FR: Rayon pour la casse de blocs.",
                        "EN: Block break radius.")
                .defineInRange("break_block_radius", 5.0D, 0.5D, 32.0D);

        NO_ITEM_DESTROY = b.comment("FR: Empêche la destruction des items drop.",
                        "EN: Prevents item destruction.")
                .define("no_item_destroy", true);

        LIFETIME_TICKS = b.comment("FR: Durée de vie du projectile avant disparition (ticks).",
                        "EN: Projectile lifetime (ticks).")
                .defineInRange("lifetime_ticks", 200, 20, 20 * 60 * 10);

        COOLDOWN_TICKS = b.comment("FR: Délai entre deux tirs (20 = 1s).",
                        "EN: Cooldown between two throws (20 = 1s).")
                .defineInRange("cooldown_ticks", 20, 0, 200);

        b.pop();
    }
}
