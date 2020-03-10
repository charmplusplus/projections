package projections.analysis;

import java.io.File;

import javax.swing.JOptionPane;

import projections.gui.MainWindow;
import projections.analysis.Analysis;

/**
 *  ProjMain.java
 *  7/21/2006
 *
 *  The Main routine for projections (used to be MainWindow.java).
 *
 */

public class ProjMain {

    // ** System-level variables ** 
    // CUR_VERSION indicates what logs this version of Projections
    // is capable of reading. Any logs that are of a higher version
    // cannot be read and this will be indicated by an unrecoverable
    // error when attempted.
    public static double CUR_VERSION = 10.0;
    public static boolean IGNORE_IDLE = false;
    public static boolean BLUEGENE = false;
    public static int BLUEGENE_SIZE[] = new int[3];

    // **CW** kind of a hack to allow users to specify initial summary
    // window data.
    public static int SUM_START_INT = 0;
    public static int SUM_END_INT = 0;
    public static long SUM_INT_SIZE = 0;
    public static boolean SUM_OVERRIDE = false;

    // **CW** workaround to print details on system usage where too many
    // entry methods prevent proper analysis (like in cpaimd).
    public static boolean PRINT_USAGE = false;

    // **CW** My little going-away joke.
    public static boolean FUNNY = false;

    // Analysis-specific global constants
    public static final int NUM_TYPES = 5;
    public static final int LOG = 0;
    public static final int SUMMARY = 1;
    public static final int SUMDETAIL = 2;
    public static final int DOP = 3;
    public static final int SUMACC = 4;

    public static MainWindow mainWindow = null;

    private static void help()
    {
	System.out.println();
	System.out.println("Usage: projections [options] [sts-filename]");
	System.out.println();
	System.out.println("-h --help: show this page");
	System.out.println("-V --version: show Projections version");
	System.out.println("-u --use-version <ver>: use old version format");
	System.out.println("--exit: exit Projections after loading input file");
	System.out.println("-no-idle: ignore idle time in analysis");
	System.out.println("-bgsize <x> <y> <z>: bluegene torus emulation");
	System.out.println("-print_usage: details written to stdout when " +
			   "viewing usage profiles.");
	System.out.println();
	System.exit(0);
    }

    public static void shutdown(int code) {
	if (mainWindow != null) {
	    mainWindow.shutdown();
	} 
	System.exit(code);
    }

    /** Intialize everything so gui choices can be made and command line driven tools can do whatever they need. 
     * */
    public static void startup(String args[]){
    	
    	/// The sts file to load
    	String loadSts=null;
    	boolean done = false;
	// Exit projections after loading file (for testing)
	boolean doExitAfterFileLoad = false;
    	// If no sts file is given, then we will search in the current directory to find sts files
    	if(args.length == 0){
    		File currentdir =new File (".");
    		File [] allChildren = currentdir.listFiles();
    		if(allChildren != null){
    			for(int i=0;i<allChildren.length && !done;i++){
    				File child = allChildren[i];
    				if(child.isFile()){
    					String name = child.getName();
    					if(name.endsWith(".sts") && ! name.endsWith(".sum.sts") ){
    						int choice = JOptionPane.showConfirmDialog(null, 
	    								 new String("<html><body>You didn't specify a file to open.<br>Do you want to open <b>" + name + "</b> from the current directory?</body></html>"),
	    								 new String("Question"),
	    								 JOptionPane.YES_NO_OPTION);
    						if(choice == JOptionPane.YES_OPTION){
    							loadSts = child.getAbsolutePath();
    							done = true;
    						} 
    					}
    				}
    			}
    		}

    	}
    	
    	int i=0;

    	while (i < args.length) {
    		if (args[i].equals("-h") ||
    				args[i].equals("--help")) {
    			help();
    		}
    		else if (args[i].equals("-V") ||
    				args[i].equals("--version")) {
    			System.out.println("Projections version: " + CUR_VERSION);
    			System.exit(0);
    		}
            else if (args[i].equals("--exit")) {
                doExitAfterFileLoad = true;
            }
    		else if (args[i].equals("-u") ||
    				args[i].equals("-use-version")) {
    			i++;
    			if (i==args.length) help();
    			double useVersion = Double.parseDouble(args[i]);
    			if (useVersion > CUR_VERSION) {
    				System.out.println("Unable to use version " + useVersion +
    						"! Maximum supported version on " +
    						"this binary is " + CUR_VERSION + ".");
    				System.exit(1);
    			}
    			CUR_VERSION = useVersion;
    		} else if (args[i].equals("-no-idle")) {
    			IGNORE_IDLE = true;
    		} else if (args[i].equals("-bgsize")) {
    			i++;
    			BLUEGENE_SIZE[0] = Integer.parseInt(args[i]);
    			i++;
    			BLUEGENE_SIZE[1] = Integer.parseInt(args[i]);
    			i++;
    			BLUEGENE_SIZE[2] = Integer.parseInt(args[i]);
    			BLUEGENE = true;
    		} else if (args[i].equals("-summary")) {
    			i++;
    			SUM_START_INT = Integer.parseInt(args[i]);
    			i++;
    			SUM_END_INT = Integer.parseInt(args[i]);
    			i++;
    			SUM_INT_SIZE = Long.parseLong(args[i]);
    			SUM_OVERRIDE = true;
    		} else if (args[i].equals("-print_usage")) {
    			PRINT_USAGE = true;
    		} else if (args[i].equals("-humor")) {
    			FUNNY = true;
    		} else /* Expected Sts Filename */ {
    			loadSts=args[i];
    		}
    		i++;
    	}

	if (doExitAfterFileLoad) {
		if (loadSts == null) {
			System.out.println("Error: --exit specified but no sts filename given!");
			System.exit(2);
		}
		Analysis a = new Analysis();
		try {
			MainWindow.CUR_VERSION = CUR_VERSION;
			a.initAnalysis(loadSts, null);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}

    	mainWindow = new MainWindow();
    	mainWindow.pack();
    	mainWindow.setTitle("Projections");
    	mainWindow.setVisible(true);

	// Load Data if specified on command line
	if (loadSts!=null) {
		mainWindow.openFile(loadSts);
	}

    }


    public static void main(String args[])
    {	
    	startup(args);
    }
}
