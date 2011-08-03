package de.deepsource.deepnotes.activities.listener;

import android.view.MotionEvent;
import android.view.View;
import de.deepsource.deepnotes.views.DrawView;

public class DrawTouchListener implements View.OnTouchListener {
	
		private boolean multiTouch = false; 
		private DrawView drawView;
		
		
		public DrawTouchListener(DrawView drawView){
			this.drawView = drawView;
		}
		
		public boolean onTouch(View v, MotionEvent event) {
			
			if(event.getPointerCount() > 1 ){
				multiTouch = true;
			}else{
				multiTouch = false;
			}
			
			switch(event.getAction()){
				case (MotionEvent.ACTION_UP):
					drawView.addPoint(-1f, -1f);
					break;
					
					
				case (MotionEvent.ACTION_DOWN):
				case (MotionEvent.ACTION_MOVE):
					if(multiTouch){
						drawView.flipPage();
					}else{
						drawView.addPoint(event.getX(), event.getY());
					break;
					}
			}	
			return true;
		}
}
