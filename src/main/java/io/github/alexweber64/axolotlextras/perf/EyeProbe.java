package io.github.alexweber64.axolotlextras.perf;

/** Lets the test driver read the camera height the sneaking mixin computed. Implemented by the mixin on the game renderer. */
public interface EyeProbe {

	/** The eye height used for the camera in the last frame. */
	float axtras$shownEyeHeight();
}
