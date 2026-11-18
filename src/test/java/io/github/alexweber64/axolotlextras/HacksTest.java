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
import io.github.alexweber64.axolotlextras.hacks.TeamGuess;
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

	// ------------------------------------------------------------------------------------------------------ teams

	@Test
	void innerObsidianLayerHasEightSpots() {
		World world = new World();
		world.layer(1, OBSIDIAN);
		BedDefense.Cover cover = BedDefense.cover(0, 64, 0, 1, 0, world, OBSIDIAN);
		assertEquals(1, cover.layer);
		assertEquals(8, cover.spots);
		assertTrue(cover.complete());
	}

	@Test
	void teamFromPrefixLeavesSpectatorsOut() {
		assertEquals(0, TeamGuess.fromTeamPrefix("§c§lR §cSteve"));
		assertEquals(1, TeamGuess.fromTeamPrefix("§9§lB §r§9WalterSpalter"));
		assertEquals(7, TeamGuess.fromTeamPrefix("§8§lS §8Alex"));
		assertEquals(5, TeamGuess.fromTeamPrefix("§f§lW §r§fSteve"));
		assertEquals(5, TeamGuess.fromTeamPrefix("§lW §rSteve"));
		assertEquals(5, TeamGuess.fromTeamPrefix("W Steve"));
		// Spectators and eliminated players: plain gray, no letter.
		assertEquals(TeamGuess.UNKNOWN, TeamGuess.fromTeamPrefix("§7nufikfufik"));
		// Lobby names start with a rank.
		assertEquals(TeamGuess.UNKNOWN, TeamGuess.fromTeamPrefix("§a[VIP] Cyverno"));
		// Letter and colour must agree, and the letter needs its space.
		assertEquals(TeamGuess.UNKNOWN, TeamGuess.fromTeamPrefix("§a§lR §aSteve"));
		assertEquals(TeamGuess.UNKNOWN, TeamGuess.fromTeamPrefix("§cRob"));
		assertEquals(TeamGuess.UNKNOWN, TeamGuess.fromTeamPrefix("R Steve"));
		assertEquals(TeamGuess.UNKNOWN, TeamGuess.fromTeamPrefix(null));
		// Gray spectators count as Gray only while names have no prefixes at all.
		assertEquals(TeamGuess.UNKNOWN, TeamGuess.ofPlayer("§7nufikfufik", true));
		assertEquals(7, TeamGuess.ofPlayer("§7nufikfufik", false));
		assertEquals(1, TeamGuess.ofPlayer("§9§lB §9Bob", true));
	}

	@Test
	void rushMessageMarksTeammates() {
		assertEquals("§c§lRush §r§9§lB §9Steve §f15m from your bed", HackMath.rushMessage("§9§lB §9Steve", false, 15));
		assertEquals("§c§lRush §r§b§lA §bAlex §a[Teammate] §f12m from your bed",
			HackMath.rushMessage("§b§lA §bAlex", true, 12));
	}

	@Test
	void teamFromNamesAndDyes() {
		assertEquals(0, TeamGuess.fromFormattedName("§c§lR §cSteve"));
		assertEquals(7, TeamGuess.fromFormattedName("§8§lS §8Alex"));
		assertEquals(TeamGuess.UNKNOWN, TeamGuess.fromFormattedName("Plain"));
		assertEquals(1, TeamGuess.fromDye(11));
		assertEquals(2, TeamGuess.fromDye(5));
		assertEquals(TeamGuess.UNKNOWN, TeamGuess.fromDye(12));
		assertEquals("§cRed", TeamGuess.colored(0));
	}

	@Test
	void clearWoolMajorityDecides() {
		int[] blocks = new int[TeamGuess.COUNT];
		int[] players = new int[TeamGuess.COUNT];
		blocks[1] = 20; // blue wool
		blocks[0] = 4; // some red bridge wool
		players[0] = 2; // two red players rushing it
		assertEquals(1, TeamGuess.guess(blocks, players));
	}

	@Test
	void playersDecideWhenWoolIsUnclear() {
		int[] blocks = new int[TeamGuess.COUNT];
		int[] players = new int[TeamGuess.COUNT];
		blocks[1] = 5;
		blocks[0] = 4;
		players[3] = 1;
		assertEquals(3, TeamGuess.guess(blocks, players));
		assertEquals(TeamGuess.UNKNOWN, TeamGuess.guess(new int[TeamGuess.COUNT], new int[TeamGuess.COUNT]));
	}

	@Test
	void sidebarYouLineGivesTheOwnTeam() {
		assertEquals(0, TeamGuess.fromSidebarLine("§cR §fRed: §a✔ §7YOU"));
		assertEquals(2, TeamGuess.fromSidebarLine("G Green: 3 YOU"));
		assertEquals(7, TeamGuess.fromSidebarLine("S Gray: ✘ YOU"));
		assertEquals(TeamGuess.UNKNOWN, TeamGuess.fromSidebarLine("§aG §fGreen: §a✔"));
		assertEquals(TeamGuess.UNKNOWN, TeamGuess.fromSidebarLine("Players: 8/8"));
		assertEquals(TeamGuess.UNKNOWN, TeamGuess.fromSidebarLine("www.hypixel.net"));
		assertEquals(TeamGuess.UNKNOWN, TeamGuess.fromSidebarLine(null));
	}

	@Test
	void npcsHaveVersionTwoUuids() {
		assertTrue(HackMath.isNpc(java.util.UUID.fromString("a1b2c3d4-0000-2000-8000-000000000000")));
		assertFalse(HackMath.isNpc(java.util.UUID.fromString("a1b2c3d4-0000-4000-8000-000000000000")));
		assertFalse(HackMath.isNpc(null));
	}

	@Test
	void optionNumbersAreReadFromTheEnd() {
		assertEquals(64, HackMath.optionNumber("axolotlextras.range.64", 1), 1e-9);
		assertEquals(7, HackMath.optionNumber("axolotlextras.range.all", 7), 1e-9);
		assertEquals(7, HackMath.optionNumber(null, 7), 1e-9);
	}

	@Test
	void redToGreenRunsFromRedToGreen() {
		assertEquals(0xFF0020, HackMath.redToGreen(0));
		assertEquals(0x00FF20, HackMath.redToGreen(1));
		assertEquals(0xFFFF20, HackMath.redToGreen(0.5));
		assertEquals(0x00FF20, HackMath.redToGreen(3));
	}
}
