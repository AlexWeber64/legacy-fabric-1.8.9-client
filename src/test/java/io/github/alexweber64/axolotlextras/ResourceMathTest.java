package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexweber64.axolotlextras.bedwars.ResourceMath;
import org.junit.jupiter.api.Test;

class ResourceMathTest {

	@Test
	void enderChestDepositsGiveTheNewTotal() {
		// Lines from the play clone's logs.
		assertArrayEquals(new int[] {ResourceMath.IRON, 24},
			ResourceMath.enderChestDeposit("Deposited x24 Iron Ingot into Ender Chest! (24 Total)"));
		assertArrayEquals(new int[] {ResourceMath.GOLD, 6},
			ResourceMath.enderChestDeposit("Deposited x6 Gold Ingot into Ender Chest! (6 Total)"));
		assertArrayEquals(new int[] {ResourceMath.DIAMOND, 3},
			ResourceMath.enderChestDeposit("§aDeposited x1 §bDiamond §ainto Ender Chest! §7(3 Total)"));
		// With AxolotlClient's game clock in front.
		assertArrayEquals(new int[] {ResourceMath.EMERALD, 2},
			ResourceMath.enderChestDeposit("§70:23§r Deposited x2 Emerald into Ender Chest! (2 Total)"));
	}

	@Test
	void otherDepositsAreNotCounted() {
		assertNull(ResourceMath.enderChestDeposit("Deposited x4 Gold Ingot into Team Chest! (4 Total)"));
		assertNull(ResourceMath.enderChestDeposit("Deposited x16 Red Wool into Ender Chest! (16 Total)"));
		assertNull(ResourceMath.enderChestDeposit("Deposited x1 Normal Lucky Block into Ender Chest! (1 Total)"));
		assertNull(ResourceMath.enderChestDeposit("You purchased Wool"));
		assertNull(ResourceMath.enderChestDeposit(null));
	}

	@Test
	void chestMismatchNamesBothCounts() {
		assertNull(ResourceMath.chestMismatch(new int[] {24, 6, 0, 0}, new int[] {24, 6, 0, 0}));
		assertEquals("expected iron 24, gold 6, diamond 0, emerald 0; found iron 20, gold 6, diamond 0, emerald 0",
			ResourceMath.chestMismatch(new int[] {24, 6, 0, 0}, new int[] {20, 6, 0, 0}));
	}

	@Test
	void indexOfKnownItems() {
		assertEquals(ResourceMath.IRON, ResourceMath.indexOf("iron_ingot"));
		assertEquals(ResourceMath.GOLD, ResourceMath.indexOf("minecraft:gold_ingot"));
		assertEquals(ResourceMath.DIAMOND, ResourceMath.indexOf("diamond"));
		assertEquals(ResourceMath.EMERALD, ResourceMath.indexOf("emerald"));
	}

	@Test
	void indexOfUnknownItems() {
		assertEquals(-1, ResourceMath.indexOf("stick"));
		assertEquals(-1, ResourceMath.indexOf("iron_sword"));
		assertEquals(-1, ResourceMath.indexOf(null));
	}

	@Test
	void rowFormatsLikeTheRustClient() {
		assertEquals("37 + 32 = 69", ResourceMath.row(37, 32, true, true));
		assertEquals("37 + 32", ResourceMath.row(37, 32, true, false));
		assertEquals("37", ResourceMath.row(37, 32, false, true));
		assertEquals("0 + 0 = 0", ResourceMath.row(0, 0, true, true));
	}

	@Test
	void rowDoesNotOverflow() {
		assertEquals("2147483647 + 2147483647 = 4294967294", ResourceMath.row(Integer.MAX_VALUE, Integer.MAX_VALUE, true, true));
	}

	@Test
	void stripsFormattingCodes() {
		assertEquals("BED WARS", ResourceMath.stripFormatting("§e§lBED WARS"));
		// An incomplete code at the very end is kept, like vanilla's own control code stripper does.
		assertEquals("a§", ResourceMath.stripFormatting("a§"));
		assertEquals("", ResourceMath.stripFormatting(null));
	}

	@Test
	void detectsBedWars() {
		assertTrue(ResourceMath.isBedWarsTitle("§e§lBED WARS"));
		assertTrue(ResourceMath.isBedWarsTitle("Bed Wars"));
		assertFalse(ResourceMath.isBedWarsTitle("§e§lSKYWARS"));
		assertFalse(ResourceMath.isBedWarsTitle(null));
	}

	@Test
	void detectsGameStartAndEnderChest() {
		assertTrue(ResourceMath.isGameStartMessage("Protect your bed and destroy the enemy beds."));
		assertFalse(ResourceMath.isGameStartMessage("hello"));
		assertTrue(ResourceMath.isEnderChestTitle("§fEnder Chest"));
		assertFalse(ResourceMath.isEnderChestTitle("Chest"));
	}
}
