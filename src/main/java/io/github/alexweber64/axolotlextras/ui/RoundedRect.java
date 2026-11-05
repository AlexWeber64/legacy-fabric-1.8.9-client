package io.github.alexweber64.axolotlextras.ui;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.axolotlclient.AxolotlClientConfig.Color;
import io.github.axolotlclient.modules.hud.util.DrawUtil;
import io.github.axolotlclient.modules.hud.util.Rectangle;

/**
 * Rounded rectangles made of horizontal strips, in the pixel style Minecraft's GUI uses. A corner is described by how
 * many pixels each of its top rows is shortened by, so no textures or extra GL state are needed.
 */
public final class RoundedRect {

	/** The option values: how round the corners of HUD boxes are. */
	public static final String[] CORNERS = {"axolotlextras.corners.off", "axolotlextras.corners.small",
		"axolotlextras.corners.medium", "axolotlextras.corners.large"};

	private static final int[][] INSETS = {{}, {1}, {2, 1}, {3, 2, 1, 1}};

	private RoundedRect() {
	}

	/** @return how many pixels each of the top rows is shortened by, for the option value (empty means square) */
	public static int[] insetsFor(String cornerOption) {
		for (int i = 0; i < CORNERS.length; i++) {
			if (CORNERS[i].equals(cornerOption)) {
				return INSETS[i];
			}
		}
		return INSETS[0];
	}

	/**
	 * The strips a rounded rectangle is made of, as {x, y, width, height} rows, top rows first, then the middle block,
	 * then the bottom rows. Falls back to one plain rectangle when the box is too small to round.
	 */
	public static int[][] strips(int x, int y, int width, int height, int[] insets) {
		int rounded = insets.length;
		if (rounded == 0 || height <= 2 * rounded + 1 || width <= 2 * insets[0] + 1) {
			return new int[][] {{x, y, width, height}};
		}
		int[][] out = new int[2 * rounded + 1][];
		for (int i = 0; i < rounded; i++) {
			out[i] = new int[] {x + insets[i], y + i, width - 2 * insets[i], 1};
			out[out.length - 1 - i] = new int[] {x + insets[i], y + height - 1 - i, width - 2 * insets[i], 1};
		}
		out[rounded] = new int[] {x, y + rounded, width, height - 2 * rounded};
		return out;
	}

	/** Corner shape for panels in the menus: rounder than the HUD boxes. */
	public static final int[] PANEL = {3, 2, 1, 1};
	/** Corner shape for small controls such as buttons and the search box. */
	public static final int[] SMALL = {2, 1};

	/** Fills a rounded rectangle with a colour given as ARGB. */
	public static void fillShape(int x, int y, int width, int height, int[] insets, int argb) {
		for (int[] s : strips(x, y, width, height, insets)) {
			DrawUtil.fillRect(s[0], s[1], s[2], s[3], argb);
		}
	}

	/** Fills a HUD box background, rounded when the HUD corner option asks for it. */
	public static void fill(Rectangle rect, Color color) {
		int[] insets = insetsFor(ExtrasModule.HUD_CORNERS.get());
		if (insets.length == 0) {
			DrawUtil.fillRect(rect, color);
			return;
		}
		int argb = color.getAsInt();
		for (int[] s : strips(rect.x(), rect.y(), rect.width(), rect.height(), insets)) {
			DrawUtil.fillRect(s[0], s[1], s[2], s[3], argb);
		}
	}
}
