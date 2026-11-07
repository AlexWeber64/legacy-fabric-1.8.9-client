package io.github.alexweber64.axolotlextras.ui;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import net.minecraft.client.gui.screen.Screen;

/** The hacks menu: the same list, search and switches as the Extras menu, with the hack options. */
public class HacksScreen extends ExtrasScreen {

	public HacksScreen(Screen parent) {
		super(parent, "§lHacks");
		section("axolotlextras.section.esp", ExtrasModule.PLAYER_ESP, ExtrasModule.ESP_TRACERS);
		section("axolotlextras.section.bedwarsInfo", ExtrasModule.GENERATOR_COUNTS, ExtrasModule.BED_DEFENSE,
			ExtrasModule.OBSIDIAN_ALERT);
	}
}
