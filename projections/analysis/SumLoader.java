/** This class reads in the files from the .sts and the various .sum files
 *  so that they can be analyzed by the LogAnalyzer.
 */


package projections.analysis;

import java.io.*;
import java.util.*;
import java.lang.*;
import projections.misc.*;

public class SumLoader extends ProjDefs
{
    public  int     NumBOCs            = 0;
    public  int     NumEntryPoints     = 0;
    public  int     NumCharesCreated   = 0;
    public  int     NumCharesProcessed = 0;
    public  int     NumMsgsCreated     = 0;
    public  int     NumMsgsProcessed   = 0;
    public  int     NumUserAttributes  = 0;
    private int     NumClasses         = 0;
    private int     EntryToAttribute[];
    private String  FileName;
    private String  ClassNames[];
    private String  UserAttributes[][];
    private Program Prog;
    
    public String[][] GetUserAttributes()
    {
	return UserAttributes;
    }
    
    /** Constructor which reads in all of the data associated with the given path
     *  and program name.
     *  @param Name base of filename of logs to read in
     *  @exception IOException if an error occurs while loading the sum that
     *      prevented opening them
     */
    public SumLoader (String Name) throws LogLoadException
    {
	int              I;
	int              Type;
	int              Time;
	int              BeginTime; 
	int              EndTime; 
	int              Len;
	int              Begin;
	long 	       back;
	String           Line;
	RandomAccessFile InFile;
	StringTokenizer  st;
	
	FileName = new String(Name);
	
	readsts();  // read in state files

    }
  
    /** Read in the .sts file and fill in our instance of the Program class.
     *  @exception LogLoadException if an error occurs while reading in the
     *      the state file
     */
    void readsts () throws LogLoadException   
    {
	int            I;
	int            ID;
	int            ChareID;
	int            MsgID;
	int            MsgIndex;
	int            Size;
	int            PseudoIndex;
	int            PseudoType;
	String         Line;
	String         Type;
	String         Name;
	BufferedReader InFile;
	Entry          TempEntry;
	
	try
	    {
		InFile = new BufferedReader(new InputStreamReader(new FileInputStream(FileName + ".sts")));

		Prog = new Program();
		
		while((Line = InFile.readLine()) != null)
		    {
			StringTokenizer st = new StringTokenizer(Line);
			String s1 = st.nextToken();
			if(s1.equals("MACHINE"))
			    Prog.Machine = st.nextToken();
			else if(s1.equals("PROCESSORS"))
			    {
				Prog.NumPe = Integer.parseInt(st.nextToken());
			    }
			else if(s1.equals("TOTAL_CHARES"))
			    {
				Prog.TotalChares = Integer.parseInt(st.nextToken());
				Prog.ChareList   = new Chare[Prog.TotalChares];
				ClassNames       = new String[Prog.TotalChares];
			    }
			else if(s1.equals("TOTAL_EPS"))
			    {
				Prog.TotalEPS    = Integer.parseInt(st.nextToken());
				UserAttributes   = new String[Prog.TotalEPS][2];
				EntryToAttribute = new int[Prog.TotalEPS];
				for (I=0; I<Prog.TotalEPS; I++)
				    EntryToAttribute[I] = -1;
			    }
			else if(s1.equals("TOTAL_MSGS"))
			    {
				Prog.TotalMsgs = Integer.parseInt(st.nextToken());
				Prog.MsgTable  = new long[Prog.TotalMsgs];
			    }
			else if(s1.equals("TOTAL_PSEUDOS"))
			    {
				Prog.TotalPseudos = Integer.parseInt (st.nextToken());
				Prog.PseudoTable  = new Pseudo [Prog.TotalPseudos];
			    }
			else if(s1.equals("TOTAL_EVENTS"))
			    {
				Prog.TotalEvents = Integer.parseInt(st.nextToken());
			    }
			else if(s1.equals("CHARE") || Line.equals("BOC"))
			    {
				ID = Integer.parseInt(st.nextToken());
				Prog.ChareList[ID]            = new Chare();
				Prog.ChareList[ID].ChareID    = ID;
				Prog.ChareList[ID].NumEntries = 0;
				Prog.ChareList[ID].Name       = st.nextToken();
				Prog.ChareList[ID].Type       = new String(s1);
				Prog.ChareList[ID].EntryList  = null;
				Prog.NumChares++;
				ClassNames[NumClasses++]      = Prog.ChareList[ID].Name;
			    }	
			else if(s1.equals("ENTRY"))
			    {
				Type    = st.nextToken();
				ID      = Integer.parseInt(st.nextToken());
				Name    = st.nextToken();
				ChareID = Integer.parseInt(st.nextToken());
				MsgID   = Integer.parseInt(st.nextToken());
				
				TempEntry         = new Entry ();
				TempEntry.Number  = ID;
				TempEntry.ChareID = ChareID;
				TempEntry.Name    = Name; // need to strip some stuff off
				TempEntry.Index   = Prog.NumEntries;
				
				Prog.NumEntries++;
				
				UserAttributes[NumUserAttributes][0] = TempEntry.Name;
				UserAttributes[NumUserAttributes][1] = new String (Type + " " + ClassNames [ChareID]);
				
				EntryToAttribute[ID] = NumUserAttributes++;
			    }
			else if(s1.equals("MESSAGE"))
			    {  
				MsgIndex                = Integer.parseInt(st.nextToken());
				Size                    = Integer.parseInt(st.nextToken());
				Prog.MsgTable[MsgIndex] = Size;
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
			else if (s1.equals ("END"))
			    break;
		    }
		InFile.close();
	    }
	catch (FileNotFoundException E)
	    {
		throw new LogLoadException (FileName + ".sts", LogLoadException.OPEN);
	    }
	catch (IOException E)
	    {
		throw new LogLoadException (FileName + ".sts", LogLoadException.READ);
	    }
    }
    

    /** Method to obtain summary information about the program's execution.
     *  @return an instance of Program
     */
    public Program getprogram ()
    {
	return Prog;
    }

}


