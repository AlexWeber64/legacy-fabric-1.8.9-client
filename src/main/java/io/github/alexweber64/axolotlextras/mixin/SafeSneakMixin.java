package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.hacks.HackRenderer;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Safe Sneak's hook: at the end of the movement input tick (after vanilla and AxolotlClient's toggle sneak have set the
 * keys), it may turn sneaking on, the same way holding the sneak key does.
 */
@Mixin(KeyboardInput.class)
public class SafeSneakMixin {

	@Inject(method = "tick", at = @At("TAIL"))
	private void axolotlextras$safeSneak(CallbackInfo ci) {
		HackRenderer.safeSneak((KeyboardInput) (Object) this);
	}
}
