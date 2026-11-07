package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.bedwars.StatsLoader;
import io.github.alexweber64.axolotlextras.bedwars.StatsOrigin;
import io.github.alexweber64.axolotlextras.bedwars.StatsRetry;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsPlayer;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsPlayerStats;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes the "?" for most players at the start of a Bed Wars game. AxolotlClient asks for a player's stats once, waits one
 * microsecond for the web request, and when the answer is not there yet it stores made-up numbers and never asks again.
 *
 * <p>With "Reliable Bed Wars Stats" on, the add-on loads the stats itself ({@link StatsLoader}) and swaps the real numbers
 * in as soon as they are there. With it off, this only asks AxolotlClient's own cache again every few ticks.
 */
@Mixin(value = BedwarsPlayer.class, remap = false)
public abstract class BedwarsPlayerMixin {

	@Shadow
	public abstract BedwarsPlayerStats getStats();

	@Shadow
	public abstract void setStats(BedwarsPlayerStats stats);

	@Shadow
	public abstract PlayerListEntry getProfile();

	@Unique
	private final StatsRetry axolotlextras$retry = new StatsRetry();

	@Inject(method = "tick", at = @At("TAIL"))
	private void axolotlextras$askAgainForRealStats(int tick, CallbackInfo ci) {
		BedwarsPlayerStats current = getStats();
		if (current != null && !(current instanceof StatsOrigin && ((StatsOrigin) current).axolotlextras$isGenerated())) {
			return;
		}
		if (!axolotlextras$retry.shouldAsk()) {
			return;
		}
		try {
			String uuid = getProfile().getProfile().getId().toString().replace("-", "");
			if (ExtrasModule.RELIABLE_STATS.get()) {
				StatsLoader.Entry entry = StatsLoader.lookup(uuid);
				int[] v = entry.values;
				if (entry.state == StatsLoader.State.READY && v != null) {
					// the counts of the current game (kills so far and so on) are kept from the placeholder
					int gameFinalKills = current == null ? 0 : current.getGameFinalKills();
					int gameFinalDeaths = current == null ? 0 : current.getGameFinalDeaths();
					int gameBeds = current == null ? 0 : current.getGameBedsBroken();
					int gameDeaths = current == null ? 0 : current.getGameDeaths();
					int gameKills = current == null ? 0 : current.getGameKills();
					setStats(new BedwarsPlayerStats(v[0], v[1], v[2], v[3], v[4], gameFinalKills, gameFinalDeaths, gameBeds,
						gameDeaths, gameKills, v[10], v[11], v[12], v[13]));
				}
			} else {
				BedwarsPlayerStats real = BedwarsPlayerStats.fromAPI(uuid);
				if (real != null) {
					setStats(real);
				}
			}
		} catch (RuntimeException e) {
			// not loaded yet, or a player without stats: the next ask tries again
		}
	}
}
