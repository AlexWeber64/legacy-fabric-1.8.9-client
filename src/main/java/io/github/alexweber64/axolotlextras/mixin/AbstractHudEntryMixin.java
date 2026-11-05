package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.ui.RoundedRect;
import io.github.axolotlclient.AxolotlClientConfig.Color;
import io.github.axolotlclient.modules.hud.gui.AbstractHudEntry;
import io.github.axolotlclient.modules.hud.util.DrawUtil;
import io.github.axolotlclient.modules.hud.util.Rectangle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The boxes of the HUD editor: AxolotlClient draws each module as a plain translucent rectangle with a black outline.
 * Here they get rounded corners, a soft fill and an accent outline, brighter under the mouse.
 */
@Mixin(value = AbstractHudEntry.class, remap = false)
public class AbstractHudEntryMixin {

	private static final int ACCENT = 0xFF35D38A;

	@Redirect(method = "renderPlaceholderBackground",
		at = @At(value = "INVOKE",
			target = "Lio/github/axolotlclient/modules/hud/gui/AbstractHudEntry;fillRect(Lio/github/axolotlclient/modules/hud/util/Rectangle;Lio/github/axolotlclient/AxolotlClientConfig/Color;)V"))
	private void axolotlextras$selectorFill(Rectangle rect, Color color) {
		if (!ExtrasModule.MODERN_MENUS.get()) {
			DrawUtil.fillRect(rect, color);
			return;
		}
		// AxolotlClient fills hovered boxes with alpha 100 and the others with alpha 50.
		boolean hovered = color.getAlpha() >= 100;
		RoundedRect.fillShape(rect.x(), rect.y(), rect.width(), rect.height(), RoundedRect.SMALL,
			hovered ? 0x6635D38A : 0x40FFFFFF);
	}

	@Redirect(method = "renderPlaceholderBackground",
		at = @At(value = "INVOKE",
			target = "Lio/github/axolotlclient/modules/hud/gui/AbstractHudEntry;outlineRect(Lio/github/axolotlclient/modules/hud/util/Rectangle;Lio/github/axolotlclient/AxolotlClientConfig/Color;)V"))
	private void axolotlextras$selectorOutline(Rectangle rect, Color color) {
		if (!ExtrasModule.MODERN_MENUS.get()) {
			DrawUtil.outlineRect(rect, color);
			return;
		}
		RoundedRect.outlineShape(rect.x(), rect.y(), rect.width(), rect.height(), RoundedRect.SMALL, ACCENT);
	}
}
