package io.github.alexweber64.axolotlextras.bedwars;

/**
 * Sees a Bed Wars game start in chat, in the normal modes and in the Dreams modes. Every start in the logs opens with
 * the same banner: a line of "▬" characters, then a title line that starts with "Bed Wars" ("Bed Wars", or "Bed Wars
 * Lucky Blocks"). Only the normal modes go on with "Protect your bed and destroy the enemy beds.", which is all
 * AxolotlClient and the add-on used to look for, so a Dreams game was never seen as started. That line still counts on
 * its own, unless a banner title started the game just before it. Pure logic; feed it every chat line in order, one
 * watcher per reader.
 */
public final class GameStartWatcher {

	/** The "Protect your bed" line comes two lines after the banner title; this many lines cover it. */
	private static final int SAME_BANNER_LINES = 4;

	private boolean afterSeparator;
	private int linesSinceStart = SAME_BANNER_LINES + 1;

	/**
	 * @param chatLine a chat line, with or without colour codes
	 * @return true when this line starts a Bed Wars game
	 */
	public boolean onLine(String chatLine) {
		String text = chatLine == null ? "" : ResourceMath.stripFormatting(chatLine).trim();
		boolean start = (afterSeparator && isBannerTitle(text))
			|| (ResourceMath.isGameStartMessage(text) && linesSinceStart > SAME_BANNER_LINES);
		afterSeparator = isSeparator(text);
		linesSinceStart = start ? 0 : Math.min(linesSinceStart + 1, SAME_BANNER_LINES + 1);
		return start;
	}

	/** A line made only of "▬" characters (Hypixel's banner border). */
	static boolean isSeparator(String text) {
		if (text.isEmpty()) {
			return false;
		}
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) != '▬') {
				return false;
			}
		}
		return true;
	}

	/** "Bed Wars" or "Bed Wars" and a mode name. Other games' banners ("Bridge Duel") do not count. */
	static boolean isBannerTitle(String text) {
		return text.equals("Bed Wars") || text.startsWith("Bed Wars ");
	}
}
