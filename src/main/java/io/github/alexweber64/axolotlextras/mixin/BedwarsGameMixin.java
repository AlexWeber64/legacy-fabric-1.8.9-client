package io.github.alexweber64.axolotlextras.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.bedwars.KdrMath;
import io.github.alexweber64.axolotlextras.bedwars.MessageFix;
import io.github.alexweber64.axolotlextras.bedwars.StatsOrigin;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsDeathType;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsGame;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsPlayer;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsPlayerStats;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsTeam;
import io.github.axolotlclient.util.events.impl.ReceiveChatMessageEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BedwarsGame.class, remap = false)
public abstract class BedwarsGameMixin {

	@Shadow
	@Final
	private Map<String, BedwarsPlayer> players;

	@Shadow
	public abstract BedwarsPlayer getSelf();

	@Shadow
	public abstract Optional<BedwarsPlayer> getPlayer(UUID uuid);

	@Shadow
	public abstract Optional<BedwarsPlayer> getPlayer(String name);

	/** The line above a player's head: KDR instead of the configured stat, and nothing for made-up stats. */
	@Inject(method = "getLevelHead", at = @At("HEAD"), cancellable = true)
	private void axolotlextras$kdrLevelHead(AbstractClientPlayerEntity entity, CallbackInfoReturnable<String> cir) {
		if (!ExtrasModule.KDR_LEVEL_HEAD.get()) {
			return;
		}
		BedwarsPlayerStats stats = realStats(getPlayer(entity.getUuid()).orElse(null));
		cir.setReturnValue(stats == null ? null : KdrMath.levelHead(stats.getKills(), stats.getDeaths()));
	}

	/** KDR to the left of the health number in the tab list. Space for it is made in PlayerListHudMixin. */
	@Inject(method = "renderCustomScoreboardObjective", at = @At("RETURN"))
	private void axolotlextras$kdrInTab(String playerName, ScoreboardObjective objective, int y, int endX, CallbackInfo ci) {
		if (!ExtrasModule.KDR_TAB.get()) {
			return;
		}
		BedwarsPlayer player = getPlayer(playerName).orElse(null);
		BedwarsPlayerStats stats = realStats(player);
		if (stats == null || (!player.isAlive() && player.isDisconnected())) {
			return;
		}
		MinecraftClient mc = MinecraftClient.getInstance();
		String health;
		if (player.isAlive()) {
			health = String.valueOf(objective.getScoreboard().getPlayerScore(playerName, objective).getScore());
		} else {
			float seconds = Math.max(0, player.getTickAlive() - mc.inGameHud.getTicks()) / 20f;
			health = String.format("%.1f", seconds) + "s";
		}
		String kdr = KdrMath.ratio(stats.getKills(), stats.getDeaths());
		float x = endX - mc.textRenderer.getStringWidth(health) - 4 - mc.textRenderer.getStringWidth(kdr);
		mc.textRenderer.drawWithShadow(kdr, x, (float) y, 0xAAAAAA);
	}

	/**
	 * AxolotlClient replaces Hypixel's death message with its own short style. With "Normal Hypixel Messages" on, Hypixel's
	 * own text is put back, and the gold final kill count AxolotlClient showed after a teammate's final kill is added after
	 * the killer's name (real stats only).
	 */
	@Inject(method = "died(Ljava/util/regex/Matcher;Ljava/lang/String;Lio/github/axolotlclient/util/events/impl/ReceiveChatMessageEvent;Lio/github/axolotlclient/modules/hypixel/bedwars/BedwarsDeathType;)V",
		at = @At("RETURN"))
	private void axolotlextras$hypixelDeathText(Matcher m, String rawMessage, ReceiveChatMessageEvent event,
		BedwarsDeathType type, CallbackInfo ci) {
		if (!ExtrasModule.NORMAL_MESSAGES.get()) {
			return;
		}
		event.setNewMessage(null);
		if (type == BedwarsDeathType.SELF_UNKNOWN || type == BedwarsDeathType.SELF_VOID
			|| !rawMessage.contains("FINAL KILL!")) {
			return;
		}
		BedwarsPlayer killer = getPlayer(m.group(2)).orElse(null);
		BedwarsPlayer me = getSelf();
		BedwarsPlayerStats stats = realStats(killer);
		if (killer == null || me == null || stats == null || killer.getTeam() != me.getTeam()) {
			return;
		}
		String original = event.getFormattedMessage().asFormattedString();
		String changed = MessageFix.appendAfterLastName(original, killer.getName(), " §6" + stats.getFinalKills());
		if (!changed.equals(original)) {
			event.setNewMessage(new LiteralText(changed));
		}
	}

	/** The gold number after the killer of a final kill is their final kill count, so it only stays when that is real. */
	@Inject(method = "formatDeath", at = @At("RETURN"), cancellable = true)
	private void axolotlextras$noMadeUpFinalKills(BedwarsPlayer player, BedwarsPlayer killer, BedwarsDeathType type,
		boolean finalDeath, CallbackInfoReturnable<String> cir) {
		if (killer != null && realStats(killer) == null) {
			cir.setReturnValue(MessageFix.stripTrailingCount(cir.getReturnValue()));
		}
	}

	/** Same for the beds broken count after the player who broke a bed. */
	@Inject(method = "formatBed", at = @At("RETURN"), cancellable = true)
	private void axolotlextras$noMadeUpBeds(BedwarsTeam team, BedwarsPlayer breaker, CallbackInfoReturnable<String> cir) {
		if (breaker != null && realStats(breaker) == null) {
			cir.setReturnValue(MessageFix.stripTrailingCount(cir.getReturnValue()));
		}
	}

	/** The per-player stat lines under "TEAM ELIMINATED" are kept only for players whose stats are real. */
	@Inject(method = "formatEliminated", at = @At("RETURN"), cancellable = true)
	private void axolotlextras$noMadeUpEliminatedStats(BedwarsTeam team, CallbackInfoReturnable<String> cir) {
		cir.setReturnValue(MessageFix.firstLine(cir.getReturnValue()) + axolotlextras$eliminatedLines(team));
	}

	/** One line per player of the team, each starting with a line break. Only players whose stats are real. */
	@Unique
	private String axolotlextras$eliminatedLines(BedwarsTeam team) {
		StringBuilder message = new StringBuilder();
		List<BedwarsPlayer> members = new ArrayList<>();
		for (BedwarsPlayer p : players.values()) {
			if (p.getTeam() == team) {
				members.add(p);
			}
		}
		Collections.sort(members, new Comparator<BedwarsPlayer>() {
			@Override
			public int compare(BedwarsPlayer a, BedwarsPlayer b) {
				return Integer.compare(a.getNumber(), b.getNumber());
			}
		});
		for (BedwarsPlayer p : members) {
			BedwarsPlayerStats stats = realStats(p);
			if (stats == null) {
				continue;
			}
			message.append("\n§b").append(stats.getStars()).append(" ").append(p.getColoredName())
				.append("§7 Beds: §f").append(stats.getBedsBroken())
				.append("§7 Finals: §f").append(stats.getFinalKills())
				.append("§7 FKDR: §f").append(KdrMath.ratio(stats.getFinalKills(), stats.getFinalDeaths()))
				.append("§7 BBLR: §f").append(KdrMath.ratio(stats.getBedsBroken(), stats.getLosses()));
		}
		return message.toString();
	}

	/**
	 * With "Normal Hypixel Messages" on, a broken bed shows Hypixel's own text. The breaker's beds broken count AxolotlClient
	 * showed in gold is added after their name when they are on your team and the stats are real.
	 */
	@Inject(method = "bedDestroyed", at = @At("RETURN"))
	private void axolotlextras$hypixelBedText(ReceiveChatMessageEvent event, BedwarsTeam team, BedwarsPlayer breaker,
		CallbackInfo ci) {
		if (!ExtrasModule.NORMAL_MESSAGES.get()) {
			return;
		}
		event.setNewMessage(null);
		BedwarsPlayer me = getSelf();
		BedwarsPlayerStats stats = realStats(breaker);
		if (breaker == null || me == null || stats == null || breaker.getTeam() != me.getTeam()) {
			return;
		}
		String original = event.getFormattedMessage().asFormattedString();
		String changed = MessageFix.appendAfterLastName(original, breaker.getName(), " §6" + stats.getBedsBroken());
		if (!changed.equals(original)) {
			event.setNewMessage(new LiteralText(changed));
		}
	}

	/** Hypixel's own "team eliminated" text, with AxolotlClient's per-player stat lines added under it (real stats only). */
	@Inject(method = "teamEliminated", at = @At("RETURN"))
	private void axolotlextras$hypixelEliminatedText(ReceiveChatMessageEvent event, BedwarsTeam team, CallbackInfo ci) {
		if (!ExtrasModule.NORMAL_MESSAGES.get()) {
			return;
		}
		event.setNewMessage(null);
		String lines = axolotlextras$eliminatedLines(team);
		if (!lines.isEmpty()) {
			event.setNewMessage(new LiteralText(event.getFormattedMessage().asFormattedString() + lines));
		}
	}

	/** Hypixel's own text when a player disconnects. */
	@Inject(method = "disconnected", at = @At("RETURN"))
	private void axolotlextras$hypixelDisconnectText(ReceiveChatMessageEvent event, BedwarsPlayer player, CallbackInfo ci) {
		if (ExtrasModule.NORMAL_MESSAGES.get()) {
			event.setNewMessage(null);
		}
	}

	/** Hypixel's own text when a player reconnects. */
	@Inject(method = "reconnected", at = @At("RETURN"))
	private void axolotlextras$hypixelReconnectText(ReceiveChatMessageEvent event, BedwarsPlayer player, CallbackInfo ci) {
		if (ExtrasModule.NORMAL_MESSAGES.get()) {
			event.setNewMessage(null);
		}
	}

	private static BedwarsPlayerStats realStats(BedwarsPlayer player) {
		if (player == null) {
			return null;
		}
		BedwarsPlayerStats stats = player.getStats();
		// If the stats mixin did not apply, nothing can be told apart, so nothing is shown.
		if (!(stats instanceof StatsOrigin) || ((StatsOrigin) stats).axolotlextras$isGenerated()) {
			return null;
		}
		return stats;
	}
}
