package io.github.alexweber64.axolotlextras.hacks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Notices enemies going invisible and keeps a mark where it happened, for {@link #LIFETIME_TICKS}. Two ways count:
 * <ul>
 * <li>Flagged: a player seen visible turns invisible on your side and stays so for {@link #CONFIRM_TICKS} (a one-tick
 * flicker, such as a death or respawn, is not reported).</li>
 * <li>Vanished: a player who held an invisibility potion just before disappears from your side while alive and well within
 * range. Some servers stop sending invisible players at all (Hypixel seems to), so this is the usual case there.</li>
 * </ul>
 * With {@code needPotion}, a flagged player must also have held the potion just before. Players who walk out of range
 * are only reported when they held the potion and were near, so leaving render distance never gives a mark by itself;
 * when they come back invisible they count as first seen invisible and give no mark. No Minecraft imports, so it can be
 * unit tested.
 */
public final class InvisTracker {

	/** How long a mark stays: 60 seconds. */
	public static final int LIFETIME_TICKS = 1200;
	/** How long a player has to stay invisible before the mark counts: half a second. */
	public static final int CONFIRM_TICKS = 10;
	/**
	 * How long before going invisible the potion must have been in their hand: drinking takes 1.6 seconds and the bottle
	 * turns to glass as the effect starts, so the potion is seen in the ticks just before. Three seconds of slack.
	 */
	public static final int POTION_WINDOW_TICKS = 60;
	/**
	 * A player who disappears nearer than this (blocks) did not walk out of range: servers send players much farther out.
	 */
	public static final double VANISH_RANGE = 32;

	/** One enemy as seen on a tick. */
	public static final class Sighting {
		public String id;
		public int entityId;
		public String name;
		public int team;
		public boolean invisible;
		public boolean holdingPotion;
		/** False while they are dying (the body stays a moment before it is removed). */
		public boolean alive = true;
		public double x, y, z;
		public float yaw, pitch;
		/** Blocks from you. */
		public double distance;
	}

	/** Where a player went invisible. */
	public static final class Mark {
		public final String id;
		public final int entityId;
		public final String name;
		public final int team;
		public final double x, y, z;
		/** Where they were looking (Minecraft degrees). */
		public final float yaw, pitch;
		/** The tick they went invisible. */
		public final int tick;
		/** True when they disappeared from your side (the server stopped sending them), false when only flagged. */
		public final boolean vanished;

		Mark(Sighting s, int tick, boolean vanished) {
			this.id = s.id;
			this.entityId = s.entityId;
			this.name = s.name;
			this.team = s.team;
			this.x = s.x;
			this.y = s.y;
			this.z = s.z;
			this.yaw = s.yaw;
			this.pitch = s.pitch;
			this.tick = tick;
			this.vanished = vanished;
		}

		/** Whole seconds since they went invisible. */
		public int secondsAgo(int now) {
			return Math.max(0, (now - tick) / 20);
		}

		/** 1 while fresh, going down to 0 over the last five seconds. */
		public double fade(int now) {
			int left = LIFETIME_TICKS - (now - tick);
			return Math.max(0, Math.min(1, left / 100.0));
		}
	}

	/** Each player's last sighting (a copy). */
	private final Map<String, Sighting> last = new HashMap<>();
	/** The last tick each player was seen holding an invisibility potion. */
	private final Map<String, Integer> lastPotion = new HashMap<>();
	/** Players that just turned invisible, waiting for {@link #CONFIRM_TICKS}. */
	private final Map<String, Mark> pending = new HashMap<>();
	/** Confirmed marks, newest last, one per player. */
	private final List<Mark> marks = new ArrayList<>();

	public void reset() {
		last.clear();
		lastPotion.clear();
		pending.clear();
		marks.clear();
	}

	/**
	 * One enemy as seen on this tick.
	 *
	 * @param needPotion only confirm a flagged player when they held a potion within {@link #POTION_WINDOW_TICKS} before
	 * @return the mark when it was just confirmed (time for the chat line), else null
	 */
	public Mark see(Sighting s, boolean needPotion, int tick) {
		if (s.holdingPotion) {
			lastPotion.put(s.id, tick);
		}
		Sighting before = last.put(s.id, copy(s));
		if (!s.invisible || !s.alive) {
			pending.remove(s.id);
			return null;
		}
		if (before != null && !before.invisible) {
			pending.put(s.id, new Mark(s, tick, false));
			return null;
		}
		Mark waiting = pending.get(s.id);
		if (waiting == null || tick - waiting.tick < CONFIRM_TICKS) {
			return null;
		}
		pending.remove(s.id);
		if (needPotion && !potionBefore(s.id, waiting.tick)) {
			return null; // invisible without a potion seen: not reported
		}
		return keep(waiting);
	}

	/**
	 * Ends a tick: players not seen on it are forgotten, except for their marks. One who held the potion just before,
	 * was alive and was near when last seen gets a vanished mark at that spot. Marks older than {@link #LIFETIME_TICKS}
	 * go.
	 *
	 * @return the marks confirmed by vanishing on this tick (time for the chat line), maybe empty
	 */
	public List<Mark> endTick(Set<String> seen, int tick) {
		List<Mark> vanished = Collections.emptyList();
		for (Iterator<Map.Entry<String, Sighting>> it = last.entrySet().iterator(); it.hasNext(); ) {
			Sighting s = it.next().getValue();
			if (seen.contains(s.id)) {
				continue;
			}
			it.remove();
			if (s.alive && s.distance < VANISH_RANGE && potionBefore(s.id, tick) && !markedSince(s.id, tick - POTION_WINDOW_TICKS)) {
				if (vanished.isEmpty()) {
					vanished = new ArrayList<>();
				}
				vanished.add(keep(new Mark(s, tick, true)));
			}
		}
		lastPotion.keySet().retainAll(seen);
		pending.keySet().retainAll(seen);
		for (Iterator<Mark> it = marks.iterator(); it.hasNext(); ) {
			if (tick - it.next().tick >= LIFETIME_TICKS) {
				it.remove();
			}
		}
		return vanished;
	}

	public List<Mark> marks() {
		return marks;
	}

	/** The mark of the player with this entity id, or null. */
	public Mark byEntityId(int entityId) {
		for (Mark mark : marks) {
			if (mark.entityId == entityId) {
				return mark;
			}
		}
		return null;
	}

	private boolean potionBefore(String id, int tick) {
		Integer potion = lastPotion.get(id);
		return potion != null && potion >= tick - POTION_WINDOW_TICKS;
	}

	/** This player already got a mark at or after {@code tick} (flagged first, then gone: one mark is enough). */
	private boolean markedSince(String id, int tick) {
		for (Mark mark : marks) {
			if (mark.id.equals(id) && mark.tick >= tick) {
				return true;
			}
		}
		return false;
	}

	private Mark keep(Mark mark) {
		for (Iterator<Mark> it = marks.iterator(); it.hasNext(); ) {
			if (it.next().id.equals(mark.id)) {
				it.remove();
			}
		}
		marks.add(mark);
		return mark;
	}

	private static Sighting copy(Sighting s) {
		Sighting c = new Sighting();
		c.id = s.id;
		c.entityId = s.entityId;
		c.name = s.name;
		c.team = s.team;
		c.invisible = s.invisible;
		c.holdingPotion = s.holdingPotion;
		c.alive = s.alive;
		c.x = s.x;
		c.y = s.y;
		c.z = s.z;
		c.yaw = s.yaw;
		c.pitch = s.pitch;
		c.distance = s.distance;
		return c;
	}

	/** The way someone looks, as a unit vector {x, y, z} (yaw 0 looks south, +z; pitch 90 looks down). */
	public static double[] look(float yaw, float pitch) {
		double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
		return new double[] {-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p)};
	}
}
