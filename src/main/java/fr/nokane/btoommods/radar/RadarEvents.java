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
            final double EPS_DIST = 0.0001; // sensibilité faible (~0.25 cm)
            final double EPS_VEL  = 0.0001;

            // Mouvement entre ce tick et le précédent
            double dx = e.player.getX() - e.player.xOld;
            double dy = e.player.getY() - e.player.yOld;
            double dz = e.player.getZ() - e.player.zOld;

            // Vitesse actuelle
            Vector3d vel = e.player.getDeltaMovement();

            // Distance totale parcourue dans le tick
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            // Si le joueur bouge (dans n'importe quel axe)
            boolean moving =
                    dist > EPS_DIST ||
                            Math.abs(vel.x) > EPS_VEL ||
                            Math.abs(vel.y) > EPS_VEL ||
                            Math.abs(vel.z) > EPS_VEL;

            // Ignorer les sneaks
            if (moving && !e.player.isCrouching()) {
                data.setLastMoveTick(e.player.level.getGameTime());
            }

            // boosters = nombre de radars portés
            int extra = e.player.inventory.items.stream()
                    .filter(s -> !s.isEmpty() && s.getItem() == ModItems.RADAR_ITEM.get())
                    .mapToInt(s -> s.getCount())
                    .sum();
            data.setBoosters(Math.max(0, extra));

            // Implant activé par défaut
            if (!data.hasImplant()) data.setImplant(true);
        });
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        e.getOriginal().getCapability(RadarCapability.CAP).ifPresent(oldCap ->
                e.getPlayer().getCapability(RadarCapability.CAP).ifPresent(newCap -> {
                    if (oldCap instanceof net.minecraftforge.common.util.INBTSerializable) {
                        @SuppressWarnings("unchecked")
                        net.minecraftforge.common.util.INBTSerializable<?> o =
                                (net.minecraftforge.common.util.INBTSerializable<?>) oldCap;
                        @SuppressWarnings("unchecked")
                        net.minecraftforge.common.util.INBTSerializable<net.minecraft.nbt.INBT> n =
                                (net.minecraftforge.common.util.INBTSerializable<net.minecraft.nbt.INBT>) newCap;
                        n.deserializeNBT(o.serializeNBT());
                    }
                })
        );
    }
}
