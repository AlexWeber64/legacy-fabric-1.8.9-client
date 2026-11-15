package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import io.github.alexweber64.axolotlextras.hacks.SafeSneak;
import org.junit.jupiter.api.Test;

class SafeSneakTest {

	private static final SafeSneak.EdgeProbe EDGE = inset -> true;
	private static final SafeSneak.EdgeProbe SOLID = inset -> false;

	@Test
	void sneaksAtTheEdgeAndHoldsAfter() {
		SafeSneak sneak = new SafeSneak(new Random(1));
		assertFalse(sneak.tick(true, false, SOLID, false, 100, 100, 0));
		assertTrue(sneak.tick(true, false, EDGE, false, 100, 100, 1000));
		// Back over the block: still sneaking until the hold is over.
		assertTrue(sneak.tick(true, false, SOLID, false, 100, 100, 1099));
		assertFalse(sneak.tick(true, false, SOLID, false, 100, 100, 1100));
	}

	@Test
	void yourOwnSneakKeyAndTheConditionsWin() {
		SafeSneak sneak = new SafeSneak(new Random(1));
		assertFalse(sneak.tick(true, true, EDGE, false, 100, 100, 0));
		assertTrue(sneak.tick(true, false, EDGE, false, 100, 100, 10));
		// Conditions gone (a menu opened, looking up): the hold ends at once.
		assertFalse(sneak.tick(false, false, SOLID, false, 100, 100, 20));
		assertFalse(sneak.tick(true, false, SOLID, false, 100, 100, 30));
	}

	@Test
	void theHoldIsRandomWithinTheBounds() {
		SafeSneak sneak = new SafeSneak(new Random(42));
		Set<Long> holds = new HashSet<>();
		long now = 0;
		for (int i = 0; i < 200; i++) {
			now += 10_000;
			sneak.tick(true, false, EDGE, false, 80, 220, now);
			long t = now + 1;
			while (sneak.tick(true, false, SOLID, false, 80, 220, t)) {
				t++;
			}
			long hold = t - now;
			assertTrue(hold >= 80 && hold <= 221, "hold " + hold);
			holds.add(hold);
		}
		assertTrue(holds.size() > 50, "the hold should vary, got " + holds.size() + " different values");
	}

	@Test
	void theStartSpotIsDrawnPerEdgeAndStaysSafe() {
		SafeSneak sneak = new SafeSneak(new Random(7));
		Set<Double> insets = new HashSet<>();
		long now = 0;
		for (int i = 0; i < 50; i++) {
			now += 10_000;
			sneak.tick(true, false, SOLID, true, 0, 0, now);
			double inset = sneak.inset();
			assertTrue(inset >= SafeSneak.MIN_INSET && inset <= SafeSneak.MAX_INSET);
			insets.add(inset);
			// Walking up to the edge and sneaking at it: the spot does not change.
			sneak.tick(true, false, SOLID, true, 0, 0, now + 1);
			assertEquals(inset, sneak.inset(), 1e-12);
			sneak.tick(true, false, EDGE, true, 50, 50, now + 2);
			sneak.tick(true, false, EDGE, true, 50, 50, now + 3);
			assertEquals(inset, sneak.inset(), 1e-12);
			// The hold ends: the next edge gets a new spot.
			sneak.tick(true, false, SOLID, true, 50, 50, now + 100);
		}
		assertTrue(insets.size() > 40);
		// The player's box is 0.3 blocks from its middle to its side: the sneak always starts before it leaves the block.
		assertTrue(SafeSneak.MIN_INSET > 0.1 && SafeSneak.MAX_INSET < 0.3);
	}

	@Test
	void withoutRandomnessTheSpotIsFixed() {
		SafeSneak sneak = new SafeSneak(new Random(3));
		sneak.tick(true, false, SOLID, false, 0, 0, 0);
		assertEquals(SafeSneak.FIXED_INSET, sneak.inset(), 1e-12);
	}

	@Test
	void boundsInEitherOrderWork() {
		SafeSneak sneak = new SafeSneak(new Random(5));
		assertTrue(sneak.tick(true, false, EDGE, false, 300, 100, 0));
		assertTrue(sneak.tick(true, false, SOLID, false, 300, 100, 99));
		assertFalse(sneak.tick(true, false, SOLID, false, 300, 100, 301));
	}
}
