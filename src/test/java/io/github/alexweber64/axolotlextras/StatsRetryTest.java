package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexweber64.axolotlextras.bedwars.StatsRetry;
import org.junit.jupiter.api.Test;

class StatsRetryTest {

	@Test
	void asksOnlyOnEveryTenthCall() {
		StatsRetry retry = new StatsRetry();
		for (int i = 1; i < StatsRetry.EVERY; i++) {
			assertFalse(retry.shouldAsk());
		}
		assertTrue(retry.shouldAsk());
		assertFalse(retry.shouldAsk());
	}

	@Test
	void givesUpAfterTheMaximumNumberOfAsks() {
		StatsRetry retry = new StatsRetry();
		int asked = 0;
		for (int i = 0; i < StatsRetry.EVERY * StatsRetry.MAX_ASKS * 3; i++) {
			if (retry.shouldAsk()) {
				asked++;
			}
		}
		assertEquals(StatsRetry.MAX_ASKS, asked);
		assertEquals(StatsRetry.MAX_ASKS, retry.asks());
		assertFalse(retry.shouldAsk());
	}
}
