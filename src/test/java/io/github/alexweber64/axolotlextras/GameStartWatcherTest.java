package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexweber64.axolotlextras.bedwars.GameStartWatcher;
import org.junit.jupiter.api.Test;

/** The chat lines are copied from the play clone's logs (2026-11-13 to 2026-11-15). */
class GameStartWatcherTest {

	private static final String BAR = "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

	/** How many lines of the given chat start a game. */
	private static int starts(String... lines) {
		GameStartWatcher watcher = new GameStartWatcher();
		int count = 0;
		for (String line : lines) {
			if (watcher.onLine(line)) {
				count++;
			}
		}
		return count;
	}

	@Test
	void normalGameStartsOnceOnTheBanner() {
		GameStartWatcher watcher = new GameStartWatcher();
		assertFalse(watcher.onLine("The game starts in 1 second!"));
		assertFalse(watcher.onLine(BAR));
		assertTrue(watcher.onLine("                                  Bed Wars"));
		assertFalse(watcher.onLine(""));
		// The line AxolotlClient used: the same start, so it does not count twice.
		assertFalse(watcher.onLine("     Protect your bed and destroy the enemy beds."));
		assertFalse(watcher.onLine("      Upgrade yourself and your team by collecting"));
		assertFalse(watcher.onLine(BAR));
	}

	@Test
	void luckyBlocksStartsOnTheBanner() {
		assertEquals(1, starts("The game starts in 1 second!", BAR, "                       Bed Wars Lucky Blocks", "",
			"    Collect Lucky Blocks from resource generators", "       to receive random loot! Break them to reveal",
			"                             their contents!", "", BAR));
	}

	@Test
	void otherBannersAndLinesDoNotStartAGame() {
		assertEquals(0, starts(BAR, "Bridge Duel", "Cross the bridge to score goals.", BAR));
		assertEquals(0, starts(BAR, "Dutch_Hack is radiating with Generosity!", BAR));
		assertEquals(0, starts(BAR, "+10 tokens! (Time Played)", BAR));
		// "Bed Wars" without the banner border before it, as in other chat.
		assertEquals(0, starts("Bed Wars", "§70:14§r +15 Bed Wars XP (Time Played)"));
	}

	@Test
	void protectLineAloneStillStartsAGame() {
		// If the banner title ever goes missing, the old line still works.
		assertEquals(1, starts("The game starts in 1 second!", "     Protect your bed and destroy the enemy beds."));
	}

	@Test
	void twoGamesInARowStartTwice() {
		assertEquals(2, starts(BAR, "Bed Wars", "", "Protect your bed and destroy the enemy beds.", BAR,
			"Sending you to mini82AQ!", "Cyverno has joined (3/16)!", BAR, "Bed Wars Lucky Blocks", BAR));
	}
}
