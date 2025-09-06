package fr.nokane.btoommods.client;

import fr.nokane.btoommods.particle.ModParticles;
import fr.nokane.btoommods.particle.client.GasCloudParticle;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientParticles {
    @SubscribeEvent
    public static void registerFactories(ParticleFactoryRegisterEvent e) {
        Minecraft.getInstance().particleEngine.register(
                ModParticles.GAS_CLOUD.get(),
                GasCloudParticle.Provider::new   // constructeur qui prend IAnimatedSprite
        );
    }
}

