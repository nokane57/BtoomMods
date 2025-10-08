package fr.nokane.btoommods.sound;

import fr.nokane.btoommods.Btoommods;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Btoommods.MOD_ID);

    public static final RegistryObject<SoundEvent> REBOND_ITEM =
            register("drop");

    public static final RegistryObject<SoundEvent> PULL_ITEM =
            register("pull");

    public static final RegistryObject<SoundEvent> PI_ITEM =
            register("pi");

    public static final RegistryObject<SoundEvent> SONAR_ITEM =
            register("sonar");

    public static final RegistryObject<SoundEvent> GAS_ITEM =
            register("gas");

    public static final RegistryObject<SoundEvent> FIRE_ITEM =
            register("fire");

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(Btoommods.MOD_ID, name);
        return SOUND_EVENTS.register(name, () -> new SoundEvent(id));
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
