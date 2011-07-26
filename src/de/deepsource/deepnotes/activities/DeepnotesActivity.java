package de.deepsource.deepnotes.activities;

import java.util.ArrayList;
import java.util.List;

import de.deepsource.deepnotes.R;
import de.deepsource.deepnotes.util.models.Note;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DeepnotesActivity extends FragmentActivity {

	private ArrayList<Note> notes;

	/** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        notes = new ArrayList<Note>();
        
        GridView notesView = (GridView) findViewById(R.id.notesView);
        NotesAdapter na  = new NotesAdapter(this, R.layout.note_item, notes);
        notesView.setAdapter(na);
        
        /*Note testNote = new Note("/deepnotes/test1.png");
        notes.add(testNote);
        na.notifyDataSetChanged();*/
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
		case (R.id.add_note):

			return true;
		}
		
		return false;
	}
	
	public class NotesAdapter extends ArrayAdapter<Note> {
		
		private int resource;

		public NotesAdapter(Context context, int resource, List<Note> notes) {
			super(context, resource, notes);
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
			noteImage.setImageBitmap(note.getImage());
			
			return noteView;
		}
		
	}
	
}