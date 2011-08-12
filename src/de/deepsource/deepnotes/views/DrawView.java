package de.deepsource.deepnotes.views;

import de.deepsource.deepnotes.utilities.PerformanceTester;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.test.PerformanceTestCase;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom View class that implements all the drawing magic.
 * 
 * @author Sebastian Ullrich
 */
public class DrawView extends View{

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
	 * flag for existing background
	 */
	private boolean hasBackground = false;

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
		bitmap = Bitmap.createBitmap(480, 800, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(bitmap);	
		
		// paint config
		paint.setStrokeWidth(10f);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeJoin(Paint.Join.ROUND);
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
		hasBackground = true;
		backgroundModified = true;
	}

	/**
	 * Draws the bitmap on background.
	 */
	public void onDraw(Canvas canvas) {
		// fill the bitmap with default background color
		canvas.drawColor(Color.WHITE);
		
		// check if there is a background set
		if(hasBackground())
			canvas.drawBitmap(background, 0f, 0f, paint);
		
		canvas.drawBitmap(bitmap, 0f, 0f, paint);
		
		canvas.drawPath(path, paint);
	}
	
	private Path path = new Path();
	private float lastX, lastY;
	
	
	
	public void startDraw(float x, float y){
		PerformanceTester.start();
		
		path.reset();
		path.moveTo(x, y);
		lastX = x;
		lastY = y;
		invalidate();
	}
	
	public void continueDraw(float x, float y){
		
		// Bezier Smoothing
		path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
		
		//path.lineTo(x, y);
		
		lastX = x;
		lastY = y;
		invalidate();
		PerformanceTester.hit();
	}
	
	public void stopDraw(){
		path.lineTo(lastX, lastY);
		canvas.drawPath(path, paint);
		path.reset();
		invalidate();
		PerformanceTester.hit();
		PerformanceTester.stop();
		PerformanceTester.printMessurement();
	}
	
	public void drawPoint(float x, float y){
		canvas.drawPoint(x, y, paint);
		canvas.save();
	}
	
	 public boolean hasBackground(){
		 return hasBackground;
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
}
