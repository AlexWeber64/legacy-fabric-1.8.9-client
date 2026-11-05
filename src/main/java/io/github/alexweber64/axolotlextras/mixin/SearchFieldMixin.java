package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.ui.MenuSkin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The search box of AxolotlClient's option pages (an anonymous text field inside OptionsScreenBuilder) draws a grey
 * "Search..." hint and a white line under the field. It gets the rounded box, and the line is painted over.
 * "method_937" is the render method as it is named in the shipped jar.
 */
@Mixin(targets = "io.github.axolotlclient.AxolotlClientConfig.screen.OptionsScreenBuilder$1", remap = false)
public abstract class SearchFieldMixin extends TextFieldWidget {

	protected SearchFieldMixin(int id, TextRenderer textRenderer, int x, int y, int width, int height) {
		super(id, textRenderer, x, y, width, height);
	}

	@Inject(method = "method_937", at = @At("HEAD"), remap = false)
	private void axolotlextras$box(CallbackInfo ci) {
		if (ExtrasModule.MODERN_MENUS.get()) {
			MenuSkin.textField(x - 4, y - 3, 108, 16, isFocused());
		}
	}

	@Inject(method = "method_937", at = @At("TAIL"), remap = false)
	private void axolotlextras$coverLine(CallbackInfo ci) {
		if (ExtrasModule.MODERN_MENUS.get()) {
			DrawableHelper.fill(x - 2, y + 11, x + 102, y + 12, isFocused() ? 0xFF222A3A : 0xFF1A2030);
		}
	}
}
