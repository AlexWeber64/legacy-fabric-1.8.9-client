package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.hacks.HackRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reads the sounds, particles and block breaking the server sends, for the Hidden Player Trail. Only reads them, they are
 * still handled as usual. Each handler first runs on the network thread and hands itself to the main thread, so only the
 * main thread run counts.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class HiddenTrailMixin {

	@Inject(method = "onPlaySound", at = @At("HEAD"))
	private void axolotlextras$trailSound(PlaySoundIdS2CPacket packet, CallbackInfo ci) {
		if (MinecraftClient.getInstance().isOnThread()) {
			HackRenderer.onServerSound(packet.getSound(), packet.getX(), packet.getY(), packet.getZ());
		}
	}

	@Inject(method = "onParticle", at = @At("HEAD"))
	private void axolotlextras$trailParticle(ParticleS2CPacket packet, CallbackInfo ci) {
		if (MinecraftClient.getInstance().isOnThread()) {
			HackRenderer.onServerParticle(packet.getParameters(), packet.getX(), packet.getY(), packet.getZ());
		}
	}

	@Inject(method = "onBlockDestroyProgress", at = @At("HEAD"))
	private void axolotlextras$trailBreaking(BlockBreakingProgressS2CPacket packet, CallbackInfo ci) {
		if (MinecraftClient.getInstance().isOnThread()) {
			HackRenderer.onBlockBreaking(packet.getEntityId(), packet.getPos(), packet.getProgress());
		}
	}
}
