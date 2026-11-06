package io.github.alexweber64.axolotlextras.perf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import io.github.axolotlclient.AxolotlClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;

/**
 * Developer tool for the frame recorder: runs a list of steps from a file, so a test needs no typing and no clicking.
 * If config/axolotl-extras-bench/run.txt exists it is read and deleted, and its lines are carried out one at a time, a step
 * every second once the game has been in a world for a few seconds. Lines:
 * <pre>
 * world FolderName            opens that singleplayer world from the title screen
 * cmd /kill @e[type=!Player]  sends the command (only /kill, /gamerule, /time and /tp are accepted)
 * bench label seconds [particles]   records frame times, with the movement scene when particles is given
 * done                        writes "[Extras] BENCH queue finished" to the log
 * quit                        closes the game
 * </pre>
 * It only works in singleplayer and reads only this one local file. Nothing is sent anywhere.
 */
public final class BenchDriver {

	private static final Deque<String> QUEUE = new ArrayDeque<>();
	private static int ticks;
	private static int worldTicks;

	private BenchDriver() {
	}

	public static void tick(MinecraftClient client) {
		ticks++;
		if (ticks % 20 != 0) {
			if (client.world != null && client.player != null) {
				worldTicks++;
			} else {
				worldTicks = 0;
			}
			return;
		}
		readFile();
		if (QUEUE.isEmpty() || FrameBench.isRunning()) {
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
		switch (parts[0]) {
			case "cmd":
				if (parts.length == 2 && parts[1].matches("/(kill|gamerule|time|tp) .{0,80}")) {
					client.player.sendChatMessage(parts[1]);
				}
				break;
			case "bench":
				if (parts.length == 2) {
					String[] a = parts[1].split(" ");
					if (a.length >= 2 && a[0].matches("[A-Za-z0-9_.-]{1,40}") && a[1].matches("[0-9]{1,4}")) {
						if (a.length == 3 && a[2].matches("[0-9]{1,3}")) {
							FrameBench.start(a[0], Integer.parseInt(a[1]), Integer.parseInt(a[2]));
						} else {
							FrameBench.start(a[0], Integer.parseInt(a[1]));
						}
					}
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
