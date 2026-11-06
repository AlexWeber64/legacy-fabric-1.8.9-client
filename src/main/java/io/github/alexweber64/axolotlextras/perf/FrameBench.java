package io.github.alexweber64.axolotlextras.perf;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Locale;

import io.github.axolotlclient.AxolotlClient;
import net.fabricmc.loader.api.FabricLoader;

/**
 * A small frame time recorder for measuring changes, used from a singleplayer world only. A chat line of the form
 * "AXBENCH label seconds" starts it (the test scripts send that with /tellraw), and when the time is up it writes the
 * summary to the log and to config/axolotl-extras-bench/results.txt, and every frame time to a csv file. Nothing is
 * sent anywhere.
 */
public final class FrameBench {

	private static boolean running;
	private static String label = "";
	private static long lastNs;
	private static long endNs;
	private static double[] ms = new double[1 << 16];
	private static int count;
	private static long gcCountStart;
	private static long gcMillisStart;

	private FrameBench() {
	}

	public static void start(String name, int seconds) {
		label = name.replaceAll("[^A-Za-z0-9_.-]", "_");
		count = 0;
		lastNs = 0;
		endNs = System.nanoTime() + seconds * 1_000_000_000L;
		gcCountStart = gcCount();
		gcMillisStart = gcMillis();
		running = true;
		AxolotlClient.LOGGER.info("[Extras] BENCH started: " + label + " for " + seconds + " s");
	}

	/** Called once per frame, at the start of the game renderer. */
	public static void onFrame() {
		if (!running) {
			return;
		}
		long now = System.nanoTime();
		if (lastNs != 0) {
			if (count == ms.length) {
				ms = Arrays.copyOf(ms, ms.length * 2);
			}
			ms[count++] = (now - lastNs) / 1_000_000.0;
		}
		lastNs = now;
		if (now >= endNs) {
			running = false;
			finish();
		}
	}

	private static void finish() {
		BenchStats stats = BenchStats.compute(ms, count);
		String line = String.format(Locale.ROOT, "%s | %s | gc runs=%d gc ms=%d", label, stats,
			gcCount() - gcCountStart, gcMillis() - gcMillisStart);
		AxolotlClient.LOGGER.info("[Extras] BENCH " + line);
		try {
			Path dir = FabricLoader.getInstance().getConfigDir().resolve("axolotl-extras-bench");
			Files.createDirectories(dir);
			Files.write(dir.resolve("results.txt"), (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			StringBuilder csv = new StringBuilder("ms\n");
			for (int i = 0; i < count; i++) {
				csv.append(String.format(Locale.ROOT, "%.3f%n", ms[i]));
			}
			Files.write(dir.resolve(label + ".csv"), csv.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			AxolotlClient.LOGGER.error("[Extras] BENCH could not write results: " + e);
		}
	}

	private static long gcCount() {
		long total = 0;
		for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
			total += Math.max(0, bean.getCollectionCount());
		}
		return total;
	}

	private static long gcMillis() {
		long total = 0;
		for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
			total += Math.max(0, bean.getCollectionTime());
		}
		return total;
	}
}
