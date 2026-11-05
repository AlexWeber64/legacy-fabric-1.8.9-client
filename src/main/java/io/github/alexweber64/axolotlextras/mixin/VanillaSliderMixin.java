package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.ui.MenuSkin;
import net.minecraft.client.gui.widget.OptionSliderWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The sliders of the vanilla options screens (sensitivity, volume, FOV). Their handle is drawn from two texture strips. */
@Mixin(OptionSliderWidget.class)
public abstract class VanillaSliderMixin {

	@Redirect(method = "mouseDragged", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/widget/OptionSliderWidget;drawTexture(IIIIII)V"))
	private void axolotlextras$handle(OptionSliderWidget self, int x, int y, int u, int v, int width, int height) {
		if (!ExtrasModule.MODERN_MENUS.get()) {
			self.drawTexture(x, y, u, v, width, height);
			return;
		}
		// the first strip is the left half of the handle: draw the whole new handle there, the second strip is dropped
		if (u == 0) {
			MenuSkin.sliderHandle(self.x, y, self.getWidth(), height, x);
		}
	}
}
