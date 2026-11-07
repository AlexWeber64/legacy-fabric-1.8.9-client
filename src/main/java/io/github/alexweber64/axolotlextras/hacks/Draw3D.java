package io.github.alexweber64.axolotlextras.hacks;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Collects lines and see-through boxes for one frame (positions relative to the camera, colours as 0xAARRGGBB), then draws
 * them all through walls in two batches: the filled boxes first, the lines on top.
 */
final class Draw3D {

	private static final int[][] EDGES = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5},
		{2, 6}, {3, 7}};
	private static final int[][] FACES = {{0, 1, 2, 3}, {7, 6, 5, 4}, {0, 4, 5, 1}, {1, 5, 6, 2}, {2, 6, 7, 3}, {3, 7, 4, 0}};

	/** {x, y, z, argb} per vertex. */
	private final List<double[]> lines = new ArrayList<>();
	private final List<double[]> quads = new ArrayList<>();

	boolean isEmpty() {
		return lines.isEmpty() && quads.isEmpty();
	}

	void line(double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
		line(x1, y1, z1, argb, x2, y2, z2, argb);
	}

	/** A line that fades from one colour to the other. */
	void line(double x1, double y1, double z1, int argb1, double x2, double y2, double z2, int argb2) {
		lines.add(new double[] {x1, y1, z1, argb1});
		lines.add(new double[] {x2, y2, z2, argb2});
	}

	void boxOutline(double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
		double[][] c = corners(x1, y1, z1, x2, y2, z2);
		for (int[] edge : EDGES) {
			line(c[edge[0]][0], c[edge[0]][1], c[edge[0]][2], c[edge[1]][0], c[edge[1]][1], c[edge[1]][2], argb);
		}
	}

	void filledBox(double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
		double[][] c = corners(x1, y1, z1, x2, y2, z2);
		for (int[] face : FACES) {
			for (int i : face) {
				quads.add(new double[] {c[i][0], c[i][1], c[i][2], argb});
			}
		}
	}

	/** A block at world coordinates, filled and outlined. */
	void block(int x, int y, int z, double camX, double camY, double camZ, int fill, int outline) {
		double x1 = x - camX - 0.002, y1 = y - camY - 0.002, z1 = z - camZ - 0.002;
		filledBox(x1, y1, z1, x1 + 1.004, y1 + 1.004, z1 + 1.004, fill);
		boxOutline(x1, y1, z1, x1 + 1.004, y1 + 1.004, z1 + 1.004, outline);
	}

	private static double[][] corners(double x1, double y1, double z1, double x2, double y2, double z2) {
		return new double[][] {
			{x1, y1, z1}, {x2, y1, z1}, {x2, y1, z2}, {x1, y1, z2},
			{x1, y2, z1}, {x2, y2, z1}, {x2, y2, z2}, {x1, y2, z2}};
	}

	/** Draws everything collected and empties the lists. */
	void flush() {
		if (isEmpty()) {
			return;
		}
		boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
		boolean fog = GL11.glIsEnabled(GL11.GL_FOG);
		boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
		GlStateManager.pushMatrix();
		GlStateManager.disableTexture();
		GlStateManager.disableLighting();
		GlStateManager.disableDepthTest();
		GlStateManager.depthMask(false);
		GlStateManager.disableFog();
		GlStateManager.disableCull();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		if (!quads.isEmpty()) {
			buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
			emit(buffer, quads);
			tessellator.draw();
		}
		if (!lines.isEmpty()) {
			GL11.glLineWidth(2f);
			buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
			emit(buffer, lines);
			tessellator.draw();
			GL11.glLineWidth(1f);
		}
		quads.clear();
		lines.clear();

		GlStateManager.enableTexture();
		GlStateManager.enableDepthTest();
		GlStateManager.depthMask(true);
		if (cull) {
			GlStateManager.enableCull();
		}
		if (!blend) {
			GlStateManager.disableBlend();
		}
		if (fog) {
			GlStateManager.enableFog();
		}
		GlStateManager.color(1f, 1f, 1f, 1f);
		GlStateManager.popMatrix();
	}

	private static void emit(BufferBuilder buffer, List<double[]> vertices) {
		for (double[] v : vertices) {
			int argb = (int) (long) v[3];
			buffer.vertex(v[0], v[1], v[2]).color(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24).next();
		}
	}
}
