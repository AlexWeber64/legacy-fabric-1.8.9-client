package io.github.alexweber64.axolotlextras.bedwars;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Works out the highest layer you can build at in the current game, for the Build Height Indicator. No Minecraft imports,
 * so it can be unit tested.
 * <ul>
 * <li>Bed Wars: the waiting lobby sidebar names the map ("Map: Antenna"), and the bundled table
 * ({@code build_heights.txt}, the list Hytils Reborn uses) gives that map's limit.</li>
 * <li>The Bridge: the start message ("Cross the bridge to score goals.", seen in a real game log) marks a Bridge game, and
 * every Bridge map has the same limit.</li>
 * </ul>
 * A map that is not in the table gives no layer, so nothing is darkened rather than a wrong layer.
 */
public final class BuildHeight {

	/** No layer known. */
	public static final int UNKNOWN = Integer.MIN_VALUE;
	/** The Bridge's build limit (the value Hytils Reborn uses for every Bridge map). */
	public static final int BRIDGE_MAX_BUILD = 100;

	private static final Pattern MAP_LINE = Pattern.compile("^Map:\\s*(.+?)\\s*$", Pattern.CASE_INSENSITIVE);
	private static final String BRIDGE_START = "Cross the bridge to score goals.";

	private final Map<String, Integer> limits;
	private String map;
	private boolean bridge;

	/** @param limits each map's build limit by lower case name: the topmost layer you can build at, plus one */
	public BuildHeight(Map<String, Integer> limits) {
		this.limits = limits;
	}

	/** Reads the table: one "Name=limit" per line, "#" starts a comment. Lines that do not parse are skipped. */
	public static Map<String, Integer> readTable(InputStream in) throws IOException {
		Map<String, Integer> limits = new HashMap<>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			int equals = line.lastIndexOf('=');
			if (line.isEmpty() || line.startsWith("#") || equals <= 0) {
				continue;
			}
			try {
				int limit = Integer.parseInt(line.substring(equals + 1).trim());
				if (limit > 0) {
					limits.put(line.substring(0, equals).trim().toLowerCase(Locale.ROOT), limit);
				}
			} catch (NumberFormatException ignored) {
				// skipped
			}
		}
		return limits;
	}

	/** A new world (another game or lobby): nothing is known. */
	public void reset() {
		map = null;
		bridge = false;
	}

	/** @param line a chat line without colour codes */
	public void onChatLine(String line) {
		if (line != null && line.trim().equals(BRIDGE_START)) {
			bridge = true;
		}
	}

	/** @param sidebarLines the sidebar lines, with or without colour codes */
	public void onSidebar(List<String> sidebarLines) {
		for (String raw : sidebarLines) {
			Matcher matcher = MAP_LINE.matcher(ResourceMath.stripFormatting(raw).trim());
			if (matcher.matches()) {
				map = matcher.group(1);
				return;
			}
		}
	}

	/** The map name read from the sidebar, or null. */
	public String map() {
		return map;
	}

	public boolean bridge() {
		return bridge;
	}

	/** The topmost layer you can build at, the one to darken, or {@link #UNKNOWN}. */
	public int topLayer() {
		if (bridge) {
			return BRIDGE_MAX_BUILD - 1;
		}
		Integer limit = map == null ? null : limits.get(map.toLowerCase(Locale.ROOT));
		return limit == null ? UNKNOWN : limit - 1;
	}
}
