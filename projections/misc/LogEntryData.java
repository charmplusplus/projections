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
    private boolean isValid = true;

    public int type;	 // type of the event eg: BEGIN_PROCESSING	
    public int mtype;	 // determines
    public long time;	 // timestamp
    public int entry;	 // EntryPoint number found in sts file
    public int event;	 // Unique sequence number assigned to CREATION Events 
    public int pe;	 // processor number where the event occurred
    public int numPEs;   // Number of processors a message was sent to.
                         // Used for CREATION_BCAST and CREATION_MULTICAST

    // version 2.0 constructs
    public int msglen;	 // only for CREATION events

    public int userEventID;     // for USER_EVENT_PAIR events only
    public long sendTime;	// sendTime 

    // version 4.0 constructs
    public long recvTime;       // the time the processor *actually* received
                                // the message.
    public int id[];            // the thread id (3D array tuple).
                                // as of ver 7.0, it is a 4-tuple

    public long cpuStartTime;   // start of cpu timer
    public long cpuEndTime;     // end of cpu timer

    public int numPerfCounts;   // number of performance counters
    public long perfCounts[];   // the array of performance counts

    public int destPEs[];       ///< list of multicast destination processors

    public Integer userSupplied;
    
    public Integer memoryUsage;
    
    
    public LogEntryData() {
	// this is fixed (since it is based on a 3D tuple)
        // As of version 7.0, it is a 4-tuple which includes array ID.
	id = new int[4];
    }

    // 9/14/2004 - added AMPI Function tracing support
    // "entry" in the case of functions will be the function ID.
    public int lineNo;          // line number of the function call.
    public String funcName;     // the name of the function
    
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
	temp.recvTime = recvTime;
	temp.id[0] = id[0];
	temp.id[1] = id[1];
	temp.id[2] = id[2];
	temp.id[3] = id[3];
	temp.cpuStartTime = cpuStartTime;
	temp.cpuEndTime = cpuEndTime;
	temp.numPerfCounts = numPerfCounts;
	temp.perfCounts = new long[numPerfCounts];
	for (int i=0; i<numPerfCounts; i++) {
	    temp.perfCounts[i] = perfCounts[i];
	}
	temp.lineNo = lineNo;
	temp.funcName = new String(funcName);
	temp.userSupplied = userSupplied;
	temp.memoryUsage = memoryUsage;
	
	return temp;
    }

    public boolean isValid() {
	return isValid;
    }

    public void setValid(boolean flag) {
	isValid = flag;
    }
}
