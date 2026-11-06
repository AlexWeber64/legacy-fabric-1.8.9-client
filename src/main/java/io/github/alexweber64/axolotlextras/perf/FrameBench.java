package io.github.alexweber64.axolotlextras.perf;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Locale;

import io.github.axolotlclient.AxolotlClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.particle.ParticleType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * A small frame time recorder for measuring changes, used from a singleplayer world only. A chat line of the form
 * "AXBENCH label seconds" starts it (the test scripts send that with /tellraw), and when the time is up it writes the
 * summary to the log and to config/axolotl-extras-bench/results.txt, and every frame time to a csv file. Nothing is
 * sent anywhere.
 *
 * "AXBENCH label seconds particles" also plays a movement scene while recording: the player sprints and jumps in circles
 * (turning every tick, turning the other way every four seconds) and the given number of particles is spawned around the
 * player every tick, which is closer to a Bed Wars fight than standing still.
 */
public final class FrameBench {

	private static boolean running;
	private static String label = "";
	private static long lastNs;
	private static long endNs;
	private static double[] ms = new double[1 << 16];
	private static int count;
	private static long gcCountStart;
	private static long gcMillisStart;
	private static boolean scene;
	private static int particlesPerTick;
	private static int sceneTicks;
	private static int particlePeak;
	private static final java.util.Random RANDOM = new java.util.Random(42);
	private static final java.util.List<Entity> SCENE_ENTITIES = new java.util.ArrayList<>();
	private static final ParticleType[] PARTICLE_TYPES = {
		ParticleType.SMOKE_LARGE, ParticleType.EXPLOSION, ParticleType.FIRE, ParticleType.CRIT};

	private FrameBench() {
	}

	public static void start(String name, int seconds) {
		start(name, seconds, -1);
	}

	/** @param particles particles per tick for the movement scene, or a negative number for no scene */
	public static void start(String name, int seconds, int particles) {
		scene = particles >= 0;
		if (scene && SCENE_ENTITIES.isEmpty()) {
			spawnScene(MinecraftClient.getInstance());
		}
		particlesPerTick = particles;
		sceneTicks = 0;
		particlePeak = 0;
		label = name.replaceAll("[^A-Za-z0-9_.-]", "_");
		count = 0;
		lastNs = 0;
		endNs = System.nanoTime() + seconds * 1_000_000_000L;
		gcCountStart = gcCount();
		gcMillisStart = gcMillis();
		running = true;
		AxolotlClient.LOGGER.info("[Extras] BENCH started: " + label + " for " + seconds + " s");
	}

	public static boolean isRunning() {
		return running;
	}

	/**
	 * Fills the area around the player with client side entities like a Bed Wars island has: 32 villagers with visible
	 * name tags, 16 invisible armor stands with visible names (the hologram lines over generators) and 16 item drops. They
	 * exist on this client only, so they cost rendering time but no server time, and appear instantly.
	 */
	private static void spawnScene(MinecraftClient client) {
		if (client.world == null || client.player == null) {
			return;
		}
		double px = client.player.x;
		double py = client.player.y;
		double pz = client.player.z;
		for (int i = 0; i < 32; i++) {
			VillagerEntity villager = new VillagerEntity(client.world);
			villager.setCustomName("Shop");
			villager.setCustomNameVisible(true);
			addToScene(client, villager, px - 7 + (i % 8) * 2, py, pz + 3 + (i / 8) * 2);
		}
		for (int i = 0; i < 16; i++) {
			ArmorStandEntity stand = new ArmorStandEntity(client.world);
			stand.setInvisible(true);
			stand.setCustomName("Diamond Generator");
			stand.setCustomNameVisible(true);
			addToScene(client, stand, px - 6 + (i % 4) * 4, py, pz - 9 + (i / 4) * 3);
		}
		for (int i = 0; i < 16; i++) {
			addToScene(client, new ItemEntity(client.world, 0, 0, 0, new ItemStack(Items.IRON_INGOT)),
				px - 8 + (i % 4) * 5, py + 0.5, pz - 6 + (i / 4) * 4);
		}
		AxolotlClient.LOGGER.info("[Extras] BENCH scene: " + SCENE_ENTITIES.size() + " entities (32 villagers, 16 named armor stands, 16 items)");
	}

	private static void addToScene(MinecraftClient client, Entity entity, double x, double y, double z) {
		entity.refreshPositionAndAngles(x, y, z, RANDOM.nextInt(360), 0);
		if (client.world.spawnEntity(entity)) {
			SCENE_ENTITIES.add(entity);
		}
	}

	/** Removes the entities {@link #spawnScene} added. */
	public static void clearScene() {
		MinecraftClient client = MinecraftClient.getInstance();
		for (Entity entity : SCENE_ENTITIES) {
			if (client.world != null) {
				client.world.removeEntity(entity);
			}
		}
		SCENE_ENTITIES.clear();
	}

	/** Called once per client tick: plays the movement scene while a recording with a scene is running. */
	public static void onTick(MinecraftClient client) {
		if (!running || !scene || client.player == null || client.world == null || client.currentScreen != null) {
			return;
		}
		ClientPlayerEntity player = client.player;
		sceneTicks++;
		KeyBinding.setKeyPressed(client.options.forwardKey.getCode(), true);
		KeyBinding.setKeyPressed(client.options.sprintKey.getCode(), true);
		KeyBinding.setKeyPressed(client.options.jumpKey.getCode(), true);
		float direction = (sceneTicks / 80) % 2 == 0 ? 1f : -1f;
		player.prevYaw = player.yaw;
		player.yaw += 3.5f * direction;
		player.prevPitch = player.pitch;
		player.pitch = (float) (10 * Math.sin(sceneTicks * 0.05));
		for (int i = 0; i < particlesPerTick; i++) {
			double x = player.x + (RANDOM.nextDouble() - 0.5) * 8;
			double y = player.y + RANDOM.nextDouble() * 3;
			double z = player.z + (RANDOM.nextDouble() - 0.5) * 8;
			client.world.addParticle(PARTICLE_TYPES[i % PARTICLE_TYPES.length], x, y, z,
				(RANDOM.nextDouble() - 0.5) * 0.05, RANDOM.nextDouble() * 0.05, (RANDOM.nextDouble() - 0.5) * 0.05);
		}
		if (sceneTicks % 5 == 0) {
			try {
				particlePeak = Math.max(particlePeak, Integer.parseInt(client.particleManager.getDebugString().trim()));
			} catch (NumberFormatException ignored) {
				// the count is only reported when it can be read
			}
		}
	}

	private static void releaseScene() {
		if (!scene) {
			return;
		}
		scene = false;
		MinecraftClient client = MinecraftClient.getInstance();
		KeyBinding.setKeyPressed(client.options.forwardKey.getCode(), false);
		KeyBinding.setKeyPressed(client.options.sprintKey.getCode(), false);
		KeyBinding.setKeyPressed(client.options.jumpKey.getCode(), false);
	}

	/** Called once per frame, at the start of the game renderer. */
	public static void onFrame() {
		if (!running) {
			return;
		}
		long now = System.nanoTime();
		if (lastNs != 0) {
			if (count == ms.length) {
				ms = Arrays.copyOf(ms, ms.length * 2);
			}
			ms[count++] = (now - lastNs) / 1_000_000.0;
		}
		lastNs = now;
		if (now >= endNs) {
			running = false;
			finish();
		}
	}

	private static void finish() {
		boolean hadScene = scene;
		releaseScene();
		BenchStats stats = BenchStats.compute(ms, count);
		String line = String.format(Locale.ROOT, "%s | %s | gc runs=%d gc ms=%d%s", label, stats,
			gcCount() - gcCountStart, gcMillis() - gcMillisStart, hadScene ? " particlesPeak=" + particlePeak : "");
		AxolotlClient.LOGGER.info("[Extras] BENCH " + line);
		try {
			Path dir = FabricLoader.getInstance().getConfigDir().resolve("axolotl-extras-bench");
			Files.createDirectories(dir);
			Files.write(dir.resolve("results.txt"), (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			StringBuilder csv = new StringBuilder("ms\n");
			for (int i = 0; i < count; i++) {
				csv.append(String.format(Locale.ROOT, "%.3f%n", ms[i]));
			}
			Files.write(dir.resolve(label + ".csv"), csv.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			AxolotlClient.LOGGER.error("[Extras] BENCH could not write results: " + e);
		}
	}

	private static long gcCount() {
		long total = 0;
		for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
			total += Math.max(0, bean.getCollectionCount());
		}
		return total;
	}

	private static long gcMillis() {
		long total = 0;
		for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
			total += Math.max(0, bean.getCollectionTime());
		}
		return total;
	}
}
