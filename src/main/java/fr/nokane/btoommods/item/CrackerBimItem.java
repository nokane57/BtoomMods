package fr.nokane.btoommods.item;

import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.entity.item.CrackerBimEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;

public class CrackerBimItem extends Item {
    public CrackerBimItem(Properties props) { super(props); }

    @Override public int getUseDuration(ItemStack stack) { return 72000; }         // comme l’arc
    @Override public UseAction getUseAnimation(ItemStack stack) { return UseAction.BOW; }

    // clic droit : on bande
    @Override
    public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return ActionResult.consume(stack);
    }

    // relâchement : on tire
    @Override
    public void releaseUsing(ItemStack stack, World level, net.minecraft.entity.LivingEntity user, int timeLeft) {
        if (!(user instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) user;

        int used = this.getUseDuration(stack) - timeLeft;              // ticks bandés
        float power = getPowerForTime(used);                           // 0..1 (comme BowItem)
        if (power < 0.1F) return;

        if (!level.isClientSide) {
            CrackerBimEntity proj = ModEntities.CRACKER_BIM.get().create(level);
            if (proj != null) {
                proj.setOwner(player);
                proj.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
                // vitesse comme une flèche : 3.0F * power, précision 1.0F
                proj.shootFromRotation(player, player.xRot, player.yRot, 0.0F, power * 3.0F, 1.0F);
                level.addFreshEntity(proj);
            }
        }

        // Consommation (sauf créatif)
        if (!player.abilities.instabuild) stack.shrink(1);

        player.awardStat(Stats.ITEM_USED.get(this));
        // petit son facultatif
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_SHOOT, SoundCategory.PLAYERS, 0.7F, 1.0F + (level.random.nextFloat() * 0.2F));
    }

    // Copie de la courbe de l'arc vanilla
    public static float getPowerForTime(int charge) {
        float f = charge / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(f, 1.0F);
    }
}