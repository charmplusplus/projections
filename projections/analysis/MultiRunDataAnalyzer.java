package projections.analysis;

import projections.gui.graph.*;
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

    // analysis count + types
    public static final int TOTAL_ANALYSIS_TAGS = 2;
    public static final int ANALYZE_SUM = 0;
    public static final int ANALYZE_AVG = 1;

    private int numEPs;
    private int numSets;

    private String epNamesList[];
    private String configList[];
    // Simple Post-analysis output table
    // analyzedDataTable dim 1 - indexed by analysis tag type
    // analyzedDataTable dim 2 - indexed by the log set ID
    // analyzedDataTable dim 3 - indexed by entry point ID
    private double analyzedDataTable[][][];

    public MultiRunDataAnalyzer() {
    }

    public void analyzeData(MultiRunData originalData) {

	numEPs = originalData.stsReaders[0].entryCount;
	numSets = originalData.stsReaders.length;

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
	// computeSumAcrossEPs(originalData);

	// data analysis for EP
	// computePercentGrowth(originalData);
	
	// significance analysis for EP
	// computeSignificance();
    }

    public void computeSum(MultiRunData originalData) {
	// processing a data set
	for (int i=0; i<numSets; i++) {
	    // processing each EP in the dataset
	    for (int j=0; j<numEPs; j++) {
		analyzedDataTable[ANALYZE_SUM][i][j] = 0;
		// taking each processor data point for a set
		for (int k=0; k<originalData.stsReaders[i].numPe; k++) {
		    analyzedDataTable[ANALYZE_SUM][i][j] +=
			originalData.sumReaders[i][k].epData[j][MRSummaryReader.TOTAL_TIME];
		}
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
    public MultiRunDataSource getData(int dataType, boolean filterTable[]) {
	String titleString = "";
	switch (dataType) {
	case ANALYZE_SUM:
	    titleString = "Sum";
	    break;
	case ANALYZE_AVG:
	    titleString = "Average";
	    break;
	}
	return new MultiRunDataSource(filter(analyzedDataTable[dataType],
					     filterTable),
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

    public String[] getMRLegendData(boolean filterTable[]) {
	return filter(epNamesList, filterTable);
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
