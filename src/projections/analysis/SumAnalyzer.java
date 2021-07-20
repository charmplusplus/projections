package projections.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;

import javax.swing.ProgressMonitor;

import projections.gui.MainWindow;
import projections.misc.SummaryFormatException;

/** This class reads and analyzes .sum files.
 */

public class SumAnalyzer extends ProjDefs
{
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
	private int myRun = 0;

    // Summary modes (so that SumAnalyzer, as a data manager, can make use
    // of one or more data modes).
    private static final int ACC_MODE = 0;
    private static final int NORMAL_MODE = 1;

    private StreamTokenizer tokenizer;
    // Holds the total time (in microseconds) spent executing messages
    private long[][] ChareTime;   
    // directed to each entry method during the entire program run
    // (3rd line of sum file)
    // Holds the total number of messages sent to each entry method during
    private int[][] NumEntryMsgs;
    // Holds the maximum time each EP spent (line 5)
    private int[][] MaxEntryTime; 
    // the entire program run (4th line of the sum file)
    private int PhaseCount;
    private long IntervalSize;//Length of interval, microseconds
    private int IntervalCount;//Number of intervals
    private long TotalTime;//Length of run, microseconds
    private long[][][] PhaseChareTime;
    private int[][][] PhaseNumEntryMsgs;

    // The values in ProcessorUtilization and IdlePercentage are integer percentages
	// (0-100), so they are byte arrays to save space
    //Holds the second line of summary data
    private byte[][] ProcessorUtilization; 
    // Holds the seventh line of summary data
    private byte[][] IdlePercentage;

    private int mode = NORMAL_MODE;
    private int nPe;

    private AccumulatedSummaryReader accumulatedReader;

    // For now, the way to do the reading of super-summary files. It will
    // be incorporated into a better framework later.
//    public SumAnalyzer(StsReader stsReader, int mode) {
//	this.mode = mode;
//	accumulatedReader =
//	    new AccumulatedSummaryReader(MainWindow.runObject[myRun].getSumAccumulatedName(),
//					 "5.0");
//	TotalTime = (long)accumulatedReader.totalTime;
//	IntervalCount = (int)accumulatedReader.numIntervals;
//	IntervalSize = (long)accumulatedReader.intervalSize;
//    }

    /********************** State Variables *******************/
    public SumAnalyzer()
	throws IOException,SummaryFormatException
    {
	int tokenType;
	int CurrentUserEntry;
	int numEntry=0;
	double versionNum = 0.0;
	IntervalCount=0;
	TotalTime=0;
    nPe = 1;
	//ChareTime= new long [NumProcessors][NumUserEntries];			
	//NumEntryMsgs = new int [NumProcessors][NumUserEntries];

	// Perform a first pass of the reading to avoid having to
	// approximate the maximum number of intervals over all
	// processors. We must also present a unified interval
	// size for the whole system.
	ProgressMonitor progressBar;
	progressBar =
	    new ProgressMonitor(MainWindow.runObject[myRun].guiRoot, "Determining max intervals",
				"", 0, nPe);
	for (int p=0; p<nPe; p++) {
            if (!progressBar.isCanceled()) {
                progressBar.setNote(p + " of " + nPe);
                progressBar.setProgress(p);
            } else {
                System.err.println("Fatal error - Projections cannot" +
                                   " function without proper number of" +
				   " summary intervals!");
                System.exit(-1);
            }
	    FileReader file=new FileReader(MainWindow.runObject[myRun].getSumName(p));
	    BufferedReader b = new BufferedReader(file);
	    tokenizer = new StreamTokenizer(b);
	    //Set up the tokenizer
	    tokenizer.parseNumbers();
	    tokenizer.eolIsSignificant(true);
	    tokenizer.whitespaceChars('/','/');
	    tokenizer.whitespaceChars(':',':');
	    tokenizer.whitespaceChars('[','[');
	    tokenizer.whitespaceChars(']',']');
	    tokenizer.wordChars('a','z');
	    tokenizer.wordChars('A','Z');
	    //Read the first line (descriptive information)
	    checkNextString("ver");
	    versionNum = nextNumber("Version Number");
	    nextNumber("processor number");
	    nPe=(int)nextNumber("number of processors");
	    checkNextString("count");
	    int myCount= (int)nextNumber("count");
	    checkNextString("ep");
	    nextNumber("ep"); // do nothing with this.
	    checkNextString("interval");
	    double interval=nextScientific("intervalsize"); 
	    long myIntervalSize = (long)Math.floor(interval*1000000);
	    // determine base interval size, the finer the better.
	    double factor = 1.0;
	    if (myIntervalSize != IntervalSize) {
		if (IntervalSize == 0) {
		    IntervalSize = myIntervalSize;
		} else {
		    factor = myIntervalSize/(IntervalSize*1.0);
		    if (myIntervalSize < IntervalSize) {
			IntervalSize = myIntervalSize;
		    }
		}
	    }
	    // if a finer size was determined by this pe info, expand
	    // the number of previous counts by the factor.
	    if (factor < 1.0) {
		IntervalCount = (int)(IntervalCount/factor);
	    }
	    // compare previous interval counts with my current count
	    // multiplied by my factor
	    if (IntervalCount<(myCount*factor)) {
		IntervalCount=(int)(myCount*factor);
	    }
	    tokenizer = null;
	    file.close();
	}
	progressBar.close();

	// second pass
	progressBar = 
	    new ProgressMonitor(MainWindow.runObject[myRun].guiRoot, "Reading summary data",
				"", 0, nPe);
	for (int p = 0; p<nPe; p++) {
            if (!progressBar.isCanceled()) {
                progressBar.setNote(p + " of " + nPe);
                progressBar.setProgress(p);
            } else {
		progressBar.close();
		return;
            }
	    FileReader file=new FileReader(MainWindow.runObject[myRun].getSumName(p));
	    BufferedReader b = new BufferedReader(file);
	    tokenizer=new StreamTokenizer(b);
	    //Set up the tokenizer
	    tokenizer.parseNumbers();
	    tokenizer.eolIsSignificant(true);
	    tokenizer.whitespaceChars('/','/'); 
	    tokenizer.whitespaceChars(':',':');
	    tokenizer.whitespaceChars('[','[');
	    tokenizer.whitespaceChars(']',']');
	    tokenizer.wordChars('a','z');
	    tokenizer.wordChars('A','Z');
	    //Read the first line (descriptive information)
	    checkNextString("ver");
	    versionNum = nextNumber("Version Number");
	    nextNumber("processor number");
	    nPe=(int)nextNumber("number of processors");
	    checkNextString("count");
	    int myCount= (int)nextNumber("count");
	    checkNextString("ep");
	    numEntry=(int)nextNumber("number of entry methods");
	    checkNextString("interval");
	    double interval=nextScientific("processor usage sample interval"); 
	    long myIntervalSize = (long)Math.floor(interval*1000000);
	    if (TotalTime < myCount*myIntervalSize) {
		TotalTime = myCount*myIntervalSize;
	    }
	    int factor = (int)(myIntervalSize/IntervalSize);
	    if (versionNum > 2) {
		checkNextString("phases");
		PhaseCount = (int)nextNumber("phases");
	    } else {
		PhaseCount = 0;
	    }
	    if (StreamTokenizer.TT_EOL!=tokenizer.nextToken())
		throw new SummaryFormatException("extra garbage at end of line 1");
	    if (p==0) {
		ProcessorUtilization = new byte[nPe][];
		IdlePercentage = new byte[nPe][];
		ChareTime= new long [nPe][numEntry];
		NumEntryMsgs = new int [nPe][numEntry];
		MaxEntryTime = new int [nPe][numEntry];
	    }
	    ProcessorUtilization[p] = new byte[IntervalCount];
	    IdlePercentage[p] = new byte[IntervalCount];
	    
	    //Read the SECOND line (processor usage)
	    int nUsageRead=0;
	    // we perform on-the-fly expansion of larger interval sized
	    // data (ie. myIntervalSize > IntervalSize).
	    int val = 0;
	    int extraCount = 0;
	    while ((tokenType=tokenizer.nextToken()) != 
		   StreamTokenizer.TT_EOL) {
		if (tokenType == StreamTokenizer.TT_NUMBER) {
                    val =  (int)tokenizer.nval;
		    for (int f=0; f<factor; f++) {
			ProcessorUtilization[p][nUsageRead++] = (byte)val;
		    }
                    if ((tokenType=tokenizer.nextToken()) == '+') {
                        tokenType=tokenizer.nextToken();
                        if (tokenType !=  StreamTokenizer.TT_NUMBER) {
			    System.out.println("Unrecorgnized syntax at " +
					       "end of line 2");
			}
			extraCount = (int)tokenizer.nval;
			if (extraCount > myCount) {
			    System.err.println("[" + p + "] Token read = [" +
					       val + "+" + extraCount +
					       "] is impossible as the " +
					       "count is larger than the " +
					       "total count of " + myCount);
			    System.exit(-1);
			}
                        for (int i=1; i<extraCount; i++) {
			    for (int f=0; f<factor; f++) {
				ProcessorUtilization[p][nUsageRead++] = (byte)val;
			    }
			}
                    } else {
			tokenizer.pushBack();
		    }
		}
		if (nUsageRead > (myCount*factor)) {
		    System.err.println("[" + p + "] Corrupted data: " +
				       "Number of datapoints read exceeds " +
				       "file recorded value of " + myCount +
				       "adjusted by factor " + factor);
		    System.err.print("The violating token is [" + val);
		    if (extraCount > 1) {
			System.err.println("+" + extraCount + "]");
		    } else {
			System.err.println("]");
		    }
		    System.exit(-1);
		}
	    }
	    if ((myCount*factor) != nUsageRead) {
		System.err.println("numIntervals for Utilization do not agree: " + 
				   (myCount*factor) + " v.s. "+nUsageRead+"!");
	    }
	    // Read in the THIRD line (time spent by entries)
	    CurrentUserEntry = 0;
	    // **CW** for now, ignore the labels. Check to see if it is a 
	    // label. if yes, consume it. if not, push it back onto the 
	    // stream.
	    if ((StreamTokenizer.TT_WORD==
		 (tokenType=tokenizer.nextToken()))) {
		// do nothing. Label consumed.
		// System.out.println(tokenizer.sval + " read.");
	    } else {
		tokenizer.pushBack();
	    }
	    while ((StreamTokenizer.TT_NUMBER==
		    (tokenType=tokenizer.nextToken())) &&
		   (numEntry>CurrentUserEntry)) {
		ChareTime[p][CurrentUserEntry++] = (int)tokenizer.nval;
	    }
	    // Make sure we're at the end of the line
	    if (StreamTokenizer.TT_EOL!=tokenType)
		throw new SummaryFormatException("extra garbage at end of line 3");
	    // Read in the FOURTH line (number of messages)
	    // **CW** for now, ignore the labels. Check to see if it is a label
	    // if yes, consume it. if not, push it back onto the stream.
	    if ((StreamTokenizer.TT_WORD==(tokenType=tokenizer.nextToken()))) {
		// do nothing. Label consumed.
		// System.out.println(tokenizer.sval + " read.");
	    } else {
		tokenizer.pushBack();
	    }
	    CurrentUserEntry = 0;
	    while ((StreamTokenizer.TT_NUMBER==(tokenType=tokenizer.nextToken())) &&
		   (numEntry>CurrentUserEntry)) {
		NumEntryMsgs[p][CurrentUserEntry] = (int)tokenizer.nval;
		CurrentUserEntry++;
	    }
	    //Make sure we're at the end of the line
	    if (StreamTokenizer.TT_EOL!=tokenType)
		throw new SummaryFormatException("extra garbage at end of line 4");
	    // Read in the FIFTH line (Maximum EP Time)
	    // **CW** for now, ignore the labels. Check to see if it is a label
	    // if yes, consume it. if not, push it back onto the stream.
	    // this line applies only to version 4.0 and above.
	    if (versionNum > 3.0) {
		if ((StreamTokenizer.TT_WORD==(tokenType=tokenizer.nextToken()))) {
		    // do nothing. Label consumed.
		    // System.out.println(tokenizer.sval + " read.");
		} else {
		    tokenizer.pushBack();
		}
		CurrentUserEntry = 0;
		while ((StreamTokenizer.TT_NUMBER==(tokenType=tokenizer.nextToken())) &&
		       (numEntry>CurrentUserEntry)) {
		    MaxEntryTime[p][CurrentUserEntry] = (int)tokenizer.nval;
		    CurrentUserEntry++;
		}
		//Make sure we're at the end of the line
		if (StreamTokenizer.TT_EOL!=tokenType)
		    throw new SummaryFormatException("extra garbage at end of line 5");
	    }
	    if (versionNum > 3.0) {
		// Read in the SIXTH line (phase pairs)
		int NumberofPairs;
		// **CW** for now, ignore the labels. Check to see if it is a label
		// if yes, consume it. if not, push it back onto the stream.
		if ((StreamTokenizer.TT_WORD==(tokenType=tokenizer.nextToken()))) {
		    // do nothing. Label consumed.
		    // System.out.println(tokenizer.sval + " read.");
		} else {
		    tokenizer.pushBack();
		}
		NumberofPairs = (int)nextNumber("Number of Marked Events");
		for (int g=0; g<NumberofPairs; g++) {
		    nextNumber("Number of Marked Events");
		    nextNumber("Number of Marked Events");
		}
		//Make sure we're at the end of the line
		if (StreamTokenizer.TT_EOL!=(tokenType=tokenizer.nextToken()))
		    throw new SummaryFormatException("extra garbage at end of line 6");
		if (PhaseCount > 1) {				
		    if (p == 0) {
			PhaseChareTime= new long [PhaseCount][nPe][numEntry];
			PhaseNumEntryMsgs = new int [PhaseCount][nPe][numEntry];
		    }
		    for(int m=0; m<PhaseCount; m++) {		
			CurrentUserEntry = 0;
			tokenizer.nextToken();
			tokenizer.nextToken();
			while ((StreamTokenizer.TT_NUMBER==(tokenType=tokenizer.nextToken())) &&
			       (numEntry>CurrentUserEntry)) {
			    PhaseNumEntryMsgs[m][p][CurrentUserEntry] = 
				(int)tokenizer.nval;
			    CurrentUserEntry++;
			}
			// Make sure we're at the end of the line
			if (StreamTokenizer.TT_EOL!=tokenType)
			    throw new SummaryFormatException("extra garbage at end of line 3");
			// Read in the FOURTH line
			CurrentUserEntry = 0;
			tokenizer.nextToken();
			tokenizer.nextToken();
			while ((StreamTokenizer.TT_NUMBER==(tokenType=tokenizer.nextToken())) &&
			       (numEntry>CurrentUserEntry)) {
			    PhaseChareTime[m][p][CurrentUserEntry] = 
				(int)tokenizer.nval;
			    CurrentUserEntry++;
			}
			//Make sure we're at the end of the line
			if (StreamTokenizer.TT_EOL!=tokenType)
			    throw new SummaryFormatException("extra garbage at end of line 4");
		    }
		}
	    }

	    //Read the SEVENTH line (processor usage)
	    // **CW** for now, ignore the labels. Check to see if it is a label
	    // if yes, consume it. if not, push it back onto the stream.
	    if (versionNum > 7.0) {
		if ((StreamTokenizer.TT_WORD==
		     (tokenType=tokenizer.nextToken()))) {
		    // do nothing. Label consumed.
		    // System.out.println(tokenizer.sval + " read.");
		} else {
		    tokenizer.pushBack();
		}
		int nIdleRead=0;
		// we perform on-the-fly expansion of larger interval sized
		// data (ie. myIntervalSize > IntervalSize).
		val = 0;
		extraCount = 0;
		while ((tokenType=tokenizer.nextToken()) != 
		       StreamTokenizer.TT_EOL) {
		    if (tokenType == StreamTokenizer.TT_NUMBER) {
			val =  (int)tokenizer.nval;
			for (int f=0; f<factor; f++) {
			    IdlePercentage[p][nIdleRead++] = (byte)val;
			}
			if ((tokenType=tokenizer.nextToken()) == '+') {
			    tokenType=tokenizer.nextToken();
			    if (tokenType !=  StreamTokenizer.TT_NUMBER) {
				System.out.println("Unrecorgnized syntax at " +
						   "end of line 2");
			    }
			    extraCount = (int)tokenizer.nval;
			    if (extraCount > myCount) {
				System.err.println("["+p+"] Token read = [" +
						   val + "+" + extraCount +
						   "] is impossible as the " +
						   "count is larger than " +
						   "the total count of " + 
						   myCount);
				System.exit(-1);
			    }
			    for (int i=1; i<extraCount; i++) {
				for (int f=0; f<factor; f++) {
				    IdlePercentage[p][nIdleRead++] = (byte)val;
				}
			    }
			} else {
			    tokenizer.pushBack();
			}
		    }
		    if (nIdleRead > (myCount*factor)) {
			System.err.println("[" + p + "] Corrupted data: " +
					   "Number of datapoints read " +
					   "exceeds " +
					   "file recorded value of " + 
					   myCount +
					   "adjusted by factor " + factor);
			System.err.print("The violating token is [" + val);
			if (extraCount > 1) {
			    System.err.println("+" + extraCount + "]");
			} else {
			    System.err.println("]");
			}
			System.exit(-1);
		    }
		}
		if ((myCount*factor) != nIdleRead) {
		    System.err.println("numIntervals for Idle do not agree: " + 
				       (myCount*factor) + " v.s. "+
				       nIdleRead+"!");
		}
	    }
	    tokenizer = null;
	    file.close();
	    // System.out.println("Finished reading in data for processor #"+p+"/"+nPe);
	}
	progressBar.close();
	MainWindow.runObject[myRun].setTotalTime(TotalTime);
    }

    private void checkNextString(String expected) 
	throws IOException,SummaryFormatException
    {
	String ret=nextString(expected);
	if (!expected.equals(ret))
	    throw new SummaryFormatException("Expected "+expected+" got "+ret);
    }

    public long getIntervalSize() {
	return IntervalSize;
    }

    public long[][] getChareTime()
    {
	return ChareTime;
    }

    public long[][] getPhaseChareTime(int Phase)
    {
	return PhaseChareTime[Phase];
    }

    public int getPhaseCount() 
    {
	return PhaseCount;
    }

    /**
     * Resample ProcessorUtilization data into SystemUsageData.
     */
    public int[][] getSystemUsageData(int intervalStart, int intervalEnd, 
				      long outIntervalSize)
    {
	int numProcessors=ProcessorUtilization.length;
	int intervalRange = intervalEnd - intervalStart + 1;
	int [][]ret = new int[numProcessors][intervalRange];

	// **CW** optimization, if outIntervalSize == IntervalSize, just
	// copy the appropriate parts of the internal array!
	if (outIntervalSize == IntervalSize) {
	    for (int p=0; p<numProcessors; p++) {
		for (int i=intervalStart; 
		     (i<intervalEnd) && (i<ProcessorUtilization[p].length); 
		     i++) {
		    ret[p][i-intervalStart] = 
			ProcessorUtilization[p][i];
		}
	    }
	    return ret;
	}

	int sourceStartInterval = 
	    (int)((intervalStart*outIntervalSize)/IntervalSize);
	double [][]tempData = new double[numProcessors][intervalRange];
	for (int p=0; p<numProcessors; p++) {
	    int interval = sourceStartInterval;
	    while ((interval < ProcessorUtilization[p].length) &&
		   (interval < sourceStartInterval+intervalRange)){
		// **CW** optimization, if there's nothing to spread, then
		// don't spread it!
		if (ProcessorUtilization[p][interval] > 0) {
		    IntervalUtils.fillIntervals(tempData[p],
						outIntervalSize,
						intervalStart,
						interval*IntervalSize,
						(interval+1)*IntervalSize,
						IntervalUtils.utilToTime(ProcessorUtilization[p][interval],
									 IntervalSize),
						false);
		}
		interval++;
	    }
	}
	for (int p=0; p<tempData.length; p++) {
	    for (int i=0; i<tempData[p].length; i++) {
		tempData[p][i] = 
		    IntervalUtils.timeToUtil(tempData[p][i],
					     outIntervalSize);
		ret[p][i] = (int)tempData[p][i];
	    }
	}
	return ret;
    }

    /**
     *
     */
    public double[][] getSummaryAverageData() {
	if (mode == NORMAL_MODE) {
	    int numProcessors = ProcessorUtilization.length;
	    double[][] ret = new double[IntervalCount][2];
	    for (int p=0; p<numProcessors; p++) {
		for (int interval=0; interval<IntervalCount; interval++) {
		    ret[interval][0] += ProcessorUtilization[p][interval];
		    ret[interval][1] += IdlePercentage[p][interval];
		}
	    }
	    for (int interval=0; interval<IntervalCount; interval++) {
		ret[interval][0] /= numProcessors*1.0;
		ret[interval][1] /= numProcessors*1.0;
	    }
	    return ret;
	} else if (mode == ACC_MODE) {
	    double[][] ret;
	    try {
		accumulatedReader.loadIntervalData(0, IntervalCount-1);
	    } catch (IOException e) {
		System.err.println("Exception caught!");
		System.exit(-1);
	    }
	    double[] temp = accumulatedReader.getUtilData();
	    ret = new double[temp.length][1];
	    for (int interval = 0; interval<temp.length; interval++) {
		ret[interval][0] = temp[interval];
	    }
	    return ret;
	}
	return null;
    }

    public long getTotalTime() 
    {
	return TotalTime;
    }
	
    private double nextNumber(String description) 
	throws IOException,SummaryFormatException
    {
	if (StreamTokenizer.TT_NUMBER!=tokenizer.nextToken()) 
	    throw new SummaryFormatException("Couldn't read "+description);
	return tokenizer.nval;
    }

    private double nextScientific(String description) 
	throws IOException,SummaryFormatException
    {
	double mantissa=nextNumber(description+" mantissa");
	String expString=nextString(description+" exponent");
	char expChar=expString.charAt(0);
	if (expChar!='e'&&expChar!='d'&&expChar!='E'&&expChar!='D')
	    throw new SummaryFormatException("Couldn't find exponent in "+
					     expString);
	int exponent;
	expString=expString.substring(1);//Clip off leading "e"
	// the old scheme does not take into account the fact that 
	// "e+kk" is a valid exponent string.
	if (expString.equals("")) {
	    tokenizer.nextToken(); // get rid of the stupid "+" symbol
	    exponent=(int)(nextNumber("exponent"));
	} else {
	    try { 
		exponent=Integer.parseInt(expString);
	    } catch (NumberFormatException e) {
		throw new SummaryFormatException("Couldn't parse exponent "+
						 expString);
	    }
	}
	return mantissa*Math.pow(10.0,exponent);
    }

    private String nextString(String description) 
	throws IOException,SummaryFormatException
    {
	if (StreamTokenizer.TT_WORD!=tokenizer.nextToken()) 
	    throw new SummaryFormatException("Couldn't read string "+
					     description);
	return tokenizer.sval;
    }

    public int[] getTotalIdlePercentage(){
        int[] totalIdlePercentage = new int[IntervalCount];
        for(int i=0; i<nPe; i++){
            for(int j=0; j<IntervalCount; j++){
                totalIdlePercentage[j] += IdlePercentage[i][j] / IntervalCount;
            }
        }
        return totalIdlePercentage;
    }
	public byte[][] IdlePercentage(){
		return IdlePercentage;
	}

}
