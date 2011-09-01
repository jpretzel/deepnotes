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
	private Bitmap backgroundBitmap;

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
		 * Needs to be called when something changed, that can be saved by the @see
		 * {@link DrawActivity}.
		 */
		void changed();

		/**
		 * Needs to be called when an user input was undone so that the @see
		 * {@link DrawActivity} can initiate reloading.
		 */
		void undone();
    }

	/**
	 * Will listen for changes and undone user inputs.
	 */
	private final DrawViewListener dvListener;

	/**
	 * Constructor.
	 *
	 * @param listener Will listen for changes and undone user inputs.
	 *
	 * @param context
	 *            The Context in which the DrawView was created.
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
	 * Initializes the DrawView.
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
	}

	@Override
	public final void onDraw(final Canvas drawCanvas) {

		// fill the bitmap with default background color
		drawCanvas.drawColor(Color.WHITE);

		// check if there is a background set
		if (backgroundBitmap != null) {
			drawCanvas.drawBitmap(backgroundBitmap, 0f, 0f, paint);
		}

		drawCanvas.drawBitmap(bitmap, 0f, 0f, paint);
		drawCanvas.drawPath(path, paint);

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
	 * Undoes an user input.
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
	 * Redraws the saved Paths.
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
	 * @param undo
	 *            Identifies whether the method was called from @see
	 *            {@link DrawView#undo()} or not.
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
				dvListener.undone();
			}

		} else {
			backgroundBitmap = null;
			clearUndoCache();

			// set delete status
			isCleared = true;
			bgModified = false;
			modified = false;

			dvListener.changed();
		}
	}

	/**
	 * Getter for @see {@link DrawView#bitmap}.
	 *
	 * @return @see {@link DrawView#bitmap}.
	 */
	public final Bitmap getBitmap() {
		return bitmap;
	}

	/**
	 * Loads @see {@link DrawView#bitmap}.
	 *
	 * TODO unterschied zum setter beschreiben?
	 *
	 * @param newBitmap
	 *            current foreground bitmap to set.
	 */
	public final void loadBitmap(final Bitmap newBitmap) {
		canvas.drawBitmap(newBitmap, new Matrix(), paint);

		// set flag isNewNote to false, because it's loaded
		Log.e("loading note", "setting isNewNote to false");
		isNewNote = false;
	}

	/**
	 * Setter for {@link DrawView#bitmap}.
	 *
	 * @param newBitmap The new {@link DrawView#bitmap}.
	 */
	public final void setBitmap(final Bitmap newBitmap) {
		this.bitmap = newBitmap;
		canvas = new Canvas(this.bitmap);
		isNewNote = false;
	}

	/**
	 * Sets the color of @see {@link DrawView#paint}.
	 *
	 * @param color The new color.
	 */
	public final void setPaintColor(final int color) {
		paint.setColor(color);
	}

	/**
	 * Gets the current color of @see {@link DrawView#paint}.
	 *
	 * @return The current color of @see {@link DrawView#paint}.
	 */
	public final int getPaintColor() {
		return paint.getColor();
	}

	/**
	 * Sets the stroke width of @see {@link DrawView#paint}.
	 *
	 * @param width The new stroke width.
	 */
	public final void setPenWidth(final float width) {
		paint.setStrokeWidth(width);
	}

	/**
	 * Gets the current stroke width of @see {@link DrawView#paint}.
	 *
	 * @return The current stroke width of @see {@link DrawView#paint}.
	 */
	public final float getPenWidth() {
		return paint.getStrokeWidth();
	}

	/**
	 * Getter for @see {@link DrawView#paint}.
	 *
	 * @return @see {@link DrawView#paint}.
	 */
	public final Paint getPaint() {
		return paint;
	}

	/**
	 * Setter for @see {@link DrawView#paint}.
	 *
	 * @param newPaint The new @see {@link DrawView#paint}.
	 */
	public final void setPaint(final Paint newPaint) {
		this.paint = newPaint;
	}

	/**
	 * Getter for @see {@link DrawView#backgroundBitmap}.
	 *
	 * @return @see {@link DrawView#backgroundBitmap}.
	 */
	public final Bitmap getBackgroundBitmap() {
		return backgroundBitmap;
	}

	/**
	 * Sets a given Bitmap as background.
	 *
	 * @param bgBitmap
	 *            The background to be set.
	 *
	 * @param newModified
	 *            Whether the background was modified (true) or just loaded from
	 *            memory (false)
	 */
	public final void setBackgroundBitmap(final Bitmap bgBitmap, final boolean newModified) {
		backgroundBitmap = bgBitmap;
		bgModified = newModified;
	}

	/**
	 * Tells if the foreground of the note the DrawView presents was modified or
	 * not.
	 *
	 * @return Whether the foreground was modified or not.
	 */
	public final boolean isModified() {
		return modified;
	}

	/**
	 * Tells if the background of the note the DrawView presents was modified or not.
	 *
	 * @return Whether the background was modified or not.
	 */
	public final boolean isBgModified() {
		return bgModified;
	}

	/**
	 * Setter @see {@link DrawView#modified}.
	 *
	 * @param newModified The new @see {@link DrawView#modified}.
	 */
	public final void setModified(final boolean newModified) {
		this.modified = newModified;
	}

	/**
	 * Setter @see {@link DrawView#bgModified}.
	 *
	 * @param bgmodified The new @see {@link DrawView#bgModified}.
	 */
	public final void setBgModified(final boolean bgmodified) {
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
		if (backgroundBitmap != null) {
			backgroundBitmap.recycle();
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
