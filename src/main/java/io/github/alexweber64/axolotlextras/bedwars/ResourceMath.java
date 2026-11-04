package io.github.alexweber64.axolotlextras.bedwars;

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
