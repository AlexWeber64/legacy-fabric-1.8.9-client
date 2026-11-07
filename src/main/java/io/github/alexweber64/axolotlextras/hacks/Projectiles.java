package io.github.alexweber64.axolotlextras.hacks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.alexweber64.axolotlextras.ExtrasModule;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.thrown.EnderPearlEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Fireball Warning, Pearl Landing and Trajectories: where flying fireballs and pearls will hit, and where your own pearl,
 * snowball, egg or arrow would land. Drawing and a local sound only, nothing is sent.
 */
final class Projectiles {

	private static final int STEPS = 300;
	/** A fireball that comes this close to you, or explodes this close, is a threat. */
	private static final double THREAT_PASS = 4, THREAT_IMPACT = 5;
	/** Blocks this close to a fireball's impact, with a blast resistance under {@link #SOFT_RESISTANCE}, are marked. */
	private static final double BLAST_RADIUS = 2.2;
	private static final float SOFT_RESISTANCE = 4f;

	private static final int FIREBALL = 0xFFFF4040, FIREBALL_FILL = 0x50FF2020, SOFT_FILL = 0x40FF9020, SOFT_LINE = 0xC0FF9020;
	private static final int PEARL = 0xFFB060FF, PEARL_FILL = 0x50A040FF;
	private static final int AIM = 0xFFFFFFFF, AIM_HIT_PLAYER = 0xFF40FF60, AIM_FILL = 0x40FFFFFF;

	/** One flying fireball or pearl, worked out once per tick. */
	private static final class Flight {
		final Trajectory.Path path;
		final boolean fireball;
		final List<int[]> soft = new ArrayList<>();

		Flight(Trajectory.Path path, boolean fireball) {
			this.path = path;
			this.fireball = fireball;
		}
	}

	private final List<Flight> flights = new ArrayList<>();
	private final List<HudLabel> labels = new ArrayList<>();
	private final Set<Integer> warned = new HashSet<>();

	void reset() {
		flights.clear();
		labels.clear();
		warned.clear();
	}

	List<HudLabel> labels() {
		return labels;
	}

	// ------------------------------------------------------------------------------------------------------------ tick

	void tick(MinecraftClient client, ClientWorld world) {
		flights.clear();
		labels.clear();
		boolean fireballs = ExtrasModule.FIREBALL_WARNING.get();
		boolean pearls = ExtrasModule.PEARL_LANDING.get();
		if (!fireballs && !pearls) {
			warned.clear();
			return;
		}
		PlayerEntity me = client.player;
		Trajectory.Collider blocks = blockCollider(world);
		Set<Integer> alive = new HashSet<>();
		for (Entity entity : world.getLoadedEntities()) {
			if (entity.removed) {
				continue;
			}
			if (fireballs && entity instanceof ExplosiveProjectileEntity) {
				alive.add(entity.getEntityId());
				fireball(client, world, (ExplosiveProjectileEntity) entity, blocks, me);
			} else if (pearls && entity instanceof EnderPearlEntity) {
				Trajectory.Path path = Trajectory.simulate(entity.x, entity.y, entity.z, entity.velocityX, entity.velocityY,
					entity.velocityZ, Trajectory.THROWN_GRAVITY, Trajectory.THROWN_DRAG, null, STEPS, blocks);
				flights.add(new Flight(path, false));
				double[] end = path.end();
				HudLabel label = new HudLabel(end[0], end[1] + 0.6, end[2]);
				label.text = "§5Pearl §7" + Math.round(distance(me, end)) + "m";
				labels.add(label);
			}
		}
		warned.retainAll(alive);
	}

	private void fireball(MinecraftClient client, ClientWorld world, ExplosiveProjectileEntity fireball,
		Trajectory.Collider blocks, PlayerEntity me) {
		double vx = fireball.velocityX, vy = fireball.velocityY, vz = fireball.velocityZ;
		double[] accel = {fireball.powerX, fireball.powerY, fireball.powerZ};
		if (vx * vx + vy * vy + vz * vz < 1e-6) {
			// No velocity known yet: go by how far it moved in the last tick, in a straight line.
			vx = fireball.x - fireball.prevX;
			vy = fireball.y - fireball.prevY;
			vz = fireball.z - fireball.prevZ;
			accel = null;
			if (vx * vx + vy * vy + vz * vz < 1e-6) {
				return;
			}
		}
		Trajectory.Path path = Trajectory.simulate(fireball.x, fireball.y, fireball.z, vx, vy, vz, 0,
			accel == null ? 1 : Trajectory.FIREBALL_DRAG, accel, STEPS, blocks);
		Flight flight = new Flight(path, true);
		if (path.hit != null && ExtrasModule.FIREBALL_BLOCKS.get()) {
			markSoftBlocks(world, path.hit, flight.soft);
		}
		flights.add(flight);

		double eyeY = me.y + me.getEyeHeight();
		double[] end = path.end();
		boolean threat = Trajectory.closestApproach(path, me.x, eyeY, me.z) < THREAT_PASS
			|| (path.hit != null && distance(me, end) < THREAT_IMPACT);
		HudLabel label = new HudLabel(fireball.x, fireball.y + 0.8, fireball.z);
		label.text = (threat ? "§c§lFIREBALL " : "§cFireball ") + "§7"
			+ Math.round(Math.sqrt(fireball.squaredDistanceTo(me))) + "m";
		labels.add(label);
		if (threat && warned.add(fireball.getEntityId()) && ExtrasModule.FIREBALL_SOUND.get()) {
			me.playSound("note.bass", 1f, 0.5f);
		}
	}

	/**
	 * The blocks around the impact that the blast is likely to break: soft ones (wool, wood, ladders) within about two
	 * blocks. An estimate: the server decides the real blast, and Hypixel makes glass and beds blast-proof.
	 */
	private static void markSoftBlocks(ClientWorld world, double[] hit, List<int[]> out) {
		int cx = (int) Math.floor(hit[0]), cy = (int) Math.floor(hit[1]), cz = (int) Math.floor(hit[2]);
		int r = (int) Math.ceil(BLAST_RADIUS);
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int x = cx - r; x <= cx + r; x++) {
			for (int y = cy - r; y <= cy + r; y++) {
				for (int z = cz - r; z <= cz + r; z++) {
					double dx = x + 0.5 - hit[0], dy = y + 0.5 - hit[1], dz = z + 0.5 - hit[2];
					if (dx * dx + dy * dy + dz * dz > BLAST_RADIUS * BLAST_RADIUS) {
						continue;
					}
					Block block = world.getBlockState(pos.setPosition(x, y, z)).getBlock();
					if (block == Blocks.AIR || block == Blocks.BED || block == Blocks.GLASS || block == Blocks.STAINED_GLASS
						|| block == Blocks.STAINED_GLASS_PANE || block.getMaterial().isFluid()) {
						continue;
					}
					if (block.getBlastResistance(null) < SOFT_RESISTANCE) {
						out.add(new int[] {x, y, z});
					}
				}
			}
		}
	}

	// ----------------------------------------------------------------------------------------------------- drawing

	void render(MinecraftClient client, float tickDelta, Draw3D draw, double camX, double camY, double camZ) {
		for (Flight flight : flights) {
			int color = flight.fireball ? FIREBALL : PEARL;
			drawPath(draw, flight.path, color, camX, camY, camZ);
			if (flight.path.hit != null) {
				double[] h = flight.path.hit;
				draw.block((int) h[3], (int) h[4], (int) h[5], camX, camY, camZ, flight.fireball ? FIREBALL_FILL : PEARL_FILL, color);
			}
			for (int[] b : flight.soft) {
				draw.block(b[0], b[1], b[2], camX, camY, camZ, SOFT_FILL, SOFT_LINE);
			}
		}
		if (ExtrasModule.TRAJECTORIES.get()) {
			aim(client, tickDelta, draw, camX, camY, camZ);
		}
	}

	/** Your own throw or shot, from your hand, as it would fly if you let go now. */
	private static void aim(MinecraftClient client, float t, Draw3D draw, double camX, double camY, double camZ) {
		PlayerEntity me = client.player;
		ItemStack held = me.getStackInHand();
		if (held == null || client.getCameraEntity() != me) {
			return;
		}
		Item item = held.getItem();
		double speed, gravity, drag;
		if ((item == Items.ENDER_PEARL || item == Items.SNOWBALL || item == Items.EGG) && ExtrasModule.TRAJECTORY_THROWN.get()) {
			speed = Trajectory.THROWN_SPEED;
			gravity = Trajectory.THROWN_GRAVITY;
			drag = Trajectory.THROWN_DRAG;
		} else if (item == Items.BOW && me.isUsingItem() && ExtrasModule.TRAJECTORY_BOW.get()) {
			speed = Trajectory.bowSpeed(me.getRemainingUseTime());
			gravity = Trajectory.ARROW_GRAVITY;
			drag = Trajectory.ARROW_DRAG;
			if (speed == 0) {
				return;
			}
		} else {
			return;
		}
		double yaw = me.prevYaw + (me.yaw - me.prevYaw) * t;
		double pitch = me.prevPitch + (me.pitch - me.prevPitch) * t;
		// Where the game spawns a thrown item or arrow: at the eyes, 0.1 lower and 0.16 to the side.
		double x = me.prevTickX + (me.x - me.prevTickX) * t - Math.cos(Math.toRadians(yaw)) * 0.16;
		double y = me.prevTickY + (me.y - me.prevTickY) * t + me.getEyeHeight() - 0.1;
		double z = me.prevTickZ + (me.z - me.prevTickZ) * t - Math.sin(Math.toRadians(yaw)) * 0.16;
		double[] dir = Trajectory.direction(yaw, pitch);

		ClientWorld world = client.world;
		Trajectory.Collider blocks = blockCollider(world);
		List<PlayerEntity> others = new ArrayList<>();
		for (PlayerEntity player : world.playerEntities) {
			if (player != me && !player.removed && player.isAlive() && !HackMath.isNpc(player.getUuid())) {
				others.add(player);
			}
		}
		PlayerEntity[] struck = new PlayerEntity[1];
		Trajectory.Collider collider = (x1, y1, z1, x2, y2, z2) -> {
			double[] block = blocks.hit(x1, y1, z1, x2, y2, z2);
			double ex = block == null ? x2 : block[0], ey = block == null ? y2 : block[1], ez = block == null ? z2 : block[2];
			Vec3d from = new Vec3d(x1, y1, z1);
			Vec3d to = new Vec3d(ex, ey, ez);
			double best = Double.MAX_VALUE;
			double[] hit = block;
			for (PlayerEntity player : others) {
				Box box = player.getBoundingBox().expand(0.3, 0.3, 0.3);
				BlockHitResult r = box.method_585(from, to);
				if (r != null) {
					double d = r.pos.squaredDistanceTo(from);
					if (d < best) {
						best = d;
						hit = new double[] {r.pos.x, r.pos.y, r.pos.z};
						struck[0] = player;
					}
				}
			}
			if (hit == block) {
				struck[0] = null;
			}
			return hit;
		};
		Trajectory.Path path = Trajectory.simulate(x, y, z, dir[0] * speed, dir[1] * speed, dir[2] * speed, gravity, drag,
			null, STEPS, collider);
		int color = struck[0] != null ? AIM_HIT_PLAYER : AIM;
		drawPath(draw, path, color, camX, camY, camZ);
		if (struck[0] != null) {
			PlayerEntity p = struck[0];
			double ox = p.prevTickX + (p.x - p.prevTickX) * t - p.x - camX;
			double oy = p.prevTickY + (p.y - p.prevTickY) * t - p.y - camY;
			double oz = p.prevTickZ + (p.z - p.prevTickZ) * t - p.z - camZ;
			Box box = p.getBoundingBox();
			draw.filledBox(box.minX + ox, box.minY + oy, box.minZ + oz, box.maxX + ox, box.maxY + oy, box.maxZ + oz, 0x4040FF60);
			draw.boxOutline(box.minX + ox, box.minY + oy, box.minZ + oz, box.maxX + ox, box.maxY + oy, box.maxZ + oz, color);
		} else if (path.hit != null) {
			double[] h = path.hit;
			draw.block((int) h[3], (int) h[4], (int) h[5], camX, camY, camZ, AIM_FILL, color);
		}
	}

	private static void drawPath(Draw3D draw, Trajectory.Path path, int color, double camX, double camY, double camZ) {
		List<double[]> points = path.points;
		for (int i = 1; i < points.size(); i++) {
			double[] a = points.get(i - 1), b = points.get(i);
			draw.line(a[0] - camX, a[1] - camY, a[2] - camZ, b[0] - camX, b[1] - camY, b[2] - camZ, color);
		}
	}

	/** Block hits as {x, y, z, blockX, blockY, blockZ}. Passes through water and plants, like the projectiles do. */
	private static Trajectory.Collider blockCollider(ClientWorld world) {
		return (x1, y1, z1, x2, y2, z2) -> {
			BlockHitResult r = world.rayTrace(new Vec3d(x1, y1, z1), new Vec3d(x2, y2, z2), false, true, false);
			if (r == null || r.type != BlockHitResult.Type.BLOCK) {
				return null;
			}
			BlockPos p = r.getBlockPos();
			return new double[] {r.pos.x, r.pos.y, r.pos.z, p.getX(), p.getY(), p.getZ()};
		};
	}

	private static double distance(PlayerEntity me, double[] point) {
		double dx = point[0] - me.x, dy = point[1] - me.y, dz = point[2] - me.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
}
