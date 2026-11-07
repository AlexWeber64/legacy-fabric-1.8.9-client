package io.github.alexweber64.axolotlextras.hacks;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import io.github.axolotlclient.AxolotlClient;

/**
 * Finds out what causes freezes. Times each hack step, writes any step slower than 50 ms to latest.log, and when the game
 * stops ticking for more than a second, writes how long, how much of it was Java's garbage collection, and which hack step
 * was the slowest meanwhile. Local log lines only.
 */
final class FreezeWatch {

	/** A single hack step this slow is written to the log. */
	private static final long SLOW_STEP_NS = 50_000_000L;

	/** A gap between two ticks this long counts as a freeze (a tick is normally 50 ms). */
	private static final long FREEZE_MS = 1000;

	/** The same slow step is written at most this often. */
	private static final long LOG_EVERY_MS = 10_000;

	private final Map<String, Long> lastLogged = new HashMap<>();
	private long lastTick;
	private long gcAtLastTick;
	private String slowest = "none";
	private long slowestNs;
	private long totalNs;

	/** @return the start time for {@link #end} */
	long start() {
		return System.nanoTime();
	}

	void end(String step, long start) {
		long ns = System.nanoTime() - start;
		totalNs += ns;
		if (ns > slowestNs) {
			slowestNs = ns;
			slowest = step;
		}
		if (ns > SLOW_STEP_NS) {
			long now = System.currentTimeMillis();
			Long last = lastLogged.get(step);
			if (last == null || now - last >= LOG_EVERY_MS) {
				lastLogged.put(step, now);
				AxolotlClient.LOGGER.warn("[Extras] Hacks: " + step + " took " + ns / 1_000_000 + " ms");
			}
		}
	}

	/** Called once per game tick. */
	void tick() {
		long now = System.currentTimeMillis();
		long gc = gcMillis();
		if (lastTick != 0 && now - lastTick > FREEZE_MS) {
			AxolotlClient.LOGGER.warn("[Extras] Freeze: no game tick for " + (now - lastTick) + " ms. Garbage collection: "
				+ (gc - gcAtLastTick) + " ms. All hacks together: " + totalNs / 1_000_000 + " ms, the slowest step: "
				+ slowest + " (" + slowestNs / 1_000_000 + " ms).");
		}
		lastTick = now;
		gcAtLastTick = gc;
		slowest = "none";
		slowestNs = 0;
		totalNs = 0;
	}

	/** Forget the last tick (a new world: loading it is not a freeze worth reporting). */
	void reset() {
		lastTick = 0;
	}

	private static long gcMillis() {
		long total = 0;
		for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
			long time = bean.getCollectionTime();
			if (time > 0) {
				total += time;
			}
		}
		return total;
	}
}
