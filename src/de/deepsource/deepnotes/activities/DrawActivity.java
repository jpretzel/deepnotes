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
import de.deepsource.deepnotes.dialogs.ColorPickerDialog;
import de.deepsource.deepnotes.utilities.IOManager;
import de.deepsource.deepnotes.views.DrawView;

/**
 * @author Sebastian Ullrich (sebastian.ullrich@deepsource.de)
 * @author Jan Pretzel (jan.pretzel@deepsource.de)
 * 
 *         This activity enables the draw view and
 */
public class DrawActivity extends Activity implements ColorPickerDialog.OnColorChangedListener {

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

	protected boolean saveStateChanged = false;
	
	/**
	 * Only use this WeakReference when you need the Activity itself as context.
	 * Otherwise chances are great chance the Activity's context is getting leaked!
	 * 
	 * @author Jan Pretzel
	 */
	private WeakReference<DrawActivity> drawActivity = new WeakReference<DrawActivity>(this);

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

		currentDrawView = initNewDrawView(0);

		// set the default paint color
		setCurrentPaint(Color.BLACK);

		viewFlipper.addView(currentDrawView);

		// add some more DrawViews,
		viewFlipper.addView(initNewDrawView(1));
		viewFlipper.addView(initNewDrawView(2));

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
	 * only be called, when a saved note was opened and not when a new note 
	 * was just created.
	 * 
	 * @author Jan Pretzel
	 */
	public void loadNotePages() {
		File notePath = new File(getFilesDir(), fileName + "/");

		if (notePath.exists()) {
			File[] files = notePath.listFiles();
			WeakReference<Bitmap> weakBitmap = null;

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
					weakBitmap = new WeakReference<Bitmap>(BitmapFactory.decodeFile(file
							.getAbsolutePath()));
					
					DrawView loadView = (DrawView) viewFlipper
							.getChildAt(index);

					// is the file a background or not?
					if (name.contains("background")) {
						loadView.setBackground(weakBitmap.get());
					} else {
						loadView.loadBitmap(weakBitmap.get());
					}
				}
			}
		}
	}
	
	/**
	 * Reloads a given note page of the note.
	 * The background will not be reloaded!
	 * 
	 * @author Jan Pretzel
	 * 
	 * @param index The index of the page that will be reloaded.
	 */
	public void reloadNotePage(int index) {
		File notePath = new File(getFilesDir(), fileName + "/");

		if (notePath.exists()) {
			String notePathString = notePath.toString();
			//DrawView dw = (DrawView) viewFlipper.getChildAt(index);
			DrawView dw = currentDrawView;
			
			File noteFile = new File(notePathString + "/" + index + ".png");
			if (noteFile.exists()) {
				// TODO: weakref?
				Bitmap note = BitmapFactory.decodeFile(
						notePathString + "/"
						+ index + ".png");
				dw.loadBitmap(note);
			}
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

		// Save triggered
		case (R.id.draw_menu_save): {
			saveNote(false);
			return true;
		}

		// Gallery import triggered
		case (R.id.draw_menu_importfromgallery): {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("image/*");
			startActivityForResult(
					Intent.createChooser(intent, "Bild ausw�hlen"),
					REQUEST_IMAGE_FROM_GALLERY);

			return true;
		}

		// Camera import triggered
		case (R.id.draw_menu_importfromcamera): {
			if (Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)) {
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				
				File file = new File(Environment.getExternalStorageDirectory() + Deepnotes.SAVE_CACHE);
				file.mkdirs();
				
				pictureUri = Uri.fromFile(new File(file, "/camera.jpg"));
				intent.putExtra(MediaStore.EXTRA_OUTPUT, pictureUri);
				intent.putExtra("return-data", true);
				startActivityForResult(intent, REQUEST_IMAGE_FROM_CAMERA);
			} else {
				Toast.makeText(this.getApplicationContext(),
						"External Storage not mounted", Toast.LENGTH_SHORT).show();
			}

			return true;
		}

		// share triggered
		case (R.id.draw_menu_share): {
			final Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
			intent.setType("image/*");
			startActivity(intent);
			return true;
		}

		// Black Color Picked
		case (R.id.draw_menu_colorblack): {
			setCurrentColor(Deepnotes.BLACK);
			return true;
		}
		
		// White Color Picked
		case (R.id.draw_menu_colorwhite): {
			setCurrentColor(Color.WHITE);
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
		
		// Custom Color Picked
		case (R.id.draw_menu_colorcustom): {
			new ColorPickerDialog(this, this, getCurrentColor()).show();
			return true;
		}
		
		// Pen Width Thick Picked
		case (R.id.draw_menu_penwidththick): {
			currentDrawView.setPenWidth(Deepnotes.PEN_WIDTH_THICK);
			return true;
		}
		
		// Pen Width Normal Picked
		case (R.id.draw_menu_penwidthnormal): {
			currentDrawView.setPenWidth(Deepnotes.PEN_WIDTH_NORMAL);
			return true;
		}
		
		// Pen Width Thin Picked
		case (R.id.draw_menu_penwidththin): {
			currentDrawView.setPenWidth(Deepnotes.PEN_WIDTH_THIN);
			return true;
		}
		
		// Pen Width Thin Picked
		case (R.id.draw_menu_undo): {
			currentDrawView.undo();
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
	private DrawView initNewDrawView(int page) {
		DrawView drawView = new DrawView(drawActivity.get());
		drawView.setPage(page);

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

		new SaveNote(drawActivity.get(), finish).execute();

		
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
		final Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setType("image/*");
		
		List<ResolveInfo> resolveInfo = getPackageManager().queryIntentActivities(intent, 0);
		
		Log.e("CROP", String.valueOf(resolveInfo.size()));
		
		if (resolveInfo.size() == 0) {
			Toast.makeText(this.getApplicationContext(),
					"No crop application found.", Toast.LENGTH_SHORT).show();

			// TODO: crop without user input

			return;
		}
		
		// force the intent to take crop activity (won't 
		// work from camera activity without this!)
		ResolveInfo res = resolveInfo.get(0);
		intent.setComponent(new ComponentName(
				res.activityInfo.packageName, 
				res.activityInfo.name));

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			File file = new File(Environment.getExternalStorageDirectory()
					+ Deepnotes.SAVE_CACHE);
			
			file.mkdirs();
			file = new File(file, "crop.jpg");
			
			outputUri = Uri.fromFile(file);

			intent.setData(pictureUri);
			int x = Deepnotes.getViewportWidth();
			int y = Deepnotes.getViewportHeight();
			intent.putExtra("outputX", x);
			intent.putExtra("outputY", y);
			intent.putExtra("aspectX", x);
			intent.putExtra("aspectY", y);
			intent.putExtra("scale", true);
			intent.putExtra("return-data", false);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);

			startActivityForResult(intent, REQUEST_IMAGE_CROP);
		} else {
			Toast.makeText(this.getApplicationContext(),
					"External Storage not mounted", Toast.LENGTH_SHORT).show();
		}
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
				WeakReference<Bitmap> weakBitmap = null;
				try {
					weakBitmap = new WeakReference<Bitmap>(MediaStore.Images.Media.getBitmap(
							getContentResolver(), outputUri));
					currentDrawView.setBackground(weakBitmap.get());
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				
			}
			
			// delete temporary created files
			// the file behind outputUri is always created 
			// and needs to be deleted
			if (outputUri != null) {
				new File(outputUri.getPath()).delete();
			}
			
			// if we came from the camera activity we need
			// to delete pictureUri too
			if (pictureUri != null) {
				String cameraImg = pictureUri.getPath();
				if (cameraImg.contains("camera.jpg")) {
					new File(cameraImg).delete();
				}
			}
			
			break;
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
		// TODO: SWITCH!!!!
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
	 * If a low memory situation occures, this will free memory
	 * by clearing the undo cache.
	 * 
	 * @author Sebastian Ullrich
	 */
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		
		
		
		// clear the undo cache of all DrawViews
		int l = viewFlipper.getChildCount();
		for (int i = 0; i < l; i++){
			DrawView dw = (DrawView) viewFlipper.getChildAt(i);
			
			if(dw != null)
				dw.clearUndoCache();
		}
	}

	/**
	 * An AsyncTask to save the current note, it's backgrounds and a thumbnail.
	 * While working this task will show a ProgressDialog telling the user that
	 * it is saving at the moment.
	 * 
	 * @author Jan Pretzel (jan.pretzel@deepsource.de)
	 */
	private static class SaveNote extends AsyncTask<Void, Void, Void> {

		private ProgressDialog dialog;
		private boolean finish;
		private DrawActivity activity;

		public SaveNote(DrawActivity activity, boolean finish) {
			dialog = new ProgressDialog(activity);
			this.finish = finish;
			this.activity = activity;
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
			String savePath = activity.getFilesDir() + Deepnotes.SAVE_THUMBNAIL;
			File file = new File(savePath);

			// Creates the directory named by this abstract pathname,
			// including any necessary but nonexistent parent directories.
			file.mkdirs();

			Bitmap bitmap = null;
			DrawView toSave = (DrawView) activity.viewFlipper.getChildAt(0);

			if (toSave.isModified() || toSave.isBGModified()) {
				Log.e("SAVE", "saving thumbnail");
				bitmap = createThumbnail();
				IOManager.writeFile(bitmap, savePath + activity.fileName + ".jpg",
						Bitmap.CompressFormat.JPEG, 70);
			}

			// save note pages with separate backgrounds
			savePath = activity.getFilesDir() + "/" + activity.fileName + "/";
			file = new File(savePath);

			for (int i = 0; i < activity.viewFlipper.getChildCount(); i++) {
				toSave = (DrawView) activity.viewFlipper.getChildAt(i);

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
				
				// bitmap gets recycled by every IOManager.writeFile(...)
//				if (bitmap != null) {
//					bitmap.recycle();
//					bitmap = null;
//				}
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
			dialog.setMessage(activity.getString(R.string.saving_note));
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
			
			// reset saveStateChanged
			activity.saveStateChanged = false;

			Toast toast = Toast.makeText(activity.getApplicationContext(), R.string.note_saved,
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
			DrawView drawView = (DrawView) activity.viewFlipper.getChildAt(0);
			WeakReference<Bitmap> firstPage =  new WeakReference<Bitmap>(drawView.getBitmap());

			// scale factor = 0.5
			float scale = 0.5f;

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

	@Override
	public void colorChanged(int color) {
		setCurrentColor(color);
	}

}
