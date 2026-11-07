package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.hacks.HackRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the hack labels at the start of the HUD, so the hotbar, chat and HUD modules stay on top of them. It runs right
 * after the HUD sets up its flat 2D view: at the very start of the method the 3D camera is still set, and labels drawn
 * there land somewhere off screen.
 */
@Mixin(InGameHud.class)
public class HackHudMixin {

	@Inject(method = "render(F)V", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/GameRenderer;setupHudMatrixMode()V", shift = At.Shift.AFTER))
	private void axolotlextras$hacksLabels(float tickDelta, CallbackInfo ci) {
		HackRenderer.renderOverlay();
	}
}
