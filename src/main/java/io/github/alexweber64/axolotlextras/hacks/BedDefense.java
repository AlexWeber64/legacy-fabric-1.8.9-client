package io.github.alexweber64.axolotlextras.hacks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Works out what a bed is defended with: every block in a dome around the two bed halves (at bed height and above, up to
 * {@link #RADIUS} blocks away counted along the axes), grouped by block type and ordered from the inside out. No
 * Minecraft imports, so it can be unit tested.
 */
public final class BedDefense {

	/** How far out the dome reaches, in blocks (counted as |dx| + |dy| + |dz| from the nearest bed half). */
	public static final int RADIUS = 5;

	/** How many block types are kept, the innermost first. */
	public static final int MAX_TYPES = 7;

	/** Looks up a block. Returns 0 for air and for anything that should not count (the bed itself, grass, flowers). */
	public interface Lookup {
		int at(int x, int y, int z);
	}

	/** One block type in the defense. */
	public static final class Layer {
		/** The caller's key for the block type (any non-zero number). */
		public final int key;
		public int count;
		private int distanceSum;

		Layer(int key) {
			this.key = key;
		}

		/** @return how far from the bed these blocks are on average */
		public double meanDistance() {
			return count == 0 ? 0 : (double) distanceSum / count;
		}
	}

	/** The result for one bed. */
	public static final class Result {
		/** Block types, innermost first. */
		public final List<Layer> layers;
		/** True when some face of the bed (top or side) touches air, so it can be broken without digging. */
		public final boolean exposed;

		Result(List<Layer> layers, boolean exposed) {
			this.layers = layers;
			this.exposed = exposed;
		}
	}

	private BedDefense() {
	}

	/**
	 * @param fx foot half of the bed
	 * @param hx head half of the bed (next to the foot, at the same height)
	 */
	public static Result analyse(int fx, int fy, int fz, int hx, int hz, Lookup world) {
		Map<Integer, Layer> byKey = new HashMap<>();
		forEachSpot(fx, fy, fz, hx, hz, (x, y, z, d) -> {
			int key = world.at(x, y, z);
			if (key == 0) {
				return;
			}
			Layer layer = byKey.get(key);
			if (layer == null) {
				layer = new Layer(key);
				byKey.put(key, layer);
			}
			layer.count++;
			layer.distanceSum += d;
		});
		List<Layer> layers = new ArrayList<>(byKey.values());
		Collections.sort(layers, (a, b) -> {
			int byDistance = Double.compare(a.meanDistance(), b.meanDistance());
			return byDistance != 0 ? byDistance : Integer.compare(b.count, a.count);
		});
		if (layers.size() > MAX_TYPES) {
			layers = new ArrayList<>(layers.subList(0, MAX_TYPES));
		}
		return new Result(layers, exposed(fx, fy, fz, hx, hz, world));
	}

	/** How well one block type (obsidian) covers a bed. */
	public static final class Cover {
		/** The layer (distance from the bed) that holds most of the blocks. */
		public final int layer;
		/** How many spots that layer has, and how many of them hold the block. */
		public final int spots, covered;
		/** How many of the uncovered spots are air (the rest hold some other block). */
		public final int openToAir;

		Cover(int layer, int spots, int covered, int openToAir) {
			this.layer = layer;
			this.spots = spots;
			this.covered = covered;
			this.openToAir = openToAir;
		}

		/** @return true when every spot of the layer holds the block, so there is no way to the bed around it */
		public boolean complete() {
			return covered == spots;
		}
	}

	/**
	 * Checks whether a block type (meant for obsidian) forms a full layer around the bed. The layer is the distance that
	 * holds the most of that block (the inner one on a tie), and every spot at that distance (bed height and above) is
	 * checked.
	 *
	 * @return null when the block is not anywhere around the bed
	 */
	public static Cover cover(int fx, int fy, int fz, int hx, int hz, Lookup world, int key) {
		int[] perLayer = new int[RADIUS + 1];
		forEachSpot(fx, fy, fz, hx, hz, (x, y, z, d) -> {
			if (world.at(x, y, z) == key) {
				perLayer[d]++;
			}
		});
		int best = 0;
		for (int d = 1; d <= RADIUS; d++) {
			if (perLayer[d] > perLayer[best]) {
				best = d;
			}
		}
		if (best == 0) {
			return null;
		}
		final int layer = best;
		int[] counts = new int[3]; // spots, covered, air
		forEachSpot(fx, fy, fz, hx, hz, (x, y, z, d) -> {
			if (d != layer) {
				return;
			}
			counts[0]++;
			int at = world.at(x, y, z);
			if (at == key) {
				counts[1]++;
			} else if (at == 0) {
				counts[2]++;
			}
		});
		return new Cover(layer, counts[0], counts[1], counts[2]);
	}

	private interface Spot {
		void at(int x, int y, int z, int distance);
	}

	/** Every spot of the dome (bed height and above, distance 1 to {@link #RADIUS}), with its distance from the bed. */
	private static void forEachSpot(int fx, int fy, int fz, int hx, int hz, Spot spot) {
		for (int x = Math.min(fx, hx) - RADIUS; x <= Math.max(fx, hx) + RADIUS; x++) {
			for (int z = Math.min(fz, hz) - RADIUS; z <= Math.max(fz, hz) + RADIUS; z++) {
				for (int y = fy; y <= fy + RADIUS; y++) {
					int d = Math.min(Math.abs(x - fx) + Math.abs(z - fz), Math.abs(x - hx) + Math.abs(z - hz)) + (y - fy);
					if (d >= 1 && d <= RADIUS) {
						spot.at(x, y, z, d);
					}
				}
			}
		}
	}

	/** The faces that matter: the top of each half and the three outward sides of each half. */
	private static boolean exposed(int fx, int fy, int fz, int hx, int hz, Lookup world) {
		int[][] halves = {{fx, fz}, {hx, hz}};
		int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for (int[] half : halves) {
			if (world.at(half[0], fy + 1, half[1]) == 0) {
				return true;
			}
			for (int[] side : sides) {
				int x = half[0] + side[0];
				int z = half[1] + side[1];
				boolean otherHalf = (x == fx && z == fz) || (x == hx && z == hz);
				if (!otherHalf && world.at(x, fy, z) == 0) {
					return true;
				}
			}
		}
		return false;
	}
}
