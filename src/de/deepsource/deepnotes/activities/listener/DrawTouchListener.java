package de.deepsource.deepnotes.activities.listener;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import de.deepsource.deepnotes.activities.DrawActivity;
import de.deepsource.deepnotes.application.Deepnotes;
import de.deepsource.deepnotes.views.DrawView;

/**
 * This is a custom touch listener with implemented draw-paint and swipe gesture
 * recognition.
 * 
 * @author Sebastian Ullrich
 */
public class DrawTouchListener implements View.OnTouchListener {

	/**
	 * The parent View.
	 */
	private DrawView drawView;

	/**
	 * The parent Activity.
	 */
	private DrawActivity drawActivity;

	/**
	 * First coordinate for detecting swipe gestures.
	 */
	public float swipeXdelta = 0f;

	/**
	 * Distance moved to trigger an swipe event. Fallback value in 
	 * case of WindowManager delivers an error value.
	 * 
	 * @see Deepnotes
	 */
	public float swipeTrigger = 100f;

	/**
	 * Custom constructor.
	 * 
	 * @param drawActivity
	 *            parent Activity object.
	 * @param drawView
	 *            parent View object.
	 */
	public DrawTouchListener(DrawActivity drawActivity, DrawView drawView) {
		this.drawView = drawView;
		this.drawActivity = drawActivity;

		/* overwriting the fallback value, if WindowManager returns a valid value */
		if (drawActivity.getWindowManager().getDefaultDisplay().getWidth() > 0)
			swipeTrigger = drawActivity.getWindowManager()
					.getDefaultDisplay().getWidth()
					* Deepnotes.SWIPE_DISTANCE_TRIGGER;
	}

	public boolean onTouch(View v, MotionEvent event) {
		/*
		 * Checking for multiTouch
		 * 
		 * To test swipe in emulator (no painting) set: 
		 * 		event.getPointerCount() == 1 
		 * 
		 * To test swipe on an physical device set: 
		 * 		event.getPointerCount() > 1
		 */
		if (event.getPointerCount() > 1) {
			Log.e("swipeTrigger", String.valueOf(swipeTrigger));
			switch (event.getAction()) {
			
			/* 2-finger click, storing first coordinate */
			case (MotionEvent.ACTION_DOWN):
				swipeXdelta = event.getX(0);
				break;

			/* 2-finger swipe, calculating direction */
			case (MotionEvent.ACTION_MOVE):
				if (Math.abs(swipeXdelta - event.getX(0)) > swipeTrigger) {
					if (swipeXdelta < event.getX(0)) {
						// trigger swipe left
						drawActivity.showPreviousDrawView();
					} else {
						// trigger wipe right
						drawActivity.showNextDrawView();
					}
				}
				break;
			}
		} else {
			/*
			 * Avoid paint events when entering a swipe gesture. See
			 * de.deepsource.deepnotes.application.Deepnotes for further
			 * information.
			 */
			if (event.getDownTime() > Deepnotes.PAINT_TIME_OFFSET) {
				switch (event.getAction()) {

				/* single-touch lost */
				case (MotionEvent.ACTION_UP):
					drawView.addPoint(-1f, -1f);
					break;

				/* single-touch enter & move */
				case (MotionEvent.ACTION_DOWN):
				case (MotionEvent.ACTION_MOVE):
					drawView.addPoint(event.getX(), event.getY());
					break;
				}
			}
		}

		return true;
	}
}
