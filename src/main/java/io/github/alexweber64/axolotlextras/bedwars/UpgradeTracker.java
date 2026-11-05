package io.github.alexweber64.axolotlextras.bedwars;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Remembers which Bed Wars team upgrades and traps your team bought, from the "Name purchased Thing" chat lines.
 * Pure logic, no Minecraft classes.
 *
 * <p>Costs of the next tier are only filled in where they are known. A null cost table means "not known", and the
 * display then shows no cost rather than a guess.
 */
public final class UpgradeTracker {

	/** Team size group, because Bed Wars prices differ between solo/doubles and 3v3v3v3/4v4v4v4. */
	public enum Mode {
		SMALL_TEAMS, BIG_TEAMS
	}

	/** One line of the "Upgrades" list. cost is -1 when there is no next tier or its price is not known. */
	public static final class Row {
		public final String name;
		public final int cost;

		Row(String name, int cost) {
			this.name = name;
			this.cost = cost;
		}
	}

	private static final Pattern PURCHASE = Pattern.compile("^[A-Za-z0-9_]{3,16} purchased (.+?)\\s*$");
	private static final Pattern TRAP = Pattern.compile("^(.+?) Trap!?$");
	private static final String[] FORGES = {"Iron Forge", "Golden Forge", "Emerald Forge", "Molten Forge"};

	/** Diamond prices, tier by tier. Only Reinforced Armor is filled in, the rest were not verified. */
	private static final int[] ARMOR_SMALL = {2, 4, 8, 16};
	private static final int[] ARMOR_BIG = {5, 10, 20, 30};

	private boolean sharpness;
	private int armor;
	private int miner;
	private int forge;
	private boolean healPool;
	private boolean dragonBuff;
	private int boots;
	private final List<String> traps = new ArrayList<>();

	public void reset() {
		sharpness = false;
		armor = 0;
		miner = 0;
		forge = 0;
		healPool = false;
		dragonBuff = false;
		boots = 0;
		traps.clear();
	}

	/** @return true when the chat line changed what is remembered */
	public boolean onMessage(String rawChatLine) {
		String line = ResourceMath.stripFormatting(rawChatLine).trim();
		if (line.contains("Trap was set off!")) {
			if (!traps.isEmpty()) {
				traps.remove(0);
				return true;
			}
			return false;
		}
		Matcher purchase = PURCHASE.matcher(line);
		if (!purchase.matches()) {
			return false;
		}
		String item = purchase.group(1);
		Matcher trap = TRAP.matcher(item);
		if (trap.matches()) {
			traps.add(trapName(trap.group(1)));
			return true;
		}
		if (item.equals("Sharpened Swords")) {
			sharpness = true;
		} else if (item.startsWith("Reinforced Armor")) {
			armor++;
		} else if (item.startsWith("Maniac Miner")) {
			miner++;
		} else if (item.startsWith("Cushioned Boots")) {
			boots++;
		} else if (item.equals("Heal Pool")) {
			healPool = true;
		} else if (item.equals("Dragon Buff")) {
			dragonBuff = true;
		} else {
			int tier = forgeTier(item);
			if (tier == 0) {
				return false;
			}
			forge = Math.max(forge, tier);
		}
		return true;
	}

	/** Hypixel's first trap is announced as "It's a" plus " Trap!", the display name is its effect. */
	static String trapName(String captured) {
		String name = captured.trim();
		if (name.toLowerCase().startsWith("it's a")) {
			return "Blindness Trap";
		}
		return name + " Trap";
	}

	private static int forgeTier(String item) {
		for (int i = 0; i < FORGES.length; i++) {
			if (FORGES[i].equals(item)) {
				return i + 1;
			}
		}
		return 0;
	}

	public List<Row> upgradeRows(Mode mode) {
		List<Row> rows = new ArrayList<>();
		if (sharpness) {
			rows.add(new Row("Sharpened Swords", -1));
		}
		if (armor > 0) {
			rows.add(new Row("Reinforced Armor " + armor, cost(mode == Mode.BIG_TEAMS ? ARMOR_BIG : ARMOR_SMALL, armor)));
		}
		if (miner > 0) {
			rows.add(new Row("Maniac Miner " + miner, -1));
		}
		if (forge > 0) {
			rows.add(new Row(FORGES[forge - 1], -1));
		}
		if (healPool) {
			rows.add(new Row("Heal Pool", -1));
		}
		if (dragonBuff) {
			rows.add(new Row("Dragon Buff", -1));
		}
		if (boots > 0) {
			rows.add(new Row("Cushioned Boots " + boots, -1));
		}
		return rows;
	}

	public List<String> trapNames() {
		return new ArrayList<>(traps);
	}

	private static int cost(int[] table, int level) {
		return table != null && level >= 0 && level < table.length ? table[level] : -1;
	}
}
