package fr.nokane.btoommods.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 🎯 Gestionnaire de bracelet actif par joueur
 * Garde en mémoire quel bracelet est actuellement utilisé
 */
public class RemoteBraceletManager {

    private static final String NBT_ACTIVE_BRACELET = "ActiveBraceletUUID";

    /**
     * Définit le bracelet actuellement actif pour ce joueur
     */
    public static void setActiveBracelet(PlayerEntity player, UUID braceletUUID) {
        if (player == null) return;

        CompoundNBT data = player.getPersistentData();
        if (braceletUUID != null) {
            data.putUUID(NBT_ACTIVE_BRACELET, braceletUUID);
        } else {
            data.remove(NBT_ACTIVE_BRACELET);
        }
    }

    /**
     * Récupère l'UUID du bracelet actif pour ce joueur
     */
    @Nullable
    public static UUID getActiveBraceletUUID(PlayerEntity player) {
        if (player == null) return null;

        CompoundNBT data = player.getPersistentData();
        if (data.hasUUID(NBT_ACTIVE_BRACELET)) {
            return data.getUUID(NBT_ACTIVE_BRACELET);
        }
        return null;
    }

    /**
     * ✅ Trouve le bracelet actif dans l'inventaire du joueur
     * Si le bracelet actif n'existe plus, sélectionne automatiquement le premier bracelet disponible
     */
    @Nullable
    public static ItemStack getActiveBracelet(PlayerEntity player) {
        if (player == null) return ItemStack.EMPTY;

        UUID activeUUID = getActiveBraceletUUID(player);

        // Cherche d'abord le bracelet actif
        if (activeUUID != null) {
            ItemStack found = findBraceletByUUID(player, activeUUID);
            if (!found.isEmpty()) {
                return found;
            }
        }

        // Si le bracelet actif n'existe plus, prend le premier disponible
        ItemStack anyBracelet = RemoteBraceletItem.findBraceletInInventory(player);
        if (!anyBracelet.isEmpty()) {
            UUID newUUID = RemoteBraceletItem.getBraceletUUID(anyBracelet);
            setActiveBracelet(player, newUUID);
            return anyBracelet;
        }

        // Aucun bracelet trouvé
        setActiveBracelet(player, null);
        return ItemStack.EMPTY;
    }

    /**
     * Trouve un bracelet spécifique par son UUID
     */
    @Nullable
    private static ItemStack findBraceletByUUID(PlayerEntity player, UUID braceletUUID) {
        if (braceletUUID == null) return ItemStack.EMPTY;

        // Vérifie main secondaire
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof RemoteBraceletItem) {
            UUID offhandUUID = RemoteBraceletItem.getBraceletUUID(offhand);
            if (braceletUUID.equals(offhandUUID)) {
                return offhand;
            }
        }

        // Vérifie inventaire principal
        for (ItemStack stack : player.inventory.items) {
            if (stack.getItem() instanceof RemoteBraceletItem) {
                UUID stackUUID = RemoteBraceletItem.getBraceletUUID(stack);
                if (braceletUUID.equals(stackUUID)) {
                    return stack;
                }
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * ✅ Change le bracelet actif vers le suivant dans l'inventaire
     * Utile pour le keybind de changement de bracelet
     */
    public static void switchToNextBracelet(PlayerEntity player) {
        if (player == null) return;

        java.util.List<ItemStack> bracelets = new java.util.ArrayList<>();

        // Collecte tous les bracelets
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof RemoteBraceletItem) {
            bracelets.add(offhand);
        }

        for (ItemStack stack : player.inventory.items) {
            if (stack.getItem() instanceof RemoteBraceletItem) {
                bracelets.add(stack);
            }
        }

        if (bracelets.isEmpty()) {
            setActiveBracelet(player, null);
            return;
        }

        if (bracelets.size() == 1) {
            // Un seul bracelet, le définir comme actif
            UUID uuid = RemoteBraceletItem.getBraceletUUID(bracelets.get(0));
            setActiveBracelet(player, uuid);
            return;
        }

        // Plusieurs bracelets : passe au suivant
        UUID currentUUID = getActiveBraceletUUID(player);
        int currentIndex = -1;

        for (int i = 0; i < bracelets.size(); i++) {
            UUID uuid = RemoteBraceletItem.getBraceletUUID(bracelets.get(i));
            if (uuid != null && uuid.equals(currentUUID)) {
                currentIndex = i;
                break;
            }
        }

        // Passe au bracelet suivant (avec wrap-around)
        int nextIndex = (currentIndex + 1) % bracelets.size();
        UUID nextUUID = RemoteBraceletItem.getBraceletUUID(bracelets.get(nextIndex));
        setActiveBracelet(player, nextUUID);

        // Message au joueur
        if (player instanceof ServerPlayerEntity) {
            String shortUUID = nextUUID.toString().substring(0, 8);
            player.displayClientMessage(
                    new net.minecraft.util.text.StringTextComponent("§6§l[BRACELET] §eChangé vers: §b" + shortUUID + "..."),
                    true // Actionbar
            );
        }
    }

    /**
     * Vérifie si le joueur possède un bracelet actif valide
     */
    public static boolean hasActiveBracelet(PlayerEntity player) {
        return !getActiveBracelet(player).isEmpty();
    }

    /**
     * Réinitialise le bracelet actif (utile en cas de mort, etc.)
     */
    public static void clearActiveBracelet(PlayerEntity player) {
        setActiveBracelet(player, null);
    }
}