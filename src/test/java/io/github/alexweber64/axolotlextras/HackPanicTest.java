package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import io.github.alexweber64.axolotlextras.hacks.HackPanic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HackPanicTest {

	private static final class FakeSwitch implements HackPanic.Switch {
		final String name;
		boolean on;

		FakeSwitch(String name, boolean on) {
			this.name = name;
			this.on = on;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public boolean on() {
			return on;
		}

		@Override
		public void set(boolean on) {
			this.on = on;
		}
	}

	@TempDir
	Path dir;

	@Test
	void offThenOnRestoresExactlyWhatWasOn() {
		FakeSwitch esp = new FakeSwitch("esp", true), tracers = new FakeSwitch("tracers", false), rush = new FakeSwitch("rush", true);
		List<FakeSwitch> all = Arrays.asList(esp, tracers, rush);
		HackPanic panic = new HackPanic(dir.resolve("panic.txt"));
		assertEquals(2, panic.allOff(all));
		assertFalse(esp.on || tracers.on || rush.on);
		assertTrue(panic.active());
		assertEquals(2, panic.restore(all));
		assertTrue(esp.on && rush.on);
		assertFalse(tracers.on, "a switch that was off stays off");
		assertFalse(panic.active());
		assertFalse(Files.exists(dir.resolve("panic.txt")));
	}

	@Test
	void pressingOffTwiceKeepsTheFirstMemoryAndAddsNewOnes() {
		FakeSwitch esp = new FakeSwitch("esp", true), tracers = new FakeSwitch("tracers", false);
		List<FakeSwitch> all = Arrays.asList(esp, tracers);
		HackPanic panic = new HackPanic(null);
		panic.allOff(all);
		assertEquals(0, panic.allOff(all), "nothing left to turn off");
		tracers.on = true; // switched on by hand while the hacks were off
		assertEquals(1, panic.allOff(all));
		assertEquals(2, panic.restore(all));
		assertTrue(esp.on && tracers.on);
	}

	@Test
	void restoreWithoutOffDoesNothing() {
		FakeSwitch esp = new FakeSwitch("esp", false);
		HackPanic panic = new HackPanic(null);
		assertEquals(-1, panic.restore(Arrays.asList(esp)));
		assertFalse(esp.on);
	}

	@Test
	void theMemorySurvivesARestart() {
		Path file = dir.resolve("sub").resolve("panic.txt");
		FakeSwitch esp = new FakeSwitch("esp", true), nametags = new FakeSwitch("nametags", true);
		new HackPanic(file).allOff(Arrays.asList(esp, nametags));
		HackPanic afterRestart = new HackPanic(file);
		assertTrue(afterRestart.active());
		assertEquals(2, afterRestart.restore(Arrays.asList(esp, nametags)));
		assertTrue(esp.on && nametags.on);
	}
}
