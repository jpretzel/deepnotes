package de.deepsource.deepnotes;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.TextView;

public class DeepnotesActivity extends FragmentActivity {

	private ViewPager notesPager;
	private NotesPagerAdapter notesPagerAdapter;
	private static Context cxt;

	/** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        notesPager = (ViewPager) findViewById(R.id.notesPager);
        cxt = this;
        notesPagerAdapter = new NotesPagerAdapter();
		notesPager.setAdapter(notesPagerAdapter);
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuInflater inflater = new MenuInflater(cxt);
		inflater.inflate(R.menu.main_menu, menu);
		
		return true;
	}

	private static class NotesPagerAdapter extends PagerAdapter {

		@Override
		public void startUpdate(View arg0) {

		}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1) {

		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == ((TextView) arg1);
		}

		@Override
		public Object instantiateItem(View arg0, int arg1) {
			TextView tv = new TextView(cxt);
			tv.setText("Slide " + arg1);
			((ViewPager) arg0).addView(tv, 0);
			return tv;
		}

		@Override
		public int getCount() {
			return 4;
		}

		@Override
		public void finishUpdate(View arg0) {

		}

		@Override
		public void destroyItem(View arg0, int arg1, Object arg2) {
			((ViewPager) arg0).removeView((TextView) arg2);
		}
	}
	
}