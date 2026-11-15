package io.github.alexweber64.axolotlextras.mixin;

import java.util.Map;

import io.github.axolotlclient.modules.hud.HudManager;
import io.github.axolotlclient.modules.hypixel.bedwars.TeamUpgradesOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Takes AxolotlClient's own "Team Upgrades" HUD out of the HUD list and the HUD editor. The add-on's Upgrade Display
 * (bedwars/UpgradeDisplayHud) replaces it and does not use it. The overlay object itself stays, because AxolotlClient's
 * Bed Wars code still calls it when a game starts and ends; it is just never drawn. Its settings are removed from the
 * Bed Wars menu in {@link BedwarsModMixin}.
 */
@Mixin(value = HudManager.class, remap = false)
public class TeamUpgradesRemovalMixin {

	@Redirect(method = "init",
		at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
		require = 1)
	private Object axolotlextras$skipTeamUpgrades(Map<Object, Object> entries, Object id, Object entry) {
		if (entry instanceof TeamUpgradesOverlay) {
			return null;
		}
		return entries.put(id, entry);
	}
}
