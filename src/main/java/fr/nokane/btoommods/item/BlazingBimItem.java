package fr.nokane.btoommods.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.entity.item.BlazingBimEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.world.World;

public class BlazingBimItem extends Item {

    public BlazingBimItem(Properties props) {
        super(props);
    }

    @Override
    public UseAction getUseAnimation(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000; // même durée que l'arc
    }

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
        float power = BowItem.getPowerForTime(charge); // 0..1
        if (power < 0.1f) return;

        if (!level.isClientSide) {
            BlazingBimEntity proj = ModEntities.BLAZING_BIM.get().create(level);
            if (proj != null) {
                proj.setOwner(player);
                proj.setItem(stack.copy());
                proj.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());

                float speedMult = ModConfigs.BLAZING.VITESSE_PROJECTILE.get().floatValue();
                proj.shootFromRotation(player, player.xRot, player.yRot, 0.0F, 3.0F * power * speedMult, 1.0F);

                level.addFreshEntity(proj);
            }
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.CROSSBOW_SHOOT, SoundCategory.PLAYERS,
                0.8F, 1.0F + (level.random.nextFloat() * 0.2F));

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.abilities.instabuild) stack.shrink(1);
    }
}
