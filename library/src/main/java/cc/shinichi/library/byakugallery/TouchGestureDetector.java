package cc.shinichi.library.byakugallery;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ScaleGestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class TouchGestureDetector {

	private final GestureDetectorCompat mGestureDetector;
	private final ScaleGestureDetector mScaleGestureDetector;

	public TouchGestureDetector(Context context, OnTouchGestureListener listener) {
		mGestureDetector = new GestureDetectorCompat(context, listener);
		mGestureDetector.setOnDoubleTapListener(listener);
		mScaleGestureDetector = new ScaleGestureDetector(context, listener);
		ScaleGestureDetectorCompat.setQuickScaleEnabled(mScaleGestureDetector, false);
	}

	public boolean onTouchEvent(MotionEvent event) {
		boolean ret = mScaleGestureDetector.onTouchEvent(event);
		if (!mScaleGestureDetector.isInProgress()) {
			ret |= mGestureDetector.onTouchEvent(event);
		}
		return ret;
	}

	public static abstract class OnTouchGestureListener
		implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener,
		ScaleGestureDetector.OnScaleGestureListener {

		@Override public boolean onDown(MotionEvent e) {
			return false;
		}

		@Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			return false;
		}

		@Override public void onLongPress(MotionEvent e) {
		}

		@Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			return false;
		}

		@Override public void onShowPress(MotionEvent e) {
		}

		@Override public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}

		@Override public boolean onDoubleTap(MotionEvent e) {
			return false;
		}

		@Override public boolean onDoubleTapEvent(MotionEvent e) {
			return false;
		}

		@Override public boolean onSingleTapConfirmed(MotionEvent e) {
			return false;
		}

		@Override public boolean onScale(ScaleGestureDetector detector) {
			return false;
		}

		@Override public boolean onScaleBegin(ScaleGestureDetector detector) {
			return false;
		}

		@Override public void onScaleEnd(ScaleGestureDetector detector) {
		}
	}
}