package io.github.alexweber64.axolotlextras.hacks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;

/**
 * A chat line and a sound when an enemy comes near your own bed. Each enemy is announced when they come in, and again
 * only after they left and a cooldown passed. Local only, nothing is sent.
 */
final class RushAlert {

	/** An enemy has to get this much farther out than the alert distance before they count as gone. */
	private static final double LEAVE_MARGIN = 8;

	/** Ticks before the same enemy is announced again. */
	private static final int COOLDOWN = 400;

	/** Height difference that still counts as near (rushers bridge at about bed height). */
	private static final double MAX_DY = 20;

	private final Set<UUID> inside = new HashSet<>();
	private final Map<UUID, Integer> lastAlert = new HashMap<>();

	void reset() {
		inside.clear();
		lastAlert.clear();
	}

	/**
	 * @param players   the players to watch: enemies, and teammates too with Rush: Include Teammates (NPCs left out)
	 * @param teammates the UUIDs of those players who are on your team, marked "[Teammate]" in the chat line
	 * @param quiet     true right after the start, when everyone still stands on their own island: players are noted
	 *                  as inside but not announced
	 */
	void tick(MinecraftClient client, BedTracker.Info ownBed, List<PlayerEntity> players, Set<UUID> teammates,
		double radius, int tick, boolean quiet) {
		if (ownBed == null) {
			return;
		}
		for (PlayerEntity player : players) {
			double dx = player.x - ownBed.centerX(), dz = player.z - ownBed.centerZ();
			double distance = Math.sqrt(dx * dx + dz * dz);
			UUID id = player.getUuid();
			if (distance <= radius && Math.abs(player.y - ownBed.bed.y) <= MAX_DY) {
				if (inside.add(id) && !quiet) {
					Integer last = lastAlert.get(id);
					if (last == null || tick - last >= COOLDOWN) {
						lastAlert.put(id, tick);
						client.inGameHud.getChatHud().addMessage(new LiteralText(HackMath.rushMessage(
							player.getName().asFormattedString(), teammates.contains(id), Math.round(distance))));
						if (ExtrasModule.RUSH_SOUND.get()) {
							client.player.playSound("note.pling", 1f, 0.5f);
						}
					}
				}
			} else if (distance > radius + LEAVE_MARGIN) {
				inside.remove(id);
			}
		}
	}
}
