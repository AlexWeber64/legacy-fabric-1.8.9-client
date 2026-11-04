package io.github.alexweber64.axolotlextras.quickplay;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

/**
 * A grid of buttons that each send one "/play" command, grouped by game. Nothing here talks to the server in any
 * way that typing the same command in chat would not.
 */
public class QuickplayScreen extends Screen {

	private static final int BUTTON_WIDTH = 110;
	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 4;
	private static final int GROUP_GAP = 10;

	@Override
	public void init() {
		buttons.clear();
		int groups = QuickplayEntries.GROUPS.length;
		int totalWidth = groups * BUTTON_WIDTH + (groups - 1) * GROUP_GAP;
		int left = (width - totalWidth) / 2;
		for (int g = 0; g < groups; g++) {
			QuickplayEntries.Entry[] entries = QuickplayEntries.GROUPS[g].entries;
			int x = left + g * (BUTTON_WIDTH + GROUP_GAP);
			for (int e = 0; e < entries.length; e++) {
				int y = 50 + e * (BUTTON_HEIGHT + GAP);
				buttons.add(new ButtonWidget(g * 100 + e, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, entries[e].label));
			}
		}
		buttons.add(new ButtonWidget(-1, width / 2 - 50, height - 30, 100, 20, "Close"));
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (button.id < 0) {
			client.setScreen(null);
			return;
		}
		int group = button.id / 100;
		int entry = button.id % 100;
		String command = QuickplayEntries.GROUPS[group].entries[entry].command;
		client.setScreen(null);
		if (client.player != null) {
			client.player.sendChatMessage("/play " + command);
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		renderBackground();
		drawCenteredString(client.textRenderer, "Quickplay", width / 2, 20, 0xFFFFFF);
		int groups = QuickplayEntries.GROUPS.length;
		int totalWidth = groups * BUTTON_WIDTH + (groups - 1) * GROUP_GAP;
		int left = (width - totalWidth) / 2;
		for (int g = 0; g < groups; g++) {
			drawCenteredString(client.textRenderer, QuickplayEntries.GROUPS[g].title,
				left + g * (BUTTON_WIDTH + GROUP_GAP) + BUTTON_WIDTH / 2, 36, 0xFFFF55);
		}
		super.render(mouseX, mouseY, delta);
	}

	@Override
	public boolean shouldPauseGame() {
		return false;
	}
}
