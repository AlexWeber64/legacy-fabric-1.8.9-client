package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.alexweber64.axolotlextras.ui.RoundedRect;
import org.junit.jupiter.api.Test;

class RoundedRectTest {

	@Test
	void optionValuesMapToCornerShapes() {
		assertEquals(0, RoundedRect.insetsFor("axolotlextras.corners.off").length);
		assertEquals(1, RoundedRect.insetsFor("axolotlextras.corners.small").length);
		assertEquals(4, RoundedRect.insetsFor("axolotlextras.corners.large").length);
		assertEquals(0, RoundedRect.insetsFor("something else").length);
	}

	@Test
	void aRoundedBoxIsTopRowsMiddleBlockAndBottomRows() {
		int[][] s = RoundedRect.strips(10, 20, 50, 30, new int[] {2, 1});
		assertEquals(5, s.length);
		assertArrayEquals(new int[] {12, 20, 46, 1}, s[0]);
		assertArrayEquals(new int[] {11, 21, 48, 1}, s[1]);
		assertArrayEquals(new int[] {10, 22, 50, 26}, s[2]);
		assertArrayEquals(new int[] {11, 48, 48, 1}, s[3]);
		assertArrayEquals(new int[] {12, 49, 46, 1}, s[4]);
	}

	@Test
	void theStripsCoverTheBoxExactlyOnce() {
		int[][] s = RoundedRect.strips(0, 0, 40, 20, new int[] {3, 2, 1, 1});
		int covered = 0;
		for (int[] strip : s) {
			covered += strip[2] * strip[3];
		}
		// 40 * 20 minus the four corner notches: 2 * (3 + 2 + 1 + 1) per side pair of rows
		assertEquals(40 * 20 - 2 * 2 * (3 + 2 + 1 + 1), covered);
	}

	@Test
	void aTooSmallBoxStaysSquare() {
		int[][] s = RoundedRect.strips(0, 0, 4, 4, new int[] {3, 2, 1, 1});
		assertEquals(1, s.length);
		assertArrayEquals(new int[] {0, 0, 4, 4}, s[0]);
	}
}
