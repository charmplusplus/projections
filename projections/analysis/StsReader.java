package projections.analysis;

/** This class reads the .sts file and parses out the
 * entry point names and numbers, message names, etc.
 */


import java.io.*;
import java.util.*;
import java.lang.*;
import projections.misc.*;
import java.awt.Color;
import projections.gui.Analysis;

public class StsReader extends ProjDefs
{
    private String baseName, bgSumName;
    private boolean hasSum, hasSumDetail, hasLog, hasBGSum;

    public static final int NUM_TYPES = 4;
    public static final int LOG = 0;
    public static final int SUMMARY = 1;
    public static final int COUNTER = 2;
    public static final int SUMDETAIL = 3;

    private StringBuffer validPEStringBuffers[];
    private int lastPE[];
    private boolean inRange[];
	
    private long totalTime=0;
    private int NumPe;
    private int EntryCount;
    private String EntryNames[][];
    
    //This stuff seems pretty useless to me
    private int TotalChares,TotalMsgs;
    private String Machine, ClassNames[];
    private Chare ChareList[];
    private long MsgTable[];
    private int userEventIndex = 0;
    private Hashtable UserEventIndices = new Hashtable();
    // index by Integer, return String name
    private Hashtable UserEvents = new Hashtable();  
    private Hashtable UserEventColors = new Hashtable();
    private String UserEventNames[];
    
    public int getNumUserDefinedEvents() {
	return UserEvents.size();
    }

    public int getUserEventIndex(int eventID) {
	Integer key = new Integer(eventID);
	return ((Integer)UserEventIndices.get(key)).intValue();
    }

    public Color getUserEventColor(int eventID) {
	Integer key = new Integer(eventID);
	return (Color) UserEventColors.get(key);
    }

    public String getUserEventName(int eventID) { 
	Integer key = new Integer(eventID);
	return (String) UserEvents.get(key);
    }

    public String[] getUserEventNames() {
	// gets an array by logical (not user-given) index
	return UserEventNames;
    }

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
	return validPEStringBuffers[type].toString();
    }

    /**
     *  Implements the state machine for constructing the valid PE string
     *  for the appropriate data type.
     */
    private void addPEToValidString(int type, int pe) {
	// start of the string
	if (lastPE[type] == -1) {
	    validPEStringBuffers[type].append(pe);
	    lastPE[type] = pe;
	} else {
	    if (pe == lastPE[type]+1) {
		// contigious
		if (!inRange[type]) {
		    validPEStringBuffers[type].append('-');
		    inRange[type] = true;
		}
		lastPE[type] = pe;
	    } else if (pe > lastPE[type]+1) {
		// disjoint, start new range.
		if (inRange[type]) {
		    // finish off the last range
		    validPEStringBuffers[type].append(lastPE[type]);
		    inRange[type] = false;
		}
		validPEStringBuffers[type].append(',');
		validPEStringBuffers[type].append(pe);
	    } else {
		// detection loop should be going in forward pe order.
		System.err.println("PE values cannot go backwards! " +
				   "Catastrophic error, exiting.");
		System.exit(-1);
	    }
	}
    }

    private void closeValidStrings() {
	for (int type=0; type<NUM_TYPES; type++) {
	    if (inRange[type]) {
		validPEStringBuffers[type].append(lastPE[type]);
	    }
	}
    }

    /** Read in and decipher the .sts file.
     *  @exception LogLoadException if an error occurs while reading in the
     *      the state file
     *  FileName is the full filename.
     */
    public StsReader(String FileName) throws LogLoadException   
    {
	validPEStringBuffers = new StringBuffer[NUM_TYPES];
	lastPE = new int[NUM_TYPES];
	inRange = new boolean[NUM_TYPES];
	for (int i=0; i<NUM_TYPES; i++) {
	    validPEStringBuffers[i] = new StringBuffer("");
	    lastPE[i] = -1;
	    inRange[i] = false;
	}
	baseName = getBaseName(FileName);

	totalTime=-1;
	try {
	    BufferedReader InFile = 
		new BufferedReader(new InputStreamReader(new FileInputStream(FileName)));
	    int ID,ChareID,MsgID;
	    String Line,Type,Name;
	    while ((Line = InFile.readLine()) != null) {
		StringTokenizer st = new StringTokenizer(Line);
		String s1 = st.nextToken();
		if (s1.equals("VERSION")) {
		    double ver = Double.parseDouble(st.nextToken());
		    Analysis.setVersion(ver);
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
		    if (!UserEvents.containsKey(key)) {
			String eventName = "";
			while (st.hasMoreTokens()) {
			    eventName = eventName + st.nextToken() + " ";
			}
			UserEvents.put(key, eventName);
			UserEventNames[userEventIndex] = eventName;
			UserEventIndices.put(key, 
					     new Integer(userEventIndex++));
		    }
		} else if (s1.equals("TOTAL_EVENTS")) {
		    // restored by Chee Wai - 7/29/2002
		    UserEventNames = 
			new String[Integer.parseInt(st.nextToken())];
		} else if (s1.equals ("END")) {
		    break;
		}
	    }
		
	    // determine if any of the data files exist.
	    // We assume they are automatically valid and this is reflected
	    // in the validPEStringBuffers. In future, whether or not the
	    // values are valid should be defered to after the data file has
	    // been successfullly read.
	    hasLog = false;
	    hasSum = false;
	    hasSumDetail = false;
	    for (int i=0;i<NumPe;i++) {
		if ((new File(getSumName(i))).isFile()) {
		    hasSum = true;
		    addPEToValidString(SUMMARY, i);
		}
		if ((new File(getSumDetailName(i))).isFile()) {
		    hasSumDetail = true;
		    addPEToValidString(SUMDETAIL, i);
		}
		if ((new File(getLogName(i))).isFile()) {
		    hasLog = true;
		    addPEToValidString(LOG, i);
		}
	    }
	    closeValidStrings();
	    bgSumName=getBgSumName();
	    hasBGSum = (bgSumName != null);
	
	    // set up UserEventColors
	    int numColors = UserEvents.size();
	    Color[] userEventColorMap = Analysis.createColorMap(numColors);
	    Enumeration keys = UserEvents.keys();
	    int colorIndex = 0;
	    while (keys.hasMoreElements()) {
		UserEventColors.put((Integer)keys.nextElement(), 
				    userEventColorMap[colorIndex]);
		colorIndex++;
	    }
	    InFile.close();
	} catch (FileNotFoundException e) {
	    throw new LogLoadException (FileName, LogLoadException.OPEN);
	} catch (IOException e) {
	    throw new LogLoadException (FileName, LogLoadException.READ);
	}
    }
    
    public int getEntryCount() { return EntryCount;}   
    
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
    
    public String getFilename() {return baseName;}   
    
    public String getLogName(int pnum) {
	return baseName+"."+pnum+".log";
    }   
    public int getProcessorCount() {
	return NumPe;
    }   
    
    public String getSumName(int pnum) {
	return baseName+"."+pnum+".sum";
    }   
    
    public String getSumDetailName(int pnum) {
	return baseName + "." + pnum + ".sumd";
    }
	
    public long getTotalTime() {return totalTime;}   
    public boolean hasLogFiles() {return hasLog;}   
    public boolean hasSumFiles() {return hasSum;}   
    public boolean hasSumDetailFiles() {return hasSumDetail;}
    public boolean hasBGSumFile() {return hasBGSum;}
    public void setTotalTime(long t) {
	totalTime=t;
    }   
    public String getBGSumName() {return bgSumName;}

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

    private String getBgSumName() {
	if ((new File(baseName + ".sum")).isFile()) {
	    return baseName + ".sum";
	} else {
	    return null;
	}
    }
}
