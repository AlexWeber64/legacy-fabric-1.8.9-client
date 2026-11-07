package io.github.alexweber64.axolotlextras.hacks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;

/**
 * Everything known about the beds around: their team, their defense (for the Bed Defense labels), their obsidian (for the
 * Obsidian Alert and its waypoints). Updated twice a second from the beds {@link BedScanner} found.
 */
final class BedTracker {

	/** How many checks in a row (half a second apart) an obsidian change has to hold before it is announced. */
	private static final int ALERT_STABLE_CHECKS = 4;

	/** Area searched for team-coloured blocks around a bed. */
	private static final int TEAM_REACH = 10, TEAM_BELOW = 5, TEAM_ABOVE = 6;

	/** Players this close to a bed count for its team. */
	private static final double PLAYER_REACH = 8;

	/** How long to wait before trying again when the team could not be told, in ticks. */
	private static final int TEAM_RETRY = 100;

	/** One bed, kept while it stands (also while its chunk is out of range, so its waypoint stays). */
	static final class Info {
		final BedScanner.Bed bed;
		int team = TeamGuess.UNKNOWN;
		int nextTeamTry;
		BedDefense.Cover obsidian;
		String shown = "none";
		String pending = "none";
		int stable;

		Info(BedScanner.Bed bed) {
			this.bed = bed;
		}

		double centerX() {
			return (bed.footX + bed.headX) / 2.0 + 0.5;
		}

		double centerZ() {
			return (bed.footZ + bed.headZ) / 2.0 + 0.5;
		}

		/** True while the obsidian alert shows a waypoint for this bed. */
		boolean waypoint() {
			return !"none".equals(shown) && obsidian != null;
		}
	}

	private final Map<Long, Info> beds = new HashMap<>();
	private final Map<Integer, ItemStack> blockIcons = new HashMap<>();
	private List<HudLabel> labels = Collections.emptyList();
	private int obsidianKey = -1;
	private Info own;
	/** What {@link #own} was found from: the team then, and whether the start spot was known. */
	private int ownFoundTeam = TeamGuess.UNKNOWN;
	private boolean ownFoundFromSpawn;

	/** How far from where you stood at the game start your own bed may be. */
	private static final double SPAWN_REACH = 40;

	/** For {@link #nearest}: any team. */
	private static final int ANY_TEAM = -2;

	void reset() {
		own = null;
		beds.clear();
		labels = Collections.emptyList();
	}

	List<HudLabel> labels() {
		return labels;
	}

	/** The beds that have a waypoint now. */
	List<Info> waypoints() {
		List<Info> out = new ArrayList<>();
		for (Info info : beds.values()) {
			if (info.waypoint()) {
				out.add(info);
			}
		}
		return out;
	}

	/**
	 * Your own bed. With the spot where you stood just after the game started, it is the bed nearest that spot (you start
	 * next to your own bed, so this works whatever the bed's colour guess says). Without it (you joined late), it is the
	 * bed of your team nearest to you. Once found, the bed takes your team, which also fixes a wrong colour guess.
	 *
	 * @param ownTeam your team (from the sidebar), or {@link TeamGuess#UNKNOWN}
	 * @param spawn   {x, z} where you stood at the game start, or null when the start was not seen
	 * @return the bed, or null when it cannot be told yet
	 */
	Info ownBed(int ownTeam, double[] spawn, double playerX, double playerZ) {
		boolean stale = own == null || !beds.containsValue(own) || ownFoundTeam != ownTeam
			|| (spawn != null && !ownFoundFromSpawn);
		if (!stale) {
			return own;
		}
		own = spawn != null ? nearest(spawn[0], spawn[1], ANY_TEAM, SPAWN_REACH) : null;
		if (own == null && spawn == null) {
			own = nearest(playerX, playerZ, ownTeam, Double.MAX_VALUE);
		}
		ownFoundTeam = ownTeam;
		ownFoundFromSpawn = spawn != null;
		if (own != null && ownTeam != TeamGuess.UNKNOWN) {
			own.team = ownTeam;
		}
		return own;
	}

	/** @param team only beds of this team, or any bed when it is {@link #ANY_TEAM} */
	private Info nearest(double x, double z, int team, double reach) {
		if (team == TeamGuess.UNKNOWN) {
			return null;
		}
		Info best = null;
		double bestDistance = reach * reach;
		for (Info info : beds.values()) {
			double dx = info.centerX() - x, dz = info.centerZ() - z, d = dx * dx + dz * dz;
			if ((team == ANY_TEAM || info.team == team) && d <= bestDistance) {
				best = info;
				bestDistance = d;
			}
		}
		return best;
	}

	/**
	 * @param ownTeam     your team, or {@link TeamGuess#UNKNOWN}
	 * @param gameRunning true once names carry team colours (in the waiting lobby they carry rank colours instead)
	 */
	void update(MinecraftClient client, ClientWorld world, List<BedScanner.Bed> found, int tick, int ownTeam,
		boolean gameRunning) {
		if (obsidianKey < 0) {
			obsidianKey = Block.getIdByBlock(Blocks.OBSIDIAN) << 4;
		}
		BlockPos.Mutable pos = new BlockPos.Mutable();
		BedDefense.Lookup lookup = (x, y, z) -> blockKey(world, pos.setPosition(x, y, z));
		Map<Long, BedScanner.Bed> current = new HashMap<>();
		for (BedScanner.Bed bed : found) {
			current.put(key(bed), bed);
		}
		// Forget beds that are gone: their chunk is loaded and the bed is not there. Beds in unloaded chunks are kept.
		for (Iterator<Map.Entry<Long, Info>> it = beds.entrySet().iterator(); it.hasNext(); ) {
			Info info = it.next().getValue();
			BedScanner.Bed bed = info.bed;
			if (!world.getChunk(bed.headX >> 4, bed.headZ >> 4).isEmpty()
				&& world.getBlockState(pos.setPosition(bed.headX, bed.y, bed.headZ)).getBlock() != Blocks.BED) {
				it.remove();
			}
		}
		List<HudLabel> next = new ArrayList<>();
		for (Map.Entry<Long, BedScanner.Bed> entry : current.entrySet()) {
			BedScanner.Bed bed = entry.getValue();
			if (world.getBlockState(pos.setPosition(bed.headX, bed.y, bed.headZ)).getBlock() != Blocks.BED) {
				continue; // broken since its chunk was last read
			}
			Info info = beds.get(entry.getKey());
			if (info == null) {
				info = new Info(bed);
				beds.put(entry.getKey(), info);
			}
			if (info.team == TeamGuess.UNKNOWN && tick >= info.nextTeamTry) {
				info.team = guessTeam(client, world, info, pos, gameRunning);
				info.nextTeamTry = tick + TEAM_RETRY;
			}
			info.obsidian = BedDefense.cover(bed.footX, bed.y, bed.footZ, bed.headX, bed.headZ, lookup, obsidianKey);
			boolean mine = info == own || (info.team != TeamGuess.UNKNOWN && info.team == ownTeam);
			if (ExtrasModule.BED_DEFENSE.get() && (!mine || ExtrasModule.BED_DEFENSE_OWN.get())) {
				next.add(defenseLabel(info, BedDefense.analyse(bed.footX, bed.y, bed.footZ, bed.headX, bed.headZ, lookup)));
			}
			if (ExtrasModule.OBSIDIAN_ALERT.get() && !mine) {
				obsidianAlert(client, info);
			}
		}
		labels = next;
	}

	private HudLabel defenseLabel(Info info, BedDefense.Result result) {
		HudLabel label = new HudLabel(info.centerX(), info.bed.y + 1.3, info.centerZ());
		for (BedDefense.Layer layer : result.layers) {
			ItemStack icon = iconFor(layer.key);
			if (icon != null) {
				label.icon(icon, String.valueOf(layer.count));
			}
		}
		String team = info.team == TeamGuess.UNKNOWN ? "" : "§" + TeamGuess.CODES[info.team] + "§l"
			+ TeamGuess.NAMES[info.team].charAt(0) + " ";
		if (result.exposed) {
			label.text = team + (label.icons.isEmpty() ? "§cNo defense" : "§cOpen");
		} else {
			label.text = team.isEmpty() ? null : team.trim();
		}
		return label;
	}

	/** A short chat line when a bed's obsidian changes (only shown here, nothing is sent). */
	private static void obsidianAlert(MinecraftClient client, Info info) {
		BedDefense.Cover obsidian = info.obsidian;
		String now = obsidian == null ? "none" : obsidian.complete() ? "full" : "gaps";
		if (!now.equals(info.pending)) {
			info.pending = now;
			info.stable = 0;
		}
		if (++info.stable < ALERT_STABLE_CHECKS || now.equals(info.shown)) {
			return;
		}
		info.shown = now;
		if (obsidian == null) {
			return; // the obsidian is gone; its waypoint goes with it
		}
		String status = obsidian.complete() ? "§a" + obsidian.covered + "/" + obsidian.spots
			: "§e" + obsidian.covered + "/" + obsidian.spots + " gaps";
		client.inGameHud.getChatHud().addMessage(new LiteralText("§5Obby §f" + bedName(info) + " " + status
			+ " §7" + distance(client, info)));
		if (ExtrasModule.OBSIDIAN_SOUND.get()) {
			client.player.playSound("note.pling", 1f, 1.5f);
		}
	}

	/** "Red bed", or the coordinates when the team is not known. */
	static String bedName(Info info) {
		return info.team == TeamGuess.UNKNOWN ? "bed at " + info.bed.headX + ", " + info.bed.headZ
			: TeamGuess.colored(info.team) + " §fbed";
	}

	/** "64m NE" from the player. */
	static String distance(MinecraftClient client, Info info) {
		double dx = info.centerX() - client.player.x;
		double dz = info.centerZ() - client.player.z;
		return Math.round(Math.sqrt(dx * dx + dz * dz)) + "m " + HackMath.compass(dx, dz);
	}

	/**
	 * Team-coloured blocks around the bed first; players next to it when that does not tell. Players only count once the
	 * game runs: in the waiting lobby their names are in rank colours (a VIP is green), not team colours.
	 */
	private static int guessTeam(MinecraftClient client, ClientWorld world, Info info, BlockPos.Mutable pos,
		boolean gameRunning) {
		int[] blockVotes = new int[TeamGuess.COUNT];
		BedScanner.Bed bed = info.bed;
		for (int x = bed.headX - TEAM_REACH; x <= bed.headX + TEAM_REACH; x++) {
			for (int z = bed.headZ - TEAM_REACH; z <= bed.headZ + TEAM_REACH; z++) {
				for (int y = bed.y - TEAM_BELOW; y <= bed.y + TEAM_ABOVE; y++) {
					BlockState state = world.getBlockState(pos.setPosition(x, y, z));
					Block block = state.getBlock();
					if (block == Blocks.WOOL || block == Blocks.STAINED_TERRACOTTA || block == Blocks.STAINED_GLASS
						|| block == Blocks.CARPET || block == Blocks.STAINED_GLASS_PANE) {
						int team = TeamGuess.fromDye(block.getData(state));
						if (team != TeamGuess.UNKNOWN) {
							blockVotes[team]++;
						}
					}
				}
			}
		}
		int[] playerVotes = new int[TeamGuess.COUNT];
		for (PlayerEntity player : gameRunning ? world.playerEntities : Collections.<PlayerEntity>emptyList()) {
			double dx = player.x - info.centerX(), dz = player.z - info.centerZ(), dy = player.y - bed.y;
			if (dx * dx + dz * dz > PLAYER_REACH * PLAYER_REACH || Math.abs(dy) > 6) {
				continue;
			}
			if (HackMath.isNpc(player.getUuid()) || (player != client.player && client.getNetworkHandler() != null
				&& client.getNetworkHandler().getPlayerListEntry(player.getUuid()) == null)) {
				continue; // an NPC
			}
			int team = TeamGuess.fromFormattedName(player.getName().asFormattedString());
			if (team != TeamGuess.UNKNOWN) {
				playerVotes[team]++;
			}
		}
		return TeamGuess.guess(blockVotes, playerVotes);
	}

	private static long key(BedScanner.Bed bed) {
		return ((long) bed.headX << 40) ^ ((long) bed.y << 20) ^ (bed.headZ & 0xFFFFFL);
	}

	/** A number per block type for {@link BedDefense}: 0 for air, beds and plants, otherwise block id and item damage. */
	private static int blockKey(ClientWorld world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if (block == Blocks.WATER || block == Blocks.FLOWING_WATER) {
			return Block.getIdByBlock(Blocks.WATER) << 4;
		}
		if (block == Blocks.LAVA || block == Blocks.FLOWING_LAVA) {
			return Block.getIdByBlock(Blocks.LAVA) << 4;
		}
		if (block == Blocks.AIR || block == Blocks.BED || block.getMaterial().isReplaceable()) {
			return 0;
		}
		return Block.getIdByBlock(block) << 4 | (block.getMeta(state) & 15);
	}

	private ItemStack iconFor(int key) {
		if (blockIcons.containsKey(key)) {
			return blockIcons.get(key);
		}
		Block block = Block.getById(key >> 4);
		ItemStack icon = null;
		if (block == Blocks.WATER) {
			icon = new ItemStack(Items.WATER_BUCKET);
		} else if (block == Blocks.LAVA) {
			icon = new ItemStack(Items.LAVA_BUCKET);
		} else {
			Item item = Item.fromBlock(block);
			if (item != null) {
				icon = new ItemStack(item, 1, key & 15);
			}
		}
		blockIcons.put(key, icon);
		return icon;
	}
}
