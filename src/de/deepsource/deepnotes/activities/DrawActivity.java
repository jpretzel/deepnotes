package de.deepsource.deepnotes.activities;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import de.deepsource.deepnotes.R;
import de.deepsource.deepnotes.views.DrawView;

public class DrawActivity extends Activity {

	private static int REQUEST_IMAGE_FROM_GALLERY = 0x00000001;
	private static int REQUEST_IMAGE_FROM_CAMERA = 0x00000010;

	private DrawView drawView;

	private OnTouchListener drawView_otl = new View.OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			drawView.addPoint(event.getX(), event.getY());
			return true;
		}
	};

	/**
	 * Called when the activity is first created.
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		drawView = new DrawView(this);
		drawView.setBackgroundColor(Color.WHITE);
		drawView.setOnTouchListener(drawView_otl);
		setContentView(drawView);
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
			drawView.clearNote();
			return true;
		}

			// Save triggered
		case (R.id.draw_menu_save): {
			saveNote(drawView.getBitmap());
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

		}
		return false;
	}

	// To handle when an image is selected from the browser, add the following
	// to your Activity
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i("ACTIVITY RESULT", "we have a result: " + requestCode + ", "
				+ resultCode);
		if (requestCode == REQUEST_IMAGE_FROM_GALLERY)
			if (resultCode == RESULT_OK) {
				Uri imageUri = data.getData();
				try {
					drawView.setBackground(MediaStore.Images.Media.getBitmap(
							this.getContentResolver(), imageUri));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		if (requestCode == REQUEST_IMAGE_FROM_CAMERA)
			if (resultCode == RESULT_OK) {
				// magic...
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
		return super.onKeyDown(keyCode, event);
	}

	/**
	 *TODO comment
	 * @param bitmap
	 * @return
	 */
	public boolean saveNote(Bitmap bitmap) {
		ContentValues values = new ContentValues(4);
		values.put(Images.Media.TITLE, "test");
		values.put(Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
		values.put(Images.Media.MIME_TYPE, "image/png");
		values.put(Images.Media.DATA, Environment.getExternalStorageDirectory() + "/deepnotes/test.png");
		
		ContentResolver resolver = getContentResolver();
		Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				values);

		try {
			OutputStream outStream = resolver.openOutputStream(uri);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
			outStream.close();
		} catch (Exception e) {
			return false;
		}

		sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));

		return true;
	}
}
