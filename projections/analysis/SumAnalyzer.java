package projections.analysis;

import java.lang.*;
import java.io.*;
import projections.misc.*;
import java.util.*;

/** This class reads and analyzes .sum files.
 */

public class SumAnalyzer extends ProjDefs
{
	private StsReader sts;
	private int 	  NumIntervals;
	private int[][][][] dataArray;
	private StreamTokenizer tokenizer;
	private long[][] ChareTime;   // Holds the total time (in microseconds) spent executing messages
								// directed to each entry method during the entire program run	(3rd line of sum file)
	private int[][] NumEntryMsgs; // Holds the total number of messages sent to each entry method during
								// the entire program run (4th line of the sum file)
	private int PhaseCount;
	private long IntervalSize;//Length of interval, microseconds
	private int IntervalCount;//Number of intervals
	private long TotalTime;//Length of run, microseconds
	private long[][][] PhaseChareTime;
	private int[][][] PhaseNumEntryMsgs;
	private int[][] ProcessorUtilization; //Holds the second line of summary data

/********************** State Variables *******************/
	public SumAnalyzer (StsReader Nsts) 
		throws IOException,SummaryFormatException
	{
	sts=Nsts;
	
	long Filled;
	double value;
	int tokenType;
	int CurrentUserEntry;
	int nPe=1,numEntry=0,versionNum=0;
	IntervalCount=0;
	TotalTime=0;
	//ChareTime= new long [NumProcessors][NumUserEntries];			
	//NumEntryMsgs = new int [NumProcessors][NumUserEntries];
	for (int p = 0; p <nPe; p++)
		{

		FileReader file=new FileReader(sts.getSumName(p));
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
		int myCount = (int)nextNumber("count");
		if (IntervalCount<myCount) IntervalCount=myCount;
		checkNextString("ep");
		numEntry=(int)nextNumber("number of entry methods");
		checkNextString("interval");
		double interval=nextScientific("processor usage sample interval"); 
		IntervalSize = (long)Math.floor(interval*1000000);
		if (TotalTime < IntervalCount*IntervalSize)
			TotalTime = IntervalCount*IntervalSize;
		
		if (versionNum > 2)
			{
			checkNextString("phases");
			PhaseCount = (int)nextNumber("phases");
			}
		else 
			PhaseCount = 1;
		
		if (StreamTokenizer.TT_EOL!=tokenizer.nextToken())
			throw new SummaryFormatException("extra garbage at end of line 1");
		if (p==0)
		{
			ProcessorUtilization = new int[nPe][];
			ChareTime= new long [nPe][numEntry];
			NumEntryMsgs = new int [nPe][numEntry];
		}
		ProcessorUtilization[p] = new int[IntervalCount + 20];

		//Read the SECOND line (processor usage)
		int nUsageRead=0;
		boolean error = false;
/*
     OLD FORMAT === UNCOMPRESSED ==
		while (StreamTokenizer.TT_NUMBER==(tokenType=tokenizer.nextToken())) {
		  try { ProcessorUtilization[p][nUsageRead++] = (int)tokenizer.nval; }
		  catch (ArrayIndexOutOfBoundsException e) {
		    if (!error) {
		      System.out.println("  ArrayIndexOutOfBoundsException nUsageRead "+
					 nUsageRead+" size "+ProcessorUtilization[p].length);
		      error = true;
		    }
		  }
		}
		if (error) { System.out.println("  nUsageRead "+nUsageRead); }

		//Make sure we're at the end of the line
		if (StreamTokenizer.TT_EOL!=tokenType)
			throw new SummaryFormatException("extra garbage at end of line 2");
*/

        	while ((tokenType=tokenizer.nextToken()) != 
		        StreamTokenizer.TT_EOL && nUsageRead < myCount) 
		{
          	  if (tokenType == StreamTokenizer.TT_NUMBER) {
                    int val =  (int)tokenizer.nval;
            	    ProcessorUtilization[p][nUsageRead++] = val;
                    if ((tokenType=tokenizer.nextToken()) == '+') 
		    {
                        tokenType=tokenizer.nextToken();
                        if (tokenType !=  StreamTokenizer.TT_NUMBER)
	                  System.out.println("Unrecorgnized syntax at end of line 2");
                        for (int i=1; i<(int)tokenizer.nval; i++)
                        ProcessorUtilization[p][nUsageRead++] = val;
                    }
                    else
                      tokenizer.pushBack();
                  }
                  else 
	            System.out.println("extra garbage at end of line 2");
	        }
                if (myCount != nUsageRead) 
                  System.out.println("numIntervals not agree" + IntervalCount + "v.s. " + nUsageRead+"!");
		   
		// Read in the THIRD line (time spent by entries)
		CurrentUserEntry = 0;
		while ((StreamTokenizer.TT_NUMBER==(tokenType=tokenizer.nextToken()))&&(numEntry>CurrentUserEntry))
			{
			ChareTime[p][CurrentUserEntry] = (int)tokenizer.nval;
			CurrentUserEntry++;
			}	
				// Make sure we're at the end of the line
		if (StreamTokenizer.TT_EOL!=tokenType)
			throw new SummaryFormatException("extra garbage at end of line 3");
		
		// Read in the FOURTH line (number of messages)
		CurrentUserEntry = 0;
		while ((StreamTokenizer.TT_NUMBER==(tokenType=tokenizer.nextToken()))&&(numEntry>CurrentUserEntry))
			{
			NumEntryMsgs[p][CurrentUserEntry] = (int)tokenizer.nval;
			CurrentUserEntry++;
			}
		
		//Make sure we're at the end of the line
		if (StreamTokenizer.TT_EOL!=tokenType)
			throw new SummaryFormatException("extra garbage at end of line 4");
		
		
		// Read in the FIFTH line
		int NumberofPairs;
		NumberofPairs = (int)nextNumber("Number of Marked Events");
		for (int g=0; g<NumberofPairs; g++)
			{
			nextNumber("Number of Marked Events");
			nextNumber("Number of Marked Events");
			}
		//Make sure we're at the end of the line
		if (StreamTokenizer.TT_EOL!=tokenType)
			throw new SummaryFormatException("extra garbage at end of line 5");
		
		if (PhaseCount > 1)
			{				
			if (p == 0)
				{
				PhaseChareTime= new long [PhaseCount][nPe][numEntry];
				PhaseNumEntryMsgs = new int [PhaseCount][nPe][numEntry];
				}
			for(int m=0; m<PhaseCount; m++)
				{		
				CurrentUserEntry = 0;
				tokenizer.nextToken();
				tokenizer.nextToken();
				while ((StreamTokenizer.TT_NUMBER==(tokenType=tokenizer.nextToken()))&&(numEntry>CurrentUserEntry))
					{
					PhaseNumEntryMsgs[m][p][CurrentUserEntry] = (int)tokenizer.nval;
					CurrentUserEntry++;
					}
				// Make sure we're at the end of the line
				if (StreamTokenizer.TT_EOL!=tokenType)
					throw new SummaryFormatException("extra garbage at end of line 3");
				// Read in the FOURTH line
				CurrentUserEntry = 0;
				tokenizer.nextToken();
				tokenizer.nextToken();
				while ((StreamTokenizer.TT_NUMBER==(tokenType=tokenizer.nextToken()))&&(numEntry>CurrentUserEntry))
					{
					PhaseChareTime[m][p][CurrentUserEntry] = (int)tokenizer.nval;
					CurrentUserEntry++;
					}
				
				//Make sure we're at the end of the line
				if (StreamTokenizer.TT_EOL!=tokenType)
					throw new SummaryFormatException("extra garbage at end of line 4");
				
				}
			}
		tokenizer = null;
		file.close();
		System.out.println("Finished reading in data for processor #"+p+"/"+nPe);
		}
	sts.setTotalTime(TotalTime);
	}
	private void checkNextString(String expected) throws IOException,SummaryFormatException
	{
	String ret=nextString(expected);
	if (!expected.equals(ret))
		throw new SummaryFormatException("Expected "+expected+" got "+ret);
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
	public int[][] GetSystemUsageData(
		int intervalStart, int intervalEnd, long OutIntervalSize) 
		throws IOException,SummaryFormatException
	{
	    int intervalRange = intervalEnd - intervalStart + 1;

	int NumProcessors=ProcessorUtilization.length;
	int[][] ret = new int[NumProcessors][intervalRange];
	for (int p = 0; p < NumProcessors; p++)
		{
		int in = 0, out=0; //Indices into ProcessorUtilization[p] and ret[p]
		int usage=0,nUsage=0; //Accumulated processor usage
		int out_t=0; //Accumulated time in output array
		while(out < intervalRange)
			{
			if (in <ProcessorUtilization[p].length)
				usage += ProcessorUtilization[p][in];
			nUsage++;
			in++;
			out_t += IntervalSize;
			if (out_t >= OutIntervalSize)
				{
				ret[p][out++] = (int)(usage/nUsage);
				out_t = 0;
				usage=0;nUsage=0;
				}
			}
		}
	return ret;
	}
	public long GetTotalTime() 
	{
		return TotalTime;
	}
	private double nextNumber(String description) throws IOException,SummaryFormatException
	{
	if (StreamTokenizer.TT_NUMBER!=tokenizer.nextToken()) 
		throw new SummaryFormatException("Couldn't read "+description);
	return tokenizer.nval;
	}
	private double nextScientific(String description) throws IOException,SummaryFormatException
	{
	double mantissa=nextNumber(description+" mantissa");
	String expString=nextString(description+" exponent");
	char expChar=expString.charAt(0);
	if (expChar!='e'&&expChar!='d'&&expChar!='E'&&expChar!='D')
		throw new SummaryFormatException("Couldn't find exponent in "+expString);
	int exponent;
	expString=expString.substring(1);//Clip off leading "e"
	try { exponent=Integer.parseInt(expString);
	} catch (NumberFormatException e) {
		throw new SummaryFormatException("Couldn't parse exponent "+expString);
	}
	return mantissa*Math.pow(10.0,exponent);
	}
	private String nextString(String description) throws IOException,SummaryFormatException
	{
	if (StreamTokenizer.TT_WORD!=tokenizer.nextToken()) 
		throw new SummaryFormatException("Couldn't read string "+description);
	return tokenizer.sval;
	}
}
