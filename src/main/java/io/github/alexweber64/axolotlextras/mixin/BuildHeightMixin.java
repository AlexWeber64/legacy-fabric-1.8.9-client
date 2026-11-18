package io.github.alexweber64.axolotlextras.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.alexweber64.axolotlextras.bedwars.BuildHeightShade;
import net.minecraft.block.Block;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Build Height Indicator: right after each block face's vertices go into the chunk mesh (before the light values), the
 * face is darkened when {@link BuildHeightShade} says so. The game's own colour steps afterwards multiply on top of it.
 * Both the smooth lighting and the flat lighting paths are covered.
 */
@Mixin(BlockModelRenderer.class)
public class BuildHeightMixin {

	@Inject(method = {"renderQuadsSmooth", "renderQuadsFlat"}, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/BufferBuilder;faceTexture2(IIII)V"))
	private void axolotlextras$shadeTopLayer(CallbackInfo ci, @Local(argsOnly = true) BlockView view,
		@Local(argsOnly = true) Block block, @Local(argsOnly = true) BlockPos pos,
		@Local(argsOnly = true) BufferBuilder buffer) {
		BuildHeightShade.shade(view, block, pos, buffer);
	}
}
