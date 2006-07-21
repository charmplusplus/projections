package projections.analysis;

import projections.gui.MainWindow;

/**
 *  ProjMain.java
 *  7/21/2006
 *
 *  The Main routine for projections (used to be MainWindow.java).
 *
 */

public class ProjMain {

    // ** System-level variables ** 
    public static double CUR_VERSION = 4.0;
    public static boolean IGNORE_IDLE = false;
    public static boolean BLUEGENE = false;
    public static int BLUEGENE_SIZE[] = new int[3];

    // **CW** workaround to print details on system usage where too many
    // entry methods prevent proper analysis (like in cpaimd).
    public static boolean PRINT_USAGE = false;

    public static void help()
    {
	System.out.println();
	System.out.println("Usage: projections [options] [sts-filename]");
	System.out.println();
	System.out.println("-h --help: show this page");
	System.out.println("-V --version: show Projections version");
	System.out.println("-u --use-version <ver>: use old version format");
	System.out.println("-no-idle: ignore idle time in analysis");
	System.out.println("-bgsize <x> <y> <z>: bluegene torus emulation");
	System.out.println("-print_usage: details written to stdout when " +
			   "viewing usage profiles.");
	System.out.println();
	System.exit(0);
    }

    public static void main(String args[])
    {
        int i=0;
	String loadSts=null;
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
	    } else if (args[i].equals("-print_usage")) {
		PRINT_USAGE = true;
	    } else /* Expected Sts Filename */ {
		loadSts=args[i];
	    }
	    i++;
	}

	MainWindow f = new MainWindow();
	f.pack();
	f.setTitle("Projections");
	f.setVisible(true);

	// Load Data if specified on command line
	if (loadSts!=null) { 
	    f.openFile(loadSts); 
	}
    }
}
