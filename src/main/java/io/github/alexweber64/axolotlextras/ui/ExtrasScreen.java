package io.github.alexweber64.axolotlextras.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.axolotlclient.AxolotlClient;
import io.github.axolotlclient.AxolotlClientConfig.options.BooleanOption;
import io.github.axolotlclient.AxolotlClientConfig.options.EnumOption;
import io.github.axolotlclient.AxolotlClientConfig.options.IntegerOption;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * A settings screen for the Extras features: one scrolling list of sections, a switch per option with its description
 * under it, and a search box. Changes apply at once and are saved when the screen closes.
 */
public class ExtrasScreen extends Screen {

	private static final int BACKGROUND = 0xFF141821;
	private static final int ROW = 0x1AFFFFFF;
	private static final int ROW_HOVER = 0x33FFFFFF;
	private static final int ACCENT = 0xFF35D38A;
	private static final int OFF = 0xFF5A6175;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int MUTED = 0xFF9AA3B5;
	private static final int HEADING = 0xFFFFD84D;

	private static final int ROW_HEIGHT = 42;
	private static final int ROW_GAP = 4;
	private static final int HEADING_HEIGHT = 20;
	private static final int SLIDER_WIDTH = 110;
	private static final int KNOB_WIDTH = 8;
	private static final int SLIDER_STEP = 5;

	/** One line of the list: a section heading or an option. */
	private static final class Entry {
		final String heading;
		final Object option;

		Entry(String heading, Object option) {
			this.heading = heading;
			this.option = option;
		}
	}

	private final Screen parent;
	private final String title;
	private final List<Entry> all = new ArrayList<>();
	private final Map<Object, Float> knob = new HashMap<>();
	private String search = "";
	private int scroll;
	/** The slider being dragged, and the left edge of its bar. */
	private IntegerOption dragging;
	private int dragLeft;
	private int panelLeft, panelTop, panelWidth, panelHeight, listTop, listBottom;

	public ExtrasScreen(Screen parent) {
		this(parent, "§lAxolotlClient Extras");
		section("axolotlextras.section.privacy", ExtrasModule.PRIVACY_LOCK, ExtrasModule.IGNORE_SERVER_BLOCKS);
		section("axolotlextras.section.bedwars", ExtrasModule.NORMAL_MESSAGES, ExtrasModule.CHAT_STATS,
			ExtrasModule.CHAT_STAT, ExtrasModule.KDR_LEVEL_HEAD, ExtrasModule.KDR_TAB, ExtrasModule.RELIABLE_STATS);
		section("axolotlextras.section.controls", ExtrasModule.TOGGLE_WITHOUT_HUD, ExtrasModule.QUICKPLAY,
			ExtrasModule.QUICKPLAY_HYPIXEL_ONLY);
		section("axolotlextras.section.look", ExtrasModule.MODERN_MENUS, ExtrasModule.HUD_CORNERS, ExtrasModule.NO_FOG,
			AxolotlClient.CONFIG.dynamicFOV, ExtrasModule.KEEP_INVENTORY_CENTERED, ExtrasModule.TRANSPARENT_UI);
		section("axolotlextras.section.oldvisuals", ExtrasModule.OLD_BLOCK_HITTING, ExtrasModule.OLD_FIRST_PERSON,
			ExtrasModule.OLD_SNEAK, ExtrasModule.OLD_THIRD_PERSON);
	}

	/** For menus with the same look and other options (see {@link HacksScreen}). Add the options with {@link #section}. */
	protected ExtrasScreen(Screen parent, String title) {
		this.parent = parent;
		this.title = title;
	}

	protected final void section(String headingKey, Object... options) {
		all.add(new Entry(headingKey, null));
		for (Object option : options) {
			all.add(new Entry(null, option));
		}
	}

	// ---------------------------------------------------------------------------------------------------- option access

	private static String name(Object option) {
		if (option instanceof BooleanOption) {
			return ((BooleanOption) option).getName();
		}
		if (option instanceof IntegerOption) {
			return ((IntegerOption) option).getName();
		}
		return ((EnumOption) option).getName();
	}

	/** The unit shown after a slider's value: the label sizes are percentages, the radar's size and place are pixels. */
	private static String unit(IntegerOption option) {
		String name = option.getName();
		if (name.contains("labelSize") || name.contains("radarIconSize") || name.contains("radarOpacity")) {
			return "%";
		}
		return name.contains("radarSize") || name.contains("radarOffset") ? " px" : "";
	}

	private static String label(Object option) {
		return I18n.translate(name(option));
	}

	private static String description(Object option) {
		String key = name(option) + ".tooltip";
		String text = I18n.translate(key);
		return text.equals(key) ? "" : text;
	}

	private static boolean on(Object option) {
		return option instanceof BooleanOption && ((BooleanOption) option).get();
	}

	// ---------------------------------------------------------------------------------------------------------- layout

	/** The entries shown now: the whole list, or only the options matching the search text (with their headings). */
	private List<Entry> visible() {
		if (search.isEmpty()) {
			return all;
		}
		String q = search.toLowerCase(Locale.ROOT);
		List<Entry> out = new ArrayList<>();
		Entry pendingHeading = null;
		for (Entry e : all) {
			if (e.heading != null) {
				pendingHeading = e;
			} else if ((label(e.option) + " " + description(e.option)).toLowerCase(Locale.ROOT).contains(q)) {
				if (pendingHeading != null) {
					out.add(pendingHeading);
					pendingHeading = null;
				}
				out.add(e);
			}
		}
		return out;
	}

	private static int heightOf(Entry e) {
		return e.heading != null ? HEADING_HEIGHT : ROW_HEIGHT + ROW_GAP;
	}

	private int contentHeight(List<Entry> list) {
		int h = 0;
		for (Entry e : list) {
			h += heightOf(e);
		}
		return h;
	}

	private void clampScroll() {
		int max = Math.max(0, contentHeight(visible()) - (listBottom - listTop));
		scroll = Math.max(0, Math.min(scroll, max));
	}

	@Override
	public void init() {
		panelWidth = Math.min(width - 40, 520);
		panelHeight = height - 40;
		panelLeft = (width - panelWidth) / 2;
		panelTop = 20;
		listTop = panelTop + 44;
		listBottom = panelTop + panelHeight - 30;
		Keyboard.enableRepeatEvents(true);
	}

	// --------------------------------------------------------------------------------------------------------- drawing

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		renderBackground();
		RoundedRect.fillShape(panelLeft, panelTop, panelWidth, panelHeight, RoundedRect.PANEL, BACKGROUND);

		int boxWidth = 150;
		int boxLeft = panelLeft + panelWidth - boxWidth - 16;
		String shown = search.isEmpty() ? "Search" : client.textRenderer.trimToWidth(search, boxWidth - 12, true);

		// list, clipped to its area (rows that run past it are covered again below)
		List<Entry> list = visible();
		clampScroll();
		int y = listTop - scroll;
		int rowLeft = panelLeft + 12;
		int rowWidth = panelWidth - 24 - 6;
		for (Entry e : list) {
			int h = heightOf(e);
			if (y + h > listTop && y < listBottom) {
				if (e.heading != null) {
					client.textRenderer.draw(I18n.translate(e.heading).toUpperCase(Locale.ROOT), rowLeft + 4, y + 8, HEADING, false);
				} else {
					boolean hovered = mouseX >= rowLeft && mouseX < rowLeft + rowWidth && mouseY >= Math.max(y, listTop)
						&& mouseY < Math.min(y + ROW_HEIGHT, listBottom);
					RoundedRect.fillShape(rowLeft, y, rowWidth, ROW_HEIGHT, RoundedRect.SMALL, hovered ? ROW_HOVER : ROW);
					drawOption(e.option, rowLeft, y, rowWidth);
				}
			}
			y += h;
		}
		// the rows above and below the list area are cut off by repainting the panel over them
		RoundedRect.fillShape(panelLeft, panelTop, panelWidth, listTop - panelTop, RoundedRect.PANEL, BACKGROUND);
		RoundedRect.fillShape(panelLeft, listBottom, panelWidth, panelTop + panelHeight - listBottom, RoundedRect.PANEL, BACKGROUND);
		drawHeader(boxLeft, boxWidth, shown);
		drawScrollbar(list);

		// footer
		client.textRenderer.draw("Changes apply at once. Press Esc to close.", panelLeft + 16, panelTop + panelHeight - 17, MUTED, false);
		if (list.isEmpty()) {
			client.textRenderer.draw("Nothing matches that search.", panelLeft + 16, listTop + 10, MUTED, false);
		}
	}

	private void drawHeader(int boxLeft, int boxWidth, String shown) {
		client.textRenderer.draw(title, panelLeft + 16, panelTop + 14, TEXT, true);
		RoundedRect.fillShape(boxLeft, panelTop + 9, boxWidth, 18, RoundedRect.SMALL, 0x33FFFFFF);
		client.textRenderer.draw(shown, boxLeft + (search.isEmpty() ? 10 : 6), panelTop + 14, search.isEmpty() ? MUTED : TEXT, false);
		if ((System.currentTimeMillis() / 500) % 2 == 0) {
			int caretX = boxLeft + 6 + (search.isEmpty() ? 0 : client.textRenderer.getStringWidth(shown) + 1);
			fill(caretX, panelTop + 13, caretX + 1, panelTop + 23, TEXT);
		}
		fill(panelLeft + 12, panelTop + 36, panelLeft + panelWidth - 12, panelTop + 37, 0x33FFFFFF);
	}

	private void drawOption(Object option, int left, int top, int width) {
		int controlRight = left + width - 10;
		String desc = description(option);
		int textWidth = width - (option instanceof IntegerOption ? SLIDER_WIDTH + 60 : 100);
		client.textRenderer.draw(label(option), left + 10, top + (desc.isEmpty() ? 17 : 6), TEXT, false);
		if (!desc.isEmpty()) {
			List<String> lines = client.textRenderer.wrapLines(desc, textWidth);
			for (int i = 0; i < Math.min(2, lines.size()); i++) {
				String line = lines.get(i);
				if (i == 1 && lines.size() > 2) {
					line = client.textRenderer.trimToWidth(line, textWidth - 12) + "...";
				}
				client.textRenderer.draw(line, left + 10, top + 17 + i * 10, MUTED, false);
			}
		}
		if (option instanceof BooleanOption) {
			float target = on(option) ? 1f : 0f;
			float now = knob.containsKey(option) ? knob.get(option) : target;
			now += (target - now) * 0.35f;
			if (Math.abs(target - now) < 0.02f) {
				now = target;
			}
			knob.put(option, now);
			int w = 34, h = 16;
			int x = controlRight - w;
			int y = top + (ROW_HEIGHT - h) / 2;
			RoundedRect.fillShape(x, y, w, h, RoundedRect.PANEL, blend(OFF, ACCENT, now));
			int knobX = x + 2 + Math.round(now * (w - 14 - 4));
			RoundedRect.fillShape(knobX, y + 2, 14, 12, RoundedRect.SMALL, 0xFFFFFFFF);
		} else if (option instanceof IntegerOption) {
			// A bar like the FOV slider: filled up to the knob, the value on its left.
			IntegerOption slider = (IntegerOption) option;
			int barLeft = controlRight - SLIDER_WIDTH;
			int middle = top + ROW_HEIGHT / 2;
			float t = fraction(slider);
			int knobX = barLeft + Math.round(t * (SLIDER_WIDTH - KNOB_WIDTH));
			RoundedRect.fillShape(barLeft, middle - 2, SLIDER_WIDTH, 4, new int[] {1}, OFF);
			RoundedRect.fillShape(barLeft, middle - 2, knobX - barLeft + KNOB_WIDTH / 2, 4, new int[] {1}, ACCENT);
			RoundedRect.fillShape(knobX, middle - 8, KNOB_WIDTH, 16, RoundedRect.SMALL, dragging == slider ? 0xFFE6E9F0 : 0xFFFFFFFF);
			String value = slider.get() + unit(slider);
			client.textRenderer.draw(value, barLeft - 8 - client.textRenderer.getStringWidth(value), middle - 4, TEXT, false);
		} else {
			String value = I18n.translate(((EnumOption) option).get());
			int w = Math.max(60, client.textRenderer.getStringWidth(value) + 16);
			int x = controlRight - w;
			int y = top + (ROW_HEIGHT - 18) / 2;
			RoundedRect.fillShape(x, y, w, 18, RoundedRect.SMALL, 0x40FFFFFF);
			client.textRenderer.draw(value, x + (w - client.textRenderer.getStringWidth(value)) / 2, y + 5, TEXT, false);
		}
	}

	private void drawScrollbar(List<Entry> list) {
		int total = contentHeight(list);
		int visibleHeight = listBottom - listTop;
		if (total <= visibleHeight) {
			return;
		}
		int trackLeft = panelLeft + panelWidth - 10;
		int barHeight = Math.max(20, visibleHeight * visibleHeight / total);
		int barTop = listTop + (int) ((long) (visibleHeight - barHeight) * scroll / (total - visibleHeight));
		RoundedRect.fillShape(trackLeft, barTop, 4, barHeight, new int[] {1}, 0x66FFFFFF);
	}

	/** Where a slider's value sits between its minimum (0) and maximum (1). */
	private static float fraction(IntegerOption slider) {
		int min = slider.getMin(), max = slider.getMax();
		return max <= min ? 0f : Math.max(0f, Math.min(1f, (slider.get() - min) / (float) (max - min)));
	}

	/** Sets the slider being dragged from the mouse position, in steps of {@link #SLIDER_STEP}. */
	private void slideTo(int mouseX) {
		int min = dragging.getMin(), max = dragging.getMax();
		float t = Math.max(0f, Math.min(1f, (mouseX - dragLeft - KNOB_WIDTH / 2f) / (float) (SLIDER_WIDTH - KNOB_WIDTH)));
		int value = Math.round((min + t * (max - min)) / SLIDER_STEP) * SLIDER_STEP;
		dragging.set(Math.max(min, Math.min(max, value)));
	}

	private static int blend(int from, int to, float t) {
		int a = (int) (((from >>> 24) & 0xFF) * (1 - t) + ((to >>> 24) & 0xFF) * t);
		int r = (int) (((from >>> 16) & 0xFF) * (1 - t) + ((to >>> 16) & 0xFF) * t);
		int g = (int) (((from >>> 8) & 0xFF) * (1 - t) + ((to >>> 8) & 0xFF) * t);
		int b = (int) ((from & 0xFF) * (1 - t) + (to & 0xFF) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	// ------------------------------------------------------------------------------------------------------ input

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (button != 0 || mouseY < listTop || mouseY >= listBottom) {
			return;
		}
		int y = listTop - scroll;
		int rowLeft = panelLeft + 12;
		int rowWidth = panelWidth - 24 - 6;
		for (Entry e : visible()) {
			int h = heightOf(e);
			if (e.option != null && mouseY >= y && mouseY < y + ROW_HEIGHT && mouseX >= rowLeft && mouseX < rowLeft + rowWidth) {
				if (e.option instanceof BooleanOption) {
					((BooleanOption) e.option).toggle();
				} else if (e.option instanceof IntegerOption) {
					// Only the bar takes clicks (a click on the text does nothing), then dragging moves the knob.
					int barLeft = rowLeft + rowWidth - 10 - SLIDER_WIDTH;
					if (mouseX >= barLeft - 6) {
						dragging = (IntegerOption) e.option;
						dragLeft = barLeft;
						slideTo(mouseX);
					}
				} else {
					((EnumOption) e.option).next();
				}
				return;
			}
			y += h;
		}
	}

	@Override
	protected void mouseDragged(int mouseX, int mouseY, int button, long time) {
		if (dragging != null && button == 0) {
			slideTo(mouseX);
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int button) {
		dragging = null;
	}

	@Override
	public void handleMouse() {
		super.handleMouse();
		int wheel = Mouse.getEventDWheel();
		if (wheel != 0) {
			scroll -= Integer.signum(wheel) * 24;
			clampScroll();
		}
	}

	@Override
	protected void keyPressed(char chr, int code) {
		if (code == Keyboard.KEY_ESCAPE) {
			if (!search.isEmpty()) {
				search = "";
				scroll = 0;
			} else {
				client.setScreen(parent);
			}
		} else if (code == Keyboard.KEY_BACK) {
			if (!search.isEmpty()) {
				search = search.substring(0, search.length() - 1);
				scroll = 0;
			}
		} else if (chr >= 32 && chr != 127 && search.length() < 30) {
			search += chr;
			scroll = 0;
		}
	}

	@Override
	public void removed() {
		Keyboard.enableRepeatEvents(false);
		try {
			AxolotlClient.configManager.save();
		} catch (RuntimeException e) {
			// A settings screen is not worth a crash. The values are still kept for this session.
			AxolotlClient.LOGGER.error("[Extras] Could not save the config: " + e);
		}
	}

	@Override
	public boolean shouldPauseGame() {
		return false;
	}
}
