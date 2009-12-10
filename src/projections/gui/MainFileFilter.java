package projections.gui;

import java.io.File;

class MainFileFilter 
    extends javax.swing.filechooser.FileFilter
{
    public boolean accept(File dir) {
	if ((dir.getName().endsWith(".sts") && dir.isFile()) ||
	    dir.isDirectory()) {
	    return true;
	} else {
	    return false;
	}
    }

    public String getDescription() {
	return "Select .sts file to load";
    }
}
