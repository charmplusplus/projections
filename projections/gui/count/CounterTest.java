package projections.gui.count;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import projections.gui.*;

/** Joshua Mostkoff Unger, unger1@uiuc.edu
 *  Parallel Programming Laboratory
 * 
 *  CounterTest is just a test of the Performance Counter display system. */
public class CounterTest
{
  // TEST MULTIPLE FILE OPENS!!!

  /** query for user selections and set up the file manager */
  public static void main(String[] args) {
    CounterFrame f = new CounterFrame();
    ProjectionsFileMgr fileMgr = null;
    try {
      if (args.length==0) {
	ProjectionsFileChooser fc = 
	  new ProjectionsFileChooser(f, "Performance Counter Analysis", 
				     ProjectionsFileChooser.MULTIPLE_FILES);
	System.out.println("fc "+fc);
	if (fc == null) { System.out.println("UH OH"); }
	if (fc.showDialog()==JFileChooser.APPROVE_OPTION) {
	  fileMgr = fc.getProjectionsFileMgr();
	}
	else { 
	  System.out.println("No files chosen!"); 
	  System.exit(0);
	}
      }
      else { fileMgr = new ProjectionsFileMgr(args); }
      fileMgr.printSts();
      f.setFileMgr(fileMgr);
      f.setSize(800,600);
      f.setVisible(true);
      f.loadFiles();
      f.sortByColumn(1);

      /*
      // try again (to test gui)
      if (fc.showDialog()==JFileChooser.APPROVE_OPTION) {
	System.out.println("Chose the files!\n");
	ProjectionsFileMgr fileMgr = fc.getProjectionsFileMgr();
	fileMgr.printSts();
      }
      else { System.out.println("No files chosen!\n"); }
      */
    }
    catch(Exception exc) { ProjectionsFileChooser.handleException(f, exc); }
  }
}
