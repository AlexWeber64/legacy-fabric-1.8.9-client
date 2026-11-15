package io.github.alexweber64.axolotlextras.hacks;

/**
 * Where a player goes on the radar: turns a world offset into radar pixels, optionally turned with your view, and keeps
 * dots that are out of range on the rim. No Minecraft imports, so it can be unit tested.
 */
public final class RadarMath {

	private RadarMath() {
	}

	/**
	 * A world offset (north is -z, east is +x) as radar pixels from the radar's middle, with y counted downwards.
	 *
	 * @param dx      offset east (blocks)
	 * @param dz      offset south (blocks)
	 * @param yaw     your view in Minecraft degrees (0 looks south, 90 looks west)
	 * @param rotate  true: your view points up; false: north points up
	 * @return {x, y} in blocks, still to be scaled
	 */
	public static double[] turn(double dx, double dz, double yaw, boolean rotate) {
		if (!rotate) {
			return new double[] {dx, dz};
		}
		double r = Math.toRadians(yaw);
		double sin = Math.sin(r), cos = Math.cos(r);
		// Forward is (-sin, cos) in x/z, your right is (-cos, -sin).
		double right = -dx * cos - dz * sin;
		double forward = -dx * sin + dz * cos;
		return new double[] {right, -forward};
	}

	/**
	 * Places a turned offset on a radar of the given radius.
	 *
	 * @param x       turned offset x in blocks (from {@link #turn})
	 * @param y       turned offset y in blocks
	 * @param range   how many blocks the radius stands for
	 * @param radius  the usable radius in pixels (the radar's half size less half an icon)
	 * @param circle  true for a round radar, false for a square one
	 * @param keepOut true: players out of range are kept on the rim; false: they are left out
	 * @return {x, y, outside} in pixels (outside is 1 when the player is out of range and was put on the rim), or null
	 *     when the player is out of range and {@code keepOut} is false
	 */
	public static double[] place(double x, double y, double range, double radius, boolean circle, boolean keepOut) {
		double scale = range <= 0 ? 0 : radius / range;
		double px = x * scale, py = y * scale;
		double reach = circle ? Math.sqrt(px * px + py * py) : Math.max(Math.abs(px), Math.abs(py));
		if (reach <= radius) {
			return new double[] {px, py, 0};
		}
		if (!keepOut) {
			return null;
		}
		double shrink = radius / reach;
		return new double[] {px * shrink, py * shrink, 1};
	}

	/**
	 * Whether the radar is centred on your bed right now.
	 *
	 * @param bedChosen   Radar Centre is set to Your Bed
	 * @param fallback    Back to You When Bed Breaks is on
	 * @param bedBroken   your bed was broken in this game (cleared when a new game starts)
	 */
	public static boolean centreOnBed(boolean bedChosen, boolean fallback, boolean bedBroken) {
		return bedChosen && !(fallback && bedBroken);
	}

	/** What a bed-centred radar can show. */
	public enum BedState {
		/** The bed is near enough: the players around it are loaded. */
		ACTIVE,
		/** The bed is known but beyond your render distance (or its chunk is not loaded): the radar goes gray. */
		OUT_OF_RANGE,
		/** No own bed known (not in a game, not found yet, or broken): the radar goes gray. */
		NO_BED
	}

	/**
	 * Whether a radar centred on your bed is live. Players are only sent to you within your render distance, so beyond it
	 * the radar would show an empty bed and mislead you.
	 *
	 * @param bedKnown       your own bed is known
	 * @param chunkLoaded    the bed's chunk is loaded on your side
	 * @param distance       horizontal blocks from you to the bed
	 * @param renderDistance your render distance in chunks
	 */
	public static BedState bedState(boolean bedKnown, boolean chunkLoaded, double distance, int renderDistance) {
		if (!bedKnown) {
			return BedState.NO_BED;
		}
		return chunkLoaded && distance <= Math.max(1, renderDistance) * 16.0 ? BedState.ACTIVE : BedState.OUT_OF_RANGE;
	}

	/**
	 * The height mark next to a face: 1 when the player is clearly above you, -1 when clearly below, 0 otherwise.
	 *
	 * @param dy      their height minus yours, in blocks
	 * @param minimum how many blocks count as clearly
	 */
	public static int heightMark(double dy, double minimum) {
		return dy > minimum ? 1 : dy < -minimum ? -1 : 0;
	}
}
