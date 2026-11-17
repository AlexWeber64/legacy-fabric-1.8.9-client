package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import io.github.alexweber64.axolotlextras.bedwars.BuildHeight;
import org.junit.jupiter.api.Test;

class BuildHeightTest {

	private static Map<String, Integer> table(String text) throws IOException {
		return BuildHeight.readTable(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void readsTheTableAndSkipsCommentsAndBadLines() throws IOException {
		Map<String, Integer> limits = table("# comment\nAntenna=89\n\nBlizzard Bay = 93\nbroken\nZero=0\nWord=abc\n");
		assertEquals(2, limits.size());
		assertEquals(89, (int) limits.get("antenna"));
		assertEquals(93, (int) limits.get("blizzard bay"));
	}

	@Test
	void theMapLineGivesTheTopLayer() throws IOException {
		BuildHeight height = new BuildHeight(table("Antenna=89\nBlizzard Bay=93\n"));
		assertEquals(BuildHeight.UNKNOWN, height.topLayer());
		height.onSidebar(Arrays.asList("§fPlayers: §a7/8", "§fMap: §aBlizzard §aBay", "§fMode: §aDoubles"));
		assertEquals("Blizzard Bay", height.map());
		assertEquals(92, height.topLayer());
	}

	@Test
	void anUnknownMapDarkensNothing() throws IOException {
		BuildHeight height = new BuildHeight(table("Antenna=89\n"));
		height.onSidebar(Arrays.asList("Map: Somewhere New"));
		assertEquals(BuildHeight.UNKNOWN, height.topLayer());
	}

	@Test
	void theBridgeStartMessageGivesTheBridgeLimit() throws IOException {
		BuildHeight height = new BuildHeight(table("Antenna=89\n"));
		height.onChatLine("Knock off your opponent to gain a clear path.");
		assertEquals(BuildHeight.UNKNOWN, height.topLayer());
		height.onChatLine("                    Cross the bridge to score goals.");
		assertEquals(99, height.topLayer());
		height.reset();
		assertEquals(BuildHeight.UNKNOWN, height.topLayer());
	}

	@Test
	void theBundledTableLoads() throws IOException {
		try (InputStream in = BuildHeight.class.getResourceAsStream("/assets/axolotlextras/build_heights.txt")) {
			Map<String, Integer> limits = BuildHeight.readTable(in);
			assertTrue(limits.size() > 150, "maps: " + limits.size());
			assertEquals(89, (int) limits.get("antenna"));
		}
	}
}
