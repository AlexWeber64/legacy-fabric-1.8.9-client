package io.github.alexweber64.axolotlextras.hacks;

import java.util.ArrayList;
import java.util.List;

/**
 * Steps a projectile forward the way Minecraft 1.8.9 moves it, one tick at a time, until it hits something. No Minecraft
 * imports, so it can be unit tested.
 *
 * <p>Per tick, like the game: check the segment from here to here + velocity for a hit, move, add the acceleration (only
 * fireballs have one), multiply by the drag, then take away the gravity.
 */
public final class Trajectory {

	/** Pearls, snowballs and eggs. */
	public static final double THROWN_SPEED = 1.5, THROWN_GRAVITY = 0.03, THROWN_DRAG = 0.99;

	/** Arrows; the speed is three times the bow's charge. */
	public static final double ARROW_GRAVITY = 0.05, ARROW_DRAG = 0.99;

	/** Fireballs (no gravity). */
	public static final double FIREBALL_DRAG = 0.95;

	/** Finds where a segment first hits something. */
	public interface Collider {
		/** @return the hit point {x, y, z} (more numbers may follow, for the caller), or null when the segment is clear */
		double[] hit(double x1, double y1, double z1, double x2, double y2, double z2);
	}

	/** The points of a flight, and what ended it. */
	public static final class Path {
		/** {x, y, z} for the start, every tick, and the hit point last. */
		public final List<double[]> points;
		/** What the collider returned for the hit, or null when nothing was hit within the steps. */
		public final double[] hit;

		Path(List<double[]> points, double[] hit) {
			this.points = points;
			this.hit = hit;
		}

		public double[] end() {
			return points.get(points.size() - 1);
		}
	}

	private Trajectory() {
	}

	/**
	 * @param accel per tick acceleration (fireballs), or null
	 */
	public static Path simulate(double x, double y, double z, double vx, double vy, double vz, double gravity, double drag,
		double[] accel, int maxSteps, Collider collider) {
		List<double[]> points = new ArrayList<>();
		points.add(new double[] {x, y, z});
		for (int i = 0; i < maxSteps; i++) {
			double nx = x + vx, ny = y + vy, nz = z + vz;
			double[] hit = collider.hit(x, y, z, nx, ny, nz);
			if (hit != null) {
				points.add(new double[] {hit[0], hit[1], hit[2]});
				return new Path(points, hit);
			}
			x = nx;
			y = ny;
			z = nz;
			points.add(new double[] {x, y, z});
			if (accel != null) {
				vx += accel[0];
				vy += accel[1];
				vz += accel[2];
			}
			vx *= drag;
			vy *= drag;
			vz *= drag;
			vy -= gravity;
			if (y < -64) {
				break; // fell into the void
			}
		}
		return new Path(points, null);
	}

	/**
	 * How fast a bow shoots after being drawn for this many ticks, as the game works it out.
	 *
	 * @return the arrow speed in blocks per tick, or 0 when the bow would not shoot yet
	 */
	public static double bowSpeed(int drawTicks) {
		double f = drawTicks / 20.0;
		f = (f * f + f * 2) / 3;
		if (f < 0.1) {
			return 0;
		}
		return Math.min(f, 1) * 3;
	}

	/** The unit direction for a yaw and pitch in degrees (Minecraft's: yaw 0 looks south, pitch 90 looks down). */
	public static double[] direction(double yawDegrees, double pitchDegrees) {
		double yaw = Math.toRadians(yawDegrees), pitch = Math.toRadians(pitchDegrees);
		return new double[] {-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)};
	}

	/** The smallest distance between any point of the path and a position. */
	public static double closestApproach(Path path, double x, double y, double z) {
		double best = Double.MAX_VALUE;
		for (double[] p : path.points) {
			double dx = p[0] - x, dy = p[1] - y, dz = p[2] - z;
			best = Math.min(best, dx * dx + dy * dy + dz * dz);
		}
		return Math.sqrt(best);
	}
}
