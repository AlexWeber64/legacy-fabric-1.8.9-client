package io.github.alexweber64.axolotlextras.mixin;

import java.util.HashMap;
import java.util.function.Function;

import io.github.axolotlclient.modules.hypixel.HypixelAbstractionLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * While a player's data is not loaded, AxolotlClient's level head shows a random level it makes up (about 150, plus or
 * minus). This returns -1 (unknown) instead, which the level head text patch shows as "?".
 */
@Mixin(value = HypixelAbstractionLayer.class, remap = false)
public abstract class HypixelAbstractionLayerMixin {

	@Redirect(method = "getPlayerLevel",
		at = @At(value = "INVOKE", target = "Ljava/util/HashMap;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"))
	private static Object axolotlextras$noMadeUpLevel(HashMap<String, Integer> temporaryValues, Object key,
		Function<String, Integer> randomLevel) {
		return Integer.valueOf(-1);
	}
}
