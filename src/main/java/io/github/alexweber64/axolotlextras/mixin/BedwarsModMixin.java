package io.github.alexweber64.axolotlextras.mixin;

import io.github.axolotlclient.AxolotlClientConfig.options.OptionCategory;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsMod;
import io.github.axolotlclient.modules.hypixel.bedwars.TeamUpgradesOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The settings of AxolotlClient's Team Upgrades HUD are left out of the Bed Wars menu, since that HUD is removed. */
@Mixin(value = BedwarsMod.class, remap = false)
public class BedwarsModMixin {

	@Shadow
	@Final
	protected TeamUpgradesOverlay upgradesOverlay;

	@Redirect(method = "init",
		at = @At(value = "INVOKE",
			target = "Lio/github/axolotlclient/AxolotlClientConfig/options/OptionCategory;add(Lio/github/axolotlclient/AxolotlClientConfig/common/options/OptionCategory;)V"),
		require = 1)
	private void axolotlextras$skipTeamUpgradesOptions(OptionCategory category,
		io.github.axolotlclient.AxolotlClientConfig.common.options.OptionCategory sub) {
		if (upgradesOverlay != null && upgradesOverlay.getNameKey().equals(sub.getName())) {
			return;
		}
		category.add(sub);
	}
}
