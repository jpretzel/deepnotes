/**
 * 
 */
package de.deepsource.deepnotes.utilities;

import java.io.File;

import android.content.Context;
import de.deepsource.deepnotes.application.Deepnotes;

/**
 * @author Jan Pretzel (jan.pretzel@deepsource.de)
 * @author Sebastian Ullrich (sebastian.ullrich@deepsource.de)
 * 
 *         This utility class handles the storage of the produced data.
 */
public final class IOManager {
	public static boolean deleteNote(Context context, String noteName) {
		// delete thumbnail
		File thumbnail = new File(context.getFilesDir()
				+ Deepnotes.SAVE_THUMBNAIL + noteName + ".png");
		if (!thumbnail.delete()) {
			return false;
		}

		// delete note images + folder
		File notePath = new File(context.getFilesDir() + "/" + noteName + "/");
		if (notePath.exists()) {
			// first delete files, because folder must be empty to be deleted
			File[] noteFiles = notePath.listFiles();

			for (File file : noteFiles) {
				if (!file.delete()) {
					return false;
				}
			}

			if (!notePath.delete()) {
				return false;
			}
		}

		return true;
	}
}
