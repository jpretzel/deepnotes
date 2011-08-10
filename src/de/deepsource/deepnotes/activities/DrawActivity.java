package de.deepsource.deepnotes.activities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;
import android.widget.Toast;
import android.widget.ViewFlipper;
import de.deepsource.deepnotes.R;
import de.deepsource.deepnotes.activities.listener.DrawTouchListener;
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
	 * Custom request code to identify the <i>image share event</i>.
	 * 
	 * @author Sebastian Ullrich
	 */
	@SuppressWarnings("unused")
	private static final int REQUEST_IMAGE_SHARE = 0x00000011;

	/**
	 * Custom request code to identify the <i>image crop</i>.
	 * 
	 * @author Sebastian Ullrich
	 */
	private static final int REQUEST_IMAGE_CROP = 0x00000100;

	private Uri pictureUri;
	private DrawView currentDrawView;
	private ViewFlipper viewFlipper;
	private String fileName;

	private Integer notePosition;
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
			notePosition = bundle.getInt(Deepnotes.SAVED_NOTE_POSITION);
			loadNotePages();
			// loadNotePage(0);
		}
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
					Bitmap bitmap = BitmapFactory.decodeFile(file
							.getAbsolutePath());
					DrawView loadView = (DrawView) viewFlipper
							.getChildAt(index);

					// is the file a background or not?
					if (name.contains("background")) {
						loadView.setBackground(bitmap);
					} else {
						loadView.loadBitmap(bitmap);
					}

					updateCurrentPaintColor();
				}
			}
		}
	}

	/*
	 * public void loadNotePage(int index) { File notePath = new
	 * File(getFilesDir(), fileName + "/");
	 * 
	 * if (notePath.exists()) { Bitmap note =
	 * BitmapFactory.decodeFile(notePath.toString() + String.valueOf(index) +
	 * ".png"); currentDrawView.setBitmap(note);
	 * 
	 * Bitmap background = BitmapFactory.decodeFile(notePath.toString() +
	 * "background_" + String.valueOf(index) + ".png");
	 * currentDrawView.setBackground(background); } }
	 */

	/**
	 * Adding the menu.
	 * 
	 * @author Sebastian Ullrich
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.draw_menu, menu);

		return true;
	}

	/**
	 * Customizing the menu.
	 * 
	 * @author Jan Pretzel
	 * @author Sebastian Ullrich
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {

		// New Note triggered
		case (R.id.draw_menu_newnote): {
			currentDrawView.clearNote();
			setSaveStateChanged(true);
			return true;
		}

		// Save triggered
		case (R.id.draw_menu_save): {
			saveNote();
			return true;
		}

		// delete triggered
		case (R.id.draw_menu_delete): {
			Intent resultIntent = new Intent();

			// only call if there is a note to delete
			if (fileName != null && IOManager.deleteNote(this, fileName)) {
				// tell the MainActivtiy that we deleted a note
				resultIntent.putExtra(Deepnotes.SAVED_NOTE_POSITION,
						notePosition);
				setResult(Deepnotes.SAVED_NOTE_DELETED, resultIntent);
			} else {
				setResult(Activity.RESULT_CANCELED);
			}

			finish();

			return true;
		}

		// Gallery import triggered
		case (R.id.draw_menu_importfromgallery): {
			Intent intent = new Intent();
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			startActivityForResult(
					Intent.createChooser(intent, "Bild auswŠhlen"),
					REQUEST_IMAGE_FROM_GALLERY);

			return true;
		}

		// Camera import triggered
		case (R.id.draw_menu_importfromcamera): {
			// Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			// String fileName =
			// getFullyQualifiedFileString(Deepnotes.saveFolder
			// + Deepnotes.savePhotos, ".jpg");
			// File file = new File(fileName);
			// pictureUri = Uri.fromFile(file);
			// intent.putExtra(MediaStore.EXTRA_OUTPUT, pictureUri);
			// startActivityForResult(intent, REQUEST_IMAGE_FROM_CAMERA);

			return true;
		}

		// share triggered
		case (R.id.draw_menu_share): {
			Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
			intent.setType("image/*");
			// TODO: Attach images! Example:
			// http://stackoverflow.com/questions/4552831/how-to-attach-multiple-files-to-email-client-in-android
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
		drawView.setOnTouchListener(new DrawTouchListener(this, drawView));
		drawView.setBackgroundColor(Color.GRAY);
		return drawView;
	}

	/**
	 * 
	 */
	private void saveNote() {
		if (!isSaveStateChanged()) {
			finish();
			return;
		}

		// do we have a new note?
		if (fileName == null) {
			fileName = String.valueOf(System.currentTimeMillis());
		}

		new SaveNote(this).execute(fileName);
	}

	/**
	 * Shows the next DrawView by triggering an animated page turn.
	 * 
	 * @author Sebastian Ullrich
	 */
	public void showNextDrawView() {
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(this,
				R.anim.slideouttoleft));
		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(this,
				R.anim.slideinfromright));
		if (!viewFlipper.isFlipping())
			viewFlipper.showNext();

		updateCurrentPaintColor();
	}

	/**
	 * Shows the previous DrawView by triggering an animated page turn.
	 * 
	 * @author Sebastian Ullrich
	 */
	public void showPreviousDrawView() {
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(this,
				R.anim.slideouttoright));
		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(this,
				R.anim.slideinfromleft));
		if (!viewFlipper.isFlipping())
			viewFlipper.showPrevious();

		updateCurrentPaintColor();
	}

	/**
	 * 
	 */
	private void updateCurrentPaintColor() {
		currentDrawView = (DrawView) viewFlipper.getChildAt(viewFlipper
				.getDisplayedChild());
		currentDrawView.setPaintColor(getCurrentColor());
	}

	/**
	 * This method intents image cropping.
	 * 
	 * @param data
	 *            Uri of imageresource
	 * 
	 * @author Sebastian Ullrich
	 */
	@SuppressWarnings("unused")
	private void cropImage(Uri data) {
		Intent intent = new Intent("com.android.camera.action.CROP");

		intent.setData(data);
		intent.putExtra("outputX", 240);
		intent.putExtra("outputY", 100);
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("scale", true);
		intent.putExtra("noFaceDetection", true);
		intent.putExtra("return-data", true);
		intent.putExtra("output", data);

		startActivityForResult(intent, REQUEST_IMAGE_CROP);
	}

	/**
	 * Called whenever an Intent from this activity is finished.
	 * 
	 * @author Sebastian Ullrich
	 * @author Jan Pretzel
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.i("ACTIVITY RESULT", "we have a result: " + requestCode + ", "
				+ resultCode);

		/* Import from gallery result */
		if (requestCode == REQUEST_IMAGE_FROM_GALLERY)
			if (resultCode == RESULT_OK) {
				Uri imageUri = data.getData();
				try {
					currentDrawView.setBackground(MediaStore.Images.Media
							.getBitmap(this.getContentResolver(), imageUri));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				// cropImage(data.getData());
			}

		/* Import from camera result */
		if (requestCode == REQUEST_IMAGE_FROM_CAMERA)
			if (resultCode == RESULT_OK) {
				Bitmap bitmap = null;
				try {
					// TODO: add image do MediaStore
					sendBroadcast(new Intent(
							Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, pictureUri));
					bitmap = MediaStore.Images.Media.getBitmap(
							getContentResolver(), pictureUri);
					currentDrawView.setBackground(bitmap);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		/* crop result */
		if (requestCode == REQUEST_IMAGE_CROP)
			if (resultCode == RESULT_OK) {
				Uri imageUri = data.getData();
				try {
					currentDrawView.setBackground(MediaStore.Images.Media
							.getBitmap(this.getContentResolver(), imageUri));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
	}

	/**
	 * Customizing the key listener.
	 * 
	 * @author Sebastian Ullrich
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			/* check for changes */
			if (isSaveStateChanged()) {
				/* Creating the save dialog. */
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.save_dialog)
						.setCancelable(true)
						.setPositiveButton(R.string.yes,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										// call the save procedure.
										saveNote();

										/*
										 * save dialog will call finish()
										 */
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
	private class SaveNote extends AsyncTask<String, Void, Void> {

		private ProgressDialog dialog;
		private Activity activity;

		public SaveNote(Activity activity) {
			dialog = new ProgressDialog(activity);
			this.activity = activity;
		}

		/**
		 * The main part of the AsyncTask. Here The note and all it's associated
		 * parts will be saved to the file system.
		 * 
		 * @author Jan Pretzel
		 */
		@Override
		protected Void doInBackground(String... params) {
			// TODO: some checking for params count. or don't use params at all?
			String fileName = params[0];

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
					IOManager.writeFile(bitmap, savePath + "background_" + i + ".jpg",
							Bitmap.CompressFormat.JPEG, 70);
				}

				// check for delete status
				if (toSave.deleteStatus()) {
					File toDelete = new File(savePath + i + ".png");
					toDelete.delete();

					toDelete = new File(savePath + "background_" + i + ".jpg");
					toDelete.delete();
				}
			}

			// everything is saved, so recycle bitmap
			if (bitmap != null) {
				bitmap.recycle();
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
			
			// TODO: add localized string
			Toast toast = Toast.makeText(activity, "SAVED!!! YEAH!", Toast.LENGTH_SHORT);
			toast.show();

			// tell the MainActivtiy that we saved a note
			Intent resultIntent = new Intent();
			resultIntent.putExtra(Deepnotes.SAVED_NOTE_NAME, fileName);

			// if we have a modified note MainActivity needs to know
			if (notePosition != null) {
				resultIntent.putExtra(Deepnotes.SAVED_NOTE_POSITION,
						notePosition);
				activity.setResult(Deepnotes.SAVED_NOTE_MODIFIED, resultIntent);
			} else {
				activity.setResult(Activity.RESULT_OK, resultIntent);
			}

			activity.finish();
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
			Bitmap firstPage = ((DrawView) viewFlipper.getChildAt(0))
					.getBitmap();
			Bitmap firstBackground = ((DrawView) viewFlipper.getChildAt(0))
					.getBackgroundBitmap();
			;

			// scale factor = 0.5
			float scale = 0.5f;

			// create matrix
			Matrix matirx = new Matrix();
			matirx.postScale(scale, scale);

			int width = firstPage.getWidth();
			int height = firstPage.getHeight();

			// create scaled bitmaps
			Bitmap firstPageScaled = Bitmap.createBitmap(firstPage, 0, 0,
					width, height, matirx, true);
			Bitmap firstBackgroundScaled = Bitmap.createBitmap(firstBackground,
					0, 0, width, height, matirx, true);

			// combine both bitmaps
			Canvas pageAndBackground = new Canvas(firstBackgroundScaled);
			pageAndBackground.drawBitmap(firstPageScaled, 0f, 0f, null);

			// free unused Bitmap (note: the other bitmaps share
			// some stuff with the returned Bitmap so don't recycle those
			firstPageScaled.recycle();

			return firstBackgroundScaled;
		}

	}

	/**
	 * @return the saveStateChanged
	 */
	public boolean isSaveStateChanged() {
		return saveStateChanged;
	}

	/**
	 * @param saveStateChanged
	 *            the saveStateChanged to set
	 */
	public void setSaveStateChanged(boolean saveStateChanged) {
		this.saveStateChanged = saveStateChanged;
	}

}
