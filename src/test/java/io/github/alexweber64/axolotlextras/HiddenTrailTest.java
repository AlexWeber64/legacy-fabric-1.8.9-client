package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexweber64.axolotlextras.hacks.HiddenTrail;
import io.github.alexweber64.axolotlextras.hacks.HiddenTrail.Kind;
import org.junit.jupiter.api.Test;

class HiddenTrailTest {

	@Test
	void crumbsSitInTheMiddleOfTheLowerBlock() {
		HiddenTrail trail = new HiddenTrail();
		// Feet on top of the block at y 64: the lower body block is 65.
		HiddenTrail.Crumb c = trail.add(10.3, 65.0, -3.8, Kind.STEP, 0, -1, null);
		assertEquals(10.5, c.x, 1e-9);
		assertEquals(65.5, c.y, 1e-9);
		assertEquals(-3.5, c.z, 1e-9);
	}

	@Test
	void theSameBlockRightAgainIsNotANewCrumb() {
		HiddenTrail trail = new HiddenTrail();
		assertNotNull(trail.add(1, 65, 1, Kind.STEP, 0, -1, null));
		assertNull(trail.add(1.4, 65, 1.2, Kind.STEP, 5, -1, null));
		assertNotNull(trail.add(1.4, 65, 1.2, Kind.STEP, HiddenTrail.SAME_BLOCK_TICKS, -1, null));
		assertEquals(2, trail.crumbs().size());
	}

	@Test
	void stepsLinkIntoAWalkAndTheNewestIsTheHead() {
		HiddenTrail trail = new HiddenTrail();
		trail.add(0, 65, 0, Kind.STEP, 0, -1, null);
		trail.add(2, 65, 0, Kind.STEP, 6, -1, null);
		trail.add(4, 65, 0, Kind.STEP, 12, -1, null);
		trail.add(40, 65, 0, Kind.STEP, 14, -1, null); // someone else
		trail.add(40, 70, 0, Kind.BREAK, 15, -1, null);
		assertTrue(HiddenTrail.linked(trail.crumbs().get(0), trail.crumbs().get(1)));
		assertFalse(HiddenTrail.linked(trail.crumbs().get(2), trail.crumbs().get(3)));
		assertFalse(HiddenTrail.linked(trail.crumbs().get(3), trail.crumbs().get(4)));
		assertEquals(3, trail.heads().size());
		assertEquals(4.5, trail.heads().get(0).x, 1e-9);
	}

	@Test
	void crumbsGoAfterTheirTime() {
		HiddenTrail trail = new HiddenTrail();
		trail.add(0, 65, 0, Kind.STEP, 0, -1, null);
		trail.add(3, 65, 0, Kind.STEP, 100, -1, null);
		trail.expire(200, 200);
		assertEquals(1, trail.crumbs().size());
		assertEquals(0.5, trail.crumbs().get(0).fade(200, 200), 1e-9);
	}

	@Test
	void aNewTrailComesAfterQuiet() {
		HiddenTrail trail = new HiddenTrail();
		assertTrue(trail.quietBefore(0));
		trail.add(0, 65, 0, Kind.STEP, 0, -1, null);
		assertFalse(trail.quietBefore(50));
		assertTrue(trail.quietBefore(HiddenTrail.QUIET_TICKS));
	}

	@Test
	void onlySoundsThatPlaceAPlayerCount() {
		assertEquals(Kind.STEP, HiddenTrail.soundKind("step.stone"));
		assertEquals(Kind.STEP, HiddenTrail.soundKind("step.cloth"));
		assertEquals(Kind.HURT, HiddenTrail.soundKind("game.player.hurt"));
		assertEquals(Kind.ACTION, HiddenTrail.soundKind("random.drink"));
		assertEquals(Kind.ACTION, HiddenTrail.soundKind("random.bow"));
		assertNull(HiddenTrail.soundKind("random.explode"));
		assertNull(HiddenTrail.soundKind("note.pling"));
		assertNull(HiddenTrail.soundKind(null));
	}

	@Test
	void theCrumbCountIsCapped() {
		HiddenTrail trail = new HiddenTrail();
		for (int i = 0; i < HiddenTrail.MAX_CRUMBS + 20; i++) {
			trail.add(i * 2, 65, 0, Kind.STEP, i, -1, null);
		}
		assertEquals(HiddenTrail.MAX_CRUMBS, trail.crumbs().size());
		assertEquals(40.5, trail.crumbs().get(0).x, 1e-9);
	}
}
