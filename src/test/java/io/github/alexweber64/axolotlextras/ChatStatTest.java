package io.github.alexweber64.axolotlextras;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.alexweber64.axolotlextras.bedwars.ChatStat;
import io.github.alexweber64.axolotlextras.bedwars.ChatStat.Counts;
import org.junit.jupiter.api.Test;

class ChatStatTest {

	private static final Counts C = new Counts(300, 120, 90, 45, 60, 40, 80, 215);

	@Test
	void eachStatUsesItsOwnNumbers() {
		assertEquals("2.50", ChatStat.KDR.format(C));
		assertEquals("2.00", ChatStat.FKDR.format(C));
		assertEquals("1.50", ChatStat.WLR.format(C));
		assertEquals("2.00", ChatStat.BBLR.format(C));
		assertEquals("215", ChatStat.STARS.format(C));
	}

	@Test
	void keysMapBackToStatsAndUnknownGivesKdr() {
		assertEquals(ChatStat.FKDR, ChatStat.byKey("fkdr"));
		assertEquals(ChatStat.STARS, ChatStat.byKey("STARS"));
		assertEquals(ChatStat.KDR, ChatStat.byKey("nonsense"));
		assertEquals("kdr", ChatStat.keys()[0]);
	}
}
