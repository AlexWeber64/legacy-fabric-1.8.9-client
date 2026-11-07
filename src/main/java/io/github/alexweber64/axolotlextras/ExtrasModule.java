package io.github.alexweber64.axolotlextras;

import io.github.alexweber64.axolotlextras.bedwars.ChatStat;
import io.github.alexweber64.axolotlextras.bedwars.ResourceCounterHud;
import io.github.alexweber64.axolotlextras.bedwars.StatAnnouncer;
import io.github.alexweber64.axolotlextras.hacks.HackRenderer;
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

	/** Boxes around other players, through walls, in their team colour. Invisible players are included. */
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

	private static KeyBinding quickplayKey;
	private static KeyBinding menuKey;
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
		HACKS.add(PLAYER_ESP, ESP_TRACERS, NAMETAGS, ENEMY_ARMOR, GENERATOR_COUNTS, BED_DEFENSE, OBSIDIAN_ALERT,
			OBSIDIAN_WAYPOINTS, RUSH_ALERT, RUSH_DISTANCE, FIREBALL_WARNING, PEARL_LANDING, TRAJECTORIES, NO_BLINDNESS);
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

		menuKey = new KeyBinding("key.axolotlextras.menu", Keyboard.KEY_F8, "category.axolotlclient");
		KeyBindingHelper.registerKeyBinding(menuKey);

		quickplayKey = new KeyBinding("key.axolotlextras.quickplay", Keyboard.KEY_Y, "category.axolotlclient");
		KeyBindingHelper.registerKeyBinding(quickplayKey);

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

	@Override
	public void tick() {
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
