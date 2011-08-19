package de.deepsource.deepnotes.views;

import java.util.Vector;

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
	@Deprecated
	private boolean cleared = false;

	/**
	 * Holds a flag neither the note is new or loaded.
	 */
	private boolean isNewNote = true;

	/**
	 * Page Counter
	 */
	private int page = 0;

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

	private Vector<Path> pathVector = new Vector<Path>();
	private Vector<Paint> paintVector = new Vector<Paint>();

	/**
	 * init
	 */
	public void init() {
		// inti bitmap and canvas
		bitmap = Bitmap.createBitmap(Deepnotes.getViewportWidth(),
				Deepnotes.getViewportHeight(), Bitmap.Config.ARGB_4444);
		canvas = new Canvas(bitmap);

		// paint config
		paint.setStrokeWidth(Deepnotes.PEN_WIDTH_NORMAL);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setAntiAlias(true);
		paint.setDither(true);

		setOnTouchListener(this);
		setBackgroundColor(Color.WHITE);

		// TODO: use or don't use?
		setWillNotDraw(true);
		setWillNotCacheDrawing(true);
	}

	/**
	 * To set the imported image as background;
	 * 
	 * @param bmp
	 *            the background to be set
	 */
	public void setBackground(Bitmap Bitmap) {
		background = Bitmap;
		backgroundModified = true;
		((DrawActivity) getContext()).setSaveStateChanged(true);
	}

	/**
	 * This method redraws the hole surface of this view.
	 * 
	 * @author Sebastian Ullrich
	 */
	@Override
	public void onDraw(Canvas canvas) {

		// fill the bitmap with default background color
		canvas.drawColor(Color.WHITE);

		// check if there is a background set
		if (background != null)
			canvas.drawBitmap(background, 0f, 0f, paint);

		canvas.drawBitmap(bitmap, 0f, 0f, paint);
		canvas.drawPath(path, paint);

	}

	private Path path = new Path();
	private float lastX, lastY;

	/**
	 * This Method starts drawing on a new path.
	 * 
	 * @param x
	 *            Initial startpoint x.
	 * @param y
	 *            Initial startpoint y.
	 * 
	 * @author Sebastian Ullrich
	 */
	public void startDraw(float x, float y) {
		path.reset();
		path.moveTo(x, y);
		lastX = x;
		lastY = y;
		// invalidate();

		modified = true;
		((DrawActivity) getContext()).setSaveStateChanged(true);
	}

	/**
	 * This offsets extends the rerender-frame to avoid render artefacts while
	 * drawing a path.
	 * 
	 * @author Sebastian Ullrich
	 */
	private static final float invalidateOffset = 50f;

	/**
	 * Continues drawing a path. Is called by an ACTION_MOVE event. This method
	 * draws a cubic bezier-curve to smooth the entered Inut, while afterwards
	 * the rerender-frame is calculated.
	 * 
	 * @param x
	 *            Continious point x.
	 * @param y
	 *            Continious point x
	 * 
	 * @author Sebastian Ullrich
	 */
	public void continueDraw(float x, float y) {
		// Bezier Smoothing
		path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);

		// calculate rerender-frame
		if (y < lastY) {
			if (x < lastX) {
				invalidate((int) (x - invalidateOffset),
						(int) (y - invalidateOffset),
						(int) (lastX + invalidateOffset),
						(int) (lastY + invalidateOffset));
			} else {
				invalidate((int) (lastX - invalidateOffset),
						(int) (y - invalidateOffset),
						(int) (x + invalidateOffset),
						(int) (lastY + invalidateOffset));
			}
		} else {
			if (x < lastX) {
				invalidate((int) (x - invalidateOffset),
						(int) (lastY - invalidateOffset),
						(int) (lastX + invalidateOffset),
						(int) (y + invalidateOffset));
			} else {
				invalidate((int) (lastX - invalidateOffset),
						(int) (lastY - invalidateOffset),
						(int) (x + invalidateOffset),
						(int) (y + invalidateOffset));
			}
		}

		// store points for next cycle
		lastX = x;
		lastY = y;
	}

	/**
	 * This Method ends drawing a new path. The current path is getting stored
	 * and reseted. Afterwards a complete rerender is called.
	 * 
	 * @author Sebastian Ullrich
	 */
	public void stopDraw() {
		// end the path and draw it
		path.lineTo(lastX, lastY);
		canvas.drawPath(path, paint);

		// storing undo information
		pathVector.add(new Path(path));
		paintVector.add(new Paint(paint));

		// clear the path
		path.reset();

		// call rerender
		invalidate();
	}

	/**
	 * 
	 */
	public void undo() {
		Log.e("undo", "called");
		if (pathVector.isEmpty())
			return;

		clearNote();

		pathVector.remove(pathVector.size() - 1);
		paintVector.remove(paintVector.size() - 1);

		int pvc = pathVector.size();

		for (int i = 0; i < pvc; i++) {
			canvas.drawPath(pathVector.get(i), paintVector.get(i));
		}
	}

	/**
	 * Clears the current page and post an invalidate state to force an update.
	 * 
	 * @author Sebastian Ullrich
	 */
	public void clearNote() {

		// clear the hole bitmap
		bitmap = Bitmap.createBitmap(Deepnotes.getViewportWidth(),
				Deepnotes.getViewportHeight(), Bitmap.Config.ARGB_4444);

		canvas = new Canvas(bitmap);
		postInvalidate();

		// set delete status
		cleared = true;
		backgroundModified = false;
		modified = false;

		// check if we have to reload a given bmp.
		if (!isNewNote) {
			Log.e("clear", "reloadNotePage");
			((DrawActivity) getContext()).reloadNotePage(page);
		}
	}

	/**
	 * Retuns the current (foreground) bitmap
	 * 
	 * @param Bitmap
	 *            current foreground bitmap
	 */
	public Bitmap getBitmap() {
		return bitmap;
	}

	/**
	 * Sets the current (foreground) bitmap
	 * 
	 * @param bmp
	 *            current foreground bitmap to set
	 */
	public void loadBitmap(Bitmap bitmap) {
		canvas.drawBitmap(bitmap, new Matrix(), paint);

		// set flog isNewNote to false, because it's loaded
		Log.e("loading note", "setting isNewNote to false");
		isNewNote = false;
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
	 * @param width
	 */
	public void setPenWidth(float width) {
		paint.setStrokeWidth(width);
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
	 * Checks for the delete status. The note will be marked to be deleted when
	 * modified and bgModified are false and cleared is true and only then. This
	 * status is only reached by clearing the note and not doing anything after
	 * that. With the help of this method we prevent setting extra statuses on
	 * drawing actions and background changes.
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
	 * Recycles the DrawView, to make sure it gets collected by the GC.
	 * 
	 * @author Jan Pretzel
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

	/**
	 * This is the default method, called on MotionEvent.
	 * 
	 * @author Sebastian Ullrich
	 * 
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		/*
		 * Checking for multiTouch
		 * 
		 * To test swipe in emulator (no painting) set: event.getPointerCount()
		 * == 1
		 * 
		 * To test swipe on an physical device set: event.getPointerCount() > 1
		 */
		if (event.getPointerCount() == 1)
			onMultiTouch(event);
		else
			onSingleTouch(event);
		return true;
	}

	/**
	 * To ensure that only one pageturn is triggered by a swipe gesture.
	 */
	private static long swipeSemaphore = 0;

	/**
	 * 
	 * @param event
	 * @return
	 */
	private boolean onSingleTouch(MotionEvent event) {
		switch (event.getAction()) {
		case (MotionEvent.ACTION_DOWN):
			startDraw(event.getX(), event.getY());
			break;

		case (MotionEvent.ACTION_MOVE):
			continueDraw(event.getX(), event.getY());
			break;

		case (MotionEvent.ACTION_UP):
			stopDraw();
			break;
		}
		return true;
	}

	/**
	 * 
	 * @param event
	 */
	private void onMultiTouch(MotionEvent event) {
		
		switch (event.getAction()) {
			
		// 2-finger swipe, calculating direction
		case (MotionEvent.ACTION_MOVE):
			Log.e("MULTI", "MOVE");

			if(swipeSemaphore != event.getDownTime())
			if (Math.abs(swipeXdelta - event.getX(0)) > swipeTrigger) {
				swipeSemaphore = event.getDownTime();
				if (swipeXdelta < event.getX(0)) {
					// trigger swipe left
					((DrawActivity) getContext()).showPreviousDrawView();
				} else {
					// trigger wipe right
					((DrawActivity) getContext()).showNextDrawView();
				}
			}
			break;
		}
	}

	/**
	 * This will clear the undo cache.
	 * 
	 * @author Sebastian Ullrich
	 */
	public void clearUndoCache() {
		pathVector.clear();
		paintVector.clear();
	}

	/**
	 * @return the page
	 */
	public int getPage() {
		return page;
	}

	/**
	 * @param page
	 *            the page to set
	 */
	public void setPage(int page) {
		this.page = page;
	}
}
