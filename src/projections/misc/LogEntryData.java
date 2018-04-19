package projections.misc;

import projections.analysis.ProjDefs;
import projections.gui.MainWindow;

/**
 *  Written by Chee Wai Lee
 *  4/12/2002
 *
 *  LogEntryData encapsulates data that can potentially be read from a
 *  projections log entry.
 *
 */

public class LogEntryData extends ProjDefs
{
	
	static private int myRun = 0;
	
    private boolean isValid = true;

    /** type of the event eg: BEGIN_PROCESSING	 */
    public int type;	 	
    
    /** determines	 */
    public int mtype;	 
   
    /** timestamp */
    public long time;	 
	
    /** used for bracketed user supplied notes, and all bracketed events in the future */
    public long endTime; 
    
    /** EntryPoint number found in sts file */
    public int entry;	 
    
    /** Unique sequence number assigned to Events. This is a unique sequence number set by the sender for BEGIN_PROCESSING */
    public int event;	 
    
    /** processor number where the event occurred */
    public int pe;

    /** Number of processors a message was sent to. Used for CREATION_BCAST and CREATION_MULTICAST */
    public int numPEs;   


    // version 2.0 constructs
    public int msglen;	 // only for CREATION events

    public int userEventID;     // for USER_EVENT_PAIR events only
    public long sendTime;	// sendTime 

    // version 4.0 constructs
    public long recvTime;       // the time the processor *actually* received
                                // the message.
    public int id[];            // the thread id (3D array tuple).
                                // as of ver 9.0, it is a 6-tuple

    public long cpuStartTime;   // start of cpu timer
    public long cpuEndTime;     // end of cpu timer

    public int numPerfCounts;   // number of performance counters
    public long perfCounts[];   // the array of performance counts

    public int destPEs[];       ///< list of multicast destination processors

    public Integer userSupplied;
    
    public long memoryUsage;
    
    /// An arbitrary string provided by the user. Should be displayed as a user event
	public String note;
 
	public int nestedID; // Nested thread ID, e.g. virtual AMPI ranks
    
    public LogEntryData() {
	// this is fixed (since it is based on a 3D tuple)
        // As of version 9.0, it is a 6-tuple which includes array ID.
	id = new int[6];
    }

    // 9/14/2004 - added AMPI Function tracing support
    // "entry" in the case of functions will be the function ID.
    public int lineNo;          // line number of the function call.
    public String funcName;     // the name of the function

    // 6/7/16 - added User Stats support
    public double stat;
    public double userTime;

    public boolean isValid() {
	return isValid;
    }

    public void setValid(boolean flag) {
	isValid = flag;
    }
    
    
    
    public String htmlFormattedDescription(){
    	
    	switch( type ) {
		case ( ProjDefs.CREATION ):
			return ( "<font size=+1 color=\"#660000\">CREATE</font> message to be sent to <em> " + MainWindow.runObject[myRun].getEntryFullNameByID(entry) + "</em>");
		case ( ProjDefs.CREATION_BCAST ):
			if (numPEs == MainWindow.runObject[myRun].getNumProcessors()) {
				return ( "<font size=+1 color=\"#666600\">GROUP BROADCAST</font> (" + numPEs + " processors)");
			} else {
				return ( "<font size=+1 color=\"#666600\">NODEGROUP BROADCAST</font> (" + numPEs + " processors)");
			}
		case ( ProjDefs.CREATION_MULTICAST ):
			return ( "<font size=+1 color=\"#666600\">MULTICAST</font> message sent to " + numPEs + " processors");
		case ( ProjDefs.BEGIN_PROCESSING ):
			return ( "<font size=+1 color=\"#000088\">BEGIN PROCESSING</font> of <em>" + MainWindow.runObject[myRun].getEntryFullNameByID(entry)  + "</em> from processor " + pe + " event=" + event);
		case ( ProjDefs.END_PROCESSING ):
			return ( "<font size=+1 color=\"#000088\">END PROCESSING</font> of message sent to <em>" + MainWindow.runObject[myRun].getEntryFullNameByID(entry)  + "</em> from processor " + pe);
		case ( ProjDefs.ENQUEUE ):
			return( "<font size=+1>ENQUEUEING</font> message received from " + "processor " + pe + " destined for " + MainWindow.runObject[myRun].getEntryFullNameByID(entry) );
		case ( ProjDefs.BEGIN_IDLE ):
			return( "<font size=+1 color=\"#333333\">IDLE begin</font>");
		case ( ProjDefs.END_IDLE ):
			return ( "<font size=+1 color=\"#333333\">IDLE end</font>");
		case ( ProjDefs.BEGIN_PACK ):
			return ( "<font size=+1 color=\"#008800\">BEGIN PACKING</font> a message to be sent");
		case ( ProjDefs.END_PACK ):
			return ( "<font size=+1 color=\"#008800\">FINISHED PACKING</font> a message to be sent");
		case ( ProjDefs.BEGIN_UNPACK ):
			return ( "<font size=+1 color=\"#880000\">BEGIN UNPACKING</font> a received message");
		case ( ProjDefs.END_UNPACK ):
			return ( "<font size=+1 color=\"#880000\">FINISHED UNPACKING</font> a received message");
		case (ProjDefs.BEGIN_COMPUTATION):
			return ( "<font size=+1 color=\"#888888\">BEGIN COMPUTATION</font>");
		case (ProjDefs.END_COMPUTATION):
			return ( "<font size=+1 color=\"#888888\">END COMPUTATION</font>");
		case (ProjDefs.USER_EVENT_PAIR):
			String name =  MainWindow.runObject[myRun].getUserEventName(userEventID);
			return ( "<font color=\"#F7D331\">Bracketed User Event (comes in pairs)</font>: " + name);
		case (ProjDefs.USER_STAT):
			String statName = MainWindow.runObject[myRun].getUserStatName(userEventID);
			String formattedString = "<font size=+1 color=\"#F7D331\">User Stat</font>: " + statName + " updated to " + stat;
			if(userTime != -1)
				formattedString = formattedString + " at user defined time " + userTime; 		//Prints User time if specified.
			return formattedString;
		case (ProjDefs.USER_EVENT):
			String name2 =  MainWindow.runObject[myRun].getUserEventName(userEventID);
			return ( "<font color=\"#F7D331\">User Event</font>: " + name2);
		case ( ProjDefs.USER_SUPPLIED_NOTE):
			if(note != null)
				return ( "<font size=+1 color=\"#880000\">USER SUPPLIED NOTE:</font> " + note);
			else
				return ( "<font size=+1 color=\"#880000\">USER SUPPLIED NOTE:</font> <i>blank</i>" );		
		default:
			System.out.println("Unknown event type");
		return ( "Unknown Event Type:" + type + " !!!");
		}
	}
    	
        
	public boolean isBeginType() {
		return ((type == BEGIN_IDLE) ||
				(type == BEGIN_PACK) ||
				(type == BEGIN_UNPACK) ||
				(type == BEGIN_PROCESSING) ||
				(type == BEGIN_TRACE) ||
				(type == BEGIN_FUNC) ||
				(type == BEGIN_INTERRUPT));
	}

	public boolean isEndType() {
		return ((type == END_IDLE) ||
				(type == END_PACK) ||
				(type == END_UNPACK) ||
				(type == END_PROCESSING) ||
				(type == END_TRACE) ||
				(type == END_FUNC) ||
				(type == END_INTERRUPT));
	}
    
}
