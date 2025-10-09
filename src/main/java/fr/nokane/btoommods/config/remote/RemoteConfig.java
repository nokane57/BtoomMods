package fr.nokane.btoommods.config.remote;

import net.minecraftforge.common.ForgeConfigSpec;

public class RemoteConfig {

    public final ForgeConfigSpec.IntValue STACK;
    public final ForgeConfigSpec.DoubleValue VITESSE_PROJECTILE;
    public final ForgeConfigSpec.DoubleValue POIDS_PROJECTILE;

    public final ForgeConfigSpec.DoubleValue REMOTE_RADIUS;
    public final ForgeConfigSpec.BooleanValue REMOTE_CAUSES_FIRE;

    public final ForgeConfigSpec.BooleanValue BREAK_BLOCKS;

    public final ForgeConfigSpec.IntValue REMOTE_MAX_ACTIVE;           // par joueur
    public final ForgeConfigSpec.IntValue MAX_REMOTE;                  // total global
    public final ForgeConfigSpec.IntValue REMOTE_SCAN_RADIUS;          // rayon scan (compte/slot)
    public final ForgeConfigSpec.IntValue REMOTE_TRIGGER_RADIUS;       // rayon déclenchement
    public final ForgeConfigSpec.IntValue REMOTE_MARKER_COOLDOWN_TICKS;// cooldown marker (ticks)
    public final ForgeConfigSpec.IntValue REMOTE_LIFETIME_TICKS;

    public RemoteConfig(ForgeConfigSpec.Builder b) {
        b.push("remote_bim");

        STACK = b.comment(
                "FR: Taille maximale de stack pour la bombe Remote.",
                "EN: Maximum stack size for Remote bomb item."
        ).defineInRange("stack", 16, 1, 64);

        VITESSE_PROJECTILE = b.comment(
                "FR: Multiplicateur de vitesse du projectile Remote.",
                "EN: Speed multiplier for Remote projectile."
        ).defineInRange("projectile_speed_mult", 1.20D, 0.1D, 10.0D);

        POIDS_PROJECTILE = b.comment(
                "FR: Coefficient de poids du projectile (affecte la gravité et le rebond).",
                "EN: Weight factor (affects gravity and bounce)."
        ).defineInRange("projectile_weight", 1.0D, 0.1D, 10.0D);

        REMOTE_RADIUS = b.comment(
                "FR: Rayon des dégâts sur les entités (en blocs).",
                "EN: Explosion radius for entity damage (in blocks)."
        ).defineInRange("radius", 12.0D, 1.0D, 128.0D);

        REMOTE_CAUSES_FIRE = b.comment(
                "FR: Si vrai, l'explosion enflamme le terrain et les entités.",
                "EN: If true, explosion causes fire."
        ).define("causes_fire", false);

        BREAK_BLOCKS = b.comment(
                "FR: Si vrai, la bombe détruit les blocs du terrain à l'explosion.",
                "EN: If true, explosion breaks blocks."
        ).define("break_blocks", true);

        REMOTE_MAX_ACTIVE = b.comment(
                "FR: Nombre maximum de bombes Remote actives par joueur.",
                "EN: Maximum number of active Remote bombs per player."
        ).defineInRange("max_active_per_player", 8, 1, 64);

        MAX_REMOTE = b.comment(
                "FR: Nombre total global maximum de bombes Remote actives (tous joueurs confondus).",
                "EN: Global maximum number of active Remote bombs (all players combined)."
        ).defineInRange("max_remote_global", 128, 1, 1024);

        REMOTE_SCAN_RADIUS = b.comment(
                "FR: Rayon de scan utilisé pour trouver les bombes Remote à gérer (en blocs).",
                "EN: Scan radius used to detect and assign Remote bombs (in blocks)."
        ).defineInRange("scan_radius", 256, 16, 2048);

        REMOTE_TRIGGER_RADIUS = b.comment(
                "FR: Rayon de déclenchement des bombes autour du joueur (en blocs).",
                "EN: Trigger radius for Remote bombs around the player."
        ).defineInRange("trigger_radius", 256, 16, 4096);

        REMOTE_MARKER_COOLDOWN_TICKS = b.comment(
                "FR: Temps entre deux envois de marqueur (ticks) vers le joueur propriétaire.",
                "EN: Cooldown between marker updates sent to the owner (in ticks)."
        ).defineInRange("marker_cooldown_ticks", 20, 1, 200);

        REMOTE_LIFETIME_TICKS = b.comment(
                "FR: Durée de vie (en ticks) d'une Remote BIM avant suppression si elle n'est pas collée.",
                "EN: Lifetime (in ticks) before a Remote BIM despawns if not stuck."
        ).defineInRange("remote_lifetime_ticks", 1200, 100, 20 * 60 * 10); // 1 min par défaut


        b.pop();
    }
}
