package fr.nokane.btoommods.client.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 🌊 Onde radar : effet simple et visible sans texture custom.
 * → Cercle de particules vertes qui s’étend autour du joueur scanneur.
 */
public class RadarWaveEffect {

    private static final List<RadarWave> ACTIVE_WAVES = new ArrayList<>();

    /** 🔹 Lance une onde radar (depuis le joueur) */
    public static void trigger(double x, double y, double z, int maxRadius, int durationTicks) {
        ACTIVE_WAVES.add(new RadarWave(x, y, z, maxRadius, durationTicks));
    }

    /** 🔹 Installe le listener client */
    public static void install() {
        MinecraftForge.EVENT_BUS.register(new RadarWaveEffect());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (ACTIVE_WAVES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Iterator<RadarWave> it = ACTIVE_WAVES.iterator();
        while (it.hasNext()) {
            RadarWave wave = it.next();
            if (!wave.tick(mc)) it.remove();
        }
    }

    // ==========================================================
    // === Classe interne représentant une onde radar ===========
    // ==========================================================
    private static class RadarWave {
        final double x, y, z;
        final int maxRadius;
        final int duration;
        int age = 0;

        RadarWave(double x, double y, double z, int maxRadius, int duration) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.maxRadius = maxRadius;
            this.duration = duration;
        }

        /** 🔁 Mise à jour de l’onde chaque tick client */
        boolean tick(Minecraft mc) {
            if (age++ >= duration) return false;

            double progress = (double) age / duration;
            double radius = progress * maxRadius;

            // Nombre de particules = circonférence du cercle
            int particleCount = (int) (radius * 3.5);

            for (int i = 0; i < particleCount; i++) {
                double angle = (i / (double) particleCount) * 2 * Math.PI;
                double px = x + Math.cos(angle) * radius;
                double pz = z + Math.sin(angle) * radius;
                double py = y + 0.1 + Math.sin(angle * 2 + age * 0.2) * 0.1; // légère oscillation

                // 💚 Particules vertes : plus visibles que HAPPY_VILLAGER
                mc.level.addParticle(ParticleTypes.HAPPY_VILLAGER, px, py, pz, 0, 0.02, 0);
            }

            return true;
        }
    }
}
