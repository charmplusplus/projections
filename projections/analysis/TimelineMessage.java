package projections.analysis;

public class TimelineMessage implements Comparable
{
    public long Time;
    public int Entry;
    public int MsgLen;
    public int EventID;
    public int SenderEventID;
    public int numPEs;
    public int destPEs[];
    
    public TimelineMessage(int senderEventID, long t,int e,int mlen) {
	this(senderEventID, t, e, mlen, 0);
    }
    
    public TimelineMessage(int senderEventID, long t,int e,int mlen,int EventID) {
	this(senderEventID, t, e, mlen, EventID, null);
    }
    
    public TimelineMessage(int senderEventID, long t, int e, int mlen, int EventID, 
			   int destPEs[]) {
   	this.SenderEventID=senderEventID;
	Time=t;
	Entry=e;
	MsgLen=mlen;
	this.EventID = EventID;
	if (destPEs != null) {
	    this.numPEs = destPEs.length;
	} else {
	    this.numPEs = 0;
	}
	this.destPEs = destPEs;
    }

    public TimelineMessage(int senderEventID, long t, int e, int mlen, int EventID, 
			   int numPEs) {
    	this.SenderEventID=senderEventID;
    	Time=t;
	Entry=e;
	MsgLen=mlen;
	this.EventID = EventID;
	this.numPEs = numPEs;
	this.destPEs = null;
    }

	public int getSenderEventID() {
		return SenderEventID;
	}

	public int compareTo(Object o) {
		return (int) (this.Time - ((TimelineMessage)o).Time);
	}
}
