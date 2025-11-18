package fr.nokane.btoommods.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.ModEntities;
import fr.nokane.btoommods.entity.item.RemoteBimEntity;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.UUID;

import static fr.nokane.btoommods.sound.ModSounds.PI_ITEM;

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
        float power = BowItem.getPowerForTime(charge);
        if (power < 0.1f) return;

        if (!level.isClientSide) {

            // ✅ UTILISE LE BRACELET ACTIF DU JOUEUR
            ItemStack bracelet = RemoteBraceletManager.getActiveBracelet(player);
            if (bracelet.isEmpty()) {
                // ❌ Pas de bracelet actif = impossible de lancer
                level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundCategory.PLAYERS, 0.5F, 1.0F);
                player.displayClientMessage(
                        new net.minecraft.util.text.StringTextComponent("§c§l[!] §cAucun bracelet Remote actif !"),
                        true
                );
                return;
            }

            // ✅ RÉCUPÈRE L'UUID DU BRACELET ACTIF
            UUID braceletUUID = RemoteBraceletItem.getBraceletUUID(bracelet);
            if (braceletUUID == null) {
                level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundCategory.PLAYERS, 0.5F, 1.0F);
                return;
            }

            // 🔒 Vérifications limites (basées sur le bracelet actif)
            int maxActive = ModConfigs.REMOTE.REMOTE_MAX_ACTIVE.get();
            int maxGlobal = ModConfigs.REMOTE.MAX_REMOTE.get();
            int scan = ModConfigs.REMOTE.REMOTE_SCAN_RADIUS.get();

            // ✅ Compte les Remote BIMs liés au bracelet ACTIF uniquement
            int active = level.getEntitiesOfClass(
                    RemoteBimEntity.class,
                    player.getBoundingBox().inflate(scan),
                    e -> braceletUUID.equals(e.getBraceletUUID())
            ).size();

            if (active >= maxActive) {
                level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK, SoundCategory.PLAYERS, 0.5F, 0.5F);
                player.displayClientMessage(
                        new net.minecraft.util.text.StringTextComponent("§c§l[!] §cLimite de Remote BIMs atteinte pour ce bracelet !"),
                        true
                );
                return;
            }

            long global = level.getEntitiesOfClass(
                    RemoteBimEntity.class,
                    new AxisAlignedBB(player.getX() - 2048, player.getY() - 2048, player.getZ() - 2048,
                            player.getX() + 2048, player.getY() + 2048, player.getZ() + 2048),
                    e -> true
            ).size();

            if (global >= maxGlobal) {
                level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK, SoundCategory.PLAYERS, 0.4F, 0.4F);
                return;
            }

            // 🧨 Création du projectile
            RemoteBimEntity proj = ModEntities.REMOTE_BIM.get().create(level);
            if (proj != null) {
                proj.setOwner(player);
                proj.setItem(stack.copy());
                proj.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());

                // ✅ ASSOCIE LE REMOTE BIM AU BRACELET ACTIF
                proj.setBraceletUUID(braceletUUID);

                // ⚖️ Vitesse basée sur le poids
                double poids = Math.max(0.1, ModConfigs.REMOTE.POIDS_PROJECTILE.get());
                double vitesseBase = 1.7D * power * ModConfigs.REMOTE.VITESSE_PROJECTILE.get();
                float vitesseFinale = (float) (vitesseBase / Math.sqrt(poids));

                // 🹠Tir sans arc excessif
                proj.shootFromRotation(player, player.xRot, player.yRot, 0.0F, vitesseFinale, 0.8F);

                // ✅ Slot automatique
                proj.assignSlotAuto();

                // ✅ Ajout à la map
                level.addFreshEntity(proj);
            }
        }

        // 🔊 Son de tir
        SoundUtils.playWorldSound(level, player.getX(), player.getY(), player.getZ(),
                PI_ITEM.get(), 1.3F, 1.0F);

        int cooldown = ModConfigs.REMOTE.COOLDOWN_TICKS.get();
        player.getCooldowns().addCooldown(this, cooldown);

        if (!level.isClientSide)
            player.getPersistentData().putInt("remote_cooldown", cooldown);

        // Stat + retrait d'item
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.abilities.instabuild) stack.shrink(1);
    }
}