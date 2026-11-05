package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.alexweber64.axolotlextras.bedwars.KdrMath;
import org.junit.jupiter.api.Test;

class KdrMathTest {

	@Test
	void ratioHasTwoDecimals() {
		assertEquals("2.50", KdrMath.ratio(10, 4));
		assertEquals("0.33", KdrMath.ratio(1, 3));
	}

	@Test
	void noDeathsGivesTheKillCount() {
		assertEquals("5.00", KdrMath.ratio(5, 0));
		assertEquals("0.00", KdrMath.ratio(0, 0));
	}

	@Test
	void levelHeadTextHasALabel() {
		assertEquals("\u00a77KDR: \u00a7f1.20", KdrMath.levelHead(6, 5));
	}
}
