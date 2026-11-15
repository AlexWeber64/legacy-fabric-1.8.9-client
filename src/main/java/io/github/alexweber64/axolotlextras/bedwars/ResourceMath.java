package io.github.alexweber64.axolotlextras.bedwars;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure logic for the Bed Wars resource counter. It has no Minecraft dependencies so it can be unit tested.
 * The behaviour follows the MCRust 1.8.9 client: iron, gold, diamonds and emeralds are counted in the main
 * inventory and the cursor, the ender chest is remembered from the last time it was open, and each row reads
 * "inventory + ender chest = total".
 */
public final class ResourceMath {

	public static final int IRON = 0;
	public static final int GOLD = 1;
	public static final int DIAMOND = 2;
	public static final int EMERALD = 3;
	public static final int COUNT = 4;

	/** Item registry names (without namespace) in the same order as the indexes above. */
	public static final String[] ITEM_NAMES = {"iron_ingot", "gold_ingot", "diamond", "emerald"};
	public static final String[] DISPLAY_NAMES = {"Iron", "Gold", "Diamond", "Emerald"};
	/** The item names in Hypixel's deposit messages, in the same order. */
	private static final String[] CHAT_NAMES = {"Iron Ingot", "Gold Ingot", "Diamond", "Emerald"};

	/**
	 * "Deposited x24 Iron Ingot into Ender Chest! (24 Total)", sometimes after AxolotlClient's game clock ("0:23 ").
	 * The Team Chest has the same message with "Team Chest" and is not counted.
	 */
	private static final Pattern DEPOSIT =
		Pattern.compile("^(?:\\d+:\\d{2}\\s+)?Deposited x(\\d+) (.+?) into Ender Chest! \\((\\d+) Total\\)$");

	private ResourceMath() {
	}

	/** Returns the resource index for an item registry name, or -1 when it is not a counted resource. */
	public static int indexOf(String itemName) {
		if (itemName == null) {
			return -1;
		}
		String name = itemName.startsWith("minecraft:") ? itemName.substring("minecraft:".length()) : itemName;
		for (int i = 0; i < COUNT; i++) {
			if (ITEM_NAMES[i].equals(name)) {
				return i;
			}
		}
		return -1;
	}

	/** Builds the text of one row, for example "37 + 32 = 69". */
	public static String row(int inventory, int enderChest, boolean showEnderChest, boolean showTotal) {
		StringBuilder sb = new StringBuilder();
		sb.append(inventory);
		if (showEnderChest) {
			sb.append(" + ").append(enderChest);
			if (showTotal) {
				sb.append(" = ").append((long) inventory + enderChest);
			}
		}
		return sb.toString();
	}

	/** Removes Minecraft formatting codes (the section sign followed by one character). */
	public static String stripFormatting(String text) {
		if (text == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '§' && i + 1 < text.length()) {
				i++;
				continue;
			}
			sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * Hypixel's message for a deposit into your ender chest, one per deposit (also when you hold the button and switch
	 * from iron to gold). It ends with the new total of that item in the ender chest, so it is the real count, not an
	 * estimate.
	 *
	 * @param chatLine the chat line, with or without colour codes
	 * @return {resource index, ender chest total}, or null when the line is no ender chest deposit of a counted resource
	 */
	public static int[] enderChestDeposit(String chatLine) {
		Matcher m = DEPOSIT.matcher(stripFormatting(chatLine).trim());
		if (!m.matches()) {
			return null;
		}
		for (int i = 0; i < COUNT; i++) {
			if (CHAT_NAMES[i].equals(m.group(2))) {
				try {
					return new int[] {i, Integer.parseInt(m.group(3))};
				} catch (NumberFormatException e) {
					return null;
				}
			}
		}
		return null;
	}

	/** True when a sidebar title belongs to Hypixel Bed Wars. */
	public static boolean isBedWarsTitle(String sidebarTitle) {
		return stripFormatting(sidebarTitle).toUpperCase().contains("BED WARS");
	}

	/** True when a chat line means a new Bed Wars game has started, so remembered ender chest counts are stale. */
	public static boolean isGameStartMessage(String chatLine) {
		return chatLine != null && chatLine.contains("Protect your bed and destroy the enemy beds");
	}

	/** True when a container title is the player's ender chest. */
	public static boolean isEnderChestTitle(String title) {
		return "Ender Chest".equals(stripFormatting(title).trim());
	}
}
