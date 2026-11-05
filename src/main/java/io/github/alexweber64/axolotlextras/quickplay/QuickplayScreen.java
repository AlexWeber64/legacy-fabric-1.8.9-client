package io.github.alexweber64.axolotlextras.quickplay;

import io.github.alexweber64.axolotlextras.ui.RoundedRect;
import io.github.axolotlclient.modules.hud.util.ItemUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

/**
 * A card per game, each with the game's icon and a button per mode. Clicking a mode sends that "/play" command. Nothing
 * here talks to the server in any way that typing the same command in chat would not.
 */
public class QuickplayScreen extends Screen {

	private static final int BACKGROUND = 0xFF141821;
	private static final int CARD = 0x1AFFFFFF;
	private static final int BUTTON = 0x33FFFFFF;
	private static final int BUTTON_HOVER = 0xFF35D38A;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int MUTED = 0xFF9AA3B5;

	private static final int BUTTON_HEIGHT = 20;
	private static final int GAP = 5;
	private static final int PAD = 12;
	private static final int HEADER = 34;

	private ItemStack[] icons;
	private int panelLeft, panelTop, panelWidth, panelHeight, cardWidth;

	@Override
	public void init() {
		icons = new ItemStack[QuickplayEntries.GROUPS.length];
		int tallest = 0;
		for (int g = 0; g < icons.length; g++) {
			icons[g] = new ItemStack(QuickplayEntries.GROUPS[g].icon);
			tallest = Math.max(tallest, QuickplayEntries.GROUPS[g].entries.length);
		}
		int games = QuickplayEntries.GROUPS.length;
		cardWidth = Math.max(90, Math.min(150, (width - 40 - PAD * (games + 1)) / games));
		panelWidth = games * cardWidth + (games + 1) * PAD;
		panelHeight = 36 + HEADER + tallest * (BUTTON_HEIGHT + GAP) + PAD + 14;
		panelLeft = (width - panelWidth) / 2;
		panelTop = Math.max(10, (height - panelHeight) / 2);
	}

	private int cardLeft(int game) {
		return panelLeft + PAD + game * (cardWidth + PAD);
	}

	private int buttonTop(int entry) {
		return panelTop + 36 + HEADER + entry * (BUTTON_HEIGHT + GAP);
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		renderBackground();
		RoundedRect.fillShape(panelLeft, panelTop, panelWidth, panelHeight, RoundedRect.PANEL, BACKGROUND);
		client.textRenderer.draw("Quickplay", panelLeft + PAD + 4, panelTop + 13, TEXT, true);
		client.textRenderer.draw("Sends the same /play command you would type", panelLeft + panelWidth - PAD
			- client.textRenderer.getStringWidth("Sends the same /play command you would type"), panelTop + 14, MUTED, false);
		fill(panelLeft + PAD, panelTop + 30, panelLeft + panelWidth - PAD, panelTop + 31, 0x33FFFFFF);

		for (int g = 0; g < QuickplayEntries.GROUPS.length; g++) {
			QuickplayEntries.Group group = QuickplayEntries.GROUPS[g];
			int left = cardLeft(g);
			int cardTop = panelTop + 36;
			int cardHeight = HEADER + group.entries.length * (BUTTON_HEIGHT + GAP) - 2;
			RoundedRect.fillShape(left, cardTop, cardWidth, cardHeight + 6, RoundedRect.SMALL, CARD);
			ItemUtil.renderGuiItemModel(icons[g], left + 8, cardTop + 8);
			client.textRenderer.draw(group.title, left + 30, cardTop + 13, 0xFFFFD84D, false);
			for (int e = 0; e < group.entries.length; e++) {
				int top = buttonTop(e);
				boolean hovered = mouseX >= left + 6 && mouseX < left + cardWidth - 6 && mouseY >= top && mouseY < top + BUTTON_HEIGHT;
				RoundedRect.fillShape(left + 6, top, cardWidth - 12, BUTTON_HEIGHT, RoundedRect.SMALL, hovered ? BUTTON_HOVER : BUTTON);
				String label = group.entries[e].label;
				client.textRenderer.draw(label, left + (cardWidth - client.textRenderer.getStringWidth(label)) / 2,
					top + 6, hovered ? 0xFF10141B : TEXT, false);
			}
		}
		client.textRenderer.draw("Esc to close", panelLeft + PAD + 4, panelTop + panelHeight - 14, MUTED, false);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (button != 0) {
			return;
		}
		for (int g = 0; g < QuickplayEntries.GROUPS.length; g++) {
			int left = cardLeft(g);
			QuickplayEntries.Entry[] entries = QuickplayEntries.GROUPS[g].entries;
			for (int e = 0; e < entries.length; e++) {
				int top = buttonTop(e);
				if (mouseX >= left + 6 && mouseX < left + cardWidth - 6 && mouseY >= top && mouseY < top + BUTTON_HEIGHT) {
					client.setScreen(null);
					if (client.player != null) {
						client.player.sendChatMessage("/play " + entries[e].command);
					}
					return;
				}
			}
		}
	}

	@Override
	protected void keyPressed(char chr, int code) {
		if (code == Keyboard.KEY_ESCAPE) {
			client.setScreen(null);
		}
	}

	@Override
	public boolean shouldPauseGame() {
		return false;
	}
}
