package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexweber64.axolotlextras.hacks.Trajectory;
import org.junit.jupiter.api.Test;

class TrajectoryTest {

	/** A flat floor at y = 64 (the top of the blocks at y 63). */
	private static final Trajectory.Collider FLOOR = (x1, y1, z1, x2, y2, z2) -> {
		if (y2 >= 64 || y1 < 64) {
			return null;
		}
		double t = (y1 - 64) / (y1 - y2);
		return new double[] {x1 + (x2 - x1) * t, 64, z1 + (z2 - z1) * t};
	};

	private static final Trajectory.Collider NOTHING = (x1, y1, z1, x2, y2, z2) -> null;

	@Test
	void firstTickMovesByTheStartVelocity() {
		Trajectory.Path path = Trajectory.simulate(0, 100, 0, 1, 0, 0, Trajectory.THROWN_GRAVITY, Trajectory.THROWN_DRAG,
			null, 2, NOTHING);
		assertEquals(1, path.points.get(1)[0], 1e-9);
		assertEquals(100, path.points.get(1)[1], 1e-9);
		// second tick: velocity was multiplied by 0.99 and lost 0.03 to gravity
		assertEquals(1.99, path.points.get(2)[0], 1e-9);
		assertEquals(100 - 0.03, path.points.get(2)[1], 1e-9);
	}

	@Test
	void aThrowLandsOnTheFloor() {
		double[] dir = Trajectory.direction(0, -30); // looking south, 30 degrees up
		Trajectory.Path path = Trajectory.simulate(0, 65.52, 0, dir[0] * 1.5, dir[1] * 1.5, dir[2] * 1.5,
			Trajectory.THROWN_GRAVITY, Trajectory.THROWN_DRAG, null, 300, FLOOR);
		assertNotNull(path.hit);
		assertEquals(64, path.end()[1], 1e-9);
		assertTrue(path.end()[2] > 30, "a pearl thrown 30 degrees up flies well over 30 blocks: " + path.end()[2]);
		assertEquals(0, path.end()[0], 1e-6);
	}

	@Test
	void arrowsFallFasterThanPearls() {
		double[] dir = Trajectory.direction(0, 0);
		Trajectory.Path pearl = Trajectory.simulate(0, 70, 0, dir[0] * 3, dir[1] * 3, dir[2] * 3,
			Trajectory.THROWN_GRAVITY, Trajectory.THROWN_DRAG, null, 300, FLOOR);
		Trajectory.Path arrow = Trajectory.simulate(0, 70, 0, dir[0] * 3, dir[1] * 3, dir[2] * 3,
			Trajectory.ARROW_GRAVITY, Trajectory.ARROW_DRAG, null, 300, FLOOR);
		assertTrue(arrow.end()[2] < pearl.end()[2]);
	}

	@Test
	void fireballsFlyStraight() {
		Trajectory.Path path = Trajectory.simulate(0, 80, 0, 0, 0, 1, 0, Trajectory.FIREBALL_DRAG,
			new double[] {0, 0, 0.1}, 50, NOTHING);
		assertNull(path.hit);
		assertEquals(80, path.end()[1], 1e-9);
		assertEquals(0, path.end()[0], 1e-9);
	}

	@Test
	void bowSpeedFollowsTheCharge() {
		assertEquals(0, Trajectory.bowSpeed(1), 1e-9); // too short to shoot
		assertEquals(3, Trajectory.bowSpeed(20), 1e-9); // full charge
		assertEquals(3, Trajectory.bowSpeed(60), 1e-9);
		assertEquals((0.25 + 1.0) / 3 * 3, Trajectory.bowSpeed(10), 1e-9);
	}

	@Test
	void directionMatchesMinecraftAxes() {
		double[] south = Trajectory.direction(0, 0);
		assertEquals(1, south[2], 1e-9);
		double[] west = Trajectory.direction(90, 0);
		assertEquals(-1, west[0], 1e-9);
		double[] down = Trajectory.direction(0, 90);
		assertEquals(-1, down[1], 1e-9);
	}

	@Test
	void closestApproachFindsTheNearestPoint() {
		Trajectory.Path path = Trajectory.simulate(0, 80, -10, 0, 0, 1, 0, 1, null, 20, NOTHING);
		assertEquals(3, Trajectory.closestApproach(path, 3, 80, 0), 1e-9);
	}
}
