package projections.analysis;

import projections.gui.*;
import projections.misc.*;

import java.io.*;

/**
 *
 *  Written by Chee Wai Lee
 *  3/29/2002
 *
 *  *****NEEDS HEAVY REWORKING*****
 *
 *  DataAnalyzer is the object that will analyze data read into 
 *  AccumulatedData and produce intermediate data that can be presented
 *  on the gui (not yet) or onto a text file (either on the terminal window,
 *  simple text gui canvas (not yet) or onto a file.
 *
 *  It should really be a base class upon which other data analyzer modules
 *  can inherit from.
 *
 */

public class DataAnalyzer {

    private boolean hasBeenAnalyzed = false;
    
    private String epNamesList[];
    private String numPeList[];
    // Simple Post-analysis output table
    // outputTable dim 1 - indexed by entry point ID
    // outputTable dim 2 - indexed by the log set ID
    // outputTable dim 3 - indexed by analysis tag type
    private String outputTable[][][];

    public DataAnalyzer() {
    }

    public void analyzeData(AccumulatedData originalData, int analysisType) {
	outputTable = 
	    new String[originalData.stsReaders[0].entryCount][originalData.stsReaders.length][MultiRunWindow.TOTAL_ANALYSIS_TAGS];
	epNamesList = new String[originalData.stsReaders[0].entryCount];
	numPeList = new String[originalData.stsReaders.length];
	// processing for each entry point
	for (int i=0; i<outputTable.length;i++) {
	    // fill up epNamesList
	    epNamesList[i] = originalData.stsReaders[0].entryList[i].name;
	    // across different data sets
	    for (int j=0; j<outputTable[i].length;j++) {
		numPeList[j] = 
		    Integer.toString(originalData.stsReaders[j].numPe);
		if ((analysisType & MultiRunWindow.ANALYZE_SUM) == 
		    MultiRunWindow.ANALYZE_SUM) {
		    int bitPosition = 
			getBitPosition(MultiRunWindow.ANALYZE_SUM);
		    outputTable[i][j][bitPosition] = "0";
		}
		// taking each processor data point for a set
		for (int k=0; k<originalData.stsReaders[j].numPe; k++) {
		    if ((analysisType & MultiRunWindow.ANALYZE_SUM) == 
			MultiRunWindow.ANALYZE_SUM) {
			int bitPosition = 
			    getBitPosition(MultiRunWindow.ANALYZE_SUM);
			outputTable[i][j][bitPosition] =
			    sumString(outputTable[i][j][bitPosition],
				      originalData.sumReaders[j][k].epData[i][MRSummaryReader.TOTAL_TIME]);
		    }
		}
	    }
	}
	hasBeenAnalyzed = true;
    }

    public void generateOutput(String target, int type) 
	throws IOException
    {
	BufferedWriter writer;
	if (target.equals("stdout")) {
	    writer = new BufferedWriter(new OutputStreamWriter(System.out));
	    outputResults(writer, type);
	} else {
	    File outputFile = new File(target);
	    // if not a valid file
	    if (outputFile.exists() && !outputFile.isFile()) {
		throw new IOException("Output file is not a regular file");
	    }
	    // if not found, create
	    if (!outputFile.exists()) {
		outputFile.createNewFile();
	    }
	    // if exists and is a regular file, clobber old contents
	    // if permissions are set right ... otherwise, expect
	    // IOException.
	    writer = new BufferedWriter(new FileWriter(outputFile));
	    outputResults(writer, type);
	}
    }

    private void outputResults(BufferedWriter writer, int type) 
	throws IOException
    {
	int bitPosition = getBitPosition(type);

	if (!hasBeenAnalyzed) {
	    throw new IOException("No meaningful data - not analyzed yet.");
	}
	// first pass to determine pretty-printing properties
	// forget it for now.
	
	// write heading line
	writer.write("                      "); // buffer space (arbitrary)
	for (int i=0; i<outputTable[0].length;i++) {
	    writer.write(" | " + numPeList[i]);
	}
	writer.newLine();

	// for each line, write entry point name followed by data
	for (int i=0; i<outputTable.length;i++) {
	    // write entry point name
	    writer.write(epNamesList[i]);
	    for (int j=0; j<outputTable[i].length;j++) {
		writer.write(" | " + outputTable[i][j][bitPosition]);
	    }
	    writer.newLine();
	}
	writer.flush();
	writer.close();
    }

    private int maxEPNameLength(EntryTypeData epList[]) {
	int max = 0;
	for (int i=0; i<epList.length; i++) {
	    if (max < epList[i].name.length()) {
		max = epList[i].name.length();
	    }
	}
	return max;
    }

    private int[] maxColumnWidth(String outputData[][][], int type) {
	int columns[] = new int[10];  // arbitrary ... I am sleepy
	// along the columns
	for (int i=0; i<outputData.length;i++) {
	    // along the rows
	    for (int j=0; j<outputData[i].length;j++) {
		
	    }
	}
	return columns;
    }

    private int getBitPosition(int powerOfTwo) {
	int position = 0;

	while ((powerOfTwo /= 2) != 0) {
	    position++;
	}
	return position;
    }

    private String sumString(String strNumber, long value) {
	long strValue = Long.parseLong(strNumber);
	strValue += value;
	return (String.valueOf(strValue));
    }
}
