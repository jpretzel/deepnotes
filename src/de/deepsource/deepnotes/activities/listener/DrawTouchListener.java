package de.deepsource.deepnotes.activities.listener;

import android.view.MotionEvent;
import android.view.View;
import de.deepsource.deepnotes.activities.DrawActivity;
import de.deepsource.deepnotes.views.DrawView;

public class DrawTouchListener implements View.OnTouchListener {

	private DrawView drawView;
	private DrawActivity drawActivity;
	
	// TODO: use % of display width
	private float swipeTrigger = 100f;
	private float swipeXdelta = 0f;

	public DrawTouchListener(DrawActivity drawActivity, DrawView drawView) {
		this.drawView = drawView;
		this.drawActivity = drawActivity;
	}

	public boolean onTouch(View v, MotionEvent event) {
		// Checking for multiTouch
		if (event.getPointerCount() > 1) {
			switch (event.getAction()) {
			// 2-finger click, storing first coordinate
			case (MotionEvent.ACTION_DOWN):
				swipeXdelta = event.getX(0);
				break;
			
			// 2-finger swipe, calculating direction
			case (MotionEvent.ACTION_MOVE):
				if(Math.abs(swipeXdelta-event.getX(0)) > swipeTrigger) {
					if(swipeXdelta < event.getX(0)){
						// swipe left
						drawActivity.showPreviousDrawView();
					}else{
						// swipe right
						drawActivity.showNextDrawView();
					}
				}
				break;
			}
		} else {
			switch (event.getAction()) {
			case (MotionEvent.ACTION_UP):
				drawView.addPoint(-1f, -1f);
				break;

			case (MotionEvent.ACTION_DOWN):
			case (MotionEvent.ACTION_MOVE):
				drawView.addPoint(event.getX(), event.getY());
			}
		}

		return true;
	}
}
