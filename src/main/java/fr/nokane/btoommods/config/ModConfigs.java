package fr.nokane.btoommods.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Units / Unités :
 * - 20 ticks = 1 second / 20 ticks = 1 seconde
 * - 1 heart = 2 HP / 1 cœur = 2 PV
 */
public class ModConfigs {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        COMMON = new Common(b);
        COMMON_SPEC = b.build();
    }

    public static class Common {
        // -------- STACKS PAR ITEM --------
        public final ForgeConfigSpec.IntValue CRACKER_STACK;
        public final ForgeConfigSpec.IntValue BLAZING_STACK;
        public final ForgeConfigSpec.IntValue TIMER_STACK;
        public final ForgeConfigSpec.IntValue REMOTE_STACK;
        public final ForgeConfigSpec.IntValue GAS_STACK;
        public final ForgeConfigSpec.IntValue GAS_DISABLED_STACK;
        public final ForgeConfigSpec.IntValue RADAR_STACK;

        // -------- CRACKER BIM --------
        public final ForgeConfigSpec.DoubleValue radius;
        public final ForgeConfigSpec.DoubleValue epicenterHearts;
        public final ForgeConfigSpec.DoubleValue blockBlast;
        public final ForgeConfigSpec.BooleanValue causesFire;
        public final ForgeConfigSpec.EnumValue<ExplosionMode> explosionMode;
        public final ForgeConfigSpec.BooleanValue manualBreakEnabled;
        public final ForgeConfigSpec.IntValue manualBreakMaxBlocks;
        public final ForgeConfigSpec.IntValue lifetimeTicks;

        // -------- BLAZING BIM --------
        public final ForgeConfigSpec.IntValue BLAZING_FIRE_LENGTH;
        public final ForgeConfigSpec.IntValue BLAZING_FIRE_WIDTH;
        public final ForgeConfigSpec.IntValue BLAZING_FIRE_LIFETIME;
        public final ForgeConfigSpec.DoubleValue BLAZING_FIRE_DMG_INSIDE_HEARTS;
        public final ForgeConfigSpec.DoubleValue BLAZING_BURN_DMG_HEARTS;
        public final ForgeConfigSpec.IntValue BLAZING_BURN_DURATION;
        public final ForgeConfigSpec.DoubleValue BLAZING_PROJECTILE_SPEED_MULT;

        // -------- GAS BIM --------
        public final ForgeConfigSpec.DoubleValue GAS_PROJECTILE_SPEED_MULT;
        public final ForgeConfigSpec.IntValue   GAS_EXPLODE_AFTER_TICKS;
        public final ForgeConfigSpec.IntValue   GAS_PICKUP_AFTER_TICKS;
        public final ForgeConfigSpec.IntValue   GAS_RING_STEP;
        public final ForgeConfigSpec.IntValue   GAS_MAX_RADIUS;
        public final ForgeConfigSpec.IntValue   GAS_EXPAND_STEP_TICKS;
        public final ForgeConfigSpec.DoubleValue GAS_DMG_INNER_HPS;
        public final ForgeConfigSpec.DoubleValue GAS_DMG_MID_HPS;
        public final ForgeConfigSpec.DoubleValue GAS_DMG_OUTER_HPS;
        public final ForgeConfigSpec.BooleanValue GAS_GRAVITY_LIMIT;    // ✅ remis
        public final ForgeConfigSpec.IntValue   GAS_MAX_ABOVE_GROUND;   // ✅ remis
        public final ForgeConfigSpec.BooleanValue GAS_DMG_BYPASS_ARMOR; // ✅ remis
        public final ForgeConfigSpec.DoubleValue  GAS_IMPACT_HEARTS;    // ✅ remis

        // -------- RADAR --------
        public final ForgeConfigSpec.IntValue RADAR_BASE_RADIUS;       // 50
        public final ForgeConfigSpec.IntValue RADAR_EXTRA_PER_ITEM;    // +5 / item en plus
        public final ForgeConfigSpec.IntValue RADAR_GLOW_TICKS;        // 3s = 60
        public final ForgeConfigSpec.IntValue RADAR_ACTIVE_WINDOW;     // 5s = 100
        public final ForgeConfigSpec.IntValue RADAR_COOLDOWN_TICKS;    // p.ex. 60

        // -------- REMOTE BIM --------
        public final ForgeConfigSpec.DoubleValue REMOTE_RADIUS;                  // dégâts entités (blocs)
        public final ForgeConfigSpec.DoubleValue REMOTE_EPICENTER_HEARTS;        // dégâts centre (cœurs)
        public final ForgeConfigSpec.DoubleValue REMOTE_BLOCK_BLAST;             // force explosion vanilla terrain
        public final ForgeConfigSpec.BooleanValue REMOTE_CAUSES_FIRE;            // feu on/off
        public final ForgeConfigSpec.EnumValue<ExplosionMode> REMOTE_EXPLOSION_MODE; // NONE / BREAK
        public final ForgeConfigSpec.BooleanValue REMOTE_MANUAL_BREAK_ENABLED;   // petit nettoyage
        public final ForgeConfigSpec.IntValue REMOTE_MANUAL_BREAK_MAX_BLOCKS;    // nb blocs max
        public final ForgeConfigSpec.IntValue REMOTE_LIFETIME_TICKS;             // durée vie (ticks)
        public final ForgeConfigSpec.IntValue REMOTE_MAX_ACTIVE;                 // limite active/joueur
        public final ForgeConfigSpec.IntValue REMOTE_SCAN_RADIUS;                // rayon scan (compte/slot)
        public final ForgeConfigSpec.IntValue REMOTE_TRIGGER_RADIUS;             // rayon déclenchement
        public final ForgeConfigSpec.IntValue REMOTE_MARKER_COOLDOWN_TICKS;      // cooldown marker (ticks)
        public final ForgeConfigSpec.DoubleValue REMOTE_PROJECTILE_SPEED_MULT;   // vitesse projectile

        // --------------  TIMER BIM -----------------------
        public final ForgeConfigSpec.IntValue    TIMER_DEFAULT_SECONDS;
        public final ForgeConfigSpec.DoubleValue TIMER_PROJECTILE_SPEED_MULT;
        public final ForgeConfigSpec.DoubleValue TIMER_EXPLOSION_STRENGTH;
        public final ForgeConfigSpec.BooleanValue TIMER_CAUSES_FIRE;
        public final ForgeConfigSpec.BooleanValue TIMER_BREAK_BLOCKS;
        public final ForgeConfigSpec.IntValue TIMER_HUD_RADIUS;
        public final ForgeConfigSpec.DoubleValue TIMER_RESTITUTION_GROUND;
        public final ForgeConfigSpec.DoubleValue TIMER_FRICTION_GROUND;
        public final ForgeConfigSpec.DoubleValue TIMER_RESTITUTION_WALL;
        public final ForgeConfigSpec.DoubleValue TIMER_FRICTION_WALL;
        public final ForgeConfigSpec.DoubleValue TIMER_MAX_BOUNCE_UP;
        public final ForgeConfigSpec.DoubleValue TIMER_STOP_EPS;

        public Common(ForgeConfigSpec.Builder b) {

            // -------- CRACKER BIM --------
            b.comment(
                    "FR: Paramètres de la bombe 'cracker' (explosion classique).",
                    "EN: Settings for the 'cracker' bomb (classic explosion)."
            ).push("cracker_bim");

            CRACKER_STACK = b.comment(
                    "FR: Taille de stack de l'item Cracker.",
                    "EN: Stack size for Cracker item."
            ).defineInRange("stack", 1, 1, 64);

            radius = b.comment(
                    "FR: Rayon maximal où l'explosion peut blesser (blocs).",
                    "EN: Max radius where the explosion can hurt (blocks)."
            ).defineInRange("radius", 5.0D, 0.0D, 64.0D);

            epicenterHearts = b.comment(
                    "FR: Dégâts au centre de l'explosion, en CŒURS (1 cœur = 2 PV).",
                    "EN: Damage at explosion epicenter, in HEARTS (1 heart = 2 HP)."
            ).defineInRange("epicenter_hearts", 8.0D, 0.0D, 100.0D);

            blockBlast = b.comment(
                    "FR: Puissance contre les blocs (plus grand = casse davantage).",
                    "EN: Blast strength vs blocks (higher = destroys more)."
            ).defineInRange("block_blast", 1.8D, 0.0D, 10.0D);

            causesFire = b.comment(
                    "FR: Si vrai, l'explosion allume du feu autour de l'impact.",
                    "EN: If true, the explosion ignites fire around impact."
            ).define("causes_fire", false);

            explosionMode = b.comment(
                    "FR: Mode de dégâts terrain: NONE = ne casse pas; BREAK = casse les blocs.",
                    "EN: Terrain damage mode: NONE = no terrain; BREAK = break blocks."
            ).defineEnum("explosion_mode", ExplosionMode.BREAK);

            manualBreakEnabled = b.comment(
                    "FR: Autorise un petit 'nettoyage' manuel de blocs restants après l'explosion.",
                    "EN: Enables a small post-blast cleanup of remaining blocks."
            ).define("manual_break_enabled", true);

            manualBreakMaxBlocks = b.comment(
                    "FR: Nombre max de blocs retirés par ce nettoyage.",
                    "EN: Max blocks removed by that cleanup."
            ).defineInRange("manual_break_max_blocks", 4, 0, 100);

            lifetimeTicks = b.comment(
                    "FR: Durée de vie de l'entité cracker (ticks) avant disparition.",
                    "EN: Cracker entity lifetime (ticks) before despawn."
            ).defineInRange("lifetime_ticks", 200, 1, 12000);

            b.pop();

            // -------- BLAZING BIM --------
            b.comment(
                    "FR: Paramètres de la bombe incendiaire (tapis de feu).",
                    "EN: Settings for the incendiary bomb (fire strip)."
            ).push("blazing_bim");

            BLAZING_STACK = b.comment(
                    "FR: Taille de stack de l'item Blazing.",
                    "EN: Stack size for Blazing item."
            ).defineInRange("stack", 16, 1, 64);

            BLAZING_FIRE_LENGTH = b.comment(
                    "FR: Longueur du tapis de feu (blocs).",
                    "EN: Length of the fire strip (blocks)."
            ).defineInRange("fire_length", 8, 1, 64);

            BLAZING_FIRE_WIDTH = b.comment(
                    "FR: Largeur du tapis de feu (en bandes).",
                    "EN: Width of the fire strip (lanes)."
            ).defineInRange("fire_width", 1, 1, 8);

            BLAZING_FIRE_LIFETIME = b.comment(
                    "FR: Durée pendant laquelle le feu persiste au sol (ticks).",
                    "EN: How long the ground fire persists (ticks)."
            ).defineInRange("fire_lifetime", 6000, 20, 20*60*30);

            BLAZING_FIRE_DMG_INSIDE_HEARTS = b.comment(
                    "FR: Dégâts PAR TICK (cœurs) aux entités à l'intérieur du tapis de feu.",
                    "EN: PER-TICK damage (hearts) to entities inside the fire field."
            ).defineInRange("inside_hearts_per_tick", 2.0, 0.0, 50.0);

            BLAZING_BURN_DMG_HEARTS = b.comment(
                    "FR: Dégâts PAR TICK (cœurs) de l'effet de brûlure appliqué.",
                    "EN: PER-TICK damage (hearts) of the applied burning effect."
            ).defineInRange("burn_hearts_per_tick", 1.0, 0.0, 50.0);

            BLAZING_BURN_DURATION = b.comment(
                    "FR: Durée de l'effet de brûlure (ticks).",
                    "EN: Duration of the burning effect (ticks)."
            ).defineInRange("burn_duration", 40, 1, 20*60*10);

            BLAZING_PROJECTILE_SPEED_MULT = b.comment(
                    "FR: Multiplicateur de vitesse du projectile incendiaire.",
                    "EN: Speed multiplier for the blazing projectile."
            ).defineInRange("projectile_speed_mult", 1.25, 0.1, 10.0);

            b.pop();

            // -------- TIMER BIM --------
            b.comment("FR: Minuteur / détonation temporisée.", "EN: Timed / delayed detonation.").push("timer_bim");

            TIMER_STACK = b.comment("Stack size for Timer item.").defineInRange("stack", 1, 1, 64);

            TIMER_DEFAULT_SECONDS = b.comment("FR: Durée par défaut du compte à rebours (secondes).",
                            "EN: Default countdown duration (seconds).")
                    .defineInRange("default_seconds", 10, 1, 300);

            TIMER_PROJECTILE_SPEED_MULT = b.comment("FR: Multiplicateur de vitesse du projectile (× puissance d'arc).",
                            "EN: Projectile speed multiplier (× bow power).")
                    .defineInRange("projectile_speed_mult", 1.0D, 0.1D, 10.0D);

            TIMER_EXPLOSION_STRENGTH = b.comment("FR: Force d'explosion vanilla quand le timer atteint 0.",
                            "EN: Vanilla explosion power when the timer hits 0.")
                    .defineInRange("explosion_strength", 3.0D, 0.1D, 10.0D);

            TIMER_CAUSES_FIRE = b.comment("FR: L'explosion allume du feu.", "EN: Explosion causes fire.")
                    .define("causes_fire", false);

            TIMER_BREAK_BLOCKS = b.comment("FR: Casse les blocs (mode BREAK) sinon NONE.",
                            "EN: Break blocks (BREAK) else NONE.")
                    .define("break_blocks", false);

            TIMER_HUD_RADIUS = b.comment(
                    "FR: Distance max (en blocs) à laquelle un joueur peut voir le HUD du Timer BIM posé.",
                    "EN: Max distance (blocks) where players can see the Timer BIM HUD."
            ).defineInRange("hud_radius", 16, 0, 128);

            TIMER_RESTITUTION_GROUND = b.comment("FR: Coefficient de rebond au sol (0 = pas de rebond, 1 = rebond parfait).",
                            "EN: Ground restitution (0 = no bounce, 1 = perfect bounce).")
                    .defineInRange("restitution_ground", 0.2D, 0.0D, 1.0D);

            TIMER_FRICTION_GROUND = b.comment("FR: Friction horizontale au sol.",
                            "EN: Ground friction.")
                    .defineInRange("friction_ground", 0.4D, 0.0D, 1.0D);

            TIMER_RESTITUTION_WALL = b.comment("FR: Coefficient de rebond contre les murs.",
                            "EN: Wall restitution.")
                    .defineInRange("restitution_wall", 0.2D, 0.0D, 1.0D);

            TIMER_FRICTION_WALL = b.comment("FR: Friction horizontale contre les murs.",
                            "EN: Wall friction.")
                    .defineInRange("friction_wall", 0.6D, 0.0D, 1.0D);

            TIMER_MAX_BOUNCE_UP = b.comment("FR: Rebond vertical max.",
                            "EN: Max vertical bounce.")
                    .defineInRange("max_bounce_up", 0.12D, 0.0D, 1.0D);

            TIMER_STOP_EPS = b.comment("FR: Tolérance d’arrêt (plus haut = s’arrête plus vite).",
                            "EN: Stop epsilon (higher = stops earlier).")
                    .defineInRange("stop_eps", 0.06D, 0.0D, 0.5D);

            b.pop();

            // -------- GAS BIM --------
            b.comment(
                    "FR: Paramètres de la bombe à gaz (nuage qui s'étend par anneaux).",
                    "EN: Settings for the gas bomb (cloud expanding in rings)."
            ).push("gas_bim");

            GAS_STACK = b.comment(
                    "FR: Taille de stack de l'item Gaz.",
                    "EN: Stack size for Gas item."
            ).defineInRange("stack", 16, 1, 64);

            GAS_PROJECTILE_SPEED_MULT = b.comment(
                    "FR: Multiplicateur de vitesse du projectile gaz (<1 = plus lent qu'un arc).",
                    "EN: Speed multiplier for the gas projectile (<1 = slower than a bow)."
            ).defineInRange("projectile_speed_mult", 0.85, 0.1, 10.0);

            GAS_EXPLODE_AFTER_TICKS = b.comment(
                    "FR: Délai avant création du nuage (pointe/fusée), en ticks.",
                    "EN: Fuse time before the cloud spawns, in ticks."
            ).defineInRange("explode_after_ticks", 60, 5, 20*60);

            GAS_PICKUP_AFTER_TICKS = b.comment(
                    "FR: Délai minimal avant de pouvoir ramasser la bombe posée (ticks).",
                    "EN: Minimum delay before a placed bomb can be picked up (ticks)."
            ).defineInRange("pickup_after_ticks", 20, 0, 20*60);

            GAS_RING_STEP = b.comment(
                    "FR: Taille d'un anneau (blocs). Le nuage utilise jusqu'à 3 anneaux.",
                    "EN: Size of one ring (blocks). The cloud uses up to 3 rings."
            ).defineInRange("ring_step", 15, 5, 64);

            GAS_MAX_RADIUS = b.comment(
                    "FR: Rayon maximum atteint par le nuage (≈ 3 × ring_step par défaut).",
                    "EN: Maximum radius reached by the cloud (≈ 3 × ring_step by default)."
            ).defineInRange("max_radius", 45, 5, 128);

            GAS_EXPAND_STEP_TICKS = b.comment(
                    "FR: Temps entre deux expansions d'anneau (ticks).",
                    "EN: Time between ring expansions (ticks)."
            ).defineInRange("expand_step_ticks", 40, 2, 20*20);

            // ====== défaut = 1 cœur/s partout ======
            GAS_DMG_INNER_HPS = b.comment(
                    "FR: Dégâts en CŒURS PAR SECONDE dans l'anneau 1 (0–ring_step).",
                    "EN: Damage in HEARTS PER SECOND in ring 1 (0–ring_step)."
            ).defineInRange("inner_hps", 1.0, 0.0, 50.0);

            GAS_DMG_MID_HPS = b.comment(
                    "FR: Dégâts en CŒURS PAR SECONDE dans l'anneau 2 (ring_step–2×ring_step).",
                    "EN: Damage in HEARTS PER SECOND in ring 2 (ring_step–2×ring_step)."
            ).defineInRange("mid_hps", 1.0, 0.0, 50.0);

            GAS_DMG_OUTER_HPS = b.comment(
                    "FR: Dégâts en CŒURS PAR SECONDE dans l'anneau 3 (2×ring_step–3×ring_step).",
                    "EN: Damage in HEARTS PER SECOND in ring 3 (2×ring_step–3×ring_step)."
            ).defineInRange("outer_hps", 1.0, 0.0, 50.0);

            GAS_GRAVITY_LIMIT = b.comment(
                    "FR: Si vrai, n'affecte que les entités proches du sol (limite de hauteur).",
                    "EN: If true, affects only entities close to the ground (height limit)."
            ).define("gravity_limit", true);

            GAS_MAX_ABOVE_GROUND = b.comment(
                    "FR: Hauteur max au-dessus du sol pour être affecté quand gravity_limit = true (blocs).",
                    "EN: Max height above ground to be affected when gravity_limit = true (blocks)."
            ).defineInRange("max_above_ground", 5, 0, 32);

            GAS_DMG_BYPASS_ARMOR = b.comment(
                    "FR: Les dégâts du gaz ignorent l'armure s'ils sont à true.",
                    "EN: Gas damage bypasses armor when true."
            ).define("bypass_armor", true);

            GAS_IMPACT_HEARTS = b.comment(
                    "FR: Dégâts en CŒURS infligés à l'IMPACT du projectile de gaz sur une entité.",
                    "EN: Damage in HEARTS dealt on IMPACT when the gas projectile hits an entity."
            ).defineInRange("impact_hearts", 1.0D, 0.0D, 50.0D);

            b.pop();

            // -------- GAS BIM (désactivé/coque) --------
            b.comment(
                    "FR: Coque de bombe gaz (récupérée une fois le gaz dissipé).",
                    "EN: Gas bomb shell (recoverable after gas dissipates)."
            ).push("gas_bim_disabled");

            GAS_DISABLED_STACK = b.comment(
                    "FR: Taille de stack de la coque de bombe gaz.",
                    "EN: Stack size for gas bomb shell."
            ).defineInRange("stack", 16, 1, 64);

            b.pop();

            // -------- RADAR --------
            b.comment(
                    "FR: Radar implanté: objet greffé, non largable. Un scan met en surbrillance (Glow) les joueurs ACTIFS",
                    "    (un joueur est actif s'il s'est déplacé dans la fenêtre d'activité ci-dessous).",
                    "    Les joueurs détectés voient aussi le scanneur en Glow. L'effet est local (par joueur).",
                    "EN: Implanted radar: grafted item, cannot be dropped. A scan highlights (Glow) ACTIVE players",
                    "    (a player is active if they moved within the activity window below).",
                    "    Detected players also see the scanner glowing. Glow is per-viewer."
            ).push("radar");

            RADAR_STACK = b.comment(
                    "FR: Taille de stack de l'item Radar.",
                    "EN: Stack size for Radar item."
            ).defineInRange("stack", 64, 1, 64);

            RADAR_BASE_RADIUS = b.comment(
                    "FR: Portée de base du balayage (en blocs).",
                    "EN: Base scan radius (in blocks)."
            ).defineInRange("base_radius", 50, 1, 256);

            RADAR_EXTRA_PER_ITEM = b.comment(
                    "FR: Bonus de portée par item RADAR supplémentaire présent dans l'inventaire (booster).",
                    "    L'implant compte pour 0; seuls les items additionnels augmentent la portée.",
                    "EN: Extra range per additional RADAR item in inventory (booster).",
                    "    The implant counts as 0; only extra items increase the range."
            ).defineInRange("extra_per_item", 5, 0, 64);

            RADAR_GLOW_TICKS = b.comment(
                    "FR: Durée de l'effet Glow appliqué après un scan (en ticks).",
                    "    S'applique aux cibles détectées, et au scanneur vu par ces cibles.",
                    "EN: Glow duration applied after a scan (in ticks).",
                    "    Applies to detected targets, and to the scanner as seen by those targets."
            ).defineInRange("glow_ticks", 60, 1, 20*30);

            RADAR_ACTIVE_WINDOW = b.comment(
                    "FR: Fenêtre d'activité (en ticks). Un joueur est 'actif' s'il a bougé pendant ces N ticks",
                    "    avant le scan (ex: 100 ticks ≈ 5 s).",
                    "EN: Activity window (in ticks). A player is considered 'active' if they moved within",
                    "    the last N ticks before the scan (e.g., 100 ticks ≈ 5 s)."
            ).defineInRange("active_window_ticks", 100, 1, 20*60);

            RADAR_COOLDOWN_TICKS = b.comment(
                    "FR: Délai minimal entre deux scans (anti-spam), en ticks.",
                    "EN: Minimum delay between scans (anti-spam), in ticks."
            ).defineInRange("cooldown_ticks", 60, 0, 20*60);

            b.pop();

            // -------- REMOTE BIM --------
            b.comment(
                    "FR: Bombe 'remote' (détonation à distance).",
                    "    - radius: rayon des dégâts entités (blocs)",
                    "    - epicenter_hearts: dégâts au centre en CŒURS (1 cœur = 2 PV)",
                    "    - block_blast + explosion_mode/causes_fire: explosion vanilla terrain",
                    "    - manual_break_*: petit nettoyage de blocs (facile à casser) après l'explosion",
                    "    - lifetime_ticks: durée de vie (0 = infini tant que la bombe est collée)",
                    "    - max_active_per_player: nombre max de remotes actives par joueur",
                    "    - scan_radius: portée de recherche pour compter/assigner les slots",
                    "    - trigger_radius: portée pour trouver les remotes à déclencher",
                    "    - marker_cooldown_ticks: délai entre envois de marqueur au propriétaire",
                    "    - projectile_speed_mult: vitesse du projectile (× puissance d'arc)",
                    "EN: Remote-controlled bomb."
            ).push("remote_bim");

            REMOTE_STACK = b.comment(
                    "FR: Taille de stack de l'item Remote.",
                    "EN: Stack size for Remote item."
            ).defineInRange("stack", 16, 1, 64);

            REMOTE_RADIUS = b.comment(
                    "FR: Rayon des dégâts entités (blocs).",
                    "EN: Damage radius for living entities (blocks)."
            ).defineInRange("radius", 12.0D, 0.0D, 64.0D);

            REMOTE_EPICENTER_HEARTS = b.comment(
                    "FR: Dégâts au centre en CŒURS (1 cœur = 2 PV).",
                    "EN: Damage at the epicenter in HEARTS (1 heart = 2 HP)."
            ).defineInRange("epicenter_hearts", 10.0D, 0.0D, 100.0D);

            REMOTE_BLOCK_BLAST = b.comment(
                    "FR: Puissance d'explosion vanilla contre le terrain.",
                    "EN: Vanilla explosion strength against blocks."
            ).defineInRange("block_blast", 0.0D, 0.0D, 10.0D);

            REMOTE_CAUSES_FIRE = b.comment(
                    "FR: Si vrai, met le feu avec l'explosion vanilla.",
                    "EN: If true, ignites fire with the vanilla explosion."
            ).define("causes_fire", false);

            REMOTE_EXPLOSION_MODE = b.comment(
                    "FR: Mode explosion terrain: NONE (ne casse pas) / BREAK (casse).",
                    "EN: Terrain mode: NONE (no terrain) / BREAK (break blocks)."
            ).defineEnum("explosion_mode", ExplosionMode.NONE);

            REMOTE_MANUAL_BREAK_ENABLED = b.comment(
                    "FR: Active un léger nettoyage de blocs faciles (hardness ≤ 3).",
                    "EN: Enables a small cleanup of easy blocks (hardness ≤ 3)."
            ).define("manual_break_enabled", true);

            REMOTE_MANUAL_BREAK_MAX_BLOCKS = b.comment(
                    "FR: Nombre maximal de blocs cassés par ce nettoyage.",
                    "EN: Maximum blocks broken by that cleanup."
            ).defineInRange("manual_break_max_blocks", 12, 0, 200);

            REMOTE_LIFETIME_TICKS = b.comment(
                    "FR: Durée de vie avant despawn quand NON collée (0 = infini).",
                    "EN: Lifetime before despawn when NOT stuck (0 = infinite)."
            ).defineInRange("lifetime_ticks", 0, 0, 12000);

            REMOTE_MAX_ACTIVE = b.comment(
                    "FR: Nombre maximal de bombes remote actives par joueur.",
                    "EN: Maximum number of active remote bombs per player."
            ).defineInRange("max_active_per_player", 8, 1, 64);

            REMOTE_SCAN_RADIUS = b.comment(
                    "FR: Rayon de scan pour compter les remotes et assigner les slots.",
                    "EN: Scan radius used to count remotes and assign slots."
            ).defineInRange("scan_radius", 256, 16, 2048);

            REMOTE_TRIGGER_RADIUS = b.comment(
                    "FR: Rayon pour trouver les remotes à déclencher (autour du joueur).",
                    "EN: Radius to find remotes to trigger (around the player)."
            ).defineInRange("trigger_radius", 256, 16, 4096);

            REMOTE_MARKER_COOLDOWN_TICKS = b.comment(
                    "FR: Délai entre envois de marqueur (ticks) quand collée.",
                    "EN: Cooldown between owner marker packets (ticks) while stuck."
            ).defineInRange("marker_cooldown_ticks", 20, 1, 200);

            REMOTE_PROJECTILE_SPEED_MULT = b.comment(
                    "FR: Multiplicateur de vitesse du projectile (× puissance d'arc).",
                    "EN: Projectile speed multiplier (× bow charge power)."
            ).defineInRange("projectile_speed_mult", 1.20D, 0.1D, 10.0D);

            b.pop();
        }
    }

    public enum ExplosionMode { NONE, BREAK }
}
