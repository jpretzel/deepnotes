package de.deepsource.deepnotes.models;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;

public class Note {
	
	private String path;
	private BitmapDrawable image;
	private String fileName;
	
	public Note(String path) {
		this.path = path;
		
		// get filename from path
//		fileName = new File(path).getName();
		fileName = path.substring(path.lastIndexOf('/') + 1);
		
		File imageFile = new File(path);
		if (imageFile.exists()) {
			Bitmap bitmap = BitmapFactory.decodeFile(path);
			
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
	
	public String getPath() {
		return path;
	}
	
	public BitmapDrawable getImage() {
		return image;
	}
	
	public String getFileName() {
		return fileName;
	}
	
}
