package fr.nokane.btoommods.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.entity.item.BlazingBimEntity;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.world.World;

import static fr.nokane.btoommods.sound.ModSounds.PI_ITEM;

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

        // 🕒 Empêche de tirer si cooldown actif
        if (player.getCooldowns().isOnCooldown(this)) {
            return ActionResult.fail(stack);
        }

        player.startUsingItem(hand);
        return new ActionResult<>(ActionResultType.CONSUME, stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, World level, LivingEntity living, int timeLeft) {
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

                // ⚙️ Synchronisation vitesse/poids
                double speedMult = ModConfigs.BLAZING.VITESSE_PROJECTILE.get();
                double poids = ModConfigs.BLAZING.POIDS_PROJECTILE.get();

                // 🔢 Logique équilibrée : plus c’est lourd, moins ça va vite
                float adjustedVelocity = (float) (3.0F * power * (speedMult / Math.sqrt(poids)));

                // Tir avec précision (1.0F = petite dispersion)
                proj.shootFromRotation(player, player.xRot, player.yRot, 0.0F, adjustedVelocity, 1.0F);

                level.addFreshEntity(proj);
            }
        }

        // 🔊 Son de tir
        SoundUtils.playWorldSound(level, player.getX(), player.getY(), player.getZ(),
                PI_ITEM.get(), 1.3F, 1.0F);

        // 🕒 Applique le cooldown configurable
        int cooldown = ModConfigs.BLAZING.COOLDOWN_TICKS.get();
        player.getCooldowns().addCooldown(this, cooldown);

        // 🔒 Bloque temporairement l’inventaire
        if (!level.isClientSide)
            player.getPersistentData().putInt("blazing_bim_cooldown", cooldown);

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.abilities.instabuild)
            stack.shrink(1);
    }
}
