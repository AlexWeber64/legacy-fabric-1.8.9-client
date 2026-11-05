package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.alexweber64.axolotlextras.bedwars.DepositTracker;
import io.github.alexweber64.axolotlextras.bedwars.ResourceMath;
import org.junit.jupiter.api.Test;

class DepositTrackerTest {

	private static int[] inv(int iron, int gold, int diamond, int emerald) {
		return new int[] {iron, gold, diamond, emerald};
	}

	@Test
	void depositIsInferredFromTheInventoryDrop() {
		DepositTracker t = new DepositTracker();
		int[] ender = new int[ResourceMath.COUNT];
		t.update(inv(37, 12, 4, 0), 100, ender);
		t.onPunch(ResourceMath.IRON, 37, 101);
		assertEquals(37, t.update(inv(0, 12, 4, 0), 102, ender));
		assertArrayEquals(new int[] {37, 0, 0, 0}, ender);
	}

	@Test
	void depositIsCappedAtTheHeldCount() {
		DepositTracker t = new DepositTracker();
		int[] ender = new int[ResourceMath.COUNT];
		t.update(inv(64, 0, 0, 0), 1, ender);
		t.onPunch(ResourceMath.IRON, 10, 2);
		// 30 left the inventory (for example the player also dropped some), but only 10 were in hand.
		assertEquals(10, t.update(inv(34, 0, 0, 0), 3, ender));
		assertEquals(10, ender[ResourceMath.IRON]);
	}

	@Test
	void noDropMeansNoDeposit() {
		DepositTracker t = new DepositTracker();
		int[] ender = new int[ResourceMath.COUNT];
		t.update(inv(37, 0, 0, 0), 1, ender);
		t.onPunch(ResourceMath.IRON, 37, 2);
		assertEquals(0, t.update(inv(37, 0, 0, 0), 3, ender));
		assertArrayEquals(new int[] {0, 0, 0, 0}, ender);
	}

	@Test
	void aDropAfterTheWindowIsIgnored() {
		DepositTracker t = new DepositTracker();
		int[] ender = new int[ResourceMath.COUNT];
		t.update(inv(37, 0, 0, 0), 1, ender);
		t.onPunch(ResourceMath.IRON, 37, 2);
		t.update(inv(37, 0, 0, 0), 2 + DepositTracker.WINDOW_TICKS + 1, ender);
		assertEquals(0, t.update(inv(0, 0, 0, 0), 2 + DepositTracker.WINDOW_TICKS + 2, ender));
		assertEquals(0, ender[ResourceMath.IRON]);
	}

	@Test
	void aDropWithoutAPunchIsIgnored() {
		DepositTracker t = new DepositTracker();
		int[] ender = new int[ResourceMath.COUNT];
		t.update(inv(37, 0, 0, 0), 1, ender);
		assertEquals(0, t.update(inv(0, 0, 0, 0), 2, ender));
	}

	@Test
	void onlyThePunchedResourceCounts() {
		DepositTracker t = new DepositTracker();
		int[] ender = new int[ResourceMath.COUNT];
		t.update(inv(37, 12, 0, 0), 1, ender);
		t.onPunch(ResourceMath.GOLD, 12, 2);
		// Iron dropped (the player spent it), gold did not: nothing is deposited.
		assertEquals(0, t.update(inv(20, 12, 0, 0), 3, ender));
	}

	@Test
	void nonResourcePunchesAreIgnored() {
		DepositTracker t = new DepositTracker();
		int[] ender = new int[ResourceMath.COUNT];
		t.update(inv(37, 0, 0, 0), 1, ender);
		t.onPunch(-1, 5, 2);
		t.onPunch(ResourceMath.IRON, 0, 2);
		assertEquals(0, t.update(inv(0, 0, 0, 0), 3, ender));
	}

	@Test
	void clearPendingAndResetForgetThePunch() {
		DepositTracker t = new DepositTracker();
		int[] ender = new int[ResourceMath.COUNT];
		t.update(inv(37, 0, 0, 0), 1, ender);
		t.onPunch(ResourceMath.IRON, 37, 2);
		t.clearPending();
		assertEquals(0, t.update(inv(0, 0, 0, 0), 3, ender));

		t.update(inv(37, 0, 0, 0), 4, ender);
		t.onPunch(ResourceMath.IRON, 37, 5);
		t.reset();
		assertEquals(0, t.update(inv(0, 0, 0, 0), 6, ender));
	}
}
