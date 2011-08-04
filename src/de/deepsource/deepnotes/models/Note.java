package de.deepsource.deepnotes.models;

import java.io.File;

import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

public class Note {
	
	private String path;
	private BitmapDrawable thumbnail;
	private String fileName;
	
	public Note(String path) {
		this.path = path;
		
		// get filename from path
//		fileName = new File(path).getName();
		fileName = path.substring(path.lastIndexOf('/') + 1);
		
		File imageFile = new File(path);
		if (imageFile.exists()) {
			thumbnail = new BitmapDrawable(BitmapFactory.decodeFile(path));
		}
	}
	
	public String getPath() {
		return path;
	}
	
	public BitmapDrawable getThumbnail() {
		return thumbnail;
	}
	
	public String getFileName() {
		return fileName;
	}
	
}
