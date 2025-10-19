package fr.nokane.btoommods.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.net.Net;
import fr.nokane.btoommods.net.RadarScanC2S;
import fr.nokane.btoommods.sound.SoundUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;

/**
 * 📡 Radar implant portable :
 * - Clic droit → déclenche un scan radar
 * - Stack configurable dans les fichiers de config
 * - Non largable
 */
public class RadarItem extends Item {

    public RadarItem() {
        super(new Item.Properties()
                .tab(ItemGroup.TAB_MISC)
                .stacksTo(ModConfigs.RADAR.STACK.get())
                .fireResistant());
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, PlayerEntity player) {
        // L’implant radar ne peut pas être jeté volontairement
        return false;
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (world.isClientSide) {
            return ActionResult.pass(player.getItemInHand(hand));
        }

        // Envoie un packet au serveur pour lancer le scan
        Net.CH.sendToServer(new RadarScanC2S());

        // Joue un son local au joueur
        SoundUtils.playWorldSound(world, player.getX(), player.getY(), player.getZ(),
                fr.nokane.btoommods.sound.ModSounds.SONAR_ITEM.get(),
                SoundUtils.VOL_SONAR, 1.0F);

        return ActionResult.success(player.getItemInHand(hand));
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return Math.max(1, Math.min(ModConfigs.RADAR.STACK.get(), 64));
    }
}
