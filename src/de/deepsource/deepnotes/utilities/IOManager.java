/*
 * Deepnotes - Note Application for Android
 *
 * Copyright (C) 2011 Sebastian Ullrich & Jan Pretzel
 * http://www.deepsource.de
 */

package de.deepsource.deepnotes.utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import de.deepsource.deepnotes.R;
import de.deepsource.deepnotes.application.Deepnotes;

/**
 * This utility class handles the storage of the produced data.
 *
 * @author Jan Pretzel (jan.pretzel@deepsource.de)
 */
public final class IOManager {

	/**
	 * Utility classes should not have public or default constructor.
	 */
	// Author: Jan Pretzel
	private IOManager() {
		// empty
	}

	/**
	 * Deletes all files belonging to a specific note.
	 *
	 * @param context
	 *            The Context in which the method is called.
	 * @param noteName
	 *            The name of the note.
	 *
	 * @return Returns true if there where no problems deleting all the notes
	 *         files, else returns false.
	 */
	// Author: Jan Pretzel
	public static boolean deleteNote(final Context context, final String noteName) {
		if (noteName == null) {
			Log.e(Deepnotes.APP_NAME, "noteName must not be null");
			throw new IllegalArgumentException();
		}

		if (context == null) {
			Log.e(Deepnotes.APP_NAME, "context must not be null");
			throw new IllegalArgumentException();
		}

		boolean deleted = true;

		// delete note images + folder
		final File notePath = new File(context.getFilesDir() + "/" + noteName + "/");
		if (notePath.exists()) {
			// first delete files, because folder must be empty to be deleted
			final File[] noteFiles = notePath.listFiles();

			for (File file : noteFiles) {
				if (!file.delete()) {
					Log.e(Deepnotes.APP_NAME, Deepnotes.ERROR_FILE);
					deleted = false;
				}
			}

			if (!notePath.delete()) {
				Log.e(Deepnotes.APP_NAME, Deepnotes.ERROR_FILE);
				deleted = false;
			}
		}

		// delete thumbnail at the end, because when something went wrong,
		// the user will still see that something of the note remaines in
		// memory
		final File thumbnail = new File(context.getFilesDir()
				+ Deepnotes.SAVE_THUMBNAIL + noteName + Deepnotes.JPG_SUFFIX);
		if (!thumbnail.delete()) {
			Log.e(Deepnotes.APP_NAME, Deepnotes.ERROR_FILE);
			deleted = false;
		}

		Toast.makeText(context, R.string.note_deleted, Toast.LENGTH_SHORT).show();

		return deleted;
	}

	/**
	 * Initiates the cache saving, that is needed for sharing a note.
	 * To do so an {@link WriteShareCache} Object is created.
	 *
	 * @param activity
	 *            The Activity, that called the method.
	 * @param noteName
	 *            The name of the note that should be shared.
	 */
	// Author: Jan Pretzel
	public static void shareNote(final Activity activity, final String noteName) {
		if (activity == null) {
			Log.e(Deepnotes.APP_NAME, "activity must not be null");
			throw new IllegalArgumentException();
		}

		if (noteName == null) {
			Log.e(Deepnotes.APP_NAME, "noteName must not be null");
			throw new IllegalArgumentException();
		}

		new WriteShareCache(activity).execute(noteName);
	}

	/**
	 * Writes the cache, that is needed for sharing a note. Both fore- and
	 * background will be saved in one single file to the external storage, so
	 * that other applications can access them to make sharing possible. The
	 * Pages will be saved as JPG-Files numbered from 0 - 3. To write the files
	 * to the storage, it uses
	 * {@link IOManager#writeFile(Bitmap, String, android.graphics.Bitmap.CompressFormat, int)}.
	 *
	 * @author Jan Pretzel (jan.pretzel@deepsource.de)
	 */
	private static class WriteShareCache extends AsyncTask<String, Void, Void> {

		/**
		 * The ProgressDialog, that will be shown while writing the cache.
		 */
		private final ProgressDialog dialog;

		/**
		 * The Activity, that initiated the cache saving.
		 */
		private final Activity activity;

		/**
		 * Stores the Uri-objects for the cached files, and will be given to the Intent.
		 */
		private final ArrayList<Uri> uris;

		/**
		 * Constructor.
		 *
		 * @param initiater The Activity, that initiated the cache saving.
		 */
		// Author: Jan Pretzel
		public WriteShareCache(final Activity initiater) {
			super();

			if (initiater == null) {
				Log.e(Deepnotes.APP_NAME, "initiater must not be null");
				throw new IllegalArgumentException();
			}

			dialog = new ProgressDialog(initiater);
			this.activity = initiater;
			uris = new ArrayList<Uri>();
		}

		// Author: Jan Pretzel
		@Override
		protected Void doInBackground(final String... params) {
			// check cache before writing new files
			checkCache();

			// create temp files
			final String noteName = params[0];
			final File notePath = new File(activity.getFilesDir() + "/" + noteName + "/");

			if (notePath.exists()) {
				final File[] notePages = notePath.listFiles(new FileFilter() {

					@Override
					public boolean accept(final File pathname) {
						if (!pathname.toString().contains("background")) {
							return true;
						}
						return false;
					}
				});

				Arrays.sort(notePages);

				Bitmap note = null;
				Bitmap draw = null;
				Canvas canvas = null;

				int count = 0;
				for (File notePage : notePages) {
					// check for background if there is one draw it
					// else draw a white background
					String notePageName = notePage.getName();
					notePageName = notePageName.substring(0, notePageName.lastIndexOf('.'));
					final String bgPath = notePath
							+ "/background_"
							+ notePageName
							+ Deepnotes.JPG_SUFFIX;
					note = BitmapFactory.decodeFile(notePage.toString());

					// create those here, so they can be recycled
					// for every loop run
					draw = Bitmap.createBitmap(
							Deepnotes.getViewportWidth(),
							Deepnotes.getViewportHeight(),
							Bitmap.Config.ARGB_8888);
					canvas = new Canvas(draw);

					if (new File(bgPath).exists()) {
						final Bitmap temp = BitmapFactory.decodeFile(bgPath);
						canvas.drawBitmap(temp, 0f, 0f, null);

						temp.recycle();
					} else {
						canvas.drawColor(Color.WHITE);
					}

					canvas.drawBitmap(note, 0f, 0f, null);

					// write to external storage, because other applications
					// cannot access internal storage of another application
					if (Environment.getExternalStorageState().equals(
							Environment.MEDIA_MOUNTED)) {
						File outPath = new File(Environment.getExternalStorageDirectory()
								+ Deepnotes.SAVE_CACHE);

						outPath.mkdirs();

						final String outFile = outPath.toString()
								+ "/" + noteName + "_"
								+ count++ + Deepnotes.JPG_SUFFIX;

						writeFile(draw, outFile,
								Bitmap.CompressFormat.JPEG, Deepnotes.JPG_QUALITY);

						outPath = new File(outFile);
						if (outPath.exists()) {
							uris.add(Uri.fromFile(outPath));
						}
					} else {
						Toast.makeText(
								activity.getApplicationContext(),
								"External Storage not mounted",
								Toast.LENGTH_SHORT).show();
					}
				}

				// recycle
				if (note != null) {
					note.recycle();
				}

				if (draw != null) {
					draw.recycle();
				}
			}

			return null;
		}

		/**
		 * Checks if the cache folder has stored more than 2MB, if that's the
		 * case it will call {@link IOManager#clearCache()} to clear the
		 * cache.
		 */
		// Author: Jan Pretzel
		private void checkCache() {
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				final File cachePath = new File(
						Environment.getExternalStorageDirectory()
						+ Deepnotes.SAVE_CACHE);
				if (cachePath.exists()) {
					final File[] cacheFiles = cachePath.listFiles();

					long size = 0;
					for (File cacheFile : cacheFiles) {
						size += cacheFile.length();

						// 2MB 2097152
						final int maxSize = 2097152;
						if (size > maxSize) {
							clearCache();
							return;
						}
					}
				}
			}
		}

		// Author: Jan Pretzel
		@Override
		protected void onPreExecute() {
			dialog.setMessage(activity.getString(R.string.share_dialog));
			dialog.show();
			super.onPreExecute();
		}

		// Author: Jan Pretzel
		@Override
		protected void onPostExecute(final Void result) {
			if (dialog.isShowing()) {
				dialog.dismiss();
			}

			super.onPostExecute(result);

			final Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
			intent.setType("image/*");
			intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			activity.startActivityForResult(intent, Deepnotes.REQUEST_SHARE);
		}

	}

	/**
	 * Clears the external cache.
	 */
	// Author: Jan Pretzel
	public static void clearCache() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			final File cachePath = new File(
					Environment.getExternalStorageDirectory()
					+ Deepnotes.SAVE_CACHE);
			if (cachePath.exists()) {
				final File[] cachedImages = cachePath.listFiles();

				for (File cachedImage : cachedImages) {
					if (!cachedImage.delete()) {
						Log.e(Deepnotes.APP_NAME, Deepnotes.ERROR_FILE);
					}
				}

				if (!cachePath.delete()) {
					Log.e(Deepnotes.APP_NAME, Deepnotes.ERROR_FILE);
				}
			}
		}
	}

	/**
	 * Writes a Bitmap to the file system.
	 *
	 * @param bitmap
	 *            The Bitmap to be saved.
	 * @param file
	 *            The path where the Bitmap will be saved.
	 * @param format
	 *            The format the file will have.
	 * @param quality
	 *            The quality the image will have.
	 */
	// Author: Jan Pretzel
	public static void writeFile(final Bitmap bitmap, final String file,
			final Bitmap.CompressFormat format, final int quality) {
		if (bitmap == null) {
			Log.e(Deepnotes.APP_NAME, "bitmap must not be null");
			throw new IllegalArgumentException();
		}

		if (file == null) {
			Log.e(Deepnotes.APP_NAME, "file must not be null");
			throw new IllegalArgumentException();
		}

		if (format == null) {
			Log.e(Deepnotes.APP_NAME, "format must not be null");
			throw new IllegalArgumentException();
		}

		FileOutputStream fos = null;

		try {
			fos = new FileOutputStream(file);
			bitmap.compress(format, quality, fos);
		} catch (FileNotFoundException e) {
			Log.e(Deepnotes.APP_NAME, "failed to write file.");
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					Log.e(Deepnotes.APP_NAME, "failed to write file.");
				}
			}
		}
	}
}
