package projections.gui;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Joshua Mostkoff Unger, unger1@uiuc.edu
 *  Parallel Programming Laboratory
 * 
 *  ProjectionsFileChooser lets the user pick a directory or files, and 
 *  then lets the user pick from all the sts files found in all the subdirs. */
public class ProjectionsFileChooser
{
  /** Allow file chooser to select multiple files */
  public static final int MULTIPLE_FILES = 1;  
  /** Restrict file chooser to just single file */
  public static final int SINGLE_FILE    = 2;

  // http://java.sun.com/products/jdk/1.1/docs/api/packages.html
  // http://java.sun.com/products/jfc/swingdoc-api-1.1/index.html
  // http://java.sun.com/docs/books/tutorial/uiswing/components/table.html
  // http://java.sun.com/docs/books/tutorial/2d/index.html

  // MAKE JDIALOG MODAL!
  // CHECK TO SEE IF THIS IS A SINGLE FILE LOAD
  // CHECK TO SEE IF THE FILE SELECTED IS JUST A FILE
  // CHECK TO SEE IF THERE IS ONLY ONE FILE???
  // WHAT IF NO FILES ARE FOUND???
  // WHAT HAPPENS IF USER CANCELS WINDOWS???
  // SAVE LAST DIRECTORY!?!?
  // STARTING WINDOW POSITIONS???
  // WORKING WINDOW WHEN SEARCHING THROUGH THINGS
  // MAKE HANDLE EXCEPTION CAUGHT IN PROJECTIONS!

  /** Default constructor */
  public ProjectionsFileChooser(Frame owner) 
    throws Exception
  { 
    this(owner, "ProjectionsFileChooser", MULTIPLE_FILES); 
  }

  /** Constructor.  Specify title of file chooser window.
   *  <type> should be ProjectionFileChooser.MULTIPLE_FILES or
   *                ProjectionFileChooser.SINGLE_FILE. */
  public ProjectionsFileChooser(Frame owner, String title, int type) 
    throws Exception
  { 
    if (!(type == MULTIPLE_FILES || type == SINGLE_FILE)) {
      throw new Exception("ProjectionFileChooser must init with:\n"+
			  "  ProjectionFileChooser.MULTIPLE_FILES or\n"+
			  "  ProjectionFileChooser.SINGLE_FILE\n");
    }
    owner_   = owner;
    title_   = title;
    type_    = type;
    fChoose_ = initFileChooser(title_+": File(s) Open");
    dialog_  = initFileDialog(title_+": Choose Files");
  }
  
  /** Given a bunch of strings, search for all sts files and set fileMgr.
   *  Return number of files found. */
  public int getFiles(String[] args) throws IOException {
    File[] fileArray = new File[args.length];
    for (int i=0; i<args.length; i++) { fileArray[i] = new File(args[i]); }
    Vector files = filterFiles(fileArray, fChoose_.getFileFilter());
    fileMgr_ = new ProjectionsFileMgr(files);
    return fileMgr_.getNumFiles();
  }

  /** Pop up first file chooser dialog, then the sts chooser dialog.
   *  Returns JFileChooser.APPROVE_OPTION if user chooses file, or
   *  JFileChooser.CANCEL_OPTION if user cancels or doesn't choose file */
  public int showDialog() {
    int returnVal = fChoose_.showDialog(null, "Open/Search");
    try {
      if (returnVal==JFileChooser.APPROVE_OPTION) {
	Vector files = filterFiles(
	  fChoose_.getSelectedFiles(), fChoose_.getFileFilter());
	// ask user to subselect all files found
	fileMgr_ = new ProjectionsFileMgr(userSubselect(files));
      }
    }
    catch (Exception exc) { handleException(owner_, exc); }
    return returnVal;
  }

  /** Returns ProjectionFileMgr for the files opened.  If no files opened,
   *  returns null */
  public ProjectionsFileMgr getProjectionsFileMgr() { return fileMgr_; }

  /** Open a window to display exception and then exit when window closed.
   *  Can call with Frame == null, but it won't be modal */
  public static void handleException(Frame f, Exception exc) {
    JDialog excDialog = new JDialog(f, true);
    excDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    excDialog.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) { System.exit(0); }
    });
    excDialog.getContentPane().add(new JScrollPane(new JTextArea(
      "Exception caught!\n"+exc.getMessage()+"\nSee sterr for stack trace\n")));
    exc.printStackTrace();
    excDialog.setSize(300, 200);
    excDialog.setVisible(true);
  }

  /** Set up the GUI for the file chooser and return it. */
  private JFileChooser initFileChooser(String title) {
    JFileChooser fc = new JFileChooser(title);
    String currDirStr = null;
    try { currDirStr = System.getProperty("PWD"); }
    catch (SecurityException se) { }
    if (currDirStr != null) {
      fc.setCurrentDirectory(new File(currDirStr));
    }
    JTextArea instructions = new JTextArea(
      "INSTRUCTIONS:\n"+
      "* Use the list to the left to choose\n"+
      "  either directories (to search) or sts files.\n"+
      "* If a single sts file is chosen, the\n"+
      "  run will be analyzed.\n"+
      "* If directories are chosen, the chosen\n"+
      "  dirs and all subdirs will be searched\n"+
      "  for any sts files, which can be further\n"+
      "  subselected.\n"+
      "* So, if several multi-run simulations are\n"+
      "  stored in several directories, just choose\n"+
      "  the parent directory and all the\n"+
      "  simulations will be found.\n"+
      "* NOTE: Use shift/control keys to select!");
    instructions.setEditable(false);
    fc.setAccessory(instructions);
    fc.setMultiSelectionEnabled(true);
    fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fc.addChoosableFileFilter(
      new GrepFileFilter("sts", "STS Files (*sts*)"));
    return fc;
  }

  /** Set up the dialog for sts file chooser and return it. */
  private JDialog initFileDialog(String title) {
    JDialog d = new JDialog(owner_, false);
    d.getContentPane().setLayout(new BorderLayout());
    d.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    d.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) { 
	dialog_.setVisible(false); 
      }
    });
    d.setTitle(title);
    d.setSize(600, 480);
    JTextArea instructions = new JTextArea(
      "All of the files below have been found\n"+
      "to match the file open filter in all of\n"+
      "the subdirectories searched.  Highlighted\n"+
      "files will be opened by Projections, so\n"+
      "further subselect only the files that are\n"+
      "desired.  NOTE: Can use control/shift keys\n"+
      "to subselect.\n");
    instructions.setEditable(false);
    d.getContentPane().add(instructions, BorderLayout.NORTH);
    JButton button1 = new JButton("Select All");
    button1.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
	int[] selectAll = new int[listSize_];
	for (int i=0; i<selectAll.length; i++) { selectAll[i] = i; }
	list_.setSelectedIndices(selectAll);
      }
    });
    JButton button2 = new WaitButton("OK", wait_);
    JPanel panel = new JPanel();
    panel.add(button1);
    panel.add(button2);
    d.getContentPane().add(panel, BorderLayout.SOUTH);
    return d;
  }
  
  /** Recursively search through all the subdirectories and finds those
   *  files specified by the filter */
  private Vector filterFiles(
    File[] files, javax.swing.filechooser.FileFilter filter) 
  {
    Vector fileVector = new Vector();
    Vector fileList = new Vector(files.length);
    for (int i=0; i<files.length; i++) { fileList.addElement(files[i]); }
    recurseFilterFiles(fileList, fileVector, filter);
    return fileVector;
  }

  /** Given a list of files in fileList, expand the directories and filter
   *  for "sts" files */
  private void recurseFilterFiles(Vector fileList, Vector fileVector, 
				  javax.swing.filechooser.FileFilter filter) 
  {
    for (int i=0; i<fileList.size(); i++) {
      File file = (File) fileList.elementAt(i);
      if (file.isDirectory()) {
	String[] dirFileStr = file.list();
	Vector newFileList = new Vector();
	for (int j=0; j<dirFileStr.length; j++) {
	  newFileList.addElement(new File(file, dirFileStr[j]));
	}
	recurseFilterFiles(newFileList, fileVector, filter);
      }
      else if (filter.accept(file)) { fileVector.addElement(file); }
    }
  }

  /** From the filtered files, now ask the user to pick which ones they 
   *  really want. */
  private String[] userSubselect(Vector files) {
    String[] filesStr = new String[files.size()];
    int[] selectAll = new int[files.size()];
    for (int i=0; i<filesStr.length; i++) {
      File file = (File) files.elementAt(i);
      try { filesStr[i]=file.getCanonicalPath(); }
      catch (IOException e) { filesStr[i]="IOException index "+i; }
      selectAll[i] = i;
    }
    list_ = new JList(filesStr);
    listSize_ = filesStr.length;
    list_.setSelectedIndices(selectAll);
    dialog_.getContentPane().add(new JScrollPane(list_), BorderLayout.CENTER);
    dialog_.setVisible(true);
    wait_.setValue(true);
    Thread thread = 
      new Thread() { public void run() { wait_.waitFor(false); } };
    thread.run();
    dialog_.setVisible(false);
    // thread waits for user's input, and only stops when user finished
    int[] selected = list_.getSelectedIndices();
    String[] returnVal = new String[selected.length];
    for (int j=0; j<selected.length; j++) {
      returnVal[j] = (String) list_.getModel().getElementAt(selected[j]);
    }
    return returnVal;
  }

  private Frame        owner_    = null;   // for making things modal
  private String       title_    = null;   // base title for dialogs
  private int          type_     = MULTIPLE_FILES;  // final result
  private JFileChooser fChoose_  = null;   // user picks dirs to search
  private JDialog      dialog_   = null;   // user picks files to use
  private JList        list_     = null;   // stores found files
  private int          listSize_ = 0;      // size of list_
  private Wait         wait_     = new Wait(true); // true if dialog waiting
  private ProjectionsFileMgr fileMgr_ = null; // based on sts, get helper files

}

