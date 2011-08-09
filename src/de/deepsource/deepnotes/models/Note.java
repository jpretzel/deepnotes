package de.deepsource.deepnotes.models;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.deepsource.deepnotes.application.Deepnotes;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

public class Note {
	
	private String created;
	private BitmapDrawable thumbnail;
	private String fileName;
	
	public Note(Context context, String name) {
		// get filename from path
//		fileName = new File(path).getName();
//		fileName = name.substring(name.lastIndexOf('/') + 1);
		
		fileName = name;
		
		// get a date from filename
		// remove suffix if there is one
		if (fileName.contains(".")) {
			fileName = fileName.substring(0, fileName.lastIndexOf('.'));
		}
		
		Date dateCreated = new Date(Long.parseLong(fileName));
		// TODO: localized pattern
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy - HH:mm");
		created = sdf.format(dateCreated);
		
		File imageFile = new File(context.getFilesDir() + Deepnotes.SAVE_THUMBNAIL + fileName + ".jpg");
		if (imageFile.exists()) {
			thumbnail = new BitmapDrawable(BitmapFactory.decodeFile(imageFile.toString()));
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
