package projections.analysis;

import projections.gui.graph.*;
import projections.gui.*;
import projections.misc.*;

import java.io.*;

/**
 *
 *  Written by Chee Wai Lee
 *  3/29/2002
 *
 *  MultiRunDataAnalyzer is the object that will analyze data read into 
 *  MultiRunData and produce intermediate data that can be presented
 *  on the gui (not yet) or onto a text file (either on the terminal window,
 *  simple text gui canvas (not yet) or onto a file.
 *
 *  It should really be a base class upon which other data analyzer modules
 *  can inherit from.
 *
 */

public class MultiRunDataAnalyzer {

    // analysis count + types (starts from zero and contigious)
    public static final int TOTAL_ANALYSIS_TAGS = 4;
    public static final int ANALYZE_SUM = 0;
    public static final int ANALYZE_AVG = 1;
    public static final int ANALYZE_MIN = 2;
    public static final int ANALYZE_MAX = 3;

    // private tags 
    private static final int TOTAL_EP_SUMMARY_TAGS = 4;
    private static final int TOTAL_CONFIG_SUMMARY_TAGS = 1;

    // ep summary tags (starts from zero and contigious)
    private static final int EP_GROWTH_PERCENT = 0;
    private static final int EP_SUM = 1;  // used to check for zero EPs
    private static final int EP_AVG_PERCENT = 2;
    private static final int EP_SIGNIFICANCE = 3;

    // config summary tags (starts from zero and contigious)
    private static final int CONFIG_SUM = 0;

    private int numEPs;
    private int numSets;

    private int significanceMap[];

    private String epNamesList[];
    private String configList[];
    // Simple Post-analysis output table
    // analyzedDataTable dim 1 - indexed by analysis tag type
    // analyzedDataTable dim 2 - indexed by the log set ID
    // analyzedDataTable dim 3 - indexed by entry point ID
    private double analyzedDataTable[][][];

    // ****************************************************************
    // analysis data that is either output-type dependent or is to be used
    // for internal analyzer purposes (like computing the significance
    // mapping). These are computed "as-is-convenient" when computing other
    // data.
    
    // summary data for the same EP across all runs
    // epSummaryData dim 1 - indexed by summary tag type
    // epSummaryData dim 2 - indexed by entry point ID
    private double epSummaryData[][];

    // summary data for the same configuration across all EPs
    // configSummaryData dim 1 - indexed by summary tag type
    // configSummaryData dim 2 - indexed by the log set ID
    private double configSummaryData[][];

    public MultiRunDataAnalyzer() {
    }

    public void analyzeData(MultiRunData originalData) {

	numEPs = originalData.stsReaders[0].entryCount;
	numSets = originalData.stsReaders.length;

	// setup summary data
	epSummaryData = new double[TOTAL_EP_SUMMARY_TAGS][numEPs];
	configSummaryData = new double[TOTAL_CONFIG_SUMMARY_TAGS][numSets];

	analyzedDataTable = 
	    new double[TOTAL_ANALYSIS_TAGS][numSets][numEPs];

	epNamesList = new String[numEPs];
	configList = new String[numSets];


	// acquiring epNames and configuration names
	for (int i=0; i<numEPs; i++) {
	    epNamesList[i] = originalData.stsReaders[0].entryList[i].name;
	}

	for (int i=0; i<numSets; i++) {
	    configList[i] = "(" + originalData.stsReaders[i].numPe + ")" +
		"[" + originalData.stsReaders[i].machineName + "]";
	}

	// simple data tabulation
	computeSum(originalData);
	computeAverage(originalData);
	// computeMax();
	// computeMin();
	// computeStdDeviation();

	// data analysis for configurations
	// computeSumAcrossEPs(); // tagged with computeSum

	// data analysis for EP
	// computeSumAcrossConfigs();  // tagged with computeSum
	computeAvgPercentGrowth();
	computeAvgPercentContribution();
	
	// significance analysis for EP
	computeSignificanceMap();
    }

    public void computeSum(MultiRunData originalData) {
	// processing a data set
	for (int i=0; i<numSets; i++) {
	    configSummaryData[CONFIG_SUM][i] = 0;
	    // processing each EP in the dataset
	    for (int j=0; j<numEPs; j++) {
		analyzedDataTable[ANALYZE_SUM][i][j] = 0;
		// taking each processor data point for a set
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		for (int k=0; k<originalData.stsReaders[i].numPe; k++) {
		    double value =
			originalData.sumReaders[i][k].epData[j][GenericSummaryReader.TOTAL_TIME];
		    analyzedDataTable[ANALYZE_SUM][i][j] +=
			value;
		    if (value > max) {
			max = value;
		    }
		    if (value < min) {
			min = value;
		    }
		}
		analyzedDataTable[ANALYZE_MIN][i][j] = min;
		analyzedDataTable[ANALYZE_MAX][i][j] = max;
		configSummaryData[CONFIG_SUM][i] +=
		    analyzedDataTable[ANALYZE_SUM][i][j];
		epSummaryData[EP_SUM][j] +=
		    analyzedDataTable[ANALYZE_SUM][i][j];
	    }
	}
    }

    /**
     *  Assumption: computeSum is run before computeAverage
     */
    public void computeAverage(MultiRunData originalData) {
	for (int i=0; i<numSets; i++) {
	    for (int j=0; j<numEPs; j++) {
		analyzedDataTable[ANALYZE_AVG][i][j] =
		    analyzedDataTable[ANALYZE_SUM][i][j] / 
		    originalData.stsReaders[i].numPe;
	    }
	}
    }

    /**
     *   getData takes the appropriate part of the analyzed data and
     *   returns the handle to the calling method (should be the controller
     *   on behalf of the GUI.
     *  
     */
    public MultiRunDataSource getData(int dataType, boolean filterTable[],
				      int sortMap[]) {
	String titleString = "";
	switch (dataType) {
	case ANALYZE_SUM:
	    titleString = "Sum";
	    break;
	case ANALYZE_AVG:
	    titleString = "Average";
	    break;
	case ANALYZE_MIN:
	    titleString = "Min";
	    break;
	case ANALYZE_MAX:
	    titleString = "Max";
	    break;
	}
	return new MultiRunDataSource(filter(Util.applyMap(analyzedDataTable[dataType],
							   sortMap),
					     Util.applyMap(filterTable,
							   sortMap)),
				      null,
				      titleString);
    }

    public MultiRunXAxis getMRXAxisData(boolean filterTable[]) {
	return new MultiRunXAxis(filter(configList, filterTable));
    }

    public MultiRunYAxis getMRYAxisData(int dataType, boolean filterTable[]) {
	String title = "";

	if (dataType == ANALYZE_SUM) {
	    title = "Time summed across processors (us)";
	}

	return new MultiRunYAxis(MultiRunYAxis.TIME,
				 title, 
				 getMax(filter(analyzedDataTable[dataType],
					       filterTable)));
    }

    public String[] getMRLegendData(boolean filterTable[],
				    int sortMap[]) {
	return filter(Util.applyMap(epNamesList, sortMap),
		      Util.applyMap(filterTable, sortMap));
    }

    /**
     *  This function must be called *after* the sum data is computed.
     *  Otherwise, it is meaningless.
     *
     */
    public boolean[] getNonZeroFilter() {
	boolean filter[] = new boolean[epNamesList.length];
	for (int i=0; i<filter.length; i++) {
	    if (epSummaryData[EP_SUM][i] == 0.0) {
		filter[i] = false;
	    } else {
		filter[i] = true;
	    }
	}
	return filter;
    }

    /**
     *  Returns a index-to-index map (for re-sorting) that allows
     *  data to be displayed in a different order.
     *
     *  May provide parameters in the future to fine-tune significance
     *  analysis.
     *
     *  returns a copy of the internal map.
     */
    public int[] getSignificanceMap() {
	int significance[] = new int[epNamesList.length];

	// default to a direct map
	if (significanceMap == null) {
	    for (int i=0; i<significance.length; i++) {
		significance[i] = i;
	    }
	} else {
	    for (int i=0; i<significanceMap.length; i++) {
		significance[i] = significanceMap[i];
	    }
	}
	
	return significance;
    }

    public int[] getGrowthMap() {
	int growth[] = new int[epNamesList.length];
	return Sorter.sort(epSummaryData[EP_GROWTH_PERCENT]);
    }

    public int[] getSizeMap() {
	int epSize[] = new int[epNamesList.length];
	return Sorter.sort(epSummaryData[EP_AVG_PERCENT]);
    }

    /**
     *  Computes the average total time growth percentage of EPs across 
     *  all runs.
     *  Useful to see if the EP scales well.
     *  Assumes that the summation data has already been computed in
     *  analyzedDataTable.
     */
    private void computeAvgPercentGrowth() {
	for (int ep=0; ep<numEPs; ep++) {
	    double growthPercent = 0;
	    for (int config=1; config<numSets; config++) {
		double previous = analyzedDataTable[ANALYZE_SUM][config-1][ep];
		double current = analyzedDataTable[ANALYZE_SUM][config][ep];
		growthPercent += (previous - current)/previous;
	    }
	    epSummaryData[EP_GROWTH_PERCENT][ep] = growthPercent/(numEPs-1);
	}
    }

    private void computeAvgPercentContribution() {
	for (int ep=0; ep<numEPs; ep++) {
	    double percentContrib = 0;
	    for (int config=0; config<numSets; config++) {
		percentContrib +=
		    analyzedDataTable[ANALYZE_SUM][config][ep] /
		    configSummaryData[CONFIG_SUM][config];
	    }
	    epSummaryData[EP_AVG_PERCENT][ep] = percentContrib/numEPs;
	}
    }

    private void computeSignificanceMap() {

	significanceMap = new int[epNamesList.length];

	// the rate of growth contributes 20% and EP time percentage
	// contributes 80%
	for (int i=0; i<epNamesList.length; i++) {
	    epSummaryData[EP_SIGNIFICANCE][i] =
		epSummaryData[EP_GROWTH_PERCENT][i]*0.2 +
		epSummaryData[EP_AVG_PERCENT][i]*0.8;
	}

	significanceMap = Sorter.sort(epSummaryData[EP_SIGNIFICANCE]);
    }

    private double getMax(double[][] inTable) {
	double max = 0.0; // negative values not expected
	for (int i=0;i<inTable.length;i++) {
	    for (int j=0;j<inTable[i].length;j++) {
		if (max < inTable[i][j]) {
		    max = inTable[i][j];
		}
	    }
	}
	return max;
    }

    private double getMax(double[] inTable) {
	double max = 0.0;
	for (int i=0; i<inTable.length;i++) {
	    if (max < inTable[i]) {
		max = inTable[i];
	    }
	}
	return max;
    }

    private double[][] filter(double[][] data, boolean filterTable[]) {
	if (filterTable != null) {
	    double returnData[][];
	    
	    returnData = new double[data.length][getNumValid(filterTable)];
	    
	    for (int i=0; i<returnData.length; i++) {
		int jIdx = 0;
		for (int j=0; j<filterTable.length; j++) {
		    if (filterTable[j]) {
			returnData[i][jIdx] = data[i][j];
			jIdx++;
		    }
		}
	    }
	    return returnData;
	} else {
	    return data;
	}
    }

    private String[] filter(String[] data, boolean filterTable[]) {
	if (filterTable != null) {
	    String returnData[];

	    returnData = new String[getNumValid(filterTable)];

	    int iIdx = 0;
	    for (int i=0; i<filterTable.length; i++) {
		if (filterTable[i]) {
		    returnData[iIdx] = data[i];
		    iIdx++;
		}
	    }
	    return returnData;
	} else {
	    return data;
	}
    }

    private int getNumValid(boolean filterTable[]) {
	int count = 0;

	for (int i=0; i<filterTable.length; i++) {
	    if (filterTable[i]) {
		count++;
	    }
	}
	return count;
    }
}
