package projections.analysis;

import java.lang.*;
import java.io.*;
import java.util.*;

import java.awt.event.*;
import javax.swing.*;

import projections.gui.*;
import projections.misc.*;

/** This class reads and analyzes .sum files.
 */

public class SumAnalyzer extends ProjDefs
{
    // Summary modes (so that SumAnalyzer, as a data manager, can make use
    // of one or more data modes).
    public static final int NUM_MODES = 2;
    public static final int ACC_MODE = 0;
    public static final int NORMAL_MODE = 1;

    private int[][][][] dataArray;
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
    //Holds the second line of summary data
    private int[][] ProcessorUtilization; 

    private int mode = NORMAL_MODE;

    private AccumulatedSummaryReader accumulatedReader;

    // For now, the way to do the reading of super-summary files. It will
    // be incorporated into a better framework later.
    public SumAnalyzer(StsReader stsReader, int mode) {
	this.mode = mode;
	accumulatedReader =
	    new AccumulatedSummaryReader(stsReader.getSumAccumulatedName(),
					 "5.0");
	TotalTime = (long)accumulatedReader.totalTime;
	IntervalCount = (int)accumulatedReader.numIntervals;
	IntervalSize = (long)accumulatedReader.intervalSize;
    }

    /********************** State Variables *******************/
    public SumAnalyzer()
	throws IOException,SummaryFormatException
    {
	long Filled;
	double value;
	int tokenType;
	int CurrentUserEntry;
	int nPe=1,numEntry=0,versionNum=0;
	IntervalCount=0;
	TotalTime=0;
	//ChareTime= new long [NumProcessors][NumUserEntries];			
	//NumEntryMsgs = new int [NumProcessors][NumUserEntries];

	// **CW** Perform a first pass of the reading to avoid having to
	// approximate the maximum number of intervals over all
	// processors.
	ProgressMonitor progressBar;
	progressBar =
	    new ProgressMonitor(Analysis.guiRoot, "Determining max intervals",
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
	    FileReader file=new FileReader(Analysis.getSumName(p));
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
	    versionNum = (int)nextNumber("Version Number");
	    int myProcessor=(int)nextNumber("processor number");
	    nPe=(int)nextNumber("number of processors");
	    checkNextString("count");
	    int myCount= (int)nextNumber("count");
	    if (IntervalCount<myCount) IntervalCount=myCount;
	    tokenizer = null;
	    file.close();
	}
	progressBar.close();

	// second pass
	progressBar = 
	    new ProgressMonitor(Analysis.guiRoot, "Reading summary data",
				"", 0, nPe);
	for (int p = 0; p <nPe; p++) {
            if (!progressBar.isCanceled()) {
                progressBar.setNote(p + " of " + nPe);
                progressBar.setProgress(p);
            } else {
		progressBar.close();
		return;
            }
	    FileReader file=new FileReader(Analysis.getSumName(p));
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
	    versionNum = (int)nextNumber("Version Number");
	    int myProcessor=(int)nextNumber("processor number");
	    nPe=(int)nextNumber("number of processors");
	    checkNextString("count");
	    int myCount= (int)nextNumber("count");
	    /** **CW** No longer done because the approximations
	     *  are failing for extremely large interval counts.
	     if (IntervalCount<myCount) IntervalCount=myCount;
	    */
	    checkNextString("ep");
	    numEntry=(int)nextNumber("number of entry methods");
	    checkNextString("interval");
	    double interval=nextScientific("processor usage sample interval"); 
	    IntervalSize = (long)Math.floor(interval*1000000);
	    /** **CW** 
	     *  The IntervalCount may be known now, but the IntervalSize
	     *  may still vary. In this case, TotalTime computed the
	     *  old way is no longer valid. Instead TotalTime should be
	     *  computed using the current summary file's interval
	     *  count (myCount) multiplied by the current IntervalSize.
	     if (TotalTime < IntervalCount*IntervalSize)
	     TotalTime = IntervalCount*IntervalSize;
	    */
	    if (TotalTime < myCount*IntervalSize) {
		TotalTime = myCount*IntervalSize;
	    }
	    if (versionNum > 2) {
		checkNextString("phases");
		PhaseCount = (int)nextNumber("phases");
	    } else {
		PhaseCount = 1;
	    }
	    if (StreamTokenizer.TT_EOL!=tokenizer.nextToken())
		throw new SummaryFormatException("extra garbage at end of line 1");
	    if (p==0) {
		ProcessorUtilization = new int[nPe][];
		ChareTime= new long [nPe][numEntry];
		NumEntryMsgs = new int [nPe][numEntry];
		MaxEntryTime = new int [nPe][numEntry];
	    }
	    ProcessorUtilization[p] = new int[IntervalCount];
	    
	    //Read the SECOND line (processor usage)
	    int nUsageRead=0;
	    boolean error = false;
	    
	    while ((tokenType=tokenizer.nextToken()) != 
		   StreamTokenizer.TT_EOL && nUsageRead < myCount) {
		if (tokenType == StreamTokenizer.TT_NUMBER) {
                    int val =  (int)tokenizer.nval;
            	    ProcessorUtilization[p][nUsageRead++] = val;
                    if ((tokenType=tokenizer.nextToken()) == '+') {
                        tokenType=tokenizer.nextToken();
                        if (tokenType !=  StreamTokenizer.TT_NUMBER)
			    System.out.println("Unrecorgnized syntax at end of line 2");
                        for (int i=1; i<(int)tokenizer.nval; i++)
			    ProcessorUtilization[p][nUsageRead++] = val;
                    } else {
			tokenizer.pushBack();
		    }
		} else {
	            System.out.println("extra garbage at end of line 2");
		}
	    }
	    if (myCount != nUsageRead) {
		System.out.println("numIntervals not agree" + 
				   IntervalCount + "v.s. " + nUsageRead+"!");
	    }
	    // Read in the THIRD line (time spent by entries)
	    CurrentUserEntry = 0;
	    // **CW** for now, ignore the labels. Check to see if it is a label
	    // if yes, consume it. if not, push it back onto the stream.
	    if ((StreamTokenizer.TT_WORD==(tokenType=tokenizer.nextToken()))) {
		// do nothing. Label consumed.
		// System.out.println(tokenizer.sval + " read.");
	    } else {
		tokenizer.pushBack();
	    }
	    while ((StreamTokenizer.TT_NUMBER==(tokenType=tokenizer.nextToken())) &&
		   (numEntry>CurrentUserEntry)) {
		ChareTime[p][CurrentUserEntry] = (int)tokenizer.nval;
		CurrentUserEntry++;
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
	    // System.out.println("num pairs is " + NumberofPairs);
	    for (int g=0; g<NumberofPairs; g++) {
		nextNumber("Number of Marked Events");
		nextNumber("Number of Marked Events");
	    }
	    //Make sure we're at the end of the line
	    /* **CW** No need for that. Unlike the previous code where we
	       use a while loop, here we know exactly what we want.
	       if (StreamTokenizer.TT_EOL!=tokenType)
	           throw new SummaryFormatException("extra garbage at end of line 6");
	    */
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
	    tokenizer = null;
	    file.close();
	    // System.out.println("Finished reading in data for processor #"+p+"/"+nPe);
	}
	progressBar.close();
	Analysis.setTotalTime(TotalTime);
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

    public long[][] GetChareTime()
    {
	return ChareTime;
    }

    public int[][] GetNumEntryMsgs()
    {
	return NumEntryMsgs;
    }

    public long[][] GetPhaseChareTime(int Phase)
    {
	return PhaseChareTime[Phase];
    }

    public int GetPhaseCount() 
    {
	return PhaseCount;
    }

    public int[][] GetPhaseNumEntryMsgs(int Phase)
    {
	return PhaseNumEntryMsgs[Phase];
    }

    /**
     * Resample ProcessorUtilization data into SystemUsageData.
     */
    public int[][] GetSystemUsageData(int intervalStart, int intervalEnd, 
				      long OutIntervalSize) 
	throws IOException,SummaryFormatException
    {
	int intervalRange = intervalEnd - intervalStart + 1;
	
	int NumProcessors=ProcessorUtilization.length;
	int[][] ret = new int[NumProcessors][intervalRange];
	for (int p = 0; p < NumProcessors; p++) {
	    int in = 0, out=0; //Indices into ProcessorUtilization[p] and ret[p]
	    int usage=0,nUsage=0; //Accumulated processor usage
	    int out_t=0; //Accumulated time in output array
	    while(out < intervalRange) {
		if (in <ProcessorUtilization[p].length)
		    usage += ProcessorUtilization[p][in];
		nUsage++;
		in++;
		out_t += IntervalSize;
		if (out_t >= OutIntervalSize) {
		    ret[p][out++] = (int)(usage/nUsage);
		    out_t = 0;
		    usage=0;nUsage=0;
		}
	    }
	}
	return ret;
    }

    /**
     *
     */
    public double[] getSummaryAverageData() {
	if (mode == NORMAL_MODE) {
	    int numProcessors = ProcessorUtilization.length;
	    double[] ret = new double[IntervalCount];
	    for (int p=0; p<numProcessors; p++) {
		for (int interval=0; interval<IntervalCount; interval++) {
		    ret[interval] += ProcessorUtilization[p][interval];
		}
	    }
	    for (int interval=0; interval<IntervalCount; interval++) {
		ret[interval] /= numProcessors*1.0;
	    }
	    return ret;
	} else if (mode == ACC_MODE) {
	    double[] ret;
	    try {
		accumulatedReader.loadIntervalData(0, IntervalCount-1);
	    } catch (IOException e) {
		System.err.println("Exception caught!");
		System.exit(-1);
	    }
	    ret = accumulatedReader.getUtilData();
	    return ret;
	}
	return null;
    }

    public long GetTotalTime() 
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
	try { 
	    exponent=Integer.parseInt(expString);
	} catch (NumberFormatException e) {
	    throw new SummaryFormatException("Couldn't parse exponent "+
					     expString);
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
}
