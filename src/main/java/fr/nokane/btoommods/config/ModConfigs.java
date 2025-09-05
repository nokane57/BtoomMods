package fr.nokane.btoommods.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ModConfigs {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        COMMON = new Common(b);
        COMMON_SPEC = b.build();
    }

    public static class Common {
        public final ForgeConfigSpec.DoubleValue radius;
        public final ForgeConfigSpec.DoubleValue epicenterHearts;
        public final ForgeConfigSpec.DoubleValue blockBlast;
        public final ForgeConfigSpec.BooleanValue causesFire;
        public final ForgeConfigSpec.EnumValue<ExplosionMode> explosionMode;
        public final ForgeConfigSpec.BooleanValue manualBreakEnabled;
        public final ForgeConfigSpec.IntValue manualBreakMaxBlocks;
        public final ForgeConfigSpec.IntValue lifetimeTicks;

        public final ForgeConfigSpec.IntValue BLAZING_FIRE_LENGTH;
        public final ForgeConfigSpec.IntValue BLAZING_FIRE_WIDTH;
        public final ForgeConfigSpec.IntValue BLAZING_FIRE_LIFETIME;
        public final ForgeConfigSpec.DoubleValue BLAZING_FIRE_DMG_INSIDE_HEARTS;
        public final ForgeConfigSpec.DoubleValue BLAZING_BURN_DMG_HEARTS;   // (non utilisé si aucun effet custom)
        public final ForgeConfigSpec.IntValue BLAZING_BURN_DURATION;
        public final ForgeConfigSpec.DoubleValue BLAZING_PROJECTILE_SPEED_MULT;

        public Common(ForgeConfigSpec.Builder b) {
            b.push("cracker_bim");

            radius = b.defineInRange("radius", 5.0D, 0.0D, 64.0D);
            epicenterHearts = b.defineInRange("epicenter_hearts", 8.0D, 0.0D, 100.0D);
            blockBlast = b.defineInRange("block_blast", 1.8D, 0.0D, 10.0D);
            causesFire = b.define("causes_fire", false);
            explosionMode = b.defineEnum("explosion_mode", ExplosionMode.BREAK);
            manualBreakEnabled = b.define("manual_break_enabled", true);
            manualBreakMaxBlocks = b.defineInRange("manual_break_max_blocks", 4, 0, 100);
            lifetimeTicks = b.defineInRange("lifetime_ticks", 200, 1, 12000);

            b.pop();

            b.push("blazing_bim");
            BLAZING_FIRE_LENGTH           = b.defineInRange("fire_length", 8, 1, 64);
            BLAZING_FIRE_WIDTH            = b.defineInRange("fire_width", 1, 1, 8);
            BLAZING_FIRE_LIFETIME         = b.defineInRange("fire_lifetime", 6000, 20, 20*60*30);
            BLAZING_FIRE_DMG_INSIDE_HEARTS= b.defineInRange("inside_hearts_per_tick", 2.0, 0.0, 50.0);
            BLAZING_BURN_DMG_HEARTS       = b.defineInRange("burn_hearts_per_tick", 1.0, 0.0, 50.0); // réservé si tu rajoutes un effet/évènement global
            BLAZING_BURN_DURATION         = b.defineInRange("burn_duration", 40, 1, 20*60*10);
            BLAZING_PROJECTILE_SPEED_MULT = b.defineInRange("projectile_speed_mult", 1.25, 0.1, 10.0);
            b.pop();
        }
    }

    public enum ExplosionMode { NONE, BREAK }
}
