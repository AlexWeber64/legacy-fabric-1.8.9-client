package io.github.alexweber64.axolotlextras.ui;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import net.minecraft.client.gui.screen.Screen;

/** The hacks menu: the same list, search and switches as the Extras menu, with the hack options. */
public class HacksScreen extends ExtrasScreen {

	public HacksScreen(Screen parent) {
		super(parent, "§lHacks");
		section("axolotlextras.section.esp", ExtrasModule.PLAYER_ESP, ExtrasModule.ESP_TRACERS, ExtrasModule.ESP_STYLE,
			ExtrasModule.ESP_COLOR, ExtrasModule.ESP_RANGE, ExtrasModule.ESP_INVISIBLE, ExtrasModule.ESP_TEAMMATES);
		section("axolotlextras.section.nametags", ExtrasModule.NAMETAGS, ExtrasModule.NAMETAG_HEALTH,
			ExtrasModule.NAMETAG_DISTANCE, ExtrasModule.ENEMY_ARMOR, ExtrasModule.ARMOR_PROTECTION,
			ExtrasModule.ARMOR_HELD, ExtrasModule.ARMOR_HELD_COUNT, ExtrasModule.ARMOR_HELD_WOOL);
		section("axolotlextras.section.bedwarsInfo", ExtrasModule.GENERATOR_COUNTS, ExtrasModule.GEN_IRON,
			ExtrasModule.GEN_GOLD, ExtrasModule.GEN_DIAMOND, ExtrasModule.GEN_EMERALD, ExtrasModule.BED_DEFENSE, ExtrasModule.BED_DEFENSE_OWN);
		section("axolotlextras.section.alerts", ExtrasModule.OBSIDIAN_ALERT, ExtrasModule.OBSIDIAN_WAYPOINTS,
			ExtrasModule.OBSIDIAN_SOUND, ExtrasModule.INVIS_ALERT, ExtrasModule.INVIS_NEED_POTION, ExtrasModule.INVIS_WAYPOINTS, ExtrasModule.INVIS_SOUND,
			ExtrasModule.HIDDEN_TRAIL, ExtrasModule.TRAIL_TIME, ExtrasModule.TRAIL_STYLE, ExtrasModule.TRAIL_LABELS,
			ExtrasModule.TRAIL_CHAT,
			ExtrasModule.RUSH_ALERT, ExtrasModule.RUSH_DISTANCE, ExtrasModule.RUSH_SOUND, ExtrasModule.RUSH_TEAMMATES);
		section("axolotlextras.section.projectiles", ExtrasModule.FIREBALL_WARNING, ExtrasModule.FIREBALL_BLOCKS,
			ExtrasModule.FIREBALL_SOUND, ExtrasModule.PEARL_LANDING, ExtrasModule.TRAJECTORIES, ExtrasModule.TRAJECTORY_BOW,
			ExtrasModule.TRAJECTORY_THROWN);
		section("axolotlextras.section.labelSizes", ExtrasModule.SIZE_GENERATORS, ExtrasModule.SIZE_BEDS,
			ExtrasModule.SIZE_WAYPOINTS, ExtrasModule.SIZE_NAMETAGS, ExtrasModule.SIZE_ARMOR, ExtrasModule.SIZE_PROJECTILES);
		section("axolotlextras.section.radar", ExtrasModule.RADAR, ExtrasModule.RADAR_CENTER, ExtrasModule.RADAR_BED_FALLBACK, ExtrasModule.RADAR_RANGE, ExtrasModule.RADAR_SHAPE,
			ExtrasModule.RADAR_SIZE,
			ExtrasModule.RADAR_ICON_SIZE, ExtrasModule.RADAR_OPACITY, ExtrasModule.RADAR_ROTATE, ExtrasModule.RADAR_SELF,
			ExtrasModule.RADAR_TEAMMATES, ExtrasModule.RADAR_INVISIBLE, ExtrasModule.RADAR_EDGE, ExtrasModule.RADAR_BORDER,
			ExtrasModule.RADAR_NAMES, ExtrasModule.RADAR_HEIGHT, ExtrasModule.RADAR_GRID);
		section("axolotlextras.section.vision", ExtrasModule.NO_BLINDNESS);
		section("axolotlextras.section.hackKeys", ExtrasModule.HACK_KEY_NOTE);
	}
}
