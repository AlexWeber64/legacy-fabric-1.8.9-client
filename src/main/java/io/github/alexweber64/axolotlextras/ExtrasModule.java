package io.github.alexweber64.axolotlextras;

import io.github.alexweber64.axolotlextras.bedwars.ResourceCounterHud;
import io.github.alexweber64.axolotlextras.bedwars.ResourceMath;
import io.github.alexweber64.axolotlextras.quickplay.QuickplayScreen;
import io.github.axolotlclient.AxolotlClient;
import io.github.axolotlclient.AxolotlClientConfig.options.BooleanOption;
import io.github.axolotlclient.AxolotlClientConfig.options.OptionCategory;
import io.github.axolotlclient.modules.AbstractModule;
import io.github.axolotlclient.modules.freelook.Freelook;
import io.github.axolotlclient.modules.hud.HudManager;
import io.github.axolotlclient.modules.hud.gui.hud.simple.ToggleSprintHud;
import io.github.axolotlclient.util.FeatureDisabler;
import io.github.axolotlclient.util.events.Events;
import net.legacyfabric.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
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

	private static KeyBinding quickplayKey;
	private static ResourceCounterHud resourceCounter;

	@Override
	public void init() {
		CATEGORY.add(IGNORE_SERVER_BLOCKS, TOGGLE_WITHOUT_HUD, NO_FOG, KEEP_INVENTORY_CENTERED, TRANSPARENT_UI, QUICKPLAY,
			QUICKPLAY_HYPIXEL_ONLY);
		AxolotlClient.CONFIG.addCategory(CATEGORY);

		// HudManager.init() has already run by now (modules are initialised in order, external ones last), so the HUD
		// category exists. A newly added entry has to be initialised by hand.
		resourceCounter = new ResourceCounterHud();
		HudManager.getInstance().add(resourceCounter);
		resourceCounter.init();
		resourceCounter.onBoundsUpdate();

		quickplayKey = new KeyBinding("key.axolotlextras.quickplay", Keyboard.KEY_Y, "category.axolotlclient");
		KeyBindingHelper.registerKeyBinding(quickplayKey);

		// Loading a different world (including joining another server or game) forgets the remembered counts.
		Events.WORLD_LOAD_EVENT.register(event -> resourceCounter.resetAll());

		Events.RECEIVE_CHAT_MESSAGE_EVENT.register(event -> {
			String line = event.getFormattedMessage().asUnformattedString();
			if (ResourceMath.isGameStartMessage(line)) {
				resourceCounter.onGameStart();
			}
		});
	}

	@Override
	public void tick() {
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
