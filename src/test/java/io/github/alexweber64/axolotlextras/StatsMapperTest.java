package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.alexweber64.axolotlextras.bedwars.StatsBackoff;
import io.github.alexweber64.axolotlextras.bedwars.StatsMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StatsMapperTest {

	@Test
	void numbersGoInTheOrderAxolotlClientsConstructorTakes() {
		Map<String, Integer> bw = new HashMap<>();
		bw.put("final_kills_bedwars", 11);
		bw.put("final_deaths_bedwars", 12);
		bw.put("beds_broken_bedwars", 13);
		bw.put("deaths_bedwars", 14);
		bw.put("kills_bedwars", 15);
		bw.put("losses_bedwars", 16);
		bw.put("wins_bedwars", 17);
		bw.put("winstreak", 18);
		assertArrayEquals(new int[] {11, 12, 13, 14, 15, 0, 0, 0, 0, 0, 16, 17, 18, 99}, StatsMapper.constructorArgs(bw, 99));
	}

	@Test
	void aPlayerWhoNeverPlayedBedWarsGetsZerosAndStarOne() {
		assertArrayEquals(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1}, StatsMapper.constructorArgs(null, null));
		assertArrayEquals(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1}, StatsMapper.constructorArgs(new HashMap<String, Integer>(), null));
	}

	@Test
	void backoffDoublesAndStopsGrowing() {
		assertEquals(500, StatsBackoff.delayMillis(1));
		assertEquals(1000, StatsBackoff.delayMillis(2));
		assertEquals(2000, StatsBackoff.delayMillis(3));
		assertEquals(4000, StatsBackoff.delayMillis(4));
		assertEquals(4000, StatsBackoff.delayMillis(9));
	}

	@Test
	void rateLimitPauseFollowsHypixelButStaysInBounds() {
		assertEquals(10_000, StatsBackoff.rateLimitPauseMillis(-1));
		assertEquals(1_000, StatsBackoff.rateLimitPauseMillis(0));
		assertEquals(105_000, StatsBackoff.rateLimitPauseMillis(105));
		assertEquals(300_000, StatsBackoff.rateLimitPauseMillis(5000));
	}
}
