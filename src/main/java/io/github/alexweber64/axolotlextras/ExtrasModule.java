package io.github.alexweber64.axolotlextras;

import io.github.axolotlclient.modules.AbstractModule;

/**
 * Entrypoint for the "axolotlclient.module" extension point. AxolotlClient calls {@link #init()} after its own
 * config exists and before the config manager loads the saved file, so options added here are saved and loaded
 * together with the rest of AxolotlClient's settings.
 */
public class ExtrasModule extends AbstractModule {

	@Override
	public void init() {
	}
}
