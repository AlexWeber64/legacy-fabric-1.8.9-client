package io.github.alexweber64.axolotlextras.hacks;

import java.util.ArrayList;
import java.util.List;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.axolotlclient.AxolotlClientConfig.options.Option;
import io.github.axolotlclient.modules.hud.gui.AbstractHudEntry;
import io.github.axolotlclient.modules.hud.util.DrawPosition;
import net.minecraft.util.Identifier;

/**
 * The radar's place in AxolotlClient's HUD editor, so it can be dragged like any HUD module. It is shown whenever the
 * Radar hack is on (so the hack keys and the emergency toggle still switch it), its size is the Radar Size option, and
 * only its position is saved here; every other radar option stays in the Hacks menu.
 */
public class RadarHud extends AbstractHudEntry {

	public static final Identifier ID = new Identifier("axolotlextras", "extrasradar");

	public RadarHud() {
		super(100, 100);
	}

	/** On with the Radar hack, not with a switch of its own. */
	@Override
	public boolean isEnabled() {
		return ExtrasModule.RADAR.get();
	}

	@Override
	public void setEnabled(boolean value) {
		ExtrasModule.RADAR.set(value);
	}

	@Override
	public void render(float delta) {
		DrawPosition pos = fitSize();
		HackRenderer.renderRadar(pos.x, pos.y, delta);
	}

	/** In the HUD editor: the usual hover box, and the radar itself on top (the world is there behind the editor). */
	@Override
	public void renderPlaceholder(float delta) {
		DrawPosition pos = fitSize();
		renderPlaceholderBackground();
		HackRenderer.renderRadar(pos.x, pos.y, delta);
	}

	/** Follows the Radar Size option, and returns where the radar's top left corner is on screen. */
	private DrawPosition fitSize() {
		Integer size = ExtrasModule.RADAR_SIZE.get();
		int s = size == null ? 100 : size;
		if (getWidth() != s || getHeight() != s) {
			setWidth(s);
			setHeight(s);
			onBoundsUpdate();
		}
		return getTruePos();
	}

	@Override
	public boolean movable() {
		return true;
	}

	/**
	 * Nothing but the position is saved here: no own on/off switch (the Radar hack is that) and no scale (Radar Size is
	 * that), so the two menus cannot disagree.
	 */
	@Override
	public List<Option<?>> getConfigurationOptions() {
		return new ArrayList<>();
	}

	/** Top right, where the radar sat before it could be moved. */
	@Override
	public double getDefaultX() {
		return 0.99;
	}

	@Override
	public double getDefaultY() {
		return 0.01;
	}

	@Override
	public Identifier getId() {
		return ID;
	}
}
