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
    public int type;	 // type of the event eg: BEGIN_PROCESSING	
    public int mtype;	 // determines
    public long time;	 // timestamp
    public int entry;	 // EntryPoint number found in sts file
    public int event;	 // Unique sequence number assigned to CREATION Events 
    public int pe;	 // processor number where the event occurred

    // version 2.0 constructs
    public int msglen;	 // only for CREATION events

    public int userEventID;     // for USER_EVENT_PAIR events only
    public long sendTime;	// sendTime 

    // version 4.0 constructs
    public long recvTime;       // the time the processor *actually* received
                                // the message.
    public int id[];            // the thread id (3D array tuple).

    public long cpuStartTime;   // start of cpu timer
    public long cpuEndTime;     // end of cpu timer

    public int numPerfCounts;   // number of performance counters
    public long perfCounts[];   // the array of performance counts

    public LogEntryData() {
	// this is fixed (since it is based on a 3D tuple)
	id = new int[3];
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
	temp.recvTime = recvTime;
	temp.id[0] = id[0];
	temp.id[1] = id[1];
	temp.id[2] = id[2];
	temp.cpuStartTime = cpuStartTime;
	temp.cpuEndTime = cpuEndTime;
	temp.numPerfCounts = numPerfCounts;
	temp.perfCounts = new long[numPerfCounts];
	for (int i=0; i<numPerfCounts; i++) {
	    temp.perfCounts[i] = perfCounts[i];
	}

	return temp;
    }   
}
