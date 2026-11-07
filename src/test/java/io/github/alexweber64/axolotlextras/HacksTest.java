package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import io.github.alexweber64.axolotlextras.bedwars.ResourceMath;
import io.github.alexweber64.axolotlextras.hacks.BedDefense;
import io.github.alexweber64.axolotlextras.hacks.HackMath;
import io.github.alexweber64.axolotlextras.hacks.ResourceClusters;
import org.junit.jupiter.api.Test;

class HacksTest {

	private static final int WOOL = 35 << 4;
	private static final int END_STONE = 121 << 4;
	private static final int OBSIDIAN = 49 << 4;

	/** A tiny world: only the blocks put in it, air everywhere else. */
	private static final class World implements BedDefense.Lookup {
		final Map<String, Integer> blocks = new HashMap<>();

		void set(int x, int y, int z, int key) {
			blocks.put(x + "," + y + "," + z, key);
		}

		@Override
		public int at(int x, int y, int z) {
			Integer key = blocks.get(x + "," + y + "," + z);
			return key == null ? 0 : key;
		}

		/** Fills every spot at exactly this distance from a bed with feet at (0, 64, 0) and head at (1, 64, 0). */
		void layer(int distance, int key) {
			for (int x = -6; x <= 7; x++) {
				for (int z = -6; z <= 6; z++) {
					for (int y = 64; y <= 70; y++) {
						int d = Math.min(Math.abs(x) + Math.abs(z), Math.abs(x - 1) + Math.abs(z)) + (y - 64);
						if (d == distance) {
							set(x, y, z, key);
						}
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------------------------------------ resource piles

	@Test
	void itemsCloseTogetherFormOnePile() {
		ResourceClusters piles = new ResourceClusters();
		piles.add(10.5, 64, 10.5, ResourceMath.IRON, 12);
		piles.add(11.2, 64, 9.8, ResourceMath.IRON, 5);
		piles.add(10.9, 64.2, 10.1, ResourceMath.GOLD, 3);
		assertEquals(1, piles.clusters().size());
		ResourceClusters.Cluster pile = piles.clusters().get(0);
		assertEquals(17, pile.counts[ResourceMath.IRON]);
		assertEquals(3, pile.counts[ResourceMath.GOLD]);
		assertEquals(20, pile.total());
		assertEquals(64.2, pile.topY(), 1e-9);
	}

	@Test
	void farApartItemsFormSeparatePiles() {
		ResourceClusters piles = new ResourceClusters();
		piles.add(0, 70, 0, ResourceMath.DIAMOND, 2);
		piles.add(40, 70, 0, ResourceMath.EMERALD, 1);
		piles.add(0, 90, 0, ResourceMath.DIAMOND, 1);
		assertEquals(3, piles.clusters().size());
	}

	@Test
	void otherItemsAreIgnored() {
		ResourceClusters piles = new ResourceClusters();
		piles.add(0, 70, 0, -1, 64);
		piles.add(0, 70, 0, ResourceMath.IRON, 0);
		assertTrue(piles.clusters().isEmpty());
	}

	// ---------------------------------------------------------------------------------------------------- bed defense

	@Test
	void layersComeInsideOut() {
		World world = new World();
		world.layer(1, END_STONE);
		world.layer(2, WOOL);
		BedDefense.Result result = BedDefense.analyse(0, 64, 0, 1, 0, world);
		assertEquals(2, result.layers.size());
		assertEquals(END_STONE, result.layers.get(0).key);
		assertEquals(8, result.layers.get(0).count);
		assertEquals(WOOL, result.layers.get(1).key);
		assertFalse(result.exposed);
	}

	@Test
	void aBareBedIsExposed() {
		BedDefense.Result result = BedDefense.analyse(0, 64, 0, 1, 0, new World());
		assertTrue(result.layers.isEmpty());
		assertTrue(result.exposed);
	}

	@Test
	void oneMissingSideBlockMakesItExposed() {
		World world = new World();
		world.layer(1, WOOL);
		world.set(-1, 64, 0, 0);
		assertTrue(BedDefense.analyse(0, 64, 0, 1, 0, world).exposed);
	}

	@Test
	void fullObsidianLayerIsComplete() {
		World world = new World();
		world.layer(1, WOOL);
		world.layer(2, OBSIDIAN);
		BedDefense.Cover cover = BedDefense.cover(0, 64, 0, 1, 0, world, OBSIDIAN);
		assertNotNull(cover);
		assertEquals(2, cover.layer);
		assertTrue(cover.complete());
		assertEquals(cover.spots, cover.covered);
	}

	@Test
	void obsidianWithGapsIsMisplaced() {
		World world = new World();
		world.layer(2, OBSIDIAN);
		world.set(-2, 64, 0, WOOL); // one spot is wool
		world.set(0, 66, 0, 0); // one spot on top is air
		BedDefense.Cover cover = BedDefense.cover(0, 64, 0, 1, 0, world, OBSIDIAN);
		assertNotNull(cover);
		assertFalse(cover.complete());
		assertEquals(2, cover.spots - cover.covered);
		assertEquals(1, cover.openToAir);
	}

	@Test
	void noObsidianGivesNoCover() {
		World world = new World();
		world.layer(1, WOOL);
		assertNull(BedDefense.cover(0, 64, 0, 1, 0, world, OBSIDIAN));
	}

	// ----------------------------------------------------------------------------------------------------- math

	@Test
	void teamColourIsTheFirstColourCode() {
		assertEquals(0xFF5555, HackMath.teamColor("§c§lR §cSteve"));
		assertEquals(0x5555FF, HackMath.teamColor("§l§9B §9Alex"));
		assertEquals(0xFFAA00, HackMath.teamColor("§6Gold"));
		assertEquals(-1, HackMath.teamColor("Plain"));
		assertEquals(-1, HackMath.teamColor(null));
	}

	@Test
	void compassFollowsMinecraftAxes() {
		assertEquals("N", HackMath.compass(0, -10));
		assertEquals("S", HackMath.compass(0, 10));
		assertEquals("E", HackMath.compass(10, 0));
		assertEquals("W", HackMath.compass(-10, 0));
		assertEquals("NE", HackMath.compass(10, -10));
		assertEquals("SW", HackMath.compass(-10, 10));
	}

	@Test
	void projectionMapsTheMiddleAndRejectsPointsBehind() {
		float[] identity = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
		// A projection that divides by -z, like a camera looking down -z.
		float[] perspective = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, -1, -1, 0, 0, -0.1f, 0};
		int[] viewport = {0, 0, 200, 100};
		assertArrayEquals(new double[] {100, 50}, HackMath.project(0, 0, -5, identity, perspective, viewport), 1e-6);
		assertArrayEquals(new double[] {150, 50}, HackMath.project(2.5, 0, -5, identity, perspective, viewport), 1e-6);
		assertNull(HackMath.project(0, 0, 5, identity, perspective, viewport));
	}
}
