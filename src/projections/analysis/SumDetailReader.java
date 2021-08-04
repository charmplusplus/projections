package projections.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.List;

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

class SumDetailReader extends ProjectionsReader
    implements IntervalCapableReader
{
    // public static meta-tags - used to allocate space in the data array
    // based on the number of tags.
    protected static final int NUM_TAGS = 2;

    // public static tags - used to access the appropriate part of the
    // array.
    private static final int TOTAL_TIME = 0;
    private static final int NUM_MSGS = 1;

    // header values
    private int numIntervals;
    private int numEPs;
    private long intervalSize;

    // Compressed Data
    // A Vector of RLEBlocks for each Type, EP combination
    // Dim 0 - indexed by data type.
    // Dim 1 - indexed by EP, a not-quite-as dense format as the actual file
    //                 but good enough.
    private List<RLEBlock> rawData[][];

    private BufferedReader reader;
    private ParseTokenizer tokenizer;
    protected SumDetailReader(File file, double Nversion)
    {
	super(file, String.valueOf(Nversion));
    }

    /**
     *  SumDetailReader expects a file. The availability check is implemented
     *  as such.
     */
    protected boolean checkAvailable() {
	return sourceFile.canRead();
    }

    protected void readStaticData() 
	throws IOException
    {
	reader = new BufferedReader(new FileReader(sourceFile));
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
	double versionNum = tokenizer.nextNumber("Version Number");
	/* **CW** It is still unclear how we should handle versioning
	 *	  in projections. This feature is tentatively dropped.
	 */
	/*
	if (versionNum != Double.parseDouble(expectedVersion)) {
	    throw new ProjectionsFormatException(expectedVersion,
						 "File version [" + 
						 versionNum + "] conflicts " +
						 "with expected version.");
	}
	*/
	tokenizer.checkNextString("cpu");
	int myPE = (int)tokenizer.nextNumber("processor number");
	int numPE = (int)tokenizer.nextNumber("number of processors");
	tokenizer.checkNextString("numIntervals");
	numIntervals = (int)tokenizer.nextNumber("numIntervals");
	tokenizer.checkNextString("numEPs");
	numEPs = (int)tokenizer.nextNumber("number of entry methods");
        System.out.println("numEPs: "+ numEPs);
	tokenizer.checkNextString("intervalSize");
	double interval =
	    tokenizer.nextScientific("processor usage sample interval");
    intervalSize = (long)Math.floor(interval*1000000);

        System.out.println("intervalSize: "+ intervalSize);

        if (StreamTokenizer.TT_EOL!=tokenizer.nextToken()) {
        System.out.println("\nEX\n");
	    throw new ProjectionsFormatException(expectedVersion, 
						 "extra garbage at end of " +
						 "header line");
	}

	reader.close();
	reader = null;
    }

    
    protected void read() 
	throws IOException
    {
	reader = new BufferedReader(new FileReader(sourceFile));

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

	// ignore the first line -> header
	tokenizer.skipLine();

	// prepare to store summary data into arrays
	rawData = new ArrayList[NUM_TAGS][numEPs];
	for (int type=0; type<NUM_TAGS; type++) {
	    for (int ep=0; ep<numEPs; ep++) {
		rawData[type][ep] = new ArrayList();
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
		throw new IOException("Bad Sumdetail format-label expected");
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
	reader.close();
	reader = null;
    }

    
    class RLEBlock {
        int count = 0;
        int value = 0;
    }

    

    private void buildTable(int type) 
	throws IOException
    {
            System.out.println("buildTable\n");
	int epIdx = 0;
	int intervalsLeft = numIntervals;

	int value = 0;
	int count = 1;
	while (StreamTokenizer.TT_EOL!=tokenizer.nextToken()) {
	    switch (tokenizer.ttype) {
	    case StreamTokenizer.TT_NUMBER:

		value = (int)tokenizer.nval;
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
		    try{
		    boolean r = rawData[type][epIdx++].add(newBlock);
		    }catch(Exception e){
		    System.out.println("\nADD EXCEPTION: " + e.toString());
		    break;
		    }
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

    public double getIntervalSize() {
	return intervalSize;
    }

    public void loadIntervalData(double intervalSize, long startInterval,
				 long endInterval)
    {
	
    }

    public void loadIntervalData(long startInterval, long endInterval)
    {
	
    }

    // These accessor methods should be used exclusively by IntervalData.java

    public int getNumIntervals() {
	return numIntervals;
    }

    protected List<RLEBlock>[] getData(int type) {
	return rawData[type];
    }
}
