package de.deepsource.deepnotes.views;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.view.View;

import de.deepsource.deepnotes.models.CoordinatePair;

public class DrawView extends View {

	private int pointBufferSize = 3;
	private int pointBuffer = 0;

	private Bitmap bitmap;
	private Canvas canvas;
	
	private CoordinatePair pair, lastPair = null;

	private List<CoordinatePair> pointList = new ArrayList<CoordinatePair>();
	
	private Paint paint = new Paint();
	
	private class backgroundPainter extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			for (int i = 0; i < pointList.size(); i++) {
				pair = pointList.get(i);
				if(lastPair != null){
					canvas.drawLine(lastPair.getX(), lastPair.getY(), pair.getX(), pair.getY(), paint);
				}
				canvas.drawCircle(pair.getX(), pair.getY(), 1f, paint);
				
				lastPair = pair;
				canvas.save();
			}
			pointList.clear();
			postInvalidate();
			return null;
		}
	}
	
	/**
	 * Constructor.
	 * @param context Context.
	 */
	public DrawView(Context context) {
		super(context);
		paint.setColor(Color.RED);
		bitmap = Bitmap.createBitmap(480, 800, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(bitmap);
		paint.setStrokeWidth(3f);
		paint.setStrokeCap(Paint.Cap.ROUND);
	}

	/**
	 * Draws the bitmap on background.
	 */
	public void onDraw(Canvas canvas) {
		canvas.drawBitmap(bitmap, 0f, 0f, paint);
	}

	/**
	 * Stores Coordinates for drawing.
	 * @param x x-coordinate.
	 * @param y	y-coordiante.
	 */
	public void addPoint(float x, float y) {
		pointList.add(new CoordinatePair(x, y));
		if (pointBuffer++ >= pointBufferSize) {
			pointBuffer = 0;
			new backgroundPainter().execute();
		}
	}
	
	/**
	 * Clears the current page and post an invalidate state to force and update.
	 */
	public void clearNote(){
		bitmap = Bitmap.createBitmap(480, 800, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(bitmap);
		postInvalidate();
	}

}
