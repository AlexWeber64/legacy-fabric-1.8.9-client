package io.github.alexweber64.axolotlextras;

import java.util.ArrayList;
import java.util.List;

import io.github.alexweber64.axolotlextras.bedwars.ChatStat;
import io.github.alexweber64.axolotlextras.bedwars.ResourceCounterHud;
import io.github.alexweber64.axolotlextras.bedwars.StatAnnouncer;
import io.github.alexweber64.axolotlextras.hacks.HackPanic;
import io.github.alexweber64.axolotlextras.hacks.HackRenderer;
import io.github.alexweber64.axolotlextras.hacks.RadarHud;
import io.github.alexweber64.axolotlextras.perf.BenchDriver;
import io.github.alexweber64.axolotlextras.perf.FrameBench;
import io.github.alexweber64.axolotlextras.ui.ExtrasScreen;
import io.github.alexweber64.axolotlextras.ui.HacksScreen;
import io.github.alexweber64.axolotlextras.ui.RoundedRect;
import io.github.alexweber64.axolotlextras.bedwars.ResourceMath;
import io.github.alexweber64.axolotlextras.bedwars.UpgradeDisplayHud;
import io.github.alexweber64.axolotlextras.quickplay.QuickplayScreen;
import io.github.axolotlclient.AxolotlClient;
import io.github.axolotlclient.AxolotlClientConfig.options.BooleanOption;
import io.github.axolotlclient.AxolotlClientConfig.options.EnumOption;
import io.github.axolotlclient.AxolotlClientConfig.options.GenericOption;
import io.github.axolotlclient.AxolotlClientConfig.options.IntegerOption;
import io.github.axolotlclient.AxolotlClientConfig.options.OptionCategory;
import io.github.axolotlclient.modules.AbstractModule;
import io.github.axolotlclient.modules.freelook.Freelook;
import io.github.axolotlclient.modules.hud.HudManager;
import io.github.axolotlclient.modules.hud.gui.hud.simple.ToggleSprintHud;
import io.github.axolotlclient.util.FeatureDisabler;
import io.github.axolotlclient.util.events.Events;
import net.fabricmc.loader.api.FabricLoader;
import net.legacyfabric.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;
import org.lwjgl.input.Keyboard;

/**
 * Entrypoint for the "axolotlclient.module" extension point. AxolotlClient calls {@link #init()} after its own
 * config exists and before the config manager loads the saved file, so options added here are saved and loaded
 * together with the rest of AxolotlClient's settings.
 *
 * <p>Every feature has its own switch, shown under "Extras" in the AxolotlClient options menu. All of them are off
 * by default.
 */
public class ExtrasModule extends AbstractModule {

	public static final OptionCategory CATEGORY = new OptionCategory("axolotlextras");

	/** The hacks, as their own category in AxolotlClient's menu (Right Shift), next to Extras. */
	public static final OptionCategory HACKS = new OptionCategory("axolotlextras.hacks");

	/** Boxes around other players, through walls (NPCs left out; see the ESP switches below for the rest). */
	public static final BooleanOption PLAYER_ESP = new BooleanOption("axolotlextras.playerEsp", false);

	/** Lines from the middle of the screen to every other player. */
	public static final BooleanOption ESP_TRACERS = new BooleanOption("axolotlextras.espTracers", false);

	/** How many resources lie on the ground at each generator, with the resource icons, through walls. */
	public static final BooleanOption GENERATOR_COUNTS = new BooleanOption("axolotlextras.generatorCounts", false);

	/** The blocks around each bed, with block icons, innermost first, through walls. */
	public static final BooleanOption BED_DEFENSE = new BooleanOption("axolotlextras.bedDefense", false);

	/** A chat note when a bed gets obsidian around it, saying whether it is a full layer or has gaps. */
	public static final BooleanOption OBSIDIAN_ALERT = new BooleanOption("axolotlextras.obsidianAlert", false);

	/** A beam and a label over every bed the obsidian alert reported, until the obsidian or the bed is gone. */
	public static final BooleanOption OBSIDIAN_WAYPOINTS = new BooleanOption("axolotlextras.obsidianWaypoints", true);

	/** Over each enemy's head: their best armor piece with its Protection level, and their held item with its enchantment. */
	public static final BooleanOption ENEMY_ARMOR = new BooleanOption("axolotlextras.enemyArmor", false);

	/** Takes away the dark fog of the blindness effect (drawing only). */
	public static final BooleanOption NO_BLINDNESS = new BooleanOption("axolotlextras.noBlindness", false);

	/** Team-coloured name, health and distance over every other player, through walls. */
	public static final BooleanOption NAMETAGS = new BooleanOption("axolotlextras.nametags", false);

	/** A chat line and a sound when an enemy comes near your own bed. */
	public static final BooleanOption RUSH_ALERT = new BooleanOption("axolotlextras.rushAlert", false);

	/** How near (in blocks) an enemy has to get to your bed for the Rush Alert. */
	public static final EnumOption RUSH_DISTANCE = new EnumOption("axolotlextras.rushDistance",
		new String[] {"axolotlextras.rush.15", "axolotlextras.rush.25", "axolotlextras.rush.40"}, "axolotlextras.rush.25");

	/** Where flying fireballs will hit, the blocks the blast will likely break, and a sound when one comes at you. */
	public static final BooleanOption FIREBALL_WARNING = new BooleanOption("axolotlextras.fireballWarning", false);

	/** Where flying ender pearls (anyone's) will land. */
	public static final BooleanOption PEARL_LANDING = new BooleanOption("axolotlextras.pearlLanding", false);

	/** The flight of your own pearl, snowball, egg or arrow (while drawing the bow) and where it would hit. */
	public static final BooleanOption TRAJECTORIES = new BooleanOption("axolotlextras.trajectories", false);

	/** A minimap of the players around you, each shown by the face of their skin. */
	public static final BooleanOption RADAR = new BooleanOption("axolotlextras.radar", false);

	// Fine tuning for the radar.

	/** What the radar's middle is: you, or your own bed (live only while the bed is within your render distance). */
	public static final EnumOption RADAR_CENTER = new EnumOption("axolotlextras.radarCenter",
		new String[] {"axolotlextras.radarCenter.player", "axolotlextras.radarCenter.bed"}, "axolotlextras.radarCenter.player");

	/**
	 * With Radar Centre on Your Bed: once your bed is broken the radar centres on you for the rest of that game. Radar
	 * Centre itself is not changed, so the next game starts on the bed again.
	 */
	public static final BooleanOption RADAR_BED_FALLBACK = new BooleanOption("axolotlextras.radarBedFallback", true);

	/** How many blocks the radar's edge stands for. */
	public static final EnumOption RADAR_RANGE = new EnumOption("axolotlextras.radarRange",
		new String[] {"axolotlextras.range.16", "axolotlextras.range.32", "axolotlextras.range.64", "axolotlextras.range.128"},
		"axolotlextras.range.64");

	/** Round or square. */
	public static final EnumOption RADAR_SHAPE = new EnumOption("axolotlextras.radarShape",
		new String[] {"axolotlextras.radarShape.circle", "axolotlextras.radarShape.square"}, "axolotlextras.radarShape.circle");

	/** The radar's width and height, in GUI pixels. */
	public static final IntegerOption RADAR_SIZE = new IntegerOption("axolotlextras.radarSize", 100, 50, 250);

	/** The size of the faces, in percent of 10 GUI pixels. */
	public static final IntegerOption RADAR_ICON_SIZE = new IntegerOption("axolotlextras.radarIconSize", 100, 50, 250);

	/** How dark the radar's background is, in percent (0 is no background). */
	public static final IntegerOption RADAR_OPACITY = new IntegerOption("axolotlextras.radarOpacity", 50, 0, 100);

	/** The radar turns with you, so your view always points up (off: north is up). */
	public static final BooleanOption RADAR_ROTATE = new BooleanOption("axolotlextras.radarRotate", true);

	/** Your own face in the middle (off: an arrow). */
	public static final BooleanOption RADAR_SELF = new BooleanOption("axolotlextras.radarSelf", true);

	/** The radar also shows your teammates. */
	public static final BooleanOption RADAR_TEAMMATES = new BooleanOption("axolotlextras.radarTeammates", true);

	/** The radar also shows players with the invisibility effect. */
	public static final BooleanOption RADAR_INVISIBLE = new BooleanOption("axolotlextras.radarInvisible", true);

	/** Players out of range stay on the radar's edge, faded, in their direction. */
	public static final BooleanOption RADAR_EDGE = new BooleanOption("axolotlextras.radarEdge", true);

	/** A frame in the team colour around each face. */
	public static final BooleanOption RADAR_BORDER = new BooleanOption("axolotlextras.radarBorder", true);

	/** The name under each face. */
	public static final BooleanOption RADAR_NAMES = new BooleanOption("axolotlextras.radarNames", false);

	/** A small arrow next to players more than 3 blocks above or below you. */
	public static final BooleanOption RADAR_HEIGHT = new BooleanOption("axolotlextras.radarHeight", true);

	/** Cross lines, a ring at half the range, and an N for north. */
	public static final BooleanOption RADAR_GRID = new BooleanOption("axolotlextras.radarGrid", true);

	// Fine tuning for the hacks above.

	/** ESP, tracers and nametags also show players with the invisibility effect. */
	public static final BooleanOption ESP_INVISIBLE = new BooleanOption("axolotlextras.espInvisible", true);

	/** ESP, tracers and nametags also show your teammates (off: enemies only). */
	public static final BooleanOption ESP_TEAMMATES = new BooleanOption("axolotlextras.espTeammates", true);

	/** The ESP box: an outline, a see-through fill, or both. */
	public static final EnumOption ESP_STYLE = new EnumOption("axolotlextras.espStyle",
		new String[] {"axolotlextras.espStyle.outline", "axolotlextras.espStyle.filled", "axolotlextras.espStyle.both"},
		"axolotlextras.espStyle.outline");

	/** The ESP and tracer colour: team colour, distance (red near, green far) or health (green full, red low). */
	public static final EnumOption ESP_COLOR = new EnumOption("axolotlextras.espColor",
		new String[] {"axolotlextras.espColor.team", "axolotlextras.espColor.distance", "axolotlextras.espColor.health"},
		"axolotlextras.espColor.team");

	/** How far away players are still shown by ESP, tracers, nametags and enemy armor. */
	public static final EnumOption ESP_RANGE = new EnumOption("axolotlextras.espRange",
		new String[] {"axolotlextras.range.32", "axolotlextras.range.64", "axolotlextras.range.128", "axolotlextras.range.all"},
		"axolotlextras.range.all");

	/** Nametags show the health. */
	public static final BooleanOption NAMETAG_HEALTH = new BooleanOption("axolotlextras.nametagHealth", true);

	/** Nametags show the distance. */
	public static final BooleanOption NAMETAG_DISTANCE = new BooleanOption("axolotlextras.nametagDistance", true);

	/** Enemy Armor also shows the held item. */
	public static final BooleanOption ARMOR_HELD = new BooleanOption("axolotlextras.armorHeld", true);

	/** Generator Counts for iron. */
	public static final BooleanOption GEN_IRON = new BooleanOption("axolotlextras.genIron", true);

	/** Generator Counts for gold. */
	public static final BooleanOption GEN_GOLD = new BooleanOption("axolotlextras.genGold", true);

	/** Generator Counts for diamonds. */
	public static final BooleanOption GEN_DIAMOND = new BooleanOption("axolotlextras.genDiamond", true);

	/** Generator Counts for emeralds. */
	public static final BooleanOption GEN_EMERALD = new BooleanOption("axolotlextras.genEmerald", true);

	/** A chat note when an enemy goes invisible, and a mark where it happened for 60 seconds. */
	public static final BooleanOption INVIS_ALERT = new BooleanOption("axolotlextras.invisAlert", false);

	/** The Invisibility Alert also puts a beam and a label over the mark, like the obsidian waypoints. */
	public static final BooleanOption INVIS_WAYPOINTS = new BooleanOption("axolotlextras.invisWaypoints", true);

	/** The Invisibility Alert only counts players seen holding an invisibility potion just before. */
	public static final BooleanOption INVIS_NEED_POTION = new BooleanOption("axolotlextras.invisNeedPotion", true);

	/**
	 * A breadcrumb trail of players you cannot see, from the footstep, hurt, drink and bow sounds, footstep particles and
	 * block breaking the server still sends for them.
	 */
	public static final BooleanOption HIDDEN_TRAIL = new BooleanOption("axolotlextras.hiddenTrail", false);

	/** How long the hidden trail's crumbs stay. */
	public static final EnumOption TRAIL_TIME = new EnumOption("axolotlextras.trailTime",
		new String[] {"axolotlextras.seconds.5", "axolotlextras.seconds.10", "axolotlextras.seconds.20", "axolotlextras.seconds.30"},
		"axolotlextras.seconds.10");

	/** Drawn markers through walls, or particles in the world. */
	public static final EnumOption TRAIL_STYLE = new EnumOption("axolotlextras.trailStyle",
		new String[] {"axolotlextras.trailStyle.markers", "axolotlextras.trailStyle.particles"}, "axolotlextras.trailStyle.markers");

	/** A label over the newest crumb of each trail. */
	public static final BooleanOption TRAIL_LABELS = new BooleanOption("axolotlextras.trailLabels", true);

	/** A chat line when a new hidden trail starts. */
	public static final BooleanOption TRAIL_CHAT = new BooleanOption("axolotlextras.trailChat", true);

	/** The Invisibility Alert plays a sound with its chat line. */
	public static final BooleanOption INVIS_SOUND = new BooleanOption("axolotlextras.invisSound", true);

	/** Bed Defense also labels your own bed. */
	public static final BooleanOption BED_DEFENSE_OWN = new BooleanOption("axolotlextras.bedDefenseOwn", true);

	/** The Obsidian Alert plays a sound with its chat line. */
	public static final BooleanOption OBSIDIAN_SOUND = new BooleanOption("axolotlextras.obsidianSound", true);

	/** The Rush Alert plays a sound with its chat line. */
	public static final BooleanOption RUSH_SOUND = new BooleanOption("axolotlextras.rushSound", true);

	/** The Fireball Warning marks the blocks the blast will likely break. */
	public static final BooleanOption FIREBALL_BLOCKS = new BooleanOption("axolotlextras.fireballBlocks", true);

	/** The Fireball Warning plays a sound when a fireball is coming at you. */
	public static final BooleanOption FIREBALL_SOUND = new BooleanOption("axolotlextras.fireballSound", true);

	/** Trajectories while drawing a bow. */
	public static final BooleanOption TRAJECTORY_BOW = new BooleanOption("axolotlextras.trajectoryBow", true);

	/** Trajectories while holding a pearl, snowball or egg. */
	public static final BooleanOption TRAJECTORY_THROWN = new BooleanOption("axolotlextras.trajectoryThrown", true);

	/** A short note above the hotbar when a hack key is used (the emergency toggle, G and H by default, or a toggle key). */
	public static final BooleanOption HACK_KEY_NOTE = new BooleanOption("axolotlextras.hackKeyNote", true);

	/**
	 * The main switch of every hack: what the emergency toggle turns off and back on, and what gets a toggle key. Their
	 * fine-tuning switches (style, sounds, sizes) are left out: they do nothing while the hack is off.
	 */
	public static final BooleanOption[] HACK_MAIN_SWITCHES = {PLAYER_ESP, ESP_TRACERS, NAMETAGS, ENEMY_ARMOR, GENERATOR_COUNTS,
		BED_DEFENSE, OBSIDIAN_ALERT, RUSH_ALERT, FIREBALL_WARNING, PEARL_LANDING, TRAJECTORIES, NO_BLINDNESS, RADAR, INVIS_ALERT,
		HIDDEN_TRAIL};

	/** Label sizes are sliders in percent, from {@link #LABEL_SIZE_MIN} to {@link #LABEL_SIZE_MAX}. */
	public static final int LABEL_SIZE_MIN = LabelSizeMigration.MIN, LABEL_SIZE_MAX = LabelSizeMigration.MAX;

	/** The size of the Generator Counts labels, in percent. */
	public static final IntegerOption SIZE_GENERATORS = labelSize("axolotlextras.labelSizeGenerators");

	/** The size of the Bed Defense labels, in percent. */
	public static final IntegerOption SIZE_BEDS = labelSize("axolotlextras.labelSizeBeds");

	/** The size of the obsidian waypoint labels, in percent. */
	public static final IntegerOption SIZE_WAYPOINTS = labelSize("axolotlextras.labelSizeWaypoints");

	/** The size of the nametags, in percent. */
	public static final IntegerOption SIZE_NAMETAGS = labelSize("axolotlextras.labelSizeNametags");

	/** The size of the Enemy Armor labels, in percent. */
	public static final IntegerOption SIZE_ARMOR = labelSize("axolotlextras.labelSizeArmor");

	/** The size of the fireball and pearl labels, in percent. */
	public static final IntegerOption SIZE_PROJECTILES = labelSize("axolotlextras.labelSizeProjectiles");

	private static IntegerOption labelSize(String name) {
		return new IntegerOption(name, 100, LABEL_SIZE_MIN, LABEL_SIZE_MAX);
	}

	/** 1.7: the sword, food and bow animations keep swinging when you swing while holding right click. */
	public static final BooleanOption OLD_BLOCK_HITTING = new BooleanOption("axolotlextras.oldBlockHitting", false);

	/** 1.7: held item positions in first person, the bow pose and the fishing rod flip. */
	public static final BooleanOption OLD_FIRST_PERSON = new BooleanOption("axolotlextras.oldFirstPerson", false);

	/** 1.7: the camera goes down at once when you sneak and comes back up slowly when you stop. */
	public static final BooleanOption OLD_SNEAK = new BooleanOption("axolotlextras.oldSneak", false);

	/** 1.7: the sword block pose seen from outside (the arm straight up in front, the sword turned flat). */
	public static final BooleanOption OLD_THIRD_PERSON = new BooleanOption("axolotlextras.oldThirdPerson", false);

	/** Loads Bed Wars stats with the add-on's own loader (parallel, with retries) instead of AxolotlClient's. On by default. */
	public static final BooleanOption RELIABLE_STATS = new BooleanOption("axolotlextras.reliableStats", true);

	/** Removes the fog on land and under water. */
	public static final BooleanOption NO_FOG = new BooleanOption("axolotlextras.noFog", false);

	/** Keeps the player inventory in the middle of the screen while potion effects are active. */
	public static final BooleanOption KEEP_INVENTORY_CENTERED = new BooleanOption("axolotlextras.keepInventoryCentered", false);

	/** Skips the dark gradient that menus draw over the world. */
	public static final BooleanOption TRANSPARENT_UI = new BooleanOption("axolotlextras.transparentUi", false);

	/** Opens the quickplay menu with its key. */
	public static final BooleanOption QUICKPLAY = new BooleanOption("axolotlextras.quickplay", false);

	/** The quickplay menu only sends /play commands, which only mean something on Hypixel. */
	public static final BooleanOption QUICKPLAY_HYPIXEL_ONLY = new BooleanOption("axolotlextras.quickplayHypixelOnly", true);

	/** Stops AxolotlClient's per-server switch-off of Freelook, Toggle Sneak, Time Changer, Full Bright and Low Fire. On by default. */
	public static final BooleanOption IGNORE_SERVER_BLOCKS = new BooleanOption("axolotlextras.ignoreServerBlocks", true);

	/** Lets the Toggle Sprint key work while its HUD module is switched off. On by default. */
	public static final BooleanOption TOGGLE_WITHOUT_HUD = new BooleanOption("axolotlextras.toggleWithoutHud", true);

	/** Bed Wars level head shows the kill/death ratio instead of the configured stat (FKDR by default). On by default. */
	public static final BooleanOption KDR_LEVEL_HEAD = new BooleanOption("axolotlextras.kdrLevelHead", true);

	/** Bed Wars tab list shows the kill/death ratio to the left of the health number. On by default. */
	public static final BooleanOption KDR_TAB = new BooleanOption("axolotlextras.kdrTab", true);

	/**
	 * Shows Hypixel's own text for deaths, broken beds, eliminated teams and disconnects instead of AxolotlClient's short
	 * rewritten ones, with the stats it added still shown next to them. On by default.
	 */
	public static final BooleanOption NORMAL_MESSAGES = new BooleanOption("axolotlextras.normalMessages", true);

	/** Flat rounded buttons and a dark background in the menus, and the restyled HUD editor boxes. On by default. */
	public static final BooleanOption MODERN_MENUS = new BooleanOption("axolotlextras.modernMenus", true);

	/** How round the corners of the HUD boxes are. */
	public static final EnumOption HUD_CORNERS = new EnumOption("axolotlextras.hudCorners", RoundedRect.CORNERS,
		RoundedRect.CORNERS[2]);

	/** Blocks AxolotlClient's calls to its own server and the Discord SDK download. On by default. */
	public static final BooleanOption PRIVACY_LOCK = new BooleanOption("axolotlextras.privacyLock", true);

	/** Prints every player's chosen stat in the chat when a Bed Wars game starts. On by default. */
	public static final BooleanOption CHAT_STATS = new BooleanOption("axolotlextras.chatStats", true);

	/** Which stat goes in that chat list. */
	public static final EnumOption CHAT_STAT = new EnumOption("axolotlextras.chatStat", ChatStat.keys(), "kdr");

	private static final StatAnnouncer STAT_ANNOUNCER = new StatAnnouncer();

	/** Set at startup when a Privacy Lock patch could not be applied. */
	private static boolean privacyPatchFailed;

	/** True when the Raw Input mod is installed, which makes AxolotlClient's own raw mouse option redundant and harmful. */
	private static boolean rawMouseGuard;
	/** True once the label sizes from the old buttons have been carried over to the sliders (see {@link LabelSizeMigration}). */
	private static boolean labelSizesMigrated;

	private static KeyBinding quickplayKey;
	/** The emergency toggle: every hack off (G), and back on as they were (H). */
	private static KeyBinding hacksOffKey, hacksOnKey;
	/** One toggle key per {@link #HACK_MAIN_SWITCHES} entry, unbound until set in Controls. */
	private static KeyBinding[] hackToggleKeys = new KeyBinding[0];
	private static final String HACK_KEY_CATEGORY = "category.axolotlextras.hacks";
	private static HackPanic hackPanic;
	private static KeyBinding menuKey;
	/** Flips AxolotlClient's own Dynamic FOV option (the FOV widening when you sprint, draw a bow or have Speed). */
	private static KeyBinding dynamicFovKey;
	/** Switches the radar's middle between you and your bed. */
	private static KeyBinding radarCenterKey;
	private static ResourceCounterHud resourceCounter;
	private static UpgradeDisplayHud upgradeDisplay;

	@Override
	public void init() {
		CATEGORY.add(new GenericOption("axolotlextras.openMenu", "axolotlextras.openMenuLabel",
			(x, y) -> client.setScreen(new ExtrasScreen(client.currentScreen))));
		CATEGORY.add(MODERN_MENUS, HUD_CORNERS, PRIVACY_LOCK, NORMAL_MESSAGES, CHAT_STATS, CHAT_STAT, IGNORE_SERVER_BLOCKS, TOGGLE_WITHOUT_HUD, KDR_LEVEL_HEAD, KDR_TAB, NO_FOG, KEEP_INVENTORY_CENTERED, TRANSPARENT_UI, QUICKPLAY,
			QUICKPLAY_HYPIXEL_ONLY, OLD_BLOCK_HITTING, OLD_FIRST_PERSON, OLD_SNEAK, OLD_THIRD_PERSON, RELIABLE_STATS);
		AxolotlClient.CONFIG.addCategory(CATEGORY);

		HACKS.add(new GenericOption("axolotlextras.openHacks", "axolotlextras.openMenuLabel",
			(x, y) -> client.setScreen(new HacksScreen(client.currentScreen))));
		HACKS.add(PLAYER_ESP, ESP_TRACERS, ESP_STYLE, ESP_COLOR, ESP_RANGE, ESP_INVISIBLE, ESP_TEAMMATES, NAMETAGS,
			NAMETAG_HEALTH, NAMETAG_DISTANCE, ENEMY_ARMOR, ARMOR_HELD, GENERATOR_COUNTS, GEN_IRON, GEN_GOLD, GEN_DIAMOND,
			GEN_EMERALD, BED_DEFENSE, BED_DEFENSE_OWN, OBSIDIAN_ALERT, OBSIDIAN_WAYPOINTS, OBSIDIAN_SOUND, INVIS_ALERT,
			INVIS_NEED_POTION, INVIS_WAYPOINTS, INVIS_SOUND, HIDDEN_TRAIL, TRAIL_TIME, TRAIL_STYLE,
			TRAIL_LABELS, TRAIL_CHAT, RUSH_ALERT, RUSH_DISTANCE, RUSH_SOUND,
			FIREBALL_WARNING, FIREBALL_BLOCKS, FIREBALL_SOUND, PEARL_LANDING, TRAJECTORIES, TRAJECTORY_BOW, TRAJECTORY_THROWN,
			SIZE_GENERATORS, SIZE_BEDS, SIZE_WAYPOINTS, SIZE_NAMETAGS, SIZE_ARMOR, SIZE_PROJECTILES, NO_BLINDNESS, RADAR, RADAR_CENTER,
			RADAR_BED_FALLBACK, 			RADAR_RANGE, RADAR_SHAPE, RADAR_SIZE, RADAR_ICON_SIZE, RADAR_OPACITY, RADAR_ROTATE,
			RADAR_SELF, RADAR_TEAMMATES, RADAR_INVISIBLE, RADAR_EDGE, RADAR_BORDER, RADAR_NAMES, RADAR_HEIGHT, RADAR_GRID,
			HACK_KEY_NOTE);
		AxolotlClient.CONFIG.addCategory(HACKS);

		// HudManager.init() has already run by now (modules are initialised in order, external ones last), so the HUD
		// category exists. A newly added entry has to be initialised by hand.
		resourceCounter = new ResourceCounterHud();
		HudManager.getInstance().add(resourceCounter);
		resourceCounter.init();
		resourceCounter.onBoundsUpdate();

		upgradeDisplay = new UpgradeDisplayHud();
		HudManager.getInstance().add(upgradeDisplay);
		upgradeDisplay.init();
		upgradeDisplay.onBoundsUpdate();

		// The radar's place in the HUD editor (it is drawn there and moved there, while the Radar hack is on).
		RadarHud radarHud = new RadarHud();
		HudManager.getInstance().add(radarHud);
		radarHud.init();
		radarHud.onBoundsUpdate();

		menuKey = new KeyBinding("key.axolotlextras.menu", Keyboard.KEY_F8, "category.axolotlclient");
		KeyBindingHelper.registerKeyBinding(menuKey);

		quickplayKey = new KeyBinding("key.axolotlextras.quickplay", Keyboard.KEY_Y, "category.axolotlclient");
		KeyBindingHelper.registerKeyBinding(quickplayKey);

		// Unbound by default, so it takes no key used in game.
		dynamicFovKey = new KeyBinding("key.axolotlextras.dynamicFov", Keyboard.KEY_NONE, "category.axolotlclient");
		KeyBindingHelper.registerKeyBinding(dynamicFovKey);

		// The hack keys have their own section in Controls. Only the emergency toggle has keys by default; the toggle keys
		// start unbound so they do not take keys used in game.
		hacksOffKey = new KeyBinding("key.axolotlextras.hacksOff", Keyboard.KEY_G, HACK_KEY_CATEGORY);
		KeyBindingHelper.registerKeyBinding(hacksOffKey);
		hacksOnKey = new KeyBinding("key.axolotlextras.hacksOn", Keyboard.KEY_H, HACK_KEY_CATEGORY);
		KeyBindingHelper.registerKeyBinding(hacksOnKey);
		hackToggleKeys = new KeyBinding[HACK_MAIN_SWITCHES.length];
		for (int i = 0; i < HACK_MAIN_SWITCHES.length; i++) {
			// "axolotlextras.playerEsp" gets the key "key.axolotlextras.toggle.playerEsp"
			String name = HACK_MAIN_SWITCHES[i].getName();
			hackToggleKeys[i] = new KeyBinding("key.axolotlextras.toggle." + name.substring(name.indexOf('.') + 1),
				Keyboard.KEY_NONE, HACK_KEY_CATEGORY);
			KeyBindingHelper.registerKeyBinding(hackToggleKeys[i]);
		}
		radarCenterKey = new KeyBinding("key.axolotlextras.radarCenter", Keyboard.KEY_NONE, HACK_KEY_CATEGORY);
		KeyBindingHelper.registerKeyBinding(radarCenterKey);
		hackPanic = new HackPanic(FabricLoader.getInstance().getConfigDir().resolve("axolotl-extras").resolve("hacks-off.txt"));

		// Loading a different world (including joining another server or game) forgets the remembered counts.
		Events.WORLD_LOAD_EVENT.register(event -> {
			resourceCounter.resetAll();
			upgradeDisplay.resetAll();
			STAT_ANNOUNCER.reset();
			HackRenderer.reset();
			if (privacyPatchFailed) {
				client.inGameHud.getChatHud().addMessage(new LiteralText(
					"§c[Extras] The Privacy Lock could not be applied: AxolotlClient may contact its server. See the log."));
			}
		});

		Events.RECEIVE_CHAT_MESSAGE_EVENT.register(event -> {
			String line = event.getFormattedMessage().asUnformattedString();
			if (ResourceMath.isGameStartMessage(line)) {
				resourceCounter.onGameStart();
				HackRenderer.onGameStart();
				upgradeDisplay.onGameStart();
				if (CHAT_STATS.get()) {
					STAT_ANNOUNCER.onGameStart();
				}
			}
			upgradeDisplay.onChatLine(line);
			// Test hook: "AXBENCH label seconds" starts the frame recorder, singleplayer only.
			if (line.startsWith("AXBENCH ") && client.isInSingleplayer()) {
				String[] parts = line.split(" ");
				if (parts.length == 3 && parts[2].matches("[0-9]{1,4}")) {
					FrameBench.start(parts[1], Integer.parseInt(parts[2]));
				} else if (parts.length == 4 && parts[2].matches("[0-9]{1,4}") && parts[3].matches("[0-9]{1,3}")) {
					FrameBench.start(parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
				}
			}
		});

		rawMouseGuard = FabricLoader.getInstance().isModLoaded("rawinput");
		labelSizesMigrated = false;

		verifyPatches();
	}

	/**
	 * Loads the AxolotlClient classes the Privacy Lock patches right away (without running them), so a patch that does
	 * not apply shows up in the log at startup, and as a chat warning, instead of being found out later.
	 */
	private static void verifyPatches() {
		String[] privacyTargets = {
			"io.github.axolotlclient.util.NetworkHelper",
			"io.github.axolotlclient.modules.rpc.gameSdk.GameSdkDownloader"
		};
		for (String name : privacyTargets) {
			try {
				Class.forName(name, false, ExtrasModule.class.getClassLoader());
			} catch (ClassNotFoundException | LinkageError | RuntimeException e) {
				privacyPatchFailed = true;
				AxolotlClient.LOGGER.error("[Extras] PRIVACY LOCK NOT APPLIED to " + name + ": " + e);
			}
		}
		if (!privacyPatchFailed) {
			AxolotlClient.LOGGER.info("[Extras] Privacy Lock patches loaded: AxolotlClient server calls and the Discord SDK download are blocked while the option is on.");
		}
		// Each Bed Wars / stats patch adds a method with a known name to the class it patches, so its presence shows it applied.
		String[][] statsPatches = {
			{"io.github.axolotlclient.modules.hypixel.bedwars.BedwarsGame", "axolotlextras$kdrLevelHead"},
			{"io.github.axolotlclient.modules.hypixel.bedwars.BedwarsGame", "axolotlextras$noMadeUpWinstreaks"},
			{"io.github.axolotlclient.modules.hypixel.bedwars.BedwarsPlayer", "axolotlextras$askAgainForRealStats"},
			{"io.github.axolotlclient.modules.hypixel.bedwars.BedwarsPlayerStats", "axolotlextras$markGenerated"},
			{"io.github.axolotlclient.modules.hypixel.HypixelAbstractionLayer", "axolotlextras$noMadeUpLevel"}};
		boolean levelTextPatched = false;
		for (java.lang.reflect.Method method : net.minecraft.client.render.entity.EntityRenderer.class.getDeclaredMethods()) {
			if (method.getName().contains("axolotlextras$unknownLevelAsQuestionMark")) {
				levelTextPatched = true;
			}
		}
		AxolotlClient.LOGGER.info("[Extras] Lobby level text patch " + (levelTextPatched ? "applied" : "NOT applied (an unknown level shows as -1)"));
		for (String[] patch : statsPatches) {
			try {
				Class<?> target = Class.forName(patch[0], false, ExtrasModule.class.getClassLoader());
				boolean applied = false;
				for (java.lang.reflect.Method method : target.getDeclaredMethods()) {
					// Mixin renames its handler methods (handler$xyz$name), so the name only has to be contained
					if (method.getName().contains(patch[1])) {
						applied = true;
					}
				}
				if (applied) {
					AxolotlClient.LOGGER.info("[Extras] Stats patch applied: " + patch[1]);
				} else {
					AxolotlClient.LOGGER.error("[Extras] STATS PATCH NOT APPLIED: " + patch[1] + " (made-up numbers may show)");
				}
			} catch (ClassNotFoundException | LinkageError | RuntimeException e) {
				AxolotlClient.LOGGER.error("[Extras] Bed Wars patches did not apply to " + patch[0] + ": " + e);
			}
		}
	}

	/** Sets the size sliders to the sizes picked with the old size buttons, if the sliders were never saved (once per start). */
	private static void carryOverLabelSizes() {
		java.nio.file.Path config = FabricLoader.getInstance().getConfigDir().resolve("AxolotlClient.json");
		if (!java.nio.file.Files.exists(config)) {
			return;
		}
		try (java.io.Reader reader = java.nio.file.Files.newBufferedReader(config, java.nio.charset.StandardCharsets.UTF_8)) {
			com.google.gson.JsonElement root = new com.google.gson.JsonParser().parse(reader);
			if (!root.isJsonObject() || !root.getAsJsonObject().has(HACKS.getName())) {
				return;
			}
			java.util.Map<String, String> saved = new java.util.HashMap<>();
			for (java.util.Map.Entry<String, com.google.gson.JsonElement> e
				: root.getAsJsonObject().getAsJsonObject(HACKS.getName()).entrySet()) {
				if (e.getValue().isJsonPrimitive()) {
					saved.put(e.getKey(), e.getValue().getAsString());
				}
			}
			java.util.Map<String, Integer> values = LabelSizeMigration.carriedOver(saved);
			for (IntegerOption slider : new IntegerOption[] {SIZE_GENERATORS, SIZE_BEDS, SIZE_WAYPOINTS, SIZE_NAMETAGS, SIZE_ARMOR,
				SIZE_PROJECTILES}) {
				Integer percent = values.get(slider.getName());
				if (percent != null) {
					slider.set(percent);
				}
			}
			if (!values.isEmpty()) {
				AxolotlClient.LOGGER.info("[Extras] Label sizes carried over to the sliders: " + values);
			}
		} catch (Exception e) {
			AxolotlClient.LOGGER.warn("[Extras] Could not carry the old label sizes over: " + e);
		}
	}

	@Override
	public void tick() {
		if (!labelSizesMigrated) {
			// The config is loaded by now (it is loaded after init), so the sizes picked with the old buttons can be carried over.
			labelSizesMigrated = true;
			carryOverLabelSizes();
		}
		if (rawMouseGuard && AxolotlClient.CONFIG.rawMouseInput.get()) {
			// AxolotlClient's own "Raw Mouse Input" is not raw input: it rescales the movement and cuts it to whole numbers,
			// so slow movements lose most of their distance while fast ones keep theirs (it feels like acceleration).
			AxolotlClient.CONFIG.rawMouseInput.set(false);
			AxolotlClient.LOGGER.info("[Extras] Turned off AxolotlClient's Raw Mouse Input option: the Raw Input mod does the real thing.");
		}
		STAT_ANNOUNCER.tick(client, ChatStat.byKey(CHAT_STAT.get()));
		BenchDriver.tick(client);
		FrameBench.onTick(client);
		HackRenderer.tick(client);
		if (hacksOffKey != null && hacksOffKey.wasPressed() && client.currentScreen == null) {
			int count = hackPanic.allOff(hackSwitches());
			saveConfig();
			emergencyNote("§cHacks off" + (count > 0 ? " §7(" + count + ")" : ""));
		}
		if (hacksOnKey != null && hacksOnKey.wasPressed() && client.currentScreen == null) {
			int count = hackPanic.restore(hackSwitches());
			saveConfig();
			emergencyNote(count < 0 ? "§7Hacks: nothing to turn back on" : "§aHacks back on §7(" + count + ")");
		}
		for (int i = 0; i < hackToggleKeys.length; i++) {
			if (hackToggleKeys[i].wasPressed() && client.currentScreen == null) {
				BooleanOption option = HACK_MAIN_SWITCHES[i];
				option.toggle();
				saveConfig();
				emergencyNote((option.get() ? "§a" : "§c") + I18n.translate(option.getName())
					+ (option.get() ? " on" : " off"));
			}
		}
		if (radarCenterKey != null && radarCenterKey.wasPressed() && client.currentScreen == null) {
			boolean toBed = !RADAR_CENTER.get().endsWith(".bed");
			RADAR_CENTER.set(toBed ? "axolotlextras.radarCenter.bed" : "axolotlextras.radarCenter.player");
			saveConfig();
			emergencyNote(toBed ? "§eRadar: centred on your bed" : "§eRadar: centred on you");
		}
		if (dynamicFovKey != null && dynamicFovKey.wasPressed() && client.currentScreen == null) {
			BooleanOption dynamicFov = AxolotlClient.CONFIG.dynamicFOV;
			dynamicFov.toggle();
			saveConfig();
			if (client.inGameHud != null) {
				client.inGameHud.setOverlayMessage(dynamicFov.get() ? "§aDynamic FOV on" : "§cDynamic FOV off", false);
			}
		}
		if (menuKey != null && menuKey.wasPressed() && client.currentScreen == null) {
			client.setScreen(new ExtrasScreen(null));
		}
		if (IGNORE_SERVER_BLOCKS.get() && anyServerBlock()) {
			// A server message (the "block_mods" channel) or an earlier address check switched something off.
			FeatureDisabler.clear();
		}
		if (TOGGLE_WITHOUT_HUD.get() && client.world != null) {
			ToggleSprintHud toggleSprint = (ToggleSprintHud) HudManager.getInstance().get(ToggleSprintHud.ID);
			if (toggleSprint != null && !toggleSprint.isEnabled()) {
				// HudManager only ticks enabled modules, and the toggle key is handled in that tick.
				toggleSprint.tick();
			}
		}
		if (quickplayKey != null && quickplayKey.wasPressed() && QUICKPLAY.get() && client.currentScreen == null
			&& client.world != null && client.player != null && quickplayAllowedHere()) {
			client.setScreen(new QuickplayScreen());
		}
	}

	/** The main switch of every hack (their fine-tuning switches stay as they are, they do nothing while the hack is off). */
	private static List<HackPanic.Switch> hackSwitches() {
		List<HackPanic.Switch> out = new ArrayList<>();
		for (BooleanOption option : HACK_MAIN_SWITCHES) {
			out.add(new HackPanic.Switch() {
				@Override
				public String name() {
					return option.getName();
				}

				@Override
				public boolean on() {
					return option.get();
				}

				@Override
				public void set(boolean on) {
					option.set(on);
				}
			});
		}
		return out;
	}

	/** A short note above the hotbar (it fades after a few seconds), unless the Hack Key Note switch is off. */
	private void emergencyNote(String text) {
		if (HACK_KEY_NOTE.get() && client.inGameHud != null) {
			client.inGameHud.setOverlayMessage(text, false);
		}
	}

	private static void saveConfig() {
		try {
			AxolotlClient.configManager.save();
		} catch (RuntimeException e) {
			AxolotlClient.LOGGER.error("[Extras] Could not save the config: " + e);
		}
	}

	private static boolean anyServerBlock() {
		ToggleSprintHud toggleSprint = (ToggleSprintHud) HudManager.getInstance().get(ToggleSprintHud.ID);
		return Freelook.getInstance().enabled.getForceDisabled()
			|| AxolotlClient.CONFIG.timeChangerEnabled.getForceDisabled()
			|| AxolotlClient.CONFIG.fullBright.getForceDisabled()
			|| AxolotlClient.CONFIG.lowFire.getForceDisabled()
			|| (toggleSprint != null && toggleSprint.toggleSneak.getForceDisabled());
	}

	private boolean quickplayAllowedHere() {
		if (!QUICKPLAY_HYPIXEL_ONLY.get()) {
			return true;
		}
		ServerInfo info = client.getCurrentServerEntry();
		return info != null && info.address != null && info.address.toLowerCase().contains("hypixel.net");
	}
}
