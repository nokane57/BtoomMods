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

    // Cracker BIM
    public static final RegistryObject<Item> CRACKER_BIM = ITEMS.register("cracker_bim",
            () -> new CrackerBimItem(new Item.Properties().tab(ItemGroup.TAB_COMBAT)) {
                @Override
                public int getItemStackLimit(ItemStack stack) {
                    return Math.max(1, Math.min(ModConfigs.CRACKER.STACK.get(), 64));
                }
            });

    // Blazing BIM
    public static final RegistryObject<Item> BLAZING_BIM = ITEMS.register("blazing_bim",
            () -> new BlazingBimItem(new Item.Properties().tab(ItemGroup.TAB_COMBAT)) {
                @Override
                public int getItemStackLimit(ItemStack stack) {
                    return Math.max(1, Math.min(ModConfigs.BLAZING.STACK.get(), 64));
                }
            });

    // Remote BIM
    public static final RegistryObject<Item> REMOTE_BIM = ITEMS.register("remote_bim",
            () -> new RemoteBimItem(new Item.Properties().tab(ItemGroup.TAB_COMBAT)) {
                @Override
                public int getItemStackLimit(ItemStack stack) {
                    return Math.max(1, Math.min(ModConfigs.REMOTE.STACK.get(), 64));
                }
            });

    // Gas BIM (⚠️ lit la bonne clé: GAS_ENABLED_STACK)
    public static final RegistryObject<Item> GAS_BIM = ITEMS.register("gas_bim",
            () -> new GasBimItem(new Item.Properties().tab(ItemGroup.TAB_COMBAT)) {
                @Override
                public int getItemStackLimit(ItemStack stack) {
                    return Math.max(1, Math.min(ModConfigs.GAS.GAS_ENABLED_STACK.get(), 64));
                }
            });

    // Gas BIM (coque vide) (⚠️ lit GAS_DISABLED_STACK)
    public static final RegistryObject<Item> GAS_BIM_DISABLED = ITEMS.register("gas_bim_disabled",
            () -> new StackFromConfigItem(
                    new Item.Properties().tab(ItemGroup.TAB_MISC),
                    () -> ModConfigs.GAS.GAS_DISABLED_STACK.get()
            ));

    // Radar implant
    public static final RegistryObject<Item> RADAR_ITEM = ITEMS.register("radar",
            () -> new StackFromConfigItem(new Item.Properties().tab(ItemGroup.TAB_MISC),
                    () -> ModConfigs.RADAR.STACK.get()) {
                @Override
                public boolean onDroppedByPlayer(ItemStack stack, PlayerEntity player) {
                    return false; // indropable
                }
            });

    // Timer BIM
    public static final RegistryObject<Item> TIMER_BIM = ITEMS.register("timer_bim",
            () -> new TimerBimItem(new Item.Properties().tab(ItemGroup.TAB_COMBAT)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
