package io.github.alexweber64.axolotlextras.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.alexweber64.axolotlextras.ExtrasModule;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.PumpkinBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7 visuals for the item in your hand: swinging while blocking, eating or drawing a bow, the 1.7 positions of items and
 * blocks, the bow pose and the fishing rod flip. Ported from the OldAnimations mod (AxolotlClient, LGPL-3.0), whose code
 * is written for other class names.
 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

	@Shadow
	@Final
	private MinecraftClient client;

	@Shadow
	@Final
	private ItemRenderer itemRenderer;

	@Shadow
	private ItemStack mainHand;

	@Unique
	private float axtras$tickDelta;

	@Inject(method = "renderArmHoldingItem", at = @At("HEAD"))
	private void axtras$rememberTickDelta(float tickDelta, CallbackInfo ci) {
		axtras$tickDelta = tickDelta;
	}

	/** Vanilla passes 0 as the swing progress while eating, blocking and drawing a bow, so the swing is not shown. */
	@ModifyArg(method = "renderArmHoldingItem",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipAndSwingOffset(FF)V"),
		slice = @Slice(
			from = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEatOrDrinkTransformation(Lnet/minecraft/client/network/AbstractClientPlayerEntity;F)V"),
			to = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyBowTransformation(FLnet/minecraft/client/network/AbstractClientPlayerEntity;)V")),
		index = 1)
	private float axtras$swingWhileUsing(float swingProgress) {
		if (ExtrasModule.OLD_BLOCK_HITTING.get() && client.player != null) {
			return client.player.getHandSwingProgress(axtras$tickDelta);
		}
		return swingProgress;
	}

	/** The 1.7 bow pose: the whole bow transformation is wrapped in two extra rotations. */
	@Redirect(method = "applyBowTransformation",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;scale(FFF)V"))
	private void axtras$oldBowRotation(float x, float y, float z) {
		boolean on = ExtrasModule.OLD_FIRST_PERSON.get();
		if (on) {
			GlStateManager.rotate(-335.0F, 0.0F, 0.0F, 1.0F);
			GlStateManager.rotate(-50.0F, 0.0F, 1.0F, 0.0F);
		}
		GlStateManager.scale(x, y, z);
		if (on) {
			GlStateManager.rotate(50.0F, 0.0F, 1.0F, 0.0F);
			GlStateManager.rotate(335.0F, 0.0F, 0.0F, 1.0F);
		}
	}

	/** Items that are flipped (the fishing rod) are turned around before the item is drawn. */
	@Inject(method = "renderArmHoldingItem",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;)V"))
	private void axtras$rodFlip(float tickDelta, CallbackInfo ci) {
		if (ExtrasModule.OLD_FIRST_PERSON.get() && mainHand != null && mainHand.getItem().shouldRotate()) {
			GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
		}
	}

	/** The 1.7 position and size of the item in your hand. These are the 1.7 values, moved to fit the 1.8 item models. */
	@Inject(method = "renderItem",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderer;renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;)V"))
	private void axtras$oldPositions(LivingEntity entity, ItemStack stack, ModelTransformation.Mode mode, CallbackInfo ci) {
		if (!ExtrasModule.OLD_FIRST_PERSON.get() || mode != ModelTransformation.Mode.FIRST_PERSON) {
			return;
		}
		BakedModel model = itemRenderer.getModels().getModel(stack);
		Item item = stack.getItem();
		if (model != null && model.isBuiltin()) {
			// skulls and banners draw themselves; only the chest is turned
			if (item instanceof BlockItem && Block.getBlockFromItem(item) instanceof ChestBlock) {
				GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
			}
			return;
		}
		if (itemRenderer.hasDepth(stack)) {
			if (item instanceof BlockItem) {
				GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
				Block block = Block.getBlockFromItem(item);
				if (block instanceof FurnaceBlock || block instanceof DispenserBlock || block instanceof PumpkinBlock) {
					GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
				}
			}
		} else {
			GlStateManager.translate(0.0F, -0.3F, 0.0F);
			GlStateManager.scale(1.5F, 1.5F, 1.5F);
			GlStateManager.rotate(50.0F, 0.0F, 1.0F, 0.0F);
			GlStateManager.rotate(335.0F, 0.0F, 0.0F, 1.0F);
			GlStateManager.translate(-0.9375F, -0.0625F, 0.0F);
			GlStateManager.translate(0.5F, 0.5F, 0.25F);
			GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
			GlStateManager.translate(0.0F, 0.0F, 0.28125F);
		}
	}
}
