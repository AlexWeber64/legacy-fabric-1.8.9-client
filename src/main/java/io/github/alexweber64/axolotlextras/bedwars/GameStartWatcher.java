package io.github.alexweber64.axolotlextras.bedwars;

/**
 * Sees a Bed Wars game start in chat, in the normal modes and in the Dreams modes. Every start in the logs opens with
 * the same banner: a line of "▬" characters, then a title line that starts with "Bed Wars" ("Bed Wars", or "Bed Wars
 * Lucky Blocks"). Only the normal modes go on with "Protect your bed and destroy the enemy beds.", which is all
 * AxolotlClient and the add-on used to look for, so a Dreams game was never seen as started. That line still counts on
 * its own, unless a banner title started the game just before it.
 *
 * <p>The end of a game prints the same banner and title ("Bed Wars"), followed by the winners ("Red - Steve, Alex") and
 * the top killers ("1st Killer - Steve - 9"). So the title alone does not decide: the next line that is not blank does.
 * The start is reported on that line, in the same tick. Pure logic; feed it every chat line in order, one watcher per
 * reader.
 */
public final class GameStartWatcher {

	/** The "Protect your bed" line comes two lines after the banner title; this many lines cover it. */
	private static final int SAME_BANNER_LINES = 4;

	private boolean afterSeparator;
	/** The banner title waiting for the next line that is not blank, or null. */
	private String pendingTitle;
	private String lastTitle = "";
	private int linesSinceStart = SAME_BANNER_LINES + 1;

	/**
	 * @param chatLine a chat line, with or without colour codes
	 * @return true when this line starts a Bed Wars game
	 */
	public boolean onLine(String chatLine) {
		String text = chatLine == null ? "" : ResourceMath.stripFormatting(chatLine).trim();
		boolean start = false;
		if (pendingTitle != null) {
			if (text.isEmpty()) {
				return false;
			}
			if (!isEndOfGameLine(text)) {
				start = true;
				lastTitle = pendingTitle;
			}
			pendingTitle = null;
		} else if (afterSeparator && isBannerTitle(text)) {
			pendingTitle = text;
		} else if (ResourceMath.isGameStartMessage(text) && linesSinceStart > SAME_BANNER_LINES) {
			start = true;
			lastTitle = text;
		}
		afterSeparator = isSeparator(text);
		linesSinceStart = start ? 0 : Math.min(linesSinceStart + 1, SAME_BANNER_LINES + 1);
		return start;
	}

	/** The banner title (or the "Protect your bed" line) of the last start, for the log. */
	public String lastTitle() {
		return lastTitle;
	}

	/**
	 * A line of the end-of-game banner: the winners ("Red - [VIP] Steve, Alex"), a top killer ("1st Killer - Steve - 9"),
	 * or a draw. Start banners go on with a description ("Protect your bed...", "Collect Lucky Blocks...").
	 */
	static boolean isEndOfGameLine(String text) {
		return text.contains(" - ") || text.contains("Killer") || text.contains("Winner") || text.startsWith("Draw");
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
