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
    public static final int TOTAL_ANALYSIS_TAGS = 1;
    public static final int ANALYZE_SUM = 0;

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

	computeSum(originalData);
	// computeAverage();
	// computeMax();
	// computeMin();
	// computeStdDeviation();

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
     *   getData takes the appropriate part of the analyzed data and
     *   returns the handle to the calling method (should be the controller
     *   on behalf of the GUI.
     *  
     */

    // ***CURRENT IMP*** for now, do not filter the EPs or configs

    public MultiRunDataSource getData(int dataType) {
	String titleString = "";
	switch (dataType) {
	case ANALYZE_SUM:
	    titleString = "Sum";
	    break;
	}
	return new MultiRunDataSource(analyzedDataTable[dataType], null,
				      titleString);
    }

    public MultiRunXAxis getMRXAxisData() {
	return new MultiRunXAxis(configList);
    }

    public MultiRunYAxis getMRYAxisData(int dataType) {
	String title = "";

	if (dataType == ANALYZE_SUM) {
	    title = "Time summed across processors (us)";
	}

	return new MultiRunYAxis(MultiRunYAxis.TIME,
				 title, 
				 getMax(analyzedDataTable[dataType]));
    }

    public String[] getMRLegendData() {
	return epNamesList;
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
}
