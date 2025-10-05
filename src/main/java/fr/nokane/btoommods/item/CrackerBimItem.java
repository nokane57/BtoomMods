package fr.nokane.btoommods.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.entity.item.CrackerBimEntity;
import fr.nokane.btoommods.sound.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.world.World;

public class CrackerBimItem extends Item {

    public CrackerBimItem(Properties props) {
        super(props);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000; // temps max d'utilisation
    }

    @Override
    public UseAction getUseAnimation(ItemStack stack) {
        return UseAction.BOW;
    }

    // Quand on commence à charger
    @Override
    public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 🔊 joue le son "pi" en boucle pendant le chargement
        level.playSound(null, player.blockPosition(),
                ModSounds.PI_ITEM.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);

        player.startUsingItem(hand);
        return ActionResult.consume(stack);
    }

    // Quand on relâche (tire)
    @Override
    public void releaseUsing(ItemStack stack, World level, LivingEntity user, int timeLeft) {
        if (!(user instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) user;

        int used = this.getUseDuration(stack) - timeLeft;
        float power = getPowerForTime(used);
        if (power < 0.1F) return; // pas assez chargé

        if (!level.isClientSide) {
            CrackerBimEntity proj = ModEntities.CRACKER_BIM.get().create(level);
            if (proj != null) {
                proj.setOwner(player);
                proj.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());

                double vitesse = 3.0F * power * ModConfigs.CRACKER.VITESSE_PROJECTILE.get();
                proj.shootFromRotation(player, player.xRot, player.yRot, 0.0F, (float) vitesse, 1.0F);
                level.addFreshEntity(proj);

                // 🔊 démarre le son "pi" loop sur l'entité projectile (jusqu’à explosion)
                level.playSound(null, proj.blockPosition(),
                        ModSounds.PI_ITEM.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
        }

        if (!player.abilities.instabuild) stack.shrink(1);

        player.awardStat(Stats.ITEM_USED.get(this));

        // petit son de tir custom ou vanilla
        level.playSound(null, player.blockPosition(),
                ModSounds.PULL_ITEM.get(), SoundCategory.PLAYERS,
                0.8F, 1.0F + level.random.nextFloat() * 0.2F);
    }

    /** Courbe de charge identique à BowItem */
    public static float getPowerForTime(int charge) {
        float f = charge / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(f, 1.0F);
    }
}
