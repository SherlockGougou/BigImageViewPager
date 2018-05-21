package cc.shinichi.library.byakugallery;

public class MathUtils {

	public static int ceilLog2(float value) {
		int i;
		for (i = 0; i < 31; i++) {
			if ((1 << i) >= value) break;
		}
		return i;
	}

	public static int floorLog2(float value) {
		int i;
		for (i = 0; i < 31; i++) {
			if ((1 << i) > value) break;
		}
		return i - 1;
	}

	// Returns the input value x clamped to the range [min, max].
	public static int clamp(int x, int min, int max) {
		if (x > max) return max;
		if (x < min) return min;
		return x;
	}

	// Returns the input value x clamped to the range [min, max].
	public static float clamp(float x, float min, float max) {
		if (x > max) return max;
		if (x < min) return min;
		return x;
	}

	// Returns the input value x clamped to the range [min, max].
	public static long clamp(long x, long min, long max) {
		if (x > max) return max;
		if (x < min) return min;
		return x;
	}
}