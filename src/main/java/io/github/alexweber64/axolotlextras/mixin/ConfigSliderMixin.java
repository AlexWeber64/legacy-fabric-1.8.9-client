package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.ui.MenuSkin;
import io.github.axolotlclient.AxolotlClientConfig.screen.widgets.OptionSliderWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The sliders of AxolotlClient's option pages. "method_893" is the drag/draw method and "method_992" the texture draw as
 * they are named in the shipped jar.
 */
@Mixin(value = OptionSliderWidget.class, remap = false)
public abstract class ConfigSliderMixin {

	@Redirect(method = "method_893", remap = false, at = @At(value = "INVOKE", remap = false,
		target = "Lio/github/axolotlclient/AxolotlClientConfig/screen/widgets/OptionSliderWidget;method_992(IIIIII)V"))
	private void axolotlextras$handle(OptionSliderWidget<?, ?> self, int x, int y, int u, int v, int width, int height) {
		if (!ExtrasModule.MODERN_MENUS.get()) {
			self.drawTexture(x, y, u, v, width, height);
			return;
		}
		if (u == 0) {
			MenuSkin.sliderHandle(self.x, y, self.getWidth(), height, x);
		}
	}
}
