package io.github.alexweber64.axolotlextras.quickplay;

/**
 * The games shown in the quickplay menu. The commands are Hypixel's "/play" mode names. They come from memory and
 * old community lists and have not been checked against a current official list, so a wrong one will simply get an
 * error message from the server. Edit them here if Hypixel renames a mode.
 */
public final class QuickplayEntries {

	public static final class Entry {
		public final String label;
		public final String command;

		Entry(String label, String command) {
			this.label = label;
			this.command = command;
		}
	}

	public static final class Group {
		public final String title;
		public final Entry[] entries;

		Group(String title, Entry... entries) {
			this.title = title;
			this.entries = entries;
		}
	}

	public static final Group[] GROUPS = {
		new Group("Bed Wars",
			new Entry("Solo", "bedwars_eight_one"),
			new Entry("Doubles", "bedwars_eight_two"),
			new Entry("3v3v3v3", "bedwars_four_three"),
			new Entry("4v4v4v4", "bedwars_four_four"),
			new Entry("4v4", "bedwars_two_four")),
		new Group("SkyWars",
			new Entry("Solo Normal", "solo_normal"),
			new Entry("Solo Insane", "solo_insane"),
			new Entry("Teams Normal", "teams_normal"),
			new Entry("Teams Insane", "teams_insane"))
	};

	private QuickplayEntries() {
	}
}
