package projections.gui;

import java.io.*;
import javax.swing.filechooser.*;

/** Simple class to use in JFileChooser */
public class GrepFileFilter extends javax.swing.filechooser.FileFilter {
  /** Contructor */
  public GrepFileFilter(String grep, String description) { 
    grep_ = grep; 
    description_ = (description==null) ?
      "GrepFileFilter (*"+grep_+"*)" : description; 
  }
  /** Return true if this file should be shown in file chooser */
  public boolean accept(File f) {
    if (grep_ == null) { return true; }
    if (f.isDirectory()) { return true; }
    if (f.getName().lastIndexOf(grep_) != -1) { return true; }
    return false;
  }
  /** Return description for file chooser */
  public String getDescription() { return description_; }

  private String grep_        = null;  // string to be matched
  private String description_ = null;  // string to display in pull-down window
}
