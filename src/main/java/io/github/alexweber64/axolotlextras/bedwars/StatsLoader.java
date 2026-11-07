package io.github.alexweber64.axolotlextras.bedwars;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.axolotlclient.AxolotlClient;
import io.github.axolotlclient.modules.hypixel.HypixelMods;

/**
 * Loads Bed Wars stats from Hypixel's public API in the background, a few players at the same time, so they are ready a moment
 * after a game starts. AxolotlClient's own loading asks once, gives up after a microsecond, caps itself at 56 lookups a minute and
 * remembers a failed request for good, which left most players without stats. This asks again after failures, pauses when Hypixel
 * says it is being asked too much, and never makes numbers up: a player is either READY (real numbers), UNKNOWN (nicked, or no
 * valid key) or still LOADING.
 *
 * <p>Only the UUIDs of players in the current Bed Wars game are sent, and only to api.hypixel.net, with the API key from the
 * AxolotlClient settings. Nothing is logged that identifies a player.
 */
public final class StatsLoader {

	public enum State { LOADING, READY, UNKNOWN, FAILED }

	/** The result for one player. The fields are read from the game thread while a loader thread fills them in. */
	public static final class Entry {
		public volatile State state = State.LOADING;
		/** The 14 numbers for AxolotlClient's BedwarsPlayerStats, when READY. */
		public volatile int[] values;
		volatile long updated;
		volatile long retryAt;
		volatile int totalAttempts;
		final AtomicBoolean busy = new AtomicBoolean();
	}

	private static final String URL_BASE = "https://api.hypixel.net/v2/player?uuid=";
	/** Stats are fetched again in the background after this long. */
	private static final long REFRESH_MILLIS = 10 * 60 * 1000L;
	private static final long FAILED_RETRY_MILLIS = 20_000L;
	private static final int MAX_TOTAL_ATTEMPTS = 16;
	private static final int THREADS = 4;

	private static final ConcurrentHashMap<String, Entry> ENTRIES = new ConcurrentHashMap<>();
	private static final ExecutorService POOL = Executors.newFixedThreadPool(THREADS, new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "axolotl-extras-stats");
			t.setDaemon(true);
			return t;
		}
	});

	private static volatile long pausedUntil;
	private static volatile boolean keyProblemLogged;
	private static volatile boolean pauseLogged;

	private StatsLoader() {
	}

	/**
	 * Looks a player up, starting the request when nothing is known yet. Cheap to call often.
	 *
	 * @param uuid the player's UUID without dashes
	 */
	public static Entry lookup(String uuid) {
		long now = System.currentTimeMillis();
		Entry entry = ENTRIES.get(uuid);
		if (entry == null) {
			entry = new Entry();
			Entry previous = ENTRIES.putIfAbsent(uuid, entry);
			if (previous != null) {
				return previous;
			}
			submit(uuid, entry);
		} else if (entry.state == State.FAILED && now >= entry.retryAt && entry.totalAttempts < MAX_TOTAL_ATTEMPTS) {
			submit(uuid, entry);
		} else if (entry.state == State.READY && now - entry.updated > REFRESH_MILLIS) {
			submit(uuid, entry);
		}
		return entry;
	}

	/** @return true when nothing more will change for this player soon: real numbers, or known to have none */
	public static boolean isSettled(String uuid) {
		Entry entry = ENTRIES.get(uuid);
		return entry != null && (entry.state == State.READY || entry.state == State.UNKNOWN);
	}

	private static void submit(final String uuid, final Entry entry) {
		if (!entry.busy.compareAndSet(false, true)) {
			return;
		}
		POOL.execute(new Runnable() {
			@Override
			public void run() {
				try {
					load(uuid, entry);
				} catch (RuntimeException e) {
					entry.state = entry.values != null ? State.READY : State.FAILED;
					entry.retryAt = System.currentTimeMillis() + FAILED_RETRY_MILLIS;
				} finally {
					entry.busy.set(false);
				}
			}
		});
	}

	private static void load(String uuid, Entry entry) {
		String key = key();
		if (key == null || key.trim().isEmpty()) {
			logKeyProblem("no Hypixel API key is set");
			entry.state = State.UNKNOWN;
			return;
		}
		long started = System.currentTimeMillis();
		int attempt = 0;
		while (attempt < StatsBackoff.MAX_ATTEMPTS && System.currentTimeMillis() - started < 3 * 60 * 1000L) {
			waitForPause();
			Outcome outcome = request(uuid, key.trim());
			if (outcome.kind == Kind.OK) {
				entry.values = outcome.values;
				entry.updated = System.currentTimeMillis();
				entry.state = State.READY;
				return;
			}
			if (outcome.kind == Kind.NO_PLAYER) {
				if (entry.values == null) {
					entry.state = State.UNKNOWN;
				}
				return;
			}
			if (outcome.kind == Kind.BAD_KEY) {
				logKeyProblem("Hypixel did not accept the API key");
				if (entry.values == null) {
					entry.state = State.UNKNOWN;
				}
				return;
			}
			if (outcome.kind == Kind.RATE_LIMITED) {
				// the pause is set; this does not count as a failed attempt
				continue;
			}
			attempt++;
			entry.totalAttempts++;
			sleep(StatsBackoff.delayMillis(attempt));
		}
		if (entry.values == null) {
			entry.state = State.FAILED;
		}
		entry.retryAt = System.currentTimeMillis() + FAILED_RETRY_MILLIS;
	}

	private enum Kind { OK, NO_PLAYER, BAD_KEY, RATE_LIMITED, ERROR }

	private static final class Outcome {
		final Kind kind;
		final int[] values;

		Outcome(Kind kind, int[] values) {
			this.kind = kind;
			this.values = values;
		}
	}

	private static Outcome request(String uuid, String key) {
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) new URL(URL_BASE + uuid).openConnection();
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(8000);
			connection.setRequestProperty("API-Key", key);
			connection.setRequestProperty("User-Agent", "axolotl-extras");
			int code = connection.getResponseCode();
			if (code == 429) {
				int reset = headerInt(connection, "Retry-After", headerInt(connection, "RateLimit-Reset", -1));
				pause(StatsBackoff.rateLimitPauseMillis(reset));
				return new Outcome(Kind.RATE_LIMITED, null);
			}
			if (code == 401 || code == 403) {
				return new Outcome(Kind.BAD_KEY, null);
			}
			if (code != 200) {
				return new Outcome(Kind.ERROR, null);
			}
			int remaining = headerInt(connection, "RateLimit-Remaining", -1);
			if (remaining >= 0 && remaining <= 2) {
				// nearly out of requests in this window: wait for it to reset instead of getting refused
				pause(StatsBackoff.rateLimitPauseMillis(headerInt(connection, "RateLimit-Reset", -1)));
			}
			return parse(read(connection.getInputStream()));
		} catch (IOException | RuntimeException e) {
			return new Outcome(Kind.ERROR, null);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private static Outcome parse(String body) {
		JsonObject root = new JsonParser().parse(body).getAsJsonObject();
		if (!root.has("success") || !root.get("success").getAsBoolean()) {
			return new Outcome(Kind.ERROR, null);
		}
		JsonElement player = root.get("player");
		if (player == null || !player.isJsonObject()) {
			return new Outcome(Kind.NO_PLAYER, null);
		}
		JsonObject playerObject = player.getAsJsonObject();
		Map<String, Integer> bedwars = new HashMap<>();
		JsonObject stats = object(playerObject, "stats");
		JsonObject bw = stats == null ? null : object(stats, "Bedwars");
		if (bw != null) {
			for (String name : new String[] {"final_kills_bedwars", "final_deaths_bedwars", "beds_broken_bedwars", "deaths_bedwars",
				"kills_bedwars", "losses_bedwars", "wins_bedwars", "winstreak"}) {
				if (bw.has(name) && bw.get(name).isJsonPrimitive()) {
					bedwars.put(name, bw.get(name).getAsInt());
				}
			}
		}
		Integer stars = null;
		JsonObject achievements = object(playerObject, "achievements");
		if (achievements != null && achievements.has("bedwars_level") && achievements.get("bedwars_level").isJsonPrimitive()) {
			stars = achievements.get("bedwars_level").getAsInt();
		}
		return new Outcome(Kind.OK, StatsMapper.constructorArgs(bedwars, stars));
	}

	private static JsonObject object(JsonObject parent, String name) {
		JsonElement element = parent.get(name);
		return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
	}

	private static String read(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int n;
		while ((n = in.read(buffer)) > 0) {
			out.write(buffer, 0, n);
		}
		return new String(out.toByteArray(), StandardCharsets.UTF_8);
	}

	private static int headerInt(HttpURLConnection connection, String name, int fallback) {
		String value = connection.getHeaderField(name);
		if (value == null) {
			return fallback;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static String key() {
		try {
			return HypixelMods.getInstance().hypixel_api_key.get();
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static void pause(long millis) {
		long until = System.currentTimeMillis() + millis;
		if (until > pausedUntil) {
			pausedUntil = until;
		}
		if (!pauseLogged) {
			pauseLogged = true;
			AxolotlClient.LOGGER.warn("[Extras] Hypixel asked to slow down: stats lookups pause for " + millis / 1000 + " s");
		}
	}

	private static void waitForPause() {
		long wait = pausedUntil - System.currentTimeMillis();
		if (wait > 0) {
			sleep(wait);
		}
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static void logKeyProblem(String what) {
		if (!keyProblemLogged) {
			keyProblemLogged = true;
			AxolotlClient.LOGGER.warn("[Extras] Bed Wars stats cannot load: " + what + ". Players show \"?\".");
		}
	}
}
