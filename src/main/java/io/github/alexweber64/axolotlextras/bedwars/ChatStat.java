package io.github.alexweber64.axolotlextras.bedwars;

/**
 * The stat shown for every player in the chat when a Bed Wars game starts. Pure logic: it takes plain numbers.
 */
public enum ChatStat {
	KDR("KDR"),
	FKDR("FKDR"),
	WLR("WLR"),
	BBLR("BBLR"),
	STARS("Stars");

	public final String label;

	ChatStat(String label) {
		this.label = label;
	}

	/** Totals from the player's Bed Wars stats. */
	public static final class Counts {
		final int kills;
		final int deaths;
		final int finalKills;
		final int finalDeaths;
		final int wins;
		final int losses;
		final int bedsBroken;
		final int stars;

		public Counts(int kills, int deaths, int finalKills, int finalDeaths, int wins, int losses, int bedsBroken, int stars) {
			this.kills = kills;
			this.deaths = deaths;
			this.finalKills = finalKills;
			this.finalDeaths = finalDeaths;
			this.wins = wins;
			this.losses = losses;
			this.bedsBroken = bedsBroken;
			this.stars = stars;
		}
	}

	public String format(Counts c) {
		switch (this) {
			case KDR:
				return KdrMath.ratio(c.kills, c.deaths);
			case FKDR:
				return KdrMath.ratio(c.finalKills, c.finalDeaths);
			case WLR:
				return KdrMath.ratio(c.wins, c.losses);
			case BBLR:
				return KdrMath.ratio(c.bedsBroken, c.losses);
			default:
				return String.valueOf(c.stars);
		}
	}

	/** @param key the option value, such as "kdr" (any case); anything unknown gives KDR */
	public static ChatStat byKey(String key) {
		for (ChatStat stat : values()) {
			if (stat.name().equalsIgnoreCase(key)) {
				return stat;
			}
		}
		return KDR;
	}

	public static String[] keys() {
		ChatStat[] all = values();
		String[] keys = new String[all.length];
		for (int i = 0; i < all.length; i++) {
			keys[i] = all[i].name().toLowerCase();
		}
		return keys;
	}
}
