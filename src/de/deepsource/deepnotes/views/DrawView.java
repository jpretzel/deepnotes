package de.deepsource.deepnotes.views;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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
	 * The painter object.
	 */
	protected Paint paint = new Paint();

	/**
	 * Flag for changes.
	 */
	private boolean modified = false;

	/**
	 * Flag for modified background.
	 */
	private boolean bgModified = false;

	/**
	 * Identifies whether the view was cleared or not.
	 */
	private boolean isCleared = false;

	/**
	 * Holds a flag neither the note is new or loaded.
	 */
	private boolean isNewNote = true;

	/**
	 * Page Counter
	 */

	/**
	 * An interface that should handle changes of the DrawView.
	 *
	 * @author Jan Pretzel
	 */
	public interface DrawViewListener {
		/**
		 * TODO .
		 */
        void changed();

        /**
         * TODO .
         */
        void cleared();
    }

	/**
	 * TODO .
	 */
	private final DrawViewListener dvListener;

	/**
	 * Constructor.
	 *
	 * @param listener TODO
	 *
	 * @param context
	 *            Context.
	 */
	public DrawView(final Context context, final DrawViewListener listener) {
		super(context);
		dvListener = listener;
		init();
	}

	/**
	 * TODO .
	 */
	private final List<Path> pathList = new ArrayList<Path>();

	/**
	 * TODO .
	 */
	private final List<Paint> paintList = new ArrayList<Paint>();

	/**
	 * TODO .
	 */
	public final void init() {
		// paint config
		paint.setStrokeWidth(Deepnotes.PEN_WIDTH_NORMAL);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setAntiAlias(true);
		paint.setDither(true);

		setOnTouchListener(this);
		setBackgroundColor(Color.WHITE);

		setWillNotDraw(true);
//		setWillNotCacheDrawing(true);
	}

	/**
	 * To set the imported image as background.
	 *
	 * @param bitmap
	 *            The background to be set.
	 *
	 * @param modified
	 *            Whether the background was modified (true) or just loaded from
	 *            memory (false)
	 */
	public final void setBackground(final Bitmap bitmap, final boolean modified) {
		background = bitmap;
		bgModified = modified;
	}

	@Override
	public final void onDraw(final Canvas canvas) {

		// fill the bitmap with default background color
		canvas.drawColor(Color.WHITE);

		// check if there is a background set
		if (background != null) {
			canvas.drawBitmap(background, 0f, 0f, paint);
		}

		canvas.drawBitmap(bitmap, 0f, 0f, paint);
		canvas.drawPath(path, paint);

	}

	/**
	 * TODO .
	 */
	private final Path path = new Path();

	/**
	 * TODO .
	 */
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
	public final void startDraw(final float x, final float y) {
		path.reset();
		path.moveTo(x, y);
		lastX = x;
		lastY = y;

		modified = true;
		dvListener.changed();
	}

	/**
	 * This offsets extends the rerender-frame to avoid render artefacts while
	 * drawing a path.
	 *
	 * @author Sebastian Ullrich
	 */
	private static final float INVALIDATE_OFFSET = 50f;

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
	public final void continueDraw(final float x, final float y) {
		// Bezier Smoothing
		path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);

		// calculate rerender-frame
		if (y < lastY) {
			if (x < lastX) {
				invalidate((int) (x - INVALIDATE_OFFSET),
						(int) (y - INVALIDATE_OFFSET),
						(int) (lastX + INVALIDATE_OFFSET),
						(int) (lastY + INVALIDATE_OFFSET));
			} else {
				invalidate((int) (lastX - INVALIDATE_OFFSET),
						(int) (y - INVALIDATE_OFFSET),
						(int) (x + INVALIDATE_OFFSET),
						(int) (lastY + INVALIDATE_OFFSET));
			}
		} else {
			if (x < lastX) {
				invalidate((int) (x - INVALIDATE_OFFSET),
						(int) (lastY - INVALIDATE_OFFSET),
						(int) (lastX + INVALIDATE_OFFSET),
						(int) (y + INVALIDATE_OFFSET));
			} else {
				invalidate((int) (lastX - INVALIDATE_OFFSET),
						(int) (lastY - INVALIDATE_OFFSET),
						(int) (x + INVALIDATE_OFFSET),
						(int) (y + INVALIDATE_OFFSET));
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
	public final void stopDraw() {
		// end the path and draw it
		path.lineTo(lastX, lastY);
		canvas.drawPath(path, paint);

		// storing undo information
		pathList.add(new Path(path));
		paintList.add(new Paint(paint));

		// clear the path
		path.reset();

		// call rerender
		invalidate();
	}

	/**
	 *
	 */
	public final void undo() {
		Log.e("undo", "called");
		if (pathList.isEmpty()) {
			return;
		}

		clearView(true);

		pathList.remove(pathList.size() - 1);
		paintList.remove(paintList.size() - 1);

		redraw();
	}

	/**
	 * TODO .
	 */
	public final void redraw() {
		if (pathList.isEmpty()) {
			return;
		}

		final int pvc = pathList.size();

		for (int i = 0; i < pvc; i++) {
			canvas.drawPath(pathList.get(i), paintList.get(i));
		}
	}

	/**
	 * Clears the current page and post an invalidate state to force an update.
	 *
	 * @param undo TODO
	 *
	 * @author Sebastian Ullrich
	 * @author Jan Pretzel
	 */
	public final void clearView(final boolean undo) {

		// clear the hole bitmap
		bitmap = Bitmap.createBitmap(Deepnotes.getViewportWidth(),
				Deepnotes.getViewportHeight(), Bitmap.Config.ARGB_8888);

		canvas = new Canvas(bitmap);
		invalidate();

		if (undo) {
			// check if we have to reload a given bmp.
			if (!isNewNote) {
				Log.e("clear", "reloadNotePage");
				dvListener.cleared();
			}

		} else {
			background = null;
			clearUndoCache();

			// set delete status
			isCleared = true;
			bgModified = false;
			modified = false;

			dvListener.changed();
		}
	}

	/**
	 * Retuns the current (foreground) bitmap.
	 *
	 * @return TODO
	 */
	public final Bitmap getBitmap() {
		return bitmap;
	}

	/**
	 * Sets the current (foreground) bitmap.
	 *
	 * @param bitmap
	 *            current foreground bitmap to set
	 */
	public final void loadBitmap(final Bitmap bitmap) {
		canvas.drawBitmap(bitmap, new Matrix(), paint);

		// set flag isNewNote to false, because it's loaded
		Log.e("loading note", "setting isNewNote to false");
		isNewNote = false;
	}

	/**
	 * TODO .
	 *
	 * @param bitmap TODO
	 */
	public final void setBitmap(final Bitmap bitmap) {
		this.bitmap = bitmap;
		canvas = new Canvas(this.bitmap);
		isNewNote = false;
	}

	/**
	 * TODO .
	 *
	 * @param color TODO
	 */
	public final void setPaintColor(final int color) {
		paint.setColor(color);
	}

	/**
	 * Returns the current picked color.
	 *
	 * @return current picked color.
	 */
	public final int getPaintColor() {
		return paint.getColor();
	}

	/**
	 * TODO .
	 *
	 * @param width TODO
	 */
	public final void setPenWidth(final float width) {
		paint.setStrokeWidth(width);
	}

	/**
	 * TODO .
	 *
	 * @param width TODO
	 * @return TODO
	 */
	public final float getPenWidth(final float width) {
		return paint.getStrokeWidth();
	}

	/**
	 * TODO .
	 *
	 * @return TODO
	 */
	public final Paint getPaint() {
		return paint;
	}

	/**
	 * TODO .
	 *
	 * @param paint TODO
	 */
	public final void setPaint(final Paint paint) {
		this.paint = paint;
	}

	/**
	 * TODO .
	 *
	 * @return TODO
	 */
	public final Bitmap getBackgroundBitmap() {
		return background;
	}

	/**
	 * TODO .
	 *
	 * @return TODO
	 */
	public final boolean isModified() {
		return modified;
	}

	/**
	 * TODO .
	 *
	 * @return TODO
	 */
	public final boolean isBGModified() {
		return bgModified;
	}

	/**
	 * TODO .
	 *
	 * @param modified TODO
	 */
	public final void setModified(final boolean modified) {
		this.modified = modified;
	}

	/**
	 * TODO .
	 *
	 * @param bgmodified TODO
	 */
	public final void setBGModified(final boolean bgmodified) {
		bgModified = bgmodified;
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
	public final boolean deleteStatus() {
		return !modified && !bgModified && isCleared;
	}

	/**
	 * Recycles the DrawView, to make sure it gets collected by the GC.
	 *
	 * @author Jan Pretzel
	 */
	public final void recycle() {
		if (bitmap != null) {
			bitmap.recycle();
		}
		if (background != null) {
			background.recycle();
		}
	}

	@Override
	public final boolean onTouch(final View view, final MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			startDraw(event.getX(), event.getY());
			break;

		case MotionEvent.ACTION_MOVE:
			continueDraw(event.getX(), event.getY());
			break;

		case MotionEvent.ACTION_UP:
			stopDraw();
			break;

		default:
			break;
		}
		return true;
	}

	/**
	 * This will clear the undo cache.
	 *
	 * @author Sebastian Ullrich
	 */
	public final void clearUndoCache() {
		pathList.clear();
		paintList.clear();
	}
}
