package projections.analysis;

import java.lang.*;
import java.io.*;
import projections.misc.*;
import java.util.*;

/** This class contains the core logic behind the analysis of the data and
 *	Prog; exports various functions by which to obtain information about part of 
 *	the data as well as summary statistics.
 */

public class SumAnalyzer extends ProjDefs
{
	private int 	  NumIntervals;
	private String	  FileName;
	private SumLoader SL;
	private Program   Prog;
	private int[][][][] dataArray;
	private StreamTokenizer tokenizer;
	private long[][] ChareTime;   // Holds the total time (in microseconds) spent executing messages
								// directed to each entry method during the entire program run	(3rd line of sum file)
	private int[][] NumEntryMsgs; // Holds the total number of messages sent to each entry method during
								// the entire program run (4th line of the sum file)
	private long[][][] PhaseChareTime;
	private int[][][] PhaseNumEntryMsgs;
	private int[][] ProcessorUtilization; //Holds the second line of summary data
  
	 /** The default constuctor.
	 *	@param ArgV command line given to Projections when it is run
	 */

	public SumAnalyzer (String ArgV) throws IOException
	{

	FileName = ArgV;
	SL		 = new SumLoader(FileName);
	Prog	 = SL.getprogram();
	}

	private double nextNumber(String description) throws IOException,SummaryFormatException
	{
	if (StreamTokenizer.TT_NUMBER!=tokenizer.nextToken()) 
		throw new SummaryFormatException("Couldn't read "+description);
	return tokenizer.nval;
	}

	private String nextString(String description) throws IOException,SummaryFormatException
	{
	if (StreamTokenizer.TT_WORD!=tokenizer.nextToken()) 
		throw new SummaryFormatException("Couldn't read string "+description);
	return tokenizer.sval;
	}

	private void checkNextString(String expected) throws IOException,SummaryFormatException
	{
	String ret=nextString(expected);
	if (!expected.equals(ret))
		throw new SummaryFormatException("Expected "+expected+" got "+ret);
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
	
/********************** State Variables *******************/
	public int myProcessor,nProcessors;//PE <num>/<out of>
	public int entryMethods;//Number of charm++ entry methodss
	public double interval;//Proc. usage sampling interval, in seconds
	public int processorUsage[];//Processor usage over time
	public int entryTimes[];//Total microseconds spent in each entry method
	public int entryCounts[];//Total number of executions of each entry method
	
	public long[] ReadInProcessorUtilization() 
	throws IOException,SummaryFormatException
	{

	//int temp = (int)(TotalTime/(int)SamplingRate);
	//	ProcessorUtilization = new int[NumProcessors][temp];
	int count;
	int temp = 1;
	long Filled;
	double value;
	int tokenType;
	int CurrentUserEntry;
	int nPe=1,numEntry=0,nPhases=0,versionNum=0;
	long intervalSize=0,maxTime=0;
	//ChareTime= new long [NumProcessors][NumUserEntries];			
	//NumEntryMsgs = new int [NumProcessors][NumUserEntries];
	for (int j = 0; j <nPe; j++)
		{

		FileReader file=new FileReader(FileName+"." +j+".sum");
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
		myProcessor=(int)nextNumber("processor number");
		nPe=(int)nextNumber("number of processors");
		checkNextString("count");
		count = (int)nextNumber("count");
		checkNextString("ep");
		numEntry=(int)nextNumber("number of entry methods");
		checkNextString("interval");
		interval=nextScientific("processor usage sample interval"); 
		intervalSize = (long)Math.floor(interval*1000000);
		
		if (versionNum > 2)
			{
			checkNextString("phases");
			nPhases = (int)nextNumber("phases");
			}
		else 
			nPhases = 1;
		
		if ((count*intervalSize) > maxTime)
			maxTime = count*(intervalSize);
		if (StreamTokenizer.TT_EOL!=tokenizer.nextToken())
			throw new SummaryFormatException("extra garbage at end of line 1");
		if (j==0)
		{
			temp = (int)(maxTime/intervalSize);
			ProcessorUtilization = new int[nPe][temp+20];
			ChareTime= new long [nPe][numEntry];
			NumEntryMsgs = new int [nPe][numEntry];
		}
		//Read the SECOND line (processor usage)
		
		int h = 0;
		while ((StreamTokenizer.TT_NUMBER==(tokenType=tokenizer.nextToken()))&&(h<temp))
			{
			ProcessorUtilization[j][h] = (int)tokenizer.nval;
			h++;
			}
		while(h < (temp+20))
			{
			ProcessorUtilization[j][h] = 0;
			h++;
			}

		//Make sure we're at the end of the line
		if (StreamTokenizer.TT_EOL!=tokenType)
			throw new SummaryFormatException("extra garbage at end of line 2");
		   
		// Read in the THIRD line
		CurrentUserEntry = 0;
		while ((StreamTokenizer.TT_NUMBER==(tokenType=tokenizer.nextToken()))&&(numEntry>CurrentUserEntry))
			{
			ChareTime[j][CurrentUserEntry] = (int)tokenizer.nval;
			CurrentUserEntry++;
			}	
				// Make sure we're at the end of the line
		if (StreamTokenizer.TT_EOL!=tokenType)
			throw new SummaryFormatException("extra garbage at end of line 3");
		
		// Read in the FOURTH line
		CurrentUserEntry = 0;
		while ((StreamTokenizer.TT_NUMBER==(tokenType=tokenizer.nextToken()))&&(numEntry>CurrentUserEntry))
			{
			NumEntryMsgs[j][CurrentUserEntry] = (int)tokenizer.nval;
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
		
		if (nPhases > 1)
			{				
			if (j == 0)
				{
				PhaseChareTime= new long [nPhases][nPe][numEntry];
				PhaseNumEntryMsgs = new int [nPhases][nPe][numEntry];
				}
			for(int m=0; m<nPhases; m++)
				{		
				CurrentUserEntry = 0;
				tokenizer.nextToken();
				tokenizer.nextToken();
				while ((StreamTokenizer.TT_NUMBER==(tokenType=tokenizer.nextToken()))&&(numEntry>CurrentUserEntry))
					{
					PhaseNumEntryMsgs[m][j][CurrentUserEntry] = (int)tokenizer.nval;
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
					PhaseChareTime[m][j][CurrentUserEntry] = (int)tokenizer.nval;
					CurrentUserEntry++;
					}
				
				//Make sure we're at the end of the line
				if (StreamTokenizer.TT_EOL!=tokenType)
					throw new SummaryFormatException("extra garbage at end of line 4");
				
				}
			}
		tokenizer = null;
		file.close();
		System.out.println("Finished reading in data for processor #"+j);
		}

	long info[]=new long[6];
	info[0]=nPe;
	info[1]=numEntry;
	info[2]=intervalSize; // in microseconds
	info[3]=maxTime; //in microseconds
	info[4]=nPhases;
	info[5]=versionNum;
	return info;
	}

	
	public int[][] GetSystemUsageData(int NumProcessors, int NumIntervals, long TotalTime, long SamplingRate) throws IOException,SummaryFormatException
	{
	int[][] SystemUsageData;
	int value;
	long Filled;
	int IntervalNum;
		long IntervalSize = (long)TotalTime/NumIntervals;
	int temp = (int)(TotalTime/(int)SamplingRate);
	SystemUsageData = new int[NumProcessors][NumIntervals];
	int j = 0;
	for (int i = 0; i < NumProcessors; i++)
		{
		j = 0;
		SystemUsageData[i][0] = 0;
		value = 0;
		Filled = 0;
		IntervalNum = 0;
		while((IntervalNum < NumIntervals) && (j <temp))
			{
			value = value + ProcessorUtilization[i][j];
			Filled = Filled + SamplingRate;
			if (Filled >= IntervalSize)
				{  
				SystemUsageData[i][IntervalNum] = (int)(value/(Filled /SamplingRate));
				IntervalNum++;
				Filled = 0;
				value = 0;
				}
			j++;
			}
		}
	return SystemUsageData;
	}
	
	public long[][] GetPhaseChareTime(int Phase)
	{
	return PhaseChareTime[Phase];
	}
	
	public int[][] GetPhaseNumEntryMsgs(int Phase)
	{
	return PhaseNumEntryMsgs[Phase];
	}

	public long[][] GetChareTime()
	{
	return ChareTime;
	}
	
	public int[][] GetNumEntryMsgs()
	{
	return NumEntryMsgs;
	}

	/* ************************* Graph functions *************************** */
   
   
	public int numpe()
	{
	return Prog.NumPe;
	}
	
	public int numuserentries()
	{
	return SL.NumUserAttributes;
	}

 /** Gives the user entry points as read in from the .sts file as an array of
	*  two strings:  one for the name of the entry point with BOC or CHARE
	*  prepended to the front and a second containing the name of its parent
	*  BOC or chare.
	*  @return a two-dimensional array of Strings containing these records
	*/
   public String[][] getuserentries()
   {
	  return SL.GetUserAttributes();
   }
}











