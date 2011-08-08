package de.deepsource.deepnotes.activities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
		// TODO: add image background

		// viewFlipper.setBackgroundColor(Color.DKGRAY);

		currentDrawView = initNewDrawView();
		viewFlipper.addView(currentDrawView);

		// add some more DrawViews,
		viewFlipper.addView(initNewDrawView());
		viewFlipper.addView(initNewDrawView());
		viewFlipper.addView(initNewDrawView());

		// load note if one was opened
		if (getIntent().hasExtra("load")) {
			Bundle bundle = getIntent().getExtras();
			fileName = bundle.getString("load");
			loadNotePages();
		}

		/*
		 * / testy ViewFlipper vf = (ViewFlipper)
		 * findViewById(R.id.viewFlipper); vf.setBackgroundColor(Color.WHITE);
		 * vf.setOutAnimation(AnimationUtils.loadAnimation(this,
		 * android.R.anim.slide_out_right));
		 * vf.setInAnimation(AnimationUtils.loadAnimation(this,
		 * android.R.anim.slide_in_left));
		 */
	}

	/**
	 * Loads an opened note with all it's saved pages and Backgrounds.
	 * This will only be called, when the note is not new and was just created.
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
				if (index < 4) {
					// load the file as Bitmap
					Bitmap bitmap = BitmapFactory.decodeFile(file
							.getAbsolutePath());
					DrawView loadView = (DrawView) viewFlipper
							.getChildAt(index);

					// is the file a background or not?
					if (name.contains("background")) {
						loadView.setBackground(bitmap);
					} else {
						loadView.setBitmap(bitmap);
					}
				}
			}
		}
	}

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
			return true;
		}

		// Save triggered
		case (R.id.draw_menu_save): {
			// do we have a new note?
			if (fileName == null) {
				fileName = String.valueOf(System.currentTimeMillis());
			}
			
			new SaveNote(this).execute(fileName);
			
			return true;
		}
		
		// delete triggered
		case (R.id.draw_menu_delete): {
			// only call if the note was saved before
			if (fileName != null) {
				if (IOManager.deleteNote(this, fileName))
					Log.e("DELETE", "note deleted");
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
					Intent.createChooser(intent, "Bild ausw�hlen"),
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
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("image/*");
			// TODO: Attach images! Example:
			// http://stackoverflow.com/questions/4552831/how-to-attach-multiple-files-to-email-client-in-android
			startActivity(intent);
		}

		// Color Picked
		case (R.id.draw_menu_changecolor): {

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
		drawView.setBackgroundColor(Color.WHITE);
		return drawView;
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
			/* Creating the save dialog. */
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.save_dialog)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									finish();
								}
							})
					.setNegativeButton(R.string.no,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			builder.create().show();
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
	 * TODO: only save modified pages and backgrounds
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
		 * The main part of the AsyncTask. Here The note and all it's
		 * associated parts will be saved to the file system.
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

			Bitmap thumbnail = createThumbnail();

			try {
				FileOutputStream fos = new FileOutputStream(savePath + fileName
						+ ".png");
				thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, fos);
				fos.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// save note pages with separate backgrounds
			for (int i = 0; i < viewFlipper.getChildCount(); i++) {
				savePath = getFilesDir() + "/" + fileName + "/";
				file = new File(savePath);
				file.mkdirs();

				// save note
				DrawView toSave = (DrawView) viewFlipper.getChildAt(i);
				Bitmap note = toSave.getBitmap();

				try {
					FileOutputStream fos = new FileOutputStream(savePath + i
							+ ".png");
					note.compress(Bitmap.CompressFormat.PNG, 100, fos);
					fos.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				// save background
				Bitmap background = toSave.getBackgroundBitmap();

				try {
					FileOutputStream fos = new FileOutputStream(savePath
							+ "background_" + i + ".png");
					background.compress(Bitmap.CompressFormat.JPEG, 80, fos);
					fos.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
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
			// TODO: add localized string
			dialog.setMessage("Saving...");
			dialog.show();
			super.onPreExecute();
		}

		/**
		 * After execution ends, the ProgressDialog will be dismissed.
		 * 
		 * @author Jan Pretzel
		 */
		@Override
		protected void onPostExecute(Void result) {
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
			
			super.onPostExecute(result);
			
			// tell the MainActivtiy that we saved a note
			Intent resultIntent = new Intent();
			resultIntent.putExtra(Deepnotes.SAVED_NOTE_NAME, fileName);
			activity.setResult(Activity.RESULT_OK, resultIntent);
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

			return firstBackgroundScaled;
		}

	}

}
