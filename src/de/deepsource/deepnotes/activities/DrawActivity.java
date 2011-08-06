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
import de.deepsource.deepnotes.views.DrawView;

/**
 * @author Sebastian Ullrich (sebastian.ullrich@deepsource.de)
 * @author Jan Pretzel (jan.pretzel@deepsource.de)
 * 
 *         This activity enables the draw view and
 */
public class DrawActivity extends Activity {

	/**
	 * Custom request codes to identify the assign result data to its origin.
	 */
	private static final int REQUEST_IMAGE_FROM_GALLERY = 0x00000001;
	private static final int REQUEST_IMAGE_FROM_CAMERA = 0x00000010;
	// private static final int REQUEST_IMAGE_SHARE = 0x00000011;
	private static final int REQUEST_IMAGE_CROP = 0x00000100;
	private Uri pictureUri;
	private DrawView currentDrawView;
	private ViewFlipper viewFlipper;

	/**
	 * Called when the activity is first created.
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.draw);

		// Setting up the View Flipper, adding Animations.
		viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
		// TODO: add image background
		
		//viewFlipper.setBackgroundColor(Color.DKGRAY);
		
		currentDrawView = initNewDrawView();
		viewFlipper.addView(currentDrawView);

		// add some more DrawViews,
		viewFlipper.addView(initNewDrawView());
		viewFlipper.addView(initNewDrawView());
		viewFlipper.addView(initNewDrawView());

		/*
		 * Bundle bundle = getIntent().getExtras(); if (bundle != null) { String
		 * fileName = bundle.getString("draw"); Bitmap bitmap =
		 * BitmapFactory.decodeFile(Environment .getExternalStorageDirectory() +
		 * Deepnotes.saveFolder + fileName);
		 * 
		 * if (bitmap != null) { drawView.setBitmap(bitmap); } }
		 */

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
	 * Adding the menu.
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.draw_menu, menu);

		return true;
	}

	/**
	 * Customizing the menu.
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
//			saveNote(String.valueOf(System.currentTimeMillis()));
			new SaveNote(this).execute(String.valueOf(System.currentTimeMillis()));
			return true;
		}

		// Gallery import triggered
		case (R.id.draw_menu_importfromgallery): {

			Intent intent = new Intent();
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			startActivityForResult(
					Intent.createChooser(intent, "Bild auswählen"),
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
	 * initiates a new draw view
	 * 
	 * @return new draw view
	 */
	private DrawView initNewDrawView() {
		DrawView drawView = new DrawView(this);
		drawView.setOnTouchListener(new DrawTouchListener(this, drawView));
		drawView.setBackgroundColor(Color.WHITE);
		return drawView;
	}

	/**
	 * Shows the next DrawView
	 */
	public void showNextDrawView(){
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slideouttoleft));
		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.slideinfromright));
		if(!viewFlipper.isFlipping())
			viewFlipper.showNext();
	}

	/**
	 * Shows the previous DrawView
	 */
	public void showPreviousDrawView(){
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slideouttoright));
		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.slideinfromleft));
		if(!viewFlipper.isFlipping())
			viewFlipper.showPrevious();
	}

	/**
	 * This method intents image cropping.
	 * 
	 * @param data
	 *            Uri of imageresource
	 */
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
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.i("ACTIVITY RESULT", "we have a result: " + requestCode + ", "
				+ resultCode);

		// Import from gallery result
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

		// Import from camera result
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

		// crop result
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
	 * customizing the key listener
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// Creating the save dialog.
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

		// Left Arrow key (Emulator)
		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			showPreviousDrawView();
		}

		// Right Arrow key (Emulator)
		if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			showNextDrawView();
		}

		return super.onKeyDown(keyCode, event);
	}
	
	private class SaveNote extends AsyncTask<String, Void, Void> {
		
		private ProgressDialog dialog;
		
		public SaveNote(Activity activity) {
			dialog = new ProgressDialog(activity);
		}

		@Override
		protected Void doInBackground(String... params) {
			String fileName = params[0];
			
			// save thumbnail
			String savePath = getFilesDir() + Deepnotes.saveThumbnail;
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
				savePath = getFilesDir() + fileName;
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
					FileOutputStream fos = new FileOutputStream(savePath + i
							+ "_background.png");
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
		
		@Override
		protected void onPreExecute() {
			// TODO: add localized string
			dialog.setMessage("Saving...");
			dialog.show();
			super.onPreExecute();
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
			super.onPostExecute(result);
		}
		
		/**
		 * Calculates a thumbnail representing the note. The first page of the note
		 * (empty or not) will be scaled by 0.5.
		 * 
		 * @return The thumbnail as Bitmap.
		 */
		private Bitmap createThumbnail() {
			// get first page of the note
			Bitmap firstPage = ((DrawView) viewFlipper.getChildAt(0)).getBitmap();
			Bitmap firstBackground = ((DrawView) viewFlipper.getChildAt(0))
					.getBackgroundBitmap();

			// scale factor = 0.5
			float scale = 0.5f;

			// create matrix
			Matrix matirx = new Matrix();
			matirx.postScale(scale, scale);

			// create scaled bitmaps
			Bitmap firstPageScaled = Bitmap.createBitmap(firstPage, 0, 0,
					firstPage.getWidth(), firstPage.getHeight(), matirx, true);
			Bitmap firstBackgroundScaled = Bitmap.createBitmap(firstBackground, 0,
					0, firstBackground.getWidth(), firstBackground.getHeight(),
					matirx, true);

			// combine both bitmaps
			Canvas pageAndBackground = new Canvas(firstBackgroundScaled);
			pageAndBackground.drawBitmap(firstPageScaled, 0f, 0f, null);

			return firstBackgroundScaled;
		}
		
	}

}
