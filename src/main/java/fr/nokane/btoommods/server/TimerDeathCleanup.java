package fr.nokane.btoommods.server;

import fr.nokane.btoommods.Btoommods;
import fr.nokane.btoommods.item.TimerBimItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Btoommods.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TimerDeathCleanup {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntityLiving() instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) event.getEntityLiving();

        event.getDrops().removeIf(drop -> {
            ItemStack stack = drop.getItem();
            if (!(stack.getItem() instanceof TimerBimItem)) return false;

            CompoundNBT tag = stack.getOrCreateTag();

            // ⚡ Supprime seulement celui marqué comme ayant explosé
            if (tag.getBoolean("JustExploded")) {
                Btoommods.LOGGER.info("[TimerCleanup] Suppression du Timer qui vient d’exploser pour {}", player.getName().getString());
                drop.remove();
                return true;
            }

            return false;
        });
    }
}
