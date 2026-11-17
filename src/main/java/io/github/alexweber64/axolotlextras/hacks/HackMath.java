package io.github.alexweber64.axolotlextras.hacks;

/** Small helpers for the hacks: team colours from formatted names, and world-to-screen projection. No Minecraft imports. */
public final class HackMath {

	private HackMath() {
	}

	/**
	 * The colour of the first colour code in a formatted name ("§c§lR §cSteve" gives red), the way Minecraft's font draws
	 * it.
	 *
	 * @return 0xRRGGBB, or -1 when the name has no colour code
	 */
	public static int teamColor(String formatted) {
		if (formatted == null) {
			return -1;
		}
		for (int i = 0; i + 1 < formatted.length(); i++) {
			if (formatted.charAt(i) == '§') {
				int code = "0123456789abcdef".indexOf(Character.toLowerCase(formatted.charAt(i + 1)));
				if (code >= 0) {
					return codeColor(code);
				}
			}
		}
		return -1;
	}

	/**
	 * Hypixel's NPCs (shopkeepers, upgrade villagers, lobby characters) are fake players with version 2 UUIDs; real players
	 * have version 4 (online accounts) or 3 (offline). Nicked players keep a version 4 UUID.
	 */
	public static boolean isNpc(java.util.UUID id) {
		return id != null && id.version() == 2;
	}

	/**
	 * The number at the end of an option value ("axolotlextras.range.64" gives 64).
	 *
	 * @return the number, or {@code fallback} when the value does not end in one ("axolotlextras.range.all")
	 */
	public static double optionNumber(String value, double fallback) {
		if (value == null) {
			return fallback;
		}
		try {
			return Double.parseDouble(value.substring(value.lastIndexOf('.') + 1));
		} catch (RuntimeException e) {
			return fallback;
		}
	}

	/** Red at 0, through yellow, to green at 1 (values outside are clamped). */
	public static int redToGreen(double fraction) {
		double f = Math.max(0, Math.min(1, fraction));
		int r = (int) Math.round(255 * Math.min(1, 2 * (1 - f)));
		int g = (int) Math.round(255 * Math.min(1, 2 * f));
		return r << 16 | g << 8 | 0x20;
	}

	/**
	 * The compass direction of an offset in Minecraft's world (north is -z, east is +x).
	 *
	 * @return one of N, NE, E, SE, S, SW, W, NW
	 */
	public static String compass(double dx, double dz) {
		String[] names = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
		// 0 degrees points south (+z), turning towards west (-x), like Minecraft's yaw.
		double degrees = Math.toDegrees(Math.atan2(-dx, dz));
		int index = (int) Math.round(((degrees % 360) + 360) % 360 / 45.0) % 8;
		return names[index];
	}

	/** The RGB value Minecraft's font uses for colour code 0 to 15. */
	static int codeColor(int code) {
		int extra = (code >> 3 & 1) * 85;
		int r = (code >> 2 & 1) * 170 + extra;
		int g = (code >> 1 & 1) * 170 + extra;
		int b = (code & 1) * 170 + extra;
		if (code == 6) {
			r += 85;
		}
		return (r << 16) | (g << 8) | b;
	}

	/**
	 * Projects a point (relative to the camera) to window pixels, like gluProject.
	 *
	 * @param modelView  the 4x4 model view matrix, column-major (as OpenGL hands it out)
	 * @param projection the 4x4 projection matrix, column-major
	 * @param viewport   x, y, width, height of the viewport
	 * @return {x, y} in window pixels with y counted from the bottom, or null when the point is behind the camera
	 */
	public static double[] project(double x, double y, double z, float[] modelView, float[] projection, int[] viewport) {
		double[] eye = multiply(modelView, x, y, z, 1);
		double[] clip = multiply(projection, eye[0], eye[1], eye[2], eye[3]);
		if (clip[3] <= 1e-6) {
			return null;
		}
		double ndcX = clip[0] / clip[3];
		double ndcY = clip[1] / clip[3];
		return new double[] {viewport[0] + viewport[2] * (ndcX + 1) / 2, viewport[1] + viewport[3] * (ndcY + 1) / 2};
	}

	private static double[] multiply(float[] m, double x, double y, double z, double w) {
		return new double[] {
			m[0] * x + m[4] * y + m[8] * z + m[12] * w,
			m[1] * x + m[5] * y + m[9] * z + m[13] * w,
			m[2] * x + m[6] * y + m[10] * z + m[14] * w,
			m[3] * x + m[7] * y + m[11] * z + m[15] * w};
	}

	/**
	 * The Rush Alert chat line, like "§c§lRush §r§9§lB §9Steve §f15m from your bed". With Rush: Include Teammates, a
	 * teammate gets a green "[Teammate]" after the name, so it is clear at a glance that nobody is attacking.
	 */
	public static String rushMessage(String formattedName, boolean teammate, long distance) {
		return "§c§lRush §r" + formattedName + (teammate ? " §a[Teammate]" : "") + " §f" + distance + "m from your bed";
	}

	/**
	 * The number on an enemy's held item icon: Sharpness or Knockback first (weapons), else how many of the item they
	 * hold when Held Item Count is on. A single item gets no number, like in the hotbar, and wool gets none when Count
	 * Wool is off.
	 *
	 * @param count      the stack size the server sent for the held item
	 * @param showCount  the Held Item Count switch
	 * @param countWool  the Count Wool switch
	 */
	public static String heldNumber(int count, int sharpness, int knockback, boolean wool, boolean showCount,
		boolean countWool) {
		if (sharpness > 0) {
			return "S" + sharpness;
		}
		if (knockback > 0) {
			return "K" + knockback;
		}
		return showCount && count > 1 && (countWool || !wool) ? String.valueOf(count) : "";
	}
}
