package projections.misc;

import projections.analysis.*;
import projections.gui.*;

import java.io.*;
import javax.swing.*;

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

    // stsReaders is unsorted by numPEs whilst the rest of this module has
    // their data arrays sorted. Therefore, a mapping has to be maintained
    // in the event that the stsReader has to be accessed.
    private int sortedStsMap[];
    
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
    
    // Names. epNames should be common to all runs or bad things will
    // happen.
    private String[] epNames;
    private String[] runNames;

    /**
     *  Constructs the sets of StsReaders and SummaryReaders (which on 
     *  construction, reads the appropriate files and holds its data).
     *  These entities can then be probed for information.
     */
    public MultiRunData(String listOfStsFilenames[]) 
	throws IOException
    {
	try {
	    numRuns = listOfStsFilenames.length;

	    // check for empty array and throw exception
	    if (numRuns == 0) {
		throw new IOException("MultiRunData cannot be initialized " +
				      "with zero runs!");
	    }

	    stsReaders = new StsReader[numRuns];
	    int pesPerRun[] = new int[numRuns];

	    // The run sequence supplied by the sts file list MUST BE
	    // sorted  in order of number of PEs.
	    // For now, this will have to suffice. Perhaps in future
	    // we'd be able to find a flexible (and meaningful) way to 
	    // deal with arbitrary sets of data.
	    //
	    // **NOTE** - 7/31/2004. A hack to deal with ActivityManagers
	    // by using a boolean flag to tell StsReader that it is being
	    // invoked by Multirun (which does not understand the concept
	    // of the Analysis static object).
	    for (int run=0; run<numRuns; run++) {
		stsReaders[run] =
		    new StsReader(listOfStsFilenames[run], true);
		pesPerRun[run] = stsReaders[run].getProcessorCount();
	    }
	    // ensure that both the stsReaders array and pesPerRun array
	    // are sorted and consistent.
	    sortedStsMap = MiscUtil.sortAndMap(pesPerRun);
	    MiscUtil.applyMap(stsReaders, sortedStsMap);

	    // ***** Apply consistency checks ***** 

	    // If summary files exist, read those first
	    // else read log files. This must be uniformly true for all
	    // runs. The PE file set need not be complete ... we'll work
	    // with whatever information we have and approximate from there.
	    boolean hasSummary = true;
	    boolean hasLog = true;
	    for (int run=0; run<numRuns; run++) {
		hasSummary = (hasSummary && stsReaders[run].hasSumFiles());
		hasLog = (hasLog && stsReaders[run].hasLogFiles());
	    }

	    // there has to be at least one run and all sts files have to
	    // agree on the number of entries (or we will be comparing
	    // oranges with apples)
	    numEPs = stsReaders[0].getEntryCount();
	    for (int run=1; run<numRuns; run++) {
		if (numEPs != stsReaders[run].getEntryCount()) {
		    System.err.println("Error! Incompatible data sets!");
		    System.exit(-1);
		}
	    }
	    // acquiring epNames and run names from the first run since
	    // the rest are consistent.
	    epNames = new String[numEPs];
	    for (int ep=0; ep<numEPs; ep++) {
		epNames[ep] = stsReaders[0].getEntryNames()[ep][0];
	    }

	    // generating the (somewhat) human-readable names of runs.
	    runNames = new String[numRuns];
	    for (int run=0; run<numRuns; run++) {
		runNames[run] = "(" + pesPerRun[run] + 
		    ")" + "[" + 
		    stsReaders[run].getMachineName() + "]";
	    }

	    // setup the base Table data for the readers to fill in.
	    dataTable = 
		new double[NUM_TYPES][numRuns][numEPs];
	    // setup the wall times for each of the runs.
	    runWallTimes = new double[numRuns];

	    // ***** Read data files. *****
	    // Solution to the horrendous memory usage: we only require
	    // the summarized information. Hence only one reader is
	    // required at any one time.
	    ProgressMonitor progressBar;
	    if (hasSummary) {
		GenericSummaryReader reader;
		OrderedIntList validPEs;
		for (int run=0; run<numRuns; run++) {
		    int numPE = pesPerRun[run];
		    validPEs = 
			stsReaders[run].getValidProcessorList(StsReader.SUMMARY);
		    validPEs.reset();
		    // approximates any incomplete data by scaling the values
		    // actually read by a scale factor.
		    double scale = numPE/(validPEs.size()*1.0);
		    progressBar =
			new ProgressMonitor(Analysis.guiRoot, 
					    "Reading summary Data for run " +
					    run + " of " + numRuns,
					    "", 0, validPEs.size());
		    // faster response time would be better in this case.
		    progressBar.setMillisToDecideToPopup(100);
		    // lower time threshold would also be helpful.
		    progressBar.setMillisToPopup(1000);
		    int count = 0;
		    while (validPEs.hasMoreElements()) {
			int pe = validPEs.nextElement();
			if (!progressBar.isCanceled()) {
			    progressBar.setNote("Reading Processor " +
						 pe + " data.");
			    progressBar.setProgress(count);
			} else {
			    // not the best thing to do, but will suffice
			    // until a more elegant system is in place.
			    System.err.println("Fatal error! Multirun " +
					       " cannot function without " +
					       " a complete read!");
			    System.exit(-1);
			}
			reader = 
			    new GenericSummaryReader(stsReaders[run].getSumName(pe),
						     Analysis.getVersion());
			for (int ep=0; ep<numEPs; ep++) {
			    dataTable[TYPE_TIME][run][ep] += 
				reader.epData[ep][GenericSummaryReader.TOTAL_TIME] * scale;
			    dataTable[TYPE_TIMES_CALLED][run][ep] +=
				reader.epData[ep][GenericSummaryReader.NUM_MSGS] * scale;
			}
			runWallTimes[run] += reader.numIntervals *
			    reader.intervalSize * 1000000.0 * scale;
			count++;
		    }
		    progressBar.close();
		}
	    } else if (hasLog) {
	    } else {
		// no data available!!! BAD!!!
		System.err.println("No data available! Catastrophic error!");
		System.exit(-1);
	    }
	} catch (IOException e) {
	    throw new IOException("MultiRun data read failed: " + 
				  Character.LINE_SEPARATOR + e);
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

