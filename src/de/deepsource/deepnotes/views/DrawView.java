/*
 * Deepnotes - Note Application for Android
 *
 * Copyright (C) 2011 Sebastian Ullrich & Jan Pretzel
 * http://www.deepsource.de
 */

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
 * @author Sebastian Ullrich (sebastian.ullrich@deepsource.de)
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
	private Paint paint = new Paint();

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
	 * An interface that should handle changes of the DrawView.
	 *
	 * @author Jan Pretzel (jan.pretzel@deepsource.de)
	 */
	public interface DrawViewListener {
		/**
		 * Needs to be called when something changed, that can be saved by the
		 * {@link DrawActivity}.
		 */
		void changed();

		/**
		 * Needs to be called when an user input was undone so that the
		 * {@link DrawActivity} can initiate reloading.
		 */
		void undone();
	}

	/**
	 * Will listen for changes and undone user inputs.
	 */
	private final DrawViewListener dvListener;

	/**
	 * Constructor of DrawView.
	 *
	 * @param listener
	 *            Will listen for changes and undone user inputs.
	 *
	 * @param context
	 *            The Context in which the DrawView was created.
	 */
	// Author: Sebastian Ullrich
	public DrawView(final Context context, final DrawViewListener listener) {
		super(context);
		dvListener = listener;
		init();
	}

	/**
	 * Will hold a history of previous paths to enable undo functionality.
	 */
	private final List<Path> pathList = new ArrayList<Path>();

	/**
	 * Will hold a history of previous colors to enable undo functionality.
	 */
	private final List<Paint> paintList = new ArrayList<Paint>();

	/**
	 * Initializes the DrawView.
	 */
	// Author: Sebastian Ullrich
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

	// Author: Sebastian Ullrich
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
	 * Current path for drawing. A copy of this objekt will be stored in
	 * {@link DrawView#pathList}.
	 */
	private final Path path = new Path();

	/**
	 * Stores the last coordinates for building a path.
	 */
	private float lastX, lastY;

	/**
	 * This Method starts drawing on a new path.
	 *
	 * @param x
	 *            Initial startpoint x.
	 * @param y
	 *            Initial startpoint y.
	 */
	// Author: Sebastian Ullrich
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
	 *            Continious point y.
	 */
	// Author: Sebastian Ullrich
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
	 */
	// Author: Sebastian Ullrich
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
	 * Undoes an user input. Removes the last stored path and color and calls
	 * {@link DrawView#redraw()} .
	 */
	// Author: Sebastian Ullrich
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
	 * Redraws the saved Paths. See {@link DrawView#undo()} for further
	 * information.
	 */
	// Author: Sebastian Ullrich
	public final void redraw() {
		// check if there is a path queue.
		if (pathList.isEmpty()) {
			return;
		}

		final int pls = pathList.size();
		for (int i = 0; i < pls; i++) {
			canvas.drawPath(pathList.get(i), paintList.get(i));
		}
	}

	/**
	 * Clears the current page and post an invalidate state to force an update.
	 *
	 * @param undo
	 *            Identifies whether the method was called from
	 *            {@link DrawView#undo()} or not.
	 */
	// Author: Jan Pretzel
	// Author: Sebastian Ullrich
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
	 * Getter for {@link DrawView#bitmap}.
	 *
	 * @return {@link DrawView#bitmap}.
	 */
	// Author: Sebastian Ullrich
	public final Bitmap getBitmap() {
		return bitmap;
	}

	/**
	 * Loads {@link DrawView#bitmap}.
	 *
	 * @param newBitmap
	 *            current foreground bitmap to set.
	 */
	// Author: Sebastian Ullrich
	public final void loadBitmap(final Bitmap newBitmap) {
		canvas.drawBitmap(newBitmap, new Matrix(), paint);

		// set flag isNewNote to false, because it's loaded
		Log.e("loading note", "setting isNewNote to false");
		isNewNote = false;
	}

	/**
	 * Setter for {@link DrawView#bitmap}.
	 *
	 * @param newBitmap
	 *            The new {@link DrawView#bitmap}.
	 */
	// Author: Jan Pretzel
	public final void setBitmap(final Bitmap newBitmap) {
		this.bitmap = newBitmap;
		canvas = new Canvas(this.bitmap);
		isNewNote = false;
	}

	/**
	 * Sets the color of {@link DrawView#paint}.
	 *
	 * @param color
	 *            The new color.
	 */
	// Author: Sebastian Ullrich
	public final void setPaintColor(final int color) {
		paint.setColor(color);
	}

	/**
	 * Gets the current color of {@link DrawView#paint}.
	 *
	 * @return The current color of {@link DrawView#paint}.
	 */
	// Author: Sebastian Ullrich
	public final int getPaintColor() {
		return paint.getColor();
	}

	/**
	 * Sets the stroke width of {@link DrawView#paint}.
	 *
	 * @param width
	 *            The new stroke width.
	 */
	// Author: Sebastian Ullrich
	public final void setPenWidth(final float width) {
		paint.setStrokeWidth(width);
	}

	/**
	 * Gets the current stroke width of {@link DrawView#paint}.
	 *
	 * @return The current stroke width of {@link DrawView#paint}.
	 */
	// Author: Sebastian Ullrich
	public final float getPenWidth() {
		return paint.getStrokeWidth();
	}

	/**
	 * Getter for {@link DrawView#paint}.
	 *
	 * @return {@link DrawView#paint}.
	 */
	// Author: Sebastian Ullrich
	public final Paint getPaint() {
		return paint;
	}

	/**
	 * Setter for {@link DrawView#paint}.
	 *
	 * @param newPaint
	 *            The new {@link DrawView#paint}.
	 */
	// Author: Sebastian Ullrich
	public final void setPaint(final Paint newPaint) {
		this.paint = newPaint;
	}

	/**
	 * Getter for {@link DrawView#backgroundBitmap}.
	 *
	 * @return {@link DrawView#backgroundBitmap}.
	 */
	// Author: Sebastian Ullrich
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
	 *            memory (false).
	 */
	// Author: Jan Pretzel
	public final void setBackgroundBitmap(final Bitmap bgBitmap,
			final boolean newModified) {
		backgroundBitmap = bgBitmap;
		bgModified = newModified;
	}

	/**
	 * Tells if the foreground of the note the DrawView presents was modified or
	 * not.
	 *
	 * @return Whether the foreground was modified or not.
	 */
	// Author: Jan Pretzel
	public final boolean isModified() {
		return modified;
	}

	/**
	 * Tells if the background of the note the DrawView presents was modified or
	 * not.
	 *
	 * @return Whether the background was modified or not.
	 */
	// Author: Jan Pretzel
	public final boolean isBgModified() {
		return bgModified;
	}

	/**
	 * Setter {@link DrawView#modified}.
	 *
	 * @param newModified
	 *            The new {@link DrawView#modified}.
	 */
	// Author: Jan Pretzel
	public final void setModified(final boolean newModified) {
		this.modified = newModified;
	}

	/**
	 * Setter {@link DrawView#bgModified}.
	 *
	 * @param bgmodified
	 *            The new {@link DrawView#bgModified}.
	 */
	// Author: Jan Pretzel
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
	 * @return whether the note should be deleted or not.
	 */
	// Author: Jan Pretzel
	public final boolean deleteStatus() {
		return !modified && !bgModified && isCleared;
	}

	/**
	 * Recycles the DrawView, to make sure it gets collected by the GC.
	 */
	// Author: Jan Pretzel
	public final void recycle() {
		if (bitmap != null) {
			bitmap.recycle();
		}
		if (backgroundBitmap != null) {
			backgroundBitmap.recycle();
		}
	}

	// Author: Sebastian Ullrich
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
	 */
	// Author: Sebastian Ullrich
	public final void clearUndoCache() {
		pathList.clear();
		paintList.clear();
	}
}
