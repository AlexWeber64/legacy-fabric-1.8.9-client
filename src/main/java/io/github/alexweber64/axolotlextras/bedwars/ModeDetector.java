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
		int teamLines = 0;
		for (String raw : sidebarLines) {
			if (TEAM_LINE.matcher(ResourceMath.stripFormatting(raw).trim()).find()) {
				teamLines++;
			}
		}
		if (teamLines >= 5) {
			return UpgradeTracker.Mode.SMALL_TEAMS;
		}
		if (teamLines >= 2) {
			return UpgradeTracker.Mode.BIG_TEAMS;
		}
		return null;
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
