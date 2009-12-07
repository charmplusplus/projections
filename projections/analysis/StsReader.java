package projections.analysis;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import projections.misc.LogLoadException;

/** 
 *  StsReader
 *  Modified by Chee Wai Lee.
 *  4/7/2003
 *  10/26/2005 - moved the functionality for various pre-processing 
 *               analysis of data files to Analysis.java
 *
 *  StsReader provides the necessary abstractions to reason about a
 *  projections .sts file and its associated data files (eg. .log, .sum
 *  .sumd etc).
 * 
 */
public class StsReader extends ProjDefs
{
    private boolean hasPAPI = false;
	
    // Sts header information
    private double version;
    private String Machine;
    private int NumPe;
    private int TotalChares;
    private int EntryCount;
    private int TotalMsgs;

    // Sts data
    private String ClassNames[];    // indexed by chare id
    private Chare ChareList[];
    private long MsgTable[];        // indexed by msg id
 
    
    /** Entry Names */
    private int entryIndex = 0; ///< The next available index
    
    /** index by Integer ID in STS file, return String name */
    private Hashtable<Integer, String> entryNames = new Hashtable<Integer, String>(); 
    /** index by Integer ID in STS file, return String name */
    private Hashtable<Integer, String>  entryChareNames = new Hashtable<Integer, String>(); 
    /** keys are indexes into flat arrays, values are the IDs given in STS file */
    private Hashtable<Integer, Integer>  entryFlatToID = new Hashtable<Integer, Integer>();
    /** keys are the IDs given in STS file, values are indexes into flat arrays */
    private Hashtable<Integer, Integer> entryIDToFlat = new Hashtable<Integer, Integer>();
    
    
    
    /// A mapping from the sparse user supplied ids for the user events to a compact set of integers used for coloring the user events
    private Hashtable<Integer, Integer> userEventIndices = new Hashtable<Integer, Integer>();
    /// Used to make values in userEventIndices unique
    private int userEventIndex = 0;

    /// The user event names index by Integer
    private TreeMap<Integer, String> userEvents = new TreeMap<Integer, String>();  
    /// The same user event names as in userEvents, but packed into an array in same manner
    private String userEventNames[];

    
    
    // AMPI Functions tracing
    private int functionEventIndex = 0;
    private Hashtable<Integer, Integer> functionEventIndices = new Hashtable<Integer, Integer>();
    private Hashtable<Integer, String> functionEvents = new Hashtable<Integer, String>();
    private String functionEventNames[];
    
    private int numPapiEvents;
    private String papiEventNames[];

    /**
     *  Basically a hack to allow multirun tool to bypass the 
     *  ActivityManager and it's use of Analysis.java. Hence this
     *  wrapper is used for normal tools.
     */
    public StsReader(String FileName) 
	throws LogLoadException
    {
	this(FileName, false);
    }

    /** 
     *  The StsReader constructor reads the .sts file indicated.
     *  @exception LogLoadException if an error occurs while reading in the
     *      the state file
     *  Pre-condition: FileName is the full pathname of the sts file.
     */
    public StsReader(String FileName, boolean isMultirun) 
	throws LogLoadException   
    {
	try {
	    BufferedReader InFile = 
		new BufferedReader(new InputStreamReader(new FileInputStream(FileName)));
	    int ID,ChareID;
	    String Line,Name;
	    while ((Line = InFile.readLine()) != null) {
		StringTokenizer st = new StringTokenizer(Line);
		String s1 = st.nextToken();
		if (s1.equals("VERSION")) {
		    version = Double.parseDouble(st.nextToken());
		} else if (s1.equals("MACHINE")) {
		    Machine = st.nextToken();
		} else if (s1.equals("PROCESSORS")) {
		    NumPe = Integer.parseInt(st.nextToken());
		} else if (s1.equals("TOTAL_CHARES")) {
		    TotalChares = Integer.parseInt(st.nextToken());
		    ChareList   = new Chare[TotalChares];
		    ClassNames  = new String[TotalChares];
		} else if (s1.equals("TOTAL_EPS")) {
		    EntryCount   = Integer.parseInt(st.nextToken());
		} else if (s1.equals("TOTAL_MSGS")) {
		    TotalMsgs = Integer.parseInt(st.nextToken());
		    MsgTable  = new long[TotalMsgs];
		} else if (s1.equals("CHARE") || Line.equals("BOC")) {
		    ID = Integer.parseInt(st.nextToken());
		    ChareList[ID]            = new Chare();
		    ChareList[ID].ChareID    = ID;
		    ChareList[ID].NumEntries = 0;
		    ChareList[ID].Name       = st.nextToken();
		    ChareList[ID].Type       = new String(s1);
		    ClassNames[ID]      = ChareList[ID].Name;
		} else if (s1.equals("ENTRY")) {
			st.nextToken(); // type
			ID      = Integer.parseInt(st.nextToken());
			StringBuffer nameBuf=new StringBuffer(st.nextToken());
			Name = nameBuf.toString();
			if (-1!=Name.indexOf('(') && -1==Name.indexOf(')')) {
				//Parse strings until we find the close-paren
				while (true) {
					String tmp=st.nextToken();
					nameBuf.append(" ");
					nameBuf.append(tmp);
					if (tmp.endsWith(")"))
						break;
				}
			}
			Name    = nameBuf.toString();
			ChareID = Integer.parseInt(st.nextToken());
			st.nextToken(); // msgid

			entryFlatToID.put(entryIndex, ID);
			entryIDToFlat.put(ID,entryIndex);
			entryIndex++;
			getEntryNames().put(ID,Name);
			getEntryChareNames().put(ID,ClassNames [ChareID]);
		} else if (s1.equals("MESSAGE")) {
		    ID  = Integer.parseInt(st.nextToken());
		    int Size  = Integer.parseInt(st.nextToken());
		    MsgTable[ID] = Size;
		} else if (s1.equals("FUNCTION")) {
		    Integer key = new Integer(st.nextToken());
		    if (!functionEvents.containsKey(key)) {
			// Allow the presence of spaces in the descriptor.
			String functionEventName = "";
			while (st.hasMoreTokens()) {
			    functionEventName += st.nextToken() + " ";
			}
			functionEvents.put(key, functionEventName);
			functionEventNames[functionEventIndex] = 
			    functionEventName;
			functionEventIndices.put(key,
						 new Integer(functionEventIndex));
		    }
		    functionEventIndex++;
		} else if (s1.equals("EVENT")) {
		    Integer key = new Integer(st.nextToken());
		    if (!userEvents.containsKey(key)) {
			String eventName = "";
			while (st.hasMoreTokens()) {
			    eventName = eventName + st.nextToken() + " ";
			}
			userEvents.put(key, eventName);
			userEventNames[userEventIndex] = eventName;
			userEventIndices.put(key, 
					     new Integer(userEventIndex++));
		    }
		} else if (s1.equals("TOTAL_EVENTS")) {
		    // restored by Chee Wai - 7/29/2002
		    userEventNames = 
			new String[Integer.parseInt(st.nextToken())];
		} else if (s1.equals("TOTAL_FUNCTIONS")) {
		    functionEventNames = 
			new String[Integer.parseInt(st.nextToken())];
		} else if (s1.equals ("TOTAL_PAPI_EVENTS")) {
		    hasPAPI = true;
		    numPapiEvents = Integer.parseInt(st.nextToken());
		    papiEventNames =
			new String[numPapiEvents];
		} else if (s1.equals ("PAPI_EVENT")) {
		    hasPAPI = true;
		    papiEventNames[Integer.parseInt(st.nextToken())] =
			st.nextToken();
		} else if (s1.equals ("END")) {
		    break;
		}
	    }
		
	    InFile.close();
	} catch (FileNotFoundException e) {
	    throw new LogLoadException (FileName, LogLoadException.OPEN);
	} catch (IOException e) {
	    throw new LogLoadException (FileName, LogLoadException.READ);
	}
    }

    /** ************** Private working/util Methods ************** */
    
    /** ****************** Accessor Methods ******************* */

    // *** Data accessors ***
    public double getVersion() {
	return version;
    }

    public int getEntryCount() { 
    	return EntryCount;
    }   
    
    public int getProcessorCount() {
	return NumPe;
    }   

    public String getMachineName() {
	return Machine;
    }
    

    
    public String getEntryNameByID(int ID) {
    	return getEntryNames().get(ID);
    }   
    
    public String getEntryNameByIndex(int index) {
    	if(entryFlatToID.containsKey(index)){
    		return getEntryNames().get(entryFlatToID.get(index));
    	} else {
    		return "Unknown";
    	}
    }   
    
    public String getEntryChareNameByID(int ID) {
    	return getEntryChareNames().get(ID);
    }   
    
    public String getEntryChareNameByIndex(int index) {
    	return getEntryChareNames().get(entryFlatToID.get(index));
    }   

    public String getEntryFullNameByID(int ID) {
    	return  getEntryChareNameByID(ID) + "::" + getEntryNameByID(ID);
    }   
    
    public String getEntryFullNameByIndex(int index) {
    	return getEntryChareNameByIndex(index) + "::" + getEntryNameByIndex(index);
    }   
    
	public Integer getEntryIndex(Integer ID) {
		if(ID<0)
    		return ID;
		return entryIDToFlat.get(ID);
	}
       
    
    // *** user event accessors ***
    public int getNumUserDefinedEvents() {
	return userEvents.size();
    }

    public Integer getUserEventIndex(int eventID) {
	Integer key = new Integer(eventID);
	if(userEventIndices.containsKey(key))
		return (userEventIndices.get(key));
	else
		return null;
    }

    public String getUserEventName(int eventID) { 
	Integer key = new Integer(eventID);
	return userEvents.get(key);
    }

    public String[] getUserEventNames() {
	// gets an array by logical (not user-given) index
	return userEventNames;
    }
    
    public Map<Integer, String>  getUserEventNameMap() {
    	// gets an array by logical (not user-given) index
    	return userEvents;
    }
    

    // *** function event accessors ***
    public int getNumFunctionEvents() {
	// **FIXME**
	// ** create a new function called getFunctionArraySize **
	// ** getNumFunctionEvents does not semantically mean the same thing!
	//
	// the +1 is a silly hack because function id counts does not begin
	// from 0. No easy solution is visible in the near-future.
	if (functionEvents.size() == 0) {
	    return 0;
	} else {
	    return functionEvents.size()+1;
	}
    }

    public int getFunctionEventIndex(int eventID) {
	Integer key = new Integer(eventID);
	return (functionEventIndices.get(key)).intValue();
    }

    public String getFunctionEventDescriptor(int eventID) {
	Integer key = new Integer(eventID);
	return functionEvents.get(key);
    }

    public String[] getFunctionEventDescriptors() {
	return functionEventNames;
    }

    public int getNumPerfCounts() {
	if (hasPAPI) {
	    return numPapiEvents;
	} else {
	    return 0;
	}
    }

    public String[] getPerfCountNames() {
	if (hasPAPI) {
	    return papiEventNames;
	} else {
	    return null;
	}
    }

	public Map<Integer, String>  getEntryNames() {
		return entryNames;
	}


	public Map<Integer, String>  getEntryChareNames() {
		return entryChareNames;
	}


}

