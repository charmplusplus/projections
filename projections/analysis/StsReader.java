package projections.analysis;

import java.io.*;
import java.util.*;
import java.lang.*;

import projections.gui.*;
import projections.misc.*;

/** 
 *  StsReader
 *  Modified by Chee Wai Lee.
 *  4/7/2003
 *
 *  StsReader provides the necessary abstractions to reason about a
 *  projections .sts file and its associated data files (eg. .log, .sum
 *  .sumd etc).
 * 
 */
public class StsReader extends ProjDefs
{
    private String baseName;
    private String logDirectory;

    private boolean hasSum, hasSumDetail, hasSumAccumulated, hasLog;

    public static final int NUM_TYPES = 4;
    public static final int LOG = 0;
    public static final int SUMMARY = 1;
    public static final int COUNTER = 2;
    public static final int SUMDETAIL = 3;

    private OrderedIntList validPEs[];
    private StringBuffer validPEStringBuffers[];
	
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
    // indexed by (ep id) X (type) where type == 0 -> entry name
    //                               and type == 1 -> class (chare) name
    private String EntryNames[][];  
    
    // User Event information
    private int userEventIndex = 0;
    private Hashtable userEventIndices = new Hashtable();
    // index by Integer, return String name
    private Hashtable userEvents = new Hashtable();  
    private String userEventNames[];
    
    /** 
     *  The StsReader constructor reads the .sts file indicated.
     *  @exception LogLoadException if an error occurs while reading in the
     *      the state file
     *  Pre-condition: FileName is the full pathname of the sts file.
     */
    public StsReader(String FileName) 
	throws LogLoadException   
    {
	validPEs = new OrderedIntList[NUM_TYPES];
	for (int i=0; i<NUM_TYPES; i++) {
	    validPEs[i] = new OrderedIntList();
	}
	baseName = getBaseName(FileName);

	try {
	    BufferedReader InFile = 
		new BufferedReader(new InputStreamReader(new FileInputStream(FileName)));
	    int ID,ChareID,MsgID;
	    String Line,Type,Name;
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
		    EntryNames   = new String[EntryCount][2];
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
		    Type    = st.nextToken();
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
		    MsgID   = Integer.parseInt(st.nextToken());
		    
		    EntryNames[ID][0] = Name;
		    EntryNames[ID][1] = ClassNames [ChareID];
		} else if (s1.equals("MESSAGE")) {
		    ID  = Integer.parseInt(st.nextToken());
		    int Size  = Integer.parseInt(st.nextToken());
		    MsgTable[ID] = Size;
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
		} else if (s1.equals ("END")) {
		    break;
		}
	    }
		
	    // determine if any of the data files exist.
	    // We assume they are automatically valid and this is reflected
	    // in the validPEs. In future, whether or not the
	    // values are valid should be defered to after the data file has
	    // been successfullly read.
	    hasLog = false;
	    hasSum = false;
	    hasSumDetail = false;
	    hasSumAccumulated = false;
	    for (int i=0;i<NumPe;i++) {
		if ((new File(getSumName(i))).isFile()) {
		    hasSum = true;
		    validPEs[SUMMARY].insert(i);
		}
		if ((new File(getSumDetailName(i))).isFile()) {
		    hasSumDetail = true;
		    validPEs[SUMDETAIL].insert(i);
		}
		if ((new File(getLogName(i))).isFile()) {
		    hasLog = true;
		    validPEs[LOG].insert(i);
		}
	    }
	    if ((new File(getSumAccumulatedName())).isFile()) {
		hasSumAccumulated = true;
	    }
	    InFile.close();
	} catch (FileNotFoundException e) {
	    throw new LogLoadException (FileName, LogLoadException.OPEN);
	} catch (IOException e) {
	    throw new LogLoadException (FileName, LogLoadException.READ);
	}
    }

    /** ************** Private working/util Methods ************** */
    
    private String getBaseName(String filename) {
	String baseName = null;
	if (filename.endsWith(".sum.sts")) {
	    baseName = filename.substring(0, filename.length()-8);
	} else if (filename.endsWith(".sts")) {
	    baseName = filename.substring(0, filename.length()-4); 
	} else {
	    System.err.println("Invalid sts filename. Catastrophic " +
			       "error. Exiting.");
	    System.exit(-1);
	}
	return baseName;
    }

    private String dirFromFile(String filename) {
	// pre condition - filename is a full path name
	int index = filename.lastIndexOf(File.separator);
	if (index != -1) {
	    return filename.substring(0,index);
	}
	return(".");	// present directory
    }

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
    
    /** 
     * Gives the user entry points as read in from the .sts file as an 
     * array of two strings:  one for the name of the entry point with 
     * BOC or CHARE prepended to the front and a second containing the 
     * name of its parent BOC or chare.
     * @return a two-dimensional array of Strings containing these records
     */
    public String[][] getEntryNames() {
	return EntryNames;
    }   
    
    // *** user event accessors ***
    public int getNumUserDefinedEvents() {
	return userEvents.size();
    }

    public int getUserEventIndex(int eventID) {
	Integer key = new Integer(eventID);
	return ((Integer)userEventIndices.get(key)).intValue();
    }

    public String getUserEventName(int eventID) { 
	Integer key = new Integer(eventID);
	return (String) userEvents.get(key);
    }

    public String[] getUserEventNames() {
	// gets an array by logical (not user-given) index
	return userEventNames;
    }

    // *** Derived information accessor ***
    public String getValidProcessorString(int type) {
	switch (type) {
	case LOG:
	    if (!hasLog) {
		System.err.println("Warning: No log files.");
	    }
	    break;
	case SUMMARY:
	    if (!hasSum) {
		System.err.println("Warning: No summary files.");
	    }
	    break;
	case SUMDETAIL:
	    if (!hasSumDetail) {
		System.err.println("Warning: No summary detail files.");
	    }
	    break;
	}
	return validPEs[type].listToString();
    }

    public OrderedIntList getValidProcessorList(int type) {
	switch (type) {
	case LOG:
	    if (!hasLog) {
		System.err.println("Warning: No log files.");
	    }
	    break;
	case SUMMARY:
	    if (!hasSum) {
		System.err.println("Warning: No summary files.");
	    }
	    break;
	case SUMDETAIL:
	    if (!hasSumDetail) {
		System.err.println("Warning: No summary detail files.");
	    }
	    break;
	}
	return validPEs[type];
    }

    public boolean hasLogFiles() {
	return hasLog;
    }   

    public boolean hasSumFiles() {
	return hasSum;
    }
   
    public boolean hasSumAccumulatedFile() {
	return hasSumAccumulated;
    }

    public boolean hasSumDetailFiles() {
	return hasSumDetail;
    }

    public String getLogPathname() {
	return logDirectory;
    }

    public String getFilename() { 
	return baseName;
    }   
    
    public String getLogName(int pnum) {
	return baseName+"."+pnum+".log";
    }   

    public String getSumName(int pnum) {
	return baseName+"."+pnum+".sum";
    }   
    
    public String getSumAccumulatedName() {
	return baseName+".sum";
    }

    public String getSumDetailName(int pnum) {
	return baseName + "." + pnum + ".sumd";
    }
}

