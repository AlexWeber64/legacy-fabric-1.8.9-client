package io.github.alexweber64.axolotlextras.mixin;

import java.util.UUID;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.axolotlclient.util.NetworkHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The "badges" feature of AxolotlClient tells its own server (moehreag.duckdns.org) your player UUID when you join and
 * leave, and asks it about the UUID of every other player it sees. With the privacy lock on none of that is sent.
 */
@Mixin(value = NetworkHelper.class, remap = false)
public class NetworkHelperMixin {

	@Inject(method = "setOnline", at = @At("HEAD"), cancellable = true)
	private static void axolotlextras$noLogin(CallbackInfo ci) {
		if (ExtrasModule.PRIVACY_LOCK.get()) {
			ci.cancel();
		}
	}

	@Inject(method = "setOffline", at = @At("HEAD"), cancellable = true)
	private static void axolotlextras$noLogout(CallbackInfo ci) {
		if (ExtrasModule.PRIVACY_LOCK.get()) {
			ci.cancel();
		}
	}

	@Inject(method = "getUser", at = @At("HEAD"), cancellable = true)
	private static void axolotlextras$noLookup(UUID uuid, CallbackInfo ci) {
		if (ExtrasModule.PRIVACY_LOCK.get()) {
			ci.cancel();
		}
	}

	@Inject(method = "getOnline", at = @At("HEAD"), cancellable = true)
	private static void axolotlextras$noPlayerQuery(UUID uuid, CallbackInfoReturnable<Boolean> cir) {
		if (ExtrasModule.PRIVACY_LOCK.get()) {
			cir.setReturnValue(false);
		}
	}
}
