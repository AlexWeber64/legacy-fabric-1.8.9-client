package io.github.alexweber64.axolotlextras.bedwars;

import java.util.List;

import io.github.axolotlclient.AxolotlClient;
import io.github.axolotlclient.AxolotlClientConfig.options.BooleanOption;
import io.github.axolotlclient.AxolotlClientConfig.options.Option;
import io.github.axolotlclient.modules.hud.gui.entry.TextHudEntry;
import io.github.axolotlclient.modules.hud.util.DrawPosition;
import io.github.axolotlclient.modules.hud.util.ItemUtil;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.slot.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;

/**
 * Bed Wars resource counter: iron, gold, diamonds and emeralds as "inventory + ender chest = total".
 * Ported from the MCRust 1.8.9 client. The ender chest count comes from Hypixel's deposit messages, which carry the new
 * total, and is replaced by the real contents whenever the ender chest is open.
 */
public class ResourceCounterHud extends TextHudEntry {

	public static final Identifier ID = new Identifier("axolotlextras", "resourcecounter");

	private static final int ROW_HEIGHT = 18;

	private final BooleanOption showEnderChest = new BooleanOption("axolotlextras.rc.enderChest", true);
	private final BooleanOption showTotal = new BooleanOption("axolotlextras.rc.total", true);
	private final BooleanOption showIcons = new BooleanOption("axolotlextras.rc.icons", true);
	private final BooleanOption onlyInBedWars = new BooleanOption("axolotlextras.rc.onlyBedWars", true);

	private final int[] inventory = new int[ResourceMath.COUNT];
	private final int[] enderChest = new int[ResourceMath.COUNT];
	private boolean inBedWars;

	/** Ticks the contents wait after the ender chest opens before they are compared (Hypixel sends them just after). */
	private static final int CHECK_TICKS = 5;
	/** Ticks the ender chest has been open, or -1 while it is closed. */
	private int chestOpenTicks = -1;
	/** The ender chest count when it opened, for the comparison. */
	private final int[] expectedChest = new int[ResourceMath.COUNT];
	private boolean chestChecked;
	/** The last container title logged as not read as an ender chest, so each one is logged once. */
	private String loggedTitle;
	/** True once this game's first ender chest read was logged. */
	private boolean loggedRead;
	private boolean wasInBedWars;
	private ItemStack[] icons;

	public ResourceCounterHud() {
		super(80, ResourceMath.COUNT * ROW_HEIGHT + 2, true);
	}

	/** Called when a new Bed Wars game starts: remembered ender chest counts belong to the previous game. */
	public void onGameStart() {
		resetAll();
	}

	/** Forgets everything: the ender chest counts and the inventory counts. */
	public void resetAll() {
		java.util.Arrays.fill(enderChest, 0);
		java.util.Arrays.fill(inventory, 0);
		wasInBedWars = false;
		loggedRead = false;
	}

	/**
	 * Called from the chat event. "Deposited x24 Iron Ingot into Ender Chest! (24 Total)" sets the iron in the ender
	 * chest to 24. Every deposit has its own message, also several in one click (hold the button and switch from iron to
	 * gold), which counting the punch could not see.
	 */
	public void onChatLine(String line) {
		int[] deposit = ResourceMath.enderChestDeposit(line);
		if (deposit != null) {
			enderChest[deposit[0]] = deposit[1];
		}
	}

	@Override
	public boolean tickable() {
		return true;
	}

	@Override
	public void tick() {
		if (client.world == null || client.player == null) {
			// Left the world (or disconnected): nothing carries over to the next one.
			inBedWars = false;
			resetAll();
			return;
		}
		ScoreboardObjective sidebar = client.world.getScoreboard().getObjectiveForSlot(1);
		inBedWars = sidebar != null && ResourceMath.isBedWarsTitle(sidebar.getDisplayName());
		if (!inBedWars) {
			if (wasInBedWars) {
				// Left Bed Wars (back to a lobby, for example).
				resetAll();
			}
			return;
		}
		wasInBedWars = true;

		java.util.Arrays.fill(inventory, 0);
		PlayerInventory playerInventory = client.player.inventory;
		for (ItemStack stack : playerInventory.main) {
			add(inventory, stack);
		}
		add(inventory, playerInventory.getCursorStack());

		// While the ender chest is open, its real contents replace the count, every tick (so items moved inside it count).
		Screen screen = client.currentScreen;
		int[] real = screen instanceof HandledScreen ? readEnderChest((HandledScreen) screen) : null;
		if (real == null) {
			chestOpenTicks = -1;
			return;
		}
		if (chestOpenTicks < 0) {
			if (!loggedRead) {
				loggedRead = true;
				AxolotlClient.LOGGER.info("[Extras] Resource Counter: reading the ender chest (once per game in this log)");
			}
			chestOpenTicks = 0;
			System.arraycopy(enderChest, 0, expectedChest, 0, expectedChest.length);
			chestChecked = false;
		} else {
			chestOpenTicks++;
		}
		if (!chestChecked && chestOpenTicks >= CHECK_TICKS) {
			chestChecked = true;
			checkAgainstExpected(real);
		}
		System.arraycopy(real, 0, enderChest, 0, real.length);
	}

	/**
	 * For debugging: once per opening, a line in latest.log when the ender chest holds something else than the counter
	 * expected. Items moved inside the chest in the first quarter second also show up here.
	 */
	private void checkAgainstExpected(int[] real) {
		String mismatch = ResourceMath.chestMismatch(expectedChest, real);
		if (mismatch != null) {
			AxolotlClient.LOGGER.info("[Extras] Resource Counter: the ender chest did not match: " + mismatch);
		}
	}

	/** @return the resources in the open ender chest, or null when no ender chest is open */
	private int[] readEnderChest(HandledScreen screen) {
		List<Slot> slots = screen.screenHandler.slots;
		if (slots.isEmpty()) {
			return null;
		}
		Inventory container = slots.get(0).inventory;
		if (container == null || container instanceof PlayerInventory || container.getName() == null) {
			return null;
		}
		String title = container.getName().asUnformattedString();
		// Hypixel's ender chest has no title, so it is also the chest you opened while looking at an ender chest block
		// (the camera stays still while a chest is open).
		if (!ResourceMath.isEnderChestTitle(title) && !lookingAtEnderChest()) {
			// For debugging: a chest that looks like the ender chest but is not read as one.
			if (title != null && title.toLowerCase().contains("ender") && !title.equals(loggedTitle)) {
				loggedTitle = title;
				AxolotlClient.LOGGER.info("[Extras] Resource Counter: opened \"" + title + "\", not read as the ender chest");
			}
			return null;
		}
		int[] counts = new int[ResourceMath.COUNT];
		for (int i = 0; i < container.getInvSize(); i++) {
			add(counts, container.getInvStack(i));
		}
		return counts;
	}

	/** True when the crosshair is on an ender chest block. */
	private boolean lookingAtEnderChest() {
		BlockHitResult hit = client.result;
		return hit != null && hit.type == BlockHitResult.Type.BLOCK && hit.getBlockPos() != null
			&& client.world.getBlockState(hit.getBlockPos()).getBlock() == Blocks.ENDERCHEST;
	}

	private static void add(int[] counts, ItemStack stack) {
		if (stack == null) {
			return;
		}
		int index = indexOf(stack.getItem());
		if (index >= 0) {
			counts[index] += Math.max(stack.count, 0);
		}
	}

	private static int indexOf(Item item) {
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

	@Override
	public void render(float delta) {
		if (onlyInBedWars.get() && !inBedWars) {
			return;
		}
		super.render(delta);
	}

	@Override
	public void renderComponent(float delta) {
		draw(inventory, enderChest);
	}

	@Override
	public void renderPlaceholderComponent(float delta) {
		draw(new int[] {37, 12, 4, 0}, new int[] {32, 1, 0, 4});
	}

	private void draw(int[] inv, int[] ender) {
		if (icons == null) {
			icons = new ItemStack[] {
				new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GOLD_INGOT),
				new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD)
			};
		}
		DrawPosition pos = getPos();
		int textX = pos.x() + (showIcons.get() ? 22 : 4);
		int widest = 0;
		String[] rows = new String[ResourceMath.COUNT];
		for (int i = 0; i < ResourceMath.COUNT; i++) {
			rows[i] = (showIcons.get() ? "" : ResourceMath.DISPLAY_NAMES[i] + ": ")
				+ ResourceMath.row(inv[i], ender[i], showEnderChest.get(), showTotal.get());
			widest = Math.max(widest, client.textRenderer.getStringWidth(rows[i]));
		}
		for (int i = 0; i < ResourceMath.COUNT; i++) {
			int y = pos.y() + 1 + i * ROW_HEIGHT;
			if (showIcons.get()) {
				ItemUtil.renderGuiItemModel(icons[i], pos.x() + 2, y);
			}
			drawString(rows[i], textX, y + 5, textColor.get().getAsInt(), shadow.get());
		}
		int newWidth = (textX - pos.x()) + widest + 4;
		if (newWidth != getWidth()) {
			setWidth(newWidth);
			onBoundsUpdate();
		}
	}

	@Override
	public boolean movable() {
		return true;
	}

	@Override
	public List<Option<?>> getConfigurationOptions() {
		List<Option<?>> options = super.getConfigurationOptions();
		options.add(showIcons);
		options.add(showEnderChest);
		options.add(showTotal);
		options.add(onlyInBedWars);
		return options;
	}

	@Override
	public Identifier getId() {
		return ID;
	}
}
