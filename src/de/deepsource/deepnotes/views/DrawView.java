package de.deepsource.deepnotes.views;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;
import de.deepsource.deepnotes.models.CoordinatePair;

/**
 * Custom View class that implements all the drawing magic.
 * 
 * @author Sebastian Ullrich
 */
public class DrawView extends View{

	/**
	 * Points to be stored before triggering the 
	 * BackgroundPainter task. This value has strong
	 * impact on the app's performance. 
	 * <ul>
	 *  <li>Low values for high speed devices</li>
	 * 	<li>High values for slow speed device</li>
	 * </ul>
	 */
	private int pointBufferSize = 3;
	
	/**
	 * Initiation of point counter.
	 */
	private int pointBuffer = 0;

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

	private CoordinatePair pair, lastPair = null;
	private List<CoordinatePair> pointList = new ArrayList<CoordinatePair>();

	private Paint paint = new Paint();
	
	private boolean modified = false;
	private boolean bgModified = false;
	private boolean cleared = false;

	/**
	 * Asynchronous background task that draws coordiantes from stack.
	 * 
	 * To put a strong focus on speed issues, this background-worker is
	 * needed to handle the drawing part. Compared to other processes, the drawing
	 * process is a high intense procedure. To set the hole drawing procedure
	 * as a background task, which just runs periodically, more resources 
	 * will be available for the coordinate capture actions, which will lead to
	 * an increased drawing experience. 
	 * 
	 * @author Sebastian Ullrich
	 */
	private class backgroundPainter extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {		
			/* over all coordiantes from Stack */
			for (int i = 0; i < pointList.size(); i++) {
				/* assign current coordinate */
				pair = pointList.get(i);
				
				/* coordinate has to be valid (value >= 0) */
				if(pair.isValid()){
					
					/* check if drawing is already in progress */
					if (lastPair != null) {
						/* last coordinates available, draw a line */
						canvas.drawLine(lastPair.getX(), lastPair.getY(),
								pair.getX(), pair.getY(), paint);
					} 
					/* last coordinates aren't available, draw a point */
					else {
						canvas.drawCircle(pair.getX(), pair.getY(), 1f, paint);
					}
					
					/* assign current coordiante as last coordiante */
					lastPair = pair;
				}
				/* current coordinate is invalid */
				else{
					lastPair = null;
				}
				canvas.save();
			}
			pointList.clear();

			/* this will force the ui to be updated */
			postInvalidate();
			return null;
		}
	}

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
		
		setBackgroundColor(Color.WHITE);
		
		bitmap = Bitmap.createBitmap(480, 800, Bitmap.Config.ARGB_8888);
		background = Bitmap.createBitmap(480, 800, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(bitmap);		
		paint.setStrokeWidth(10f);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setAntiAlias(true);
		paint.setDither(true);
	}

	/**
	 * To set the imported image as background;
	 * 
	 * @param bmp
	 * 				the background to be set
	 */
	public void setBackground(Bitmap bmp) {
		background = bmp;
		bgModified = true;
	}

	/**
	 * Draws the bitmap on background.
	 */
	public void onDraw(Canvas canvas) {
		canvas.drawBitmap(background, 0f, 0f, paint);
		canvas.drawBitmap(bitmap, 0f, 0f, paint);
	}

	/**
	 * Stores Coordinates for drawing.
	 * 
	 * @param x
	 *            x-coordinate.
	 * @param y
	 *            y-coordiante.
	 */
	public void addPoint(float x, float y) {
		pointList.add(new CoordinatePair(x, y));
		if (pointBuffer++ >= pointBufferSize) {
			pointBuffer = 0;
			new backgroundPainter().execute();
			modified = true;
		}
	}

	/**
	 * Clears the current page and post an invalidate state to force an update.
	 */
	public void clearNote() {
		bitmap = Bitmap.createBitmap(480, 800, Bitmap.Config.ARGB_8888);
		background = Bitmap.createBitmap(480, 800, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(bitmap);
		postInvalidate();
		
		// set delete status
		cleared = true;
		bgModified = false;
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
	public void loadBitmap(Bitmap bmp) {
		canvas.drawBitmap(bmp, new Matrix(), paint);
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
		return bgModified;
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
		if (!modified && !bgModified && cleared) {
			return true;
		}
		
		return false;
	}
}
