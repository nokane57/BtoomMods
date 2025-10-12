package fr.nokane.btoommods.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

/**
 * 🛰️ Télécommande (Remote Bracelet)
 * - Ouvre le GUI du bracelet côté client
 * - Serveur-safe (aucune référence client directe)
 */
public class RemoteBraceletItem extends Item {

    public RemoteBraceletItem(Properties props) {
        super(props);
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // ✅ Côté client : ouvrir le GUI via DistExecutor
        if (world.isClientSide) {
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientActions::openBraceletGui);
        }

        return ActionResult.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent("§7Un bracelet de télécommande haute technologie."));
        tooltip.add(new StringTextComponent("§8Clique droit pour ouvrir le module de contrôle."));
        super.appendHoverText(stack, world, tooltip, flag);
    }

    /**
     * Classe interne statique utilisée pour appeler le code client sans crash serveur.
     */
    @OnlyIn(Dist.CLIENT)
    private static class ClientActions {
        public static void openBraceletGui() {
            // On importe ici seulement côté client
            fr.nokane.btoommods.client.ClientOnly.openBraceletScreen();
        }
    }
}
