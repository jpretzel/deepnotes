package de.deepsource.deepnotes.application;

import android.app.Application;
import android.graphics.Color;

public class Deepnotes extends Application {
	
	/**
	 * Identifier for the sub folder, where thumbnails will be saved.
	 * 
	 * @author Jan Pretzel
	 */
	public static final String SAVE_THUMBNAIL = "/thumbnail/";
	
	/**
	 * Identifier for a note's name send with an intent.
	 * 
	 * @author Jan Pretzel
	 */
	public static final String SAVED_NOTE_NAME = "savedNoteName";
	
	/**
	 * Identifier for a request code, to share a note.
	 * 
	 * @author Jan Pretzel
	 */
	public static final int REQUEST_SHARE_NOTE = 0x00000002;
	
	/**
	 * 
	 */
	private static int viewportWidth;
	
	/**
	 * 
	 */
	private static int viewportHeight;
	
	public static final int NOTEPAGE_COUNT = 3;
	
	/**
	 * <h1>Distance moved to trigger an swipe event.</h1>
	 * 
	 * Sets the min. distance in percent swiped on surface,
	 * to trigger an swipe event.
	 * Value must be < 1.0f.
	 * 
	 * @author Sebastian Ullrich
	 * @value 1.0f;
	 */
	public static final float SWIPE_DISTANCE_TRIGGER = 0.4f;
	
	/**
	 *<h1>Avoid paint events when entering a swipe gesture.</h1>
	 * 
	 * To avoid paint events that accidently occur when swipe gestures
	 * entered, this value sets an offset time in ms, the event handler
	 * waits for executing rather a paint event or an swipe gesture.
	 * 
	 * @author Sebastian Ullrich
	 */
	public static final long PAINT_TIME_OFFSET = 200;
	
	/**
	 * <h1>Deepnotes Default Color Black</h1>
	 * 
	 * Custom style guide dependend color.
	 * 
	 * @author Sebastian Ullrich
	 */
	public static final int BLACK = Color.rgb(51, 51, 51);
	
	/**
	 * <h1>Deepnotes Default Color White</h1>
	 * 
	 * Custom style guide dependend color.
	 * 
	 * @author Sebastian Ullrich
	 */
	public static final int WHITE = Color.WHITE;
	
	/**
	 * <h1>Deepnotes Default Color Red</h1>
	 * 
	 * Custom style guide dependend color.
	 * 
	 * @author Sebastian Ullrich
	 */
	public static final int RED = Color.rgb(196, 27, 26);
	
	/**
	 * <h1>Deepnotes Default Color Yellow</h1>
	 * 
	 * Custom style guide dependend color.
	 * 
	 * @author Sebastian Ullrich
	 */
	public static final int YELLOW = Color.rgb(255, 194, 0);
	
	/**
	 * <h1>Deepnotes Background Color for Note</h1>
	 * 
	 * Custom style guide dependend color.
	 * 
	 * @author Sebastian Ullrich
	 */
	public static final int NOTE_BACKGROUND_COLOR = Color.rgb(255, 255, 255);

	public static int getViewportWidth() {
		return viewportWidth;
	}

	public static void setViewportWidth(int _viewportWidth) {
		viewportWidth = _viewportWidth;
	}

	public static int getViewportHeight() {
		return viewportHeight;
	}

	public static void setViewportHeight(int _viewportHeight) {
		viewportHeight = _viewportHeight;
	}
}
