package io.github.alexweber64.axolotlextras.bedwars;

/**
 * Tracks "punch the ender chest to deposit what is in your hand". The client never sees the deposit itself, so it is
 * inferred: after a punch on an ender chest with a resource in hand, a drop in that resource's inventory total within
 * {@link #WINDOW_TICKS} ticks counts as accepted by the server and is added to the remembered ender chest count. If the
 * total does not drop in time the punch is forgotten. Opening the ender chest replaces any estimate with the real
 * contents.
 */
public final class DepositTracker {

	/** Half a second at 20 ticks per second. */
	public static final int WINDOW_TICKS = 10;

	private final int[] lastInventory = new int[ResourceMath.COUNT];
	private boolean haveLast;
	private int pendingIndex = -1;
	private int pendingCount;
	private int pendingTick;

	/** Records a punch on an ender chest. {@code index} is the resource in hand, or -1 if it is not a resource. */
	public void onPunch(int index, int heldCount, int tick) {
		if (index < 0 || index >= ResourceMath.COUNT || heldCount <= 0) {
			return;
		}
		pendingIndex = index;
		pendingCount = heldCount;
		pendingTick = tick;
	}

	/**
	 * Call once per tick with the current inventory totals. Adds an inferred deposit to {@code enderChest}.
	 *
	 * @return the amount added (0 when nothing was deposited)
	 */
	public int update(int[] inventory, int tick, int[] enderChest) {
		int added = 0;
		if (haveLast && pendingIndex >= 0) {
			if (tick - pendingTick > WINDOW_TICKS) {
				pendingIndex = -1;
			} else {
				int drop = lastInventory[pendingIndex] - inventory[pendingIndex];
				if (drop > 0) {
					added = Math.min(drop, pendingCount);
					enderChest[pendingIndex] += added;
					pendingIndex = -1;
				}
			}
		}
		System.arraycopy(inventory, 0, lastInventory, 0, ResourceMath.COUNT);
		haveLast = true;
		return added;
	}

	/** Forgets any pending punch, for example when the real ender chest contents have just been read. */
	public void clearPending() {
		pendingIndex = -1;
	}

	/** Forgets everything, for example when leaving a world. */
	public void reset() {
		pendingIndex = -1;
		haveLast = false;
	}
}
