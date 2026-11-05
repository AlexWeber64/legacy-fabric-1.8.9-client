package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.ui.MenuSkin;
import net.minecraft.client.gui.widget.ListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scrolling lists (AxolotlClient's option pages, the server list, resource packs and so on) draw a black bar with a grey
 * handle. The restyled bar is drawn over it once the list is done, with the same geometry.
 */
@Mixin(ListWidget.class)
public abstract class ListWidgetMixin {

	@Shadow
	protected int yStart;
	@Shadow
	protected int yEnd;
	@Shadow
	protected float scrollAmount;
	@Shadow
	protected boolean visible;

	@Shadow
	public abstract int getMaxScroll();

	@Shadow
	protected abstract int getMaxPosition();

	@Shadow
	protected abstract int getScrollbarPosition();

	@Inject(method = "render", at = @At("TAIL"))
	private void axolotlextras$modernScrollbar(int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!ExtrasModule.MODERN_MENUS.get() || !visible) {
			return;
		}
		int maxScroll = getMaxScroll();
		if (maxScroll <= 0) {
			return;
		}
		int height = yEnd - yStart;
		int handle = height * height / getMaxPosition();
		handle = Math.max(32, Math.min(handle, height - 8));
		int handleTop = (int) scrollAmount * (height - handle) / maxScroll + yStart;
		if (handleTop < yStart) {
			handleTop = yStart;
		}
		int x = getScrollbarPosition();
		boolean hovered = mouseX >= x && mouseX < x + 6 && mouseY >= yStart && mouseY < yEnd;
		MenuSkin.scrollbar(x, yStart, yEnd, handleTop, handle, hovered);
	}
}
