package de.deepsource.deepnotes.activities;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import de.deepsource.deepnotes.R;
import de.deepsource.deepnotes.application.Deepnotes;
import de.deepsource.deepnotes.models.Note;
import de.deepsource.deepnotes.utilities.IOManager;

/**
 * This activity handles the main window of the application.
 *
 * @author Jan Pretzel (jan.pretzel@deepsource.de)
 */
public class MainActivity extends FragmentActivity {

     /**
	 * TODO.
	 */
	private ArrayList<Note> notes;

	/**
	 * TODO.
	 */
	private NotesAdapter na;

	/**
	 * TODO.
	 */
	private GridView notesView;

	/**
	 * TODO.
	 */
	private static final int MAX_NOTES = 50;

	/**
	 * Called when the activity is first created.
	 *
	 * @param savedInstanceState TODO
	 *
	 * @author Jan Pretzel
	 */
	@Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        notes = new ArrayList<Note>();
        na = new NotesAdapter(getApplicationContext(),
        		R.layout.note_item, R.id.fileName, notes);

        notesView = (GridView) findViewById(R.id.notesView);

        registerForContextMenu(notesView);
        notesView.setAdapter(na);

        notesView.setOnItemClickListener(
        		new OnItemClickListener() {

					@Override
					public void onItemClick(
							final AdapterView<?> parent,
							final View view,
							final int position,
							final long id) {

						final Intent intent = new Intent(
								getApplicationContext(),
								DrawActivity.class);
						intent.putExtra(
								Deepnotes.SAVED_NOTE_NAME,
								notes.get(position).getFileName());

						startActivity(intent);
					}
				});
	}

	/**
	 * Loads all saved notes.
	 *
	 * @author Jan Pretzel
	 */
	public final void loadNotes() {
		File notePath = new File(getFilesDir() + Deepnotes.SAVE_THUMBNAIL);

		if (notePath.exists()) {
			File[] notePages = notePath.listFiles();

			// sort this array in reverse order (newest first)
			// this is needed, because listFiles() documentation says:
			// "There is no guarantee that the name strings in the resulting
			// array will appear in any specific order; they are not, in
			// particular, guaranteed to appear in alphabetical order."
			Arrays.sort(notePages, Collections.reverseOrder());

			for (File note : notePages) {
				na.add(new Note(getApplicationContext(), note.getName()));
			}
		}
	}

	/**
	 * Adding the menu.
	 *
	 * @param menu TODO.
	 *
	 * @return TODO.
	 *
	 * @author Jan Pretzel
	 */
	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = new MenuInflater(getApplicationContext());
		inflater.inflate(R.menu.main_menu, menu);

		return true;
	}

	/**
	 * Handle menu actions.
	 *
	 * @param item TODO.
	 *
	 * @return TODO.
	 *
	 * @author Jan Pretzel
	 */
	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case (R.id.main_menu_addnote):
			if (notes.size() < MAX_NOTES) {
				final Intent intent = new Intent(getApplicationContext(),
						DrawActivity.class);
				startActivity(intent);
			} else {
				Toast.makeText(
						getApplicationContext(),
						R.string.max_notes,
						Toast.LENGTH_SHORT).show();
			}

			return true;


		case (R.id.main_menu_credits):
			return true;

		case (R.id.main_menu_help):
			return true;

			// TODO: delete
		case (R.id.gc):
			System.gc();
			Log.e("MAIN RESUME", String.valueOf(android.os.Debug
					.getNativeHeapAllocatedSize()));
			break;


		default:
			break;
		}

		return false;
	}

	/**
	 * Adding context menu.
	 *
	 * @param menu TODO
	 *
	 * @param v TODO
	 *
	 * @param menuInfo TODO
	 *
	 * @author Jan Pretzel
	 */
	@Override
	public final void onCreateContextMenu(final ContextMenu menu, final View v,
			final ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		MenuInflater inflater = new MenuInflater(getApplicationContext());
		inflater.inflate(R.menu.main_contextmenu, menu);
		menu.setHeaderTitle(R.string.note);
	}

	/**
	 * Handle context menu actions.
	 *
	 * @param item TODO
	 *
	 * @return TODO
	 *
	 * @author Jan Pretzel
	 */
	@Override
	public final boolean onContextItemSelected(final MenuItem item) {
		super.onContextItemSelected(item);

		AdapterView.AdapterContextMenuInfo menuInfo;
		menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		int index = menuInfo.position;

		switch (item.getItemId()) {
		case (R.id.main_contextmenu_removenote):
			if (IOManager.deleteNote(getApplicationContext(), notes.get(index).getFileName())) {
				notes.remove(index);
				na.notifyDataSetChanged();
			} else {
				Toast.makeText(
						getApplicationContext(),
						R.string.delete_exception,
						Toast.LENGTH_SHORT).show();
			}

			return true;

		case (R.id.main_contextmenu_sendnote):
			WeakReference<MainActivity> weakActivity = new WeakReference<MainActivity>(this);
			IOManager.shareNote(weakActivity.get(), notes.get(index).getFileName());
			return true;

		default:
			break;
		}

		return false;
	}

	@Override
	protected final void onResume() {
		super.onResume();
		loadNotes();
	}

	@Override
	protected final void onPause() {
		super.onPause();

		// recycle notes to free up memory
		int count = notes.size();
		for (int i = 0; i < count; i++) {
			notes.get(i).recycle();
		}

		// and get rid of references
		na.clear();
	}

	/**
	 * The NotesAdapter handles the notes for the GridView.
	 *
	 * @author Jan Pretzel (jan.pretzel@deepsource.de)
	 */
	private static class NotesAdapter extends ArrayAdapter<Note> {

		/**
		 * TODO.
		 */
		private final int resource;

		/**
		 *
		 * @param context TODO
		 * @param resource TODO
		 * @param textViewResourceId TODO
		 * @param objects TODO
		 */
		public NotesAdapter(final Context context, final int resource,
				final int textViewResourceId, final List<Note> objects) {
			super(context, resource, textViewResourceId, objects);
			this.resource = resource;
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			super.getView(position, convertView, parent);

			LinearLayout noteView;
			Note note = getItem(position);

			if (convertView == null) {
				noteView = new LinearLayout(getContext());
				String inflater = Context.LAYOUT_INFLATER_SERVICE;
				LayoutInflater li = (LayoutInflater) getContext().getSystemService(inflater);
				li.inflate(resource, noteView, true);
			} else {
				noteView = (LinearLayout) convertView;
			}

			TextView fileName = (TextView) noteView.findViewById(R.id.fileName);
			ImageView noteImage = (ImageView) noteView.findViewById(R.id.noteImage);

			fileName.setText(note.getCreated());
			noteImage.setImageBitmap(note.getThumbnail());

			return noteView;
		}
	}
}
