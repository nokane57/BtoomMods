package fr.nokane.btoommods.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.entity.item.GasDisabledBimEntity;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.world.World;

import static fr.nokane.btoommods.sound.ModSounds.PI_ITEM;

/**
 * 💨 GasDisabledBimItem
 * - Tire un projectile de type GasDisabledBimEntity
 * - Même physique que le Timer BIM
 * - Pas de gaz ni explosion de gaz
 */
public class GasDisabledBimItem extends Item {

    public GasDisabledBimItem(Properties props) {
        super(props);
    }

    @Override
    public UseAction getUseAnimation(ItemStack s) {
        return UseAction.BOW;
    }

    @Override
    public int getUseDuration(ItemStack s) {
        return 72000;
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

        int used = this.getUseDuration(stack) - timeLeft;
        float power = Math.min(1.0f, used / 20.0f);
        if (power < 0.1f) return;

        if (!level.isClientSide) {
            GasDisabledBimEntity proj = ModEntities.GAS_BIM_DISABLED.get().create(level);
            if (proj != null) {
                proj.setOwner(player);
                proj.setItem(stack.copy());
                proj.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());

                float velocity = (float) (1.7f * power * ModConfigs.GAS_DISABLED.VITESSE_PROJECTILE.get());
                proj.shootFromRotation(player, player.xRot, player.yRot, 0.0F, velocity, 0.9F);

                level.addFreshEntity(proj);
            }
        }

        SoundUtils.playWorldSound(level, player.getX(), player.getY(), player.getZ(),
                PI_ITEM.get(), 1.2F, 1.0F);

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.abilities.instabuild) stack.shrink(1);
    }
}
