package fr.nokane.btoommods.config;

import fr.nokane.btoommods.Btoommods;
import fr.nokane.btoommods.config.blazing.BlazingConfig;
import fr.nokane.btoommods.config.cracker.CrackerConfig;
import fr.nokane.btoommods.config.gas.GasConfig;
import fr.nokane.btoommods.config.remote.RemoteConfig;
import fr.nokane.btoommods.config.radar.RadarConfig;
import fr.nokane.btoommods.config.timer.TimerConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class ModConfigs {

    // ---- CONFIGS PAR ITEM ----
    public static ForgeConfigSpec TIMER_SPEC, GAS_SPEC, CRACKER_SPEC, BLAZING_SPEC, REMOTE_SPEC, RADAR_SPEC;
    public static TimerConfig TIMER;
    public static GasConfig GAS;
    public static CrackerConfig CRACKER;
    public static BlazingConfig BLAZING;
    public static RemoteConfig REMOTE;
    public static RadarConfig RADAR;

    public static void register() {
        ModLoadingContext ctx = ModLoadingContext.get();

        // === TIMER BIM ===
        ForgeConfigSpec.Builder timerBuilder = new ForgeConfigSpec.Builder();
        TIMER = new TimerConfig(timerBuilder);
        TIMER_SPEC = timerBuilder.build();
        ctx.registerConfig(ModConfig.Type.COMMON, TIMER_SPEC, Btoommods.MOD_ID + "/timer_bim.toml");

        // === GAS BIM ===
        ForgeConfigSpec.Builder gasBuilder = new ForgeConfigSpec.Builder();
        GAS = new GasConfig(gasBuilder);
        GAS_SPEC = gasBuilder.build();
        ctx.registerConfig(ModConfig.Type.COMMON, GAS_SPEC, Btoommods.MOD_ID + "/gas_bim.toml");

        // === CRACKER BIM ===
        ForgeConfigSpec.Builder crackerBuilder = new ForgeConfigSpec.Builder();
        CRACKER = new CrackerConfig(crackerBuilder);
        CRACKER_SPEC = crackerBuilder.build();
        ctx.registerConfig(ModConfig.Type.COMMON, CRACKER_SPEC, Btoommods.MOD_ID + "/cracker_bim.toml");

        // === BLAZING BIM ===
        ForgeConfigSpec.Builder blazingBuilder = new ForgeConfigSpec.Builder();
        BLAZING = new BlazingConfig(blazingBuilder);
        BLAZING_SPEC = blazingBuilder.build();
        ctx.registerConfig(ModConfig.Type.COMMON, BLAZING_SPEC, Btoommods.MOD_ID + "/blazing_bim.toml");

        // === REMOTE BIM ===
        ForgeConfigSpec.Builder remoteBuilder = new ForgeConfigSpec.Builder();
        REMOTE = new RemoteConfig(remoteBuilder);
        REMOTE_SPEC = remoteBuilder.build();
        ctx.registerConfig(ModConfig.Type.COMMON, REMOTE_SPEC, Btoommods.MOD_ID + "/remote_bim.toml");

        // === RADAR ===
        ForgeConfigSpec.Builder radarBuilder = new ForgeConfigSpec.Builder();
        RADAR = new RadarConfig(radarBuilder);
        RADAR_SPEC = radarBuilder.build();
        ctx.registerConfig(ModConfig.Type.COMMON, RADAR_SPEC, Btoommods.MOD_ID + "/radar.toml");
    }
}
