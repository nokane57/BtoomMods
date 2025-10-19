package fr.nokane.btoommods.config.timer;

import net.minecraftforge.common.ForgeConfigSpec;

public class TimerConfig {

    // ---- Paramètres généraux ----
    public final ForgeConfigSpec.IntValue STACK;
    public final ForgeConfigSpec.IntValue DEFAULT_SECONDS;
    public final ForgeConfigSpec.DoubleValue VITESSE_PROJECTILE;
    public final ForgeConfigSpec.DoubleValue POIDS_PROJECTILE;
    public final ForgeConfigSpec.DoubleValue EXPLOSION_STRENGTH;
    public final ForgeConfigSpec.DoubleValue EXPLOSION_RADIUS; // Rayon dégâts explosion classique
    public final ForgeConfigSpec.BooleanValue CAUSES_FIRE;
    public final ForgeConfigSpec.BooleanValue BREAK_BLOCKS;
    public final ForgeConfigSpec.DoubleValue BREAK_BLOCK_RADIUS; // Rayon casse bloc
    public final ForgeConfigSpec.BooleanValue NO_ITEM_DESTROY;   // Protection items
    public final ForgeConfigSpec.IntValue HUD_RADIUS;

    // ---- Physique / Rebond ----
    public final ForgeConfigSpec.DoubleValue RESTITUTION_GROUND;
    public final ForgeConfigSpec.DoubleValue FRICTION_GROUND;
    public final ForgeConfigSpec.DoubleValue RESTITUTION_WALL;
    public final ForgeConfigSpec.DoubleValue FRICTION_WALL;
    public final ForgeConfigSpec.DoubleValue RESTITUTION_ENTITY;
    public final ForgeConfigSpec.DoubleValue MAX_BOUNCE_UP;
    public final ForgeConfigSpec.DoubleValue STOP_EPS;

    // ---- Dégâts et explosion ----
    public final ForgeConfigSpec.DoubleValue IMPACT_HEARTS;
    public final ForgeConfigSpec.IntValue COOLDOWN_TICKS;
    public final ForgeConfigSpec.DoubleValue EXPLOSION_VISUAL_RADIUS;

    // ---- Explosion inventaire & dégâts configurables ----
    public final ForgeConfigSpec.DoubleValue INVENTORY_EXPLOSION_DAMAGE;   // Dégâts si explose dans l’inventaire
    public final ForgeConfigSpec.DoubleValue INVENTORY_EXPLOSION_RADIUS;   // Portée explosion dans inventaire
    public final ForgeConfigSpec.DoubleValue MAX_DAMAGE_AT_EPICENTER;      // Dégât max (épicentre)

    public TimerConfig(ForgeConfigSpec.Builder b) {
        b.comment("Configuration du Timer BIM").push("timer");

        // --- Général ---
        STACK = b.comment("Taille maximale de stack du Timer BIM.")
                .defineInRange("stack", 1, 1, 64);

        DEFAULT_SECONDS = b.comment("Durée par défaut du minuteur (en secondes).")
                .defineInRange("default_seconds", 10, 1, 300);

        VITESSE_PROJECTILE = b.comment("Vitesse de tir du projectile (multiplicateur).")
                .defineInRange("vitesse_projectile", 1.0D, 0.1D, 10.0D);

        POIDS_PROJECTILE = b.comment("Poids du projectile : plus haut = chute plus rapide (gravité).")
                .defineInRange("poids_projectile", 0.04D, 0.0D, 1.0D);

        EXPLOSION_STRENGTH = b.comment("Force visuelle de l'explosion (TNT = 4.0).")
                .defineInRange("explosion_strength", 3.0D, 0.1D, 20.0D);

        EXPLOSION_RADIUS = b.comment("Rayon d'effet de l'explosion (pour les entités).")
                .defineInRange("explosion_radius", 4.0D, 0.5D, 64.0D);

        BREAK_BLOCKS = b.comment("Si vrai, l'explosion casse les blocs environnants.")
                .define("break_blocks", true);

        BREAK_BLOCK_RADIUS = b.comment("Rayon de casse des blocs (indépendant du rayon de dégâts).")
                .defineInRange("break_block_radius", 3.5D, 0.5D, 32.0D);

        CAUSES_FIRE = b.comment("L'explosion déclenche un feu.")
                .define("causes_fire", false);

        NO_ITEM_DESTROY = b.comment("Empêche la destruction des items lors de l'explosion.")
                .define("no_item_destroy", true);

        HUD_RADIUS = b.comment("Distance maximale à laquelle le HUD du timer est visible (en blocs).")
                .defineInRange("hud_radius", 16, 0, 128);

        // --- Physique / rebond ---
        RESTITUTION_GROUND = b.comment("Rebond au sol (0 = pas de rebond, 1 = rebond parfait).")
                .defineInRange("restitution_ground", 0.25D, 0.0D, 1.0D);

        FRICTION_GROUND = b.comment("Friction horizontale au sol.")
                .defineInRange("friction_ground", 0.5D, 0.0D, 1.0D);

        RESTITUTION_WALL = b.comment("Rebond contre les murs.")
                .defineInRange("restitution_wall", 0.25D, 0.0D, 1.0D);

        FRICTION_WALL = b.comment("Friction contre les murs.")
                .defineInRange("friction_wall", 0.65D, 0.0D, 1.0D);

        RESTITUTION_ENTITY = b.comment("Rebond contre les entités vivantes (0 = aucun rebond, 1 = rebond parfait).")
                .defineInRange("restitution_entity", 0.4D, 0.0D, 1.0D);

        MAX_BOUNCE_UP = b.comment("Hauteur maximale du rebond vertical.")
                .defineInRange("max_bounce_up", 0.12D, 0.0D, 1.0D);

        STOP_EPS = b.comment("Tolérance d'arrêt (plus haut = s'arrête plus vite).")
                .defineInRange("stop_eps", 0.05D, 0.0D, 0.5D);

        // --- Dégâts généraux ---
        IMPACT_HEARTS = b.comment("Dégâts (en cœurs) infligés à l'impact du projectile.")
                .defineInRange("impact_hearts", 1.0D, 0.0D, 50.0D);

        COOLDOWN_TICKS = b.comment(
                "FR: Délai (en ticks) entre deux tirs du Timer BIM (20 = 1 seconde).",
                "EN: Cooldown in ticks between two Timer BIM throws (20 = 1 second)."
        ).defineInRange("cooldown_ticks", 20, 0, 200);

        EXPLOSION_VISUAL_RADIUS = b.comment("Rayon visuel de l'explosion (n'affecte pas les dégâts).")
                .defineInRange("explosion_visual_radius", 6.0D, 0.5D, 128.0D);

        // --- Explosion inventaire / auto-dégâts ---
        INVENTORY_EXPLOSION_DAMAGE = b.comment("Dégâts infligés au joueur si la Timer explose dans son inventaire.")
                .defineInRange("inventory_explosion_damage", 12.0D, 0.0D, 1000.0D);

        INVENTORY_EXPLOSION_RADIUS = b.comment("Rayon d'effet de l'explosion si elle explose dans l'inventaire.")
                .defineInRange("inventory_explosion_radius", 3.0D, 0.5D, 64.0D);

        MAX_DAMAGE_AT_EPICENTER = b.comment("Dégâts maximum infligés au centre de l'explosion (épicentre).")
                .defineInRange("max_damage_at_epicenter", 20.0D, 0.0D, 1000.0D);

        b.pop();
    }
}
