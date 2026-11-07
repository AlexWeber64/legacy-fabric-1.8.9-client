package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.hacks.HackRenderer;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The hacks' world hook: after the world is drawn and before the hand (the same place Forge fires its "render world last"
 * event). Player boxes and tracers are drawn here, and the camera matrices are saved for the HUD labels.
 */
@Mixin(GameRenderer.class)
public class HackWorldRenderMixin {

	@Inject(method = "renderWorld(IFJ)V", at = @At(value = "INVOKE_STRING",
		target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=hand"))
	private void axolotlextras$hacksWorldPass(int pass, float tickDelta, long limitTime, CallbackInfo ci) {
		HackRenderer.onWorldRender(tickDelta);
	}
}
