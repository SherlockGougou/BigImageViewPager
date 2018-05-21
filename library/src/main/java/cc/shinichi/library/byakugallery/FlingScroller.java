package cc.shinichi.library.byakugallery;

public class FlingScroller {
	@SuppressWarnings("unused") private static final String TAG = "FlingController";

	// The fling duration (in milliseconds) when velocity is 1 pixel/second
	private static final float FLING_DURATION_PARAM = 50f;
	private static final int DECELERATED_FACTOR = 4;

	private int mStartX, mStartY;
	private int mMinX, mMinY, mMaxX, mMaxY;
	private double mSinAngle;
	private double mCosAngle;
	private int mDuration;
	private int mDistance;
	private int mFinalX, mFinalY;

	private int mCurrX, mCurrY;
	private double mCurrV;

	public int getFinalX() {
		return mFinalX;
	}

	public int getFinalY() {
		return mFinalY;
	}

	public int getDuration() {
		return mDuration;
	}

	public int getCurrX() {
		return mCurrX;
	}

	public int getCurrY() {
		return mCurrY;
	}

	public int getCurrVelocityX() {
		return (int) Math.round(mCurrV * mCosAngle);
	}

	public int getCurrVelocityY() {
		return (int) Math.round(mCurrV * mSinAngle);
	}

	public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
		mStartX = startX;
		mStartY = startY;
		mMinX = minX;
		mMinY = minY;
		mMaxX = maxX;
		mMaxY = maxY;

		double velocity = Math.hypot(velocityX, velocityY);
		mSinAngle = velocityY / velocity;
		mCosAngle = velocityX / velocity;
		//
		// The position formula: x(t) = s + (e - s) * (1 - (1 - t / T) ^ d)
		//     velocity formula: v(t) = d * (e - s) * (1 - t / T) ^ (d - 1) / T
		// Thus,
		//     v0 = d * (e - s) / T => (e - s) = v0 * T / d
		//

		// Ta = T_ref * (Va / V_ref) ^ (1 / (d - 1)); V_ref = 1 pixel/second;
		mDuration =
			(int) Math.round(FLING_DURATION_PARAM * Math.pow(Math.abs(velocity), 1.0 / (DECELERATED_FACTOR - 1)));

		// (e - s) = v0 * T / d
		mDistance = (int) Math.round(velocity * mDuration / DECELERATED_FACTOR / 1000);

		mFinalX = getX(1.0f);
		mFinalY = getY(1.0f);
	}

	public void computeScrollOffset(float progress) {
		progress = Math.min(progress, 1);
		float f = 1 - progress;
		f = 1 - (float) Math.pow(f, DECELERATED_FACTOR);
		mCurrX = getX(f);
		mCurrY = getY(f);
		mCurrV = getV(progress);
	}

	private int getX(float f) {
		int r = (int) Math.round(mStartX + f * mDistance * mCosAngle);
		if (mCosAngle > 0 && mStartX <= mMaxX) {
			r = Math.min(r, mMaxX);
		} else if (mCosAngle < 0 && mStartX >= mMinX) {
			r = Math.max(r, mMinX);
		}
		return r;
	}

	private int getY(float f) {
		int r = (int) Math.round(mStartY + f * mDistance * mSinAngle);
		if (mSinAngle > 0 && mStartY <= mMaxY) {
			r = Math.min(r, mMaxY);
		} else if (mSinAngle < 0 && mStartY >= mMinY) {
			r = Math.max(r, mMinY);
		}
		return r;
	}

	private double getV(float progress) {
		// velocity formula: v(t) = d * (e - s) * (1 - t / T) ^ (d - 1) / T
		return DECELERATED_FACTOR * mDistance * 1000 * Math.pow(1 - progress, DECELERATED_FACTOR - 1) / mDuration;
	}
}