package de.deepsource.deepnotes.util.models;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;

public class Note {
	
	private String fileName;
	private BitmapDrawable image;
	
	public Note(String fileName) {
		this.fileName = fileName;
		
		File imageFile = new File(fileName);
		if (imageFile.exists()) {
			Bitmap bitmap = BitmapFactory.decodeFile(fileName);
			
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			int newWidth = width / 2;
			int newHeight = height / 2;
			
			float scaleWidth = ((float) newWidth) / width;
			float scaleHeight = ((float) newHeight) / height;
			
			Matrix matirx = new Matrix();
			matirx.postScale(scaleWidth, scaleHeight);
			
			Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matirx, true);
			
			image = new BitmapDrawable(resizedBitmap);
		}
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public BitmapDrawable getImage() {
		return image;
	}
	
	public void setImage(BitmapDrawable image) {
		this.image = image;
	}
	
	
}
