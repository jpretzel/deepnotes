package de.deepsource.deepnotes.activities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.ViewFlipper;
import de.deepsource.deepnotes.R;
import de.deepsource.deepnotes.application.Deepnotes;
import de.deepsource.deepnotes.utilities.IOManager;
import de.deepsource.deepnotes.views.DrawView;

/**
 * @author Sebastian Ullrich (sebastian.ullrich@deepsource.de)
 * @author Jan Pretzel (jan.pretzel@deepsource.de)
 * 
 *         This activity enables the draw view and
 */
public class DrawActivity extends Activity {

	/**
	 * Custom request code to identify the <i>image pick from gallery</i>.
	 * 
	 * @author Sebastian Ullrich
	 */
	private static final int REQUEST_IMAGE_FROM_GALLERY = 0x00000001;

	/**
	 * Custom request code to identify the <i>camera image capture</i>.
	 * 
	 * @author Jan Pretzel
	 */
	private static final int REQUEST_IMAGE_FROM_CAMERA = 0x00000010;

	/**
	 * Custom request code to identify the <i>image crop</i>.
	 * 
	 * @author Sebastian Ullrich
	 */
	private static final int REQUEST_IMAGE_CROP = 0x00000100;

	private Uri pictureUri;
	private Uri outputUri;
	private DrawView currentDrawView;
	protected ViewFlipper viewFlipper;
	protected String fileName;

	protected Integer notePosition;
	private int currentPaint;

	/* current picked color */
	private int currentColor = Deepnotes.BLACK;

	private boolean saveStateChanged = false;

	/**
	 * Called when the activity is first created.
	 * 
	 * @author Sebastian Ullrich
	 * @author Jan Pretzel
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.draw);

		// Setting up the View Flipper, adding Animations.
		viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);

		currentDrawView = initNewDrawView();

		// set the default paint color
		setCurrentPaint(Deepnotes.BLACK);

		viewFlipper.addView(currentDrawView);

		// add some more DrawViews,
		viewFlipper.addView(initNewDrawView());
		viewFlipper.addView(initNewDrawView());

		// load note if one was opened
		if (getIntent().hasExtra(Deepnotes.SAVED_NOTE_NAME)) {
			Bundle bundle = getIntent().getExtras();
			fileName = bundle.getString(Deepnotes.SAVED_NOTE_NAME);
			loadNotePages();
		}

		Log.e("INIT DRAW",
				String.valueOf(android.os.Debug.getNativeHeapAllocatedSize()));
	}

	/**
	 * @return the currentPaint
	 */
	public int getCurrentPaint() {
		return currentPaint;
	}

	/**
	 * @param currentPaint
	 *            the currentPaint to set
	 */
	public void setCurrentPaint(int currentPaint) {
		this.currentPaint = currentPaint;
	}

	/**
	 * @return the currentColor
	 */
	public int getCurrentColor() {
		return currentColor;
	}

	/**
	 * @param currentColor
	 *            the currentColor to set
	 */
	public void setCurrentColor(int currentColor) {
		this.currentColor = currentColor;
		currentDrawView.setPaintColor(currentColor);
	}

	/**
	 * Loads an opened note with all it's saved pages and Backgrounds. This will
	 * only be called, when the note is not new and was just created.
	 * 
	 * @author Jan Pretzel
	 */
	public void loadNotePages() {
		File notePath = new File(getFilesDir(), fileName + "/");

		if (notePath.exists()) {
			File[] files = notePath.listFiles();
			Bitmap weakBitmap = null;

			for (File file : files) {
				String name = file.getName();
				int index = 0;
				try {
					index = Integer.parseInt(String.valueOf(name.charAt(name
							.lastIndexOf('.') - 1)));
				} catch (NumberFormatException e) {
					// don't run false files
					continue;
				}

				// don't run false files
				if (index < 3) {
					// load the file as Bitmap
					weakBitmap = BitmapFactory.decodeFile(file
							.getAbsolutePath());
					
					DrawView loadView = (DrawView) viewFlipper
							.getChildAt(index);

					// is the file a background or not?
					if (name.contains("background")) {
						loadView.setBackground(weakBitmap);
					} else {
						loadView.loadBitmap(weakBitmap);
					}
				}
			}
			
			/*if (bitmap != null) {
				bitmap.recycle();
				bitmap = null;
			}*/
		}
	}

	/**
	 * Adding the menu.
	 * 
	 * @author Sebastian Ullrich
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = new MenuInflater(this.getApplicationContext());
		inflater.inflate(R.menu.draw_menu, menu);

		return true;
	}

	/**
	 * Customizing the menu.
	 * 
	 * @author Jan Pretzel
	 * @author Sebastian Ullrich
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {

		// New Note triggered
		case (R.id.draw_menu_newnote): {
			currentDrawView.clearNote();
			saveStateChanged = true;
			return true;
		}

		// Save triggered
		case (R.id.draw_menu_save): {
			saveNote(false);
			return true;
		}

		// delete triggered
		case (R.id.draw_menu_delete): {
			IOManager.deleteNote(this.getApplicationContext(), fileName);
			finish();

			return true;
		}

		// Gallery import triggered
		case (R.id.draw_menu_importfromgallery): {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("image/*");
			startActivityForResult(
					Intent.createChooser(intent, "Bild auswŠhlen"),
					REQUEST_IMAGE_FROM_GALLERY);

			return true;
		}

		// Camera import triggered
		case (R.id.draw_menu_importfromcamera): {
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			// String fileName =
			// getFullyQualifiedFileString(Deepnotes.saveFolder
			// + Deepnotes.savePhotos, ".jpg");
			// File file = new File(fileName);
			pictureUri = Uri.fromFile(new File(Environment
					.getExternalStorageDirectory() + "/testcam.jpg"));
			intent.putExtra(MediaStore.EXTRA_OUTPUT, pictureUri);
			intent.putExtra("return-data", true);
			startActivityForResult(intent, REQUEST_IMAGE_FROM_CAMERA);

			return true;
		}

		// share triggered
		case (R.id.draw_menu_share): {
			Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
			intent.setType("image/*");
			startActivity(intent);
			return true;
		}

		// Black Color Picked
		case (R.id.draw_menu_colorblack): {
			setCurrentColor(Deepnotes.BLACK);
			return true;
		}

		// Red Color Picked
		case (R.id.draw_menu_colorred): {
			setCurrentColor(Deepnotes.RED);
			return true;
		}

		// Yellow Color Picked
		case (R.id.draw_menu_coloryellow): {
			setCurrentColor(Deepnotes.YELLOW);
			return true;
		}

		}
		return false;
	}

	/**
	 * Initiates a new DrawView
	 * 
	 * @return new DrawView
	 * @author Sebastian Ullrich
	 */
	private DrawView initNewDrawView() {
		DrawView drawView = new DrawView(this);
		drawView.setBackgroundColor(Color.GRAY);
		return drawView;
	}

	/**
	 * 
	 */
	private void saveNote(boolean finish) {
		if (!saveStateChanged) {
			return;
		}

		// do we have a new note?
		if (fileName == null) {
			fileName = String.valueOf(System.currentTimeMillis());
		}

		new SaveNote(this, finish).execute();
	}

	/**
	 * Shows the next DrawView by triggering an animated page turn.
	 * 
	 * @author Sebastian Ullrich
	 */
	public void showNextDrawView() {
		showPageToast(true);
		
		viewFlipper.showNext();
		currentDrawView = (DrawView) viewFlipper.getCurrentView();
	}

	/**
	 * Shows the previous DrawView by triggering an animated page turn.
	 * 
	 * @author Sebastian Ullrich
	 */
	public void showPreviousDrawView() {
		showPageToast(false);

		viewFlipper.showPrevious();
		currentDrawView = (DrawView) viewFlipper.getCurrentView();
	}

	private void showPageToast(boolean next) {
		int currentPage = viewFlipper.getDisplayedChild() + 1;
		int size = viewFlipper.getChildCount();

		String msg = new String();

		if (next) {
			// show next page
			if (currentPage == size)
				msg = "1";
			else
				msg = String.valueOf(currentPage + 1);
		} else {
			if (currentPage == 1)
				msg = String.valueOf(size);
			else
				msg = String.valueOf(currentPage - 1);
		}

		Toast toast = Toast.makeText(this.getApplicationContext(), msg, Toast.LENGTH_SHORT);
		toast.show();
	}

	/**
	 * This method intents image cropping.
	 * 
	 * @author Jan Pretzel
	 * @author Sebastian Ullrich
	 * 
	 * @param data
	 *            Uri of imageresource
	 * 
	 */
	private void cropImage() {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setType("image/*");
		
		List<ResolveInfo> resolveInfo = getPackageManager().queryIntentActivities(intent, 0);
		
		Log.e("CROP", String.valueOf(resolveInfo.size()));
		
		if (resolveInfo.size() == 0) {
			Toast.makeText(this.getApplicationContext(), "No crop application found.", Toast.LENGTH_SHORT).show();
			
			// TODO: crop without user input
			
			return;
		}
		
		// force the intent to take crop activity (won't 
		// work from camera activity without this!)
		ResolveInfo res = resolveInfo.get(0);
		intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));

		File file = new File(Environment.getExternalStorageDirectory()
				+ "/test.jpg");
		outputUri = Uri.fromFile(file);

		intent.setData(pictureUri);
		int x = Deepnotes.getViewportWidth();
		int y = Deepnotes.getViewportHeight();
		// intent.putExtra("setWallpaper", false);
		intent.putExtra("outputX", x);
		intent.putExtra("outputY", y);
		intent.putExtra("aspectX", x);
		intent.putExtra("aspectY", y);
		intent.putExtra("scale", true);
		// intent.putExtra("noFaceDetection", true);
		intent.putExtra("return-data", false);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);

		startActivityForResult(intent, REQUEST_IMAGE_CROP);
	}

	/**
	 * Called whenever an Intent from this activity is finished.
	 * 
	 * @author Sebastian Ullrich
	 * @author Jan Pretzel
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		/* Import from gallery result */
		case (REQUEST_IMAGE_FROM_GALLERY): {
			if (resultCode == RESULT_OK) {
				pictureUri = data.getData();
				cropImage();

				break;
			}
		}

		/* Import from camera result */
		case (REQUEST_IMAGE_FROM_CAMERA): {
			if (resultCode == RESULT_OK) {
				cropImage();

				break;
			}
		}

		/* crop result */
		case (REQUEST_IMAGE_CROP): {
			if (resultCode == RESULT_OK) {
				Bitmap bitmap = null;
				try {
					bitmap = MediaStore.Images.Media.getBitmap(
							getContentResolver(), outputUri);
					currentDrawView.setBackground(bitmap);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		}
	}

	/**
	 * Customizing the key listener.
	 * 
	 * @author Sebastian Ullrich
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			Log.e("recycle", String.valueOf(android.os.Debug.getNativeHeapAllocatedSize()));
			currentDrawView.undo();
		}
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			/* check for changes */
			if (saveStateChanged) {
				/* Creating the save dialog. */
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.save_dialog)
						.setCancelable(true)
						.setPositiveButton(R.string.yes,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										// call the save procedure.
										saveNote(true);
									}
								})
						.setNegativeButton(R.string.no,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										finish();
									}
								});
				builder.create().show();
			} else {
				finish();
			}
			return true;
		}

		/* Left Arrow key (Emulator / Hardkey Device) */
		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			showPreviousDrawView();
		}

		/* Right Arrow key (Emulator / Hardkey Device) */
		if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			showNextDrawView();
		}

		return super.onKeyDown(keyCode, event);
	}

	/**
	 * An AsyncTask to save the current note, it's backgrounds and a thumbnail.
	 * While working this task will show a ProgressDialog telling the user that
	 * it is saving at the moment.
	 * 
	 * @author Jan Pretzel (jan.pretzel@deepsource.de)
	 */
	private class SaveNote extends AsyncTask<Void, Void, Void> {

		private ProgressDialog dialog;
		private boolean finish;
		private Activity activity;

		public SaveNote(Activity activity, boolean finish) {
			dialog = new ProgressDialog(activity);
			this. activity = activity;
			this.finish = finish;
		}

		/**
		 * The main part of the AsyncTask. Here The note and all it's associated
		 * parts will be saved to the file system.
		 * 
		 * @author Jan Pretzel
		 */
		@Override
		protected Void doInBackground(Void... params) {

			// save thumbnail
			String savePath = getFilesDir() + Deepnotes.SAVE_THUMBNAIL;
			File file = new File(savePath);

			// Creates the directory named by this abstract pathname,
			// including any necessary but nonexistent parent directories.
			file.mkdirs();

			Bitmap bitmap = null;
			DrawView toSave = (DrawView) viewFlipper.getChildAt(0);

			if (toSave.isModified() || toSave.isBGModified()) {
				Log.e("SAVE", "saving thumbnail");
				bitmap = createThumbnail();
				IOManager.writeFile(bitmap, savePath + fileName + ".jpg",
						Bitmap.CompressFormat.JPEG, 70);
			}

			// save note pages with separate backgrounds
			savePath = getFilesDir() + "/" + fileName + "/";
			file = new File(savePath);

			for (int i = 0; i < viewFlipper.getChildCount(); i++) {
				toSave = (DrawView) viewFlipper.getChildAt(i);

				if (toSave.isModified()) {
					Log.e("SAVE", "saving note " + i);
					// first saved note page will create a sub folder
					file.mkdirs();

					// save note
					bitmap = toSave.getBitmap();
					IOManager.writeFile(bitmap, savePath + i + ".png",
							Bitmap.CompressFormat.PNG, 100);
				}

				if (toSave.isBGModified()) {
					Log.e("SAVE", "saving background " + i);
					// save background
					bitmap = toSave.getBackgroundBitmap();
					IOManager.writeFile(bitmap, savePath + "background_" + i
							+ ".jpg", Bitmap.CompressFormat.JPEG, 70);
				}

				// check for delete status
				if (toSave.deleteStatus()) {
					File toDelete = new File(savePath + i + ".png");
					toDelete.delete();

					toDelete = new File(savePath + "background_" + i + ".jpg");
					toDelete.delete();
				}
				
				// everything is saved, so recycle bitmap
				if (bitmap != null) {
					bitmap.recycle();
					bitmap = null;
				}
			}

			return null;
		}

		/**
		 * Before execution starts, the ProgressDialog will be shown.
		 * 
		 * @author Jan Pretzel
		 */
		@Override
		protected void onPreExecute() {
			dialog.setMessage(getString(R.string.saving_note));
			dialog.show();
			super.onPreExecute();
		}

		/**
		 * After execution ends, the ProgressDialog will be dismissed. Also we
		 * finish the activity and tell MainActivity what to do.
		 * 
		 * @author Jan Pretzel
		 */
		@Override
		protected void onPostExecute(Void result) {
			if (dialog.isShowing()) {
				dialog.dismiss();
			}

			super.onPostExecute(result);

			Toast toast = Toast.makeText(getApplicationContext(), R.string.note_saved,
					Toast.LENGTH_SHORT);
			toast.show();

			// need to finish in onPostExecute, else we leak the window
			if (finish) {
				activity.finish();
			}
		}

		/**
		 * Calculates a thumbnail representing the note. The first page of the
		 * note (empty or not) will be scaled by 0.5.
		 * 
		 * @author Jan Pretzel
		 * 
		 * @return The thumbnail as Bitmap.
		 */
		private Bitmap createThumbnail() {
			// get first page of the note
			DrawView drawView = (DrawView) viewFlipper.getChildAt(0);
			WeakReference<Bitmap> firstPage =  new WeakReference<Bitmap>(drawView.getBitmap());

			// scale factor = 0.5
			float scale = 0.1f;

			// create matrix
			Matrix matirx = new Matrix();
			matirx.postScale(scale, scale);

			int width = firstPage.get().getWidth();
			int height = firstPage.get().getHeight();

			// create scaled bitmaps
			Bitmap firstPageScaled = Bitmap.createBitmap(firstPage.get(), 0, 0,
					width, height, matirx, true);

			Canvas pageAndBackground;
			WeakReference<Bitmap> firstBackgroundScaled;

			if (drawView.isBGModified()) {
				WeakReference<Bitmap> firstBackground = new WeakReference<Bitmap>(drawView.getBackgroundBitmap());
				firstBackgroundScaled = new WeakReference<Bitmap>(Bitmap.createBitmap(firstBackground.get(), 0,
						0, width, height, matirx, true));
				pageAndBackground = new Canvas(firstBackgroundScaled.get());
			} else {
				firstBackgroundScaled = new WeakReference<Bitmap>(Bitmap.createBitmap(
						(int) (width * scale), (int) (height * scale),
						Bitmap.Config.ARGB_4444));
				pageAndBackground = new Canvas(firstBackgroundScaled.get());
				pageAndBackground.drawColor(Color.WHITE);
			}

			// combine both bitmaps
			pageAndBackground.drawBitmap(firstPageScaled, 0f, 0f, null);

			// free unused Bitmap (note: the other bitmaps share
			// some stuff with the returned Bitmap so don't recycle those
			firstPageScaled.recycle();
			firstPageScaled = null;
			pageAndBackground = null;

			return firstBackgroundScaled.get();
		}
	}

//	@Override
//	protected void onSaveInstanceState(Bundle outState) {
//		super.onSaveInstanceState(outState);
//
//		outState.putParcelable("0",
//				((DrawView) viewFlipper.getChildAt(0)).getBitmap());
//		outState.putParcelable("1",
//				((DrawView) viewFlipper.getChildAt(1)).getBitmap());
//		outState.putParcelable("2",
//				((DrawView) viewFlipper.getChildAt(2)).getBitmap());
//	}
//
//	@Override
//	protected void onRestoreInstanceState(Bundle savedInstanceState) {
//		super.onRestoreInstanceState(savedInstanceState);
//
//		((DrawView) viewFlipper.getChildAt(0))
//				.loadBitmap((Bitmap) savedInstanceState.getParcelable("0"));
//		((DrawView) viewFlipper.getChildAt(0))
//				.loadBitmap((Bitmap) savedInstanceState.getParcelable("0"));
//		((DrawView) viewFlipper.getChildAt(0))
//				.loadBitmap((Bitmap) savedInstanceState.getParcelable("0"));
//	}

	@Override
	protected void onDestroy() {
		// trying to free heap by force
		Log.i("DrawActivity", "onDestroy() called.");
		super.onDestroy();
//		viewFlipper.stopFlipping();
		
		// THIS IS IMPORTANT
		// without this the Activity won't get collected by the GC
		// and we then leak a lot of memory
		int count = viewFlipper.getChildCount();
		for (int i = 0; i < count; i++) {
			DrawView dw = (DrawView) viewFlipper.getChildAt(i);
			dw.recycle();
		}

//		// TODO: needed?
//		viewFlipper.removeAllViews();
//		viewFlipper = null;
//		currentDrawView = null;
	}

	/**
	 * @param saveStateChanged
	 *            the saveStateChanged to set
	 */
	public void setSaveStateChanged(boolean saveStateChanged) {
		this.saveStateChanged = saveStateChanged;
	}

}
