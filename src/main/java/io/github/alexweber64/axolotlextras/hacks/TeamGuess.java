package io.github.alexweber64.axolotlextras.hacks;

/**
 * Works out which Bed Wars team a bed belongs to: from the coloured blocks around it (team wool, clay and glass), or
 * else from the players standing next to it. No Minecraft imports, so it can be unit tested.
 */
public final class TeamGuess {

	public static final int UNKNOWN = -1;

	/** Hypixel's eight teams, with the colour code their names are written in. */
	public static final String[] NAMES = {"Red", "Blue", "Green", "Yellow", "Aqua", "White", "Pink", "Gray"};
	public static final char[] CODES = {'c', '9', 'a', 'e', 'b', 'f', 'd', '8'};
	public static final int COUNT = NAMES.length;

	/** The coloured blocks needed before they decide the team, and how far the top colour has to lead the next one. */
	static final int MIN_BLOCKS = 4;
	static final int LEAD_FACTOR = 2;

	private TeamGuess() {
	}

	/** @return the team for a dye colour (wool, clay or glass metadata), or {@link #UNKNOWN} */
	public static int fromDye(int meta) {
		switch (meta) {
			case 14: return 0; // red
			case 11: return 1; // blue
			case 5: case 13: return 2; // lime, green
			case 4: return 3; // yellow
			case 3: case 9: return 4; // light blue, cyan
			case 0: return 5; // white
			case 6: case 2: return 6; // pink, magenta
			case 7: case 8: return 7; // gray, light gray
			default: return UNKNOWN;
		}
	}

	/** @return the team for a colour code character, or {@link #UNKNOWN} */
	public static int fromColorCode(char code) {
		switch (Character.toLowerCase(code)) {
			case 'c': case '4': return 0;
			case '9': case '1': return 1;
			case 'a': case '2': return 2;
			case 'e': case '6': return 3;
			case 'b': case '3': return 4;
			case 'f': return 5;
			case 'd': case '5': return 6;
			case '8': case '7': return 7;
			default: return UNKNOWN;
		}
	}

	/** @return the team of a formatted player name ("§c§lR §cSteve" is Red), or {@link #UNKNOWN} */
	public static int fromFormattedName(String formatted) {
		if (formatted == null) {
			return UNKNOWN;
		}
		for (int i = 0; i + 1 < formatted.length(); i++) {
			if (formatted.charAt(i) == '§' && "0123456789abcdef".indexOf(Character.toLowerCase(formatted.charAt(i + 1))) >= 0) {
				return fromColorCode(formatted.charAt(i + 1));
			}
		}
		return UNKNOWN;
	}

	/**
	 * Your own team from a line of the Bed Wars sidebar: Hypixel writes "YOU" after your team ("R Red: ✔ YOU"). This works
	 * on every map and before your name gets its team colour (in the waiting lobby names are in rank colours, so a VIP's
	 * green name says nothing about the team).
	 *
	 * @param line a sidebar line, with or without colour codes
	 * @return the team, or {@link #UNKNOWN} when the line is not your team's line
	 */
	public static int fromSidebarLine(String line) {
		if (line == null) {
			return UNKNOWN;
		}
		String plain = strip(line);
		if (!plain.matches(".*\\bYOU\\b.*")) {
			return UNKNOWN;
		}
		for (int team = 0; team < COUNT; team++) {
			if (plain.matches(".*\\b" + NAMES[team] + ":.*") || (team == 7 && plain.matches(".*\\bGrey:.*"))) {
				return team;
			}
		}
		return UNKNOWN;
	}

	/** The text without colour and format codes. */
	public static String strip(String formatted) {
		return formatted.replaceAll("§.?", "");
	}

	/**
	 * @param blockVotes  coloured blocks around the bed, per team
	 * @param playerVotes players standing next to the bed, per team
	 * @return the team, or {@link #UNKNOWN} when neither tells
	 */
	public static int guess(int[] blockVotes, int[] playerVotes) {
		int top = top(blockVotes);
		if (top != UNKNOWN && blockVotes[top] >= MIN_BLOCKS && blockVotes[top] >= LEAD_FACTOR * second(blockVotes, top)) {
			return top;
		}
		int player = top(playerVotes);
		return player != UNKNOWN && playerVotes[player] > 0 ? player : UNKNOWN;
	}

	/** @return the team name in its colour, like "§cRed" */
	public static String colored(int team) {
		return team < 0 || team >= COUNT ? "§7?" : "§" + CODES[team] + NAMES[team];
	}

	private static int top(int[] votes) {
		int best = UNKNOWN;
		for (int i = 0; i < votes.length; i++) {
			if (votes[i] > 0 && (best == UNKNOWN || votes[i] > votes[best])) {
				best = i;
			}
		}
		return best;
	}

	private static int second(int[] votes, int top) {
		int best = 0;
		for (int i = 0; i < votes.length; i++) {
			if (i != top) {
				best = Math.max(best, votes[i]);
			}
		}
		return best;
	}
}
