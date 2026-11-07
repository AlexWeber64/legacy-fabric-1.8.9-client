package io.github.alexweber64.axolotlextras.hacks;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.bedwars.ResourceMath;
import io.github.alexweber64.axolotlextras.ui.RoundedRect;
import io.github.axolotlclient.modules.hud.util.ItemUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Draws the hacks: player boxes and tracers in the world (through walls), and the generator and bed labels on the HUD.
 * The labels are placed by projecting their world position with the camera matrices saved during the world pass, so they
 * keep the same size at any distance and show through walls. Everything here is drawing only, nothing is sent.
 */
public final class HackRenderer {

	private static final int LABEL_BACKGROUND = 0xA0101318;
	private static final int ICON = 18;

	/** A label on the HUD: a row of icons with a number on each, at a point in the world. */
	private static final class Label {
		final double x, y, z;
		final List<ItemStack> icons = new ArrayList<>();
		final List<String> numbers = new ArrayList<>();
		String warning;

		Label(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
	private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
	private static final float[] modelView = new float[16];
	private static final float[] projection = new float[16];
	private static final int[] viewport = new int[4];
	private static boolean captured;
	private static double camX, camY, camZ;

	private static final ResourceClusters CLUSTERS = new ResourceClusters();
	private static final BedScanner SCANNER = new BedScanner();
	private static final Map<Integer, ItemStack> BLOCK_ICONS = new HashMap<>();
	private static final ItemStack[] RESOURCE_ICONS = new ItemStack[ResourceMath.COUNT];
	private static List<Label> generatorLabels = Collections.emptyList();
	private static List<Label> bedLabels = Collections.emptyList();
	private static int ticks;

	/** What the obsidian alert last said about each bed (by head position), and what it would say now. */
	private static final class AlertState {
		String shown = "none";
		String pending = "none";
		int stable;
	}

	/** How many bed checks in a row (half a second apart) a change has to hold before it is announced. */
	private static final int ALERT_STABLE_CHECKS = 4;

	private static final Map<Long, AlertState> ALERTS = new HashMap<>();
	private static int obsidianKey = -1;

	private HackRenderer() {
	}

	/** Forgets everything (a new world was loaded). */
	public static void reset() {
		SCANNER.reset();
		ALERTS.clear();
		generatorLabels = Collections.emptyList();
		bedLabels = Collections.emptyList();
		captured = false;
	}

	// ------------------------------------------------------------------------------------------------------------ tick

	public static void tick(MinecraftClient client) {
		ClientWorld world = client.world;
		if (world == null || client.player == null) {
			reset();
			return;
		}
		ticks++;
		generatorLabels = ExtrasModule.GENERATOR_COUNTS.get() ? findGenerators(world) : Collections.<Label>emptyList();
		if (ExtrasModule.BED_DEFENSE.get() || ExtrasModule.OBSIDIAN_ALERT.get()) {
			SCANNER.tick(world, client.player.x, client.player.z);
			if (ticks % 10 == 0) {
				List<Label> labels = findBeds(client, world);
				bedLabels = ExtrasModule.BED_DEFENSE.get() ? labels : Collections.<Label>emptyList();
			}
		} else if (!bedLabels.isEmpty() || !ALERTS.isEmpty()) {
			SCANNER.reset();
			ALERTS.clear();
			bedLabels = Collections.emptyList();
		}
	}

	private static List<Label> findGenerators(ClientWorld world) {
		if (RESOURCE_ICONS[0] == null) {
			RESOURCE_ICONS[ResourceMath.IRON] = new ItemStack(Items.IRON_INGOT);
			RESOURCE_ICONS[ResourceMath.GOLD] = new ItemStack(Items.GOLD_INGOT);
			RESOURCE_ICONS[ResourceMath.DIAMOND] = new ItemStack(Items.DIAMOND);
			RESOURCE_ICONS[ResourceMath.EMERALD] = new ItemStack(Items.EMERALD);
		}
		CLUSTERS.clear();
		for (Entity entity : world.getLoadedEntities()) {
			if (!(entity instanceof ItemEntity) || entity.removed) {
				continue;
			}
			ItemStack stack = ((ItemEntity) entity).getItemStack();
			if (stack != null) {
				CLUSTERS.add(entity.x, entity.y, entity.z, resourceIndex(stack.getItem()), stack.count);
			}
		}
		List<Label> labels = new ArrayList<>();
		for (ResourceClusters.Cluster cluster : CLUSTERS.clusters()) {
			Label label = new Label(cluster.centerX(), cluster.topY() + 0.9, cluster.centerZ());
			for (int r = 0; r < ResourceMath.COUNT; r++) {
				if (cluster.counts[r] > 0) {
					label.icons.add(RESOURCE_ICONS[r]);
					label.numbers.add(String.valueOf(cluster.counts[r]));
				}
			}
			labels.add(label);
		}
		return labels;
	}

	private static int resourceIndex(Item item) {
		if (item == Items.IRON_INGOT) {
			return ResourceMath.IRON;
		} else if (item == Items.GOLD_INGOT) {
			return ResourceMath.GOLD;
		} else if (item == Items.DIAMOND) {
			return ResourceMath.DIAMOND;
		} else if (item == Items.EMERALD) {
			return ResourceMath.EMERALD;
		}
		return -1;
	}

	private static List<Label> findBeds(MinecraftClient client, ClientWorld world) {
		if (obsidianKey < 0) {
			obsidianKey = Block.getIdByBlock(Blocks.OBSIDIAN) << 4;
		}
		List<Label> labels = new ArrayList<>();
		BlockPos.Mutable pos = new BlockPos.Mutable();
		BedDefense.Lookup lookup = (x, y, z) -> blockKey(world, pos.setPosition(x, y, z));
		for (BedScanner.Bed bed : SCANNER.beds()) {
			if (world.getBlockState(pos.setPosition(bed.headX, bed.y, bed.headZ)).getBlock() != Blocks.BED) {
				continue; // broken since the chunk was last read
			}
			BedDefense.Result result = BedDefense.analyse(bed.footX, bed.y, bed.footZ, bed.headX, bed.headZ, lookup);
			Label label = new Label((bed.footX + bed.headX) / 2.0 + 0.5, bed.y + 1.3, (bed.footZ + bed.headZ) / 2.0 + 0.5);
			for (BedDefense.Layer layer : result.layers) {
				ItemStack icon = iconFor(layer.key);
				if (icon != null) {
					label.icons.add(icon);
					label.numbers.add(String.valueOf(layer.count));
				}
			}
			BedDefense.Cover obsidian = BedDefense.cover(bed.footX, bed.y, bed.footZ, bed.headX, bed.headZ, lookup, obsidianKey);
			if (result.exposed) {
				label.warning = label.icons.isEmpty() ? "§cNo defense" : "§cOpen";
			} else if (obsidian != null) {
				label.warning = obsidian.complete() ? "§5Obby" : "§5Obby gaps";
			}
			labels.add(label);
			if (ExtrasModule.OBSIDIAN_ALERT.get()) {
				obsidianAlert(client, bed, obsidian);
			}
		}
		return labels;
	}

	/**
	 * Says in chat (only on this screen, nothing is sent) when a bed gets obsidian around it, and whether the obsidian is a
	 * full layer or has gaps. A change has to hold for two seconds first, so a layer that is still being placed does not
	 * flood the chat.
	 */
	private static void obsidianAlert(MinecraftClient client, BedScanner.Bed bed, BedDefense.Cover obsidian) {
		long key = ((long) bed.headX << 40) ^ ((long) bed.y << 20) ^ (bed.headZ & 0xFFFFFL);
		AlertState state = ALERTS.get(key);
		if (state == null) {
			state = new AlertState();
			ALERTS.put(key, state);
		}
		String now = obsidian == null ? "none" : obsidian.complete() ? "full" : "gaps";
		if (!now.equals(state.pending)) {
			state.pending = now;
			state.stable = 0;
		}
		if (++state.stable < ALERT_STABLE_CHECKS || now.equals(state.shown)) {
			return;
		}
		state.shown = now;
		if (obsidian == null || client.player == null) {
			return; // obsidian gone: nothing worth a message
		}
		double dx = bed.headX + 0.5 - client.player.x;
		double dz = bed.headZ + 0.5 - client.player.z;
		String where = "Bed at " + bed.headX + ", " + bed.headZ + " (" + Math.round(Math.sqrt(dx * dx + dz * dz)) + " blocks "
			+ HackMath.compass(dx, dz) + ")";
		String text;
		if (obsidian.complete()) {
			text = where + " has obsidian all around it (layer " + obsidian.layer + ", " + obsidian.spots + " blocks).";
		} else {
			int gaps = obsidian.spots - obsidian.covered;
			text = where + " has obsidian, but it is misplaced: " + gaps + " of the " + obsidian.spots + " spots in its layer are"
				+ " not obsidian" + (obsidian.openToAir > 0 ? " (" + obsidian.openToAir + " of them air)" : "")
				+ ", so there is a way around it.";
		}
		client.inGameHud.getChatHud().addMessage(new LiteralText("§5[Hacks] §f" + text));
		client.player.playSound("note.pling", 1f, 1.5f);
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

	private static ItemStack iconFor(int key) {
		if (BLOCK_ICONS.containsKey(key)) {
			return BLOCK_ICONS.get(key);
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
		BLOCK_ICONS.put(key, icon);
		return icon;
	}

	// ----------------------------------------------------------------------------------------------------- world pass

	/** Called at the end of the world pass, just before the hand is drawn. */
	public static void onWorldRender(float tickDelta) {
		MinecraftClient client = MinecraftClient.getInstance();
		Entity camera = client.getCameraEntity();
		if (camera == null || client.world == null) {
			return;
		}
		camX = camera.prevTickX + (camera.x - camera.prevTickX) * tickDelta;
		camY = camera.prevTickY + (camera.y - camera.prevTickY) * tickDelta;
		camZ = camera.prevTickZ + (camera.z - camera.prevTickZ) * tickDelta;
		readMatrix(GL11.GL_MODELVIEW_MATRIX, modelView);
		readMatrix(GL11.GL_PROJECTION_MATRIX, projection);
		VIEWPORT.clear();
		GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
		for (int i = 0; i < 4; i++) {
			viewport[i] = VIEWPORT.get(i);
		}
		captured = true;

		if (ExtrasModule.PLAYER_ESP.get() || ExtrasModule.ESP_TRACERS.get()) {
			drawPlayers(client, camera, tickDelta);
		}
	}

	private static void readMatrix(int which, float[] into) {
		MATRIX.clear();
		GL11.glGetFloat(which, MATRIX);
		for (int i = 0; i < 16; i++) {
			into[i] = MATRIX.get(i);
		}
	}

	private static List<PlayerEntity> targets(MinecraftClient client, Entity camera) {
		List<PlayerEntity> out = new ArrayList<>();
		for (PlayerEntity player : client.world.playerEntities) {
			if (player == client.player || player == camera || player.removed || !player.isAlive()) {
				continue;
			}
			// NPCs and Hypixel's hidden check bots are not in the tab list; real players are.
			if (client.getNetworkHandler() != null && client.getNetworkHandler().getPlayerListEntry(player.getUuid()) == null) {
				continue;
			}
			out.add(player);
		}
		return out;
	}

	private static void drawPlayers(MinecraftClient client, Entity camera, float tickDelta) {
		List<PlayerEntity> players = targets(client, camera);
		if (players.isEmpty()) {
			return;
		}
		boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
		boolean fog = GL11.glIsEnabled(GL11.GL_FOG);
		GlStateManager.pushMatrix();
		GlStateManager.disableTexture();
		GlStateManager.disableLighting();
		GlStateManager.disableDepthTest();
		GlStateManager.depthMask(false);
		GlStateManager.disableFog();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		GL11.glLineWidth(2f);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
		double[] eye = null;
		if (ExtrasModule.ESP_TRACERS.get()) {
			// The tracers start just in front of the eyes, so they come out of the middle of the screen.
			double yaw = Math.toRadians(camera.prevYaw + (camera.yaw - camera.prevYaw) * tickDelta);
			double pitch = Math.toRadians(camera.prevPitch + (camera.pitch - camera.prevPitch) * tickDelta);
			eye = new double[] {-Math.sin(yaw) * Math.cos(pitch), camera.getEyeHeight() - Math.sin(pitch),
				Math.cos(yaw) * Math.cos(pitch)};
		}
		for (PlayerEntity player : players) {
			int rgb = HackMath.teamColor(player.getName().asFormattedString());
			if (rgb < 0) {
				rgb = 0xFFFFFF;
			}
			int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
			double ox = player.prevTickX + (player.x - player.prevTickX) * tickDelta - camX;
			double oy = player.prevTickY + (player.y - player.prevTickY) * tickDelta - camY;
			double oz = player.prevTickZ + (player.z - player.prevTickZ) * tickDelta - camZ;
			if (ExtrasModule.PLAYER_ESP.get()) {
				Box box = player.getBoundingBox();
				box(buffer, box.minX - player.x + ox - 0.05, box.minY - player.y + oy, box.minZ - player.z + oz - 0.05,
					box.maxX - player.x + ox + 0.05, box.maxY - player.y + oy + 0.1, box.maxZ - player.z + oz + 0.05, r, g, b);
			}
			if (eye != null) {
				buffer.vertex(eye[0], eye[1], eye[2]).color(r, g, b, 200).next();
				buffer.vertex(ox, oy + player.height / 2, oz).color(r, g, b, 200).next();
			}
		}
		tessellator.draw();

		GL11.glLineWidth(1f);
		GlStateManager.enableTexture();
		GlStateManager.enableDepthTest();
		GlStateManager.depthMask(true);
		if (!blend) {
			GlStateManager.disableBlend();
		}
		if (fog) {
			GlStateManager.enableFog();
		}
		GlStateManager.color(1f, 1f, 1f, 1f);
		GlStateManager.popMatrix();
	}

	/** The 12 edges of a box, as line pairs. */
	private static void box(BufferBuilder buffer, double x1, double y1, double z1, double x2, double y2, double z2,
		int r, int g, int b) {
		double[][] corners = {
			{x1, y1, z1}, {x2, y1, z1}, {x2, y1, z2}, {x1, y1, z2},
			{x1, y2, z1}, {x2, y2, z1}, {x2, y2, z2}, {x1, y2, z2}};
		int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
		for (int[] edge : edges) {
			double[] a = corners[edge[0]];
			double[] c = corners[edge[1]];
			buffer.vertex(a[0], a[1], a[2]).color(r, g, b, 255).next();
			buffer.vertex(c[0], c[1], c[2]).color(r, g, b, 255).next();
		}
	}

	// ------------------------------------------------------------------------------------------------------- HUD pass

	/** Called when the HUD is drawn: the generator and bed labels at their places on screen. */
	public static void renderOverlay() {
		if (!captured || (generatorLabels.isEmpty() && bedLabels.isEmpty())) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) {
			return;
		}
		int scale = new Window(client).getScaleFactor();
		List<Label> labels = new ArrayList<>(generatorLabels.size() + bedLabels.size());
		labels.addAll(generatorLabels);
		labels.addAll(bedLabels);
		// The farthest first, so nearer labels end up on top.
		Collections.sort(labels, (a, b) -> Double.compare(distanceSq(b), distanceSq(a)));
		for (Label label : labels) {
			double[] screen = HackMath.project(label.x - camX, label.y - camY, label.z - camZ, modelView, projection, viewport);
			if (screen == null) {
				continue;
			}
			drawLabel(client, label, (int) (screen[0] / scale), (int) ((client.height - screen[1]) / scale));
		}
		GlStateManager.color(1f, 1f, 1f, 1f);
	}

	private static double distanceSq(Label label) {
		double dx = label.x - camX, dy = label.y - camY, dz = label.z - camZ;
		return dx * dx + dy * dy + dz * dz;
	}

	private static void drawLabel(MinecraftClient client, Label label, int centerX, int centerY) {
		int warningWidth = label.warning == null ? 0 : client.textRenderer.getStringWidth(label.warning) + 6;
		int width = label.icons.size() * ICON + warningWidth + 4;
		int height = ICON + 2;
		int left = centerX - width / 2;
		int top = centerY - height / 2;
		RoundedRect.fillShape(left, top, width, height, RoundedRect.SMALL, LABEL_BACKGROUND);
		int x = left + 2;
		if (label.warning != null) {
			client.textRenderer.drawWithShadow(label.warning, x + 2, top + 6, 0xFFFFFFFF);
			x += warningWidth;
		}
		for (int i = 0; i < label.icons.size(); i++) {
			ItemStack icon = label.icons.get(i);
			ItemUtil.renderGuiItemModel(icon, x + 1, top + 2);
			ItemUtil.renderGuiItemOverlay(client.textRenderer, icon, x + 1, top + 2, label.numbers.get(i), 0xFFFFFF, true);
			x += ICON;
		}
	}
}
