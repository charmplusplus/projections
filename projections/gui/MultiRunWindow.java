package projections.gui;

import projections.misc.*;
import projections.analysis.*;

import java.awt.*;
import java.awt.event.*;

/**
 *  Written by Chee Wai Lee
 *  3/27/2002
 *
 *  ScalaVista is the main static class that will start any gui interface 
 *  (currently none) and analysis modules.
 *
 */

public class MultiRunWindow extends Frame 
    implements ActionListener
{
    // public static definitions
    // 1) log types
    public static final int SUMMARY            = 1;

    // 2) command line analysis types
    //    multiple types can be displayed together by logical operations
    // [[[[[ possible additions are: ANALYZE_AVG, ANALYZE_MAX etc ...]]]]]
    public static final int TOTAL_ANALYSIS_TAGS  = 1;
    public static final int ANALYZE_SUM        = 1;

    private static int argIdx;

    // options data
    private static String outputFullPathName="stdout";
    private static boolean isDefault;
    private static int logType = SUMMARY;

    // main or required data
    private static String baseName;
    private static String logSetPathNames[];

    // objects
    private static AccumulatedData accumulated;  // all data
    private static DataAnalyzer analyzer;

    //**************** GUI STUFF ***********************************

    private MainWindow mainWindow;
    private MultiRunControlPanel controlPanel;
    
    public MultiRunWindow(MainWindow mainWindow) 
    {
	this.mainWindow = mainWindow;

	addWindowListener(new WindowAdapter()
	    {
		public void windowClosing(WindowEvent e)
		{
		    Close();
		}
	    });
	setBackground(Color.lightGray);

	CreateLayout();
	pack();

	setTitle("Multi-Run Analysis");
	setVisible(true);
    }

    public void actionPerformed(ActionEvent evt)
    {
    }

    public void Close()
    {
	setVisible(false);
	dispose();
	mainWindow.CloseMultiRunWindow();
    }

    private void CreateLayout()
    {
	Panel p = new Panel();
	add("Center", p);
	p.setBackground(Color.gray);

	// setup the commandline (control) panel
	controlPanel = new MultiRunControlPanel(this);

	GridBagLayout      gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();

	p.setLayout(gbl);

	gbc.fill = GridBagConstraints.BOTH;
	Util.gblAdd(p, controlPanel, gbc, 0,0, 1,1, 2,1, 2,2,2,2); 
    }

    //**************** OLD TEXT-BASED CODES BELOW ******************

    /**
     *  Main starting point.
     *  The gui interface is used in the absence of appropriate command-line
     *  arguments.
     *
     *  Pre-conditions
     *  --------------
     *  Log & Summary file sets are assumed to be found in separate
     *  directories. The sets are also assumed to share the same base name
     *  and represent the same application.
     *
     *  Command-line interface format:
     *  ------------------------------
     *  <options> <basename> <log-set-path>*
     *
     *  options: 
     *   -o <output fullpathname> or --output
     *        outputs text report in specified file. Default stdout.
     *   -d <root fullpathname> or --default
     *        default log organization rooted at root.
     *   -b <log type> or --behavior
     *        specify type of file to analyze. Default "summary".
     *        valid types: summary. no support for actual logs yet.
     *   -h or --help
     *        displays help information.
     *
     *  IN THE PSEUDO-GUI VERSION, THE CONTROL PANEL CALLS THIS FUNCTION
     *  TO START OFF THE COMPUTATION.
     *
     */
    public static void processCommandLine(String args[]) {
	if (args.length == 0) {
	    // activate gui interface
	} else {
	    // process command line
	    argIdx = 0;
	    try {
		parseOptions(args);
		setBaseName(args);
		setLogSetPaths(args);
	    } catch (CommandLineException e) {
		System.err.println(e.toString());
		System.err.println();
		displayHelp();
		return; // only in the gui
	    }
	    try {
		// create accumulator object to read and summarize data
		accumulated = new AccumulatedData();
		accumulated.initialize(baseName, logSetPathNames, isDefault, 
				       logType);
		// create analyzer object to look at data and generate output
		analyzer = new DataAnalyzer();
		analyzer.analyzeData(accumulated, ANALYZE_SUM);
		analyzer.generateOutput(outputFullPathName, ANALYZE_SUM);
	    } catch (java.io.IOException e) {
		System.err.println(e.toString());
		// not in gui --- System.exit(-1);
		return;  // only in gui
	    }
	}
    }

    // Command-line processing methods
    private static void parseOptions(String args[]) 
	throws CommandLineException 
    {
	String argument = args[argIdx];
	while (isOption(argument)) {
	    // process options
	    if (argument.equals("-o") ||
		argument.equals("--output")) {
		argIdx++;  // consume option tag
		// exactly one option argument is expected.
		if (argIdx+1 > args.length || isOption(args[argIdx])) {
		    throw new CommandLineException("No output file specified");
		}
		outputFullPathName = args[argIdx++];
	    } else if (argument.equals("-d") ||
		       argument.equals("--default")) {
		argIdx++; // consume option tag
		// exactly one option argument is expected.
		if (argIdx+1 > args.length || isOption(args[argIdx])) {
		    throw new CommandLineException("No default root " + 
						   "pathname specified.");
		}
		isDefault = true;
		logSetPathNames = new String[1];
		logSetPathNames[0] = args[argIdx++];
	    } else if (argument.equals("-b") ||
		       argument.equals("--behavior")) {
		argIdx++; // consume option tag
		// exactly one option argument is expected.
		if (argIdx+1 > args.length || isOption(args[argIdx])) {
		    throw new CommandLineException("No behavior specified");
		}
		if (args[argIdx].equals("summary")) {
		    logType = SUMMARY;
		} else {
		    throw new CommandLineException("Invalid behavior - " +
						   logType);
		}
		argIdx++;
	    } else if (argument.equals("-h") ||
		       argument.equals("--help")) {
		// displayHelp();  // exits the system, so don't change argIdx
		// ugly fix to displaying help -- throw exception
		// when fully "gui-ized", will no longer exist.
		throw new CommandLineException("Displaying Help Information");
	    } else {
		throw new CommandLineException("Invalid option " + argument);
	    }
	    // check for more arguments/options
	    if (argIdx >= args.length) {
		// formats may change. It is not the job of the option parser
		// to detect that no arguments follow the option sequence.
		break;
	    }
	    argument = args[argIdx];
	}
    }

    private static void setBaseName(String args[]) 
	throws CommandLineException
    {
	if (argIdx >= args.length) {
	    throw new CommandLineException("Missing basename argument.");
	}
	if (isOption(args[argIdx])) {
	    throw new CommandLineException("options must be contigious.");
	}
	baseName = args[argIdx++];
    }

    private static void setLogSetPaths(String args[]) 
	throws CommandLineException
    {
	int numPaths = args.length - argIdx;
	if (isDefault) {
	    // do nothing, default set will be used.
	    // all log set arguments are ignored.
	    return;
	}
	if (numPaths <= 0) {
	    throw new CommandLineException("At least one log set path " +
					   "must be supplied.");
	}
	logSetPathNames = new String[numPaths];
	for (int i=0; i<numPaths; i++) {
	    if (isOption(args[argIdx])) {
		throw new CommandLineException("options must be contigious.");
	    }
	    logSetPathNames[i] = args[argIdx++];
	}
    }

    private static void displayHelp() {
	System.err.println("GUI Usage: not pertinent anymore");
	System.err.println();
	System.err.println("CmdLine Usage: <options> <basename> " + 
			   "<log-set-path>+");
	System.err.println();
	System.err.println("options:");
	System.err.println(" [-o|--output] <output-fullpathname>");
	System.err.println("     specify report output file. Default stdout");
	System.err.println(" [-d|--default] <root-fullpathname>");
	System.err.println("     default log organization rooted at " +
			   "root-fullpathname");
	System.err.println(" [-b|--behavior] <type of log file>");
	System.err.println("     default: summary");
	System.err.println("     valid: summary");
	System.err.println(" [-h|--help]");
	System.err.println("     this information");
	// not in gui ---- System.exit(0);
    }

    private static boolean isOption(String argument) {
	return argument.startsWith("-");
    }
}
