package projections.gui;

import java.io.*;

public class ColorFileFilter 
    extends javax.swing.filechooser.FileFilter
{
    public boolean accept(File dir) {
	if ((dir.getName().endsWith(".map") && dir.isFile()) ||
	    dir.isDirectory()) {
	    return true;
	} else {
	    return false;
	}
    }

    public String getDescription() {
	return ".map";
    }
}
