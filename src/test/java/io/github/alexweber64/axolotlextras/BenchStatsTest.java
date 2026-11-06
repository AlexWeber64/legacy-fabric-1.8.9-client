package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.alexweber64.axolotlextras.perf.BenchStats;
import org.junit.jupiter.api.Test;

class BenchStatsTest {

	@Test
	void steadyFramesGiveTheSameAverageAndLows() {
		double[] ms = new double[200];
		java.util.Arrays.fill(ms, 4.0);
		BenchStats s = BenchStats.compute(ms, ms.length);
		assertEquals(250.0, s.avgFps, 0.01);
		assertEquals(250.0, s.low1Fps, 0.01);
		assertEquals(4.0, s.worstMs, 0.001);
	}

	@Test
	void aFewSlowFramesPullTheOnePercentLowDown() {
		double[] ms = new double[200];
		java.util.Arrays.fill(ms, 5.0);
		ms[10] = 25.0;
		ms[90] = 35.0;
		BenchStats s = BenchStats.compute(ms, ms.length);
		// the slowest 1 percent of 200 frames is 2 frames: (25 + 35) / 2 = 30 ms
		assertEquals(1000.0 / 30.0, s.low1Fps, 0.01);
		assertEquals(35.0, s.worstMs, 0.001);
		assertEquals(200, s.frames);
	}

	@Test
	void onlyTheFirstCountEntriesAreUsed() {
		double[] ms = {4.0, 4.0, 100.0, 100.0};
		BenchStats s = BenchStats.compute(ms, 2);
		assertEquals(250.0, s.avgFps, 0.01);
	}

	@Test
	void noFramesGiveZeros() {
		assertEquals(0, BenchStats.compute(new double[0], 0).frames);
	}
}
