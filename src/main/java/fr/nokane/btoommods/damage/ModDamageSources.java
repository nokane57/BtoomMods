// ModDamageSources.java
package fr.nokane.btoommods.damage;

import net.minecraft.util.DamageSource;

public final class ModDamageSources {
    // Pas de registry ici : simple constante.
    public static final DamageSource BLAZING_BURN =
            (new DamageSource("blazing_burn")).setIsFire(); // feu → respecte fire resistance, etc.

    private ModDamageSources() {}
}
