package fr.nokane.btoommods.item;

import fr.nokane.btoommods.Btoommods;
import fr.nokane.btoommods.config.ModConfigs;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Btoommods.MOD_ID);

    // Cracker : garde ta classe, mais lis la config dynamiquement
    public static final RegistryObject<Item> CRACKER_BIM = ITEMS.register("cracker_bim",
            () -> new CrackerBimItem(new Item.Properties().tab(ItemGroup.TAB_COMBAT)) {
                @Override
                public int getItemStackLimit(ItemStack stack) {
                    int v = ModConfigs.COMMON.CRACKER_STACK.get();
                    return (v < 1) ? 1 : Math.min(v, 64);
                }
                // Si erreur mappings, renomme la méthode en getItemStackLimit(...)
            });

    public static final RegistryObject<Item> BLAZING_BIM = ITEMS.register("blazing_bim",
            () -> new BlazingBimItem(new Item.Properties().tab(ItemGroup.TAB_COMBAT)) {
                @Override
                public int getItemStackLimit(ItemStack stack) {
                    int v = ModConfigs.COMMON.BLAZING_STACK.get();
                    return (v < 1) ? 1 : Math.min(v, 64);
                }
            });

    public static final RegistryObject<Item> TIMER_BIM = ITEMS.register("timer_bim",
            // pas de classe dédiée → on utilise directement l’item utilitaire
            () -> new StackFromConfigItem(new Item.Properties().tab(ItemGroup.TAB_COMBAT),
                    () -> ModConfigs.COMMON.TIMER_STACK.get()));

    public static final RegistryObject<Item> REMOTE_BIM = ITEMS.register("remote_bim",
            () -> new RemoteBimItem(new Item.Properties().tab(ItemGroup.TAB_COMBAT)) {
                @Override
                public int getItemStackLimit(ItemStack stack) {
                    int v = ModConfigs.COMMON.REMOTE_STACK.get();
                    return (v < 1) ? 1 : Math.min(v, 64);
                }
            });

    public static final RegistryObject<Item> GAS_BIM = ITEMS.register("gas_bim",
            () -> new GasBimItem(new Item.Properties().tab(ItemGroup.TAB_COMBAT)) {
                @Override
                public int getItemStackLimit(ItemStack stack) {
                    int v = ModConfigs.COMMON.GAS_STACK.get();
                    return (v < 1) ? 1 : Math.min(v, 64);
                }
            });

    public static final RegistryObject<Item> GAS_BIM_DISABLED = ITEMS.register("gas_bim_disabled",
            () -> new StackFromConfigItem(new Item.Properties().tab(ItemGroup.TAB_MISC),
                    () -> ModConfigs.COMMON.GAS_DISABLED_STACK.get()));

    public static final RegistryObject<Item> RADAR_ITEM = ITEMS.register("radar",
            () -> new StackFromConfigItem(new Item.Properties().tab(ItemGroup.TAB_MISC),
                    () -> ModConfigs.COMMON.RADAR_STACK.get()) {
                @Override
                public boolean onDroppedByPlayer(ItemStack stack, PlayerEntity player) {
                    return false; // indropable
                }
            });

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
