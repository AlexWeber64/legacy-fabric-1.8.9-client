package io.github.alexweber64.axolotlextras.bedwars;

import java.util.ArrayList;
import java.util.List;

import io.github.axolotlclient.AxolotlClientConfig.options.BooleanOption;
import io.github.axolotlclient.AxolotlClientConfig.options.EnumOption;
import io.github.axolotlclient.AxolotlClientConfig.options.Option;
import io.github.axolotlclient.modules.hud.gui.entry.TextHudEntry;
import io.github.axolotlclient.modules.hud.util.DrawPosition;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;

/**
 * Bed Wars upgrade list as text: a bold "Upgrades" heading, one line per bought upgrade with the price of the next tier
 * on the right, then a "Traps" heading with the traps. Follows the Lunar Client layout.
 */
public class UpgradeDisplayHud extends TextHudEntry {

	public static final Identifier ID = new Identifier("axolotlextras", "upgradedisplay");

	private static final String[] MODES = {"auto", "small_teams", "big_teams"};
	private static final int LINE = 11;
	private static final int SECTION_GAP = 5;
	private static final int PAD = 4;

	private final BooleanOption onlyInBedWars = new BooleanOption("axolotlextras.ud.onlyBedWars", true);
	private final BooleanOption showCost = new BooleanOption("axolotlextras.ud.showCost", true);
	private final EnumOption mode = new EnumOption("axolotlextras.ud.mode", MODES, MODES[0]);

	private final UpgradeTracker tracker = new UpgradeTracker();
	private boolean inBedWars;
	private boolean wasInBedWars;
	/** Team size read from the sidebar, kept until the world changes. Null until it could be read. */
	private UpgradeTracker.Mode detectedMode;
	/** True once the in-game team count has settled the team size, so the lobby's mode line no longer matters. */
	private boolean modeConfirmed;

	public UpgradeDisplayHud() {
		super(120, 40, true);
	}

	/** Forgets everything: the upgrades, the traps and the team size belong to the world that just ended. */
	public void resetAll() {
		tracker.reset();
		detectedMode = null;
		modeConfirmed = false;
		wasInBedWars = false;
	}

	/** A game just started: upgrades start over, the team size read in the lobby still holds. */
	public void onGameStart() {
		tracker.reset();
	}

	/** Called from the chat event with the unformatted line. */
	public void onChatLine(String line) {
		tracker.onMessage(line);
	}

	@Override
	public boolean tickable() {
		return true;
	}

	@Override
	public void tick() {
		if (client.world == null || client.player == null) {
			inBedWars = false;
			resetAll();
			return;
		}
		ScoreboardObjective sidebar = client.world.getScoreboard().getObjectiveForSlot(1);
		inBedWars = sidebar != null && ResourceMath.isBedWarsTitle(sidebar.getDisplayName());
		if (!inBedWars && wasInBedWars) {
			resetAll();
		}
		wasInBedWars = inBedWars;
		if (inBedWars && !modeConfirmed) {
			List<String> lines = sidebarLines(sidebar);
			// The in-game team count is the second check and wins: it settles solo/doubles against 3s/4s/4v4.
			UpgradeTracker.Mode byTeams = ModeDetector.fromTeamLines(lines);
			if (byTeams != null) {
				detectedMode = byTeams;
				modeConfirmed = true;
			} else if (detectedMode == null) {
				detectedMode = ModeDetector.fromModeLine(lines);
			}
		}
	}

	@Override
	public void render(float delta) {
		if (onlyInBedWars.get() && !inBedWars) {
			return;
		}
		if (tracker.upgradeRows(currentMode()).isEmpty() && tracker.trapNames().isEmpty()) {
			// Nothing bought yet: no empty box.
			return;
		}
		super.render(delta);
	}

	@Override
	public void renderComponent(float delta) {
		draw(tracker.upgradeRows(currentMode()), tracker.trapNames());
	}

	@Override
	public void renderPlaceholderComponent(float delta) {
		UpgradeTracker sample = new UpgradeTracker();
		sample.onMessage("Steve purchased Cushioned Boots I");
		sample.onMessage("Steve purchased Reinforced Armor I");
		sample.onMessage("Steve purchased It's a Trap!");
		UpgradeTracker.Mode sampleMode = currentMode() == null ? UpgradeTracker.Mode.BIG_TEAMS : currentMode();
		draw(sample.upgradeRows(sampleMode), sample.trapNames());
	}

	/** The chosen team size group, or the detected one on "auto" (null while unknown, then no prices show). */
	private UpgradeTracker.Mode currentMode() {
		String chosen = mode.get();
		if (chosen.equals(MODES[1])) {
			return UpgradeTracker.Mode.SMALL_TEAMS;
		}
		if (chosen.equals(MODES[2])) {
			return UpgradeTracker.Mode.BIG_TEAMS;
		}
		return detectedMode;
	}

	/** The sidebar as the player sees it: each score's name with its team prefix and suffix. */
	private List<String> sidebarLines(ScoreboardObjective sidebar) {
		Scoreboard scoreboard = client.world.getScoreboard();
		List<String> lines = new ArrayList<>();
		for (ScoreboardPlayerScore score : scoreboard.getAllPlayerScores(sidebar)) {
			String name = score.getPlayerName();
			if (name != null && !name.startsWith("#")) {
				lines.add(Team.decorateName(scoreboard.getPlayerTeam(name), name));
			}
		}
		return lines;
	}

	private void draw(List<UpgradeTracker.Row> upgrades, List<String> traps) {
		DrawPosition pos = getPos();
		int color = textColor.get().getAsInt();
		boolean shade = shadow.get();
		int x = pos.x() + PAD;
		int y = pos.y() + PAD;

		// First pass: the widest left text and the widest cost, so the costs line up on the right.
		int leftWidest = client.textRenderer.getStringWidth("Upgrades");
		int rightWidest = 0;
		String[] costs = new String[upgrades.size()];
		for (int i = 0; i < upgrades.size(); i++) {
			UpgradeTracker.Row row = upgrades.get(i);
			leftWidest = Math.max(leftWidest, client.textRenderer.getStringWidth(row.name));
			if (showCost.get() && row.cost >= 0) {
				costs[i] = row.cost + " ↑";
				rightWidest = Math.max(rightWidest, client.textRenderer.getStringWidth(costs[i]));
			}
		}
		for (String trap : traps) {
			leftWidest = Math.max(leftWidest, client.textRenderer.getStringWidth(trap));
		}
		int width = PAD + leftWidest + (rightWidest > 0 ? 8 + rightWidest : 0) + PAD;

		if (!upgrades.isEmpty()) {
			drawString("§e§lUpgrades", x, y, color, shade);
			y += LINE;
			for (int i = 0; i < upgrades.size(); i++) {
				drawString(upgrades.get(i).name, x, y, color, shade);
				if (costs[i] != null) {
					int w = client.textRenderer.getStringWidth(costs[i]);
					drawString("§b" + costs[i], pos.x() + width - PAD - w, y, color, shade);
				}
				y += LINE;
			}
		}
		if (!traps.isEmpty()) {
			if (!upgrades.isEmpty()) {
				y += SECTION_GAP;
			}
			drawString("§e§lTraps", x, y, color, shade);
			y += LINE;
			for (String trap : traps) {
				drawString(trap, x, y, color, shade);
				y += LINE;
			}
		}
		int height = Math.max(y - pos.y() + PAD - 2, 12);
		if (upgrades.isEmpty() && traps.isEmpty()) {
			width = 0;
		}
		if (width > 0 && (width != getWidth() || height != getHeight())) {
			setWidth(width);
			setHeight(height);
			onBoundsUpdate();
		}
	}

	@Override
	public boolean movable() {
		return true;
	}

	@Override
	public List<Option<?>> getConfigurationOptions() {
		List<Option<?>> options = super.getConfigurationOptions();
		options.add(showCost);
		options.add(mode);
		options.add(onlyInBedWars);
		return options;
	}

	@Override
	public Identifier getId() {
		return ID;
	}
}
