package io.github.alexweber64.axolotlextras.bedwars;

/** How long to wait before asking Hypixel again after a failed request. Pure logic. */
public final class StatsBackoff {

	/** Requests per player before the loader gives up for a while. */
	public static final int MAX_ATTEMPTS = 4;

	private StatsBackoff() {
	}

	/** @param attempt the number of requests already made (1 for the first failure): 500 ms, 1 s, 2 s, then 4 s */
	public static long delayMillis(int attempt) {
		int step = Math.max(0, Math.min(attempt - 1, 3));
		return 500L << step;
	}

	/**
	 * @param retryAfterSeconds the wait Hypixel asked for (the Retry-After or RateLimit-Reset header), or a negative number if none
	 * @return the pause for all requests after a 429, at least one second and at most five minutes
	 */
	public static long rateLimitPauseMillis(int retryAfterSeconds) {
		int seconds = retryAfterSeconds < 0 ? 10 : retryAfterSeconds;
		return Math.max(1, Math.min(seconds, 300)) * 1000L;
	}
}
