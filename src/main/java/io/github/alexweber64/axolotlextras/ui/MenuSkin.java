package io.github.alexweber64.axolotlextras.ui;

import net.minecraft.client.MinecraftClient;

/** The flat rounded look shared by every restyled widget. */
public final class MenuSkin {

	public static final int NORMAL = 0xE61C2230;
	public static final int HOVER = 0xF02B3447;
	public static final int DISABLED = 0xE6141821;
	public static final int ACCENT = 0xFF35D38A;

	private static final int[] HANDLE = {1};

	private MenuSkin() {
	}

	/**
	 * Draws one button: background, then (through {@code between}) anything that goes under the label, then the label.
	 * Sliders draw their handle in between so the value text stays readable on top of it.
	 *
	 * @param centerShift how far the label's centre is moved to the left, for buttons that carry a small control on
	 *                    their right side
	 */
	public static void button(MinecraftClient client, int x, int y, int width, int height, String label, boolean active,
		boolean hovered, int centerShift, Runnable between) {
		boolean lit = active && hovered;
		RoundedRect.fillShape(x, y, width, height, RoundedRect.SMALL, !active ? DISABLED : lit ? HOVER : NORMAL);
		if (between != null) {
			between.run();
		}
		if (lit) {
			RoundedRect.outlineShape(x, y, width, height, RoundedRect.SMALL, ACCENT);
		}
		int color = !active ? 0xFF6B7385 : lit ? 0xFFFFFFFF : 0xFFE6E9F0;
		String text = label == null ? "" : label;
		client.textRenderer.draw(text, x + (width - centerShift - client.textRenderer.getStringWidth(text)) / 2f,
			y + (height - 8) / 2f, color, false);
	}

	public static void button(MinecraftClient client, int x, int y, int width, int height, String label, boolean active,
		boolean hovered, int centerShift) {
		button(client, x, y, width, height, label, active, hovered, centerShift, null);
	}

	/**
	 * The slider part drawn over a restyled button: the stretch up to the handle is tinted, and the handle is a small
	 * rounded block. Replaces the texture handle both sliders used to draw.
	 *
	 * @param handleX where the handle's left edge goes (the widget draws it 8 pixels wide)
	 */
	public static void sliderHandle(int widgetX, int widgetY, int widgetWidth, int widgetHeight, int handleX) {
		int filled = Math.min(handleX + 4 - widgetX, widgetWidth);
		if (filled > 2) {
			RoundedRect.fillShape(widgetX, widgetY, filled, widgetHeight, RoundedRect.SMALL, 0x4035D38A);
		}
		RoundedRect.fillShape(handleX, widgetY + 2, 8, widgetHeight - 4, RoundedRect.SMALL, 0xFFFFFFFF);
	}

	/** A text field: dark rounded box, with an accent outline while it has focus. */
	public static void textField(int x, int y, int width, int height, boolean focused) {
		RoundedRect.fillShape(x, y, width, height, RoundedRect.SMALL, focused ? 0xFF222A3A : 0xFF1A2030);
		RoundedRect.outlineShape(x, y, width, height, RoundedRect.SMALL, focused ? ACCENT : 0xFF3A4256);
	}

	/** A scrollbar: a dark track with a rounded handle that turns accent coloured under the mouse. */
	public static void scrollbar(int x, int top, int bottom, int handleTop, int handleHeight, boolean hovered) {
		RoundedRect.fillShape(x, top, 6, bottom - top, HANDLE, 0xFF141821);
		RoundedRect.fillShape(x + 1, handleTop, 4, handleHeight, HANDLE, hovered ? ACCENT : 0xFF5A6175);
	}
}
