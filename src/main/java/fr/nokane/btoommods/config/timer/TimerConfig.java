package fr.nokane.btoommods.config.timer;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration du Timer BIM (version FR originale)
 */
public class TimerConfig {

    // ---- Paramètres généraux ----
    public final ForgeConfigSpec.IntValue STACK;
    public final ForgeConfigSpec.IntValue DEFAULT_SECONDS;
    public final ForgeConfigSpec.DoubleValue VITESSE_PROJECTILE;
    public final ForgeConfigSpec.DoubleValue POIDS_PROJECTILE;
    public final ForgeConfigSpec.DoubleValue EXPLOSION_STRENGTH;
    public final ForgeConfigSpec.DoubleValue EXPLOSION_RADIUS;
    public final ForgeConfigSpec.BooleanValue CAUSES_FIRE;
    public final ForgeConfigSpec.BooleanValue BREAK_BLOCKS;
    public final ForgeConfigSpec.DoubleValue BREAK_BLOCK_RADIUS;
    public final ForgeConfigSpec.BooleanValue NO_ITEM_DESTROY;
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

    // ---- Explosion inventaire ----
    public final ForgeConfigSpec.DoubleValue INVENTORY_EXPLOSION_DAMAGE;
    public final ForgeConfigSpec.DoubleValue INVENTORY_EXPLOSION_RADIUS;
    public final ForgeConfigSpec.DoubleValue MAX_DAMAGE_AT_EPICENTER;

    public TimerConfig(ForgeConfigSpec.Builder b) {
        b.comment("Configuration du Timer BIM").push("timer");

        STACK = b.defineInRange("stack", 1, 1, 64);
        DEFAULT_SECONDS = b.defineInRange("default_seconds", 10, 1, 300);
        VITESSE_PROJECTILE = b.defineInRange("vitesse_projectile", 1.0D, 0.1D, 10.0D);
        POIDS_PROJECTILE = b.defineInRange("poids_projectile", 0.04D, 0.0D, 1.0D);

        EXPLOSION_STRENGTH = b.defineInRange("explosion_strength", 3.0D, 0.1D, 20.0D);
        EXPLOSION_RADIUS = b.defineInRange("explosion_radius", 4.0D, 0.5D, 64.0D);
        BREAK_BLOCKS = b.define("break_blocks", true);
        BREAK_BLOCK_RADIUS = b.defineInRange("break_block_radius", 3.5D, 0.5D, 32.0D);
        CAUSES_FIRE = b.define("causes_fire", false);
        NO_ITEM_DESTROY = b.define("no_item_destroy", true);
        HUD_RADIUS = b.defineInRange("hud_radius", 16, 0, 128);

        RESTITUTION_GROUND = b.defineInRange("restitution_ground", 0.25D, 0.0D, 1.0D);
        FRICTION_GROUND = b.defineInRange("friction_ground", 0.5D, 0.0D, 1.0D);
        RESTITUTION_WALL = b.defineInRange("restitution_wall", 0.25D, 0.0D, 1.0D);
        FRICTION_WALL = b.defineInRange("friction_wall", 0.65D, 0.0D, 1.0D);
        RESTITUTION_ENTITY = b.defineInRange("restitution_entity", 0.4D, 0.0D, 1.0D);
        MAX_BOUNCE_UP = b.defineInRange("max_bounce_up", 0.12D, 0.0D, 1.0D);
        STOP_EPS = b.defineInRange("stop_eps", 0.05D, 0.0D, 0.5D);

        IMPACT_HEARTS = b.defineInRange("impact_hearts", 1.0D, 0.0D, 50.0D);
        COOLDOWN_TICKS = b.defineInRange("cooldown_ticks", 20, 0, 200);
        EXPLOSION_VISUAL_RADIUS = b.defineInRange("explosion_visual_radius", 6.0D, 0.5D, 128.0D);

        INVENTORY_EXPLOSION_DAMAGE = b.defineInRange("inventory_explosion_damage", 12.0D, 0.0D, 1000.0D);
        INVENTORY_EXPLOSION_RADIUS = b.defineInRange("inventory_explosion_radius", 3.0D, 0.5D, 64.0D);
        MAX_DAMAGE_AT_EPICENTER = b.defineInRange("max_damage_at_epicenter", 20.0D, 0.0D, 1000.0D);

        b.pop();
    }
}
