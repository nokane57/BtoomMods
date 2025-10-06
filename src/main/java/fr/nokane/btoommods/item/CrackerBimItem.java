package fr.nokane.btoommods.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.entity.item.CrackerBimEntity;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.world.World;

public class CrackerBimItem extends Item {

    public CrackerBimItem(Properties props) {
        super(props);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAction getUseAnimation(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        SoundUtils.playWorldSound(world, player.getX(), player.getY(), player.getZ(),
                fr.nokane.btoommods.sound.ModSounds.PI_ITEM.get(), 1.3F, 1.0F);
        player.startUsingItem(hand);
        return ActionResult.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, World world, LivingEntity user, int timeLeft) {
        if (!(user instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) user;

        int used = this.getUseDuration(stack) - timeLeft;
        float power = getPowerForTime(used);
        if (power < 0.1F) return;

        if (!world.isClientSide) {
            CrackerBimEntity proj = ModEntities.CRACKER_BIM.get().create(world);
            if (proj != null) {
                proj.setOwner(player);
                proj.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());

                double vitesse = 3.0F * power * ModConfigs.CRACKER.VITESSE_PROJECTILE.get();
                double poids = ModConfigs.CRACKER.POIDS_PROJECTILE.get();

                // ⚖️ vitesse ajustée selon le poids
                float finalSpeed = (float) (vitesse / Math.max(0.1, poids));

                proj.shootFromRotation(player, player.xRot, player.yRot, 0.0F, finalSpeed, 1.0F);
                world.addFreshEntity(proj);
            }
        }

        if (!player.abilities.instabuild) stack.shrink(1);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    public static float getPowerForTime(int charge) {
        float f = charge / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(f, 1.0F);
    }
}
