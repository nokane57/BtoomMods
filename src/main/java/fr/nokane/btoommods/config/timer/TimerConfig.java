package fr.nokane.btoommods.config.timer;

import net.minecraftforge.common.ForgeConfigSpec;

public class TimerConfig {

    // ---- Paramètres généraux ----
    public final ForgeConfigSpec.IntValue STACK;
    public final ForgeConfigSpec.IntValue DEFAULT_SECONDS;
    public final ForgeConfigSpec.DoubleValue VITESSE_PROJECTILE;
    public final ForgeConfigSpec.DoubleValue POIDS_PROJECTILE;
    public final ForgeConfigSpec.DoubleValue EXPLOSION_STRENGTH;
    public final ForgeConfigSpec.BooleanValue CAUSES_FIRE;
    public final ForgeConfigSpec.BooleanValue BREAK_BLOCKS;
    public final ForgeConfigSpec.IntValue HUD_RADIUS;

    // ---- Physique / Rebond ----
    public final ForgeConfigSpec.DoubleValue RESTITUTION_GROUND;
    public final ForgeConfigSpec.DoubleValue FRICTION_GROUND;
    public final ForgeConfigSpec.DoubleValue RESTITUTION_WALL;
    public final ForgeConfigSpec.DoubleValue FRICTION_WALL;
    public final ForgeConfigSpec.DoubleValue RESTITUTION_ENTITY;
    public final ForgeConfigSpec.DoubleValue MAX_BOUNCE_UP;
    public final ForgeConfigSpec.DoubleValue STOP_EPS;

    // ---- Dégâts ----
    public final ForgeConfigSpec.DoubleValue IMPACT_HEARTS;

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

        EXPLOSION_STRENGTH = b.comment("Force d'explosion à la détonation.")
                .defineInRange("explosion_strength", 3.0D, 0.1D, 10.0D);

        CAUSES_FIRE = b.comment("L'explosion déclenche un feu.")
                .define("causes_fire", false);

        BREAK_BLOCKS = b.comment("L'explosion casse les blocs environnants.")
                .define("break_blocks", false);

        HUD_RADIUS = b.comment("Distance maximale à laquelle le HUD du timer est visible (blocs).")
                .defineInRange("hud_radius", 16, 0, 128);

        // --- Physique ---
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

        // --- Dégâts ---
        IMPACT_HEARTS = b.comment("Dégâts (en cœurs) infligés à l'impact du projectile.")
                .defineInRange("impact_hearts", 1.0D, 0.0D, 50.0D);

        b.pop();
    }
}
