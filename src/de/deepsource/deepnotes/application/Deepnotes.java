package de.deepsource.deepnotes.application;

import android.app.Application;
import android.graphics.Color;

/**
 * Deepnotes Application implementation is used for global variables and
 * constants.
 *
 * @author Jan Pretzel
 * @author Sebastian Ullrich
 */
public class Deepnotes extends Application {

	/**
	 * Identifier for the application name.
	 */
	public static final String APP_NAME = "deepnotes";

	/**
	 * Identifier for the sub folder, where thumbnails will be saved.
	 *
	 * @author Jan Pretzel
	 */
	public static final String SAVE_THUMBNAIL = "/thumbnail/";

	/**
	 * Identifier for the sub folder, where cached files will be saved.
	 * We cannot use getExternalCacheDir() because it's not supported in
	 * our minimum version.
	 *
	 * @author Jan Pretzel
	 */
	public static final String SAVE_CACHE = "/deepnotes/.cache/";

	/**
	 * Identifier for a note's name send with an intent.
	 *
	 * @author Jan Pretzel
	 */
	public static final String SAVED_NOTE_NAME = "savedNoteName";

	/**
	 * Identifier for an error message when a file could not be created.
	 */
	public static final String ERROR_FILE = "failed to delete file";

	/**
	 * Identifier for a request code, to share a note.
	 *
	 * @author Jan Pretzel
	 */
	public static final int REQUEST_SHARE = 0x00000002;

	/**
	 * Identifier for the quality used for jpgs.
	 */
	public static final int JPG_QUALITY = 70;

	/**
	 * Identifier for the jpg suffix.
	 */
	public static final String JPG_SUFFIX = ".jpg";

	/**
	 * Identifier for the quality used for pngs.
	 */
	public static final int PNG_QUALITY = 100;

	/**
	 * Identifier for the png suffix.
	 */
	public static final String PNG_SUFFIX = ".png";

	/**
	 * The width of the device.
	 */
	private static int viewportWidth;

	/**
	 * The height of the device.
	 */
	private static int viewportHeight;

	/**
	 * Identifier for the number of pages per note.
	 */
	public static final int NOTEPAGE_COUNT = 3;

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
	public static final int NOTE_BG_COLOR = Color.rgb(255, 255, 255);

	/**
	 * <h1>Deepnotes Pen Width</h1>
	 *
	 * Thick width for pen.
	 *
	 * @author Sebastian Ullrich
	 */
	public static final float PEN_WIDTH_THICK = 15f;

	/**
	 * <h1>Deepnotes Pen Width</h1>
	 *
	 * Normal width for pen.
	 *
	 * @author Sebastian Ullrich
	 */
	public static final float PEN_WIDTH_NORMAL = 10f;

	/**
	 * <h1>Deepnotes Pen Width</h1>
	 *
	 * Thin width for pen.
	 *
	 * @author Sebastian Ullrich
	 */
	public static final float PEN_WIDTH_THIN = 5f;

	/**
	 * Getter for @see {@link Deepnotes#viewportWidth}.
	 *
	 * @return {@link Deepnotes#viewportWidth}.
	 */
	public static int getViewportWidth() {
		return viewportWidth;
	}

	/**
	 * Setter for {@link Deepnotes#viewportWidth}.
	 *
	 * @param newViewportWidth The new width for the device.
	 */
	public static void setViewportWidth(final int newViewportWidth) {
		viewportWidth = newViewportWidth;
	}

	/**
	 * Getter for @see {@link Deepnotes#viewportHeight}.
	 *
	 * @return {@link Deepnotes#viewportHeight}.
	 */
	public static int getViewportHeight() {
		return viewportHeight;
	}

	/**
	 * Setter for {@link Deepnotes#viewportHeight}.
	 *
	 * @param newViewportHeight The new height for the device.
	 */
	public static void setViewportHeight(final int newViewportHeight) {
		viewportHeight = newViewportHeight;
	}
}
