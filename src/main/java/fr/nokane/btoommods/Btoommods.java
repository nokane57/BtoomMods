package fr.nokane.btoommods;


import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.effect.ModEffects;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.item.ModItems;
import fr.nokane.btoommods.net.Net;
import fr.nokane.btoommods.particle.ModParticles;
import fr.nokane.btoommods.radar.RadarCapability;
import fr.nokane.btoommods.sound.ModSounds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Btoommods.MOD_ID)
public class Btoommods {

    public static final String MOD_ID = "btoommods";
    private static final Logger LOGGER = LogManager.getLogger();

    public Btoommods() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModConfigs.register();

        modEventBus.addListener(this::setup);

        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModParticles.register(modEventBus);
        ModEffects.register(modEventBus);
        ModSounds.register(modEventBus);
    }


    private void setup(final FMLCommonSetupEvent event) {
        RadarCapability.registerManually();
        event.enqueueWork(Net::registerMessages);
        LOGGER.info("[BTOOM MOD] Common setup loaded.");
    }

}
