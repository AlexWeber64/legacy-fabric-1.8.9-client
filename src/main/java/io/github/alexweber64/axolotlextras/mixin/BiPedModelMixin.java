package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import net.minecraft.client.render.entity.model.BiPedModel;
import net.minecraft.client.render.model.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7 sword block pose, arm part: 1.8 turns the blocking arm 30 degrees inward, 1.7 held it straight. Ported from the
 * OldAnimations mod (AxolotlClient, LGPL-3.0).
 */
@Mixin(BiPedModel.class)
public abstract class BiPedModelMixin {

	@Shadow
	public ModelPart rightArm;

	@Shadow
	public int rightArmPose;

	/** The model sets the arm turn to minus 30 degrees for pose 3 (blocking), and swing animations add to it afterwards. */
	@Inject(method = "setAngles", at = @At("TAIL"))
	private void axtras$straightBlockingArm(float limbAngle, float limbDistance, float animationProgress, float headYaw,
		float headPitch, float scale, net.minecraft.entity.Entity entity, CallbackInfo ci) {
		if (ExtrasModule.OLD_THIRD_PERSON.get() && rightArmPose == 3) {
			rightArm.posY += (float) (Math.PI / 6.0);
		}
	}
}
