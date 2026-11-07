package io.github.alexweber64.axolotlextras.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

/** A label on the HUD at a point in the world: an optional text, then a row of icons with a number on each. */
final class HudLabel {
	final double x, y, z;
	final List<ItemStack> icons = new ArrayList<>();
	final List<String> numbers = new ArrayList<>();
	/** Shown before the icons; may hold colour codes. */
	String text;
	/** Moves the label up on screen by this many GUI pixels (to clear a name tag, for example). */
	float raise;
	/** The size of the label (1 is normal), from its Label Sizes option. */
	float scale = 1f;

	HudLabel(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	HudLabel icon(ItemStack stack, String number) {
		icons.add(stack);
		numbers.add(number);
		return this;
	}
}
