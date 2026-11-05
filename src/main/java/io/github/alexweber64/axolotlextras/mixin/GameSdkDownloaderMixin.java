package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.axolotlclient.AxolotlClientConfig.options.BooleanOption;
import io.github.axolotlclient.modules.rpc.gameSdk.GameSdkDownloader;
import io.github.axolotlclient.util.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Turning on the Discord RPC of AxolotlClient downloads Discord's game SDK (a native library) with no check of the
 * file and loads it into the game. With the privacy lock on nothing is downloaded and RPC stays off.
 */
@Mixin(value = GameSdkDownloader.class, remap = false)
public class GameSdkDownloaderMixin {

	@Inject(method = "downloadSdk(Lio/github/axolotlclient/util/Logger;Lio/github/axolotlclient/AxolotlClientConfig/options/BooleanOption;)V",
		at = @At("HEAD"), cancellable = true)
	private static void axolotlextras$noSdkDownload(Logger logger, BooleanOption rpcEnabled, CallbackInfo ci) {
		if (ExtrasModule.PRIVACY_LOCK.get()) {
			rpcEnabled.set(false);
			ci.cancel();
		}
	}
}
