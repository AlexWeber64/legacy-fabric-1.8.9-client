package io.github.alexweber64.axolotlextras.bedwars;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tells solo/doubles (8 teams) from 3v3v3v3, 4v4v4v4 and 4v4 (4 teams or fewer) by reading the Bed Wars sidebar. Pure
 * logic. The waiting lobby layout was checked against a screenshot, the in-game team lines are from memory of Hypixel, so
 * a null result means "could not tell".
 */
public final class ModeDetector {

	private static final Pattern MODE_LINE = Pattern.compile("^Mode:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	/** A team line such as "R Red: <check mark>" or "[R] Red:". */
	private static final Pattern TEAM_LINE = Pattern.compile("^\\[?[A-Z]\\]?\\s+[A-Z][a-z]+:");

	private ModeDetector() {
	}

	/** The mode line first, then the team count. @return the team size group, or null when the lines do not say */
	public static UpgradeTracker.Mode detect(List<String> sidebarLines) {
		UpgradeTracker.Mode named = fromModeLine(sidebarLines);
		return named != null ? named : fromTeamLines(sidebarLines);
	}

	/** The waiting lobby sidebar has a "Mode: Doubles" line (seen on a real Hypixel lobby). */
	public static UpgradeTracker.Mode fromModeLine(List<String> sidebarLines) {
		for (String raw : sidebarLines) {
			Matcher mode = MODE_LINE.matcher(ResourceMath.stripFormatting(raw).trim());
			if (mode.matches()) {
				UpgradeTracker.Mode named = fromName(mode.group(1));
				if (named != null) {
					return named;
				}
			}
		}
		return null;
	}

	/**
	 * The in-game sidebar has one line per team: eight teams mean solo or doubles, four or two (4v4) mean the 3s/4s
	 * prices. Fewer than two team lines, as in a lobby, gives null.
	 */
	public static UpgradeTracker.Mode fromTeamLines(List<String> sidebarLines) {
		return fromTeamCount(countTeamLines(sidebarLines));
	}

	/** The number of team lines ("R Red: ...") on the sidebar. */
	public static int countTeamLines(List<String> sidebarLines) {
		int teamLines = 0;
		for (String raw : sidebarLines) {
			if (TEAM_LINE.matcher(ResourceMath.stripFormatting(raw).trim()).find()) {
				teamLines++;
			}
		}
		return teamLines;
	}

	/** Five or more team lines: solo or doubles. Two to four: the 3s/4s prices. Fewer: null (not in a game). */
	public static UpgradeTracker.Mode fromTeamCount(int teamLines) {
		if (teamLines >= 5) {
			return UpgradeTracker.Mode.SMALL_TEAMS;
		}
		if (teamLines >= 2) {
			return UpgradeTracker.Mode.BIG_TEAMS;
		}
		return null;
	}

	/**
	 * Decides the team size from the in-game team lines only once they have settled. Hypixel fills the sidebar in over a
	 * few moments when a game starts, so a doubles game can show only two to four of its eight team lines at first,
	 * which used to be read as 3s/4s for the whole game. Five or more lines settle it at once (only solo and doubles have
	 * that many); fewer wait {@link #SETTLE_TICKS} after the first team line and then use the most lines seen.
	 */
	public static final class Settler {

		/** Three seconds. Nobody buys a diamond upgrade that early, so the wait costs nothing. */
		public static final int SETTLE_TICKS = 60;

		private int firstTick = -1;
		private int most;

		public void reset() {
			firstTick = -1;
			most = 0;
		}

		/**
		 * @param teamLines the team lines on the sidebar now
		 * @param tick      a counter that goes up by one per game tick
		 * @return the team size group once it is settled, else null
		 */
		public UpgradeTracker.Mode offer(int teamLines, int tick) {
			if (firstTick < 0) {
				if (teamLines < 2) {
					return null;
				}
				firstTick = tick;
			}
			most = Math.max(most, teamLines);
			if (most >= 5 || tick - firstTick >= SETTLE_TICKS) {
				return fromTeamCount(most);
			}
			return null;
		}
	}

	static UpgradeTracker.Mode fromName(String name) {
		String n = name.toLowerCase();
		if (n.contains("solo") || n.contains("double")) {
			return UpgradeTracker.Mode.SMALL_TEAMS;
		}
		if (n.contains("3v3v3v3") || n.contains("4v4v4v4") || n.contains("4v4") || n.contains("three") || n.contains("four")) {
			return UpgradeTracker.Mode.BIG_TEAMS;
		}
		return null;
	}
}
