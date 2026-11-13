package io.github.alexweber64.axolotlextras.hacks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A breadcrumb trail of players you cannot see: footstep sounds, footstep particles, hurt, drink and bow sounds the
 * server sends from a spot where no entity is loaded on your side, and blocks being broken by an entity you do not have.
 * The server sends these for every player, also ones it no longer sends you (as it seems to do for invisible players).
 * Each crumb sits in the middle of the lower block of the two a player takes up. No Minecraft imports, so it can be unit
 * tested.
 */
public final class HiddenTrail {

	/** What gave a crumb away. */
	public enum Kind {
		/** A footstep sound or particle: they stood here. */
		STEP,
		/** A hurt sound: they got hit here. */
		HURT,
		/** Drinking, eating or a bow shot: they stood here. */
		ACTION,
		/** A block being broken: they are within reach of it, the crumb is the block itself. */
		BREAK
	}

	/** One sign of a hidden player. */
	public static final class Crumb {
		/** The middle of the block (for a step, the lower block of the two the player takes up). */
		public final double x, y, z;
		public final Kind kind;
		public final int tick;
		/** The team when known (from an invisibility mark), else {@code unknownTeam}. */
		public final int team;
		/** The name when known, else null. */
		public final String name;

		Crumb(double x, double y, double z, Kind kind, int tick, int team, String name) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.kind = kind;
			this.tick = tick;
			this.team = team;
			this.name = name;
		}

		/** 1 while fresh, going down to 0 at the end of {@code lifetime}. */
		public double fade(int now, int lifetime) {
			return Math.max(0, Math.min(1, 1 - (now - tick) / (double) Math.max(1, lifetime)));
		}
	}

	/** Two crumbs this near in blocks and time belong to the same walk. */
	public static final double LINK_DISTANCE = 4.5;
	public static final int LINK_TICKS = 40;
	/** The same block again this soon is not a new crumb (a step sound comes every few ticks). */
	public static final int SAME_BLOCK_TICKS = 10;
	/** A trail counts as new after this long without crumbs. */
	public static final int QUIET_TICKS = 100;
	/** At most this many crumbs are kept (the oldest go first), so a busy fight cannot slow the game down. */
	public static final int MAX_CRUMBS = 300;

	private final List<Crumb> crumbs = new ArrayList<>();
	private int lastAdded = Integer.MIN_VALUE / 2;

	public void reset() {
		crumbs.clear();
		lastAdded = Integer.MIN_VALUE / 2;
	}

	/**
	 * Adds a crumb at the block holding the given spot (for a sound or particle, the player's feet).
	 *
	 * @return the crumb, or null when the same block already got one just now
	 */
	public Crumb add(double x, double y, double z, Kind kind, int tick, int team, String name) {
		double cx = Math.floor(x) + 0.5, cy = Math.floor(y) + 0.5, cz = Math.floor(z) + 0.5;
		for (Crumb c : crumbs) {
			if (c.x == cx && c.y == cy && c.z == cz && c.kind == kind && tick - c.tick < SAME_BLOCK_TICKS) {
				return null;
			}
		}
		Crumb crumb = new Crumb(cx, cy, cz, kind, tick, team, name);
		if (crumbs.size() >= MAX_CRUMBS) {
			crumbs.remove(0);
		}
		crumbs.add(crumb);
		lastAdded = tick;
		return crumb;
	}

	/** True when nothing was added for {@link #QUIET_TICKS} before this tick (call before {@link #add}). */
	public boolean quietBefore(int tick) {
		return tick - lastAdded >= QUIET_TICKS;
	}

	/** Drops crumbs older than {@code lifetime} ticks. */
	public void expire(int tick, int lifetime) {
		for (Iterator<Crumb> it = crumbs.iterator(); it.hasNext(); ) {
			if (tick - it.next().tick >= lifetime) {
				it.remove();
			}
		}
	}

	/** Oldest first. */
	public List<Crumb> crumbs() {
		return crumbs;
	}

	/** {@code later} follows {@code earlier} on the same walk (breaks are spots, not steps, and are never linked). */
	public static boolean linked(Crumb earlier, Crumb later) {
		if (earlier.kind == Kind.BREAK || later.kind == Kind.BREAK) {
			return false;
		}
		int dt = later.tick - earlier.tick;
		if (dt < 0 || dt > LINK_TICKS) {
			return false;
		}
		double dx = later.x - earlier.x, dy = later.y - earlier.y, dz = later.z - earlier.z;
		return dx * dx + dy * dy + dz * dz <= LINK_DISTANCE * LINK_DISTANCE;
	}

	/** The newest crumb of each walk: no later crumb is linked to it. These get the label. */
	public List<Crumb> heads() {
		List<Crumb> heads = new ArrayList<>();
		for (int i = 0; i < crumbs.size(); i++) {
			Crumb c = crumbs.get(i);
			boolean followed = false;
			for (int j = i + 1; j < crumbs.size() && !followed; j++) {
				followed = linked(c, crumbs.get(j));
			}
			if (!followed) {
				heads.add(c);
			}
		}
		return heads;
	}

	/** The kind of a sound the server played, or null when it says nothing about where a player is. */
	public static Kind soundKind(String sound) {
		if (sound == null) {
			return null;
		}
		if (sound.startsWith("step.")) {
			return Kind.STEP;
		}
		if (sound.equals("game.player.hurt") || sound.startsWith("game.player.hurt.")) {
			return Kind.HURT;
		}
		if (sound.equals("random.drink") || sound.equals("random.eat") || sound.equals("random.burp")
			|| sound.equals("random.bow")) {
			return Kind.ACTION;
		}
		return null;
	}
}
