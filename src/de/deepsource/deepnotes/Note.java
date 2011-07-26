package de.deepsource.deepnotes;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Note {
	
	private String fileName;
	private Bitmap image;
	
	public Note(String fileName) {
		this.fileName = fileName;
		
		File imageFile = new File(fileName);
		if (imageFile.exists()) {
			image = BitmapFactory.decodeFile(fileName);
		}
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public Bitmap getImage() {
		return image;
	}
	
	public void setImage(Bitmap image) {
		this.image = image;
	}
	
	
}
