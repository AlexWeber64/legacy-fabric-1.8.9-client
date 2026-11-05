package io.github.alexweber64.axolotlextras.ui;

import net.minecraft.client.MinecraftClient;

/** The flat rounded look shared by every restyled button. */
public final class MenuSkin {

	public static final int NORMAL = 0xE61C2230;
	public static final int HOVER = 0xF02B3447;
	public static final int DISABLED = 0xE6141821;
	public static final int ACCENT = 0xFF35D38A;

	private MenuSkin() {
	}

	/**
	 * Draws one button.
	 *
	 * @param centerShift how far the label's centre is moved to the left, for buttons that carry a small control on
	 *                    their right side
	 */
	public static void button(MinecraftClient client, int x, int y, int width, int height, String label, boolean active,
		boolean hovered, int centerShift) {
		boolean lit = active && hovered;
		RoundedRect.fillShape(x, y, width, height, RoundedRect.SMALL, !active ? DISABLED : lit ? HOVER : NORMAL);
		if (lit) {
			RoundedRect.outlineShape(x, y, width, height, RoundedRect.SMALL, ACCENT);
		}
		int color = !active ? 0xFF6B7385 : lit ? 0xFFFFFFFF : 0xFFE6E9F0;
		String text = label == null ? "" : label;
		client.textRenderer.draw(text, x + (width - centerShift - client.textRenderer.getStringWidth(text)) / 2f,
			y + (height - 8) / 2f, color, false);
	}
}
