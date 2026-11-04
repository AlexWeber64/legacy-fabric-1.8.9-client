package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends HandledScreen {

	// Never called: a mixin class needs a constructor that matches its superclass.
	protected InventoryScreenMixin(ScreenHandler handler) {
		super(handler);
	}

	/**
	 * While a potion effect is active vanilla pushes the inventory 160 pixels to the right to make room for the effect
	 * list. This puts it back in the middle. The effect list is still drawn, to the left of the inventory.
	 */
	@Inject(method = "applyStatusEffectOffset", at = @At("TAIL"))
	private void axolotlextras$keepCentered(CallbackInfo ci) {
		if (ExtrasModule.KEEP_INVENTORY_CENTERED.get()) {
			this.x = (this.width - this.backgroundWidth) / 2;
		}
	}
}
