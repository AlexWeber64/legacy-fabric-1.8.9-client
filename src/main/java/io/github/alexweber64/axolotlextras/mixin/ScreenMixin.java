package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {

	@Shadow
	protected MinecraftClient client;

	/**
	 * With a world loaded, screens darken it with a gradient. Skipping that makes menus and inventories see-through.
	 * Screens drawn without a world (the title screen) keep their normal background.
	 */
	/** Menus without a world behind them: a dark gradient instead of the tiled dirt texture. */
	@Inject(method = "renderDirtBackground", at = @At("HEAD"), cancellable = true)
	private void axolotlextras$modernBackground(int offset, CallbackInfo ci) {
		if (!ExtrasModule.MODERN_MENUS.get() || client == null) {
			return;
		}
		ci.cancel();
		// the game is drawn in scaled units, so use the scaled size
		net.minecraft.client.util.Window window = new net.minecraft.client.util.Window(client);
		int w = (int) Math.ceil(window.getScaledWidth());
		int h = (int) Math.ceil(window.getScaledHeight());
		int bands = 24;
		for (int i = 0; i < bands; i++) {
			int t = i * 255 / (bands - 1);
			int r = 14 + (29 - 14) * t / 255;
			int g = 18 + (36 - 18) * t / 255;
			int b = 24 + (51 - 24) * t / 255;
			net.minecraft.client.gui.DrawableHelper.fill(0, h * i / bands, w, h * (i + 1) / bands + 1,
				0xFF000000 | (r << 16) | (g << 8) | b);
		}
	}

	@Inject(method = "renderBackground(I)V", at = @At("HEAD"), cancellable = true)
	private void axolotlextras$transparentUi(int tint, CallbackInfo ci) {
		if (ExtrasModule.TRANSPARENT_UI.get() && client != null && client.world != null) {
			ci.cancel();
		}
	}
}
