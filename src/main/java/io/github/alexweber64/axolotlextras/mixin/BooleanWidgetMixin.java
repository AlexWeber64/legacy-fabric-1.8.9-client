package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.ui.RoundedRect;
import io.github.axolotlclient.AxolotlClientConfig.options.BooleanOption;
import io.github.axolotlclient.AxolotlClientConfig.screen.widgets.BooleanWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** AxolotlClient's ON/OFF switch buttons, drawn flat and rounded with a green ON and a red OFF. */
@Mixin(value = BooleanWidget.class, remap = false)
public abstract class BooleanWidgetMixin extends ButtonWidget {

	@Shadow
	public BooleanOption option;

	protected BooleanWidgetMixin(int id, int x, int y, String message) {
		super(id, x, y, message);
	}

	@Inject(method = "method_891", at = @At("HEAD"), cancellable = true, remap = false)
	private void axolotlextras$modernSwitch(MinecraftClient client, int mouseX, int mouseY, CallbackInfo ci) {
		if (!ExtrasModule.MODERN_MENUS.get()) {
			return;
		}
		ci.cancel();
		if (!visible) {
			return;
		}
		boolean on = option.get();
		boolean locked = option.getForceDisabled();
		hovered = !locked && mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
		RoundedRect.fillShape(x, y, width, height, RoundedRect.SMALL, hovered ? 0xF02B3447 : 0xE6141821);
		RoundedRect.outlineShape(x, y, width, height, RoundedRect.SMALL, locked ? 0xFF4A5164 : on ? 0xFF3FA96B : 0xFF9B4A4A);
		String text = I18n.translate(on ? "options.on" : "options.off");
		int color = locked ? 0xFF6B7385 : on ? 0xFF55FF7F : 0xFFFF6B6B;
		client.textRenderer.draw(text, x + (width - client.textRenderer.getStringWidth(text)) / 2f,
			y + (height - 8) / 2f, color, false);
	}
}
