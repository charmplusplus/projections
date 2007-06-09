// UNUSED FILE


//package projections.gui.count;
//
//import java.awt.*;
//import java.awt.event.*;
//import java.io.*;
//import javax.swing.*;
//
//import projections.gui.*;
//
///** Joshua Mostkoff Unger, unger1@uiuc.edu
// *  Parallel Programming Laboratory
// * 
// *  CounterTest is just a test of the Performance Counter display system. */
//public class CounterTest
//{
//  // TEST MULTIPLE FILE OPENS!!!
//
//  /** query for user selections and set up the file manager */
//  public static void main(String[] args) {
//    CounterFrame f = new CounterFrame();
//    ProjectionsFileMgr fileMgr = null;
//    try {
//      ProjectionsFileChooser fc =
//	new ProjectionsFileChooser(f, "Performance Counter Analysis",
//				   ProjectionsFileChooser.MULTIPLE_FILES);
//      if (args.length==0) {
//	if (fc.showDialog()==JFileChooser.APPROVE_OPTION) {
//	  fileMgr = fc.getProjectionsFileMgr();
//	}
//	else {
//	  System.out.println("No files chosen!");
//	  System.exit(0);
//	}
//      }
//      else {
//	boolean test = false;
//	for (int i=0; i<args.length && !test; i++) {
//	  File file = new File(args[i]);
//	  if (file.isDirectory()) { test = true; }
//	  else if (!args[i].startsWith("./") ||
//		   !args[i].startsWith("/") ||
//		   !args[i].startsWith("../")) {
//	    args[i] = "./"+args[i];
//	  }
//	}
//	if (test) { fc.getFiles(args); }
//	fileMgr = (test) ?
//	  fc.getProjectionsFileMgr() : new ProjectionsFileMgr(args);
//      }
//      fileMgr.printSts();
//      f.setFileMgr(fileMgr);
//      f.setSize(800,600);
//      f.loadFiles();
//      f.sortByColumn(1);
//      f.setVisible(true);
//
//      /*
//      // try again (to test gui)
//      if (fc.showDialog()==JFileChooser.APPROVE_OPTION) {
//	System.out.println("Chose the files!\n");
//	ProjectionsFileMgr fileMgr = fc.getProjectionsFileMgr();
//	fileMgr.printSts();
//      }
//      else { System.out.println("No files chosen!\n"); }
//      */
//    }
//    catch(Exception exc) { ProjectionsFileChooser.handleException(f, exc); }
//  }
//}
