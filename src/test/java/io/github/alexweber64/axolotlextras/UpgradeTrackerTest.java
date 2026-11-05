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
	void unknownPricesAreNotGuessed() {
		UpgradeTracker t = new UpgradeTracker();
		t.onMessage("Steve purchased Cushioned Boots I");
		assertEquals(-1, t.upgradeRows(SMALL).get(0).cost);
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
