package io.github.alexweber64.axolotlextras.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.ui.MenuSkin;
import io.github.axolotlclient.AxolotlClientConfig.screen.ButtonWidgetList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ListWidget;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AxolotlClient's option pages render their own list instead of the vanilla one, including a scroll bar of their own.
 * Once the list is drawn, the restyled bar is drawn over it with the same geometry. "method_1053" is the render method
 * as it is named in the shipped jar.
 */
@Mixin(value = ButtonWidgetList.class, remap = false)
public abstract class ConfigListMixin extends ListWidget {

	protected ConfigListMixin(MinecraftClient client, int width, int height, int top, int bottom, int entryHeight) {
		super(client, width, height, top, bottom, entryHeight);
	}

	/**
	 * The list only draws its own bar when the maximum scroll is above zero, so it is told it is zero at that one place
	 * and draws no bar. The restyled bar below uses the real value.
	 */
	@Redirect(method = "method_1053", remap = false, at = @At(value = "INVOKE", remap = false,
		target = "Lio/github/axolotlclient/AxolotlClientConfig/screen/ButtonWidgetList;method_1068()I"))
	private int axolotlextras$noOwnBar(ButtonWidgetList self) {
		return ExtrasModule.MODERN_MENUS.get() ? 0 : self.getMaxScroll();
	}

	@Inject(method = "method_1053", at = @At("TAIL"), remap = false)
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
		// the list clips its own drawing to its area with a scissor box, which would also clip the new bar
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
		GlStateManager.color(1, 1, 1, 1);
		MenuSkin.scrollbar(x, yStart, yEnd, handleTop, handle, hovered);
	}
}
