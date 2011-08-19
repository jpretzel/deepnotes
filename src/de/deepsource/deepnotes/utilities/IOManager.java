/**
 * 
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
 * @author Jan Pretzel (jan.pretzel@deepsource.de)
 * 
 *         This utility class handles the storage of the produced data.
 */
public final class IOManager {
	public static boolean deleteNote(Context context, String noteName) {
		// delete thumbnail
		File thumbnail = new File(context.getFilesDir()
				+ Deepnotes.SAVE_THUMBNAIL + noteName + ".jpg");
		if (!thumbnail.delete()) {
			return false;
		}

		// delete note images + folder
		File notePath = new File(context.getFilesDir() + "/" + noteName + "/");
		if (notePath.exists()) {
			// first delete files, because folder must be empty to be deleted
			File[] noteFiles = notePath.listFiles();

			for (File file : noteFiles) {
				if (!file.delete()) {
					return false;
				}
			}

			if (!notePath.delete()) {
				return false;
			}
		}
		
		Toast toast = Toast.makeText(context, R.string.note_deleted, Toast.LENGTH_SHORT);
		toast.show();

		return true;
	}
	
	public static void shareNote(Activity context, String noteName) {
		new WriteShareCache(context).execute(noteName);
	}
	
	private static class WriteShareCache extends AsyncTask<String, Void, Void> {
		
		private ProgressDialog dialog;
		private Activity activity;
		private ArrayList<Uri> uris;
		
		public WriteShareCache(Activity activity) {
			dialog = new ProgressDialog(activity);
			this.activity = activity;
			uris = new ArrayList<Uri>();
		}

		@Override
		protected Void doInBackground(String... params) {
			// check cache before writing new files
			checkCache();
			
			// create temp files
			String noteName = params[0];
			File notePath = new File(activity.getFilesDir() + "/" + noteName + "/");
			
			if (notePath.exists()) {
				File[] notePages = notePath.listFiles(new FileFilter() {
					
					@Override
					public boolean accept(File pathname) {
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
				
				int i = 0;
				for (File notePage : notePages) {
					// check for background if there is one draw it
					// else draw a white background
					String notePageName = notePage.getName();
					notePageName = notePageName.substring(0, notePageName.lastIndexOf("."));
					String bgPath = notePath + "/background_" + notePageName + ".jpg";
					Log.e("CACHE", bgPath);
					note = BitmapFactory.decodeFile(notePage.toString());
					
					// create those here, so they can be recycled
					// for every loop run
					draw = Bitmap.createBitmap(
							Deepnotes.getViewportWidth(), 
							Deepnotes.getViewportHeight(), 
							Bitmap.Config.ARGB_4444);
					canvas = new Canvas(draw);
					
					if (new File(bgPath).exists()) {
						Bitmap temp = BitmapFactory.decodeFile(bgPath);
						canvas.drawBitmap(temp, 0f, 0f, null);
					} else {
						canvas.drawColor(Color.WHITE);
					}
					
					canvas.drawBitmap(note, 0f, 0f, null);
					
					// write to external storage, because other applications
					// cannot access internal storage of another application
					if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
						Log.e("WRITE", "cache write");
						File outPath = new File(Environment.getExternalStorageDirectory()
								+ Deepnotes.SAVE_CACHE);
						
						outPath.mkdirs();
						
						String outFile = outPath.toString() + "/" + noteName + "_" + i++ + ".jpg";

						writeFile(draw, outFile, Bitmap.CompressFormat.JPEG, 70);

						outPath = new File(outFile);
						if (outPath.exists()) {
							uris.add(Uri.fromFile(outPath));
						}
					} else {
						Toast.makeText(activity.getApplicationContext(), "External Storage not mounted", Toast.LENGTH_SHORT).show();
					}
				}
				
				// recycle
				if (note != null) {
					note.recycle();
					note = null;
				}
				
				if (draw != null) {
					draw.recycle();
					draw = null;
				}
				
				canvas = null;
			}
			
			return null;
		}
		
		private void checkCache() {
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				File cachePath = new File(Environment.getExternalStorageDirectory() + Deepnotes.SAVE_CACHE);
				if (cachePath.exists()) {
					File[] cacheFiles = cachePath.listFiles();
					
					long size = 0;
					for (File cacheFile : cacheFiles) {
						size += cacheFile.length();
						
						// 2MB 2097152
						if (size > 2097152) {
							clearCache();
							return;
						}
					}
				}
			}
		}
		
		@Override
		protected void onPreExecute() {
			dialog.setMessage("prepare yourself");
			dialog.show();
			super.onPreExecute();
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
			
			super.onPostExecute(result);
			
			final Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
			intent.setType("image/*");
			intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			activity.startActivityForResult(intent, Deepnotes.REQUEST_SHARE_NOTE);
		}
		
	}
	
	public static void clearCache() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			Log.e("CLEAR", "cache clear");
			File cachePath = new File(Environment.getExternalStorageDirectory() + Deepnotes.SAVE_CACHE);
			if (cachePath.exists()) {
				File[] cachedImages = cachePath.listFiles();
				
				for (File cachedImage : cachedImages) {
					cachedImage.delete();
				}
				
				cachePath.delete();
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
	public static void writeFile(Bitmap bitmap, String file,
			Bitmap.CompressFormat format, int quality) {
		FileOutputStream fos = null;

		try {
			fos = new FileOutputStream(file);
			bitmap.compress(format, quality, fos);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
