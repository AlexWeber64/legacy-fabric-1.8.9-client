package io.github.alexweber64.axolotlextras.hacks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.axolotlclient.AxolotlClientConfig.options.IntegerOption;
import io.github.axolotlclient.modules.hud.util.ItemUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

/**
 * The radar: a minimap on the HUD with every player around you drawn as the face of their skin (the same 8x8 face and hat
 * layer the tab list uses), framed in their team colour. Drawing only, nothing is sent.
 */
final class Radar {

	/** One player for the radar, with whether they are on your team. */
	static final class Target {
		final PlayerEntity player;
		final boolean teammate;

		Target(PlayerEntity player, boolean teammate) {
			this.player = player;
			this.teammate = teammate;
		}
	}

	/** Your bed as the radar's middle, with whether the radar is live there. */
	static final class Bed {
		final double x, y, z;
		final RadarMath.BedState state;

		Bed(double x, double y, double z, RadarMath.BedState state) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.state = state;
		}
	}

	private static final int BACKGROUND_RGB = 0x101318;
	private static final int RIM = 0x90FFFFFF;
	private static final int GRID = 0x30FFFFFF;
	private static final int NORTH = 0xFFFFD84D;
	/** The same parts while a bed-centred radar is not live: gray, so it is plain that it shows nothing. */
	private static final int GRAY_BACKGROUND_RGB = 0x3C3C3C;
	private static final int GRAY_RIM = 0x90808080;
	private static final int GRAY_GRID = 0x30A0A0A0;
	private static final int GRAY_TEXT = 0xFFA0A0A0;
	private static ItemStack bedIcon;
	private static final int ABOVE = 0xFF55FF55;
	private static final int BELOW = 0xFFFF5555;
	private static final int SEGMENTS = 64;
	/** The face size at 100 percent, in GUI pixels. */
	private static final int BASE_ICON = 10;
	/** How many blocks above or below you get a height arrow. */
	private static final double HEIGHT_MARK = 3;

	private Radar() {
	}

	/**
	 * @param left the radar's left edge on screen (from its place in the HUD editor)
	 * @param top  the radar's top edge on screen
	 * @param bed  null: the radar is centred on you; else centred on your bed, and gray (no other players) unless the bed
	 *     is within your render distance
	 */
	static void render(MinecraftClient client, int left, int top, List<Target> targets, float t, Bed bed) {
		PlayerEntity me = client.player;
		int size = value(ExtrasModule.RADAR_SIZE, 100);
		double half = size / 2.0;
		double cx = left + half, cy = top + half;

		boolean circle = !ExtrasModule.RADAR_SHAPE.get().endsWith(".square");
		boolean rotate = ExtrasModule.RADAR_ROTATE.get();
		double range = HackMath.optionNumber(ExtrasModule.RADAR_RANGE.get(), 64);
		float icon = BASE_ICON * value(ExtrasModule.RADAR_ICON_SIZE, 100) / 100f;
		double usable = Math.max(1, half - icon / 2 - 1);
		double yaw = me.prevYaw + (me.yaw - me.prevYaw) * t;
		double myX = lerp(me.prevTickX, me.x, t), myY = lerp(me.prevTickY, me.y, t), myZ = lerp(me.prevTickZ, me.z, t);
		// The middle of the radar: you, or your bed.
		boolean onBed = bed != null && bed.state != RadarMath.BedState.NO_BED;
		boolean live = bed == null || bed.state == RadarMath.BedState.ACTIVE;
		double originX = onBed ? bed.x : myX, originZ = onBed ? bed.z : myZ;

		boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
		boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
		// The fan and triangles are wound either way, so nothing may be culled.
		GlStateManager.disableCull();
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);

		// Background, grid and rim.
		GlStateManager.disableTexture();
		int alpha = Math.round(255 * value(ExtrasModule.RADAR_OPACITY, 50) / 100f);
		if (alpha > 0) {
			shape(GL11.GL_TRIANGLE_FAN, cx, cy, half, circle, alpha << 24 | (live ? BACKGROUND_RGB : GRAY_BACKGROUND_RGB));
		}
		int grid = live ? GRID : GRAY_GRID;
		if (ExtrasModule.RADAR_GRID.get()) {
			line(cx - half, cy, cx + half, cy, grid);
			line(cx, cy - half, cx, cy + half, grid);
			shape(GL11.GL_LINE_LOOP, cx, cy, half / 2, circle, grid);
		}
		shape(GL11.GL_LINE_LOOP, cx, cy, half, circle, live ? RIM : GRAY_RIM);
		GlStateManager.enableTexture();

		if (ExtrasModule.RADAR_GRID.get()) {
			double[] n = RadarMath.turn(0, -1, yaw, rotate);
			double[] spot = RadarMath.place(n[0], n[1], 1, half - 5, circle, true);
			client.textRenderer.drawWithShadow("N", (float) (cx + spot[0] - 2.5), (float) (cy + spot[1] - 3.5),
				live ? NORTH : GRAY_TEXT);
		}

		if (!live) {
			// Not live: no other players (they are not sent to you that far away), only the reason and where you are.
			String reason = onBed
				? "Bed out of range (" + Math.round(Math.hypot(myX - originX, myZ - originZ)) + "m)" : "No bed";
			small(client, reason, cx, cy + (onBed ? icon / 2 + 2 : -2), GRAY_TEXT);
			if (onBed) {
				GlStateManager.disableTexture();
				rect(cx - 3, cy - 2, cx + 3, cy + 2, 0xFF808080);
				GlStateManager.enableTexture();
				self(client, me, cx, cy, myX - originX, myZ - originZ, yaw, rotate, range, usable, circle, icon, true);
			}
			finish(blend, cull);
			return;
		}

		// Other players, the farthest first so nearer ones end up on top.
		List<double[]> spots = new ArrayList<>();
		List<Target> shown = new ArrayList<>();
		boolean teammates = ExtrasModule.RADAR_TEAMMATES.get(), invisible = ExtrasModule.RADAR_INVISIBLE.get();
		boolean keepOut = ExtrasModule.RADAR_EDGE.get();
		for (Target target : targets) {
			PlayerEntity p = target.player;
			if ((!teammates && target.teammate) || (!invisible && p.isInvisible())) {
				continue;
			}
			double dx = lerp(p.prevTickX, p.x, t) - originX, dz = lerp(p.prevTickZ, p.z, t) - originZ;
			double[] turned = RadarMath.turn(dx, dz, yaw, rotate);
			double[] spot = RadarMath.place(turned[0], turned[1], range, usable, circle, keepOut);
			if (spot != null) {
				spots.add(new double[] {spot[0], spot[1], spot[2], dx * dx + dz * dz, lerp(p.prevTickY, p.y, t) - myY});
				shown.add(target);
			}
		}
		List<Integer> order = new ArrayList<>();
		for (int i = 0; i < spots.size(); i++) {
			order.add(i);
		}
		Collections.sort(order, (a, b) -> Double.compare(spots.get(b)[3], spots.get(a)[3]));
		for (int i : order) {
			double[] spot = spots.get(i);
			PlayerEntity p = shown.get(i).player;
			boolean faded = spot[2] > 0 || p.isInvisible();
			face(client, p, cx + spot[0], cy + spot[1], icon, faded ? 0.5f : 1f);
			if (ExtrasModule.RADAR_HEIGHT.get()) {
				int mark = RadarMath.heightMark(spot[4], HEIGHT_MARK);
				if (mark != 0) {
					heightArrow(cx + spot[0] + icon / 2 + 1, cy + spot[1] - icon / 2, mark > 0);
				}
			}
			if (ExtrasModule.RADAR_NAMES.get()) {
				name(client, p, cx + spot[0], cy + spot[1] + icon / 2 + 1.5);
			}
		}

		if (onBed) {
			// Your bed in the middle, you where you are around it.
			if (bedIcon == null) {
				bedIcon = new ItemStack(Items.BED);
			}
			GlStateManager.pushMatrix();
			GlStateManager.translate(cx - icon / 2, cy - icon / 2, 0);
			GlStateManager.scale(icon / 16f, icon / 16f, 1f);
			ItemUtil.renderGuiItemModel(bedIcon, 0, 0);
			GlStateManager.popMatrix();
			GlStateManager.disableLighting();
			GlStateManager.enableBlend();
			GlStateManager.blendFuncSeparate(770, 771, 1, 0);
			self(client, me, cx, cy, myX - originX, myZ - originZ, yaw, rotate, range, usable, circle, icon, false);
		} else {
			self(client, me, cx, cy, 0, 0, yaw, rotate, range, usable, circle, icon, false);
		}
		finish(blend, cull);
	}

	/**
	 * You on the radar: your face (or an arrow) at your offset from the middle, kept on the rim when out of range.
	 *
	 * @param faded true while the radar is not live
	 */
	private static void self(MinecraftClient client, PlayerEntity me, double cx, double cy, double dx, double dz, double yaw,
		boolean rotate, double range, double usable, boolean circle, float icon, boolean faded) {
		double[] turned = RadarMath.turn(dx, dz, yaw, rotate);
		double[] spot = RadarMath.place(turned[0], turned[1], range, usable, circle, true);
		double x = cx + spot[0], y = cy + spot[1];
		float alpha = faded || spot[2] > 0 ? 0.6f : 1f;
		// Your view on the radar: always up when it turns with you.
		double r = Math.toRadians(yaw);
		double[] look = RadarMath.turn(-Math.sin(r), Math.cos(r), yaw, rotate);
		int arrow = Math.round(alpha * 255) << 24 | 0xFFFFFF;
		if (ExtrasModule.RADAR_SELF.get()) {
			face(client, me, x, y, icon, alpha);
			if (!rotate) {
				// North up: a small mark in front of the face shows where you look.
				direction(x, y, look[0], look[1], icon / 2 + 2, 3, arrow);
			}
		} else {
			direction(x, y, look[0], look[1], -2, 4, arrow);
		}
	}

	/** Text at half size, centred on x. */
	private static void small(MinecraftClient client, String text, double x, double y, int argb) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, 0);
		GlStateManager.scale(0.5f, 0.5f, 1f);
		client.textRenderer.drawWithShadow(text, -client.textRenderer.getStringWidth(text) / 2f, 0, argb);
		GlStateManager.popMatrix();
	}

	private static void finish(boolean blend, boolean cull) {
		if (!blend) {
			GlStateManager.disableBlend();
		}
		if (cull) {
			GlStateManager.enableCull();
		}
		GlStateManager.color(1f, 1f, 1f, 1f);
	}

	private static double lerp(double from, double to, float t) {
		return from + (to - from) * t;
	}

	private static int value(IntegerOption option, int fallback) {
		Integer v = option.get();
		return v == null ? fallback : v;
	}

	/** A face from the player's skin (face and hat layer), with a frame in the team colour. */
	private static void face(MinecraftClient client, PlayerEntity player, double x, double y, float icon, float alpha) {
		int a = Math.round(alpha * 255) << 24;
		GlStateManager.pushMatrix();
		GlStateManager.translate(x - icon / 2, y - icon / 2, 0);
		if (ExtrasModule.RADAR_BORDER.get()) {
			int rgb = HackMath.teamColor(player.getName().asFormattedString());
			GlStateManager.disableTexture();
			rect(-1, -1, icon + 1, icon + 1, a | (rgb < 0 ? 0xFFFFFF : rgb));
			GlStateManager.enableTexture();
		}
		// The skin texture. Careful with the names: in these mappings getCapeId() is the one that returns the skin (with the
		// default skin as fallback), and getSkinId() returns the cape, null without one. Binding null crashes the game.
		Identifier skin = player instanceof AbstractClientPlayerEntity ? ((AbstractClientPlayerEntity) player).getCapeId() : null;
		if (skin != null) {
			client.getTextureManager().bindTexture(skin);
			GlStateManager.color(1f, 1f, 1f, alpha);
			GlStateManager.scale(icon / 8f, icon / 8f, 1f);
			DrawableHelper.drawTexture(0, 0, 8f, 8f, 8, 8, 8, 8, 64f, 64f);
			if (player.isPartVisible(PlayerModelPart.HAT)) {
				DrawableHelper.drawTexture(0, 0, 40f, 8f, 8, 8, 8, 8, 64f, 64f);
			}
			GlStateManager.color(1f, 1f, 1f, 1f);
		} else {
			GlStateManager.disableTexture();
			rect(0, 0, icon, icon, a | 0x808080);
			GlStateManager.enableTexture();
		}
		GlStateManager.popMatrix();
	}

	/** The player's name (in their colour) under the face, at half size. */
	private static void name(MinecraftClient client, PlayerEntity player, double x, double y) {
		small(client, player.getName().asFormattedString(), x, y, 0xFFFFFFFF);
	}

	/** A small triangle pointing up (above you) or down (below you). */
	private static void heightArrow(double x, double y, boolean up) {
		GlStateManager.disableTexture();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
		int argb = up ? ABOVE : BELOW;
		if (up) {
			vertex(buffer, x + 1.5, y, argb);
			vertex(buffer, x, y + 3, argb);
			vertex(buffer, x + 3, y + 3, argb);
		} else {
			vertex(buffer, x, y, argb);
			vertex(buffer, x + 1.5, y + 3, argb);
			vertex(buffer, x + 3, y, argb);
		}
		tessellator.draw();
		GlStateManager.enableTexture();
	}

	/** A triangle at {@code distance} from the middle, pointing along {fx, fy} (a unit vector on the radar). */
	private static void direction(double cx, double cy, double fx, double fy, double distance, double width, int argb) {
		double px = -fy, py = fx;
		double tipX = cx + fx * (distance + width), tipY = cy + fy * (distance + width);
		double baseX = cx + fx * distance, baseY = cy + fy * distance;
		GlStateManager.disableTexture();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
		vertex(buffer, tipX, tipY, argb);
		vertex(buffer, baseX + px * width, baseY + py * width, argb);
		vertex(buffer, baseX - px * width, baseY - py * width, argb);
		tessellator.draw();
		GlStateManager.enableTexture();
	}

	/** A circle or square around the middle, filled (triangle fan) or as an outline (line loop). */
	private static void shape(int mode, double cx, double cy, double r, boolean circle, int argb) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(mode, VertexFormats.POSITION_COLOR);
		if (mode == GL11.GL_TRIANGLE_FAN) {
			vertex(buffer, cx, cy, argb);
		}
		if (circle) {
			// The fan repeats the first rim point to close; the loop closes by itself.
			int points = mode == GL11.GL_TRIANGLE_FAN ? SEGMENTS + 1 : SEGMENTS;
			for (int i = 0; i < points; i++) {
				double a = 2 * Math.PI * i / SEGMENTS;
				vertex(buffer, cx + Math.cos(a) * r, cy + Math.sin(a) * r, argb);
			}
		} else {
			vertex(buffer, cx - r, cy - r, argb);
			vertex(buffer, cx - r, cy + r, argb);
			vertex(buffer, cx + r, cy + r, argb);
			vertex(buffer, cx + r, cy - r, argb);
			if (mode == GL11.GL_TRIANGLE_FAN) {
				vertex(buffer, cx - r, cy - r, argb);
			}
		}
		tessellator.draw();
	}

	private static void line(double x1, double y1, double x2, double y2, int argb) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
		vertex(buffer, x1, y1, argb);
		vertex(buffer, x2, y2, argb);
		tessellator.draw();
	}

	private static void rect(double x1, double y1, double x2, double y2, int argb) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
		vertex(buffer, x1, y1, argb);
		vertex(buffer, x1, y2, argb);
		vertex(buffer, x2, y2, argb);
		vertex(buffer, x2, y1, argb);
		tessellator.draw();
	}

	private static void vertex(BufferBuilder buffer, double x, double y, int argb) {
		buffer.vertex(x, y, 0).color(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24).next();
	}
}
