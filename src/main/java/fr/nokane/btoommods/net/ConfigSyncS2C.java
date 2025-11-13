package fr.nokane.btoommods.net;

import fr.nokane.btoommods.config.ModConfigs;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Packet pour synchroniser TOUTES les configs du serveur vers le client
 * à la connexion. Force le client à utiliser les valeurs du serveur.
 */
public class ConfigSyncS2C {

    private final Map<String, Object> configValues;

    public ConfigSyncS2C() {
        this.configValues = new HashMap<>();
        collectServerValues();
    }

    private ConfigSyncS2C(Map<String, Object> values) {
        this.configValues = values;
    }

    private void collectServerValues() {
        // ==================== TIMER ====================
        configValues.put("timer.stack", ModConfigs.TIMER.STACK.get());
        configValues.put("timer.default_seconds", ModConfigs.TIMER.DEFAULT_SECONDS.get());
        configValues.put("timer.vitesse_projectile", ModConfigs.TIMER.VITESSE_PROJECTILE.get());
        configValues.put("timer.poids_projectile", ModConfigs.TIMER.POIDS_PROJECTILE.get());
        configValues.put("timer.cooldown_ticks", ModConfigs.TIMER.COOLDOWN_TICKS.get());
        configValues.put("timer.hud_radius", ModConfigs.TIMER.HUD_RADIUS.get());
        configValues.put("timer.restitution_ground", ModConfigs.TIMER.RESTITUTION_GROUND.get());
        configValues.put("timer.friction_ground", ModConfigs.TIMER.FRICTION_GROUND.get());
        configValues.put("timer.restitution_wall", ModConfigs.TIMER.RESTITUTION_WALL.get());
        configValues.put("timer.friction_wall", ModConfigs.TIMER.FRICTION_WALL.get());
        configValues.put("timer.restitution_entity", ModConfigs.TIMER.RESTITUTION_ENTITY.get());
        configValues.put("timer.max_bounce_up", ModConfigs.TIMER.MAX_BOUNCE_UP.get());
        configValues.put("timer.stop_eps", ModConfigs.TIMER.STOP_EPS.get());
        configValues.put("timer.impact_hearts", ModConfigs.TIMER.IMPACT_HEARTS.get());
        configValues.put("timer.inventory_epicenter_damage", ModConfigs.TIMER.INVENTORY_EPICENTER_DAMAGE.get());
        configValues.put("timer.inventory_radius_damage", ModConfigs.TIMER.INVENTORY_RADIUS_DAMAGE.get());
        configValues.put("timer.inventory_radius", ModConfigs.TIMER.INVENTORY_RADIUS.get());
        configValues.put("timer.item_epicenter_damage", ModConfigs.TIMER.ITEM_EPICENTER_DAMAGE.get());
        configValues.put("timer.item_radius_damage", ModConfigs.TIMER.ITEM_RADIUS_DAMAGE.get());
        configValues.put("timer.item_radius", ModConfigs.TIMER.ITEM_RADIUS.get());
        configValues.put("timer.projectile_epicenter_damage", ModConfigs.TIMER.PROJECTILE_EPICENTER_DAMAGE.get());
        configValues.put("timer.projectile_radius_damage", ModConfigs.TIMER.PROJECTILE_RADIUS_DAMAGE.get());
        configValues.put("timer.projectile_radius", ModConfigs.TIMER.PROJECTILE_RADIUS.get());
        configValues.put("timer.causes_fire", ModConfigs.TIMER.CAUSES_FIRE.get());
        configValues.put("timer.break_blocks", ModConfigs.TIMER.BREAK_BLOCKS.get());
        configValues.put("timer.no_item_destroy", ModConfigs.TIMER.NO_ITEM_DESTROY.get());

        // ==================== REMOTE ====================
        configValues.put("remote.stack", ModConfigs.REMOTE.STACK.get());
        configValues.put("remote.vitesse_projectile", ModConfigs.REMOTE.VITESSE_PROJECTILE.get());
        configValues.put("remote.poids_projectile", ModConfigs.REMOTE.POIDS_PROJECTILE.get());
        configValues.put("remote.radius", ModConfigs.REMOTE.REMOTE_RADIUS.get());
        configValues.put("remote.causes_fire", ModConfigs.REMOTE.REMOTE_CAUSES_FIRE.get());
        configValues.put("remote.break_blocks", ModConfigs.REMOTE.BREAK_BLOCKS.get());
        configValues.put("remote.max_active", ModConfigs.REMOTE.REMOTE_MAX_ACTIVE.get());
        configValues.put("remote.max_remote_global", ModConfigs.REMOTE.MAX_REMOTE.get());
        configValues.put("remote.scan_radius", ModConfigs.REMOTE.REMOTE_SCAN_RADIUS.get());
        configValues.put("remote.trigger_radius", ModConfigs.REMOTE.REMOTE_TRIGGER_RADIUS.get());
        configValues.put("remote.marker_cooldown_ticks", ModConfigs.REMOTE.REMOTE_MARKER_COOLDOWN_TICKS.get());
        configValues.put("remote.lifetime_ticks", ModConfigs.REMOTE.REMOTE_LIFETIME_TICKS.get());
        configValues.put("remote.no_item_destroy", ModConfigs.REMOTE.REMOTE_NO_ITEM_DESTROY.get());
        configValues.put("remote.break_radius", ModConfigs.REMOTE.REMOTE_BREAK_RADIUS.get());
        configValues.put("remote.cooldown_ticks", ModConfigs.REMOTE.COOLDOWN_TICKS.get());

        // ==================== RADAR ====================
        configValues.put("radar.stack", ModConfigs.RADAR.STACK.get());
        configValues.put("radar.base_radius", ModConfigs.RADAR.RADAR_BASE_RADIUS.get());
        configValues.put("radar.extra_per_item", ModConfigs.RADAR.RADAR_EXTRA_PER_ITEM.get());
        configValues.put("radar.glow_ticks", ModConfigs.RADAR.RADAR_GLOW_TICKS.get());
        configValues.put("radar.active_window", ModConfigs.RADAR.RADAR_ACTIVE_WINDOW.get());
        configValues.put("radar.cooldown_ticks", ModConfigs.RADAR.RADAR_COOLDOWN_TICKS.get());
        configValues.put("radar.wave_duration", ModConfigs.RADAR.RADAR_WAVE_DURATION.get());
        configValues.put("radar.ignore_sneak", ModConfigs.RADAR.RADAR_IGNORE_SNEAK.get());
        configValues.put("radar.require_movement", ModConfigs.RADAR.RADAR_REQUIRE_MOVEMENT.get());
        configValues.put("radar.glow_visible_range", ModConfigs.RADAR.RADAR_GLOW_VISIBLE_RANGE.get());
        configValues.put("radar.message_range", ModConfigs.RADAR.ACTIVATION_MESSAGE.get());

        // ==================== GAS (ENABLED) ====================
        configValues.put("gas.enabled_stack", ModConfigs.GAS.GAS_ENABLED_STACK.get());
        configValues.put("gas.vitesse_projectile", ModConfigs.GAS.VITESSE_PROJECTILE.get());
        configValues.put("gas.poids_projectile", ModConfigs.GAS.POIDS_PROJECTILE.get());
        configValues.put("gas.impact_hearts", ModConfigs.GAS.GAS_IMPACT_HEARTS.get());
        configValues.put("gas.dmg_bypass_armor", ModConfigs.GAS.GAS_DMG_BYPASS_ARMOR.get());
        configValues.put("gas.explode_after_ticks", ModConfigs.GAS.GAS_EXPLODE_AFTER_TICKS.get());
        configValues.put("gas.lifetime_ticks", ModConfigs.GAS.GAS_LIFETIME_TICKS.get());
        configValues.put("gas.pickup_after_ticks", ModConfigs.GAS.GAS_PICKUP_AFTER_TICKS.get());
        configValues.put("gas.radius", ModConfigs.GAS.RADIUS.get());
        configValues.put("gas.damage_hearth", ModConfigs.GAS.GAS_DAMAGE_HEARTH.get());
        configValues.put("gas.storm_damage_hearth", ModConfigs.GAS.GAS_STORM_DAMAGE_HEARTH.get());
        configValues.put("gas.restitution_ground", ModConfigs.GAS.RESTITUTION_GROUND.get());
        configValues.put("gas.friction_ground", ModConfigs.GAS.FRICTION_GROUND.get());
        configValues.put("gas.restitution_wall", ModConfigs.GAS.RESTITUTION_WALL.get());
        configValues.put("gas.friction_wall", ModConfigs.GAS.FRICTION_WALL.get());
        configValues.put("gas.max_bounce_up", ModConfigs.GAS.MAX_BOUNCE_UP.get());
        configValues.put("gas.stop_eps", ModConfigs.GAS.STOP_EPS.get());
        configValues.put("gas.min_height", ModConfigs.GAS.GAS_MIN_HEIGHT.get());
        configValues.put("gas.max_height", ModConfigs.GAS.GAS_MAX_HEIGHT.get());
        configValues.put("gas.spread_speed", ModConfigs.GAS.GAS_SPREAD_SPEED.get());
        configValues.put("gas.cooldown_ticks", ModConfigs.GAS.COOLDOWN_TICKS.get());

        // ==================== GAS DISABLED ====================
        configValues.put("gas_disabled.stack", ModConfigs.GAS_DISABLED.GAS_DISABLED_STACK.get());
        configValues.put("gas_disabled.vitesse_projectile", ModConfigs.GAS_DISABLED.VITESSE_PROJECTILE.get());
        configValues.put("gas_disabled.poids_projectile", ModConfigs.GAS_DISABLED.POIDS_PROJECTILE.get());
        configValues.put("gas_disabled.impact_hearts", ModConfigs.GAS_DISABLED.GAS_IMPACT_HEARTS.get());
        configValues.put("gas_disabled.restitution_ground", ModConfigs.GAS_DISABLED.RESTITUTION_GROUND.get());
        configValues.put("gas_disabled.friction_ground", ModConfigs.GAS_DISABLED.FRICTION_GROUND.get());
        configValues.put("gas_disabled.restitution_wall", ModConfigs.GAS_DISABLED.RESTITUTION_WALL.get());
        configValues.put("gas_disabled.friction_wall", ModConfigs.GAS_DISABLED.FRICTION_WALL.get());
        configValues.put("gas_disabled.max_bounce_up", ModConfigs.GAS_DISABLED.MAX_BOUNCE_UP.get());
        configValues.put("gas_disabled.stop_eps", ModConfigs.GAS_DISABLED.STOP_EPS.get());
        configValues.put("gas_disabled.cooldown_ticks", ModConfigs.GAS_DISABLED.COOLDOWN_TICKS.get());

        // ==================== CRACKER ====================
        configValues.put("cracker.stack", ModConfigs.CRACKER.STACK.get());
        configValues.put("cracker.vitesse_projectile", ModConfigs.CRACKER.VITESSE_PROJECTILE.get());
        configValues.put("cracker.poids_projectile", ModConfigs.CRACKER.POIDS_PROJECTILE.get());
        configValues.put("cracker.explosion_strength", ModConfigs.CRACKER.EXPLOSION_STRENGTH.get());
        configValues.put("cracker.radius", ModConfigs.CRACKER.RADIUS.get());
        configValues.put("cracker.visual_radius", ModConfigs.CRACKER.VISUAL_RADIUS.get());
        configValues.put("cracker.break_block", ModConfigs.CRACKER.BREAK_BLOCK.get());
        configValues.put("cracker.direct_hit_multiplier", ModConfigs.CRACKER.DIRECT_HIT_MULTIPLIER.get());
        configValues.put("cracker.epicenter_damage", ModConfigs.CRACKER.EPICENTER_DAMAGE.get());
        configValues.put("cracker.no_item_destroy", ModConfigs.CRACKER.NO_ITEM_DESTROY.get());
        configValues.put("cracker.break_block_radius", ModConfigs.CRACKER.BREAK_BLOCK_RADIUS.get());
        configValues.put("cracker.lifetime_ticks", ModConfigs.CRACKER.LIFETIME_TICKS.get());
        configValues.put("cracker.cooldown_ticks", ModConfigs.CRACKER.COOLDOWN_TICKS.get());

        // ==================== BLAZING ====================
        configValues.put("blazing.stack", ModConfigs.BLAZING.STACK.get());
        configValues.put("blazing.vitesse_projectile", ModConfigs.BLAZING.VITESSE_PROJECTILE.get());
        configValues.put("blazing.poids_projectile", ModConfigs.BLAZING.POIDS_PROJECTILE.get());
        configValues.put("blazing.fire_length", ModConfigs.BLAZING.BLAZING_FIRE_LENGTH.get());
        configValues.put("blazing.fire_width", ModConfigs.BLAZING.BLAZING_FIRE_WIDTH.get());
        configValues.put("blazing.fire_lifetime", ModConfigs.BLAZING.BLAZING_FIRE_LIFETIME.get());
        configValues.put("blazing.fire_dmg_inside_hearts", ModConfigs.BLAZING.BLAZING_FIRE_DMG_INSIDE_HEARTS.get());
        configValues.put("blazing.burn_dmg_hearts", ModConfigs.BLAZING.BLAZING_BURN_DMG_HEARTS.get());
        configValues.put("blazing.burn_duration", ModConfigs.BLAZING.BLAZING_BURN_DURATION.get());
        configValues.put("blazing.fire_damage_through_blocks", ModConfigs.BLAZING.FIRE_DAMAGE_THROUGH_BLOCKS.get());
        configValues.put("blazing.cooldown_ticks", ModConfigs.BLAZING.COOLDOWN_TICKS.get());
    }

    public static void encode(ConfigSyncS2C msg, PacketBuffer buf) {
        buf.writeInt(msg.configValues.size());
        for (Map.Entry<String, Object> entry : msg.configValues.entrySet()) {
            buf.writeUtf(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof Integer) {
                buf.writeByte(0);
                buf.writeInt((Integer) value);
            } else if (value instanceof Double) {
                buf.writeByte(1);
                buf.writeDouble((Double) value);
            } else if (value instanceof Boolean) {
                buf.writeByte(2);
                buf.writeBoolean((Boolean) value);
            }
        }
    }

    public static ConfigSyncS2C decode(PacketBuffer buf) {
        Map<String, Object> values = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            byte type = buf.readByte();
            switch (type) {
                case 0: values.put(key, buf.readInt()); break;
                case 1: values.put(key, buf.readDouble()); break;
                case 2: values.put(key, buf.readBoolean()); break;
            }
        }
        return new ConfigSyncS2C(values);
    }

    public static void handle(ConfigSyncS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> handleClient(msg));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(ConfigSyncS2C msg) {
        // ==================== TIMER ====================
        applyValue(msg, "timer.stack", ModConfigs.TIMER.STACK);
        applyValue(msg, "timer.default_seconds", ModConfigs.TIMER.DEFAULT_SECONDS);
        applyValue(msg, "timer.vitesse_projectile", ModConfigs.TIMER.VITESSE_PROJECTILE);
        applyValue(msg, "timer.poids_projectile", ModConfigs.TIMER.POIDS_PROJECTILE);
        applyValue(msg, "timer.cooldown_ticks", ModConfigs.TIMER.COOLDOWN_TICKS);
        applyValue(msg, "timer.hud_radius", ModConfigs.TIMER.HUD_RADIUS);
        applyValue(msg, "timer.restitution_ground", ModConfigs.TIMER.RESTITUTION_GROUND);
        applyValue(msg, "timer.friction_ground", ModConfigs.TIMER.FRICTION_GROUND);
        applyValue(msg, "timer.restitution_wall", ModConfigs.TIMER.RESTITUTION_WALL);
        applyValue(msg, "timer.friction_wall", ModConfigs.TIMER.FRICTION_WALL);
        applyValue(msg, "timer.restitution_entity", ModConfigs.TIMER.RESTITUTION_ENTITY);
        applyValue(msg, "timer.max_bounce_up", ModConfigs.TIMER.MAX_BOUNCE_UP);
        applyValue(msg, "timer.stop_eps", ModConfigs.TIMER.STOP_EPS);
        applyValue(msg, "timer.impact_hearts", ModConfigs.TIMER.IMPACT_HEARTS);
        applyValue(msg, "timer.inventory_epicenter_damage", ModConfigs.TIMER.INVENTORY_EPICENTER_DAMAGE);
        applyValue(msg, "timer.inventory_radius_damage", ModConfigs.TIMER.INVENTORY_RADIUS_DAMAGE);
        applyValue(msg, "timer.inventory_radius", ModConfigs.TIMER.INVENTORY_RADIUS);
        applyValue(msg, "timer.item_epicenter_damage", ModConfigs.TIMER.ITEM_EPICENTER_DAMAGE);
        applyValue(msg, "timer.item_radius_damage", ModConfigs.TIMER.ITEM_RADIUS_DAMAGE);
        applyValue(msg, "timer.item_radius", ModConfigs.TIMER.ITEM_RADIUS);
        applyValue(msg, "timer.projectile_epicenter_damage", ModConfigs.TIMER.PROJECTILE_EPICENTER_DAMAGE);
        applyValue(msg, "timer.projectile_radius_damage", ModConfigs.TIMER.PROJECTILE_RADIUS_DAMAGE);
        applyValue(msg, "timer.projectile_radius", ModConfigs.TIMER.PROJECTILE_RADIUS);
        applyValue(msg, "timer.causes_fire", ModConfigs.TIMER.CAUSES_FIRE);
        applyValue(msg, "timer.break_blocks", ModConfigs.TIMER.BREAK_BLOCKS);
        applyValue(msg, "timer.no_item_destroy", ModConfigs.TIMER.NO_ITEM_DESTROY);

        // ==================== REMOTE ====================
        applyValue(msg, "remote.stack", ModConfigs.REMOTE.STACK);
        applyValue(msg, "remote.vitesse_projectile", ModConfigs.REMOTE.VITESSE_PROJECTILE);
        applyValue(msg, "remote.poids_projectile", ModConfigs.REMOTE.POIDS_PROJECTILE);
        applyValue(msg, "remote.radius", ModConfigs.REMOTE.REMOTE_RADIUS);
        applyValue(msg, "remote.causes_fire", ModConfigs.REMOTE.REMOTE_CAUSES_FIRE);
        applyValue(msg, "remote.break_blocks", ModConfigs.REMOTE.BREAK_BLOCKS);
        applyValue(msg, "remote.max_active", ModConfigs.REMOTE.REMOTE_MAX_ACTIVE);
        applyValue(msg, "remote.max_remote_global", ModConfigs.REMOTE.MAX_REMOTE);
        applyValue(msg, "remote.scan_radius", ModConfigs.REMOTE.REMOTE_SCAN_RADIUS);
        applyValue(msg, "remote.trigger_radius", ModConfigs.REMOTE.REMOTE_TRIGGER_RADIUS);
        applyValue(msg, "remote.marker_cooldown_ticks", ModConfigs.REMOTE.REMOTE_MARKER_COOLDOWN_TICKS);
        applyValue(msg, "remote.lifetime_ticks", ModConfigs.REMOTE.REMOTE_LIFETIME_TICKS);
        applyValue(msg, "remote.no_item_destroy", ModConfigs.REMOTE.REMOTE_NO_ITEM_DESTROY);
        applyValue(msg, "remote.break_radius", ModConfigs.REMOTE.REMOTE_BREAK_RADIUS);
        applyValue(msg, "remote.cooldown_ticks", ModConfigs.REMOTE.COOLDOWN_TICKS);

        // ==================== RADAR ====================
        applyValue(msg, "radar.stack", ModConfigs.RADAR.STACK);
        applyValue(msg, "radar.base_radius", ModConfigs.RADAR.RADAR_BASE_RADIUS);
        applyValue(msg, "radar.extra_per_item", ModConfigs.RADAR.RADAR_EXTRA_PER_ITEM);
        applyValue(msg, "radar.glow_ticks", ModConfigs.RADAR.RADAR_GLOW_TICKS);
        applyValue(msg, "radar.active_window", ModConfigs.RADAR.RADAR_ACTIVE_WINDOW);
        applyValue(msg, "radar.cooldown_ticks", ModConfigs.RADAR.RADAR_COOLDOWN_TICKS);
        applyValue(msg, "radar.wave_duration", ModConfigs.RADAR.RADAR_WAVE_DURATION);
        applyValue(msg, "radar.ignore_sneak", ModConfigs.RADAR.RADAR_IGNORE_SNEAK);
        applyValue(msg, "radar.require_movement", ModConfigs.RADAR.RADAR_REQUIRE_MOVEMENT);
        applyValue(msg, "radar.glow_visible_range", ModConfigs.RADAR.RADAR_GLOW_VISIBLE_RANGE);
        applyValue(msg, "radar.message_range", ModConfigs.RADAR.ACTIVATION_MESSAGE);

        // ==================== GAS (ENABLED) ====================
        applyValue(msg, "gas.enabled_stack", ModConfigs.GAS.GAS_ENABLED_STACK);
        applyValue(msg, "gas.vitesse_projectile", ModConfigs.GAS.VITESSE_PROJECTILE);
        applyValue(msg, "gas.poids_projectile", ModConfigs.GAS.POIDS_PROJECTILE);
        applyValue(msg, "gas.impact_hearts", ModConfigs.GAS.GAS_IMPACT_HEARTS);
        applyValue(msg, "gas.dmg_bypass_armor", ModConfigs.GAS.GAS_DMG_BYPASS_ARMOR);
        applyValue(msg, "gas.explode_after_ticks", ModConfigs.GAS.GAS_EXPLODE_AFTER_TICKS);
        applyValue(msg, "gas.lifetime_ticks", ModConfigs.GAS.GAS_LIFETIME_TICKS);
        applyValue(msg, "gas.pickup_after_ticks", ModConfigs.GAS.GAS_PICKUP_AFTER_TICKS);
        applyValue(msg, "gas.radius", ModConfigs.GAS.RADIUS);
        applyValue(msg, "gas.damage_hearth", ModConfigs.GAS.GAS_DAMAGE_HEARTH);
        applyValue(msg, "gas.storm_damage_hearth", ModConfigs.GAS.GAS_STORM_DAMAGE_HEARTH);
        applyValue(msg, "gas.restitution_ground", ModConfigs.GAS.RESTITUTION_GROUND);
        applyValue(msg, "gas.friction_ground", ModConfigs.GAS.FRICTION_GROUND);
        applyValue(msg, "gas.restitution_wall", ModConfigs.GAS.RESTITUTION_WALL);
        applyValue(msg, "gas.friction_wall", ModConfigs.GAS.FRICTION_WALL);
        applyValue(msg, "gas.max_bounce_up", ModConfigs.GAS.MAX_BOUNCE_UP);
        applyValue(msg, "gas.stop_eps", ModConfigs.GAS.STOP_EPS);
        applyValue(msg, "gas.min_height", ModConfigs.GAS.GAS_MIN_HEIGHT);
        applyValue(msg, "gas.max_height", ModConfigs.GAS.GAS_MAX_HEIGHT);
        applyValue(msg, "gas.spread_speed", ModConfigs.GAS.GAS_SPREAD_SPEED);
        applyValue(msg, "gas.cooldown_ticks", ModConfigs.GAS.COOLDOWN_TICKS);

        // ==================== GAS DISABLED ====================
        applyValue(msg, "gas_disabled.stack", ModConfigs.GAS_DISABLED.GAS_DISABLED_STACK);
        applyValue(msg, "gas_disabled.vitesse_projectile", ModConfigs.GAS_DISABLED.VITESSE_PROJECTILE);
        applyValue(msg, "gas_disabled.poids_projectile", ModConfigs.GAS_DISABLED.POIDS_PROJECTILE);
        applyValue(msg, "gas_disabled.impact_hearts", ModConfigs.GAS_DISABLED.GAS_IMPACT_HEARTS);
        applyValue(msg, "gas_disabled.restitution_ground", ModConfigs.GAS_DISABLED.RESTITUTION_GROUND);
        applyValue(msg, "gas_disabled.friction_ground", ModConfigs.GAS_DISABLED.FRICTION_GROUND);
        applyValue(msg, "gas_disabled.restitution_wall", ModConfigs.GAS_DISABLED.RESTITUTION_WALL);
        applyValue(msg, "gas_disabled.friction_wall", ModConfigs.GAS_DISABLED.FRICTION_WALL);
        applyValue(msg, "gas_disabled.max_bounce_up", ModConfigs.GAS_DISABLED.MAX_BOUNCE_UP);
        applyValue(msg, "gas_disabled.stop_eps", ModConfigs.GAS_DISABLED.STOP_EPS);
        applyValue(msg, "gas_disabled.cooldown_ticks", ModConfigs.GAS_DISABLED.COOLDOWN_TICKS);

        // ==================== CRACKER ====================
        applyValue(msg, "cracker.stack", ModConfigs.CRACKER.STACK);
        applyValue(msg, "cracker.vitesse_projectile", ModConfigs.CRACKER.VITESSE_PROJECTILE);
        applyValue(msg, "cracker.poids_projectile", ModConfigs.CRACKER.POIDS_PROJECTILE);
        applyValue(msg, "cracker.explosion_strength", ModConfigs.CRACKER.EXPLOSION_STRENGTH);
        applyValue(msg, "cracker.radius", ModConfigs.CRACKER.RADIUS);
        applyValue(msg, "cracker.visual_radius", ModConfigs.CRACKER.VISUAL_RADIUS);
        applyValue(msg, "cracker.break_block", ModConfigs.CRACKER.BREAK_BLOCK);
        applyValue(msg, "cracker.direct_hit_multiplier", ModConfigs.CRACKER.DIRECT_HIT_MULTIPLIER);
        applyValue(msg, "cracker.epicenter_damage", ModConfigs.CRACKER.EPICENTER_DAMAGE);
        applyValue(msg, "cracker.no_item_destroy", ModConfigs.CRACKER.NO_ITEM_DESTROY);
        applyValue(msg, "cracker.break_block_radius", ModConfigs.CRACKER.BREAK_BLOCK_RADIUS);
        applyValue(msg, "cracker.lifetime_ticks", ModConfigs.CRACKER.LIFETIME_TICKS);
        applyValue(msg, "cracker.cooldown_ticks", ModConfigs.CRACKER.COOLDOWN_TICKS);

        // ==================== BLAZING ====================
        applyValue(msg, "blazing.stack", ModConfigs.BLAZING.STACK);
        applyValue(msg, "blazing.vitesse_projectile", ModConfigs.BLAZING.VITESSE_PROJECTILE);
        applyValue(msg, "blazing.poids_projectile", ModConfigs.BLAZING.POIDS_PROJECTILE);
        applyValue(msg, "blazing.fire_length", ModConfigs.BLAZING.BLAZING_FIRE_LENGTH);
        applyValue(msg, "blazing.fire_width", ModConfigs.BLAZING.BLAZING_FIRE_WIDTH);
        applyValue(msg, "blazing.fire_lifetime", ModConfigs.BLAZING.BLAZING_FIRE_LIFETIME);
        applyValue(msg, "blazing.fire_dmg_inside_hearts", ModConfigs.BLAZING.BLAZING_FIRE_DMG_INSIDE_HEARTS);
        applyValue(msg, "blazing.burn_dmg_hearts", ModConfigs.BLAZING.BLAZING_BURN_DMG_HEARTS);
        applyValue(msg, "blazing.burn_duration", ModConfigs.BLAZING.BLAZING_BURN_DURATION);
        applyValue(msg, "blazing.fire_damage_through_blocks", ModConfigs.BLAZING.FIRE_DAMAGE_THROUGH_BLOCKS);
        applyValue(msg, "blazing.cooldown_ticks", ModConfigs.BLAZING.COOLDOWN_TICKS);
    }

    @SuppressWarnings("unchecked")
    private static <T> void applyValue(ConfigSyncS2C msg, String key, net.minecraftforge.common.ForgeConfigSpec.ConfigValue<T> configValue) {
        Object value = msg.configValues.get(key);
        if (value != null) {
            configValue.set((T) value);
        }
    }
}