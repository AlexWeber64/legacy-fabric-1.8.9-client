package io.github.alexweber64.axolotlextras.mixin;

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
 * Only players whose data was already fetched earlier (you, and players seen in the lobby) got real numbers. This asks
 * again every few ticks while a player only has made-up numbers, and swaps in the real ones when they arrive.
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
		if (current == null || !(current instanceof StatsOrigin) || !((StatsOrigin) current).axolotlextras$isGenerated()) {
			return;
		}
		if (!axolotlextras$retry.shouldAsk()) {
			return;
		}
		try {
			String uuid = getProfile().getProfile().getId().toString().replace("-", "");
			BedwarsPlayerStats real = BedwarsPlayerStats.fromAPI(uuid);
			if (real != null) {
				setStats(real);
			}
		} catch (RuntimeException e) {
			// not loaded yet, or a player without stats: the next ask tries again
		}
	}
}
