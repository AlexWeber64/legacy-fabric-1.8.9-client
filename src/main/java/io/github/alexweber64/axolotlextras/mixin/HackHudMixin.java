package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.hacks.HackRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the generator and bed labels first thing in the HUD, so the hotbar, chat and HUD modules stay on top of them. */
@Mixin(InGameHud.class)
public class HackHudMixin {

	@Inject(method = "render(F)V", at = @At("HEAD"))
	private void axolotlextras$hacksLabels(float tickDelta, CallbackInfo ci) {
		HackRenderer.renderOverlay();
	}
}
