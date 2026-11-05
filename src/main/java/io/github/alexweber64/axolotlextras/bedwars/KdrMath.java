package io.github.alexweber64.axolotlextras.bedwars;

import java.util.Locale;

/** Kill/death ratio text, kept free of Minecraft classes so it can be tested. */
public final class KdrMath {

	private KdrMath() {
	}

	/** Kills divided by deaths with two decimals. With no deaths the ratio is the kill count itself. */
	public static String ratio(int kills, int deaths) {
		double value = deaths <= 0 ? Math.max(kills, 0) : (double) kills / deaths;
		return String.format(Locale.ROOT, "%.2f", value);
	}

	/** The text shown above a player's head. */
	public static String levelHead(int kills, int deaths) {
		return "§7KDR: §f" + ratio(kills, deaths);
	}
}
