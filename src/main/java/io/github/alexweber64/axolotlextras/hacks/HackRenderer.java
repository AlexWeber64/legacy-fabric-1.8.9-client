package io.github.alexweber64.axolotlextras.hacks;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.alexweber64.axolotlextras.ExtrasModule;
import io.github.alexweber64.axolotlextras.bedwars.ResourceMath;
import io.github.alexweber64.axolotlextras.ui.RoundedRect;
import io.github.axolotlclient.AxolotlClientConfig.options.IntegerOption;
import io.github.axolotlclient.modules.hud.util.ItemUtil;
import net.minecraft.client.MinecraftClient;
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
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.Team;
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
	private static final Projectiles PROJECTILES = new Projectiles();
	private static final RushAlert RUSH = new RushAlert();
	private static final Draw3D DRAW = new Draw3D();
	private static final FreezeWatch WATCH = new FreezeWatch();
	/** {x, z} where you stood shortly after the game started (for finding your own bed), or null. */
	private static double[] spawn;
	private static int spawnCountdown = -1;
	/** The tick the start spot was taken, or -1. */
	private static int spawnTick = -1;
	/** Your team (from the sidebar, or your name colour once the game runs), or {@link TeamGuess#UNKNOWN}. */
	private static int ownTeam = TeamGuess.UNKNOWN;
	/** True once names carry team colours: the sidebar shows your team, or the game start was seen. */
	private static boolean gameRunning;
	/** No rush alerts this long after the start spot is taken: everyone still stands on their own island. */
	private static final int RUSH_QUIET_TICKS = 200;
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
		PROJECTILES.reset();
		RUSH.reset();
		WATCH.reset();
		spawn = null;
		spawnCountdown = -1;
		spawnTick = -1;
		ownTeam = TeamGuess.UNKNOWN;
		gameRunning = false;
		generatorLabels = Collections.emptyList();
		waypointLabels = Collections.emptyList();
		captured = false;
	}

	/** A Bed Wars game started: two seconds from now you stand on your own island, which tells which bed is yours. */
	public static void onGameStart() {
		spawn = null;
		spawnTick = -1;
		spawnCountdown = 40;
	}

	// ------------------------------------------------------------------------------------------------------------ tick

	public static void tick(MinecraftClient client) {
		WATCH.tick();
		ClientWorld world = client.world;
		if (world == null || client.player == null) {
			reset();
			return;
		}
		ticks++;
		if (spawnCountdown >= 0 && spawnCountdown-- == 0) {
			spawn = new double[] {client.player.x, client.player.z};
			spawnTick = ticks;
		}
		long t = WATCH.start();
		if (ticks % 10 == 0) {
			findOwnTeam(client, world);
			WATCH.end("own team (sidebar)", t);
		}
		t = WATCH.start();
		PROJECTILES.tick(client, world);
		WATCH.end("fireball and pearl paths", t);
		t = WATCH.start();
		generatorLabels = ExtrasModule.GENERATOR_COUNTS.get() ? findGenerators(world) : Collections.<HudLabel>emptyList();
		WATCH.end("generator counts", t);
		boolean beds = ExtrasModule.BED_DEFENSE.get() || ExtrasModule.OBSIDIAN_ALERT.get() || ExtrasModule.RUSH_ALERT.get()
			|| radarOnBed();
		if (beds) {
			t = WATCH.start();
			SCANNER.tick(world, client.player.x, client.player.z);
			WATCH.end("bed scan", t);
			if (ticks % 10 == 0) {
				t = WATCH.start();
				BEDS.ownBed(ownTeam, spawn, client.player.x, client.player.z);
				BEDS.update(client, world, SCANNER.beds(), ticks, ownTeam, gameRunning);
				waypointLabels = ExtrasModule.OBSIDIAN_ALERT.get() && ExtrasModule.OBSIDIAN_WAYPOINTS.get()
					? waypointLabels(client) : Collections.<HudLabel>emptyList();
				WATCH.end("bed defense, teams and obsidian", t);
			}
			if (ExtrasModule.RUSH_ALERT.get() && gameRunning) {
				// Every enemy counts here, also invisible and far ones (the ESP filters do not apply).
				List<PlayerEntity> enemies = new ArrayList<>();
				for (PlayerEntity player : realPlayers(client)) {
					if (!isTeammate(client, player)) {
						enemies.add(player);
					}
				}
				boolean quiet = spawnTick >= 0 && ticks - spawnTick < RUSH_QUIET_TICKS;
				t = WATCH.start();
				RUSH.tick(client, BEDS.ownBed(ownTeam, spawn, client.player.x, client.player.z), enemies, rushDistance(),
					ticks, quiet);
				WATCH.end("rush alert", t);
			}
		} else if (!BEDS.labels().isEmpty() || !waypointLabels.isEmpty()) {
			SCANNER.reset();
			BEDS.reset();
			waypointLabels = Collections.emptyList();
		}
	}

	/**
	 * Your team. The Bed Wars sidebar says it on every map ("R Red: YOU"). Without it, your name colour, but only after
	 * the game start: in the waiting lobby names are in rank colours (VIP is green), which used to make you "Green".
	 */
	private static void findOwnTeam(MinecraftClient client, ClientWorld world) {
		int team = sidebarTeam(world);
		if (team == TeamGuess.UNKNOWN && spawn != null) {
			team = TeamGuess.fromFormattedName(client.player.getName().asFormattedString());
		}
		ownTeam = team;
		gameRunning = team != TeamGuess.UNKNOWN || spawn != null;
	}

	/** The team on the sidebar line that has "YOU" on it, or {@link TeamGuess#UNKNOWN}. */
	private static int sidebarTeam(ClientWorld world) {
		Scoreboard scoreboard = world.getScoreboard();
		ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(1);
		if (sidebar == null) {
			return TeamGuess.UNKNOWN;
		}
		for (ScoreboardPlayerScore score : scoreboard.getAllPlayerScores(sidebar)) {
			String name = score.getPlayerName();
			int team = TeamGuess.fromSidebarLine(Team.decorateName(scoreboard.getPlayerTeam(name), name));
			if (team != TeamGuess.UNKNOWN) {
				return team;
			}
		}
		return TeamGuess.UNKNOWN;
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
			// Not getItemStack(): it writes "Item entity has no item?!" to the log while the item has not arrived yet.
			ItemStack stack = entity.getDataTracker().getStack(10);
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

	/** The resource, or -1 when it is not one or its switch is off. */
	private static int resourceIndex(Item item) {
		if (item == Items.IRON_INGOT && ExtrasModule.GEN_IRON.get()) {
			return ResourceMath.IRON;
		} else if (item == Items.GOLD_INGOT && ExtrasModule.GEN_GOLD.get()) {
			return ResourceMath.GOLD;
		} else if (item == Items.DIAMOND && ExtrasModule.GEN_DIAMOND.get()) {
			return ResourceMath.DIAMOND;
		} else if (item == Items.EMERALD && ExtrasModule.GEN_EMERALD.get()) {
			return ResourceMath.EMERALD;
		}
		return -1;
	}

	/** True when the radar is on and centred on your bed (then the bed scan runs for it, too). */
	private static boolean radarOnBed() {
		return ExtrasModule.RADAR.get() && ExtrasModule.RADAR_CENTER.get().endsWith(".bed");
	}

	/** Your bed for the radar: where it is and whether it is within your render distance. */
	private static Radar.Bed radarBed(MinecraftClient client) {
		BedTracker.Info info = BEDS.ownBed(ownTeam, spawn, client.player.x, client.player.z);
		if (info == null) {
			return new Radar.Bed(0, 0, 0, RadarMath.BedState.NO_BED);
		}
		double x = info.centerX(), z = info.centerZ();
		boolean loaded = !client.world.getChunk(info.bed.headX >> 4, info.bed.headZ >> 4).isEmpty();
		double distance = Math.hypot(x - client.player.x, z - client.player.z);
		return new Radar.Bed(x, info.bed.y + 0.5, z,
			RadarMath.bedState(true, loaded, distance, client.options.viewDistance));
	}

	/** The Rush Distance option ("axolotlextras.rush.25" and so on) in blocks. */
	private static double rushDistance() {
		return HackMath.optionNumber(ExtrasModule.RUSH_DISTANCE.get(), 25);
	}

	/** "Red obby 8/8 64m NE" over every bed with obsidian, above its Bed Defense label. */
	private static List<HudLabel> waypointLabels(MinecraftClient client) {
		List<HudLabel> labels = new ArrayList<>();
		for (BedTracker.Info info : BEDS.waypoints()) {
			HudLabel label = new HudLabel(info.centerX(), info.bed.y + 1.3, info.centerZ());
			BedDefense.Cover cover = info.obsidian;
			label.text = TeamGuess.colored(info.team) + " §5obby " + (cover.complete() ? "§a" : "§e")
				+ cover.covered + "/" + cover.spots + " §7" + BedTracker.distance(client, info);
			label.raise = ExtrasModule.BED_DEFENSE.get() ? 10 * size(ExtrasModule.SIZE_BEDS) + 6 * size(ExtrasModule.SIZE_WAYPOINTS) + 8 : 0;
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

		long t = WATCH.start();
		if (ExtrasModule.PLAYER_ESP.get() || ExtrasModule.ESP_TRACERS.get()) {
			addPlayers(client, camera, tickDelta, DRAW);
		}
		if (ExtrasModule.OBSIDIAN_ALERT.get() && ExtrasModule.OBSIDIAN_WAYPOINTS.get()) {
			for (BedTracker.Info info : BEDS.waypoints()) {
				// A tall beam over the bed, in the team colour, so its island can be found from afar.
				int rgb = info.team == TeamGuess.UNKNOWN ? UNKNOWN_TEAM_RGB : HackMath.teamColor("\u00a7" + TeamGuess.CODES[info.team]);
				double x = info.centerX() - camX, z = info.centerZ() - camZ, y = info.bed.y - camY;
				DRAW.line(x, y, z, 0xE6000000 | rgb, x, y + 60, z, 0x3C000000 | rgb);
			}
		}
		PROJECTILES.render(client, tickDelta, DRAW, camX, camY, camZ);
		DRAW.flush();
		WATCH.end("ESP, beams and trajectories (drawing)", t);
	}

	private static void readMatrix(int which, float[] into) {
		MATRIX.clear();
		GL11.glGetFloat(which, MATRIX);
		for (int i = 0; i < 16; i++) {
			into[i] = MATRIX.get(i);
		}
	}

	/**
	 * Other real players. Left out: NPCs (Hypixel's shopkeepers and upgrade villagers are fake players with version 2
	 * UUIDs, a hidden name tag or "[NPC]" in the name), and players not in the tab list (hidden check bots).
	 */
	private static List<PlayerEntity> realPlayers(MinecraftClient client) {
		List<PlayerEntity> out = new ArrayList<>();
		Entity camera = client.getCameraEntity();
		for (PlayerEntity player : client.world.playerEntities) {
			if (player == client.player || player == camera || player.removed || !player.isAlive()) {
				continue;
			}
			if (HackMath.isNpc(player.getUuid())) {
				continue;
			}
			if (client.getNetworkHandler() != null && client.getNetworkHandler().getPlayerListEntry(player.getUuid()) == null) {
				continue;
			}
			AbstractTeam team = player.getScoreboardTeam();
			if (team != null && team.getNameTagVisibilityRule() == AbstractTeam.VisibilityRule.NEVER) {
				continue;
			}
			if (player.getName().asFormattedString().contains("[NPC]")) {
				continue;
			}
			out.add(player);
		}
		return out;
	}

	/** A teammate: only once the game runs (in the lobby nobody is), by scoreboard team or by team colour. */
	private static boolean isTeammate(MinecraftClient client, PlayerEntity player) {
		if (!gameRunning) {
			return false;
		}
		if (client.player.isInSameTeam(player)) {
			return true;
		}
		int team = TeamGuess.fromFormattedName(player.getName().asFormattedString());
		return ownTeam != TeamGuess.UNKNOWN && team == ownTeam;
	}

	/** The players ESP, tracers, nametags and enemy armor show, after the range, invisible and teammate switches. */
	private static List<PlayerEntity> targets(MinecraftClient client) {
		double range = HackMath.optionNumber(ExtrasModule.ESP_RANGE.get(), Double.MAX_VALUE);
		boolean invisible = ExtrasModule.ESP_INVISIBLE.get(), teammates = ExtrasModule.ESP_TEAMMATES.get();
		List<PlayerEntity> out = new ArrayList<>();
		for (PlayerEntity player : realPlayers(client)) {
			if (!invisible && player.isInvisible()) {
				continue;
			}
			if (range < Double.MAX_VALUE && player.squaredDistanceTo(client.player) > range * range) {
				continue;
			}
			if (!teammates && isTeammate(client, player)) {
				continue;
			}
			out.add(player);
		}
		return out;
	}

	/** The ESP colour of a player, after the ESP Color switch. */
	private static int espColor(MinecraftClient client, PlayerEntity player) {
		String mode = ExtrasModule.ESP_COLOR.get();
		if (mode.endsWith(".distance")) {
			return HackMath.redToGreen(Math.sqrt(player.squaredDistanceTo(client.player)) / 64);
		}
		if (mode.endsWith(".health")) {
			Float health = healthValue(client, player);
			return health == null ? 0xFFFFFF : HackMath.redToGreen(health / Math.max(1f, player.getMaxHealth()));
		}
		int rgb = HackMath.teamColor(player.getName().asFormattedString());
		return rgb < 0 ? 0xFFFFFF : rgb;
	}

	private static void addPlayers(MinecraftClient client, Entity camera, float tickDelta, Draw3D draw) {
		double[] eye = null;
		if (ExtrasModule.ESP_TRACERS.get()) {
			// The tracers start just in front of the eyes, so they come out of the middle of the screen.
			double yaw = Math.toRadians(camera.prevYaw + (camera.yaw - camera.prevYaw) * tickDelta);
			double pitch = Math.toRadians(camera.prevPitch + (camera.pitch - camera.prevPitch) * tickDelta);
			eye = new double[] {-Math.sin(yaw) * Math.cos(pitch), camera.getEyeHeight() - Math.sin(pitch),
				Math.cos(yaw) * Math.cos(pitch)};
		}
		for (PlayerEntity player : targets(client)) {
			int rgb = espColor(client, player);
			double ox = player.prevTickX + (player.x - player.prevTickX) * tickDelta - camX;
			double oy = player.prevTickY + (player.y - player.prevTickY) * tickDelta - camY;
			double oz = player.prevTickZ + (player.z - player.prevTickZ) * tickDelta - camZ;
			if (ExtrasModule.PLAYER_ESP.get()) {
				Box box = player.getBoundingBox();
				double x1 = box.minX - player.x + ox - 0.05, y1 = box.minY - player.y + oy, z1 = box.minZ - player.z + oz - 0.05;
				double x2 = box.maxX - player.x + ox + 0.05, y2 = box.maxY - player.y + oy + 0.1, z2 = box.maxZ - player.z + oz + 0.05;
				String style = ExtrasModule.ESP_STYLE.get();
				if (!style.endsWith(".outline")) {
					draw.filledBox(x1, y1, z1, x2, y2, z2, 0x40000000 | rgb);
				}
				if (!style.endsWith(".filled")) {
					draw.boxOutline(x1, y1, z1, x2, y2, z2, 0xFF000000 | rgb);
				}
			}
			if (eye != null) {
				draw.line(eye[0], eye[1], eye[2], ox, oy + player.height / 2, oz, 0xC8000000 | rgb);
			}
		}
	}

	// ------------------------------------------------------------------------------------------------------- HUD pass

	/** Enemy armor over each enemy's head: their best armor piece with its Protection level, and what they hold. */
	private static List<HudLabel> armorLabels(MinecraftClient client) {
		List<HudLabel> labels = new ArrayList<>();
		float t = lastTickDelta;
		for (PlayerEntity player : targets(client)) {
			if (isTeammate(client, player)) {
				continue;
			}
			HudLabel label = new HudLabel(player.prevTickX + (player.x - player.prevTickX) * t,
				player.prevTickY + (player.y - player.prevTickY) * t + player.height + 0.5,
				player.prevTickZ + (player.z - player.prevTickZ) * t);
			float armorSize = size(ExtrasModule.SIZE_ARMOR);
			// Above the name tag: half of each label's height plus a gap (the game's own name tag when ours is off).
			label.raise = ExtrasModule.NAMETAGS.get() ? 6 * size(ExtrasModule.SIZE_NAMETAGS) + 10 * armorSize + 2 : 4 + 10 * armorSize;
			ItemStack armor = bestArmor(player);
			if (armor != null) {
				int protection = protection(player);
				label.icon(armor, protection > 0 ? "P" + protection : "");
			}
			ItemStack held = ExtrasModule.ARMOR_HELD.get() ? player.getStackInHand() : null;
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

	/** Nametags through walls: team-coloured name, health and distance, over every other player. */
	private static List<HudLabel> nametags(MinecraftClient client) {
		List<HudLabel> labels = new ArrayList<>();
		float t = lastTickDelta;
		for (PlayerEntity player : targets(client)) {
			HudLabel label = new HudLabel(player.prevTickX + (player.x - player.prevTickX) * t,
				player.prevTickY + (player.y - player.prevTickY) * t + player.height + 0.5,
				player.prevTickZ + (player.z - player.prevTickZ) * t);
			Float health = ExtrasModule.NAMETAG_HEALTH.get() ? healthValue(client, player) : null;
			label.text = player.getName().asFormattedString()
				+ (health == null ? "" : " \u00a7c" + Math.round(health) + "\u2764")
				+ (ExtrasModule.NAMETAG_DISTANCE.get()
					? " \u00a77" + Math.round(Math.sqrt(player.squaredDistanceTo(client.player))) + "m" : "");
			labels.add(label);
		}
		return labels;
	}

	/**
	 * A player's health: from the server's health score (under the name or in the tab list, as Bed Wars shows it) when there
	 * is one, else the health the server sent for the player. Null when neither is known, so no number is made up.
	 */
	private static Float healthValue(MinecraftClient client, PlayerEntity player) {
		Scoreboard scoreboard = client.world.getScoreboard();
		String name = player.getGameProfile().getName();
		Map<ScoreboardObjective, ScoreboardPlayerScore> scores = scoreboard.getPlayerObjectives(name);
		for (int slot : new int[] {2, 0}) {
			ScoreboardObjective objective = scoreboard.getObjectiveForSlot(slot);
			if (objective != null && scores.containsKey(objective)) {
				return (float) scores.get(objective).getScore();
			}
		}
		float health = player.getHealth();
		return health > 0 && !Float.isNaN(health) ? health : null;
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
		long t = WATCH.start();
		if (ExtrasModule.RADAR.get()) {
			// Every real player, the radar's own switches filter them (not the ESP range, invisible and teammate ones).
			List<Radar.Target> targets = new ArrayList<>();
			for (PlayerEntity player : realPlayers(client)) {
				targets.add(new Radar.Target(player, isTeammate(client, player)));
			}
			Radar.render(client, targets, lastTickDelta, radarOnBed() ? radarBed(client) : null);
			WATCH.end("radar (drawing)", t);
			t = WATCH.start();
		}
		List<HudLabel> labels = new ArrayList<>();
		add(labels, generatorLabels, ExtrasModule.SIZE_GENERATORS);
		add(labels, BEDS.labels(), ExtrasModule.SIZE_BEDS);
		add(labels, waypointLabels, ExtrasModule.SIZE_WAYPOINTS);
		add(labels, PROJECTILES.labels(), ExtrasModule.SIZE_PROJECTILES);
		if (ExtrasModule.NAMETAGS.get()) {
			add(labels, nametags(client), ExtrasModule.SIZE_NAMETAGS);
		}
		if (ExtrasModule.ENEMY_ARMOR.get()) {
			add(labels, armorLabels(client), ExtrasModule.SIZE_ARMOR);
		}
		if (labels.isEmpty()) {
			WATCH.end("labels (drawing)", t);
			return;
		}
		int scale = new Window(client).getScaleFactor();
		// The farthest first, so nearer labels end up on top.
		Collections.sort(labels, (a, b) -> Double.compare(distanceSq(b), distanceSq(a)));
		for (HudLabel label : labels) {
			double[] screen = HackMath.project(label.x - camX, label.y - camY, label.z - camZ, modelView, projection, viewport);
			if (screen != null) {
				GlStateManager.pushMatrix();
				GlStateManager.translate((float) (screen[0] / scale),
					(float) ((client.height - screen[1]) / scale) - label.raise, 0f);
				GlStateManager.scale(label.scale, label.scale, 1f);
				drawLabel(client, label, 0, 0);
				GlStateManager.popMatrix();
			}
		}
		GlStateManager.disableLighting();
		GlStateManager.color(1f, 1f, 1f, 1f);
		WATCH.end("labels (drawing)", t);
	}

	private static void add(List<HudLabel> into, List<HudLabel> labels, IntegerOption size) {
		float scale = size(size);
		for (HudLabel label : labels) {
			label.scale = scale;
			into.add(label);
		}
	}

	/** A Label Sizes slider as a factor (150 percent is 1.5). */
	private static float size(IntegerOption option) {
		Integer percent = option.get();
		return percent == null ? 1f : percent / 100f;
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
