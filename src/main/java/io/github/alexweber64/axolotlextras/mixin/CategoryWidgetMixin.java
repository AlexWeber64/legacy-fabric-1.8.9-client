package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.ui.MenuSkin;
import io.github.axolotlclient.AxolotlClientConfig.screen.widgets.BooleanWidget;
import io.github.axolotlclient.AxolotlClientConfig.screen.widgets.CategoryWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The category buttons of AxolotlClient's options screen draw themselves from the button texture. With Modern Menus on
 * they get the flat rounded look instead. "method_891" is the button render method as it is named in the shipped jar.
 */
@Mixin(value = CategoryWidget.class, remap = false)
public abstract class CategoryWidgetMixin extends ButtonWidget {

	@Shadow
	public BooleanWidget enabledButton;

	protected CategoryWidgetMixin(int id, int x, int y, String message) {
		super(id, x, y, message);
	}

	@Inject(method = "method_891", at = @At("HEAD"), cancellable = true, remap = false)
	private void axolotlextras$modernCategory(MinecraftClient client, int mouseX, int mouseY, CallbackInfo ci) {
		if (!ExtrasModule.MODERN_MENUS.get()) {
			return;
		}
		ci.cancel();
		if (!visible) {
			return;
		}
		boolean onChip = enabledButton != null && enabledButton.isMouseOver(client, mouseX, mouseY);
		hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height && !onChip;
		MenuSkin.button(client, x, y, width, height, message, active, hovered,
			enabledButton != null ? enabledButton.getWidth() : 0);
		if (enabledButton != null) {
			enabledButton.y = y + 2;
			enabledButton.render(client, mouseX, mouseY);
		}
	}
}
