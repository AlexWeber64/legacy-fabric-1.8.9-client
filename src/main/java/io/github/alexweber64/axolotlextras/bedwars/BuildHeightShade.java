package io.github.alexweber64.axolotlextras.bedwars;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.axolotlclient.AxolotlClient;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

/**
 * Build Height Indicator: wool on the topmost layer you can build at is drawn darker, and so is red and blue stained clay
 * (The Bridge's blocks). The layer comes from {@link BuildHeight}. The darkening is done while the game builds the chunk
 * meshes (see BuildHeightMixin), so the world is rebuilt once when the layer becomes known or the option changes.
 */
public final class BuildHeightShade {

	/** How bright the darkened blocks are (1 is normal). */
	private static final float BRIGHTNESS = 0.6f;
	/** Stained clay colours (block metadata) that get darkened: red and blue. */
	private static final int CLAY_RED = 14, CLAY_BLUE = 11;
	private static final int SIDEBAR_CHECK_TICKS = 20;

	private static BuildHeight height = new BuildHeight(Collections.<String, Integer>emptyMap());
	/** The layer being darkened, read by the chunk builder threads. */
	private static volatile int layer = BuildHeight.UNKNOWN;
	private static int ticks;

	private BuildHeightShade() {
	}

	/** Loads the bundled map table. */
	public static void init() {
		try (InputStream in = BuildHeightShade.class.getResourceAsStream("/assets/axolotlextras/build_heights.txt")) {
			Map<String, Integer> limits = BuildHeight.readTable(in);
			height = new BuildHeight(limits);
			AxolotlClient.LOGGER.info("[Extras] Build Height: " + limits.size() + " Bed Wars maps in the table");
		} catch (Exception e) {
			AxolotlClient.LOGGER.warn("[Extras] Build Height: could not read the map table: " + e);
		}
		boolean patched = false;
		for (java.lang.reflect.Method method : net.minecraft.client.render.block.BlockModelRenderer.class.getDeclaredMethods()) {
			// Mixin renames its handler methods (handler$xyz$name), so the name only has to be contained
			patched |= method.getName().contains("axolotlextras$shadeTopLayer");
		}
		AxolotlClient.LOGGER.info("[Extras] Build Height patch " + (patched ? "applied" : "NOT applied (nothing is darkened)"));
	}

	/** A new world was loaded: the old layer no longer applies (its chunks are rebuilt anyway). */
	public static void reset() {
		height.reset();
		layer = BuildHeight.UNKNOWN;
	}

	public static void onChatLine(String line) {
		height.onChatLine(line);
	}

	public static void tick(MinecraftClient client) {
		if (client.world == null || client.worldRenderer == null) {
			return;
		}
		if (++ticks % SIDEBAR_CHECK_TICKS == 0) {
			ScoreboardObjective sidebar = client.world.getScoreboard().getObjectiveForSlot(1);
			if (sidebar != null) {
				height.onSidebar(sidebarLines(client.world.getScoreboard(), sidebar));
			}
		}
		int now = ExtrasModule.BUILD_HEIGHT.get() ? height.topLayer() : BuildHeight.UNKNOWN;
		if (now != layer) {
			layer = now;
			if (now != BuildHeight.UNKNOWN) {
				AxolotlClient.LOGGER.info("[Extras] Build Height: darkening layer Y=" + now + " ("
					+ (height.bridge() ? "The Bridge" : "map " + height.map()) + ")");
			}
			client.worldRenderer.reload();
		}
	}

	/**
	 * Called for each block face as it goes into the chunk mesh, after its vertices are written: darkens the face when the
	 * block is on the top layer and is wool or red or blue stained clay.
	 */
	public static void shade(BlockView view, Block block, BlockPos pos, BufferBuilder buffer) {
		int top = layer;
		if (top == BuildHeight.UNKNOWN || pos.getY() != top) {
			return;
		}
		if (block == Blocks.WOOL || block == Blocks.STAINED_TERRACOTTA && isRedOrBlue(block.getData(view.getBlockState(pos)))) {
			// The face's four vertices, counted back from the last one written.
			for (int vertex = 4; vertex >= 1; vertex--) {
				buffer.faceTint(BRIGHTNESS, BRIGHTNESS, BRIGHTNESS, vertex);
			}
		}
	}

	private static boolean isRedOrBlue(int meta) {
		return meta == CLAY_RED || meta == CLAY_BLUE;
	}

	private static List<String> sidebarLines(Scoreboard scoreboard, ScoreboardObjective sidebar) {
		List<String> lines = new ArrayList<>();
		for (ScoreboardPlayerScore score : scoreboard.getAllPlayerScores(sidebar)) {
			String name = score.getPlayerName();
			if (name != null && !name.startsWith("#")) {
				lines.add(Team.decorateName(scoreboard.getPlayerTeam(name), name));
			}
		}
		return lines;
	}
}
