package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ui.RoundedRect;
import io.github.axolotlclient.AxolotlClientConfig.Color;
import io.github.axolotlclient.modules.hud.gui.entry.BoxHudEntry;
import io.github.axolotlclient.modules.hud.util.Rectangle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BoxHudEntry.class, remap = false)
public class BoxHudEntryMixin {

	/** Draws the background of every HUD box with rounded corners (see the HUD Corners option). */
	@Redirect(method = "render",
		at = @At(value = "INVOKE",
			target = "Lio/github/axolotlclient/modules/hud/gui/entry/BoxHudEntry;fillRect(Lio/github/axolotlclient/modules/hud/util/Rectangle;Lio/github/axolotlclient/AxolotlClientConfig/Color;)V"))
	private void axolotlextras$roundedBackground(Rectangle rect, Color color) {
		RoundedRect.fill(rect, color);
	}
}
