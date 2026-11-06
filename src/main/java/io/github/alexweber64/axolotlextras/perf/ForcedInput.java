package io.github.alexweber64.axolotlextras.perf;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;

/**
 * Movement input for the test tools. AxolotlClient reads the real keyboard for the movement keys, so pressing them in code
 * has no effect; this wraps the player's input instead and forces the wanted state after the normal keyboard input.
 */
public final class ForcedInput extends KeyboardInput {

	public static boolean forward;
	public static boolean sneak;
	public static boolean jump;

	private ForcedInput(GameOptions options) {
		super(options);
	}

	/** Puts this input on the player, if it is not there yet. */
	public static void install(MinecraftClient client) {
		if (client.player != null && !(client.player.input instanceof ForcedInput)) {
			client.player.input = new ForcedInput(client.options);
		}
	}

	public static void releaseAll() {
		forward = false;
		sneak = false;
		jump = false;
	}

	@Override
	public void tick() {
		super.tick();
		if (forward) {
			movementForward = 1.0F;
		}
		if (jump) {
			jumping = true;
		}
		if (sneak) {
			sneaking = true;
			movementSideways *= 0.3F;
			movementForward *= 0.3F;
		}
	}
}
