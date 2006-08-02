package projections.analysis;

import projections.gui.graph.*;
import projections.gui.*;
import projections.misc.*;

import java.io.*;
import java.util.*;
import java.awt.*; // unfortunate!!!

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
    public static final int NUM_EXTR_ENTRIES = 1;
    public static final int EXTR_OVERHEAD = 0;

    private static final String extraNames[] =
    {"Idle Time and System Overhead"};

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

    // static color assignments for categories
    private static final Color catColors[] =
    { Color.green, Color.yellow, Color.red, Color.white };

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

    // OUTPUT data array
    double outputData[][];
    
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
	    new double[MultiRunData.NUM_TYPES][numRuns][NUM_EXTR_ENTRIES];

	double runWallTimes[] = data.getRunWallTimes();
	// Overhead and Idle time only applies to the time type. All other
	// information is (correctly) left at zero.
	for (int run=0; run<numRuns; run++) {
	    extraTable[MultiRunData.TYPE_TIME][run][EXTR_OVERHEAD] =
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
		continue;
	    }
	    // TEST #2 - Change
	    // test to see if the values generally climbs or drops from run 
	    // to run. The current implementation is extremely crude and may
	    // form the basis for future automated performance analysis
	    // research.
	    int consecutiveIncrements = 0;
	    int consecutiveDecrements = 0;
	    boolean lastIncremented = true;
	    double avgChange = 0.0;
	    double prevValue = dataTable[dataType][0][ep];
	    if (dataTable[dataType][1][ep] > prevValue) {
		consecutiveIncrements = 1;
		avgChange += dataTable[dataType][1][ep] - prevValue;
		lastIncremented = true;
	    } else if (dataTable[dataType][1][ep] < prevValue) {
		consecutiveDecrements = 1;
		avgChange += prevValue - dataTable[dataType][1][ep];
		lastIncremented = false;
	    }
	    prevValue = dataTable[dataType][1][ep];
	    for (int run=2; run<numRuns; run++) {
		if (dataTable[dataType][run][ep] > prevValue) {
		    double change =
			dataTable[dataType][run][ep] - prevValue;
		    if (lastIncremented) {
			avgChange = avgChange*consecutiveIncrements + change;
			consecutiveIncrements++;
			avgChange /= consecutiveIncrements;
		    } else {
			consecutiveIncrements = 1;
			avgChange = change;
		    }
		    lastIncremented = true;
		} else if (dataTable[dataType][run][ep] < prevValue) {
		    double change =
			prevValue - dataTable[dataType][run][ep];
		    if (!lastIncremented) {
			avgChange = avgChange*consecutiveDecrements + change;
			consecutiveDecrements++;
			avgChange /= consecutiveDecrements;
		    } else {
			consecutiveDecrements = 1;
			avgChange = change;
		    }
		    lastIncremented = false;
		}
		prevValue = dataTable[dataType][run][ep];
	    }
	    if (numRuns == 1) {
		categories[dataType][CAT_EP_NO_CHANGE].add(new Integer(ep));
	    } else {		
		if (((consecutiveIncrements > (numRuns-1)*0.5) || 
		     (consecutiveDecrements > (numRuns-1)*0.5)) &&
		    (avgChange > dataTable[dataType][0][ep]*0.1)) {
		    categories[dataType][CAT_EP_CHANGE].add(new Integer(ep));
		} else {
		    categories[dataType][CAT_EP_NO_CHANGE].add(new Integer(ep));
		}
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
	// we need one additional row for the header
	return categories[dataType][categoryIndex].size();
    }
    
    /**
     *  The only reason we still pass in the dataType and categoryIndex
     *  is to provide the flexibility to return additional columns
     *  depending on the data type or category type.
     */
    public int getNumColumns(int dataType, int categoryIndex) {
	// we need one additional column for the entry point names.
	return numRuns+1;
    }

    public String getColumnName(int dataType, int categoryIndex, int col) {
	// first column is always the entry point name
	if (col == 0) {
	    return "Entry Point Name";
	} else {
	    return runNames[col-1];
	}
    }

    public Object getTableValueAt(int dataType, int categoryIndex, 
				  int row, int col) {
	int epIndex = 
	    ((Integer)categories[dataType][categoryIndex].elementAt(row)).intValue();

	// column 0 is always the entry point name/description
	if (col == 0) {
	    // if extra info, use the string found in extraNames
	    if (epIndex >= numEPs) {
		return extraNames[numEPs-epIndex];
	    } else {
		return epNames[epIndex];
	    }
	} else {
	    // if extra info, use different array.
	    if (epIndex >= numEPs) {
		return new Double(extraTable[dataType][col-1][numEPs-epIndex]);
	    } else {
		return new Double(dataTable[dataType][col-1][epIndex]);
	    }
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
     *
     *   This is the default behavior where every EP is displayed "as is"
     *   except for insignificant EPs which appear as a single gray group.
     */
    public MultiRunDataSource getDataSource(int dataType) {
	// output array - indexed by run then by entry (combination of
	// both EPs and extra information.
	Color colorMap[];

	int numYvalues =
	    numEPs - categories[dataType][CAT_EP_INSIGNIFICANT].size() + 1 +
	    NUM_EXTR_ENTRIES;
	outputData = new double[numRuns][numYvalues];

	String titleString = "";
	switch (dataType) {
	case MultiRunData.TYPE_TIME:
	    titleString = "Time taken";
	    break;
	case MultiRunData.TYPE_TIMES_CALLED:
	    titleString = "Number of times called";
	    break;
	case MultiRunData.TYPE_NUM_MSG_SENT:
	    titleString = "Messages sent per processor";
	    break;
	case MultiRunData.TYPE_SIZE_MSG:
	    titleString = "Amount of data sent";
	    break;
	}

	// **CW** 0 will be replaced by an appropriate static
	// constants.
	computeOutputArray(outputData, dataType, 0);
	colorMap = computeColorMap(numYvalues, dataType, 0);
	
	return new MultiRunDataSource(this,
				      outputData,
				      dataType,
				      colorMap,
				      titleString);
    }

    public MultiRunXAxis getMRXAxisData() {
	return new MultiRunXAxis(runNames);
    }

    public MultiRunYAxis getMRYAxisData(int dataType) {
	String title = "";
	int outAxisType = MultiRunYAxis.TIME; // default

	switch (dataType) {
	case MultiRunData.TYPE_TIME:
	    title = "Time summed across processors (us)";
	    outAxisType = MultiRunYAxis.TIME;
	    break;
	case MultiRunData.TYPE_TIMES_CALLED:
	    title = "Number of times entry point was called";
	    outAxisType = MultiRunYAxis.MSG;
	    break;
	case MultiRunData.TYPE_NUM_MSG_SENT:
	    title = "Number of messages sent per processor";
	    outAxisType = MultiRunYAxis.MSG;
	    break;
	case MultiRunData.TYPE_SIZE_MSG:
	    title = "Total amount of data sent (bytes)";
	    outAxisType = MultiRunYAxis.MSG;
	    break;
	}

	ProjectionsStatistics stats =
	    new ProjectionsStatistics();
	for (int run=0; run<numRuns; run++) {
	    stats.accumulate(outputData[run]);
	}

	return new MultiRunYAxis(outAxisType,
				 title, 
				 stats.getMax());
    }

    public String[] getPopup(int xVal, int yVal, int dataType) {
	String returnStrings[];

	returnStrings = new String[4];
	returnStrings[0] = runNames[xVal];

	// returning category names
	int numNoChange = categories[dataType][CAT_EP_NO_CHANGE].size();
	int numInsignificant = 
	    categories[dataType][CAT_EP_INSIGNIFICANT].size();
	int numChanged =
	    categories[dataType][CAT_EP_CHANGE].size();
	int numOverhead =
	    categories[dataType][CAT_OVERHEAD_IDLE].size();
	int category = 0;
	if (yVal < numNoChange) {
	    category = CAT_EP_NO_CHANGE;
	    int catIdx = yVal;
	    int epIdx = 
		((Integer)categories[dataType][category].elementAt(catIdx)).intValue();
	    returnStrings[2] = "Entry Point: " + epNames[epIdx];
	} else if (yVal < numNoChange + 1) {
	    category = CAT_EP_INSIGNIFICANT;
	    returnStrings[2] = "Bunch of EPs";
	} else if (yVal < numNoChange + 1 + numChanged) {
	    category = CAT_EP_CHANGE;
	    int catIdx = yVal-1-numNoChange;
	    int epIdx = 
		((Integer)categories[dataType][category].elementAt(catIdx)).intValue();
	    returnStrings[2] = "Entry Point: " + epNames[epIdx];
	} else {
	    category = CAT_OVERHEAD_IDLE;
	    returnStrings[2] = "";
	}
	returnStrings[1] = catNames[category];

	// returning values
	switch (dataType) {
	case MultiRunData.TYPE_TIME:
	    returnStrings[3] = "Exec Time: " + 
		U.t((long)outputData[xVal][yVal]);
	    break;
	case MultiRunData.TYPE_TIMES_CALLED:
	    returnStrings[3] = "Times called: " + (long)outputData[xVal][yVal];
	    break;
	case MultiRunData.TYPE_NUM_MSG_SENT:
	    returnStrings[3] = "Msgs Sent: " + (long)outputData[xVal][yVal];
	    break;
	case MultiRunData.TYPE_SIZE_MSG:
	    returnStrings[3] = "Msg Volume: " + (long)outputData[xVal][yVal];
	    break;
	}
	return returnStrings;
    }

    /**
     *  convenience method for generating the appropriate data array
     *  given a certain categorization display scheme.
     *
     *  The default scheme is to arrange the categories in the following
     *  order (from bottom to top):
     *  NO_CHANGE, INSIGNIFICANT, SIGNIFICANT, OVERHEAD
     *
     *  Only Insignificant EPs will be presented as a unified group
     *  (they are usually zeros).
     */
    private void computeOutputArray(double data[][],int dataType,int scheme) {
	// fill the appropriate parts of dataTable and extraTable into data
	// this process is broken into a phase for each category.
	int entry = 0;
	// CAT_EP_NO_CHANGE
	int numNoChange = categories[dataType][CAT_EP_NO_CHANGE].size();
	for (int catIdx=0; catIdx<numNoChange; catIdx++) {
	    int epIdx = 
		((Integer)categories[dataType][CAT_EP_NO_CHANGE].elementAt(catIdx)).intValue();
	    for (int run=0; run<numRuns; run++) {
		data[run][entry] = dataTable[dataType][run][epIdx];
	    }
	    entry++;
	}
	// CAT_EP_INSIGNIFICANT
	int numInsignificant = 
	    categories[dataType][CAT_EP_INSIGNIFICANT].size();
	for (int run=0; run<numRuns; run++) {
	    for (int catIdx=0; catIdx<numInsignificant; catIdx++) {
		int epIdx =
		    ((Integer)categories[dataType][CAT_EP_INSIGNIFICANT].elementAt(catIdx)).intValue();
		data[run][entry] += dataTable[dataType][run][epIdx];
	    }
	}
	entry++;
	// CAT_EP_CHANGE
	int numChanged =
	    categories[dataType][CAT_EP_CHANGE].size();
	for (int catIdx=0; catIdx<numChanged; catIdx++) {
	    int epIdx = 
		((Integer)categories[dataType][CAT_EP_CHANGE].elementAt(catIdx)).intValue();
	    for (int run=0; run<numRuns; run++) {
		data[run][entry] = dataTable[dataType][run][epIdx];
	    }
	    entry++;
	}
	// CAT_OVERHEAD_IDLE
	int numOverhead =
	    categories[dataType][CAT_OVERHEAD_IDLE].size();
	for (int catIdx=0; catIdx<numOverhead; catIdx++) {
	    int entryIdx =
		numEPs -
		((Integer)categories[dataType][CAT_OVERHEAD_IDLE].elementAt(catIdx)).intValue();
	    for (int run=0; run<numRuns; run++) {
		// testing.
		// data[run][entry] = 0;
		data[run][entry] = extraTable[dataType][run][entryIdx];
	    }
	    entry++;
	}
    }

    /**
     *  convenience method for generating the appropriate color map
     *  given a certain categorization display scheme.
     *
     *  The default scheme is to color Insignificant EPs gray, Overhead
     *  white and leave every other EP colored (but kept in position).
     */
    private Color[] computeColorMap(int numColors, int dataType, int scheme) {
	// Ask Analysis for a simple (for now) colormap.
	// Then overwrite the slot for insignificant and overhead colors
	Color colorMap[] = new Color[numColors];

	int numNoChange =
	    categories[dataType][CAT_EP_NO_CHANGE].size();
	int numChanged =
	    categories[dataType][CAT_EP_CHANGE].size();
	colorMap = ColorManager.createColorMap(numColors);

	// set insignificant category to gray
	colorMap[numNoChange] = Color.gray;

	/*
	for (int i=0; i<colorMap.length; i++) {
	    System.out.println(colorMap[i]);
	}
	*/

	// set the overhead colors
	int numOverhead =
	    categories[dataType][CAT_OVERHEAD_IDLE].size();
	int offset =
	    numNoChange + 1 + numChanged;
	for (int catIdx=0; catIdx<numOverhead; catIdx++) {
	    switch (catIdx) {
	    case EXTR_OVERHEAD:
		colorMap[offset+catIdx] = Color.white;
	    }
	}
	return colorMap;
    }
}
