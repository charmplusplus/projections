package projections.analysis;

/** This class reads the .sts file and parses out the
 * entry point names and numbers, message names, etc.
 */


import java.io.*;
import java.util.*;
import java.lang.*;
import projections.misc.*;

public class StsReader extends ProjDefs
{
	private String baseName, sumBaseName;
	private boolean hasSum, hasLog;
	
	private long totalTime;
	private int NumPe;
	private int EntryCount;
	private String  EntryNames[][];
	
	//This stuff seems pretty useless to me
	private int TotalChares,TotalMsgs;
	private String Machine, ClassNames[];
	private Chare ChareList[];
	private long MsgTable[];
	
  
	/** Read in and decipher the .sts file.
	 *  @exception LogLoadException if an error occurs while reading in the
	 *      the state file
	 *  FileName is the full filename.
	 */
	public StsReader(String FileName) throws LogLoadException   
        {
	        // "I think my basename can be found by stripping .sts"
		baseName = getBaseName(FileName);
	        // "I believe my summary base name should be before .sum.sts"
		sumBaseName = getSumBaseName(FileName);

		// "if user selected a summary sts file ..."
		hasSum=(new File(getSumName(0))).isFile();
		// "No summary?!? Maybe user selected a normal sts file ..."
		if (!hasSum) {
		    sumBaseName = baseName;
		    hasSum = (new File(getSumName(0))).isFile();
		}
		totalTime=-1;
	try
	    {
		BufferedReader InFile = new BufferedReader(
			new InputStreamReader(new FileInputStream(FileName)));
		
		int ID,ChareID,MsgID;
		String Line,Type,Name;
		while((Line = InFile.readLine()) != null)
		    {
			StringTokenizer st = new StringTokenizer(Line);
			String s1 = st.nextToken();
			if(s1.equals("MACHINE"))
			    Machine = st.nextToken();
			else if(s1.equals("PROCESSORS"))
			    {
				NumPe = Integer.parseInt(st.nextToken());
			    }
			else if(s1.equals("TOTAL_CHARES"))
			    {
				TotalChares = Integer.parseInt(st.nextToken());
				ChareList   = new Chare[TotalChares];
				ClassNames  = new String[TotalChares];
			    }
			else if(s1.equals("TOTAL_EPS"))
			    {
				EntryCount   = Integer.parseInt(st.nextToken());
				EntryNames   = new String[EntryCount][2];
			    }
			else if(s1.equals("TOTAL_MSGS"))
			    {
				TotalMsgs = Integer.parseInt(st.nextToken());
				MsgTable  = new long[TotalMsgs];
			    }
			else if(s1.equals("CHARE") || Line.equals("BOC"))
			    {
				ID = Integer.parseInt(st.nextToken());
				ChareList[ID]            = new Chare();
				ChareList[ID].ChareID    = ID;
				ChareList[ID].NumEntries = 0;
				ChareList[ID].Name       = st.nextToken();
				ChareList[ID].Type       = new String(s1);
				ClassNames[ID]      = ChareList[ID].Name;
			    }	
			else if(s1.equals("ENTRY"))
			    {
				Type    = st.nextToken();
				ID      = Integer.parseInt(st.nextToken());
				StringBuffer nameBuf=new StringBuffer(st.nextToken());
				Name = nameBuf.toString();
				if (-1!=Name.indexOf('(') && -1==Name.indexOf(')'))
				  //Parse strings until we find the close-paren
				  while (true) {
					String tmp=st.nextToken();
					nameBuf.append(" ");
					nameBuf.append(tmp);
					if (tmp.endsWith(")"))
						break;
				  }
				Name    = nameBuf.toString();
				ChareID = Integer.parseInt(st.nextToken());
				MsgID   = Integer.parseInt(st.nextToken());
				
				EntryNames[ID][0] = Name;
				EntryNames[ID][1] = ClassNames [ChareID];
			    }
			else if(s1.equals("MESSAGE"))
			    {
				ID  = Integer.parseInt(st.nextToken());
				int Size  = Integer.parseInt(st.nextToken());
				MsgTable[ID] = Size;
			    }
	/* No longer used-- OSL, 2/8/2001
			else if(s1.equals("TOTAL_PSEUDOS"))
			{
			   Prog.TotalPseudos = Integer.parseInt (st.nextToken());
			   Prog.PseudoTable  = new Pseudo [Prog.TotalPseudos];
			}
			else if(s1.equals("TOTAL_EVENTS"))
			{
			   Prog.TotalEvents = Integer.parseInt(st.nextToken());
			}
			else if(s1.equals("PSEUDO"))
			{
			   PseudoIndex = Integer.parseInt(st.nextToken());
			   PseudoType  = Integer.parseInt(st.nextToken());
			   Name        = st.nextToken();
			   Prog.PseudoTable[Prog.NumPseudos]       = new Pseudo ();
			   Prog.PseudoTable[Prog.NumPseudos].Index = PseudoIndex;
			   Prog.PseudoTable[Prog.NumPseudos].Type  = PseudoType;
			   Prog.PseudoTable[Prog.NumPseudos].Name  = Name;
			   Prog.NumPseudos++;
			}
	*/
			else if (s1.equals ("END"))
			    break;
		    }


		// "if user selected a summary sts file, log files are
		// always ignored"
		// test to see if at least one log file exists
		hasLog = false;
		for (int i=0;i<NumPe;i++) {
		    if ((new File(getLogName(i))).isFile()) {
			hasLog = true;
		    }
		}

		InFile.close();
	    }
	catch (FileNotFoundException E)
	    {
		throw new LogLoadException (FileName, LogLoadException.OPEN);
	    }
	catch (IOException E)
	    {
		throw new LogLoadException (FileName, LogLoadException.READ);
	    }
	}
   public int getEntryCount() { return EntryCount;}   
   /** Gives the user entry points as read in from the .sts file as an array of
	*  two strings:  one for the name of the entry point with BOC or CHARE
	*  prepended to the front and a second containing the name of its parent
	*  BOC or chare.
	*  @return a two-dimensional array of Strings containing these records
	*/
   public String[][] getEntryNames()
   {
	  return EntryNames;
   }   
   public String getFilename() {return baseName;}   
   public String getLogName(int pnum) {return baseName+"."+pnum+".log";}   
   public int getProcessorCount() {return NumPe;}   
   public String getSumName(int pnum) {return sumBaseName+"."+pnum+".sum";}   
   public long getTotalTime() {return totalTime;}   
   public boolean hasLogFiles() {return hasLog;}   
   public boolean hasSumFiles() {return hasSum;}   
   public void setTotalTime(long t) {totalTime=t;}   


    // assume .sts
    private String getBaseName(String filename) {
	return filename.substring(0, filename.length()-4); 
    }

    // assume .sum.sts
    private String getSumBaseName(String filename) {
	return filename.substring(0, filename.length()-8); 
    }
}
