package io.github.alexweber64.axolotlextras.bedwars;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Remembers which Bed Wars team upgrades and traps your team has, from the "Name purchased Thing" chat lines and the
 * "Name unlocked Thing" lines Lucky Blocks writes for an upgrade from a block. Levels are read from the line ("Reinforced
 * Armor II" is level 2), so a missed line does not shift the count, and the armor and sword enchantments can raise them
 * (see {@link #seeArmorProtection} and {@link #seeSharpSwords}). Pure logic, no Minecraft classes.
 *
 * <p>Costs of the next tier come from a list of Hypixel's diamond prices and the Cushioned Boots prices, both given by the
 * player. When the team size is not known (mode null) no price is shown.
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

	/** "Steve purchased Reinforced Armor II", or "Steve unlocked Reinforced Armor II" (Lucky Blocks); AxolotlClient's chat
	 * clock ("2:05 ") may come first. */
	private static final Pattern PURCHASE = Pattern.compile("^(?:\\d+:\\d\\d )?[A-Za-z0-9_]{3,16} (?:purchased|unlocked) (.+?)\\s*$");
	/** Lucky Blocks: "A Slow trap has been placed for your team!" */
	private static final Pattern PLACED_TRAP = Pattern.compile("^(?:\\d+:\\d\\d )?An? (.+?) trap has been placed for your team!$");
	private static final Pattern LEVEL = Pattern.compile(" (I|II|III|IV|V)$");
	private static final Pattern TRAP = Pattern.compile("^(.+?) Trap!?$");
	private static final String[] FORGES = {"Iron Forge", "Golden Forge", "Emerald Forge", "Molten Forge"};

	/** Diamond price of each tier, in order. "Small" is solo and doubles, "big" is 3v3v3v3 and 4v4v4v4. */
	private static final int[] ARMOR_SMALL = {2, 4, 8, 16};
	private static final int[] ARMOR_BIG = {5, 10, 20, 30};
	private static final int[] MINER_SMALL = {2, 4};
	private static final int[] MINER_BIG = {4, 6};
	private static final int[] FORGE_SMALL = {2, 4, 6, 8};
	private static final int[] FORGE_BIG = {4, 8, 12, 16};
	private static final int[] BOOTS_SMALL = {1, 2};
	private static final int[] BOOTS_BIG = {3, 4};

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
		Matcher placed = PLACED_TRAP.matcher(line);
		if (placed.matches()) {
			traps.add(placed.group(1) + " Trap");
			return true;
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
			armor = nextLevel(item, armor);
		} else if (item.startsWith("Maniac Miner")) {
			miner = nextLevel(item, miner);
		} else if (item.startsWith("Cushioned Boots")) {
			boots = nextLevel(item, boots);
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

	/**
	 * The level after this line: the Roman numeral at its end ("Reinforced Armor III" is 3), never lower than the level
	 * already known; without a numeral, one more than before.
	 */
	public static int nextLevel(String item, int current) {
		Matcher level = LEVEL.matcher(item);
		if (!level.find()) {
			return current + 1;
		}
		return Math.max(current, roman(level.group(1)));
	}

	private static int roman(String numeral) {
		switch (numeral) {
			case "I": return 1;
			case "II": return 2;
			case "III": return 3;
			case "IV": return 4;
			default: return 5;
		}
	}

	/**
	 * The lowest Protection level on your four armor pieces. Reinforced Armor puts Protection on all of them, so this is
	 * at least the team's level (a single enchanted piece from a Lucky Block does not count). Only raises the level.
	 *
	 * @return true when the level went up
	 */
	public boolean seeArmorProtection(int lowestProtection) {
		int level = Math.min(lowestProtection, ARMOR_SMALL.length);
		if (level > armor) {
			armor = level;
			return true;
		}
		return false;
	}

	/**
	 * Called when every sword you carry has Sharpness (at least one sword): Sharpened Swords enchants them all.
	 *
	 * @return true when Sharpened Swords was not known yet
	 */
	public boolean seeSharpSwords() {
		if (sharpness) {
			return false;
		}
		sharpness = true;
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

	/** @param mode the team size group, or null when it is not known yet (then no prices are given) */
	public List<Row> upgradeRows(Mode mode) {
		List<Row> rows = new ArrayList<>();
		if (sharpness) {
			rows.add(new Row("Sharpened Swords", -1));
		}
		if (armor > 0) {
			rows.add(new Row("Reinforced Armor " + armor, cost(mode, ARMOR_SMALL, ARMOR_BIG, armor)));
		}
		if (miner > 0) {
			rows.add(new Row("Maniac Miner " + miner, cost(mode, MINER_SMALL, MINER_BIG, miner)));
		}
		if (forge > 0) {
			rows.add(new Row(FORGES[forge - 1], cost(mode, FORGE_SMALL, FORGE_BIG, forge)));
		}
		if (healPool) {
			rows.add(new Row("Heal Pool", -1));
		}
		if (dragonBuff) {
			rows.add(new Row("Dragon Buff", -1));
		}
		if (boots > 0) {
			rows.add(new Row("Cushioned Boots " + boots, cost(mode, BOOTS_SMALL, BOOTS_BIG, boots)));
		}
		return rows;
	}

	public List<String> trapNames() {
		return new ArrayList<>(traps);
	}

	/** Price of the tier after the one you have (level counts from 1, so it indexes the next tier), or -1. */
	private static int cost(Mode mode, int[] small, int[] big, int level) {
		if (mode == null) {
			return -1;
		}
		int[] table = mode == Mode.BIG_TEAMS ? big : small;
		return level >= 0 && level < table.length ? table[level] : -1;
	}
}
