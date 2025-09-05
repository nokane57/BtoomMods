// fr/nokane/btoommods/effect/custom/BlazingBurnEffect.java
package fr.nokane.btoommods.effect.custom;

import fr.nokane.btoommods.config.ModConfigs;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraft.util.DamageSource;

public class BlazingBurnEffect extends Effect {
    private static final DamageSource BLAZING_BURN = (new DamageSource("blazing_burn")).setIsFire();
    public BlazingBurnEffect(EffectType type, int color) { super(type, color); }
    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return true; } // chaque tick
    @Override public void applyEffectTick(LivingEntity e, int amp) {
        float hearts = ModConfigs.COMMON.BLAZING_BURN_DMG_HEARTS.get().floatValue();
        e.hurt(BLAZING_BURN, hearts * 2.0F);
    }
}
