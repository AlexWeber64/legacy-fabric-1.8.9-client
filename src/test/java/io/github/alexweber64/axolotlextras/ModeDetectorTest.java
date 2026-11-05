package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;

import io.github.alexweber64.axolotlextras.bedwars.ModeDetector;
import io.github.alexweber64.axolotlextras.bedwars.UpgradeTracker.Mode;
import org.junit.jupiter.api.Test;

class ModeDetectorTest {

	private static String[] teams(String... names) {
		String[] out = new String[names.length];
		for (int i = 0; i < names.length; i++) {
			out[i] = "§c" + names[i].charAt(0) + " §f" + names[i] + ":§a ✓";
		}
		return out;
	}

	@Test
	void eightTeamLinesMeanSoloOrDoubles() {
		assertEquals(Mode.SMALL_TEAMS, ModeDetector.detect(Arrays.asList(teams(
			"Red", "Blue", "Green", "Yellow", "Aqua", "White", "Pink", "Gray"))));
	}

	@Test
	void fourOrFewerTeamLinesMeanBigTeams() {
		assertEquals(Mode.BIG_TEAMS, ModeDetector.detect(Arrays.asList(teams("Red", "Blue", "Green", "Yellow"))));
		assertEquals(Mode.BIG_TEAMS, ModeDetector.detect(Arrays.asList(teams("Red", "Blue"))));
	}

	@Test
	void aModeLineWinsWhenPresent() {
		assertEquals(Mode.SMALL_TEAMS, ModeDetector.detect(Arrays.asList("Map: Amazon", "Mode: Doubles")));
		assertEquals(Mode.BIG_TEAMS, ModeDetector.detect(Arrays.asList("§fMode: §a4v4v4v4")));
	}

	@Test
	void theRealWaitingLobbySidebarSaysDoubles() {
		// Copied from a screenshot of the Bed Wars waiting lobby.
		java.util.List<String> lobby = Arrays.asList("§7 11/05/26 §8m97CU", "", "Map: §aCascade",
			"Players: §a11/16", "", "Waiting...", "", "Mode: §aDoubles", "Version: §7v1.11.3",
			"§ewww.hypixel.net");
		assertEquals(Mode.SMALL_TEAMS, ModeDetector.detect(lobby));
		assertEquals(Mode.SMALL_TEAMS, ModeDetector.fromModeLine(lobby));
		assertNull(ModeDetector.fromTeamLines(lobby));
	}

	@Test
	void theTeamCountIsTheSecondCheck() {
		assertNull(ModeDetector.fromModeLine(Arrays.asList(teams("Red", "Blue"))));
		assertEquals(Mode.BIG_TEAMS, ModeDetector.fromTeamLines(Arrays.asList(teams("Red", "Blue"))));
	}

	@Test
	void unrelatedLinesGiveNoAnswer() {
		assertNull(ModeDetector.detect(Collections.<String>emptyList()));
		assertNull(ModeDetector.detect(Arrays.asList("Map: Amazon", "Players: 3/8", "Level: 120", "R Red: ")));
	}
}
