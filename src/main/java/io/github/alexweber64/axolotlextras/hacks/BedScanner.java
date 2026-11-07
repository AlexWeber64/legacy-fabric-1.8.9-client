package io.github.alexweber64.axolotlextras.hacks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

/**
 * Finds beds in the loaded chunks around the player. A few chunks are read per tick straight from their block arrays (a
 * Bed Wars map is mostly empty sections, which are skipped), so a full pass over the area takes about two seconds and
 * costs next to nothing per frame.
 */
final class BedScanner {

	/** How many chunks out from the player are searched. */
	private static final int RADIUS = 10;

	/** Chunks read per tick. */
	private static final int PER_TICK = 12;

	/** One bed: its foot and head halves. */
	static final class Bed {
		final int footX, y, footZ, headX, headZ;

		Bed(int footX, int y, int footZ, int headX, int headZ) {
			this.footX = footX;
			this.y = y;
			this.footZ = footZ;
			this.headX = headX;
			this.headZ = headZ;
		}
	}

	private final Map<Long, List<Bed>> bedsByChunk = new HashMap<>();
	private final List<long[]> queue = new ArrayList<>();
	private int cursor;
	private int bedId = -1;

	void reset() {
		bedsByChunk.clear();
		queue.clear();
		cursor = 0;
	}

	List<Bed> beds() {
		List<Bed> out = new ArrayList<>();
		for (List<Bed> list : bedsByChunk.values()) {
			out.addAll(list);
		}
		return out;
	}

	void tick(ClientWorld world, double playerX, double playerZ) {
		if (bedId < 0) {
			bedId = Block.getIdByBlock(Blocks.BED);
		}
		if (cursor >= queue.size()) {
			// Start a new pass around where the player is now. Chunks that left the area are forgotten.
			queue.clear();
			cursor = 0;
			int pcx = (int) Math.floor(playerX) >> 4;
			int pcz = (int) Math.floor(playerZ) >> 4;
			for (int dx = -RADIUS; dx <= RADIUS; dx++) {
				for (int dz = -RADIUS; dz <= RADIUS; dz++) {
					queue.add(new long[] {pcx + dx, pcz + dz});
				}
			}
			bedsByChunk.keySet().removeIf(key -> {
				int cx = (int) (key >> 32);
				int cz = (int) (long) key;
				return Math.abs(cx - pcx) > RADIUS || Math.abs(cz - pcz) > RADIUS;
			});
		}
		for (int i = 0; i < PER_TICK && cursor < queue.size(); i++, cursor++) {
			long[] pos = queue.get(cursor);
			scan(world, (int) pos[0], (int) pos[1]);
		}
	}

	private void scan(ClientWorld world, int cx, int cz) {
		long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
		Chunk chunk = world.getChunk(cx, cz);
		if (chunk == null || chunk.isEmpty()) {
			bedsByChunk.remove(key);
			return;
		}
		List<Bed> found = null;
		for (ChunkSection section : chunk.getBlockStorage()) {
			if (section == null || section.isEmpty()) {
				continue;
			}
			char[] states = section.getBlockStates();
			for (int i = 0; i < states.length; i++) {
				char state = states[i];
				// The array holds (block id << 4 | metadata). For a bed, the value 8 marks the head half and bits 0-1 the facing.
				if ((state >> 4) != bedId || (state & 8) == 0) {
					continue;
				}
				int headX = (cx << 4) + (i & 15);
				int headZ = (cz << 4) + ((i >> 4) & 15);
				int y = section.getYOffset() + ((i >> 8) & 15);
				// Facing 0 south (+z), 1 west (-x), 2 north (-z), 3 east (+x); the head lies one step that way from the foot.
				int facing = state & 3;
				int stepX = facing == 1 ? -1 : facing == 3 ? 1 : 0;
				int stepZ = facing == 0 ? 1 : facing == 2 ? -1 : 0;
				if (found == null) {
					found = new ArrayList<>();
				}
				found.add(new Bed(headX - stepX, y, headZ - stepZ, headX, headZ));
			}
		}
		if (found == null) {
			bedsByChunk.remove(key);
		} else {
			bedsByChunk.put(key, found);
		}
	}
}
