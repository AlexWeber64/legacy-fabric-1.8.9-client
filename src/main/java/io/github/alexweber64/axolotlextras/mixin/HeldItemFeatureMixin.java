package io.github.alexweber64.axolotlextras.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.alexweber64.axolotlextras.ExtrasModule;
import net.minecraft.client.render.entity.feature.HeldItemRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7 sword block pose, item part: while a player blocks, the sword is moved and turned the way 1.7 drew it. Ported from the
 * OldAnimations mod (AxolotlClient, LGPL-3.0).
 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemFeatureMixin {

	@Inject(method = "render",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;)V"))
	private void axtras$oldBlockPose(LivingEntity entity, float limbAngle, float limbDistance, float tickDelta,
		float animationProgress, float headYaw, float headPitch, float scale, CallbackInfo ci) {
		if (!ExtrasModule.OLD_THIRD_PERSON.get() || !(entity instanceof PlayerEntity)) {
			return;
		}
		PlayerEntity player = (PlayerEntity) entity;
		ItemStack stack = player.getStackInHand();
		if (stack != null && player.isUsingItem() && stack.getUseAction() == UseAction.BLOCK) {
			GlStateManager.translate(0.05F, 0.0F, -0.1F);
			GlStateManager.rotate(-50.0F, 0.0F, 1.0F, 0.0F);
			GlStateManager.rotate(-10.0F, 1.0F, 0.0F, 0.0F);
			GlStateManager.rotate(-60.0F, 0.0F, 0.0F, 1.0F);
		}
	}
}
