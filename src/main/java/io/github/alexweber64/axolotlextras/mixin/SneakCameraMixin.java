package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.perf.EyeProbe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7 sneaking: the camera height follows the eye height with a tick of delay, drops at once when you start sneaking and
 * rises by 60 percent of the gap per tick when you stop. Ported from the OldAnimations mod (AxolotlClient, LGPL-3.0).
 */
@Mixin(GameRenderer.class)
public abstract class SneakCameraMixin implements EyeProbe {

	@Unique
	private float axtras$lastHeight;

	@Unique
	private float axtras$height;

	@Unique
	private float axtras$shownHeight;

	@Unique
	private boolean axtras$started;

	@Inject(method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;updateHeldItems()V"))
	private void axtras$trackEyeHeight(CallbackInfo ci) {
		Entity camera = MinecraftClient.getInstance().getCameraEntity();
		if (camera == null) {
			return;
		}
		float target = camera.getEyeHeight();
		if (!axtras$started) {
			axtras$height = target;
			axtras$started = true;
		}
		axtras$lastHeight = axtras$height;
		axtras$height = target > axtras$height ? axtras$height + (target - axtras$height) * 0.6F : target;
	}

	@Inject(method = "setupCamera", at = @At("HEAD"))
	private void axtras$blendEyeHeight(float tickDelta, int anaglyphPass, CallbackInfo ci) {
		axtras$shownHeight = axtras$lastHeight + (axtras$height - axtras$lastHeight) * tickDelta;
	}

	@Override
	public float axtras$shownEyeHeight() {
		return axtras$shownHeight;
	}

	@Redirect(method = "transformCamera",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getEyeHeight()F"))
	private float axtras$oldEyeHeight(Entity entity) {
		if (ExtrasModule.OLD_SNEAK.get() && entity instanceof ClientPlayerEntity && !((ClientPlayerEntity) entity).isSleeping()) {
			return axtras$shownHeight;
		}
		return entity.getEyeHeight();
	}
}
