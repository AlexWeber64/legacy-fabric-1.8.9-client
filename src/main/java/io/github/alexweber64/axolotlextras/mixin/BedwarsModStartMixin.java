package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.bedwars.GameStartWatcher;
import io.github.axolotlclient.AxolotlClient;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsGame;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsMod;
import io.github.axolotlclient.util.events.impl.ReceiveChatMessageEvent;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AxolotlClient starts its Bed Wars game only on "Protect your bed and destroy the enemy beds.", which the Dreams modes
 * (Lucky Blocks and others) do not print, so its whole Bed Wars module stayed off there. This starts it on the banner
 * title as well (see {@link GameStartWatcher}), the same way AxolotlClient does: a countdown of 10 ticks, after which it
 * reads the teams from the tab list. AxolotlClient's own check then sees the countdown running and does nothing.
 */
@Mixin(value = BedwarsMod.class, remap = false)
public abstract class BedwarsModStartMixin {

	@Shadow
	protected BedwarsGame currentGame;

	@Shadow
	private int targetTick;

	@Shadow
	public abstract boolean isEnabled();

	@Unique
	private final GameStartWatcher axolotlextras$startWatcher = new GameStartWatcher();

	@Inject(method = "onMessage", at = @At("HEAD"), require = 1)
	private void axolotlextras$startOnBanner(ReceiveChatMessageEvent event, CallbackInfo ci) {
		String line = event.getFormattedMessage().asUnformattedString();
		if (axolotlextras$startWatcher.onLine(line) && currentGame == null && isEnabled() && targetTick < 0) {
			targetTick = MinecraftClient.getInstance().inGameHud.getTicks() + 10;
			AxolotlClient.LOGGER.info("[Extras] Bed Wars game start seen (" + line.trim() + ")");
		}
	}
}
