package io.github.alexweber64.axolotlextras.hacks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The emergency toggle: {@link #allOff} switches every hack off and remembers which were on, {@link #restore} switches
 * exactly those back on. The remembered set is kept in a small file, so it survives a restart or a crash while the hacks
 * are off. No Minecraft imports, so it can be unit tested.
 */
public final class HackPanic {

	/** One hack switch. */
	public interface Switch {
		String name();

		boolean on();

		void set(boolean on);
	}

	private static final String ACTIVE = "hacks off";

	private final Path file;
	private final Set<String> remembered = new LinkedHashSet<>();
	private boolean active;

	/** @param file where the remembered switches are kept while the hacks are off (deleted when they are back on) */
	public HackPanic(Path file) {
		this.file = file;
		try {
			if (file != null && Files.exists(file)) {
				List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
				if (!lines.isEmpty() && ACTIVE.equals(lines.get(0).trim())) {
					active = true;
					for (String line : lines.subList(1, lines.size())) {
						if (!line.trim().isEmpty()) {
							remembered.add(line.trim());
						}
					}
				}
			}
		} catch (IOException e) {
			// Unreadable: start with nothing remembered.
		}
	}

	/** True while the hacks are switched off by {@link #allOff} and not yet restored. */
	public boolean active() {
		return active;
	}

	/** The switches that {@link #restore} would turn back on. */
	public Set<String> remembered() {
		return Collections.unmodifiableSet(remembered);
	}

	/**
	 * Switches every hack off. The ones that were on are remembered; pressing it again while the hacks are off adds any
	 * that were switched on by hand since, and never forgets the earlier ones.
	 *
	 * @return how many switches were turned off now
	 */
	public int allOff(List<? extends Switch> switches) {
		int count = 0;
		for (Switch s : switches) {
			if (s.on()) {
				remembered.add(s.name());
				s.set(false);
				count++;
			}
		}
		active = true;
		save();
		return count;
	}

	/**
	 * Switches the remembered hacks back on and forgets them.
	 *
	 * @return how many switches were turned on, or -1 when the hacks were not switched off by {@link #allOff}
	 */
	public int restore(List<? extends Switch> switches) {
		if (!active) {
			return -1;
		}
		int count = 0;
		for (Switch s : switches) {
			if (remembered.contains(s.name()) && !s.on()) {
				s.set(true);
				count++;
			}
		}
		remembered.clear();
		active = false;
		save();
		return count;
	}

	private void save() {
		if (file == null) {
			return;
		}
		try {
			if (!active) {
				Files.deleteIfExists(file);
				return;
			}
			List<String> lines = new ArrayList<>();
			lines.add(ACTIVE);
			lines.addAll(remembered);
			Files.createDirectories(file.getParent());
			Files.write(file, lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			// The switches still change; only a restart while they are off would forget the remembered set.
		}
	}
}
