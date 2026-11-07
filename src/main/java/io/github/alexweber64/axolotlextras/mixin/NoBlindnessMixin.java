package io.github.alexweber64.axolotlextras.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.alexweber64.axolotlextras.ExtrasModule;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * No Blindness: the fog and sky colour code (updateFog) and the fog distance code (renderFog) are told the camera entity is
 * not blind. Only these two drawing methods are changed, so everything else that checks for blindness (sprinting, for
 * example) still sees the real effect.
 */
@Mixin(GameRenderer.class)
public class NoBlindnessMixin {

	@WrapOperation(method = {"updateFog", "renderFog"}, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z"))
	private boolean axolotlextras$noBlindness(LivingEntity entity, StatusEffect effect, Operation<Boolean> original) {
		if (effect == StatusEffect.BLINDNESS && ExtrasModule.NO_BLINDNESS.get()) {
			return false;
		}
		return original.call(entity, effect);
	}
}
