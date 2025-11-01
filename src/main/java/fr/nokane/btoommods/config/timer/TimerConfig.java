package fr.nokane.btoommods.config.timer;

import net.minecraftforge.common.ForgeConfigSpec;

/** Configuration du Timer BIM */
public class TimerConfig {

    // ---- Paramètres généraux ----
    public final ForgeConfigSpec.IntValue STACK;
    public final ForgeConfigSpec.IntValue DEFAULT_SECONDS;
    public final ForgeConfigSpec.DoubleValue VITESSE_PROJECTILE;
    public final ForgeConfigSpec.DoubleValue POIDS_PROJECTILE;
    public final ForgeConfigSpec.IntValue COOLDOWN_TICKS;
    public final ForgeConfigSpec.IntValue HUD_RADIUS;

    // ---- Physique / Rebond ----
    public final ForgeConfigSpec.DoubleValue RESTITUTION_GROUND;
    public final ForgeConfigSpec.DoubleValue FRICTION_GROUND;
    public final ForgeConfigSpec.DoubleValue RESTITUTION_WALL;
    public final ForgeConfigSpec.DoubleValue FRICTION_WALL;
    public final ForgeConfigSpec.DoubleValue RESTITUTION_ENTITY;
    public final ForgeConfigSpec.DoubleValue MAX_BOUNCE_UP;
    public final ForgeConfigSpec.DoubleValue STOP_EPS;

    // ---- Dégâts à l'impact (projectile qui tape un entity sans exploser) ----
    public final ForgeConfigSpec.DoubleValue IMPACT_HEARTS;

    // ---- INVENTAIRE (explosion depuis l’inventaire) ----
    public final ForgeConfigSpec.DoubleValue INVENTORY_EPICENTER_DAMAGE; // en demi-cœurs (HP)
    public final ForgeConfigSpec.DoubleValue INVENTORY_RADIUS_DAMAGE;    // en demi-cœurs (HP)
    public final ForgeConfigSpec.DoubleValue INVENTORY_RADIUS;           // rayon des dégâts

    // ---- ITEM DROP (timer au sol / droppé) ----
    public final ForgeConfigSpec.DoubleValue ITEM_EPICENTER_DAMAGE;
    public final ForgeConfigSpec.DoubleValue ITEM_RADIUS_DAMAGE;
    public final ForgeConfigSpec.DoubleValue ITEM_RADIUS;

    // ---- PROJECTILE (timer lancé) ----
    public final ForgeConfigSpec.DoubleValue PROJECTILE_EPICENTER_DAMAGE;
    public final ForgeConfigSpec.DoubleValue PROJECTILE_RADIUS_DAMAGE;
    public final ForgeConfigSpec.DoubleValue PROJECTILE_RADIUS;

    // ---- Autres options ----
    public final ForgeConfigSpec.BooleanValue CAUSES_FIRE;
    public final ForgeConfigSpec.BooleanValue BREAK_BLOCKS;
    public final ForgeConfigSpec.BooleanValue NO_ITEM_DESTROY;

    public TimerConfig(ForgeConfigSpec.Builder b) {
        b.comment("Configuration du Timer BIM").push("timer");

        // Généraux
        STACK = b.defineInRange("stack", 1, 1, 64);
        DEFAULT_SECONDS = b.defineInRange("default_seconds", 10, 1, 300);
        VITESSE_PROJECTILE = b.defineInRange("vitesse_projectile", 1.0D, 0.1D, 10.0D);
        POIDS_PROJECTILE = b.defineInRange("poids_projectile", 0.04D, 0.0D, 1.0D);
        COOLDOWN_TICKS = b.defineInRange("cooldown_ticks", 20, 0, 200);
        HUD_RADIUS = b.defineInRange("hud_radius", 16, 0, 128);

        // Physique / rebond
        RESTITUTION_GROUND = b.defineInRange("restitution_ground", 0.25D, 0.0D, 1.0D);
        FRICTION_GROUND = b.defineInRange("friction_ground", 0.5D, 0.0D, 1.0D);
        RESTITUTION_WALL = b.defineInRange("restitution_wall", 0.25D, 0.0D, 1.0D);
        FRICTION_WALL = b.defineInRange("friction_wall", 0.65D, 0.0D, 1.0D);
        RESTITUTION_ENTITY = b.defineInRange("restitution_entity", 0.4D, 0.0D, 1.0D);
        MAX_BOUNCE_UP = b.defineInRange("max_bounce_up", 0.12D, 0.0D, 1.0D);
        STOP_EPS = b.defineInRange("stop_eps", 0.05D, 0.0D, 0.5D);

        IMPACT_HEARTS = b.defineInRange("impact_hearts", 1.0D, 0.0D, 50.0D);

        // Inventaire
        b.comment("Explosion d'un timer dans l'inventaire (dégâts en demi-cœurs / HP).");
        INVENTORY_EPICENTER_DAMAGE = b.defineInRange("inventory_epicenter_damage", 32.0D, 0.0D, 1000.0D);
        INVENTORY_RADIUS_DAMAGE    = b.defineInRange("inventory_radius_damage", 8.0D,  0.0D, 1000.0D);
        INVENTORY_RADIUS           = b.defineInRange("inventory_radius",          3.0D, 0.1D, 64.0D);

        // Item drop
        b.comment("Explosion d'un timer posé ou droppé (dégâts en demi-cœurs / HP).");
        ITEM_EPICENTER_DAMAGE = b.defineInRange("item_epicenter_damage", 28.0D, 0.0D, 1000.0D);
        ITEM_RADIUS_DAMAGE    = b.defineInRange("item_radius_damage",    6.0D,  0.0D, 1000.0D);
        ITEM_RADIUS           = b.defineInRange("item_radius",           3.5D,  0.1D, 64.0D);

        // Projectile
        b.comment("Explosion d'un timer lancé (projectile) (dégâts en demi-cœurs / HP).");
        PROJECTILE_EPICENTER_DAMAGE = b.defineInRange("projectile_epicenter_damage", 24.0D, 0.0D, 1000.0D);
        PROJECTILE_RADIUS_DAMAGE    = b.defineInRange("projectile_radius_damage",    6.0D,  0.0D, 1000.0D);
        PROJECTILE_RADIUS           = b.defineInRange("projectile_radius",           4.0D,  0.1D, 64.0D);

        // Autres
        CAUSES_FIRE    = b.define("causes_fire", false);
        BREAK_BLOCKS   = b.define("break_blocks", true);
        NO_ITEM_DESTROY= b.define("no_item_destroy", true);

        b.pop();
    }
}
