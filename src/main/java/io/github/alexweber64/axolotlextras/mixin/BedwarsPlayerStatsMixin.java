package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.bedwars.StatsOrigin;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsPlayerStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BedwarsPlayerStats.class, remap = false)
public class BedwarsPlayerStatsMixin implements StatsOrigin {

	@Unique
	private boolean axolotlextras$generated;

	@Inject(method = "generateFake", at = @At("RETURN"))
	private static void axolotlextras$flagGenerated(String name, CallbackInfoReturnable<BedwarsPlayerStats> cir) {
		BedwarsPlayerStats stats = cir.getReturnValue();
		if (stats != null) {
			((StatsOrigin) stats).axolotlextras$markGenerated();
		}
	}

	@Override
	public void axolotlextras$markGenerated() {
		axolotlextras$generated = true;
	}

	@Override
	public boolean axolotlextras$isGenerated() {
		return axolotlextras$generated;
	}
}
