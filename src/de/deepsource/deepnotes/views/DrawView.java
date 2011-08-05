package de.deepsource.deepnotes.views;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ViewFlipper;

import de.deepsource.deepnotes.models.CoordinatePair;

public class DrawView extends View{

	private int pointBufferSize = 1;
	private int pointBuffer = 0;

	private Bitmap bitmap;
	private Bitmap background;

	private Canvas canvas;

	private CoordinatePair pair, lastPair = null;
	private List<CoordinatePair> pointList = new ArrayList<CoordinatePair>();

	private Paint paint = new Paint();

	private class backgroundPainter extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			for (int i = 0; i < pointList.size(); i++) {
				pair = pointList.get(i);
				if(pair.isValid()){
					if (lastPair != null) {
						canvas.drawLine(lastPair.getX(), lastPair.getY(),
								pair.getX(), pair.getY(), paint);
					} else {
						canvas.drawCircle(pair.getX(), pair.getY(), 1f, paint);
					}
					lastPair = pair;
				}else{
					lastPair = null;
				}
				canvas.save();
			}
			pointList.clear();

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
		paint.setColor(Color.RED);
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
	 */
	public void setBackground(Bitmap bmp) {
		background = bmp;
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
		}
	}

	/**
	 * Clears the current page and post an invalidate state to force an update.
	 */
	public void clearNote() {
		bitmap = Bitmap.createBitmap(480, 800, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(bitmap);
		postInvalidate();
	}
	
	public Bitmap getBitmap() {
		return bitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	public void setPaintColor(int color) {
		paint.setColor(color);
	}
	
	public Bitmap getBackgroundBitmap() {
		return background;
	}

}
