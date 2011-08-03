package de.deepsource.deepnotes.activities.listener;

import android.view.MotionEvent;
import android.view.View;
import de.deepsource.deepnotes.activities.DrawActivity;
import de.deepsource.deepnotes.views.DrawView;

public class DrawTouchListener implements View.OnTouchListener {

	private boolean multiTouch = false;
	private DrawView drawView;
	private DrawActivity drawActivity;

	public DrawTouchListener(DrawActivity drawActivity, DrawView drawView) {
		this.drawView = drawView;
		this.drawActivity = drawActivity;
	}

	public boolean onTouch(View v, MotionEvent event) {

		if (event.getPointerCount() > 1) {
			
			return true;
		}

		switch (event.getAction()) {
		case (MotionEvent.ACTION_UP):
			drawView.addPoint(-1f, -1f);
			break;

		case (MotionEvent.ACTION_DOWN):
		case (MotionEvent.ACTION_MOVE):
			drawView.addPoint(event.getX(), event.getY());
		}

		return true;
	}
}
