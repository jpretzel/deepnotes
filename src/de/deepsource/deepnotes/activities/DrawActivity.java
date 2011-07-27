package de.deepsource.deepnotes.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.graphics.Color;
import android.os.Bundle;
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
		case (R.id.draw_menu_newnote): {
			drawView.clearNote();
			return true;
		}

		case (R.id.draw_menu_backtomainmenu): {
			finish();
			return true;
		}
		}
		return false;
	}
	
	/**
	 * customizing the key listener
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if(keyCode == KeyEvent.KEYCODE_BACK){
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setMessage(R.string.save_dialog)
	    	       .setCancelable(false)
	    	       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	                finish();
	    	           }
	    	       })
	    	       .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	                dialog.cancel();
	    	           }
	    	       });
	    	builder.create().show();
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
}
