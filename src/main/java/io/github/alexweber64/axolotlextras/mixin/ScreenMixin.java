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
	@Inject(method = "renderBackground(I)V", at = @At("HEAD"), cancellable = true)
	private void axolotlextras$transparentUi(int tint, CallbackInfo ci) {
		if (ExtrasModule.TRANSPARENT_UI.get() && client != null && client.world != null) {
			ci.cancel();
		}
	}
}
