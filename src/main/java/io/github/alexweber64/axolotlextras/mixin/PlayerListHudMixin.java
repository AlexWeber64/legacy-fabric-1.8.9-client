package io.github.alexweber64.axolotlextras.mixin;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.axolotlclient.modules.hypixel.bedwars.BedwarsMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

	/** Room for the KDR text: PlayerListHud.render keeps the width of the score column in its 7th int local (slot 12). */
	@ModifyVariable(method = "render", at = @At("STORE"), ordinal = 6)
	private int axolotlextras$widenScoreColumn(int width) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (ExtrasModule.KDR_TAB.get() && width > 0 && client.world != null
			&& BedwarsMod.getInstance().isEnabled() && BedwarsMod.getInstance().inGame()) {
			return width + client.textRenderer.getStringWidth("00.00") + 4;
		}
		return width;
	}
}
