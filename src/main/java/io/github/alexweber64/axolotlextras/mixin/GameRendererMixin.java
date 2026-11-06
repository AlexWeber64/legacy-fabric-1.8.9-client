package io.github.alexweber64.axolotlextras.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.perf.FrameBench;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

	/**
	 * renderFog sets up the fog for the frame (distance fog, and the denser fog under water or in lava). Switching fog
	 * off once it has run leaves the sky and fog colour calculations alone.
	 */
	/** Frame time recording for the benchmark (does nothing unless a benchmark is running). */
	@Inject(method = "render", at = @At("HEAD"))
	private void axolotlextras$frame(float tickDelta, long nanoTime, CallbackInfo ci) {
		FrameBench.onFrame();
	}

	@Inject(method = "renderFog", at = @At("RETURN"))
	private void axolotlextras$noFog(int mode, float tickDelta, CallbackInfo ci) {
		if (ExtrasModule.NO_FOG.get()) {
			GlStateManager.disableFog();
		}
	}
}
