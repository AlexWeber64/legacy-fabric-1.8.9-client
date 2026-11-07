package io.github.alexweber64.axolotlextras.hacks;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.bedwars.ResourceMath;
import io.github.alexweber64.axolotlextras.ui.RoundedRect;
import io.github.axolotlclient.modules.hud.util.ItemUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Draws the hacks: player boxes, tracers and waypoint beams in the world (through walls), and the labels (generators,
 * beds, waypoints, enemy armor) on the HUD. Labels are placed by projecting their world position with the camera matrices
 * saved during the world pass, so they keep the same size at any distance and show through walls. Drawing only, nothing
 * is sent.
 */
public final class HackRenderer {

	private static final int LABEL_BACKGROUND = 0xA0101318;
	private static final int ICON = 18;
	private static final int UNKNOWN_TEAM_RGB = 0xAA00AA;

	private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
	private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
	private static final float[] modelView = new float[16];
	private static final float[] projection = new float[16];
	private static final int[] viewport = new int[4];
	private static boolean captured;
	private static double camX, camY, camZ;
	private static float lastTickDelta;

	private static final ResourceClusters CLUSTERS = new ResourceClusters();
	private static final BedScanner SCANNER = new BedScanner();
	private static final BedTracker BEDS = new BedTracker();
	private static final ItemStack[] RESOURCE_ICONS = new ItemStack[ResourceMath.COUNT];
	private static List<HudLabel> generatorLabels = Collections.emptyList();
	private static List<HudLabel> waypointLabels = Collections.emptyList();
	private static int ticks;

	private HackRenderer() {
	}

	/** Forgets everything (a new world was loaded). */
	public static void reset() {
		SCANNER.reset();
		BEDS.reset();
		generatorLabels = Collections.emptyList();
		waypointLabels = Collections.emptyList();
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
		generatorLabels = ExtrasModule.GENERATOR_COUNTS.get() ? findGenerators(world) : Collections.<HudLabel>emptyList();
		boolean beds = ExtrasModule.BED_DEFENSE.get() || ExtrasModule.OBSIDIAN_ALERT.get();
		if (beds) {
			SCANNER.tick(world, client.player.x, client.player.z);
			if (ticks % 10 == 0) {
				BEDS.update(client, world, SCANNER.beds(), ticks);
				waypointLabels = ExtrasModule.OBSIDIAN_ALERT.get() && ExtrasModule.OBSIDIAN_WAYPOINTS.get()
					? waypointLabels(client) : Collections.<HudLabel>emptyList();
			}
		} else if (!BEDS.labels().isEmpty() || !waypointLabels.isEmpty()) {
			SCANNER.reset();
			BEDS.reset();
			waypointLabels = Collections.emptyList();
		}
	}

	private static List<HudLabel> findGenerators(ClientWorld world) {
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
		List<HudLabel> labels = new ArrayList<>();
		for (ResourceClusters.Cluster cluster : CLUSTERS.clusters()) {
			HudLabel label = new HudLabel(cluster.centerX(), cluster.topY() + 0.9, cluster.centerZ());
			for (int r = 0; r < ResourceMath.COUNT; r++) {
				if (cluster.counts[r] > 0) {
					label.icon(RESOURCE_ICONS[r], String.valueOf(cluster.counts[r]));
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

	/** "Red obby 8/8 64m NE" over every bed with obsidian, above its Bed Defense label. */
	private static List<HudLabel> waypointLabels(MinecraftClient client) {
		List<HudLabel> labels = new ArrayList<>();
		for (BedTracker.Info info : BEDS.waypoints()) {
			HudLabel label = new HudLabel(info.centerX(), info.bed.y + 1.3, info.centerZ());
			BedDefense.Cover cover = info.obsidian;
			label.text = TeamGuess.colored(info.team) + " §5obby " + (cover.complete() ? "§a" : "§e")
				+ cover.covered + "/" + cover.spots + " §7" + BedTracker.distance(client, info);
			label.raise = ExtrasModule.BED_DEFENSE.get() ? 24 : 0;
			labels.add(label);
		}
		return labels;
	}

	// ----------------------------------------------------------------------------------------------------- world pass

	/** Called at the end of the world pass, just before the hand is drawn. */
	public static void onWorldRender(float tickDelta) {
		MinecraftClient client = MinecraftClient.getInstance();
		Entity camera = client.getCameraEntity();
		if (camera == null || client.world == null) {
			return;
		}
		lastTickDelta = tickDelta;
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

		boolean players = ExtrasModule.PLAYER_ESP.get() || ExtrasModule.ESP_TRACERS.get();
		List<BedTracker.Info> waypoints = ExtrasModule.OBSIDIAN_ALERT.get() && ExtrasModule.OBSIDIAN_WAYPOINTS.get()
			? BEDS.waypoints() : Collections.<BedTracker.Info>emptyList();
		if (players || !waypoints.isEmpty()) {
			drawLines(client, camera, tickDelta, players, waypoints);
		}
	}

	private static void readMatrix(int which, float[] into) {
		MATRIX.clear();
		GL11.glGetFloat(which, MATRIX);
		for (int i = 0; i < 16; i++) {
			into[i] = MATRIX.get(i);
		}
	}

	/** Other real players (NPCs and Hypixel's hidden check bots are not in the tab list, so they are left out). */
	private static List<PlayerEntity> targets(MinecraftClient client) {
		List<PlayerEntity> out = new ArrayList<>();
		Entity camera = client.getCameraEntity();
		for (PlayerEntity player : client.world.playerEntities) {
			if (player == client.player || player == camera || player.removed || !player.isAlive()) {
				continue;
			}
			if (client.getNetworkHandler() != null && client.getNetworkHandler().getPlayerListEntry(player.getUuid()) == null) {
				continue;
			}
			out.add(player);
		}
		return out;
	}

	private static void drawLines(MinecraftClient client, Entity camera, float tickDelta, boolean players,
		List<BedTracker.Info> waypoints) {
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
		if (players) {
			addPlayers(client, camera, tickDelta, buffer);
		}
		for (BedTracker.Info info : waypoints) {
			// A tall beam over the bed, in the team's colour, so its island can be found from afar.
			int rgb = info.team == TeamGuess.UNKNOWN ? UNKNOWN_TEAM_RGB : HackMath.teamColor("§" + TeamGuess.CODES[info.team]);
			double x = info.centerX() - camX, z = info.centerZ() - camZ, y = info.bed.y - camY;
			buffer.vertex(x, y, z).color(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, 230).next();
			buffer.vertex(x, y + 60, z).color(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, 60).next();
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

	private static void addPlayers(MinecraftClient client, Entity camera, float tickDelta, BufferBuilder buffer) {
		double[] eye = null;
		if (ExtrasModule.ESP_TRACERS.get()) {
			// The tracers start just in front of the eyes, so they come out of the middle of the screen.
			double yaw = Math.toRadians(camera.prevYaw + (camera.yaw - camera.prevYaw) * tickDelta);
			double pitch = Math.toRadians(camera.prevPitch + (camera.pitch - camera.prevPitch) * tickDelta);
			eye = new double[] {-Math.sin(yaw) * Math.cos(pitch), camera.getEyeHeight() - Math.sin(pitch),
				Math.cos(yaw) * Math.cos(pitch)};
		}
		for (PlayerEntity player : targets(client)) {
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

	/** Enemy armor over each enemy's head: their best armor piece with its Protection level, and what they hold. */
	private static List<HudLabel> armorLabels(MinecraftClient client) {
		List<HudLabel> labels = new ArrayList<>();
		int ownTeam = TeamGuess.fromFormattedName(client.player.getName().asFormattedString());
		float t = lastTickDelta;
		for (PlayerEntity player : targets(client)) {
			int team = TeamGuess.fromFormattedName(player.getName().asFormattedString());
			if (team != TeamGuess.UNKNOWN && team == ownTeam) {
				continue;
			}
			HudLabel label = new HudLabel(player.prevTickX + (player.x - player.prevTickX) * t,
				player.prevTickY + (player.y - player.prevTickY) * t + player.height + 0.5,
				player.prevTickZ + (player.z - player.prevTickZ) * t);
			label.raise = 14; // above the name tag
			ItemStack armor = bestArmor(player);
			if (armor != null) {
				int protection = protection(player);
				label.icon(armor, protection > 0 ? "P" + protection : "");
			}
			ItemStack held = player.getStackInHand();
			if (held != null) {
				int sharpness = EnchantmentHelper.getLevel(Enchantment.SHARPNESS.id, held);
				int knockback = EnchantmentHelper.getLevel(Enchantment.KNOCKBACK.id, held);
				label.icon(held, sharpness > 0 ? "S" + sharpness : knockback > 0 ? "K" + knockback : "");
			}
			if (!label.icons.isEmpty()) {
				labels.add(label);
			}
		}
		return labels;
	}

	/** Leggings first (Bed Wars armor upgrades replace the leggings and boots), then boots, chestplate, helmet. */
	private static ItemStack bestArmor(PlayerEntity player) {
		int[] order = {1, 0, 2, 3};
		for (int slot : order) {
			ItemStack stack = player.getArmorSlot(slot);
			if (stack != null) {
				return stack;
			}
		}
		return null;
	}

	/** The highest Protection level on any armor piece (the Bed Wars team upgrade puts it on every piece). */
	private static int protection(PlayerEntity player) {
		int best = 0;
		for (int slot = 0; slot < 4; slot++) {
			ItemStack stack = player.getArmorSlot(slot);
			if (stack != null) {
				best = Math.max(best, EnchantmentHelper.getLevel(Enchantment.PROTECTION.id, stack));
			}
		}
		return best;
	}

	/** Called when the HUD is drawn: every label at its place on screen. */
	public static void renderOverlay() {
		if (!captured) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null || client.player == null) {
			return;
		}
		List<HudLabel> labels = new ArrayList<>();
		labels.addAll(generatorLabels);
		labels.addAll(BEDS.labels());
		labels.addAll(waypointLabels);
		if (ExtrasModule.ENEMY_ARMOR.get()) {
			labels.addAll(armorLabels(client));
		}
		if (labels.isEmpty()) {
			return;
		}
		int scale = new Window(client).getScaleFactor();
		// The farthest first, so nearer labels end up on top.
		Collections.sort(labels, (a, b) -> Double.compare(distanceSq(b), distanceSq(a)));
		for (HudLabel label : labels) {
			double[] screen = HackMath.project(label.x - camX, label.y - camY, label.z - camZ, modelView, projection, viewport);
			if (screen != null) {
				drawLabel(client, label, (int) (screen[0] / scale), (int) ((client.height - screen[1]) / scale) - label.raise);
			}
		}
		GlStateManager.color(1f, 1f, 1f, 1f);
	}

	private static double distanceSq(HudLabel label) {
		double dx = label.x - camX, dy = label.y - camY, dz = label.z - camZ;
		return dx * dx + dy * dy + dz * dz;
	}

	private static void drawLabel(MinecraftClient client, HudLabel label, int centerX, int centerY) {
		int textWidth = label.text == null ? 0 : client.textRenderer.getStringWidth(label.text) + (label.icons.isEmpty() ? 4 : 6);
		int width = label.icons.size() * ICON + textWidth + 4;
		int height = label.icons.isEmpty() ? 12 : ICON + 2;
		int left = centerX - width / 2;
		int top = centerY - height / 2;
		RoundedRect.fillShape(left, top, width, height, RoundedRect.SMALL, LABEL_BACKGROUND);
		int x = left + 2;
		if (label.text != null) {
			client.textRenderer.drawWithShadow(label.text, x + 2, top + (height - 8) / 2, 0xFFFFFFFF);
			x += textWidth;
		}
		for (int i = 0; i < label.icons.size(); i++) {
			ItemStack icon = label.icons.get(i);
			ItemUtil.renderGuiItemModel(icon, x + 1, top + 2);
			String number = label.numbers.get(i);
			if (!number.isEmpty()) {
				ItemUtil.renderGuiItemOverlay(client.textRenderer, icon, x + 1, top + 2, number, 0xFFFFFF, true);
			}
			x += ICON;
		}
	}
}
