package io.github.alexweber64.axolotlextras.perf;

import java.util.Arrays;
import java.util.Locale;

/** Frame time statistics. Pure logic, no Minecraft classes. */
public final class BenchStats {

	public final int frames;
	public final double avgFps;
	/** Average FPS over the slowest 1 percent of frames. */
	public final double low1Fps;
	/** Average FPS over the slowest 0.1 percent of frames. */
	public final double low01Fps;
	public final double worstMs;
	public final double p99Ms;

	private BenchStats(int frames, double avgFps, double low1Fps, double low01Fps, double worstMs, double p99Ms) {
		this.frames = frames;
		this.avgFps = avgFps;
		this.low1Fps = low1Fps;
		this.low01Fps = low01Fps;
		this.worstMs = worstMs;
		this.p99Ms = p99Ms;
	}

	/** @param frameMs frame times in milliseconds, only the first {@code count} entries are used */
	public static BenchStats compute(double[] frameMs, int count) {
		if (count <= 0) {
			return new BenchStats(0, 0, 0, 0, 0, 0);
		}
		double[] sorted = Arrays.copyOf(frameMs, count);
		Arrays.sort(sorted);
		double total = 0;
		for (double v : sorted) {
			total += v;
		}
		return new BenchStats(count, 1000.0 * count / total, fpsOfWorst(sorted, 0.01), fpsOfWorst(sorted, 0.001),
			sorted[count - 1], sorted[Math.min(count - 1, (int) Math.floor(count * 0.99))]);
	}

	/** Average FPS of the slowest {@code share} of the frames (sorted ascending, so the slowest are at the end). */
	private static double fpsOfWorst(double[] sortedAscending, double share) {
		int count = sortedAscending.length;
		int k = Math.max(1, (int) Math.ceil(count * share));
		double sum = 0;
		for (int i = count - k; i < count; i++) {
			sum += sortedAscending[i];
		}
		return 1000.0 / (sum / k);
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "frames=%d avg=%.1f fps, 1%%low=%.1f fps, 0.1%%low=%.1f fps, p99=%.2f ms, worst=%.2f ms",
			frames, avgFps, low1Fps, low01Fps, p99Ms, worstMs);
	}
}
