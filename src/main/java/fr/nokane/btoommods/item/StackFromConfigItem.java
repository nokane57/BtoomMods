// fr/nokane/btoommods/item/StackFromConfigItem.java
package fr.nokane.btoommods.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.function.IntSupplier;

public class StackFromConfigItem extends Item {
    private final IntSupplier supplier;

    public StackFromConfigItem(Properties props, IntSupplier supplier) {
        // On met une valeur haute par défaut; la vraie limite viendra de la méthode surchargée.
        super(props.stacksTo(64));
        this.supplier = supplier;
    }

    // *** IMPORTANT ***
    // Avec Mojmaps 1.16.5, c'est souvent getMaxStackSize(ItemStack).
    // Si ta mappe se plaint, renomme en getItemStackLimit(ItemStack) (ancien MCP).

    @Override
    public int getItemStackLimit(ItemStack stack) {
        int v = supplier.getAsInt();
        if (v < 1) v = 1;
        if (v > 64) v = 64;
        return v;
    }
}
