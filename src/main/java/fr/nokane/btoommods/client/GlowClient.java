// fr/nokane/btoommods/client/GlowClient.java
package fr.nokane.btoommods.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effects;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class GlowClient {
    private static final Map<Integer, Long> GLOW_UNTIL = new HashMap<>();

    private GlowClient(){}

    /** Appelé une fois au setup client pour brancher le tick handler */
    public static void install() {
        MinecraftForge.EVENT_BUS.register(new GlowClient());
    }

    /** Applique/renouvelle le glow pour ids[] pendant ticks */
    public static void apply(int[] ids, int ticks){
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        long until = now + ticks;

        for (int id : ids) {
            Entity e = mc.level.getEntity(id);
            if (e != null) {
                e.setGlowing(true);
                // garder la date d’expiration la plus lointaine si re-scan
                GLOW_UNTIL.merge(id, until, Math::max);
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e){
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long now = mc.level.getGameTime();
        Iterator<Map.Entry<Integer, Long>> it = GLOW_UNTIL.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<Integer, Long> en = it.next();
            if (now >= en.getValue()){
                Entity ent = mc.level.getEntity(en.getKey());
                if (ent != null) {
                    // ne coupe pas si l’entité a un vrai effet potion GLOWING côté serveur
                    if (ent instanceof LivingEntity) {
                        if (!((LivingEntity) ent).hasEffect(Effects.GLOWING)) {
                            ent.setGlowing(false);
                        }
                    } else {
                        ent.setGlowing(false);
                    }
                }
                it.remove();
            }
        }
    }
}
