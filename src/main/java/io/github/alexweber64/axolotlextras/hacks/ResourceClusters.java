package io.github.alexweber64.axolotlextras.hacks;

import java.util.ArrayList;
import java.util.List;

import io.github.alexweber64.axolotlextras.bedwars.ResourceMath;

/**
 * Groups dropped iron, gold, diamonds and emeralds into piles, one pile per generator. Items that lie within a few blocks
 * of each other belong to the same pile. No Minecraft imports, so it can be unit tested.
 */
public final class ResourceClusters {

	/** How far (in blocks, per horizontal axis) an item may lie from a pile's first item and still count for it. */
	static final double HORIZONTAL_REACH = 2.5;

	/** The same, up and down. */
	static final double VERTICAL_REACH = 2.0;

	/** One pile of resources on the ground. */
	public static final class Cluster {
		/** Item counts per resource, indexed like {@link ResourceMath} (iron, gold, diamond, emerald). */
		public final int[] counts = new int[ResourceMath.COUNT];
		final double anchorX, anchorY, anchorZ;
		private double sumX, sumZ;
		private int items;
		private double topY;

		Cluster(double x, double y, double z) {
			anchorX = x;
			anchorY = y;
			anchorZ = z;
			topY = y;
		}

		void add(double x, double y, double z, int resource, int count) {
			counts[resource] += count;
			sumX += x;
			sumZ += z;
			items++;
			topY = Math.max(topY, y);
		}

		boolean reaches(double x, double y, double z) {
			return Math.abs(x - anchorX) <= HORIZONTAL_REACH && Math.abs(z - anchorZ) <= HORIZONTAL_REACH
				&& Math.abs(y - anchorY) <= VERTICAL_REACH;
		}

		/** @return the middle of the pile, across */
		public double centerX() {
			return sumX / items;
		}

		public double centerZ() {
			return sumZ / items;
		}

		/** @return the height of the highest item in the pile */
		public double topY() {
			return topY;
		}

		public int total() {
			int t = 0;
			for (int c : counts) {
				t += c;
			}
			return t;
		}
	}

	private final List<Cluster> clusters = new ArrayList<>();

	/** Forgets the piles of the previous pass. */
	public void clear() {
		clusters.clear();
	}

	/**
	 * Adds one item entity on the ground.
	 *
	 * @param resource the resource index from {@link ResourceMath}, anything else is ignored
	 * @param count    the stack size of the item entity (merged stacks carry their full count)
	 */
	public void add(double x, double y, double z, int resource, int count) {
		if (resource < 0 || resource >= ResourceMath.COUNT || count <= 0) {
			return;
		}
		for (Cluster cluster : clusters) {
			if (cluster.reaches(x, y, z)) {
				cluster.add(x, y, z, resource, count);
				return;
			}
		}
		Cluster cluster = new Cluster(x, y, z);
		cluster.add(x, y, z, resource, count);
		clusters.add(cluster);
	}

	public List<Cluster> clusters() {
		return clusters;
	}
}
