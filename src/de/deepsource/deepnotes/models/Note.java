package de.deepsource.deepnotes.models;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

public class Note {
	
	private String created;
	private BitmapDrawable thumbnail;
	private String fileName;
	
	public Note(String path) {
		// get filename from path
//		fileName = new File(path).getName();
		fileName = path.substring(path.lastIndexOf('/') + 1);
		
		// get a date from filename
		created = fileName.substring(0, fileName.lastIndexOf('.'));
		Date dateCreated = new Date(Long.parseLong(created));
		// TODO: localized pattern
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy - HH:mm");
		created = sdf.format(dateCreated);
		
		File imageFile = new File(path);
		if (imageFile.exists()) {
			thumbnail = new BitmapDrawable(BitmapFactory.decodeFile(path));
		}
	}
	
	public String getCreated() {
		return created;
	}
	
	public BitmapDrawable getThumbnail() {
		return thumbnail;
	}
	
	public String getFileName() {
		return fileName;
	}
	
}
