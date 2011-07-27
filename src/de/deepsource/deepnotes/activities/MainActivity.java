package de.deepsource.deepnotes.activities;

import java.util.ArrayList;
import java.util.List;

import de.deepsource.deepnotes.R;
import de.deepsource.deepnotes.models.Note;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends FragmentActivity {

	private ArrayList<Note> notes;
	private NotesAdapter na;

	/** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        notes = new ArrayList<Note>();
        na = new NotesAdapter(this, R.layout.note_item, R.id.fileName, notes);
        
        GridView notesView = (GridView) findViewById(R.id.notesView);
        registerForContextMenu(notesView);
        notesView.setAdapter(na);
        
        Note testNote = new Note("/sdcard/deepnotes/test2.png");
        notes.add(0, testNote);
        na.notifyDataSetChanged();
	}    
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.main_menu, menu);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		
		switch (item.getItemId()) {
		case (R.id.main_menu_addnote):{
			notes.add(0, new Note("/sdcard/deepnotes/test3.png"));
			na.notifyDataSetChanged();
			return true;
		}
		
		case (R.id.main_menu_draw): {
			Intent intent = new Intent(this, DrawActivity.class);
			startActivity(intent);
			return true;
		}
		
		case (R.id.main_menu_exit): {
			finish();
			return true;
		}
		}
		
		return false;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.main_contextmenu, menu);
		menu.setHeaderTitle("WAS WEI§ ICH");
	}
	
	public class NotesAdapter extends ArrayAdapter<Note> {

		private int resource;

		/*public NotesAdapter(Context context, int resource, List<Note> notes) {
			super(context, resource, notes);
			this.resource = resource;
		}*/
		
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
			
			fileName.setText(note.getFileName());
			noteImage.setImageDrawable(note.getImage());
			
			return noteView;
		}
		
	}
	
}