package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class LabelSizeMigrationTest {

	@Test
	void oldButtonValuesBecomeSliderPercentages() {
		Map<String, String> saved = new HashMap<>();
		saved.put("axolotlextras.sizeGenerators", "axolotlextras.scale.25");
		saved.put("axolotlextras.sizeNametags", "axolotlextras.scale.50");
		saved.put("axolotlextras.sizeProjectiles", "axolotlextras.scale.200");
		Map<String, Integer> out = LabelSizeMigration.carriedOver(saved);
		assertEquals(25, out.get("axolotlextras.labelSizeGenerators"));
		assertEquals(50, out.get("axolotlextras.labelSizeNametags"));
		assertEquals(200, out.get("axolotlextras.labelSizeProjectiles"));
		assertEquals(3, out.size());
	}

	@Test
	void aSavedSliderIsNeverOverwritten() {
		Map<String, String> saved = new HashMap<>();
		saved.put("axolotlextras.sizeBeds", "axolotlextras.scale.25");
		saved.put("axolotlextras.labelSizeBeds", "140");
		assertTrue(LabelSizeMigration.carriedOver(saved).isEmpty());
	}

	@Test
	void oddValuesAreSkippedOrClamped() {
		Map<String, String> saved = new HashMap<>();
		saved.put("axolotlextras.sizeArmor", "something else");
		saved.put("axolotlextras.sizeWaypoints", "axolotlextras.scale.10");
		Map<String, Integer> out = LabelSizeMigration.carriedOver(saved);
		assertEquals(1, out.size());
		assertEquals(LabelSizeMigration.MIN, out.get("axolotlextras.labelSizeWaypoints"));
	}

	@Test
	void nothingSavedMeansNothingToDo() {
		assertTrue(LabelSizeMigration.carriedOver(new HashMap<>()).isEmpty());
	}
}
