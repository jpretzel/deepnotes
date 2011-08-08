package de.deepsource.deepnotes.application;

import android.app.Application;

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
	 * Identifier for a note's position in the GridView, send with an intent.
	 * With the help of the position the note's thumbnail can be updated
	 * or deleted.
	 * 
	 * @author Jan Pretzel
	 */
	public static final String SAVED_NOTE_POSITION = "savedNotePosition";
	
	public static final int SAVED_NOTE_DELETED = 0x00000002;
	
	
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
}
