package fr.nokane.btoommods.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.entity.item.GasBimEntity;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.world.World;

import static fr.nokane.btoommods.sound.ModSounds.PI_ITEM;

public class GasBimItem extends Item {

    public GasBimItem(Properties props) { super(props); }

    @Override
    public UseAction getUseAnimation(ItemStack s) { return UseAction.BOW; }

    @Override
    public int getUseDuration(ItemStack s) { return 72000; }

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
            GasBimEntity proj = ModEntities.GAS_BIM.get().create(level);
            if (proj != null) {
                proj.setOwner(player);
                proj.setItem(stack.copy());
                proj.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());

                // ⚙️ vitesse identique au Timer BIM
                float velocity = (float) (1.7f * power * ModConfigs.TIMER.VITESSE_PROJECTILE.get());
                proj.shootFromRotation(player, player.xRot, player.yRot, 0.0F, velocity, 0.9F);

                level.addFreshEntity(proj);
            }
        }

        SoundUtils.playWorldSound(level, player.getX(), player.getY(), player.getZ(),
                PI_ITEM.get(), 1.3F, 1.0F);

        int cooldown = ModConfigs.GAS.COOLDOWN_TICKS.get();
        player.getCooldowns().addCooldown(this, cooldown);

        if (!level.isClientSide)
            player.getPersistentData().putInt("gas_bim_cooldown", cooldown);

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.abilities.instabuild) stack.shrink(1);
    }

}
