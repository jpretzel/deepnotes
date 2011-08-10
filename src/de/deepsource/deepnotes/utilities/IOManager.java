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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
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
		
		// TODO: add localized String
		Toast toast = Toast.makeText(context, "DELTORRRRRRRD!", Toast.LENGTH_SHORT);
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
				
				int i = 0;
				for (File notePage : notePages) {
					Bitmap draw;
					
					// check for background
					String bgPath = notePath + "background_" + notePage.getName();
					if (new File(bgPath).exists()) {
						Bitmap note = BitmapFactory.decodeFile(notePage.toString());
						draw = BitmapFactory.decodeFile(bgPath);
						Canvas canvas = new Canvas(draw);
						canvas.drawBitmap(note, 0f, 0f, null);
					} else {
						draw = BitmapFactory.decodeFile(notePage.toString());
					}
					
					String outPath = Environment.getExternalStorageDirectory() + "/" + noteName + "_" + i++ + ".jpg";
					
					writeFile(draw, outPath, Bitmap.CompressFormat.JPEG, 70);
					
					File test = new File(outPath);
					if (test.exists()) {
						Uri u = Uri.fromFile(test);
						uris.add(u);
					}
				}
			}
			
			return null;
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
			activity.startActivity(intent);
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

		if (bitmap != null) {
			bitmap.recycle();
		}
	}
}
