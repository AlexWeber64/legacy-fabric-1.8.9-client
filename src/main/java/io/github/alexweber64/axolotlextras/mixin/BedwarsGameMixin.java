package io.github.alexweber64.axolotlextras.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.bedwars.KdrMath;
import io.github.alexweber64.axolotlextras.bedwars.MessageFix;
import io.github.alexweber64.axolotlextras.bedwars.StatsOrigin;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsDeathType;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsGame;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsPlayer;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsPlayerStats;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsTeam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
		StringBuilder message = new StringBuilder(MessageFix.firstLine(cir.getReturnValue()));
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
		cir.setReturnValue(message.toString());
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
