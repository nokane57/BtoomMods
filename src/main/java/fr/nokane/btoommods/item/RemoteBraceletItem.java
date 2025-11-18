package fr.nokane.btoommods.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 🛰️ Télécommande (Remote Bracelet)
 * - Chaque bracelet a un UUID unique
 * - Les Remote BIMs sont liés à cet UUID
 * - Le bracelet peut être échangé entre joueurs
 */
public class RemoteBraceletItem extends Item {

    private static final String NBT_BRACELET_UUID = "BraceletUUID";

    public RemoteBraceletItem(Properties props) {
        super(props);
    }

    /**
     * Récupère ou crée l'UUID unique du bracelet
     */
    public static UUID getBraceletUUID(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof RemoteBraceletItem)) {
            return null;
        }

        CompoundNBT nbt = stack.getOrCreateTag();

        // Si le bracelet n'a pas encore d'UUID, en créer un
        if (!nbt.hasUUID(NBT_BRACELET_UUID)) {
            UUID newUUID = UUID.randomUUID();
            nbt.putUUID(NBT_BRACELET_UUID, newUUID);
        }

        return nbt.getUUID(NBT_BRACELET_UUID);
    }

    /**
     * Définit manuellement l'UUID du bracelet (utile pour la fabrication ou les commandes)
     */
    public static void setBraceletUUID(ItemStack stack, UUID uuid) {
        if (stack.isEmpty() || !(stack.getItem() instanceof RemoteBraceletItem)) {
            return;
        }
        stack.getOrCreateTag().putUUID(NBT_BRACELET_UUID, uuid);
    }

    /**
     * Trouve le bracelet Remote dans l'inventaire d'un joueur
     */
    public static ItemStack findBraceletInInventory(PlayerEntity player) {
        // Vérifie main secondaire en premier (plus rapide pour l'usage)
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof RemoteBraceletItem) {
            return offhand;
        }

        // Vérifie l'inventaire principal
        for (ItemStack stack : player.inventory.items) {
            if (stack.getItem() instanceof RemoteBraceletItem) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // ⌛ Si tenu dans la main secondaire : ne rien faire
        if (hand == Hand.OFF_HAND) {
            return ActionResult.success(stack);
        }

        // ✅ Assure que le bracelet a un UUID
        UUID braceletUUID = getBraceletUUID(stack);

        // ✅ DÉFINIT CE BRACELET COMME ACTIF
        if (!world.isClientSide && braceletUUID != null) {
            RemoteBraceletManager.setActiveBracelet(player, braceletUUID);

            String shortUUID = braceletUUID.toString().substring(0, 8);
            player.displayClientMessage(
                    new StringTextComponent("§6§l[BRACELET] §eActivé: §b" + shortUUID + "..."),
                    true // Actionbar
            );
        }

        // ✅ Côté client : ouvrir le GUI via DistExecutor
        if (world.isClientSide) {
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientActions::openBraceletGui);
        }

        return ActionResult.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        UUID braceletUUID = getBraceletUUID(stack);

        tooltip.add(new StringTextComponent("§7Un bracelet de télécommande haute technologie."));
        tooltip.add(new StringTextComponent("§8Clic droit (main principale) pour activer et ouvrir."));
        tooltip.add(new StringTextComponent("§8Tenir en main secondaire pour déclencher avec 1–8."));

        // Affiche l'ID du bracelet en mode debug (F3+H)
        if (flag.isAdvanced() && braceletUUID != null) {
            tooltip.add(new StringTextComponent("§8ID: " + braceletUUID.toString().substring(0, 8) + "..."));
        }

        super.appendHoverText(stack, world, tooltip, flag);
    }

    /**
     * Les bracelets ne stackent pas car chacun a un UUID unique
     */
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    /**
     * Classe interne statique utilisée pour appeler le code client sans crash serveur.
     */
    @OnlyIn(Dist.CLIENT)
    private static class ClientActions {
        public static void openBraceletGui() {
            fr.nokane.btoommods.client.ClientOnly.openBraceletScreen();
        }
    }
}