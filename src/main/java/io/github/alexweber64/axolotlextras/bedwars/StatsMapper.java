package io.github.alexweber64.axolotlextras.bedwars;

import java.util.Map;

/**
 * Turns the Bed Wars numbers of a Hypixel API player reply into the 14 numbers AxolotlClient's BedwarsPlayerStats takes,
 * in its order. Pure logic, no Minecraft classes.
 */
public final class StatsMapper {

	private StatsMapper() {
	}

	/**
	 * @param bedwars the "stats.Bedwars" values by name (a player who never played Bed Wars has none: all zero)
	 * @param stars the "achievements.bedwars_level" value, or null when missing (1 then)
	 * @return finalKills, finalDeaths, bedsBroken, deaths, kills, then the five counts of the current game (all 0 at the start),
	 *         losses, wins, winstreak, stars
	 */
	public static int[] constructorArgs(Map<String, Integer> bedwars, Integer stars) {
		return new int[] {
			value(bedwars, "final_kills_bedwars"),
			value(bedwars, "final_deaths_bedwars"),
			value(bedwars, "beds_broken_bedwars"),
			value(bedwars, "deaths_bedwars"),
			value(bedwars, "kills_bedwars"),
			0, 0, 0, 0, 0,
			value(bedwars, "losses_bedwars"),
			value(bedwars, "wins_bedwars"),
			value(bedwars, "winstreak"),
			stars == null ? 1 : stars
		};
	}

	private static int value(Map<String, Integer> values, String key) {
		Integer v = values == null ? null : values.get(key);
		return v == null ? 0 : v;
	}
}
