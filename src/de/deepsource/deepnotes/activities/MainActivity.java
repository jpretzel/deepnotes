package de.deepsource.deepnotes.activities;

import java.io.File;
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
import android.view.Display;
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
	protected final Context context = this;
	private GridView notesView;

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
        na = new NotesAdapter(context.getApplicationContext(), R.layout.note_item, R.id.fileName, notes);
        
        notesView = (GridView) findViewById(R.id.notesView);
        registerForContextMenu(notesView);
        notesView.setAdapter(na);
        
        notesView.setOnItemClickListener(
        		new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						
						final Intent intent = new Intent(context.getApplicationContext(), DrawActivity.class);
						intent.putExtra(Deepnotes.SAVED_NOTE_NAME, notes.get(position).getFileName());
						
						Log.e("INIT", String.valueOf(android.os.Debug.getNativeHeapAllocatedSize()));
						// TODO: does it help?
						//System.gc();
						Log.e("INIT", String.valueOf(android.os.Debug.getNativeHeapAllocatedSize()));

						startActivity(intent);
					}
				});
        
        Display display = getWindowManager().getDefaultDisplay();
        
        Log.e("WIDTH", String.valueOf(display.getWidth()));
        Deepnotes.setViewportWidth(800);
        Deepnotes.setViewportHeight(600);
        
//        loadNotes();
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
			
			// sort this array in reverse order (newest first)
			// this is needed, because lisFiles() documentation says:
			// "There is no guarantee that the name strings in the resulting 
			// array will appear in any specific order; they are not, in 
			// particular, guaranteed to appear in alphabetical order."
			Arrays.sort(notePages, Collections.reverseOrder());
			
			for (File note : notePages) {
				notes.add(new Note(context.getApplicationContext(), note.getName()));
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
		
		MenuInflater inflater = new MenuInflater(context.getApplicationContext());
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
			Log.e("INIT", String.valueOf(android.os.Debug.getNativeHeapAllocatedSize()));
			Intent intent = new Intent(context.getApplicationContext(), DrawActivity.class);
			startActivity(intent);
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
		
		case (Deepnotes.REQUEST_SHARE_NOTE): {
			// always clear cache, regardless of the resultCode
			// TODO: find another way to clear cache
			// this will lead to the share app not loading any image at all
			IOManager.clearCache();
			break;
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
		
		MenuInflater inflater = new MenuInflater(context.getApplicationContext());
		inflater.inflate(R.menu.main_contextmenu, menu);
		menu.setHeaderTitle(R.string.note);
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
			
			if (IOManager.deleteNote(context.getApplicationContext(), notes.get(index).getFileName())) {
				notes.remove(index);
				na.notifyDataSetChanged();
			}
			
			return true;
		}
		
		case (R.id.main_contextmenu_sendnote): {
			AdapterView.AdapterContextMenuInfo menuInfo;
			menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			int index = menuInfo.position;
			
			IOManager.shareNote(this, notes.get(index).getFileName());
			Log.e("SHARE", "save rdy");
		}
		}
		
		return false;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		loadNotes();
		Log.e("INIT", String.valueOf(android.os.Debug.getNativeHeapAllocatedSize()));
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		int count = notes.size();
		for (int i = 0; i < count; i++) {
			notes.get(i).recycle();
		}
		
		// recycle notes to free up memory
		notes.clear();
		na.notifyDataSetChanged();
	}
	
	/**
	 * The NotesAdapter handles the notes for the GridView.
	 * 
	 * @author Jan Pretzel (jan.pretzel@deepsource.de)
	 */
	private static class NotesAdapter extends ArrayAdapter<Note> {

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
			noteImage.setImageBitmap(note.getThumbnail());
			
			return noteView;
		}
		
	}
	
}