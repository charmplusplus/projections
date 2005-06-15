package projections.analysis;

import java.io.*;

/**
 *  AccumulatedSummaryReader
 *  by Chee Wai Lee
 *  1/27/2004
 *
 *  This reader reads a single summary-type file with utilization
 *  data already accumulated across PEs at runtime.
 *
 *  This is the sparsest of all summary formats and is expected to be 
 *  in much greater use in the face of super-large machines like
 *  the 128K processor bluegene/L and 1 million-processor bluegene Cyclops
 *  machines.
 */
public class AccumulatedSummaryReader extends ProjectionsReader
    implements IntervalCapableReader
{
    // Static Data items
    public int numProcessors;
    public long numIntervals;
    public double intervalSize;
    public double totalTime;

    // Interval Data
    private double utilization[];  // indexed by interval number

    public AccumulatedSummaryReader(String filename, String version) {
	super(filename, version);
    }
   
    // ****** Contract requirements as a ProjectionsReader

    /**
     *  AccumulatedSummaryReader expects a file. 
     *  The availability check is implemented as such.
     */
    protected boolean checkAvailable() {
	File sourceFile = new File(sourceString);
	return sourceFile.canRead();
    }

    protected void readStaticData() 
	throws IOException
    {
	BufferedReader reader =
	    new BufferedReader(new FileReader(sourceString));
	ParseTokenizer tokenizer = initNewTokenizer(reader);
	
	// all IOExceptions here are caused by ParseTokenizer and hence
	// if they are caught, become a ProjectionsFormatException since
	// parsing failed.
	try {
	    // begin parsing the first line (header) of the summary file
	    tokenizer.checkNextString("ver");
	    double tempVersionNum = tokenizer.nextNumber("Version");
	    // **CW** tentatively, the version check is a numeric check.
	    // It is conceivable that the version tag need not be numeric.
	    if (tempVersionNum != Double.parseDouble(expectedVersion)) {
		throw new ProjectionsFormatException(expectedVersion,
						     "Expected version does " +
						     "not match one read on " +
						     "file!");
	    }

	    tokenizer.checkNextString("pes");
	    numProcessors = (int)tokenizer.nextNumber("Total Number of " +
						      "Processors");
	    tokenizer.checkNextString("count");
	    numIntervals = (long)tokenizer.nextNumber("Number of Intervals");
	    tokenizer.checkNextString("interval");
	    intervalSize = tokenizer.nextScientific("Interval Size") *
		1000000;  // convert from seconds to usecs
	    
	    // derived data
	    totalTime = numIntervals*intervalSize;

	    // check for extra stuff at the end of this line
	    if (!tokenizer.isEOL()) {
		throw new ProjectionsFormatException(expectedVersion,
						     "Extra Stuff at the " +
						     "end of header line.");
	    }
	} catch (IOException e) {
	    throw new ProjectionsFormatException(expectedVersion,
						 e.toString());
	}
	// now we are done with the header. Close the reader.
	reader.close();
    }
    
    public void reset() 
	throws IOException
    {
	// do nothing. This reader is not intended to work in a resetable
	// fashion.
    }

    private ParseTokenizer initNewTokenizer(Reader reader) {
	ParseTokenizer tokenizer;

	tokenizer=new ParseTokenizer(reader);
	tokenizer.parseNumbers();
	tokenizer.eolIsSignificant(true);
	tokenizer.whitespaceChars('/','/'); 
	tokenizer.whitespaceChars(':',':');
	tokenizer.whitespaceChars('[','[');
	tokenizer.whitespaceChars(']',']');
	tokenizer.wordChars('a','z');
	tokenizer.wordChars('A','Z');

	return tokenizer;
    }

    // ****** Contract methods as an IntervalCapableReader

    public double getIntervalSize() {
	return intervalSize;
    }

    /**
     *  Load the specified intervals using the natural intervalSize
     *  read from this summary file.
     */
    public void loadIntervalData(long startInterval, long endInterval)
	throws IOException 
    {
	loadIntervalData(intervalSize, startInterval, endInterval);
    }

    /**
     *  Load the specified intervals using a user-required intervalSize
     */
    public void loadIntervalData(double intervalSize,
				 long startInterval, long endInterval)
	throws IOException 
    {
	BufferedReader reader =
	    new BufferedReader(new FileReader(sourceString));
	ParseTokenizer tokenizer = initNewTokenizer(reader);

	// initialize utilization array
	int numIntervals = (int)(endInterval-startInterval+1);
	utilization = new double[numIntervals];

	// skip the first line (header)
	tokenizer.skipLine();

	// setting up read parameters
	double originalIntervalSize = this.intervalSize;

	double newStartTime = intervalSize*startInterval;
	double newEndTime = endInterval*intervalSize;
	
	long interval = 0;
	double delta = Double.MIN_VALUE; // to throw off exact values **HACK**

	while (!(tokenizer.isEOL())) {
	    // find out when actual data is to be read.
	    if ((interval+1)*originalIntervalSize < newStartTime) {
		// do nothing if end time of reading interval is not
		// past the start time of interest.
		interval++;
	    } else {
		double valueRead = tokenizer.nval;
		IntervalUtils.fillIntervals(utilization, 
					    (long)intervalSize,
					    startInterval,
					    (long)(interval *
						   originalIntervalSize),
					    (long)((interval+1) *
						   originalIntervalSize),
					    originalIntervalSize *
					    (valueRead/100),
					    false);
		interval++;
		// decide whether to stop
		if (interval*originalIntervalSize > newEndTime) {
		    break;
		}
	    }
	}
	if (tokenizer.isEOL()) {
	    throw new ProjectionsFormatException(expectedVersion,
						 "Unexpected EOL " +
						 "when reading " +
						 "utilization data!");
	}
	
	reader.close();

	// data read and filled is in the form of absolute time. Convert
	// to utilization.
	IntervalUtils.timeToUtil(utilization, intervalSize);
    }

    // ****** Accessors for interval data
    
    /** 
     *  getUtilData acquires the most recently loaded utilization data 
     *  from this Reader.
     *
     *  WARNING: The object acquiring this data is responsible for 
     *           removing all references to it or memory leaks WILL
     *           occur.
     */
    public double[] getUtilData() {
	return utilization;
    }

    public static void main(String args[]) {
	String filename = args[0];
	AccumulatedSummaryReader reader = 
	    new AccumulatedSummaryReader(filename, "5.0");
	// verify that header data was read.
	System.out.println(reader.getIntervalSize());
	System.out.println(reader.numProcessors);
	System.out.println(reader.numIntervals);
	System.out.println(reader.intervalSize);
	System.out.println(reader.totalTime);

	// VERY SPECIFIC to the file 
	// /scratch/namdlogs/32000/leanMD-bg.sum
	// on prowess.
	// READ and SPIT everything out.
	try {
	    reader.loadIntervalData(0, reader.numIntervals-1);
	    // now acquire the data and print.
	    double data[] = reader.getUtilData();
	    for (int i=0; i<data.length; i++) {
		System.out.print((long)(data[i]) + " ");
	    }
	    System.out.println();
	    reader.reset();
	} catch (IOException e) {
	    System.err.println(e.toString());
	}
    }
}
