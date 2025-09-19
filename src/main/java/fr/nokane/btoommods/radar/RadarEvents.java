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
            // --- Seuils anti-bruit (distance/tick et vitesse projetée sur l’axe avant/arrière)
            final double EPS_DIST = 0.0125;         // ~1.25 cm / tick
            final double EPS_VEL  = 0.0125;

            // Vecteur "regard" horizontal (axe avant/arrière)
            Vector3d look = e.player.getLookAngle();
            double lx = look.x, lz = look.z;
            double ll = Math.hypot(lx, lz);
            if (ll < 1.0E-6) {
                // fallback: si jamais le look est (0,0), on ne marque pas comme actif
                lx = 0; lz = 0; ll = 1.0;
            }

            // Déplacement horizontal depuis le tick précédent
            double dx = e.player.getX() - e.player.xOld;
            double dz = e.player.getZ() - e.player.zOld;

            // Projection du déplacement sur l'axe avant/arrière (avance = +, recule = -)
            double alongDist = (dx * lx + dz * lz) / ll;

            // Projection de la vitesse horizontale actuelle sur l'axe avant/arrière
            Vector3d v = e.player.getDeltaMovement();
            double alongVel  = (v.x * lx + v.z * lz) / ll;

            // "Mouvement" = avance OU recule (module de la composante avant/arrière)
            boolean forwardOrBackward =
                    Math.abs(alongDist) > EPS_DIST || Math.abs(alongVel) > EPS_VEL;

            // Ne pas "réveiller" si le joueur est accroupi
            if (forwardOrBackward && !e.player.isCrouching()) {
                data.setLastMoveTick(e.player.level.getGameTime());
            }

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
