package de.deepsource.deepnotes.models;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import de.deepsource.deepnotes.R;
import de.deepsource.deepnotes.application.Deepnotes;

/**
 * Simple model class to hold note elements required by MainActivity.
 * It holds the file name, the thumbnail of the note and it convertes
 * the file name to human readable date.
 *
 * @author Jan Pretzel
 */
public class Note {

	/**
	 * A human readable date, which will
	 * be displayed in MainActivity.
	 */
	private final String created;

	/**
	 * The thumbnail, which will be displayed
	 * in MainActivity.
	 */
	private Bitmap thumbnail;

	/**
	 * The file name without suffix, which is mainly
	 * used to communicate between MainActivity and
	 * DrawActivity.
	 */
	private String fileName;

	/**
	 * Constructor saves the file name, a human readable date and the thumbnail.
	 *
	 * @param context
	 *            The context from which we will get the applications directory
	 *            and resources.
	 *
	 * @param name
	 *            The files name.
	 */
	public Note(final Context context, final String name) {
		fileName = name;

		// get a date from filename
		// remove suffix if there is one
		if (fileName.contains(".")) {
			fileName = fileName.substring(0, fileName.lastIndexOf('.'));
		}

		final Date dateCreated = new Date(Long.parseLong(fileName));
		final SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.date_time_format));
		created = sdf.format(dateCreated);

		final File imageFile = new File(context.getFilesDir() + Deepnotes.SAVE_THUMBNAIL + fileName + ".jpg");
		if (imageFile.exists()) {
			thumbnail = BitmapFactory.decodeFile(imageFile.toString());
		}
	}

	/**
	 * Recycles the thumbnail bitmap.
	 * Should only be called when the thumbnail is not needed anymore.
	 */
	public final void recycle() {
		thumbnail.recycle();
	}

	/**
	 * Getter for created.
	 *
	 * @return A human readable date.
	 */
	public final String getCreated() {
		return created;
	}

	/**
	 * Getter for the thumbnail.
	 *
	 * @return The thumbnail as Bitmap.
	 */
	public final Bitmap getThumbnail() {
		return thumbnail;
	}

	/**
	 * Getter for the file name.
	 *
	 * @return The file name, without suffix.
	 */
	public final String getFileName() {
		return fileName;
	}

}
