package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.ui.MenuSkin;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Text fields (the search box and the colour fields of AxolotlClient's options) draw a white outline and a black fill
 * first. The first fill becomes the rounded box, the second is dropped.
 */
@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin {

	@Shadow
	private boolean focused;

	@Redirect(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;fill(IIIII)V", ordinal = 0))
	private void axolotlextras$box(int left, int top, int right, int bottom, int color) {
		if (!ExtrasModule.MODERN_MENUS.get()) {
			DrawableHelper.fill(left, top, right, bottom, color);
			return;
		}
		MenuSkin.textField(left, top, right - left, bottom - top, focused);
	}

	@Redirect(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;fill(IIIII)V", ordinal = 1))
	private void axolotlextras$noBlackFill(int left, int top, int right, int bottom, int color) {
		if (!ExtrasModule.MODERN_MENUS.get()) {
			DrawableHelper.fill(left, top, right, bottom, color);
		}
	}
}
