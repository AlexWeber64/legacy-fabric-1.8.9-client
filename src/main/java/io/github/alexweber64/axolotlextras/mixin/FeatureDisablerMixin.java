package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.axolotlclient.AxolotlClientConfig.options.BooleanOption;
import io.github.axolotlclient.util.FeatureDisabler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FeatureDisabler.class, remap = false)
public class FeatureDisablerMixin {

	/**
	 * AxolotlClient switches Freelook (freelook mode), Toggle Sneak, Time Changer, Full Bright and Low Fire off by
	 * server address. With "Ignore Server Blocks" on, that check never switches anything off.
	 */
	@Inject(method = "disableOption", at = @At("HEAD"), cancellable = true)
	private static void axolotlextras$skipAddressCheck(BooleanOption option, String[] servers, String currentServer, CallbackInfo ci) {
		if (ExtrasModule.IGNORE_SERVER_BLOCKS.get()) {
			ci.cancel();
		}
	}
}
