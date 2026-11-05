package io.github.alexweber64.axolotlextras.bedwars;

import java.util.List;

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
 * Ported from the MCRust 1.8.9 client. The ender chest is the count from the last time it was open.
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
	private final DepositTracker deposits = new DepositTracker();
	private boolean inBedWars;
	private boolean wasInBedWars;
	private boolean attackWasPressed;
	private int tickCount;
	private ItemStack[] icons;

	public ResourceCounterHud() {
		super(80, ResourceMath.COUNT * ROW_HEIGHT + 2, true);
	}

	/** Called when a new Bed Wars game starts: remembered ender chest counts belong to the previous game. */
	public void onGameStart() {
		resetAll();
	}

	/** Forgets everything: the ender chest counts, the inventory counts and any pending deposit. */
	public void resetAll() {
		java.util.Arrays.fill(enderChest, 0);
		java.util.Arrays.fill(inventory, 0);
		deposits.reset();
		wasInBedWars = false;
	}

	@Override
	public boolean tickable() {
		return true;
	}

	@Override
	public void tick() {
		tickCount++;
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

		Screen screen = client.currentScreen;
		boolean readChest = false;
		if (screen instanceof HandledScreen) {
			readChest = readEnderChest((HandledScreen) screen);
		}
		if (readChest) {
			// The real contents just replaced the remembered counts, so any estimate from a punch is obsolete.
			deposits.clearPending();
			deposits.update(inventory, tickCount, enderChest);
			deposits.clearPending();
		} else {
			detectPunch();
			deposits.update(inventory, tickCount, enderChest);
		}
	}

	/** Left-clicking an ender chest with resources in hand deposits them (Hypixel Bed Wars). */
	private void detectPunch() {
		boolean attack = client.options.attackKey.isPressed();
		boolean fresh = attack && !attackWasPressed;
		attackWasPressed = attack;
		if (!fresh || client.currentScreen != null) {
			return;
		}
		BlockHitResult target = client.result;
		if (target == null || target.type != BlockHitResult.Type.BLOCK) {
			return;
		}
		if (client.world.getBlockState(target.getBlockPos()).getBlock() != Blocks.ENDERCHEST) {
			return;
		}
		ItemStack held = client.player.inventory.getMainHandStack();
		if (held != null) {
			deposits.onPunch(indexOf(held.getItem()), held.count, tickCount);
		}
	}

	/** @return true when an ender chest was open and its contents were read */
	private boolean readEnderChest(HandledScreen screen) {
		List<Slot> slots = screen.screenHandler.slots;
		if (slots.isEmpty()) {
			return false;
		}
		Inventory container = slots.get(0).inventory;
		if (container == null || container instanceof PlayerInventory || container.getName() == null) {
			return false;
		}
		if (!ResourceMath.isEnderChestTitle(container.getName().asUnformattedString())) {
			return false;
		}
		int[] counts = new int[ResourceMath.COUNT];
		for (int i = 0; i < container.getInvSize(); i++) {
			add(counts, container.getInvStack(i));
		}
		System.arraycopy(counts, 0, enderChest, 0, counts.length);
		return true;
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
