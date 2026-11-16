package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import io.github.alexweber64.axolotlextras.bedwars.UpgradeTracker;
import io.github.alexweber64.axolotlextras.bedwars.UpgradeTracker.Row;
import org.junit.jupiter.api.Test;

class UpgradeTrackerTest {

	private static final UpgradeTracker.Mode SMALL = UpgradeTracker.Mode.SMALL_TEAMS;

	@Test
	void purchasesAreRemembered() {
		UpgradeTracker t = new UpgradeTracker();
		assertTrue(t.onMessage("§aSteve§7 purchased Sharpened Swords"));
		assertTrue(t.onMessage("Steve purchased Cushioned Boots I"));
		assertTrue(t.onMessage("Steve purchased Heal Pool"));
		List<Row> rows = t.upgradeRows(SMALL);
		assertEquals(3, rows.size());
		assertEquals("Sharpened Swords", rows.get(0).name);
		assertEquals("Heal Pool", rows.get(1).name);
		assertEquals("Cushioned Boots 1", rows.get(2).name);
	}

	@Test
	void tiersCountUpAndShowTheNextPrice() {
		UpgradeTracker t = new UpgradeTracker();
		t.onMessage("Steve purchased Reinforced Armor I");
		assertEquals("Reinforced Armor 1", t.upgradeRows(SMALL).get(0).name);
		assertEquals(4, t.upgradeRows(SMALL).get(0).cost);
		assertEquals(10, t.upgradeRows(UpgradeTracker.Mode.BIG_TEAMS).get(0).cost);
		t.onMessage("Steve purchased Reinforced Armor II");
		t.onMessage("Steve purchased Reinforced Armor III");
		t.onMessage("Steve purchased Reinforced Armor IV");
		assertEquals(-1, t.upgradeRows(SMALL).get(0).cost);
	}

	@Test
	void minerAndForgeShowTheNextPriceForBothTeamSizes() {
		UpgradeTracker t = new UpgradeTracker();
		t.onMessage("Steve purchased Maniac Miner I");
		t.onMessage("Steve purchased Iron Forge");
		UpgradeTracker.Mode big = UpgradeTracker.Mode.BIG_TEAMS;
		assertEquals(4, t.upgradeRows(SMALL).get(0).cost);
		assertEquals(6, t.upgradeRows(big).get(0).cost);
		assertEquals(4, t.upgradeRows(SMALL).get(1).cost);
		assertEquals(8, t.upgradeRows(big).get(1).cost);
		t.onMessage("Steve purchased Maniac Miner II");
		t.onMessage("Steve purchased Golden Forge");
		t.onMessage("Steve purchased Emerald Forge");
		t.onMessage("Steve purchased Molten Forge");
		assertEquals(-1, t.upgradeRows(SMALL).get(0).cost);
		assertEquals(-1, t.upgradeRows(SMALL).get(1).cost);
	}

	@Test
	void singleLevelUpgradesHaveNoNextPrice() {
		UpgradeTracker t = new UpgradeTracker();
		t.onMessage("Steve purchased Sharpened Swords");
		t.onMessage("Steve purchased Heal Pool");
		t.onMessage("Steve purchased Dragon Buff");
		for (UpgradeTracker.Row row : t.upgradeRows(SMALL)) {
			assertEquals(-1, row.cost);
		}
	}

	@Test
	void bootsPricesDependOnTeamSize() {
		UpgradeTracker t = new UpgradeTracker();
		t.onMessage("Steve purchased Cushioned Boots I");
		assertEquals(2, t.upgradeRows(SMALL).get(0).cost);
		assertEquals(4, t.upgradeRows(UpgradeTracker.Mode.BIG_TEAMS).get(0).cost);
		t.onMessage("Steve purchased Cushioned Boots II");
		assertEquals(-1, t.upgradeRows(SMALL).get(0).cost);
	}

	@Test
	void noPricesWhileTheTeamSizeIsUnknown() {
		UpgradeTracker t = new UpgradeTracker();
		t.onMessage("Steve purchased Cushioned Boots I");
		t.onMessage("Steve purchased Reinforced Armor I");
		for (UpgradeTracker.Row row : t.upgradeRows(null)) {
			assertEquals(-1, row.cost);
		}
	}

	@Test
	void forgeShowsItsHighestTier() {
		UpgradeTracker t = new UpgradeTracker();
		t.onMessage("Steve purchased Iron Forge");
		t.onMessage("Steve purchased Golden Forge");
		assertEquals("Golden Forge", t.upgradeRows(SMALL).get(0).name);
	}

	@Test
	void trapsAreListedAndRemovedWhenSetOff() {
		UpgradeTracker t = new UpgradeTracker();
		assertTrue(t.onMessage("Steve purchased It's a Trap!"));
		assertTrue(t.onMessage("Steve purchased Alarm Trap"));
		assertEquals(Arrays.asList("Blindness Trap", "Alarm Trap"), t.trapNames());
		assertTrue(t.onMessage("Your Trap was set off!"));
		assertEquals(Arrays.asList("Alarm Trap"), t.trapNames());
	}

	@Test
	void luckyBlockUnlocksCountWithTheirLevel() {
		// Lines from a Lucky Blocks game on 2026-11-16: a block gave Reinforced Armor I, a teammate's gave II.
		UpgradeTracker t = new UpgradeTracker();
		assertTrue(t.onMessage("Cyverno unlocked Reinforced Armor I"));
		assertTrue(t.onMessage("Wolff27 unlocked Reinforced Armor II"));
		assertTrue(t.onMessage("uwureach unlocked Maniac Miner I"));
		assertTrue(t.onMessage("Wolff27 unlocked Heal Pool"));
		List<Row> rows = t.upgradeRows(SMALL);
		assertEquals("Reinforced Armor 2", rows.get(0).name);
		assertEquals(8, rows.get(0).cost);
		assertEquals("Maniac Miner 1", rows.get(1).name);
		assertEquals("Heal Pool", rows.get(2).name);
	}

	@Test
	void theLevelComesFromTheLineNotFromCounting() {
		UpgradeTracker t = new UpgradeTracker();
		// The line for level I was missed (you reconnected): level III is still level III.
		t.onMessage("Steve purchased Reinforced Armor III");
		assertEquals("Reinforced Armor 3", t.upgradeRows(SMALL).get(0).name);
		// A late or repeated line never lowers it.
		t.onMessage("Steve purchased Reinforced Armor II");
		assertEquals("Reinforced Armor 3", t.upgradeRows(SMALL).get(0).name);
		assertEquals(3, UpgradeTracker.nextLevel("Cushioned Boots", 2));
	}

	@Test
	void armorAndSwordsRaiseTheLevel() {
		UpgradeTracker t = new UpgradeTracker();
		assertTrue(t.seeArmorProtection(2));
		assertFalse(t.seeArmorProtection(1));
		assertFalse(t.seeArmorProtection(2));
		assertEquals("Reinforced Armor 2", t.upgradeRows(SMALL).get(0).name);
		assertTrue(t.seeSharpSwords());
		assertFalse(t.seeSharpSwords());
		assertEquals("Sharpened Swords", t.upgradeRows(SMALL).get(0).name);
	}

	@Test
	void clockPrefixAndPlacedTraps() {
		UpgradeTracker t = new UpgradeTracker();
		assertTrue(t.onMessage("§70:32§r Cyverno unlocked Reinforced Armor I"));
		assertTrue(t.onMessage("A Slow trap has been placed for your team!"));
		assertEquals(Arrays.asList("Slow Trap"), t.trapNames());
		assertFalse(t.onMessage("Steve unlocked the Wool"));
	}

	@Test
	void playerChatCannotSpoofAPurchase() {
		UpgradeTracker t = new UpgradeTracker();
		assertFalse(t.onMessage("[VIP] Steve: Steve purchased Heal Pool"));
		assertFalse(t.onMessage("Steve: purchased Heal Pool"));
		assertTrue(t.upgradeRows(SMALL).isEmpty());
	}

	@Test
	void unrelatedLinesAndResetDoNothing() {
		UpgradeTracker t = new UpgradeTracker();
		assertFalse(t.onMessage("Steve purchased Wool"));
		assertFalse(t.onMessage("Hello there"));
		t.onMessage("Steve purchased Heal Pool");
		t.reset();
		assertTrue(t.upgradeRows(SMALL).isEmpty());
		assertTrue(t.trapNames().isEmpty());
	}
}
