package projections.analysis;

import projections.misc.*;

import java.lang.*;
import java.io.*;
import java.util.*;

/** 
 *  Written by Chee Wai Lee
 *  2/12/2002
 *
 *  The SumDetailReader reads .sumd files to produce data to be 
 *  consumed by the analyzer.
 *
 *  This is the new summary format that stores information by intervals.
 *  Phase information will probably be written in later.
 *
 *  Updated
 *  -------
 *  3/18/2003 - changed to read and store data in a dense internal data
 *              structure. This dense structure should be used up till
 *              the time Graph is presented with the data for rendering.
 */

public class SumDetailReader
    extends ProjectionsReader
{
    // public static meta-tags - used to allocate space in the data array
    // based on the number of tags.
    public static final int NUM_TAGS = 2;

    // public static tags - used to access the appropriate part of the
    // array.
    public static final int TOTAL_TIME = 0;
    public static final int NUM_MSGS = 1;

    // header values
    public int versionNum;
    public int myPE;
    public int numPE;
    public int numIntervals;
    public int numEPs;
    public double intervalSize;

    // Compressed Data
    // A Vector of RLEBlocks for each Type, EP combination
    // Dim 0 - indexed by data type.
    // Dim 1 - indexed by EP, a not-quite-as dense format as the actual file
    //                 but good enough.
    private Vector rawData[][];

    // private miscellaneous data
    private double version;
    private BufferedReader reader;
    private ParseTokenizer tokenizer;
    private int tokenType;

    public SumDetailReader(String filename, double Nversion) 
	throws IOException
    {
	super();
	try {
	    reader = new BufferedReader(new FileReader(filename));
	    version = Nversion;
	    readData();  // readData() calls read() in superclass
	    reader.close();
	    reader = null;
	} catch (IOException e) {
	    throw new IOException("Error reading file " + filename +
				  ": " + e.toString());
	}
    }

    // Method to nullify data for garbage collection
    protected void nullifyData() {
	rawData = null;
    }

    // Method to recognize the file type. For now, just say "yes".
    protected boolean isAvailable() {
	return true;
    }

    protected long read() 
	throws IOException
    {
	long byteCount = 0;

	// Set up the tokenizer  
	tokenizer=new ParseTokenizer(reader);
	tokenizer.parseNumbers();
	tokenizer.eolIsSignificant(true);
	tokenizer.whitespaceChars('/','/'); 
	tokenizer.whitespaceChars(':',':');
	tokenizer.whitespaceChars('[','[');
	tokenizer.whitespaceChars(']',']');
	tokenizer.wordChars('a','z');
	tokenizer.wordChars('A','Z');
	tokenizer.wordChars('+','+');

	// Read the first line (Header information)
	tokenizer.checkNextString("ver");
	versionNum = (int)tokenizer.nextNumber("Version Number");
	tokenizer.checkNextString("cpu");
	myPE = (int)tokenizer.nextNumber("processor number");
	numPE = (int)tokenizer.nextNumber("number of processors");
	tokenizer.checkNextString("numIntervals");
	numIntervals = (int)tokenizer.nextNumber("numIntervals");
	tokenizer.checkNextString("numEPs");
	numEPs = (int)tokenizer.nextNumber("number of entry methods");
	tokenizer.checkNextString("intervalSize");
	intervalSize = 
	    tokenizer.nextScientific("processor usage sample interval"); 
	if (StreamTokenizer.TT_EOL!=tokenizer.nextToken()) {
	    throw new IOException("extra garbage at end of header line");
	}

	// prepare to store summary data into arrays
	rawData = new Vector[NUM_TAGS][numEPs];
	for (int type=0; type<NUM_TAGS; type++) {
	    for (int ep=0; ep<numEPs; ep++) {
		rawData[type][ep] = new Vector();
	    }
	}

	// Read the data (labelled lines)
	// labels that are not recognized are ignored.
	// this allows the reading format to be flexible between different
	// versions.
	//
	// The current format uses run-length encoding that cuts across
	// EPs, so an EP ID count has to be maintained for each line.
	while (StreamTokenizer.TT_EOF!=tokenizer.nextToken()) {
	    if (tokenizer.ttype != StreamTokenizer.TT_WORD) {
		throw new IOException("Bad Sumdetail format - label expected");
	    }
	    String label = tokenizer.sval;
	    if (label.equals("ExeTimePerEPperInterval")) {
		buildTable(TOTAL_TIME);
	    } else if (label.equals("EPCallTimePerInterval")) {
		buildTable(NUM_MSGS);
	    } else {
		// do nothing. Unrecognized labels are not an error.
		// this allows new formats to be implemented without
		// immediately rendering this tool useless.
	    }
	}
	return byteCount;
    }

    private void buildTable(int type) 
	throws IOException
    {
	int epIdx = 0;
	int intervalsLeft = numIntervals;

	double value = 0;
	int count = 1;
	while (StreamTokenizer.TT_EOL!=tokenizer.nextToken()) {
	    switch (tokenizer.ttype) {
	    case StreamTokenizer.TT_NUMBER:

		value = tokenizer.nval;
		count = 1;

		if (StreamTokenizer.TT_WORD==tokenizer.nextToken()) {
		    String temp = tokenizer.sval;
		    if (temp.startsWith("+")) {
			count = Integer.parseInt(temp.substring(1));
		    } else {
			throw new IOException("Bad SumDetail Format - " +
					      "invalid data in run-length " +
					      "encoded block.");
		    }
		} else {
		    tokenizer.pushBack();
		}

		// store the data into our table
		while (count > intervalsLeft) {
		    RLEBlock newBlock = new RLEBlock();
		    newBlock.value = value;
		    newBlock.count = intervalsLeft;
		    rawData[type][epIdx++].add(newBlock);
		    count -= intervalsLeft;
		    intervalsLeft = numIntervals;
		}
		// handle left-overs
		if (count < intervalsLeft) {
		    RLEBlock newBlock = new RLEBlock();
		    newBlock.value = value;
		    newBlock.count = count;
		    rawData[type][epIdx].add(newBlock);
		    intervalsLeft -= count;
		} else if (count == intervalsLeft) {
		    RLEBlock newBlock = new RLEBlock();
		    newBlock.value = value;
		    newBlock.count = count;
		    rawData[type][epIdx++].add(newBlock);
		    intervalsLeft = numIntervals;
		}
		break;
	    default:
		throw new IOException("Bad SumDetail Format - number " +
				      "expected.");
	    }
	}
    }

    // These accessor methods should be used exclusively by IntervalData.java

    public int getNumIntervals() {
	return numIntervals;
    }

    public double getIntervalSize() {
	return intervalSize;
    }

    public Vector[] getData(int type) {
	return rawData[type];
    }
}
