package io.github.alexweber64.axolotlextras.hacks;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import io.github.axolotlclient.AxolotlClient;

/**
 * Finds out what causes freezes. Times each hack step, writes any step slower than 50 ms to latest.log, and when the game
 * stops ticking for more than a second, writes how long, how much of it was Java's garbage collection, and which hack step
 * was the slowest meanwhile. While a freeze is going on, a small watcher thread writes where the game thread is stuck (its
 * stack), twice, so the cause shows even when it is not in the hacks. Local log lines only.
 */
final class FreezeWatch {

	/** A single hack step this slow is written to the log. */
	private static final long SLOW_STEP_NS = 50_000_000L;

	/** A gap between two ticks this long counts as a freeze (a tick is normally 50 ms). */
	private static final long FREEZE_MS = 1000;

	/** The same slow step is written at most this often. */
	private static final long LOG_EVERY_MS = 10_000;

	/** How long into a freeze the watcher looks at the game thread (twice, to see whether it is stuck in one place). */
	private static final long[] SNAPSHOT_AT_MS = {600, 900};

	/** Stack lines written per snapshot. */
	private static final int STACK_LINES = 30;

	private final Map<String, Long> lastLogged = new HashMap<>();
	private long lastTick;
	private long gcAtLastTick;
	private String slowest = "none";
	private long slowestNs;
	private long totalNs;

	/** Read by the watcher thread: when the game last ticked (0 while not in a world), and the game thread. */
	private volatile long lastTickNanos;
	private volatile Thread gameThread;
	private boolean watcherStarted;

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
		if (!watcherStarted) {
			watcherStarted = true;
			gameThread = Thread.currentThread();
			Thread watcher = new Thread(this::watch, "Extras freeze watcher");
			watcher.setDaemon(true);
			watcher.setPriority(Thread.MIN_PRIORITY);
			watcher.start();
		}
		long now = System.currentTimeMillis();
		long gc = gcMillis();
		if (lastTick != 0 && now - lastTick > FREEZE_MS) {
			AxolotlClient.LOGGER.warn("[Extras] Freeze: no game tick for " + (now - lastTick) + " ms. Garbage collection: "
				+ (gc - gcAtLastTick) + " ms. All hacks together: " + totalNs / 1_000_000 + " ms, the slowest step: "
				+ slowest + " (" + slowestNs / 1_000_000 + " ms).");
		}
		lastTick = now;
		lastTickNanos = System.nanoTime();
		gcAtLastTick = gc;
		slowest = "none";
		slowestNs = 0;
		totalNs = 0;
	}

	/** Forget the last tick (a new world: loading it is not a freeze worth reporting). */
	void reset() {
		lastTick = 0;
		lastTickNanos = 0;
	}

	/** The watcher thread: ten times a second, checks whether the game thread has stopped ticking. */
	private void watch() {
		long stall = 0;
		int snapshots = 0;
		while (true) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return;
			}
			long last = lastTickNanos;
			Thread game = gameThread;
			if (last == 0 || game == null) {
				continue;
			}
			long gapMs = (System.nanoTime() - last) / 1_000_000;
			if (last != stall) {
				stall = last;
				snapshots = 0;
			}
			if (snapshots < SNAPSHOT_AT_MS.length && gapMs >= SNAPSHOT_AT_MS[snapshots]) {
				snapshots++;
				StringBuilder out = new StringBuilder("[Extras] Freeze going on (" + gapMs + " ms so far), the game thread is at:");
				StackTraceElement[] stack = game.getStackTrace();
				for (int i = 0; i < stack.length && i < STACK_LINES; i++) {
					out.append("\n\tat ").append(stack[i]);
				}
				AxolotlClient.LOGGER.warn(out.toString());
			}
		}
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
