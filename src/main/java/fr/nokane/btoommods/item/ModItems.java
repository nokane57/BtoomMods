package fr.nokane.btoommods.item;

import fr.nokane.btoommods.Btoommods;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Btoommods.MOD_ID);

    public static final RegistryObject<Item> CRACKER_BIM = ITEMS.register("cracker_bim",
            () -> new CrackerBimItem(new Item.Properties()
                    .tab(ItemGroup.TAB_COMBAT) // mets ce qui te plaît
                    .stacksTo(16)));

    public static final RegistryObject<Item> BLAZING_BIM = ITEMS.register("blazing_bim",
            () -> new BlazingBimItem(new Item.Properties().tab(ItemGroup.TAB_COMBAT).stacksTo(16)));

    public static final RegistryObject<Item> TIMER_BIM = ITEMS.register("timer_bim",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_COMBAT).stacksTo(1)));

    public static final RegistryObject<Item> REMOTE_BIM = ITEMS.register("remote_bim",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_COMBAT).stacksTo(1)));

    public static final RegistryObject<Item> GAS_BIM = ITEMS.register("gas_bim",
            () -> new GasBimItem(new Item.Properties().tab(ItemGroup.TAB_COMBAT).stacksTo(16)));

    // “désactivé” (coque) -> ramassable, pas utilisable
    public static final RegistryObject<Item> GAS_BIM_DISABLED = ITEMS.register("gas_bim_disabled",
            () -> new Item(new Item.Properties().tab(ItemGroup.TAB_MISC).stacksTo(16)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
