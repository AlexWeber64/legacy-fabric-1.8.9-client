package io.github.alexweber64.axolotlextras.bedwars;

/**
 * When to look again for a player's real stats. AxolotlClient asks for them once, waits a microsecond, and then keeps made-up
 * numbers for good; the web request it started does finish a moment later, so this asks again every few calls for a while.
 * Pure logic, no Minecraft classes.
 */
public final class StatsRetry {

	/** Ask again every this many calls of the player's tick. */
	public static final int EVERY = 10;

	/** Give up after this many asks (a nicked player or a failed request never gets real stats). */
	public static final int MAX_ASKS = 60;

	private int calls;
	private int asks;

	/** @return true when the caller should look for the real stats now */
	public boolean shouldAsk() {
		if (asks >= MAX_ASKS) {
			return false;
		}
		calls++;
		if (calls % EVERY != 0) {
			return false;
		}
		asks++;
		return true;
	}

	public int asks() {
		return asks;
	}
}
