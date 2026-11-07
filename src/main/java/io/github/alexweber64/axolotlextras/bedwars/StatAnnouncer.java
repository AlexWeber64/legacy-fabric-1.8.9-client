package io.github.alexweber64.axolotlextras.bedwars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsGame;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsMod;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsPlayer;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsPlayerStats;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsTeam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.LiteralText;

/**
 * Prints one chat line per team with every player's chosen stat once a Bed Wars game has started. Uses AxolotlClient's
 * Bed Wars module for the players and their stats, so that module has to be switched on. Only the local chat is touched,
 * nothing is sent to the server.
 */
public final class StatAnnouncer {

	/** After this many ticks players whose stats are still missing are shown as "?". */
	private static final int WAIT_TICKS = 200;
	/** Give up if the game never became ready (for example the Bed Wars module is off). */
	private static final int GIVE_UP_TICKS = 600;

	private int pending = -1;

	/** A game just started. */
	public void onGameStart() {
		pending = 0;
	}

	public void reset() {
		pending = -1;
	}

	public void tick(MinecraftClient client, ChatStat stat) {
		if (pending < 0) {
			return;
		}
		pending++;
		if (client.player == null || client.world == null) {
			pending = -1;
			return;
		}
		try {
			BedwarsMod mod = BedwarsMod.getInstance();
			if (mod == null || !mod.isEnabled()) {
				pending = -1;
				return;
			}
			Optional<BedwarsGame> game = mod.getGame();
			if (!game.isPresent() || !game.get().isStarted()) {
				if (pending > GIVE_UP_TICKS) {
					pending = -1;
				}
				return;
			}
			Map<String, BedwarsPlayer> players = new LinkedHashMap<>();
			for (PlayerListEntry entry : client.player.networkHandler.getPlayerList()) {
				String name = entry.getProfile().getName();
				game.get().getPlayer(name).ifPresent(p -> players.put(name, p));
			}
			if (players.isEmpty()) {
				if (pending > GIVE_UP_TICKS) {
					pending = -1;
				}
				return;
			}
			boolean allLoaded = true;
			for (BedwarsPlayer p : players.values()) {
				BedwarsPlayerStats s = p.getStats();
				// made-up numbers count as not loaded: the real ones usually arrive a second or two later
				if (s == null || !(s instanceof StatsOrigin) || ((StatsOrigin) s).axolotlextras$isGenerated()) {
					allLoaded = false;
				}
			}
			if (!allLoaded && pending < WAIT_TICKS) {
				return;
			}
			pending = -1;
			print(client, stat, new ArrayList<>(players.values()));
		} catch (RuntimeException e) {
			// A stats line is not worth a crash.
			pending = -1;
		}
	}

	private static void print(MinecraftClient client, ChatStat stat, List<BedwarsPlayer> players) {
		Collections.sort(players, new Comparator<BedwarsPlayer>() {
			@Override
			public int compare(BedwarsPlayer a, BedwarsPlayer b) {
				int byTeam = Integer.compare(a.getTeam().ordinal(), b.getTeam().ordinal());
				return byTeam != 0 ? byTeam : Integer.compare(a.getNumber(), b.getNumber());
			}
		});
		int real = 0;
		Map<BedwarsTeam, StringBuilder> lines = new LinkedHashMap<>();
		for (BedwarsPlayer p : players) {
			String value = valueFor(stat, p);
			if (value != null) {
				real++;
			}
			StringBuilder line = lines.get(p.getTeam());
			if (line == null) {
				line = new StringBuilder(p.getTeam().getColorSection()).append(p.getTeam().getPrefix()).append(" ");
				lines.put(p.getTeam(), line);
			} else {
				line.append("§8 | ");
			}
			line.append(p.getColoredName()).append(" §7").append(value == null ? "?" : value);
		}
		client.inGameHud.getChatHud().addMessage(new LiteralText("§6§l" + stat.label + " of every player"));
		if (real == 0) {
			client.inGameHud.getChatHud().addMessage(new LiteralText(
				"§cNo real stats available. A Hypixel API key is needed (AxolotlClient options, Hypixel Mods)."));
			return;
		}
		for (StringBuilder line : lines.values()) {
			client.inGameHud.getChatHud().addMessage(new LiteralText(line.toString()));
		}
	}

	/** @return the stat, or null when the stats are missing or made up */
	private static String valueFor(ChatStat stat, BedwarsPlayer player) {
		BedwarsPlayerStats s = player.getStats();
		if (s == null || !(s instanceof StatsOrigin) || ((StatsOrigin) s).axolotlextras$isGenerated()) {
			return null;
		}
		return stat.format(new ChatStat.Counts(s.getKills(), s.getDeaths(), s.getFinalKills(), s.getFinalDeaths(),
			s.getWins(), s.getLosses(), s.getBedsBroken(), s.getStars()));
	}
}
