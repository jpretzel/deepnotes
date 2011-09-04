/*
 * Deepnotes - Note Application for Android
 *
 * Copyright (C) 2011 Sebastian Ullrich & Jan Pretzel
 * http://www.deepsource.de
 */

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
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
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
import de.deepsource.deepnotes.views.DrawView.DrawViewListener;

/**
 * This activity enables the draw view and forwards important events to
 * DrawView.
 *
 * @author Sebastian Ullrich (sebastian.ullrich@deepsource.de)
 * @author Jan Pretzel (jan.pretzel@deepsource.de)
 */
public class DrawActivity extends Activity implements
		ColorPickerDialog.OnColorChangedListener, DrawViewListener {

	/**
	 * Custom request code to identify the <i>image pick from gallery</i>.
	 */
	private static final int REQUEST_GALLERY = 0x00000001;

	/**
	 * Custom request code to identify the <i>camera image capture</i>.
	 */
	private static final int REQUEST_CAMERA = 0x00000010;

	/**
	 * Custom request code to identify the <i>image crop</i>.
	 */
	private static final int REQUEST_CROP = 0x00000100;

	/**
	 * When importing an image, the path of the original will be saved here.
	 */
	private transient Uri pictureUri;

	/**
	 * When importing and image, the cropped image will be saved here.
	 */
	private transient Uri outputUri;

	/**
	 * The currently displayed {@link DrawView}.
	 */
	private transient DrawView currentDrawView;

	/**
	 * This {@link ViewGroup} holds the {@link DrawView}-objects.
	 */
	private transient ViewFlipper viewFlipper;

	/**
	 * The name of the note used in the file system.
	 */
	private transient String fileName;

	/**
	 * Holds the current color and pen-width.
	 */
	private int currentPaint;

	/**
	 * Sets the startup color to standard Black.
	 */
	private int currentColor = Deepnotes.BLACK;

	/**
	 * Identifies whether the note must be saved because it changed or not.
	 */
	private boolean saveStateChanged = false;

	/**
	 * Only use this WeakReference when you need the Activity itself as context.
	 * Otherwise chances are great chance the Activity's context is getting
	 * leaked!
	 */
	private final WeakReference<DrawActivity> weakThis = new WeakReference<DrawActivity>(
			this);

	// Author: Jan Pretzel
	// Author: Sebastian Ullrich
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.draw);

		// be sure the drawing cache is cleared otherwise we may get stuff from
		// other notes
		clearDrawingCache();

		// get device width and height BEFORE initializing DrawViews
		final Display display = getWindowManager().getDefaultDisplay();
		Deepnotes.setViewportWidth(display.getWidth());
		Deepnotes.setViewportHeight(display.getHeight());

		// Setting up the View Flipper, adding Animations.
		viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);

		if (getIntent().hasExtra(Deepnotes.SAVED_NOTE_NAME)) {
			final Bundle bundle = getIntent().getExtras();
			fileName = bundle.getString(Deepnotes.SAVED_NOTE_NAME);
		}

		currentDrawView = new DrawView(weakThis.get(), this);
		viewFlipper.addView(currentDrawView);
		loadNotePage(currentDrawView, viewFlipper.getDisplayedChild());

		// set the default paint color
		setCurrentPaint(Color.BLACK);

		// add some more DrawViews,
		viewFlipper.addView(new DrawView(weakThis.get(), this));
		viewFlipper.addView(new DrawView(weakThis.get(), this));
	}

	/**
	 * Loads a specific note page, in other words it's background and foreground
	 * bitmaps. To do so it checks if there is a saved image for the foreground
	 * and loads it, otherwise an empty Bitmap is created. In both situations
	 * the saved Paths will be drawn to one of the two Bitmaps. For the
	 * background it first checks if there is something to load from cache, if
	 * not it checks if there is a saved file to load.
	 *
	 * @param drawView
	 *            The {@link DrawView} to load.
	 * @param position
	 *            The position in the {@link DrawActivity#viewFlipper}.
	 */
	// Author: Jan Pretzel
	private void loadNotePage(final DrawView drawView,
			final int position) {
		if (drawView == null) {
			Log.e(Deepnotes.APP_NAME, "drawView must not be null");
			throw new IllegalArgumentException();
		}

		if (position < 0 || position >= viewFlipper.getChildCount()) {
			Log.e(Deepnotes.APP_NAME, "position is out of bounds");
			throw new IllegalArgumentException();
		}

		final File notePath = new File(getFilesDir(), fileName + "/");
		boolean loaded = false;

		// load saved page if there is one
		if (fileName != null && notePath.exists()) {
			final String notePathString = notePath.toString() + "/" + position
					+ Deepnotes.PNG_SUFFIX;
			Bitmap note = null;

			final File noteFile = new File(notePathString);
			if (noteFile.exists()) {
				note = BitmapFactory.decodeFile(notePathString);
				drawView.setBitmap(note.copy(Bitmap.Config.ARGB_8888, true));
				loaded = true;
			}
		}

		// set background if one exists
		// first check cached files
		boolean modified = false;
		File file = new File(getCacheDir() + "/" + position
				+ Deepnotes.JPG_SUFFIX);
		String path = null;
		Bitmap bitmap = null;

		if (file.exists()) {
			path = file.getPath();
			modified = true;
		} else {
			// check if there is a saved background
			file = new File(notePath.toString() + "/background_" + position
					+ Deepnotes.JPG_SUFFIX);

			if (file.exists()) {
				path = file.getPath();
			}
		}

		if (path != null) {
			bitmap = BitmapFactory.decodeFile(path);
			drawView.setBackgroundBitmap(
					bitmap.copy(Bitmap.Config.ARGB_8888, true), modified);
		}

		if (loaded) {
			drawView.redraw();
			return;
		}

		// else the page is empty
		drawView.setBitmap(Bitmap.createBitmap(Deepnotes.getViewportWidth(),
				Deepnotes.getViewportHeight(), Bitmap.Config.ARGB_8888));
		drawView.redraw();
	}

	/**
	 * Reloads a given note page of the note. The background will not be
	 * reloaded!
	 *
	 * @param index
	 *            The index of the page that will be reloaded.
	 */
	// Author: Jan Pretzel
	private void reloadNotePage(final int index) {
		if (index < 0 || index >= viewFlipper.getChildCount()) {
			Log.e(Deepnotes.APP_NAME, "index is out of bounds");
			throw new IllegalArgumentException();
		}

		final File notePath = new File(getFilesDir(), fileName + "/");

		if (notePath.exists()) {
			final String notePathString = notePath.toString();
			final DrawView reloadView = (DrawView) viewFlipper
					.getChildAt(index);

			final File noteFile = new File(notePathString + "/" + index
					+ Deepnotes.PNG_SUFFIX);
			if (noteFile.exists()) {
				final Bitmap note = BitmapFactory.decodeFile(notePathString
						+ "/" + index + Deepnotes.PNG_SUFFIX);
				reloadView.loadBitmap(note);
			}
		}
	}

	// Author: Sebastian Ullrich
	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);

		final MenuInflater inflater = new MenuInflater(
				this.getApplicationContext());
		inflater.inflate(R.menu.draw_menu, menu);

		return true;
	}

	// Author: Jan Pretzel
	// Author: Sebastian Ullrich
	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		super.onOptionsItemSelected(item);
		boolean handled = false;

		switch (item.getItemId()) {

		// Save triggered
		case R.id.draw_menu_save:
			saveNote(false, false);
			handled = true;
			break;

		// Delete triggered
		case R.id.draw_menu_delete:
			// is there a note to delete?
			if (fileName != null
					&& !IOManager.deleteNote(getApplicationContext(), fileName)) {
				Toast.makeText(getApplicationContext(),
						R.string.delete_exception, Toast.LENGTH_SHORT).show();
			}
			finish();
			handled = true;
			break;

		// Gallery import triggered
		case R.id.draw_menu_importfromgallery:
			final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("image/*");
			startActivityForResult(Intent.createChooser(intent,
					getString(R.string.choose_image)), REQUEST_GALLERY);
			handled = true;
			break;

		// Camera import triggered
		case R.id.draw_menu_importfromcamera:
			if (Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)) {
				final Intent cameraIntent = new Intent(
						MediaStore.ACTION_IMAGE_CAPTURE);

				final File file = new File(
						Environment.getExternalStorageDirectory()
								+ Deepnotes.SAVE_CACHE);
				file.mkdirs();

				pictureUri = Uri.fromFile(new File(file, "/camera.jpg"));
				cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, pictureUri);
				cameraIntent.putExtra("return-data", true);
				startActivityForResult(cameraIntent, REQUEST_CAMERA);
			} else {
				Toast.makeText(this.getApplicationContext(),
						"External Storage not mounted", Toast.LENGTH_SHORT)
						.show();
			}
			handled = true;
			break;

		// share triggered
		case R.id.draw_menu_share:
			if (saveStateChanged) {
				saveNote(false, true);
			} else {
				IOManager.shareNote(weakThis.get(), fileName);
			}
			handled = true;
			break;

		// Black Color Picked
		case R.id.draw_menu_colorblack:
			setCurrentColor(Deepnotes.BLACK);
			handled = true;
			break;

		// White Color Picked
		case R.id.draw_menu_colorwhite:
			setCurrentColor(Color.WHITE);
			handled = true;
			break;

		// Custom Color Picked
		case R.id.draw_menu_colorcustom:
			new ColorPickerDialog(this, this, getCurrentColor()).show();
			handled = true;
			break;

		// Pen Width Thick Picked
		case R.id.draw_menu_penwidththick:
			currentDrawView.setPenWidth(Deepnotes.PEN_WIDTH_THICK);
			handled = true;
			break;

		// Pen Width Normal Picked
		case R.id.draw_menu_penwidthnormal:
			currentDrawView.setPenWidth(Deepnotes.PEN_WIDTH_NORMAL);
			handled = true;
			break;

		// Pen Width Thin Picked
		case R.id.draw_menu_penwidththin:
			currentDrawView.setPenWidth(Deepnotes.PEN_WIDTH_THIN);
			handled = true;
			break;

		// Pen Width Thin Picked
		case R.id.draw_menu_undo:
			currentDrawView.undo();
			handled = true;
			break;

		// Page Forward
		case R.id.draw_menu_forward:
			showNextDrawView();
			handled = true;
			break;

		// Page Backward
		case R.id.draw_menu_back:
			showPreviousDrawView();
			handled = true;
			break;

		// Clear Page
		case R.id.draw_menu_clear:
			currentDrawView.clearView(false);

			// delete cached background if there is one
			final File file = new File(getCacheDir() + "/"
					+ viewFlipper.getDisplayedChild() + Deepnotes.JPG_SUFFIX);
			if (file.exists() && !file.delete()) {
				Log.e(Deepnotes.APP_NAME, Deepnotes.ERROR_FILE);
			}
			handled = true;
			break;

		default:
			break;
		}

		return handled;
	}

	/**
	 * Initiates saving of the note, by instantiating a {@link SaveNote}
	 * -object. Because of the saving being progressed in an AsyncTask, we need
	 * to wait for it to finish before we can close the
	 *
	 * {@link DrawActivity} or share the note. Therefore we need two flags
	 *      to pass through that tell the AsyncTask if we want to save or share.
	 *
	 * @param finish
	 *            Tells {@link SaveNote} whether the
	 *            {@link DrawActivity} shall be closed after saving or not.
	 * @param share
	 *            Tells {@link SaveNote} whether to share the note after
	 *            saving or not.
	 */
	// Author: Jan Pretzel
	protected final void saveNote(final boolean finish, final boolean share) {
		if (!saveStateChanged) {
			return;
		}

		// do we have a new note?
		if (fileName == null) {
			fileName = String.valueOf(System.currentTimeMillis());
		}

		new SaveNote(weakThis.get(), finish, share).execute();

	}

	/**
	 * Shows the next {@link DrawView} by triggering an animated page turn.
	 * Modify this method for adding animations etc.
	 */
	// Author: Sebastian Ullrich
	public final void showNextDrawView() {
		showPage(PAGE_FORWARD);
	}

	/**
	 * Shows the previous {@link DrawView} by triggering an animated page
	 * turn. Modify this method for adding animations etc.
	 */
	// Author: Sebastian Ullrich
	public final void showPreviousDrawView() {
		showPage(PAGE_BACKWARD);
	}

	/**
	 * Identifier for at page forward flip.
	 */
	private static final boolean PAGE_FORWARD = true;

	/**
	 * Identifier for at page backward flip.
	 */
	private static final boolean PAGE_BACKWARD = false;

	/**
	 * Shows the next page of the note depending on the direction. To save
	 * memory the current page will be cached and it's Bitmaps recycled before
	 * we load the new displayed note.
	 *
	 * @param direction
	 *            The page flip direction. Expects
	 *            {@link DrawActivity#PAGE_FORWARD} or
	 *            {@link DrawActivity#PAGE_BACKWARD}
	 */
	// Author: Jan Pretzel
	// Author: Sebastian Ullrich
	private void showPage(final boolean direction) {
		final Paint tempPaint = ((DrawView) viewFlipper.getCurrentView())
				.getPaint();
		showPageToast(direction);

		saveDrawingCache();
		currentDrawView.recycle();

		if (direction == PAGE_FORWARD) {
			viewFlipper.showNext();
		} else {
			viewFlipper.showPrevious();
		}

		currentDrawView = (DrawView) viewFlipper.getCurrentView();
		loadNotePage(currentDrawView, viewFlipper.getDisplayedChild());
		currentDrawView.setPaint(tempPaint);
	}

	/**
	 * Will queue a toast message with the current page number.
	 *
	 * @param direction
	 *            The page flip direction. Expects
	 *            {@link DrawActivity#PAGE_FORWARD} or
	 *            {@link DrawActivity#PAGE_BACKWARD}
	 */
	// Author: Sebastian Ullrich
	private void showPageToast(final boolean direction) {
		final int currentPage = viewFlipper.getDisplayedChild() + 1;
		final int size = viewFlipper.getChildCount();

		String msg;

		if (direction == PAGE_FORWARD) {
			// show next page
			if (currentPage == size) {
				msg = "1";
			} else {
				msg = String.valueOf(currentPage + 1);
			}
		} else {
			if (currentPage == 1) {
				msg = String.valueOf(size);
			} else {
				msg = String.valueOf(currentPage - 1);
			}
		}

		Toast.makeText(this.getApplicationContext(), msg, Toast.LENGTH_SHORT)
				.show();
	}

	/**
	 * This method intents image cropping. To handle cropping correctly we need
	 * to check if there is an Activity to handle cropping first. If there is
	 * one, we need to force the intent to use it, otherwise we risk to get
	 * errors when coming from a camera Activity.
	 */
	// Author: Jan Pretzel
	private void cropImage() {
		final Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setType("image/*");

		final List<ResolveInfo> resolveInfo = getPackageManager()
				.queryIntentActivities(intent, 0);

		if (resolveInfo.isEmpty()) {
			Toast.makeText(this.getApplicationContext(),
					"No crop application found.", Toast.LENGTH_SHORT).show();

			outputUri = pictureUri;
			setPageBackground();
			return;
		}

		// force the intent to take crop activity (won't
		// work from camera activity without this!)
		final ResolveInfo res = resolveInfo.get(0);
		intent.setComponent(new ComponentName(res.activityInfo.packageName,
				res.activityInfo.name));

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			File file = new File(Environment.getExternalStorageDirectory()
					+ Deepnotes.SAVE_CACHE);

			file.mkdirs();
			file = new File(file, "crop.jpg");

			outputUri = Uri.fromFile(file);

			intent.setData(pictureUri);
			final int width = Deepnotes.getViewportWidth();
			final int height = Deepnotes.getViewportHeight();
			intent.putExtra("outputX", width);
			intent.putExtra("outputY", height);
			intent.putExtra("aspectX", width);
			intent.putExtra("aspectY", height);
			intent.putExtra("scale", true);
			intent.putExtra("return-data", false);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);

			startActivityForResult(intent, REQUEST_CROP);
		} else {
			Toast.makeText(this.getApplicationContext(),
					"External Storage not mounted", Toast.LENGTH_SHORT).show();
		}
	}

	// Author: Jan Pretzel
	// Author: Sebastian Ullrich
	@Override
	public final void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		// Import from gallery result
		case REQUEST_GALLERY:
			if (resultCode == RESULT_OK) {
				pictureUri = data.getData();
				cropImage();

				break;
			}

		// Import from camera result
		case REQUEST_CAMERA:
			if (resultCode == RESULT_OK) {
				cropImage();

				break;
			}

		// crop result
		case REQUEST_CROP:
			if (resultCode == RESULT_OK) {
				setPageBackground();
			}

			// delete temporary created files
			// the file behind outputUri is always created
			// and needs to be deleted
			if (outputUri != null && !new File(outputUri.getPath()).delete()) {
				Log.e(Deepnotes.APP_NAME, Deepnotes.ERROR_FILE);
			}

			// if we came from the camera activity we need
			// to delete pictureUri too
			if (pictureUri != null) {
				final String cameraImg = pictureUri.getPath();
				if (cameraImg.contains("camera.jpg")
						&& !new File(cameraImg).delete()) {
					Log.e(Deepnotes.APP_NAME, Deepnotes.ERROR_FILE);
				}
			}

			break;

		default:
			break;
		}
	}

	/**
	 * Sets the imported images as background for the currently displayed
	 * {@link DrawView}.
	 */
	// Author: Jan Pretzel
	private void setPageBackground() {
		Bitmap bitmap = null;
		try {
			bitmap = MediaStore.Images.Media.getBitmap(
					getContentResolver(), outputUri);
			currentDrawView.setBackgroundBitmap(bitmap, true);
		} catch (FileNotFoundException e) {
			Log.e(Deepnotes.APP_NAME, "failed to get image.");
		} catch (IOException e) {
			Log.e(Deepnotes.APP_NAME, "failed to get image.");
		}

		saveStateChanged = true;
	}

	// Author: Sebastian Ullrich
	@Override
	public final boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			/* check for changes */
			if (saveStateChanged) {
				/* Creating the save dialog. */
				final AlertDialog.Builder builder = new AlertDialog.Builder(
						this);
				builder.setMessage(R.string.save_dialog)
						.setCancelable(true)
						.setPositiveButton(R.string.yes,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(
											final DialogInterface dialog,
											final int identification) {
										// call the save procedure.
										saveNote(true, false);
									}
								})
						.setNegativeButton(R.string.no,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(
											final DialogInterface dialog,
											final int identification) {
										finish();
									}
								});
				builder.create().show();
			} else {
				finish();
			}

			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Clears the cached note pages. Drawing cache is located in the internal
	 * storage of the application, and cannot be read by other applications.
	 */
	// Author: Jan Pretzel
	private void clearDrawingCache() {
		final File[] files = getCacheDir().listFiles();

		for (File file : files) {
			if (!file.delete()) {
				Log.e(Deepnotes.APP_NAME, Deepnotes.ERROR_FILE);
			}
		}
	}

	/**
	 * Saves the currently displayed note to the drawing cache. Drawing cache is
	 * located in the internal storage of the application, and cannot be read by
	 * other applications.
	 */
	// Author: Jan Pretzel
	private void saveDrawingCache() {
		if (currentDrawView.isBgModified()) {
			String cachePath = getCacheDir().toString();
			final File file = new File(cachePath);
			file.mkdirs();

			cachePath += "/" + viewFlipper.getDisplayedChild()
					+ Deepnotes.JPG_SUFFIX;
			final Bitmap bitmap = currentDrawView.getBackgroundBitmap();

			IOManager.writeFile(bitmap, cachePath, Bitmap.CompressFormat.JPEG,
					Deepnotes.JPG_QUALITY);
		}
	}

	/**
	 * Will be called before the Activity is destroyed. We will need to clear
	 * the drawing cache here and make sure the Bitmaps can get garbage
	 * collected.
	 */
	// Author: Jan Pretzel
	@Override
	protected final void onDestroy() {
		clearDrawingCache();

		// THIS IS IMPORTANT
		// without this the Activity won't get collected by the GC
		// and we then leak a lot of memory
		final int count = viewFlipper.getChildCount();
		for (int i = 0; i < count; i++) {
			final DrawView clearView = (DrawView) viewFlipper.getChildAt(i);
			clearView.recycle();
		}

		super.onDestroy();
	}

	/**
	 * When we listen to a color change of an {@link DrawView} we need to
	 * adjust {@link DrawActivity#currentColor}.
	 *
	 * @param color
	 *            The new {@link DrawActivity#currentColor}.
	 */
	// Author: Sebastian Ullrich
	@Override
	public final void colorChanged(final int color) {
		setCurrentColor(color);
	}

	/**
	 * When a {@link DrawView} changes in terms of background or foreground we
	 * need to set {@link DrawActivity#saveStateChanged} to true.
	 */
	// Author: Jan Pretzel
	@Override
	public final void changed() {
		saveStateChanged = true;
	}

	/**
	 * When a {@link DrawView} undid something we need to call
	 * {@link DrawActivity#reloadNotePage(int)} for the currently displayed
	 * {@link DrawView}.
	 */
	// Author: Jan Pretzel
	@Override
	public final void undone() {
		reloadNotePage(viewFlipper.getDisplayedChild());
	}

	/**
	 * Getter for {@link DrawActivity#currentPaint}.
	 *
	 * @return The {@link DrawActivity#currentPaint}.
	 */
	// Author: Sebastian Ullrich
	public final int getCurrentPaint() {
		return currentPaint;
	}

	/**
	 * Setter for {@link DrawActivity#currentPaint}.
	 *
	 * @param newPaint
	 *            The new {@link DrawActivity#currentPaint}.
	 */
	// Author: Sebastian Ullrich
	public final void setCurrentPaint(final int newPaint) {
		this.currentPaint = newPaint;
	}

	/**
	 * Getter for {@link DrawActivity#currentColor}.
	 *
	 * @return The new {@link DrawActivity#currentColor}.
	 */
	// Author: Sebastian Ullrich
	public final int getCurrentColor() {
		return currentColor;
	}

	/**
	 * Setter for {@link DrawActivity#currentColor}.
	 *
	 * @param newCurrentColor
	 *            The new {@link DrawActivity#currentColor}.
	 */
	// Author: Sebastian Ullrich
	public final void setCurrentColor(final int newCurrentColor) {
		this.currentColor = newCurrentColor;
		currentDrawView.setPaintColor(newCurrentColor);
	}

	/**
	 * An AsyncTask to save the current note, it's backgrounds and a thumbnail.
	 * While working this task will show a ProgressDialog telling the user that
	 * it is saving at the moment. To write the files to the storage, it uses
	 * {@link IOManager#writeFile(Bitmap, String, android.graphics.Bitmap.CompressFormat, int)}
	 * . Thumbnails will be saved in a subfolder called 'thumbnail'. Fore- and
	 * backgrounds will be saved to a subfolder with the
	 * {@link DrawActivity#fileName} as name. Foregrounds will be saved as
	 * PNG-files to enable transparency and names will be numbered from 0 - 3.
	 * Backgrounds will be saved as JPG-files and have the same name as the
	 * foreground with 'background_' as prefix.
	 *
	 * @author Jan Pretzel (jan.pretzel@deepsource.de)
	 */
	private static class SaveNote extends AsyncTask<Void, Void, Void> {

		/**
		 * The ProgressDialog that will be shown while saving.
		 */
		private final ProgressDialog dialog;

		/**
		 * Identifier that tells us whether to finish {@link DrawActivity}
		 * after saving or not.
		 */
		private final boolean finish;

		/**
		 * Identifier that tells us whether to share the note after saving or
		 * not.
		 */
		private final boolean share;

		/**
		 * The {@link DrawActivity} that instantiated this class.
		 */
		private final DrawActivity activity;

		/**
		 * Constructor.
		 *
		 * @param dActivity
		 *            The {@link DrawActivity} that instantiated this
		 *            class.
		 * @param finActivity
		 *            Identifier that tells us whether to finish
		 *            {@link DrawActivity} after saving or not.
		 * @param shareActivity
		 *            Identifier that tells us whether to share the note after
		 *            saving or not.
		 */
		// Author: Jan Pretzel
		public SaveNote(final DrawActivity dActivity,
				final boolean finActivity, final boolean shareActivity) {
			super();

			if (dActivity == null) {
				Log.e(Deepnotes.APP_NAME, "dActivity must not be null");
				throw new IllegalArgumentException();
			}

			dialog = new ProgressDialog(dActivity);
			this.finish = finActivity;
			this.share = shareActivity;
			this.activity = dActivity;
		}

		/**
		 * The main part of the AsyncTask. Here The note and all it's associated
		 * parts will be saved to the file system.
		 *
		 * @param params
		 *            Since it's type is Void there will be no parameters at
		 *            all.
		 *
		 * @return Since the return values type is Void, null will be returned.
		 */
		// Author: Jan Pretzel
		@Override
		protected Void doInBackground(final Void... params) {

			// save thumbnail
			String savePath = activity.getFilesDir() + Deepnotes.SAVE_THUMBNAIL;
			File file = new File(savePath);

			// Creates the directory named by this abstract pathname,
			// including any necessary but nonexistent parent directories.
			file.mkdirs();

			Bitmap bitmap = null;
			DrawView toSave = (DrawView) activity.viewFlipper.getChildAt(0);
			activity.loadNotePage(toSave, 0);

			if ((toSave.isModified() || toSave.isBgModified())
					|| toSave.deleteStatus()) {
				bitmap = createThumbnail();

				IOManager.writeFile(bitmap, savePath + activity.fileName
						+ Deepnotes.JPG_SUFFIX, Bitmap.CompressFormat.JPEG,
						Deepnotes.JPG_QUALITY);
			}

			// save note pages with separate backgrounds
			savePath = activity.getFilesDir() + "/" + activity.fileName + "/";
			file = new File(savePath);

			for (int i = 0; i < activity.viewFlipper.getChildCount(); i++) {
				toSave = (DrawView) activity.viewFlipper.getChildAt(i);
				activity.loadNotePage(toSave, i);

				if (toSave.isModified()) {
					// first saved note page will create a sub folder
					file.mkdirs();

					// save note
					bitmap = toSave.getBitmap();

					IOManager.writeFile(bitmap, savePath + i
							+ Deepnotes.PNG_SUFFIX, Bitmap.CompressFormat.PNG,
							Deepnotes.PNG_QUALITY);

					toSave.setModified(false);
				}

				if (toSave.isBgModified()) {
					// save background
					bitmap = toSave.getBackgroundBitmap();

					IOManager.writeFile(bitmap, savePath + "background_" + i
							+ Deepnotes.JPG_SUFFIX, Bitmap.CompressFormat.JPEG,
							Deepnotes.JPG_QUALITY);

					toSave.setBgModified(false);
				}

				File toDelete = null;
				// check for delete status
				if (toSave.deleteStatus()) {
					toDelete = new File(savePath + i + Deepnotes.PNG_SUFFIX);
					if (!toDelete.delete()) {
						Log.e(Deepnotes.APP_NAME, Deepnotes.ERROR_FILE);
					}
					toSave.setModified(false);

					toDelete = new File(savePath + "background_" + i
							+ Deepnotes.JPG_SUFFIX);
					if (!toDelete.delete()) {
						Log.e(Deepnotes.APP_NAME, Deepnotes.ERROR_FILE);
					}
					toSave.setBgModified(false);
				}
			}

			return null;
		}

		/**
		 * Before execution starts, the ProgressDialog will be shown.
		 */
		// Author: Jan Pretzel
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
		 * @param result
		 *            Since the return values type is Void, null will be
		 *            returned.
		 */
		// Author: Jan Pretzel
		@Override
		protected void onPostExecute(final Void result) {
			if (dialog.isShowing()) {
				dialog.dismiss();
			}

			super.onPostExecute(result);

			// reset saveStateChanged
			activity.saveStateChanged = false;

			final Toast toast = Toast.makeText(
					activity.getApplicationContext(), R.string.note_saved,
					Toast.LENGTH_SHORT);
			toast.show();

			if (share) {
				IOManager.shareNote(activity, activity.fileName);
			}

			// need to finish in onPostExecute, else we leak the window
			if (finish) {
				activity.finish();
			}
		}

		/**
		 * Calculates a thumbnail representing the note. The first page of the
		 * note (empty or not) will be scaled by 0.5 to save memory.
		 *
		 * @return The thumbnail as Bitmap.
		 */
		// Author: Jan Pretzel
		private Bitmap createThumbnail() {
			// get first page of the note
			final DrawView drawView = (DrawView) activity.viewFlipper
					.getChildAt(0);
			final Bitmap firstPage = drawView.getBitmap();

			// scale factor = 0.5
			final float scale = 0.5f;

			// create matrix
			final Matrix matirx = new Matrix();
			matirx.postScale(scale, scale);

			final int width = firstPage.getWidth();
			final int height = firstPage.getHeight();

			// create scaled bitmaps
			final Bitmap firstPageScaled = Bitmap.createBitmap(firstPage, 0, 0,
					width, height, matirx, true);

			Canvas pageAndBackground;
			Bitmap firstBGScaled;

			if (drawView.isBgModified()) {
				final Bitmap firstBackground = drawView.getBackgroundBitmap();
				firstBGScaled = Bitmap.createBitmap(firstBackground, 0, 0,
						width, height, matirx, true);
				pageAndBackground = new Canvas(firstBGScaled);
			} else {
				firstBGScaled = Bitmap.createBitmap((int) (width * scale),
						(int) (height * scale), Bitmap.Config.ARGB_8888);
				pageAndBackground = new Canvas(firstBGScaled);
				pageAndBackground.drawColor(Color.WHITE);
			}

			// combine both bitmaps
			pageAndBackground.drawBitmap(firstPageScaled, 0f, 0f, null);

			// free unused Bitmap
			firstPageScaled.recycle();

			return firstBGScaled;
		}
	}

}
