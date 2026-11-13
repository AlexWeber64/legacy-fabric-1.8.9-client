package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.github.alexweber64.axolotlextras.hacks.InvisTracker;
import org.junit.jupiter.api.Test;

class InvisTrackerTest {

	private static final Set<String> A = Collections.singleton("a");
	private static final Set<String> NONE = Collections.emptySet();

	private static InvisTracker.Sighting sighting(boolean invisible, boolean potion, double x, double distance) {
		InvisTracker.Sighting s = new InvisTracker.Sighting();
		s.id = "a";
		s.entityId = 7;
		s.name = "Alex";
		s.invisible = invisible;
		s.holdingPotion = potion;
		s.x = x;
		s.y = 64;
		s.yaw = 90;
		s.pitch = 10;
		s.distance = distance;
		return s;
	}

	private static InvisTracker.Mark run(InvisTracker tracker, boolean invisible, int from, int to) {
		return run(tracker, invisible, false, false, from, to);
	}

	/** Ticks a player through, from {@code from} to {@code to} (inclusive), and returns the last confirmed mark. */
	private static InvisTracker.Mark run(InvisTracker tracker, boolean invisible, boolean potion, boolean needPotion,
		int from, int to) {
		InvisTracker.Mark confirmed = null;
		for (int t = from; t <= to; t++) {
			InvisTracker.Mark m = tracker.see(sighting(invisible, potion, t, 10), needPotion, t);
			if (m != null) {
				confirmed = m;
			}
			tracker.endTick(A, t);
		}
		return confirmed;
	}

	@Test
	void goingInvisibleIsMarkedWhereItHappened() {
		InvisTracker tracker = new InvisTracker();
		assertNull(run(tracker, false, 0, 5));
		InvisTracker.Mark mark = run(tracker, true, 6, 6 + InvisTracker.CONFIRM_TICKS);
		assertNotNull(mark);
		assertEquals(6, mark.x, 1e-9); // the spot of the first invisible tick, not where they are now
		assertEquals(6, mark.tick);
		assertFalse(mark.vanished);
		assertEquals(1, tracker.marks().size());
	}

	@Test
	void aShortFlickerIsNotReported() {
		InvisTracker tracker = new InvisTracker();
		run(tracker, false, 0, 5);
		assertNull(run(tracker, true, 6, 8));
		assertNull(run(tracker, false, 9, 30));
		assertTrue(tracker.marks().isEmpty());
	}

	@Test
	void someoneFirstSeenInvisibleGivesNoMark() {
		InvisTracker tracker = new InvisTracker();
		assertNull(run(tracker, true, 0, 100));
		assertTrue(tracker.marks().isEmpty());
	}

	@Test
	void marksLastSixtySecondsAndOnePerPlayer() {
		InvisTracker tracker = new InvisTracker();
		run(tracker, false, 0, 5);
		run(tracker, true, 6, 20);
		run(tracker, false, 21, 25);
		run(tracker, true, 26, 40);
		assertEquals(1, tracker.marks().size());
		assertEquals(26, tracker.marks().get(0).tick);
		// Gone from range but the mark stays until its time is up.
		tracker.endTick(NONE, 26 + InvisTracker.LIFETIME_TICKS - 1);
		assertEquals(1, tracker.marks().size());
		tracker.endTick(NONE, 26 + InvisTracker.LIFETIME_TICKS);
		assertTrue(tracker.marks().isEmpty());
	}

	@Test
	void leavingRenderDistanceGivesNoMark() {
		InvisTracker tracker = new InvisTracker();
		// Walking off at the edge of range, no potion: gone, but no mark.
		for (int t = 0; t <= 5; t++) {
			tracker.see(sighting(false, false, t, 60), false, t);
			tracker.endTick(A, t);
		}
		assertTrue(tracker.endTick(NONE, 6).isEmpty());
		for (int t = 7; t < 200; t++) {
			tracker.endTick(NONE, t);
		}
		// Coming back already invisible gives no mark either.
		assertNull(run(tracker, true, 200, 400));
		assertTrue(tracker.marks().isEmpty());
	}

	@Test
	void vanishingRightAfterThePotionIsMarked() {
		InvisTracker tracker = new InvisTracker();
		run(tracker, false, true, true, 0, 30); // drinking, 10 blocks away
		List<InvisTracker.Mark> gone = tracker.endTick(NONE, 31);
		assertEquals(1, gone.size());
		assertTrue(gone.get(0).vanished);
		assertEquals(30, gone.get(0).x, 1e-9); // where they were last seen
		assertEquals(7, gone.get(0).entityId);
		assertEquals(gone.get(0), tracker.byEntityId(7));
	}

	@Test
	void vanishingFarAwayOrLongAfterThePotionIsNot() {
		InvisTracker far = new InvisTracker();
		for (int t = 0; t <= 30; t++) {
			far.see(sighting(false, true, t, InvisTracker.VANISH_RANGE + 1), true, t);
			far.endTick(A, t);
		}
		assertTrue(far.endTick(NONE, 31).isEmpty());

		InvisTracker late = new InvisTracker();
		run(late, false, true, true, 0, 5);
		run(late, false, false, true, 6, 6 + InvisTracker.POTION_WINDOW_TICKS);
		assertTrue(late.endTick(NONE, 7 + InvisTracker.POTION_WINDOW_TICKS).isEmpty());
	}

	@Test
	void dyingWithAPotionIsNotVanishing() {
		InvisTracker tracker = new InvisTracker();
		run(tracker, false, true, true, 0, 10);
		InvisTracker.Sighting dying = sighting(false, true, 11, 10);
		dying.alive = false;
		tracker.see(dying, true, 11);
		tracker.endTick(A, 11);
		assertTrue(tracker.endTick(NONE, 12).isEmpty());
	}

	@Test
	void flaggedThenGoneGivesOneMark() {
		InvisTracker tracker = new InvisTracker();
		run(tracker, false, true, true, 0, 10);
		assertNotNull(run(tracker, true, false, true, 11, 11 + InvisTracker.CONFIRM_TICKS));
		assertTrue(tracker.endTick(NONE, 30).isEmpty());
		assertEquals(1, tracker.marks().size());
	}

	@Test
	void withNeedPotionOnlyAPotionInHandCounts() {
		InvisTracker tracker = new InvisTracker();
		run(tracker, false, false, true, 0, 5);
		assertNull(run(tracker, true, false, true, 6, 40));
		assertTrue(tracker.marks().isEmpty());

		InvisTracker drank = new InvisTracker();
		run(drank, false, true, true, 0, 30);
		assertNotNull(run(drank, true, false, true, 31, 50));
	}

	@Test
	void lookPointsTheMinecraftWay() {
		double[] south = InvisTracker.look(0, 0);
		assertEquals(1, south[2], 1e-9);
		double[] west = InvisTracker.look(90, 0);
		assertEquals(-1, west[0], 1e-9);
		double[] down = InvisTracker.look(0, 90);
		assertEquals(-1, down[1], 1e-9);
	}
}
