package projections.analysis;

import projections.misc.*;

import java.lang.*;
import java.io.*;
import java.util.*;

/** 
 *  Written by Chee Wai Lee
 *  2/12/2002
 *
 *  The GenericSumDetailReader reads .sumd files to produce data to be 
 *  consumed by the analyzer.
 *
 *  This is the new summary format that stores information by intervals.
 *  Phase information will probably be written in later.
 *
 */

public class GenericSumDetailReader
{
    // private static meta-tags - used to allocate space in the data array
    // based on the number of tags.
    private static final int NUM_TAGS = 2;

    // public static tags - used to access the appropriate part of the
    // array.
    public static final int TOTAL_TIME = 0;
    public static final int NUM_MSGS = 1;

    // header values
    public int versionNum;
    public int myPE;
    public int numPE;
    public int numIntervals;
    public int numEPs;  // bizzare ... already found in .sts file
    public double intervalSize;

    // Data values
    // Unlike the old summary readers. We read the data in a consistent
    // 2D array indexed by EP and Interval for each type of data.
    // Dimension 0 - indexed by data type.
    // Dimension 1 - indexed by Entry Point ID
    // Dimension 2 - indexed by Interval number.
    public double data[][][];

    // private miscellaneous data
    private double version;
    private BufferedReader reader;
    private ParseTokenizer tokenizer;
    private int tokenType;

    public GenericSumDetailReader(String filename, double Nversion) 
	throws IOException
    {
	try {
	    reader = new BufferedReader(new FileReader(filename));
	    version = Nversion;
	    read();
	    reader.close();
	    reader = null;
	} catch (IOException e) {
	    throw new IOException("Error reading file " + filename);
	}
    }

    // Methods to parse the summary file

    public void read()
	throws IOException
    {
	//Set up the tokenizer  **GLOBAL** yucks!
	tokenizer=new ParseTokenizer(reader);
	tokenizer.parseNumbers();
	tokenizer.eolIsSignificant(true);
	tokenizer.whitespaceChars('/','/'); 
	tokenizer.whitespaceChars(':',':');
	tokenizer.whitespaceChars('[','[');
	tokenizer.whitespaceChars(']',']');
	tokenizer.wordChars('a','z');
	tokenizer.wordChars('A','Z');

	// Read the first line (Header information)
	tokenizer.checkNextString("ver");
	versionNum = (int)tokenizer.nextNumber("Version Number");
	myPE = (int)tokenizer.nextNumber("processor number");
	numPE = (int)tokenizer.nextNumber("number of processors");
	tokenizer.checkNextString("count");
	numIntervals = (int)tokenizer.nextNumber("count");
	tokenizer.checkNextString("ep");
	numEPs = (int)tokenizer.nextNumber("number of entry methods");
	tokenizer.checkNextString("interval");
	double intervalSize = 
	    tokenizer.nextScientific("processor usage sample interval"); 
	if (StreamTokenizer.TT_EOL!=tokenizer.nextToken()) {
	    throw new IOException("extra garbage at end of header line");
	}

	// prepare to store summary data into arrays
	data = new double[NUM_TAGS][numEPs][numIntervals];

	// Read the data (labelled lines)
	// labels that are not recognized are ignored.
	// this allows the reading format to be flexible between different
	// versions.
	//
	// The current format uses run-length encoding that cuts across
	// EPs, so an EP ID count has to be maintained for each line.
	//
	while ((tokenType=tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
	    if (tokenType != StreamTokenizer.TT_WORD) {
		throw new IOException("Line Label expected.");
	    }
	    String label = (String)tokenizer.sval;
	    // BIG if-then-else statement to deal with each time of 
	    // information. Convenience methods should be used to 
	    // deal with multiple types sharing the same format to avoid
	    // code bloat. In this case, most if not all lines should
	    // consist of i-values for each interval for j different EPs.
	    if (label.equals("ExeTimePerEPperInterval")) {
		intervalByEPRunLE(TOTAL_TIME, label);
	    } else if (label.equals("EPCallTimePerInterval")) {
		intervalByEPRunLE(NUM_MSGS, label);
	    } else {
		// do nothing. Unrecognized labels are not an error.
		// this allows new formats to be implemented without
		// immediately rendering this tool useless.
	    }
	}
    }

    /**
     *  reads a line of sum_detail information for an appropriate type.
     */
    private void intervalByEPRunLE(int type, String label) 
	throws IOException
    {
	// indices at which we are about to fill the next piece of data.
	Integer nextInterval = new Integer(0);
	Integer nextEP = new Integer(0);
	while ((tokenType=tokenizer.nextToken())!=StreamTokenizer.TT_EOL) {
	    if (tokenType == StreamTokenizer.TT_NUMBER) {
		double value = (double)tokenizer.nval;
		// RLE
		if ((tokenType=tokenizer.nextToken()) =='+') {
		    if ((tokenType=tokenizer.nextToken()) ==
			StreamTokenizer.TT_NUMBER) {
			int valueCount = (int)tokenizer.nval;
			fillData(type,nextEP,nextInterval,valueCount,value);
		    } else {
			// Bad RLE format
			throw new IOException("Bad RLE encoding. Expected" +
					      " a count after plus token.");
		    }
		} else {
		    // not RLE. Store the lone value and reset state-machine 
		    // to expect the next value.
		    fillData(type,nextEP,nextInterval,1,value);
		    tokenizer.pushBack();
		}
	    } else {
		throw new IOException("Expected a number at line labelled: " +
				      label);
	    }
	}
	// The data is corrupted if we encounter the end-of-line prematurely.
	if ((nextInterval.intValue() < numIntervals) || 
	    (nextEP.intValue() < numEPs)) {
	    throw new IOException("Premature end of data at line labelled: "+
				  label);
	}
    }

    /**
     *  fills the appropriate part of the data array from a data read.
     *  IOExceptions are raised if data writing crosses a type boundary.
     *  (ie. the counts do not match).
     *
     *  Quite ugly - I need to return the resulting next ep index and the
     *    next interval index, so they have to be passed in by reference
     *    using objects. The other uglier alternative would be to make
     *    fillData return a "pair" class.
     */
    private void fillData(int type, Integer epIdx, Integer intervalIdx, 
			  int count, double value) 
	throws IOException
    {
	long entriesLeft = 
	    (numEPs-epIdx.intValue())*numIntervals - intervalIdx.intValue();
	if (entriesLeft < count) {
	    throw new IOException("RLE counts do not match.");
	}
	int numDataRows = count/numIntervals;
	int numDataRemainder = count%numIntervals;
	int finalEPIdx = numDataRows+epIdx.intValue();
	int finalIntervalIdx = 
	    (numDataRemainder+intervalIdx.intValue())%numIntervals;
	if (numDataRemainder+intervalIdx.intValue() >= numIntervals) {
	    finalEPIdx++;
	}
	// fill data array
	int ep = epIdx.intValue();
	int interval = intervalIdx.intValue();
	for (int i=0; i<count; i++) {
	    data[type][ep][interval] = value;
	    // update indices
	    if (++interval >= numIntervals) {
		interval = 0;
		ep++;
	    }
	}
	// set the returning indices (first, make a check)
	if ((interval != finalIntervalIdx) || (ep != finalEPIdx)) {
	    throw new IOException("Data counts computed do not match " +
				  "number of data entries filled!");
	}
	epIdx = new Integer(finalEPIdx);
	intervalIdx = new Integer(finalIntervalIdx);
    }
}
