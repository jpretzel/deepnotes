package de.deepsource.deepnotes.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
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

	private ArrayList<Note> notes;
	private NotesAdapter na;
	private Context context = this;
	
	private static final int REQUEST_NOTE = 0x00000001;

	/** 
	 * Called when the activity is first created. 
	 * 
	 * @author Jan Pretzel
	 */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        notes = new ArrayList<Note>();
        na = new NotesAdapter(this, R.layout.note_item, R.id.fileName, notes);
        
        GridView notesView = (GridView) findViewById(R.id.notesView);
        registerForContextMenu(notesView);
        notesView.setAdapter(na);
        
        notesView.setOnItemClickListener(
        		new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						
						Intent intent = new Intent(context, DrawActivity.class);
						intent.putExtra(Deepnotes.SAVED_NOTE_NAME, notes.get(position).getFileName());
						intent.putExtra(Deepnotes.SAVED_NOTE_POSITION, position);
						
						startActivityForResult(intent, REQUEST_NOTE);			
					}
				});
        
        loadNotes();
	}    
	
	/**
	 * Loads all saved notes.
	 * 
	 * @author Jan Pretzel
	 */
	public void loadNotes() {
		File notePath = new File(getFilesDir() + Deepnotes.SAVE_THUMBNAIL);
		
		if (notePath.exists()) {
			File[] notePages = notePath.listFiles();
			
			for (File note : notePages) {
				notes.add(0, new Note(this.getApplicationContext(), note.getName()));
				na.notifyDataSetChanged();
			}
		}
	}
	
	/**
	 * Adding the menu.
	 * 
	 * @author Jan Pretzel
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.main_menu, menu);
		
		return true;
	}
	
	/**
	 * Handle menu actions.
	 * 
	 * @author Jan Pretzel
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		
		switch (item.getItemId()) {
		case (R.id.main_menu_addnote): {
			Intent intent = new Intent(this, DrawActivity.class);
			startActivityForResult(intent, REQUEST_NOTE);
			return true;
		}
		}
		
		return false;
	}
	
	/**
	 * Handle Activity results.
	 * 
	 * @author Jan Pretzel
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case (REQUEST_NOTE): {
			if (resultCode == Activity.RESULT_OK) {
				Bundle bundle = data.getExtras();
				String noteName = bundle.getString(Deepnotes.SAVED_NOTE_NAME);
				notes.add(0, new Note(this.getApplicationContext(), noteName));
				na.notifyDataSetChanged();
			} else if (resultCode == Deepnotes.SAVED_NOTE_DELETED) {
				Bundle bundle = data.getExtras();
				int notePosition = bundle.getInt(Deepnotes.SAVED_NOTE_POSITION);
				notes.remove(notePosition);
				na.notifyDataSetChanged();
			}
		}
		}
	}
	
	/**
	 * Adding context menu.
	 * 
	 * @author Jan Pretzel
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.main_contextmenu, menu);
		// TODO: add localized menu name
		menu.setHeaderTitle("WAS WEI§ ICH");
	}
	
	/**
	 * Handle context menu actions.
	 * 
	 * @author Jan Pretzel
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
		
		switch (item.getItemId()) {
		case (R.id.main_contextmenu_removenote): {
			AdapterView.AdapterContextMenuInfo menuInfo;
			menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			int index = menuInfo.position;
			
			if (IOManager.deleteNote(this, notes.get(index).getFileName())) {
				notes.remove(index);
				na.notifyDataSetChanged();
			}
			
			return true;
		}
		}
		
		return false;
	}
	
	/**
	 * The NotesAdapter handles the notes for the GridView.
	 * 
	 * @author Jan Pretzel (jan.pretzel@deepsource.de)
	 */
	public static class NotesAdapter extends ArrayAdapter<Note> {

		private int resource;
		
		public NotesAdapter(Context context, int resource,
				int textViewResourceId, List<Note> objects) {
			super(context, resource, textViewResourceId, objects);
			this.resource = resource;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
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
			noteImage.setImageDrawable(note.getThumbnail());
			
			return noteView;
		}
		
	}
	
}