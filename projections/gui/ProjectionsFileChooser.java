package projections.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

/** Joshua Mostkoff Unger, unger1@uiuc.edu
 *  Parallel Programming Laboratory
 *
 *  ProjectionsFileChooser lets the user pick a directory or files, and
 *  then lets the user pick from all the sts files found in all the subdirs. 
 */
public class ProjectionsFileChooser
{
    Frame        owner_    = null;   // for making things modal
    private String       title_    = null;   // base title for dialogs
    private JFileChooser fChoose_  = null;   // user picks dirs to search
    JDialog      dialog_   = null;   // user picks files to use
    JList        list_     = null;   // stores found files
    int          listSize_ = 0;      // size of list_
	private Wait         wait_     = new Wait(true); // true if dialog waiting
    ProjectionsFileMgr fileMgr_ = null; //based on sts,get helper files

    // Allow file chooser to select multiple files
    public static final int MULTIPLE_FILES = 1;
    // Restrict file chooser to just single file
    public static final int SINGLE_FILE    = 2;
    // File Name returned by userSubSelect
    public String[] userSelect_returnVal;
    // Selected File indices in UserSubSelect
    // Had to do this because these are set in the 
    // ActionListener for JButton in dialog_
    public int [] userSelect_selected;

    public CallBack callback;

    /** 
     *  Default constructor 
     */
    public ProjectionsFileChooser(Frame owner)
    {
	this(owner, "ProjectionsFileChooser", MULTIPLE_FILES);
    }

    /** 
     *  Constructor.  Specify title of file chooser window.
     *  <type> should be ProjectionFileChooser.MULTIPLE_FILES or
     *                ProjectionFileChooser.SINGLE_FILE. 
     */
    public ProjectionsFileChooser(Frame owner, String title, int type)
    {
	if (!(type == MULTIPLE_FILES || type == SINGLE_FILE)) {
	    System.err.println("ProjectionFileChooser must init with:\n"+
			       "  ProjectionFileChooser.MULTIPLE_FILES or\n"+
			       "  ProjectionFileChooser.SINGLE_FILE");
	    System.exit(-1);
	}
	owner_   = owner;
	title_   = title;
	fChoose_ = initFileChooser(title_+": File(s) Open");
	dialog_  = initFileDialog(title_+": Choose Files");
    }

    /** 
     *  Given a bunch of strings, search for all sts files and set fileMgr.
     *  Return number of files found. 
     */
    public int getFiles(String[] args) throws IOException {
	File[] fileArray = new File[args.length];
	for (int i=0; i<args.length; i++) { fileArray[i] = new File(args[i]); }
	Vector files = filterFiles(fileArray, fChoose_.getFileFilter());
	fileMgr_ = new ProjectionsFileMgr(files);
	return fileMgr_.getNumFiles();
    }

    /** 
     *  Pop up first file chooser dialog, then the sts chooser dialog.
     *  Returns JFileChooser.APPROVE_OPTION if user chooses file, or
     *  JFileChooser.CANCEL_OPTION if user cancels or doesn't choose file 
     */
    public int showDialog(CallBack callback) {
	this.callback = callback;
	return showDialog();
    }

    public int showDialog() {
	int returnVal = fChoose_.showDialog(null, "Open/Search");
	try {
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
		Vector files = 
		    filterFiles(fChoose_.getSelectedFiles(), 
				fChoose_.getFileFilter());
		// ask user to subselect all files found
		userSubselect(files);
	    }
	} catch (Exception exc) {
	    System.err.println(exc.toString());
	    System.exit(-1);
	}
	return returnVal;
    }

    /** 
     *  Returns ProjectionFileMgr for the files opened.  If no files opened,
     *  returns null 
     */
    public ProjectionsFileMgr getProjectionsFileMgr() { 
	return fileMgr_; 
    }

    /** 
     *  Set up the GUI for the file chooser and return it. 
     */
    private JFileChooser initFileChooser(String title) {
	JFileChooser fc = new JFileChooser(title);
	String currDirStr = null;
	try { 
	    currDirStr = System.getProperty("PWD"); 
	} catch (SecurityException se) { 
	    // do nothing
	}
	if (currDirStr != null) {
	    fc.setCurrentDirectory(new File(currDirStr));
	}
	JTextArea instructions = 
	    new JTextArea("INSTRUCTIONS:\n" +
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
	fc.addChoosableFileFilter(new GrepFileFilter("sts", 
						     "STS Files (*sts*)"));
	return fc;
    }

    /** 
     *  Set up the dialog for sts file chooser and return it. 
     */
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
	JTextArea instructions = 
	    new JTextArea("All of the files below have been found\n"+
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
		    for (int i=0; i<selectAll.length; i++) { 
			selectAll[i] = i; 
		    }
		    list_.setSelectedIndices(selectAll);
		}
	    });
	// JButton button2 = new WaitButton("OK", wait_);
	JButton button2 = new JButton("OK");
	button2.addActionListener( new ActionListener() {
		public void actionPerformed(ActionEvent ae){
		    userSelect_selected = list_.getSelectedIndices();
		    userSelect_returnVal = 
			new String[userSelect_selected.length];
		    for (int j=0; j<userSelect_selected.length; j++) {
      			userSelect_returnVal[j] = 
			    (String)list_.getModel().getElementAt(userSelect_selected[j]);
		    }
		    try {
			fileMgr_ = 
			    new ProjectionsFileMgr(userSelect_returnVal);
		    } catch(Exception e) {
			e.printStackTrace();
		    }
		    dialog_.setVisible(false);
		    // **CW** this is another hack. There should be an
		    // interface to serve as a contract between
		    // ProjectionsFileChooser and the calling frame.
		    ((MultiRunWindow)owner_).dialogCallback();
		}
	    });
	JPanel panel = new JPanel();
	panel.add(button1);
	panel.add(button2);
	d.getContentPane().add(panel, BorderLayout.SOUTH);
	return d;
    }

    /** 
     *  Recursively search through all the subdirectories and finds those
     *  files specified by the filter 
     */
    private Vector filterFiles(File[] files, 
			       javax.swing.filechooser.FileFilter filter)
    {
	Vector fileVector = new Vector();
	Vector fileList = new Vector(files.length);
	for (int i=0; i<files.length; i++) { 
	    fileList.addElement(files[i]); 
	}
	recurseFilterFiles(fileList, fileVector, filter);
	return fileVector;
    }

    /** 
     *  Given a list of files in fileList, expand the directories and filter
     *  for "sts" files 
     */
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

    /** 
     *  From the filtered files, now ask the user to pick which ones they
     *  really want. 
     */
    private String[] userSubselect(Vector files) {
	String[] filesStr = new String[files.size()];
	int[] selectAll = new int[files.size()];
	
	for (int i=0; i<filesStr.length; i++) {
	    File file = (File) files.elementAt(i);
	    try {
		filesStr[i]=file.getCanonicalPath();
	    } catch (IOException e) {
		System.out.println("exception in USersubselect");
		filesStr[i]="IOException index "+i; }
	    selectAll[i] = i;
	}

	list_ = new JList(filesStr);
	listSize_ = filesStr.length;
	list_.setSelectedIndices(selectAll);
	dialog_.getContentPane().add(new JScrollPane(list_), 
				     BorderLayout.CENTER);
	dialog_.setVisible(true);
	return userSelect_returnVal;
    }
}
