package io.github.alexweber64.axolotlextras.bedwars;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text helpers for AxolotlClient's rewritten Bed Wars chat messages. Pure logic.
 *
 * <p>AxolotlClient appends a gold number to the killer of a final kill (their final kill count) and to the player who
 * broke a bed (their beds broken count). Without a Hypixel API key those counts come from made-up stats, so they are
 * removed again in that case.
 */
public final class MessageFix {

	/** A trailing " section-sign 6 digits" as AxolotlClient appends it. */
	private static final Pattern TRAILING_COUNT = Pattern.compile("^(.*) §6\\d+$", Pattern.DOTALL);

	private MessageFix() {
	}

	public static String stripTrailingCount(String message) {
		Matcher m = TRAILING_COUNT.matcher(message);
		return m.matches() ? m.group(1) : message;
	}

	/**
	 * Puts {@code suffix} right after the last occurrence of {@code name} in a formatted chat line, and then restores the
	 * colour that was active at the end of the name, so the rest of the line keeps its look.
	 *
	 * @return the line unchanged when the name does not occur
	 */
	public static String appendAfterLastName(String formatted, String name, String suffix) {
		int start = formatted.lastIndexOf(name);
		if (start < 0 || name.isEmpty()) {
			return formatted;
		}
		int end = start + name.length();
		String before = formatted.substring(0, end);
		String restore = "";
		int sign = before.lastIndexOf('§');
		if (sign >= 0 && sign + 1 < before.length()) {
			restore = before.substring(sign, sign + 2);
		}
		return before + suffix + restore + formatted.substring(end);
	}

	/** The text before the first line break: the heading of a "TEAM ELIMINATED" message. */
	public static String firstLine(String message) {
		int newline = message.indexOf('\n');
		return newline < 0 ? message : message.substring(0, newline);
	}
}
