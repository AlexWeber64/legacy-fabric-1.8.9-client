package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.alexweber64.axolotlextras.bedwars.MessageFix;
import org.junit.jupiter.api.Test;

class MessageFixTest {

	/** The section sign that starts a Minecraft colour code. */
	private static final String S = "§";

	@Test
	void theTrailingGoldCountIsRemoved() {
		String kill = S + "dP1 Brauniecher " + S + "6" + S + "l/YEETED INTO VOID/ " + S + "fW2 malwaredestroyer";
		assertEquals(kill, MessageFix.stripTrailingCount(kill + " " + S + "6" + "9185"));
		String bed = S + "dPink Bed " + S + "7/broken/ " + S + "fW2 malwaredestroyer";
		assertEquals(bed, MessageFix.stripTrailingCount(bed + " " + S + "6" + "554"));
	}

	@Test
	void messagesWithoutACountAreUntouched() {
		String plain = S + "eY1 Vuzka " + S + "7/rekt/ " + S + "bA2 kongstrongDE";
		assertEquals(plain, MessageFix.stripTrailingCount(plain));
	}

	@Test
	void aWhiteNumberAfterANameIsNotMistakenForACount() {
		String name = S + "fW2 player " + S + "f2024";
		assertEquals(name, MessageFix.stripTrailingCount(name));
	}

	@Test
	void aCountGoesRightAfterTheKillerAndTheColourIsRestored() {
		String line = S + "bAlice" + S + "7 was knocked into the void by " + S + "cBob" + S + "7. " + S + "6" + S + "lFINAL KILL!";
		String expected = S + "bAlice" + S + "7 was knocked into the void by " + S + "cBob" + " " + S + "6" + "42" + S + "c"
			+ S + "7. " + S + "6" + S + "lFINAL KILL!";
		assertEquals(expected, MessageFix.appendAfterLastName(line, "Bob", " " + S + "6" + "42"));
	}

	@Test
	void aMissingNameLeavesTheLineAlone() {
		String line = S + "bAlice" + S + "7 fell.";
		assertEquals(line, MessageFix.appendAfterLastName(line, "Bob", " 1"));
	}

	@Test
	void firstLineStopsAtTheFirstBreak() {
		assertEquals("TEAM ELIMINATED", MessageFix.firstLine("TEAM ELIMINATED\nline two\nline three"));
		assertEquals("one line", MessageFix.firstLine("one line"));
	}
}
