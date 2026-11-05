package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.ui.MenuSkin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws every button (vanilla menus, AxolotlClient's menus and the HUD editor) in the flat rounded style. */
@Mixin(ButtonWidget.class)
public abstract class ButtonWidgetMixin {

	@Shadow
	public int x;
	@Shadow
	public int y;
	@Shadow
	protected int width;
	@Shadow
	protected int height;
	@Shadow
	public String message;
	@Shadow
	public boolean active;
	@Shadow
	public boolean visible;
	@Shadow
	protected boolean hovered;

	@Shadow
	protected abstract void mouseDragged(MinecraftClient client, int mouseX, int mouseY);

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void axolotlextras$modernButton(MinecraftClient client, int mouseX, int mouseY, CallbackInfo ci) {
		if (!ExtrasModule.MODERN_MENUS.get()) {
			return;
		}
		ci.cancel();
		if (!visible) {
			return;
		}
		hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
		// Sliders draw their handle in the middle, under the label.
		MenuSkin.button(client, x, y, width, height, message, active, hovered, 0, () -> mouseDragged(client, mouseX, mouseY));
	}
}
