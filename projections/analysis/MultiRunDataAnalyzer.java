package projections.analysis;

import projections.gui.graph.*;
import projections.gui.*;
import projections.misc.*;

import java.io.*;
import java.util.*;

/**
 *  Written by Chee Wai Lee
 *  3/29/2002
 *  Updated:
 *    2/4/2003 - Chee Wai Lee. Streamlined some features.
 *
 *  MultiRunDataAnalyzer is the object that will analyze data read into 
 *  MultiRunData and produce intermediate data that can then be presented
 *  on a gui.
 *
 *  This includes the categorization of data according to the module's
 *  interpretation of the data.
 *
 *  **CW** NOTE TO SELF - seems very tightly coupled with MultiRunData.
 *  May well be appropriate to make data analyzers inherit from base data
 *  classes. 
 */
public class MultiRunDataAnalyzer {

    private MultiRunData data;

    // data table - acquired from MultiRunData
    private double dataTable[][][];

    // extra data entries not directly read from the File formats.
    // Dimension 0 - indexed by data type (eg. time)
    // Dimension 1 - indexed by Run Log ID
    // Dimension 2 - indexed by Special Entry ID (eg. Idle time)
    private double extraTable[][][];

    // accompanying static fields for extraTable. The information is
    // publically published for use by the GUI and Data Analyzer(s).
    public static final int NUM_EXTR_TYPES = 1;
    public static final int EXTR_TYPE_TIME = 0;

    public static final int NUM_EXTR_ENTRIES = 1;
    public static final int EXTR_OVERHEAD = 0;

    // special statistical information. Used by the analyzer to deduce
    // certain properties of the runs.
    // runTimeSums - indexed by Run Log ID
    private double runTimeSum[];
    // epTimeMean - indexed by Entry Point ID
    private double epTimeMean[];
    // epTimeVariance - indexed by Entry Point ID
    private double epTimeVariance[];

    // statistical information on the extra information.
    // extraTimeMean - indexed by extra entry ID
    private double extraTimeMean[];
    // extraTimeVariance - indexed by extra entry ID
    private double extraTimeVariance[];

    // Categorization support data structure
    // Dimension 0 - indexed by data type.
    // Dimension 1 - indexed by the category ID. 
    // Each vector is the list of EPs that fall into the category.
    private Vector categories[][];
    private String catNames[];

    // accompanying static data needs to be published for the TableModel
    private static final int NUM_CATEGORIES = 4;
    private static final int CAT_EP_NO_CHANGE = 0;
    private static final int CAT_EP_INSIGNIFICANT = 1;
    private static final int CAT_EP_CHANGE = 2;
    private static final int CAT_OVERHEAD_IDLE = 3;

    // base data information
    private int numRuns;
    private int numEPs;
    
    // names - for publishing onto the appropriate GUI.
    private String epNames[];
    private String runNames[];

    // Create one statistics object for each data type read from
    // the summary format. These can be reused independently.
    private ProjectionsStatistics timeStats; 
    private ProjectionsStatistics numCallsStats;
    
    public MultiRunDataAnalyzer(MultiRunData data) {

	timeStats = new ProjectionsStatistics();
	numCallsStats = new ProjectionsStatistics();

	this.data = data;
	numEPs = data.getNumEPs();
	numRuns = data.getNumRuns();

	epNames = data.getEPNames();
	runNames = data.getRunNames();

	dataTable = data.getData();

	// perform analysis phase 1 - compute basic statistical info
	computeDerivedInformation();
	// perform analysis phase 2 - compute any extra information
	computeExtraInformation();
	// perform analysis phase 3 - compute extra statistical info
	computeExtraDerivedInformation();

	constructCategories();
    }

    // ****** Data analysis methods *******

    /**
     *  Computes the derived statistics from the base information. We can
     *  thus compute things like the total time used across all EPs, across
     *  all PEs.
     *
     *  Derived information may be obtained from the extra information.
     *  Hence, they use a combined information table.
     */
    private void computeDerivedInformation() {
	// compute runTimeSum - This does NOT include idle time.
	runTimeSum = new double[numRuns];
	for (int run=0; run<numRuns; run++) {
	    // make sure statistics object is clean.
	    timeStats.reset();
	    timeStats.accumulate(dataTable[MultiRunData.TYPE_TIME][run]);
	    runTimeSum[run] = timeStats.getSum();
	}
	// compute epTimeMean and epTimeVariance
	epTimeMean = new double[numEPs];
	epTimeVariance = new double[numEPs];
	for (int ep=0; ep<numEPs; ep++) {
	    // make sure statistics object is clean.
	    timeStats.reset();
	    for (int run=0; run<numRuns; run++) {
		timeStats.accumulate(dataTable[MultiRunData.TYPE_TIME][run][ep]);
	    }
	    epTimeMean[ep] = timeStats.getMean();
	    epTimeVariance[ep] = timeStats.getVariance(epTimeMean[ep]);
	}
    }

    public void computeExtraInformation() {
	// Overhead + Idle time
	// initialize structure
	extraTable = 
	    new double[NUM_EXTR_TYPES][numRuns][NUM_EXTR_ENTRIES];

	double runWallTimes[] = data.getRunWallTimes();
	for (int run=0; run<numRuns; run++) {
	    extraTable[EXTR_TYPE_TIME][run][EXTR_OVERHEAD] =
		runWallTimes[run] - runTimeSum[run];
	}
    }

    public void computeExtraDerivedInformation() {
	// To be implemented if statistical information is
	// desired for the extra information. In this case, it
	// is a little tough to get that information.
    }

    // ******* Categorization methods for derived informatoin *******

    /**
     *  This method sets up the necessary categorization data structures
     *  before handing them to the "AI" for categorization of each
     *  Entry Point.
     */
    private void constructCategories() {
	// prepare categorization data structures
	categories = 
	    new Vector[MultiRunData.NUM_TYPES][NUM_CATEGORIES];
	catNames = new String[NUM_CATEGORIES];
	for (int cat=0; cat<NUM_CATEGORIES; cat++) {
	    catNames[cat] = getCategoryName(cat);
	}
	for (int type=0; type<MultiRunData.NUM_TYPES; type++) {
	    for (int category=0; category<NUM_CATEGORIES; category++) {
		categories[type][category] = new Vector();
	    }
	    categorize(type);
	}
    }

    /**
     *  This method allows us to put our hardcoding in one easy
     *  to monitor place.
     */
    private String getCategoryName(int categoryID) {
	switch (categoryID) {
	case CAT_EP_NO_CHANGE:
	    return "EPs with little change";
	case CAT_EP_INSIGNIFICANT:
	    return "Insignificant EPs";
	case CAT_EP_CHANGE:
	    return "EPs with change";
	case CAT_OVERHEAD_IDLE:
	    return "Idle time and System Overhead";
	default:
	    return "unknown category";
	}
    }

    public void categorize(int dataType) {
	// first, categorize the application's EPs
	for (int ep=0; ep<numEPs; ep++) {
	    // TEST #1 - Significance
	    // test to see if majority of runs shows ep is insignificant
	    int insigCount = 0;
	    for (int run=0; run<numRuns; run++) {
		if (dataTable[dataType][run][ep] < 0.01*runTimeSum[run]) {
		    insigCount++;
		}
	    }
	    if (insigCount > numRuns/2) {
		categories[dataType][CAT_EP_INSIGNIFICANT].add(new Integer(ep));
		break;
	    }
	    // TEST #2 - Change
	    // test to see if the values generally climbs or drops from run 
	    // to run. The current implementation is extremely crude and may
	    // form the basis for future automated performance analysis
	    // research.
	    //
	    // For example, the current scheme is unable to realize that a
	    // curve that is upward growing but tapers off is a good curve.
	    // This requires runs to be ordered in some fashion.
	    int incrementCount = 0;
	    int decrementCount = 0;
	    double avgDeviation = 0.0;
	    double startValue = dataTable[dataType][0][ep];
	    for (int run=1; run<numRuns; run++) {
		avgDeviation +=
		    Math.abs(dataTable[dataType][run][ep] - startValue);
		if (dataTable[dataType][run][ep] > startValue) {
		    incrementCount++;
		} else if (dataTable[dataType][run][ep] < startValue) {
		    decrementCount++;
		}
	    }
	    avgDeviation /= numRuns-1;
	    if (((incrementCount > numRuns*0.75) || 
		 (decrementCount > numRuns*0.75)) &&
		(avgDeviation > 0.05*epTimeMean[ep])) {
		categories[dataType][CAT_EP_CHANGE].add(new Integer(ep));
	    } else {
		categories[dataType][CAT_EP_NO_CHANGE].add(new Integer(ep));
	    }
	}
	// then, add special (extra) EPs to the appropriate category
	for (int entry=0; entry<NUM_EXTR_ENTRIES; entry++) {
	    switch (entry) {
	    case EXTR_OVERHEAD:
		categories[dataType][CAT_OVERHEAD_IDLE].add(new Integer(numEPs+EXTR_OVERHEAD));
		break;
	    }
	}
    }

    // **** Interface to Tables and Table Models ****
    
    /**
     *  This call is made by the table model for the appropriate category.
     *  As such, it supplies the index it needs data from.
     */
    public int getNumRows(int dataType, int categoryIndex) {
	return categories[dataType][categoryIndex].size();
    }
    
    /**
     *  The only reason we still pass in the dataType and categoryIndex
     *  is to provide the flexibility to return additional columns
     *  depending on the data type or category type.
     */
    public int getNumColumns(int dataType, int categoryIndex) {
	return numRuns;
    }

    public double getTableValueAt(int dataType, int categoryIndex, 
				  int row, int col) {
	int epIndex = 
	    ((Integer)categories[dataType][categoryIndex].elementAt(row)).intValue();
	// if extra info, use different array.
	if (epIndex >= numEPs) {
	    return extraTable[dataType][col][numEPs-epIndex];
	} else {
	    return dataTable[dataType][col][epIndex];
	}
    }

    /**
     *  Needed by MultiRunTables to begin constructing the individual
     *  tables.
     */
    public String[] getCategoryNames() {
	return catNames;
    }

    // **** Standard Interface to Graphs ****

    /**
     *   getDataSource takes the appropriate part of the analyzed data and
     *   constructs a DataSource object suitable for display on an 
     *   AreaGraphPanel.
     */
    public MultiRunDataSource getDataSource(int dataType) {
	double dataArray[][];

	String titleString = "";
	switch (dataType) {
	case MultiRunData.TYPE_TIME:
	    titleString = "Time taken";
	    break;
	case MultiRunData.TYPE_NUM_MSG_SENT:
	    titleString = "Messages sent per processor";
	    break;
	case MultiRunData.TYPE_SIZE_MSG:
	    titleString = "Amount of data sent";
	    break;
	}

	return new MultiRunDataSource(dataTable[dataType],
				      null, // currently no color map
				      titleString);
    }

    public MultiRunXAxis getMRXAxisData() {
	return new MultiRunXAxis(runNames);
    }

    public MultiRunYAxis getMRYAxisData(int dataType) {
	String title = "";

	switch (dataType) {
	case MultiRunData.TYPE_TIME:
	    title = "Time summed across processors (us)";
	    break;
	case MultiRunData.TYPE_NUM_MSG_SENT:
	    title = "Number of messages sent per processor";
	    break;
	case MultiRunData.TYPE_SIZE_MSG:
	    title = "Total amount of data sent (bytes)";
	    break;
	}

	ProjectionsStatistics stats =
	    new ProjectionsStatistics();
	for (int run=0; run<numRuns; run++) {
	    stats.accumulate(dataTable[dataType][run]);
	}
	return new MultiRunYAxis(MultiRunYAxis.TIME,
				 title, 
				 stats.getMax());
    }
}
