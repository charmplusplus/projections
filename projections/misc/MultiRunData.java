package projections.misc;

import projections.analysis.*;
import projections.gui.*;

import java.io.*;

/**
 *
 *  Written by Chee Wai Lee
 *  3/28/2002
 *  Updated:
 *    2/4/2003 - Chee Wai Lee. streamlined the code. Move the construction
 *               of the basic summed table from MultiRunDataAnalyzer.
 *
 *  MultiRunData is the main class encapsulating data read from multiple
 *  summary or log files.
 *
 */

public class MultiRunData 
{
    // IO reader objects (holds data after construction unless on exception)
    private StsReader stsReaders[];
    // sumReaders dim 1 - indexed by Run Log ID
    // sumReaders dim 2 - indexed by PE number
    private GenericSummaryReader sumReaders[][];
    
    // Data entries computed from IO reader objects summed across all
    // PEs. The data should be accessed via accessor methods.
    // Dimension 0 - indexed by data type (time, #msg, msgsize)
    // Dimension 1 - indexed by Run Log ID
    // Dimension 2 - indexed by Entry Point ID
    private double dataTable[][][];

    // accompanying static data for dataTable. These types are publically
    // published for use by the GUI and Data Analyzer(s).
    public static final int NUM_TYPES = 4;
    public static final int TYPE_TIME = 0;
    public static final int TYPE_TIMES_CALLED = 1;
    public static final int TYPE_NUM_MSG_SENT = 2;
    public static final int TYPE_SIZE_MSG = 3;    

    // Short names of data types associated with Multirun data. Can
    // be used by GUIs to automatically generate components like buttons
    // or radio boxes.
    private static final String typeNames[] =
    {"Execution Time", "Num Msgs Received", "Num Msgs Sent", "Msg Size"};

    // Non-standard data entries from summary files.
    // runTimes - The total absolute wall time the run took summed across
    //            all PEs. Effectively the total amount of work done +
    //            overhead.
    // Dimension 0 - indexed by Run Log ID
    private double runWallTimes[];
    
    // fixed statically determinable information after reading the
    // sts data.
    private int numRuns;
    private int numEPs;
    
    // Names
    private String[] epNames;
    private String[] runNames;

    // Create one statistics object for each data type read from
    // the summary format. These can be reused independently.
    private ProjectionsStatistics timeStats; 
    private ProjectionsStatistics numCallsStats;
    
    /**
     *  Constructs the sets of StsReaders and SummaryReaders (which on 
     *  construction, reads the appropriate files and holds its data).
     *  These entities can then be probed for information.
     */
    public MultiRunData(String listOfStsFilenames[]) 
	throws IOException
    {
	timeStats = new ProjectionsStatistics();
	numCallsStats = new ProjectionsStatistics();
	
	try {
	    numRuns = listOfStsFilenames.length;

	    // check for empty array and throw exception
	    if (numRuns == 0) {
		throw new IOException("MultiRunData cannot be initialized " +
				      "with zero runs!");
	    }

	    stsReaders = new StsReader[numRuns];
	    sumReaders = new GenericSummaryReader[numRuns][];
	    for (int run=0; run<numRuns; run++) {
		stsReaders[run] =
		    new StsReader(listOfStsFilenames[run]);
		int numPE = stsReaders[run].getProcessorCount();
		sumReaders[run] =
		    new GenericSummaryReader[numPE];
		for (int pe=0; pe<numPE; pe++) {
		    sumReaders[run][pe] =
			new GenericSummaryReader(getSumFilename(listOfStsFilenames[run],
								pe),
						 Analysis.getVersion());
		}
	    }
	    // there has to be at least one run and all sts files have to
	    // agree on the number of entries (or we will be comparing
	    // oranges with apples)
	    numEPs = stsReaders[0].getEntryCount();

	    // acquiring epNames and run names
	    epNames = new String[numEPs];
	    for (int ep=0; ep<numEPs; ep++) {
		epNames[ep] = stsReaders[0].getEntryNames()[ep][0];
	    }
	    
	    runNames = new String[numRuns];
	    for (int run=0; run<numRuns; run++) {
		runNames[run] = "(" + stsReaders[run].getProcessorCount() + 
		    ")" + "[" + stsReaders[run].getMachineName() + "]";
	    }

	    // begin computing information
	    computeBaseInformation();
	} catch (IOException e) {
	    throw new IOException("MultiRun data read failed: " + 
				  Character.LINE_SEPARATOR + e);
	}
    }
    
    /**
     *  This method acquires the summary filename from the
     *  sts filename.
     *
     *  **CW** BUggy ... cannot deal with normal log-generated sts files.
     *  Please fix.
     */
    private String getSumFilename(String stsFilename, int pe) {
	String  withoutSts = 
	    stsFilename.substring(0, stsFilename.lastIndexOf('.'));
	// behavior depends on whether the sts file is a summary based one
	// or a standard sts file.
	if (withoutSts.substring(withoutSts.lastIndexOf('.'), 
				 withoutSts.length()).equals(".sum")) {
	    return withoutSts.substring(0, withoutSts.lastIndexOf('.')) +
		"." + pe + ".sum";
	} else {
	    return withoutSts + "." + pe + ".sum";
	}
    }
    
    /**
     *  Computes data for each entry in dataTable from the readers using
     *  the inherited accumulate methods from ProjectionsData across PEs.
     *
     *  The simplest of such computations is the sum of all values. In
     *  future, however, we may want to compute simple statistics such
     *  as the mean and variance of the data across PEs.
     */
    private void computeBaseInformation() {
	dataTable = 
	    new double[NUM_TYPES][numRuns][numEPs];

	for (int run=0; run<numRuns; run++) {
	    for (int ep=0; ep<numEPs; ep++) {
		// gathering statistics across all PEs
		// make sure statistics objects are clean.
		timeStats.reset();
		numCallsStats.reset();
		for (int pe=0; pe<stsReaders[run].getProcessorCount(); pe++) {
		    // this is silly because of a design fault where
		    // GenericSummaryReader uses a reversed indexing scheme
		    // from this code.
		    timeStats.accumulate(sumReaders[run][pe].epData[ep][GenericSummaryReader.TOTAL_TIME]);
		    numCallsStats.accumulate(sumReaders[run][pe].epData[ep][GenericSummaryReader.NUM_MSGS]);
		}
		dataTable[TYPE_TIME][run][ep] = timeStats.getSum();
		dataTable[TYPE_TIMES_CALLED][run][ep] =
		    numCallsStats.getSum();
	    }
	}

	// other data - run total times
	runWallTimes = new double[numRuns];
	for (int run=0; run<numRuns; run++) {
	    timeStats.reset();
	    for (int pe=0; pe<stsReaders[run].getProcessorCount(); pe++) {
		// intervalSize in sumReaders is given in seconds. However,
		// we are working with microseconds.
		timeStats.accumulate(sumReaders[run][pe].numIntervals *
				     (sumReaders[run][pe].intervalSize*
				      1000000.0));
	    }
	    runWallTimes[run] = timeStats.getSum();
	}
    }

    // Accessor Methods

    /**
     *  Returns the number of Entry Points read by the readers.
     */
    public int getNumEPs() {
	return numEPs;
    }

    /**
     *  Returns the number of Runs read by the readers.
     */
    public int getNumRuns() {
	return numRuns;
    }

    /**
     *  Returns the data table given a certain type index.
     */
    public double[][] getData(int type) {
	return dataTable[type];
    }

    /**
     *  Get everything.
     */
    public double[][][] getData() {
	return dataTable;
    }

    /**
     *  Returns the entry point row given a type and a run ID.
     */
    public double[] getEPData(int type, int runID) {
	return dataTable[type][runID];
    }

    /**
     *  Convenience Method. Returns the Run column given a type and
     *  an Entry Point ID.
     *
     *  EFFICIENCY NOTE: This method should only be used if the Data
     *    Analyzer wishes to traverse the data columns repeatedly.
     *    Otherwise, it is more efficient to obtain the whole table 
     *    using getData.
     */
    public double[] getRunData(int type, int ep) {
	double[] returnData = new double[numRuns];

	for (int run=0; run<numRuns; run++) {
	    returnData[run] = dataTable[type][run][ep];
	}
	return returnData;
    }

    /**
     *  Returns the total amount of time spent by the application
     *  for each run summed across all PEs of that run. Effectively
     *  gives the total amount of work + overhead for the run.
     */
    public double[] getRunWallTimes() {
	return runWallTimes;
    }

    /**
     *  Returns an array of names corresponding to each EP ID.
     */
    public String[] getEPNames() {
	return epNames;
    }

    /**
     *  Returns an array of names corresponding to each Run ID.
     */
    public String[] getRunNames() {
	return runNames;
    }

    /**
     *  Returns the statically assigned string associated with the
     *  provided data type.
     */
    public static String getTypeName(int dataType) {
	return typeNames[dataType];
    }
}

