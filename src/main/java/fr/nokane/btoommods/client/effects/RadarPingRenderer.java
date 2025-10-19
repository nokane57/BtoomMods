package fr.nokane.btoommods.client.effects;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class RadarPingRenderer {

    private static final List<Ping> PINGS = new LinkedList<>();

    public static void addPing(double x, double y, double z, boolean isPlayer) {
        PINGS.add(new Ping(x, y, z, isPlayer, 60)); // visible 3s
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderWorldLastEvent e) {
        if (PINGS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        double camX = mc.gameRenderer.getMainCamera().getPosition().x;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        double camZ = mc.gameRenderer.getMainCamera().getPosition().z;

        for (Iterator<Ping> it = PINGS.iterator(); it.hasNext();) {
            Ping p = it.next();
            if (--p.ticks <= 0) { it.remove(); continue; }

            RenderSystem.pushMatrix();
            RenderSystem.translated(p.x - camX, p.y - camY + 0.2, p.z - camZ);
            float r = p.isPlayer ? 0.2f : 0.0f;
            float g = p.isPlayer ? 1.0f : 0.8f;
            float b = 0.0f;
            float a = p.ticks / 60f;
            RenderSystem.disableTexture();
            RenderSystem.enableBlend();
            RenderSystem.color4f(r, g, b, a);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex3f(0, 0, 0);
            for (int i = 0; i <= 360; i += 10) {
                double rad = Math.toRadians(i);
                float radius = 0.5f;
                GL11.glVertex3f((float) Math.cos(rad) * radius, 0, (float) Math.sin(rad) * radius);
            }
            GL11.glEnd();
            RenderSystem.enableTexture();
            RenderSystem.popMatrix();
        }
    }

    private static class Ping {
        double x, y, z;
        boolean isPlayer;
        int ticks;

        Ping(double x, double y, double z, boolean isPlayer, int ticks) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.isPlayer = isPlayer;
            this.ticks = ticks;
        }
    }
}
