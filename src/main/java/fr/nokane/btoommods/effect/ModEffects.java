// fr/nokane/btoommods/effect/ModEffects.java
package fr.nokane.btoommods.effect;

import fr.nokane.btoommods.Btoommods;
import fr.nokane.btoommods.effect.custom.BlazingBurnEffect;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEffects {
    public static final DeferredRegister<Effect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.POTIONS, Btoommods.MOD_ID);

    public static final RegistryObject<Effect> BLAZING_BURN = EFFECTS.register("blazing_burn",
            () -> new BlazingBurnEffect(EffectType.HARMFUL, 0xFF6600));

    public static void register(IEventBus bus) { EFFECTS.register(bus); }
}
