package io.github.alexweber64.axolotlextras.mixin;

import java.util.Map;
import java.util.UUID;

import io.github.axolotlclient.AxolotlClient;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsGame;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsPlayer;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsTeam;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AxolotlClient finds you in a Bed Wars game only once, from the tab list at the moment the game starts. If your entry is
 * not there yet (Hypixel still scrambles names in the first moments), it never finds you, and its "Hardcore Hearts" code
 * then crashes the game on every frame (crash of 2026-11-15, NullPointerException in displayHardcoreHearts). This looks
 * for you again every tick until you are found, and until then hands AxolotlClient a stand-in that still has its bed, so
 * the hearts stay normal instead of crashing.
 */
@Mixin(value = BedwarsGame.class, remap = false)
public abstract class BedwarsGameSelfMixin {

	@Shadow
	private BedwarsPlayer me;

	@Shadow
	@Final
	private Map<String, BedwarsPlayer> players;

	@Shadow
	@Final
	private Map<UUID, BedwarsPlayer> playersById;

	@Shadow
	private boolean started;

	@Shadow
	public abstract String getFormattedTime();

	/** Only ever handed to AxolotlClient's hearts code, which asks it nothing but whether the bed is still there. */
	@Unique
	private static BedwarsPlayer axolotlextras$standIn;

	/** True once the stand-in was handed out in this game, so the log gets one line per game, not one per frame. */
	@Unique
	private boolean axolotlextras$standInLogged;

	/** You, looked up by your UUID first (names can be scrambled), then by name. */
	@Inject(method = "tick", at = @At("HEAD"), require = 1)
	private void axolotlextras$findSelf(CallbackInfo ci) {
		if (me != null || !started) {
			return;
		}
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null) {
			return;
		}
		BedwarsPlayer found = playersById.get(mc.player.getUuid());
		if (found == null) {
			found = players.get(mc.player.getGameProfile().getName());
		}
		if (found != null) {
			me = found;
			AxolotlClient.LOGGER.info("[Extras] Bed Wars self fix: found you in this game after the start (AxolotlClient"
				+ " had missed you)");
		}
	}

	@Inject(method = "getSelf", at = @At("RETURN"), cancellable = true, require = 1)
	private void axolotlextras$neverNullSelf(CallbackInfoReturnable<BedwarsPlayer> cir) {
		if (cir.getReturnValue() == null) {
			if (axolotlextras$standIn == null) {
				// A new player has its bed, so the hearts look as they always do.
				axolotlextras$standIn = new BedwarsPlayer(BedwarsTeam.values()[0], null, 0);
			}
			cir.setReturnValue(axolotlextras$standIn);
			if (!axolotlextras$standInLogged) {
				axolotlextras$standInLogged = true;
				AxolotlClient.LOGGER.info("[Extras] Bed Wars self fix: AxolotlClient has not found you yet, Hardcore Hearts"
					+ " uses a stand-in (this used to crash the game)");
			}
		}
	}

	/** The top bar shows your kills, deaths and beds, which would crash the same way while you are not found yet. */
	@Inject(method = "calculateTopBarText", at = @At("HEAD"), cancellable = true, require = 1)
	private void axolotlextras$topBarWithoutSelf(CallbackInfoReturnable<String> cir) {
		if (me == null) {
			cir.setReturnValue(getFormattedTime());
		}
	}
}
