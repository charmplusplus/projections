package projections.misc;

/**
 *  Written by Chee Wai Lee
 *  4/12/2002
 *
 *  LogEntryData encapsulates data that can potentially be read from a
 *  projections log entry.
 *
 */

public class LogEntryData 
{
    // bad - should use accessors, but what the heck 8)
    public int type;		// type of the event eg: BEGIN_PROCESSING	
    public int mtype;		// determines
    public long time;		// timestamp
    public int entry;		// EntryPoint number found in sts file
    public int event;		// Unique sequence number assigned to CREATION Events 
    public int pe;		// processor number where the event occurred
    public int msglen;		// only for CREATION events

    public int userEventID;     // for USER_EVENT_PAIR events only
    public long sendTime;	// sendTime 

    public LogEntryData() {
    }

  /* return the copy of the current object */ 
    public LogEntryData copyOf(){
	LogEntryData temp = new LogEntryData();
	temp.type   = type;
	temp.mtype  = mtype;
	temp.time   = time;
	temp.entry  = entry;
	temp.event  = event;
	temp.pe     = pe;
	temp.msglen = msglen;
	temp.sendTime = sendTime;
	return temp;
    }   
}
