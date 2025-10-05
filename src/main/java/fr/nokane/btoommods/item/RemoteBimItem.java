package fr.nokane.btoommods.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.entity.item.RemoteBimEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

public class RemoteBimItem extends Item {

    public RemoteBimItem(Properties props) {
        super(props);
    }

    @Override
    public UseAction getUseAnimation(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
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

        int charge = this.getUseDuration(stack) - timeLeft;
        float f = BowItem.getPowerForTime(charge);
        if (f < 0.1f) return;

        if (!level.isClientSide) {
            // ✅ Lecture depuis la RemoteConfig
            int maxActive = ModConfigs.REMOTE.REMOTE_MAX_ACTIVE.get();
            int maxGlobal = ModConfigs.REMOTE.MAX_REMOTE.get();
            int scan = ModConfigs.REMOTE.REMOTE_SCAN_RADIUS.get();

            // ✅ Comptage des bombes actives du joueur
            int active = level.getEntitiesOfClass(
                    RemoteBimEntity.class,
                    player.getBoundingBox().inflate(scan),
                    e -> e.getOwner() != null && e.getOwner().getUUID().equals(player.getUUID())
            ).size();

            if (active >= maxActive) {
                level.playSound(null, player.blockPosition(),
                        SoundEvents.UI_BUTTON_CLICK, SoundCategory.PLAYERS,
                        0.5F, 0.5F);
                return;
            }

            // ✅ Vérifie le total global (corrigé)
            AxisAlignedBB area = player.getBoundingBox().inflate(2048); // grande zone
            long global = level.getEntitiesOfClass(RemoteBimEntity.class, area, e -> true).size();

            if (global >= maxGlobal) {
                level.playSound(null, player.blockPosition(),
                        SoundEvents.UI_BUTTON_CLICK, SoundCategory.PLAYERS,
                        0.4F, 0.4F);
                return;
            }

            // ✅ Création et lancement du projectile
            RemoteBimEntity proj = ModEntities.REMOTE_BIM.get().create(level);
            if (proj != null) {
                proj.setOwner(player);
                proj.setItem(stack.copy());
                proj.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());

                double mult = ModConfigs.REMOTE.VITESSE_PROJECTILE.get();
                float speed = (float) (mult * f);
                proj.shootFromRotation(player, player.xRot, player.yRot, 0.0F, speed, 0.9F);

                proj.assignSlotAuto();
                level.addFreshEntity(proj);
            }
        }

        // ✅ Son et consommation
        level.playSound(null, player.blockPosition(),
                SoundEvents.CROSSBOW_SHOOT, SoundCategory.PLAYERS,
                0.8F, 1.0F);
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.abilities.instabuild) stack.shrink(1);
    }
}
