package de.deepsource.deepnotes.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import de.deepsource.deepnotes.activities.DrawActivity;
import de.deepsource.deepnotes.application.Deepnotes;

/**
 * Custom View class that implements all the drawing magic.
 * 
 * @author Sebastian Ullrich
 */
public class DrawView extends View implements View.OnTouchListener {

	/**
	 * The foreground bitmap to paint on.
	 */
	private Bitmap bitmap;
	
	/**
	 * The background bitmap to set images on.
	 */
	private Bitmap background;

	/**
	 * The parent canvas.
	 */
	private Canvas canvas;
	
	/**
	 * The painter object
	 */
	protected Paint paint = new Paint();
	
	/**
	 * Flag for changes
	 */
	private boolean modified = false;
	
	/**
	 * Flag for modified background
	 */
	private boolean backgroundModified = false;
	
	/**
	 * TODO: donno
	 */
	private boolean cleared = false;

	/**
	 * Constructor.
	 * 
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public DrawView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	/**
	 * Constructor.
	 * 
	 * @param context
	 * @param attrs
	 */
	public DrawView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	/**
	 * Constructor.
	 * 
	 * @param context
	 *            Context.
	 */
	public DrawView(Context context) {
		super(context);
		init();
	}

	/**
	 * init
	 */
	public void init() {
		// inti bitmap and canvas
		bitmap = Bitmap.createBitmap(Deepnotes.getViewportWidth(), Deepnotes.getViewportHeight(), Bitmap.Config.ARGB_4444);
		canvas = new Canvas(bitmap);	
		
		// paint config
		paint.setStrokeWidth(10f);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setAntiAlias(true);
		paint.setDither(true);
		
		setOnTouchListener(this);
		
		// TODO: use or don't use?
		setWillNotDraw(true);
		setWillNotCacheDrawing(true);
	}

	/**
	 * To set the imported image as background;
	 * 
	 * @param bmp
	 * 				the background to be set
	 */
	public void setBackground(Bitmap Bitmap) {
		background = Bitmap;
		backgroundModified = true;
		((DrawActivity) getContext()).setSaveStateChanged(true);
	}

	/**
	 * Draws the bitmap on background.
	 */
	@Override
	public void onDraw(Canvas canvas) {
		// fill the bitmap with default background color
		canvas.drawColor(Color.WHITE);
		
		// check if there is a background set
		if(background != null)
			canvas.drawBitmap(background, 0f, 0f, paint);
		
		canvas.drawBitmap(bitmap, 0f, 0f, paint);
		
		canvas.drawPath(path, paint);
	}
	
	private Path path = new Path();
	private float lastX, lastY;
	
	public void startDraw(float x, float y) {
		path.reset();
		path.moveTo(x, y);
		lastX = x;
		lastY = y;
		//invalidate();
		
		modified = true;
		((DrawActivity) getContext()).setSaveStateChanged(true);
	}
	
	private static final float invalidateOffset = 50f;
	
	public void continueDraw(float x, float y) {
		// Bezier Smoothing
		path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
	
		if(y < lastY){
			if(x < lastX){
				postInvalidate(
						(int)(x - invalidateOffset), 
						(int)(y - invalidateOffset), 
						(int)(lastX + invalidateOffset), 
						(int)(lastY + invalidateOffset));
			}else{
				postInvalidate(
						(int)(lastX - invalidateOffset), 
						(int)(y - invalidateOffset), 
						(int)(x + invalidateOffset), 
						(int)(lastY + invalidateOffset));
			}
		}else{
			if(x < lastX){
				postInvalidate(
						(int)(x - invalidateOffset), 
						(int)(lastY - invalidateOffset), 
						(int)(lastX + invalidateOffset), 
						(int)(y + invalidateOffset));
			}else{
				postInvalidate(
						(int)(lastX - invalidateOffset), 
						(int)(lastY - invalidateOffset), 
						(int)(x + invalidateOffset), 
						(int)(y + invalidateOffset));
			}
		}
		
		//path.lineTo(x, y);
		lastX = x;
		lastY = y;
	}
	
	public void stopDraw() {
		path.lineTo(lastX, lastY);
		canvas.drawPath(path, paint);
		path.reset();
		invalidate();
	}
	
	public void drawPoint(float x, float y) {
		canvas.drawPoint(x, y, paint);
		canvas.save();
	}

	/**
	 * Clears the current page and post an invalidate state to force an update.
	 */
	public void clearNote() {
		bitmap = Bitmap.createBitmap(
				Deepnotes.getViewportWidth(), 
				Deepnotes.getViewportHeight(), 
				Bitmap.Config.ARGB_4444);
		
		if(backgroundModified)
			background = Bitmap.createBitmap(
					Deepnotes.getViewportWidth(), 
					Deepnotes.getViewportHeight(), 
					Bitmap.Config.ARGB_4444);

		canvas = new Canvas(bitmap);
		postInvalidate();
		
		// set delete status
		cleared = true;
		backgroundModified = false;
		modified = false;
	}
	
	/**
	 * Retuns the current (foreground) bitmap
	 * 
	 * @param Bitmap 
	 * 				current foreground bitmap 
	 */
	public Bitmap getBitmap() {
		return bitmap;
	}

	/**
	 * Sets the current (foreground) bitmap
	 * 
	 * @param bmp
	 * 				current foreground bitmap to set
	 */
	public void loadBitmap(Bitmap weakBitmap) {
		canvas.drawBitmap(weakBitmap, new Matrix(), paint);
	}
	
	/**
	 * 
	 * @param color
	 */
	public void setPaintColor(int color) {
		paint.setColor(color);
	}
	
	/**
	 * Returns the current picked color.
	 * 
	 * @return current picked color.
	 */
	public int getPaintColor() {
		return paint.getColor();
	}
	
	/**
	 * 
	 * @return
	 */
	public Bitmap getBackgroundBitmap() {
		return background;
	}
	
	public boolean isModified() {
		return modified;
	}
	
	public boolean isBGModified() {
		return backgroundModified;
	}
	
	/**
	 * Checks for the delete status. The note will be marked to be
	 * deleted when modified and bgModified are false and cleared is
	 * true and only then. This status is only reached by clearing the
	 * note and not doing anything after that. With the help of this 
	 * method we prevent setting extra statuses on drawing actions and
	 * background changes.
	 * 
	 * @author Jan Pretzel
	 * 
	 * @return whether the note should be deleted or not.
	 */
	public boolean deleteStatus() {
		if (!modified && !backgroundModified && cleared) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param visible
	 * @author Sebastian Ullrich
	 */
	public void recycle() {
		bitmap.recycle();
		bitmap = null;
		if (background != null) {
			background.recycle();
			background = null;
		}
		canvas = null;
		paint = null;
	}

	/******************************************************************************
	 * TOUCHLISTENER!!!!!!!!!!!!!!!!!
	 ******************************************************************************/
	
	private float swipeXdelta = 0f;
	
	private float swipeTrigger = 100f;
	
	private boolean swipeGestureTriggered = false;
	
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		/*
		 * Checking for multiTouch
		 * 
		 * To test swipe in emulator (no painting) set: 
		 * 		event.getPointerCount() == 1
		 * 
		 * To test swipe on an physical device set: 
		 * 		event.getPointerCount() > 1
		 * 
		 */
		if (event.getPointerCount() > 1)
			onMultiTouch(event);
		else
			onSingleTouch(event);
		
		return true;
	}
	
	private boolean onSingleTouch(MotionEvent event) {
		/*
		 * Avoid paint events when entering a swipe gesture. See
		 * de.deepsource.deepnotes.application.Deepnotes for further
		 * information.
		 */
	
			// There have been changes, a save dialog should appear
//			drawActivity.setSaveStateChanged(true);
			
			switch (event.getAction()) {
				case (MotionEvent.ACTION_DOWN):
					startDraw(event.getX(), event.getY());
					swipeXdelta = event.getX(0);
					break;
				
				case (MotionEvent.ACTION_MOVE):
					continueDraw(event.getX(), event.getY());
					break;
				
				case (MotionEvent.ACTION_UP):
					stopDraw();
					swipeGestureTriggered = false;
					break;
			}
			return true;
	}

	private void onMultiTouch(MotionEvent event) {
		switch (event.getAction()) {

		// 2-finger swipe, calculating direction
		case (MotionEvent.ACTION_MOVE):
			Log.e("MULTI", "MOVE");
			if (!swipeGestureTriggered 
					&& Math.abs(swipeXdelta - event.getX(0)) > swipeTrigger) {
				if (swipeXdelta < event.getX(0)) {
					// trigger swipe left
					((DrawActivity) getContext()).showPreviousDrawView();
				} else {
					// trigger wipe right
					((DrawActivity) getContext()).showNextDrawView();
				}
				
				swipeGestureTriggered = true;
			}
			break;
		}
	}
}
