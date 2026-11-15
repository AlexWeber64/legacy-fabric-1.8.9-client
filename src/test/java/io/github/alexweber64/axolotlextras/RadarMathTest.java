package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.alexweber64.axolotlextras.hacks.RadarMath;
import org.junit.jupiter.api.Test;

class RadarMathTest {

	private static final double EPS = 1e-9;

	@Test
	void northUpKeepsTheWorldAxes() {
		double[] p = RadarMath.turn(5, -7, 123, false);
		assertEquals(5, p[0], EPS);
		assertEquals(-7, p[1], EPS);
	}

	@Test
	void lookingSouthPutsSouthUpAndWestRight() {
		double[] ahead = RadarMath.turn(0, 10, 0, true);
		assertEquals(0, ahead[0], EPS);
		assertEquals(-10, ahead[1], EPS);
		double[] west = RadarMath.turn(-10, 0, 0, true);
		assertEquals(10, west[0], EPS);
		assertEquals(0, west[1], EPS);
	}

	@Test
	void lookingNorthPutsNorthUpAndEastRight() {
		double[] ahead = RadarMath.turn(0, -10, 180, true);
		assertEquals(0, ahead[0], EPS);
		assertEquals(-10, ahead[1], EPS);
		double[] east = RadarMath.turn(10, 0, 180, true);
		assertEquals(10, east[0], EPS);
		assertEquals(0, east[1], EPS);
	}

	@Test
	void lookingWestPutsWestUpAndNorthRight() {
		double[] ahead = RadarMath.turn(-10, 0, 90, true);
		assertEquals(0, ahead[0], EPS);
		assertEquals(-10, ahead[1], EPS);
		double[] north = RadarMath.turn(0, -10, 90, true);
		assertEquals(10, north[0], EPS);
		assertEquals(0, north[1], EPS);
	}

	@Test
	void inRangeIsScaled() {
		double[] p = RadarMath.place(16, -32, 64, 50, true, true);
		assertEquals(12.5, p[0], EPS);
		assertEquals(-25, p[1], EPS);
		assertEquals(0, p[2], EPS);
	}

	@Test
	void outOfRangeGoesOnTheCircleRim() {
		double[] p = RadarMath.place(60, 80, 50, 40, true, true);
		assertEquals(40, Math.hypot(p[0], p[1]), EPS);
		assertEquals(0.75, p[0] / p[1], EPS);
		assertEquals(1, p[2], EPS);
	}

	@Test
	void squareRimUsesTheLargerSide() {
		// Inside the square's corner but outside the circle.
		double[] corner = RadarMath.place(45, 45, 50, 50, false, false);
		assertEquals(45, corner[0], EPS);
		assertEquals(0, corner[2], EPS);
		assertNull(RadarMath.place(45, 45, 50, 50, true, false));
		double[] out = RadarMath.place(100, 50, 50, 50, false, true);
		assertEquals(50, out[0], EPS);
		assertEquals(25, out[1], EPS);
	}

	@Test
	void outOfRangeIsLeftOutWhenAsked() {
		assertNull(RadarMath.place(0, 100, 50, 40, true, false));
	}

	@Test
	void bedRadarIsLiveOnlyWithinRenderDistance() {
		assertEquals(RadarMath.BedState.NO_BED, RadarMath.bedState(false, true, 0, 8));
		assertEquals(RadarMath.BedState.ACTIVE, RadarMath.bedState(true, true, 128, 8));
		assertEquals(RadarMath.BedState.OUT_OF_RANGE, RadarMath.bedState(true, true, 129, 8));
		// Within the distance but the chunk is not loaded (just teleported, or the server sends fewer chunks).
		assertEquals(RadarMath.BedState.OUT_OF_RANGE, RadarMath.bedState(true, false, 20, 8));
	}

	@Test
	void aBrokenBedSendsTheRadarBackToYou() {
		assertEquals(true, RadarMath.centreOnBed(true, true, false));
		assertEquals(false, RadarMath.centreOnBed(true, true, true));
		// Switched off: it stays on the bed (gray, "No bed").
		assertEquals(true, RadarMath.centreOnBed(true, false, true));
		assertEquals(false, RadarMath.centreOnBed(false, true, false));
	}

	@Test
	void heightMarks() {
		assertEquals(1, RadarMath.heightMark(4, 3));
		assertEquals(-1, RadarMath.heightMark(-4, 3));
		assertEquals(0, RadarMath.heightMark(2.9, 3));
	}
}
