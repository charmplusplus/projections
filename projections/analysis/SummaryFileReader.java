package projections.analysis;

import java.io.*;

/**
 *   SummaryFileReader
 *   by Chee Wai Lee
 *   1/24/2004
 *
 *   This class's job is to read a single projections summary file and 
 *   provide data suitable for interval-based analysis.
 */
public class SummaryFileReader extends ProjectionsReader
    implements IntervalCapableReader
{
    // Static Data items
    public int processor;
    public int numProcessors;
    public long numIntervals;
    public int numEntries;
    public double intervalSize;
    public double totalTime;
    public long numPhases;

    // Interval Data
    private double utilization[];  // indexed by interval number

    // The following are very specialized non-interval summary data
    // that are probably safe to keep in memory.
    private long totalEPExecTime[];  // indexed by EPid
    private long timesEPcalled[];    // indexed by EPid
    private long maxEPExecTime[];    // indexed by EPid

    // Phase Data - this is a potentially large array. Do not read
    // until required (like Interval Data)
    private long phaseEPExecTime[][]; // indexed by Phase Idx, EPid
    private long phaseEPNumMsgs[][];  // indexed by Phase Idx, EPid

    public SummaryFileReader(String filename, String version) {
	super(filename, version);
	try {
	    // This Reader's specialized data are small enough to warrant
	    // an attempt to read at constructor time.
	    readSpecializedData();
	} catch (ProjectionsFormatException e) {
	    System.err.println("Format Exception when reading from " +
			       "source [" + sourceString + "]");
	    System.err.println(e.toString());
	    System.err.println("Data is now marked as unavailable.");
	    markUnavailable();
	} catch (IOException e) {
	    System.err.println("Unexpected IO error when reading from " +
			       "source [" + sourceString + "]");
	    System.err.println("Data is now marked as unavailable.");
	    markUnavailable();
	}
    }
   
    // ****** Contract requirements as a ProjectionsReader

    /**
     *  SummaryFileReader expects a file. The availability check is implemented
     *  as such.
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
	    processor = (int)tokenizer.nextNumber("Processor ID");
	    numProcessors = (int)tokenizer.nextNumber("Total Number of " +
						      "Processors");
	    tokenizer.checkNextString("count");
	    numIntervals = (long)tokenizer.nextNumber("Number of Intervals");
	    tokenizer.checkNextString("ep");
	    numEntries = (int)tokenizer.nextNumber("Number of Entry Methods");
	    tokenizer.checkNextString("interval");
	    intervalSize = tokenizer.nextScientific("Interval Size") *
		1000000;  // convert from seconds to usecs
	    
	    // derived data
	    totalTime = numIntervals*intervalSize;

	    // **CW** I am not sure if this is the best way to do version
	    // control. It should work for now.
	    if (tempVersionNum > 2.0) {
		tokenizer.checkNextString("phases");
		numPhases = (long)tokenizer.nextNumber("Number of Phases");
	    } else {
		numPhases = 1;
	    }
	    
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

    /**
     *  Reads and fills the arrays for various EP-based summaries.
     *  These should not be large arrays.
     */
    private void readSpecializedData() 
	throws IOException
    {
	BufferedReader reader =
	    new BufferedReader(new FileReader(sourceString));
	ParseTokenizer tokenizer = initNewTokenizer(reader);

	reader.close();
    }

    // ****** Contract methods as an IntervalCapableReader

    public double getIntervalSize() {
	return intervalSize;
    }

    /**
     *  Load the specified intervals using the natural intervalSize
     *  read from this summary file. This is a special case.
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
	if (Double.parseDouble(expectedVersion) >= 3.0) {
	    // VERSION 3.0 and above -> delta encoding for util data
	    while (!tokenizer.testEOL()) {
		if (tokenizer.nextToken() == StreamTokenizer.TT_NUMBER) {
		    double valueRead = tokenizer.nval;
		    long numInBlock = 1; // at least one must be read.
		    if (tokenizer.nextToken() == '+') {
			if (tokenizer.nextToken() != 
			    StreamTokenizer.TT_NUMBER) {
			    throw new ProjectionsFormatException(expectedVersion,
								 "Corrupt " +
								 "util " +
								 "data!");
			}
			numInBlock = (long)tokenizer.nval;
		    } else { // not a '+' token
			tokenizer.pushBack();
		    }
		    // convert to a time value for redestribution to a new
		    // array.
		    IntervalUtils.fillIntervals(utilization, intervalSize,
						(int)startInterval, 
						interval*originalIntervalSize,
						(interval+numInBlock)*originalIntervalSize-delta,
						numInBlock*originalIntervalSize*(valueRead/100),
						false);
		    interval += numInBlock;
		    // decide whether to stop
		    if (interval*originalIntervalSize > newEndTime) {
			break;
		    }
		} else {
		    throw new ProjectionsFormatException(expectedVersion,
							 "Corrupt util " +
							 "data!");
		}
	    }
	} else {
	    // VERSION 2.0 and below -> no delta encoding for util data
	    while (!(tokenizer.isEOL())) {
		// find out when actual data is to be read.
		if ((interval+1)*originalIntervalSize < newStartTime) {
		    // do nothing if end time of reading interval is not
		    // past the start time of interest.
		    interval++;
		} else {
		    double valueRead = tokenizer.nval;
		    IntervalUtils.fillIntervals(utilization, intervalSize,
						(int)startInterval,
						interval*originalIntervalSize,
						(interval+1)*originalIntervalSize-delta,
						originalIntervalSize*(valueRead/100),
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

    // ****** Additional Data load methods for phase information (if any)

    public void loadPhaseData(long startPhase, long endPhase) 
	throws IOException
    {
	
    }

    public static void main(String args[]) {
	String filename = args[0];
	SummaryFileReader reader = new SummaryFileReader(filename, "4.0");
	// verify that header data was read.
	System.out.println(reader.getIntervalSize());
	System.out.println(reader.processor);
	System.out.println(reader.numProcessors);
	System.out.println(reader.numIntervals);
	System.out.println(reader.numEntries);
	System.out.println(reader.intervalSize);
	System.out.println(reader.totalTime);
	System.out.println(reader.numPhases);

	// test the reading of 10 pieces of meaningful data
	// VERY SPECIFIC to the file 
	// /scratch/namdlogs/100-nosync_Jan09/namd2.nosync.proj.0.sum
	// on prowess.
	// results should read - 0 22 41 0 16 15 15 0 0 0
	long startIndex = reader.numIntervals - 17679 - 7;
	try {
	    // in the middle test, default
	    reader.loadIntervalData(startIndex, startIndex+9);
	    // now acquire the data and print.
	    double data[] = reader.getUtilData();
	    for (int i=0; i<data.length; i++) {
		System.out.print(data[i] + " ");
	    }
	    System.out.println();
	    reader.reset();
	    // in the middle test, sanity check on general case.
	    reader.loadIntervalData(reader.getIntervalSize(),
				    startIndex, startIndex+9);
	    // now acquire the data and print.
	    data = reader.getUtilData();
	    for (int i=0; i<data.length; i++) {
		System.out.print(data[i] + " ");
	    }
	    System.out.println();
	    reader.reset();
	    // on the start boundary test
	    reader.loadIntervalData(0, 10);
	    // now acquire the data and print.
	    data = reader.getUtilData();
	    for (int i=0; i<data.length; i++) {
		System.out.print(data[i] + " ");
	    }
	    System.out.println();
	    reader.reset();
	    // on the end boundary test
	    reader.loadIntervalData(reader.numIntervals-11, 
				    reader.numIntervals-1);
	    // now acquire the data and print.
	    data = reader.getUtilData();
	    for (int i=0; i<data.length; i++) {
		System.out.print(data[i] + " ");
	    }
	    System.out.println();
	    reader.reset();
	    // cross the end boundary test failure test
	    reader.loadIntervalData(reader.numIntervals-11, 
				    reader.numIntervals+5);
	    // now acquire the data and print.
	    data = reader.getUtilData();
	    for (int i=0; i<data.length; i++) {
		System.out.print(data[i] + " ");
	    }
	    System.out.println();
	    reader.reset();
	} catch (IOException e) {
	    System.err.println(e.toString());
	}
    }
}
