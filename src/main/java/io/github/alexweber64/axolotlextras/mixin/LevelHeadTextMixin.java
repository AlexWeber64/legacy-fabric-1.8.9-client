package io.github.alexweber64.axolotlextras.mixin;

import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Shows an unknown level (-1) in AxolotlClient's lobby level head as "?" instead of "Level: -1". The method belongs to
 * AxolotlClient's own entity renderer patch, so this only applies if that patch was applied first; if it was not, the level
 * head just keeps its text.
 */
@Mixin(EntityRenderer.class)
public abstract class LevelHeadTextMixin {

	@ModifyVariable(method = "axolotlclient$drawLevelHead", at = @At("HEAD"), argsOnly = true, remap = false, require = 0)
	private String axolotlextras$unknownLevelAsQuestionMark(String text) {
		if (text != null && text.endsWith(" -1")) {
			return text.substring(0, text.length() - 2) + "?";
		}
		return text;
	}
}
