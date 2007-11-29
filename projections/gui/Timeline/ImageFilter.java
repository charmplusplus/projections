/* @file This file is based on the following files:
 *     http://java.sun.com/docs/books/tutorial/uiswing/examples/components/FileChooserDemo2Project/src/components/ImageFilter.java
 *     http://java.sun.com/docs/books/tutorial/uiswing/examples/components/FileChooserDemo2Project/src/components/Utils.java
 */

package projections.gui.Timeline;

import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.*; 

/** A file filter that only allows jpeg, jpg, or png file extensions */
public class ImageFilter extends FileFilter {

	/** Determine if a file's name has an appropriate jpeg extension */
	public boolean isJPEG(File f) {
        String extension = getExtension(f);
        if (extension != null) {
            return ( extension.equals("jpeg") || extension.equals("jpg") ); 
        }
        return false;
    }
	
	/** Determine if a file's name has an appropriate png extension */
	public boolean isPNG(File f) {
        String extension = getExtension(f);
        if (extension != null) {
            return ( extension.equals("png") ); 
        }
        return false;
    }
	
	
    //Accept all directories and all gif, jpg, tiff, or png files.
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String extension = getExtension(f);
        if (extension != null) {
            if (extension.equals("jpeg") ||
                extension.equals("jpg") ||
                extension.equals("png")) {
                    return true;
            } else {
                return false;
            }
        }

        return false;
    }

    //The description of this filter
    public String getDescription() {
        return "png or jpg";
    }
    
    /*
     * Get the extension of a file.
     */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }
    
    
}