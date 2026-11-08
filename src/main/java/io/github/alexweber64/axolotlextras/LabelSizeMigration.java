package io.github.alexweber64.axolotlextras;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Carries the label sizes picked with the old size buttons ("axolotlextras.scale.50") over to the size sliders, once.
 * The sliders have new option names, because AxolotlClient cannot read the old text values into a number option. A slider
 * that has been saved already is left alone, so a later change is never overwritten. No Minecraft imports, so it can be
 * unit tested.
 */
public final class LabelSizeMigration {

	/** Old button option name, new slider option name. */
	static final String[][] RENAMED = {
		{"axolotlextras.sizeGenerators", "axolotlextras.labelSizeGenerators"},
		{"axolotlextras.sizeBeds", "axolotlextras.labelSizeBeds"},
		{"axolotlextras.sizeWaypoints", "axolotlextras.labelSizeWaypoints"},
		{"axolotlextras.sizeNametags", "axolotlextras.labelSizeNametags"},
		{"axolotlextras.sizeArmor", "axolotlextras.labelSizeArmor"},
		{"axolotlextras.sizeProjectiles", "axolotlextras.labelSizeProjectiles"},
	};

	/** The slider range, in percent. */
	public static final int MIN = 25, MAX = 200;

	private LabelSizeMigration() {
	}

	/**
	 * @param hacks the saved Hacks options, name to value as text
	 * @return the sliders to set, new option name to percent (clamped to the slider range)
	 */
	public static Map<String, Integer> carriedOver(Map<String, String> hacks) {
		Map<String, Integer> out = new LinkedHashMap<>();
		for (String[] pair : RENAMED) {
			String old = hacks.get(pair[0]);
			if (old == null || hacks.containsKey(pair[1])) {
				continue;
			}
			try {
				int percent = Integer.parseInt(old.substring(old.lastIndexOf('.') + 1));
				out.put(pair[1], Math.max(MIN, Math.min(MAX, percent)));
			} catch (NumberFormatException e) {
				// not a size value: keep the slider's default
			}
		}
		return out;
	}
}
