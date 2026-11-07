package io.github.alexweber64.axolotlextras.perf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.bedwars.StatsLoader;
import io.github.axolotlclient.AxolotlClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.ScreenshotUtils;

/**
 * Developer tool for testing: runs a list of steps from a file, so a test needs no typing and no clicking.
 * If config/axolotl-extras-bench/run.txt exists it is read and deleted, and its lines are carried out one at a time, a step
 * every second once the game has been in a world for a few seconds. Lines:
 * <pre>
 * world FolderName            opens that singleplayer world from the title screen
 * cmd /give @p diamond_sword  sends the command (only /kill, /gamerule, /time, /tp, /give, /gamemode, /clear, /summon)
 * bench label seconds [particles]   records frame times, with the movement scene when particles is given
 * slot 0..8                   selects a hotbar slot
 * look yaw pitch              turns the player
 * perspective 0..2            first person, third person back, third person front
 * hold use|attack|sneak|forward seconds   holds that key for the time (the next steps carry on meanwhile)
 * swingblock seconds name ticks   one swing, then blocking at once, and a screenshot that many ticks later
 * wait seconds                waits before the next step
 * option name on|off          switches one of the 1.7 visuals options (oldBlockHitting, oldFirstPerson, oldSneak, oldThirdPerson)
 * statstest                 looks up 2 real and 16 made-up player ids with the stats loader and logs how long it took
 * eyelog seconds             logs the real and the shown eye height every tick for that long
 * shot name                   saves a screenshot named name.png in the game's screenshots folder
 * done                        writes "[Extras] BENCH queue finished" to the log
 * quit                        closes the game
 * </pre>
 * It only works in singleplayer and reads only this one local file. Nothing is sent anywhere.
 */
public final class BenchDriver {

	private enum Hold { USE, ATTACK, SNEAK, FORWARD }

	private static final Deque<String> QUEUE = new ArrayDeque<>();
	private static final Map<Hold, Integer> HOLDS = new EnumMap<>(Hold.class);
	private static int ticks;
	private static int worldTicks;
	private static int waitTicks;
	private static int eyeLogTicks;
	private static List<String> statsTestIds;
	private static long statsTestStart;
	private static int pendingShotTicks;
	private static String pendingShotName;

	private BenchDriver() {
	}

	public static void tick(MinecraftClient client) {
		ticks++;
		if (client.world != null && client.player != null) {
			worldTicks++;
		} else {
			worldTicks = 0;
		}
		tickHolds(client);
		if (statsTestIds != null) {
			int ready = 0;
			int unknown = 0;
			for (String id : statsTestIds) {
				StatsLoader.Entry entry = StatsLoader.lookup(id);
				if (entry.state == StatsLoader.State.READY) {
					ready++;
				} else if (entry.state == StatsLoader.State.UNKNOWN) {
					unknown++;
				}
			}
			long elapsed = System.currentTimeMillis() - statsTestStart;
			if (ready + unknown == statsTestIds.size() || elapsed > 60_000) {
				AxolotlClient.LOGGER.info("[Extras] BENCH statstest: " + ready + " ready, " + unknown + " unknown of "
					+ statsTestIds.size() + " after " + elapsed + " ms");
				statsTestIds = null;
			}
		}
		if (eyeLogTicks > 0 && client.player != null && client.gameRenderer instanceof EyeProbe) {
			eyeLogTicks--;
			AxolotlClient.LOGGER.info(String.format(Locale.ROOT, "[Extras] BENCH eye sneaking=%b real=%.3f shown=%.3f", client.player.isSneaking(),
				client.player.getEyeHeight(), ((EyeProbe) client.gameRenderer).axtras$shownEyeHeight()));
		}
		if (pendingShotTicks > 0 && --pendingShotTicks == 0) {
			ScreenshotUtils.saveScreenshot(client.runDirectory, pendingShotName + ".png", client.width, client.height,
				client.getFramebuffer());
			AxolotlClient.LOGGER.info("[Extras] BENCH driver: saved screenshot " + pendingShotName);
		}
		if (waitTicks > 0) {
			waitTicks--;
		}
		if (ticks % 20 != 0) {
			return;
		}
		readFile();
		if (QUEUE.isEmpty() || FrameBench.isRunning() || waitTicks > 0) {
			return;
		}
		String line = QUEUE.peek();
		String[] parts = line.split(" ", 2);
		if (parts[0].equals("world")) {
			QUEUE.poll();
			if (client.world == null && client.currentScreen instanceof TitleScreen && parts.length == 2) {
				String folder = parts[1].trim();
				AxolotlClient.LOGGER.info("[Extras] BENCH driver: opening world " + folder);
				client.startIntegratedServer(folder, folder, null);
			}
			return;
		}
		if (client.world == null || client.player == null || client.currentScreen != null || worldTicks < 100) {
			return;
		}
		QUEUE.poll();
		String arg = parts.length == 2 ? parts[1].trim() : "";
		switch (parts[0]) {
			case "cmd":
				if (arg.matches("/(kill|gamerule|time|tp|give|gamemode|clear|summon|replaceitem) .{0,80}")) {
					client.player.sendChatMessage(arg);
				}
				break;
			case "bench":
				bench(arg);
				break;
			case "slot":
				if (arg.matches("[0-8]")) {
					client.player.inventory.selectedSlot = Integer.parseInt(arg);
				}
				break;
			case "look":
				look(client, arg);
				break;
			case "perspective":
				if (arg.matches("[0-2]")) {
					client.options.perspective = Integer.parseInt(arg);
				}
				break;
			case "hold":
				hold(arg);
				break;
			case "wait":
				if (arg.matches("[0-9]{1,3}")) {
					waitTicks = Integer.parseInt(arg) * 20;
				}
				break;
			case "option":
				option(arg);
				break;
			case "swingblock":
				swingBlock(client, arg);
				break;
			case "statstest":
				// two real accounts (Notch and the Hypixel account) and sixteen random ids that no player has
				statsTestIds = new java.util.ArrayList<>();
				statsTestIds.add("069a79f444e94726a5befca90e38aaf5");
				statsTestIds.add("f7c77d999f154a66a87dc4a51ef30d19");
				java.util.Random random = new java.util.Random(7);
				for (int i = 0; i < 16; i++) {
					statsTestIds.add(String.format("%016x%016x", random.nextLong(), random.nextLong()));
				}
				statsTestStart = System.currentTimeMillis();
				break;
			case "eyelog":
				if (arg.matches("[0-9]{1,3}")) {
					eyeLogTicks = Integer.parseInt(arg) * 20;
				}
				break;
			case "shot":
				if (arg.matches("[A-Za-z0-9_.-]{1,40}")) {
					ScreenshotUtils.saveScreenshot(client.runDirectory, arg + ".png", client.width, client.height,
						client.getFramebuffer());
					AxolotlClient.LOGGER.info("[Extras] BENCH driver: saved screenshot " + arg);
				}
				break;
			case "done":
				AxolotlClient.LOGGER.info("[Extras] BENCH queue finished");
				break;
			case "quit":
				FrameBench.clearScene();
				client.scheduleStop();
				break;
			default:
				AxolotlClient.LOGGER.warn("[Extras] BENCH driver: ignored line: " + line);
		}
	}

	private static void bench(String arg) {
		String[] a = arg.split(" ");
		if (a.length >= 2 && a[0].matches("[A-Za-z0-9_.-]{1,40}") && a[1].matches("[0-9]{1,4}")) {
			if (a.length == 3 && a[2].matches("[0-9]{1,3}")) {
				FrameBench.start(a[0], Integer.parseInt(a[1]), Integer.parseInt(a[2]));
			} else {
				FrameBench.start(a[0], Integer.parseInt(a[1]));
			}
		}
	}

	private static void look(MinecraftClient client, String arg) {
		String[] a = arg.split(" ");
		try {
			if (a.length == 2) {
				float yaw = Float.parseFloat(a[0]);
				float pitch = Float.parseFloat(a[1]);
				client.player.yaw = yaw;
				client.player.prevYaw = yaw;
				client.player.pitch = pitch;
				client.player.prevPitch = pitch;
			}
		} catch (NumberFormatException ignored) {
			AxolotlClient.LOGGER.warn("[Extras] BENCH driver: bad look line: " + arg);
		}
	}

	/** swingblock seconds name ticks: one swing, then blocking at once, and a screenshot that many ticks later. */
	private static void swingBlock(MinecraftClient client, String arg) {
		String[] a = arg.split(" ");
		if (a.length == 3 && a[0].matches("[0-9]{1,2}") && a[1].matches("[A-Za-z0-9_.-]{1,40}") && a[2].matches("[0-9]{1,2}")) {
			KeyBinding.onKeyPressed(client.options.attackKey.getCode());
			HOLDS.put(Hold.USE, Integer.parseInt(a[0]) * 20);
			pendingShotName = a[1];
			pendingShotTicks = Integer.parseInt(a[2]) + 1;
		}
	}

	private static void hold(String arg) {
		String[] a = arg.split(" ");
		if (a.length == 2 && a[1].matches("[0-9]{1,3}")) {
			for (Hold h : Hold.values()) {
				if (h.name().toLowerCase(Locale.ROOT).equals(a[0])) {
					HOLDS.put(h, Integer.parseInt(a[1]) * 20);
				}
			}
		}
	}

	private static void option(String arg) {
		String[] a = arg.split(" ");
		if (a.length != 2 || !(a[1].equals("on") || a[1].equals("off"))) {
			return;
		}
		boolean on = a[1].equals("on");
		switch (a[0]) {
			case "oldBlockHitting":
				ExtrasModule.OLD_BLOCK_HITTING.set(on);
				break;
			case "oldFirstPerson":
				ExtrasModule.OLD_FIRST_PERSON.set(on);
				break;
			case "oldSneak":
				ExtrasModule.OLD_SNEAK.set(on);
				break;
			case "oldThirdPerson":
				ExtrasModule.OLD_THIRD_PERSON.set(on);
				break;
			default:
				AxolotlClient.LOGGER.warn("[Extras] BENCH driver: unknown option " + a[0]);
		}
	}

	private static KeyBinding key(MinecraftClient client, Hold hold) {
		switch (hold) {
			case USE:
				return client.options.useKey;
			case ATTACK:
				return client.options.attackKey;
			case SNEAK:
				return client.options.sneakKey;
			default:
				return client.options.forwardKey;
		}
	}

	private static void tickHolds(MinecraftClient client) {
		for (Hold h : Hold.values()) {
			Integer left = HOLDS.get(h);
			if (left == null) {
				continue;
			}
			boolean on = left > 0;
			if (h == Hold.SNEAK || h == Hold.FORWARD) {
				// AxolotlClient reads the real keyboard for these, so the input is wrapped instead
				ForcedInput.install(client);
				if (h == Hold.SNEAK) {
					ForcedInput.sneak = on;
				} else {
					ForcedInput.forward = on;
				}
			} else {
				int code = key(client, h).getCode();
				KeyBinding.setKeyPressed(code, on);
				if (h == Hold.ATTACK && on && left % 10 == 0) {
					// a swing starts from a key press event, not from the key being held
					KeyBinding.onKeyPressed(code);
				}
			}
			if (on) {
				HOLDS.put(h, left - 1);
			} else {
				HOLDS.remove(h);
			}
		}
	}

	private static void readFile() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve("axolotl-extras-bench").resolve("run.txt");
		if (!Files.isRegularFile(file)) {
			return;
		}
		try {
			List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
			Files.delete(file);
			for (String line : lines) {
				String trimmed = line.trim();
				if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
					QUEUE.add(trimmed);
				}
			}
			AxolotlClient.LOGGER.info(String.format(Locale.ROOT, "[Extras] BENCH driver: %d steps queued", QUEUE.size()));
		} catch (IOException e) {
			AxolotlClient.LOGGER.error("[Extras] BENCH driver could not read run.txt: " + e);
		}
	}
}
