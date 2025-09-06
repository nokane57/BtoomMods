// fr/nokane/btoommods/item/GasBimItem.java
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
        float f = BowItem.getPowerForTime(charge); // 0..1
        if (f < 0.1f) return;

        if (!level.isClientSide) {
            GasBimEntity proj = ModEntities.GAS_BIM.get().create(level);
            if (proj != null) {
                proj.setOwner(player);
                proj.setItem(stack.copy());
                proj.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
                float speedMult = ModConfigs.COMMON.GAS_PROJECTILE_SPEED_MULT.get().floatValue(); // < 1.0
                proj.shootFromRotation(player, player.xRot, player.yRot, 0.0F, 3.0F * f * speedMult, 1.2F);
                level.addFreshEntity(proj);
            }
        }

        level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundCategory.PLAYERS, 0.8F, 0.9F);
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.abilities.instabuild) stack.shrink(1);
    }
}
