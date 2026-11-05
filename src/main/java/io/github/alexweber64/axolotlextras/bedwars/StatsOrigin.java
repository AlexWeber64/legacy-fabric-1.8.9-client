package io.github.alexweber64.axolotlextras.bedwars;

/**
 * Added to AxolotlClient's BedwarsPlayerStats by a mixin. AxolotlClient fills in random numbers when it cannot load a
 * player's real stats (for example without a Hypixel API key), and this tells the two apart.
 */
public interface StatsOrigin {

	void axolotlextras$markGenerated();

	boolean axolotlextras$isGenerated();
}
