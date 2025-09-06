// fr/nokane/btoommods/particle/ModParticles.java
package fr.nokane.btoommods.particle;

import fr.nokane.btoommods.Btoommods;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Btoommods.MOD_ID);

    public static final RegistryObject<BasicParticleType> GAS_CLOUD =
            PARTICLES.register("gas_cloud", () -> new BasicParticleType(false)); // alwaysShow

    public static void register(IEventBus bus){ PARTICLES.register(bus); }
}
