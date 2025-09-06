// fr/nokane/btoommods/radar/RadarEvents.java
package fr.nokane.btoommods.radar;

import fr.nokane.btoommods.Btoommods;
import fr.nokane.btoommods.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Btoommods.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RadarEvents {

    @SubscribeEvent
    public static void attachCaps(AttachCapabilitiesEvent<Entity> e) {
        if (e.getObject() instanceof PlayerEntity) {
            e.addCapability(RadarCapability.ID, new RadarCapability.Provider());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.level.isClientSide) return;

        e.player.getCapability(RadarCapability.CAP).ifPresent(data -> {
            Vector3d v = e.player.getDeltaMovement();
            boolean moved = v.lengthSqr() > 1.0E-4
                    || e.player.xOld != e.player.getX()
                    || e.player.zOld != e.player.getZ();
            if (moved) data.setLastMoveTick(e.player.level.getGameTime());

            // boosters = nombre d'items RADAR dans l'inventaire (hors implant)
            int extra = e.player.inventory.items.stream()
                    .filter(s -> !s.isEmpty() && s.getItem() == ModItems.RADAR_ITEM.get())
                    .mapToInt(s -> s.getCount())
                    .sum();
            data.setBoosters(Math.max(0, extra));

            // l’implant est greffé par défaut
            if (!data.hasImplant()) data.setImplant(true);
        });
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        e.getOriginal().getCapability(RadarCapability.CAP).ifPresent(oldCap ->
                e.getPlayer().getCapability(RadarCapability.CAP).ifPresent(newCap -> {
                    // copie simple via NBT
                    if (oldCap instanceof net.minecraftforge.common.util.INBTSerializable) {
                        @SuppressWarnings("unchecked")
                        net.minecraftforge.common.util.INBTSerializable<?> o =
                                (net.minecraftforge.common.util.INBTSerializable<?>) oldCap;
                        @SuppressWarnings("unchecked")
                        net.minecraftforge.common.util.INBTSerializable net =
                                (net.minecraftforge.common.util.INBTSerializable) newCap;
                        net.deserializeNBT(o.serializeNBT());
                    }
                })
        );
    }
}
