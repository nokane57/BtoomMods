package fr.nokane.btoommods.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.entity.item.GasBimEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.world.World;

public class GasBimItem extends Item {
    public GasBimItem(Properties props) { super(props); }

    @Override public UseAction getUseAnimation(ItemStack s) { return UseAction.BOW; }
    @Override public int getUseDuration(ItemStack s) { return 72000; }

    @Override
    public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return new ActionResult<>(ActionResultType.CONSUME, stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, World level, net.minecraft.entity.LivingEntity living, int timeLeft) {
        if (!(living instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) living;

        int charge = this.getUseDuration(stack) - timeLeft;
        float power = BowItem.getPowerForTime(charge);
        if (power < 0.1f) return;

        if (!level.isClientSide) {
            GasBimEntity proj = ModEntities.GAS_BIM.get().create(level);
            if (proj != null) {
                proj.setOwner(player);
                proj.setItem(stack.copy());
                proj.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
                float speedMult = ModConfigs.GAS.VITESSE_PROJECTILE.get().floatValue();
                proj.shootFromRotation(player, player.xRot, player.yRot, 0.0F, 3.0F * power * speedMult, 1.2F);
                level.addFreshEntity(proj);
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.abilities.instabuild) stack.shrink(1);
    }
}
