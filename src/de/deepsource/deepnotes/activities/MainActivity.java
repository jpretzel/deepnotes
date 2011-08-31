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
	private List<Note> notes;

	/**
	 * TODO.
	 */
	private NotesAdapter notesAdapter;

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
        notesAdapter = new NotesAdapter(getApplicationContext(),
        		R.layout.note_item, R.id.fileName, notes);

        final GridView notesView = (GridView) findViewById(R.id.notesView);

        registerForContextMenu(notesView);
        notesView.setAdapter(notesAdapter);

        notesView.setOnItemClickListener(
        		new OnItemClickListener() {

					@Override
					public void onItemClick(
							final AdapterView<?> parent,
							final View view,
							final int position,
							final long identification) {

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
		final File notePath = new File(getFilesDir() + Deepnotes.SAVE_THUMBNAIL);

		if (notePath.exists()) {
			final File[] notePages = notePath.listFiles();

			// sort this array in reverse order (newest first)
			// this is needed, because listFiles() documentation says:
			// "There is no guarantee that the name strings in the resulting
			// array will appear in any specific order; they are not, in
			// particular, guaranteed to appear in alphabetical order."
			Arrays.sort(notePages, Collections.reverseOrder());

			for (File note : notePages) {
				notesAdapter.add(new Note(getApplicationContext(), note.getName()));
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

		final MenuInflater inflater = new MenuInflater(getApplicationContext());
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

		boolean handled = false;

		switch (item.getItemId()) {
		case R.id.main_menu_addnote:
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

			handled = true;
			break;


		case R.id.main_menu_credits:
			handled = true;
			break;

		case R.id.main_menu_help:
			handled = true;
			break;

		default:
			break;
		}

		return handled;
	}

	/**
	 * Adding context menu.
	 *
	 * @param menu TODO
	 *
	 * @param view TODO
	 *
	 * @param menuInfo TODO
	 *
	 * @author Jan Pretzel
	 */
	@Override
	public final void onCreateContextMenu(final ContextMenu menu, final View view,
			final ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);

		final MenuInflater inflater = new MenuInflater(getApplicationContext());
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

		boolean handled = false;

		AdapterView.AdapterContextMenuInfo menuInfo;
		menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		final int index = menuInfo.position;

		switch (item.getItemId()) {
		case R.id.main_contextmenu_removenote:
			if (IOManager.deleteNote(getApplicationContext(), notes.get(index).getFileName())) {
				notes.remove(index);
				notesAdapter.notifyDataSetChanged();
			} else {
				Toast.makeText(
						getApplicationContext(),
						R.string.delete_exception,
						Toast.LENGTH_SHORT).show();
			}

			handled = true;
			break;

		case R.id.main_contextmenu_sendnote:
			final WeakReference<MainActivity> weakActivity = new WeakReference<MainActivity>(this);
			IOManager.shareNote(weakActivity.get(), notes.get(index).getFileName());
			handled = true;
			break;

		default:
			break;
		}

		return handled;
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
		final int count = notes.size();
		for (int i = 0; i < count; i++) {
			notes.get(i).recycle();
		}

		// and get rid of references
		notesAdapter.clear();
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
		 * @param textViewResId TODO
		 * @param objects TODO
		 */
		public NotesAdapter(final Context context, final int resource,
				final int textViewResId, final List<Note> objects) {
			super(context, resource, textViewResId, objects);
			this.resource = resource;
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			super.getView(position, convertView, parent);

			LinearLayout noteView;
			final Note note = getItem(position);

			if (convertView == null) {
				noteView = new LinearLayout(getContext());
				final String inflater = Context.LAYOUT_INFLATER_SERVICE;
				final LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(inflater);
				layoutInflater.inflate(resource, noteView, true);
			} else {
				noteView = (LinearLayout) convertView;
			}

			final TextView fileName = (TextView) noteView.findViewById(R.id.fileName);
			final ImageView noteImage = (ImageView) noteView.findViewById(R.id.noteImage);

			fileName.setText(note.getCreated());
			noteImage.setImageBitmap(note.getThumbnail());

			return noteView;
		}
	}
}
