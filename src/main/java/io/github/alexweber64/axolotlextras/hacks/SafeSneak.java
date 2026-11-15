package io.github.alexweber64.axolotlextras.hacks;

import java.util.Random;

/**
 * Safe Sneak: sneaks for you at the edge of a block, so you do not fall off while bridging, and stands up again a
 * moment after. Unlike the other hacks this changes what the server sees: it sends real sneak packets.
 *
 * <p>Each tick, while the conditions hold (on the ground, looking down far enough, holding blocks, not walking
 * forward), the caller asks whether the next step leaves the block: whether the player's box, shrunk by
 * {@link #inset()} on each side and moved by their motion and one block down, touches no block. If so it sneaks; after
 * the edge it keeps sneaking for a random time between the two hold options. The inset is also drawn at random for each
 * edge, so the sneak does not start at exactly the same spot every time. No Minecraft imports, so it can be unit tested.
 */
public final class SafeSneak {

	/** Is the next step off the edge, with the player's box shrunk by this much on each side? */
	public interface EdgeProbe {
		boolean offEdge(double inset);
	}

	/** The inset without randomness: the sneak starts when the player's middle is 0.1 blocks past the edge. */
	public static final double FIXED_INSET = 0.2;
	/** The random inset range: from 0.05 to 0.18 blocks past the edge, well before the box leaves the block (0.3). */
	public static final double MIN_INSET = 0.12, MAX_INSET = 0.25;

	private final Random random;
	private long holdUntil = Long.MIN_VALUE;
	private double randomInset;
	private double inset = FIXED_INSET;
	private boolean wasSneaking;

	public SafeSneak(Random random) {
		this.random = random;
		drawInset();
	}

	/** Forgets the hold (switched off, a menu opened, a new world). */
	public void reset() {
		if (wasSneaking) {
			drawInset();
		}
		holdUntil = Long.MIN_VALUE;
		wasSneaking = false;
	}

	/** The start spot for the next edge: drawn once per edge, not per tick (that would favour an early start). */
	private void drawInset() {
		randomInset = MIN_INSET + random.nextDouble() * (MAX_INSET - MIN_INSET);
	}

	/** The inset used for the current edge. */
	public double inset() {
		return inset;
	}

	/**
	 * One tick.
	 *
	 * @param active     the hack is on and every condition holds; when false nothing is forced (an ongoing hold ends)
	 * @param userSneaks you are holding the sneak key yourself: left alone
	 * @param probe      the edge check
	 * @param randomEdge draw the inset at random for each edge
	 * @param holdMinMs  the shortest hold after the edge (ms)
	 * @param holdMaxMs  the longest hold after the edge (ms)
	 * @param nowMs      the current time (ms)
	 * @return true when the player should sneak on this tick
	 */
	public boolean tick(boolean active, boolean userSneaks, EdgeProbe probe, boolean randomEdge, int holdMinMs,
		int holdMaxMs, long nowMs) {
		if (!active || userSneaks) {
			reset();
			return false;
		}
		inset = randomEdge ? randomInset : FIXED_INSET;
		boolean sneak;
		if (probe.offEdge(inset)) {
			holdUntil = nowMs + holdMs(holdMinMs, holdMaxMs);
			sneak = true;
		} else {
			sneak = nowMs < holdUntil;
		}
		if (wasSneaking && !sneak) {
			drawInset(); // this edge is done
		}
		wasSneaking = sneak;
		return sneak;
	}

	/** A random hold between the two bounds (either order), both included. */
	int holdMs(int a, int b) {
		int min = Math.max(0, Math.min(a, b)), max = Math.max(0, Math.max(a, b));
		return min + (max > min ? random.nextInt(max - min + 1) : 0);
	}
}
