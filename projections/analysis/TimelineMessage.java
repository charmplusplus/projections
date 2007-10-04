package projections.analysis;

public class TimelineMessage
{
    public long Time;
    public int Entry;
    public int MsgLen;
    public int EventID;
    public int numPEs;
    public int destPEs[];
    
    public TimelineMessage(long t,int e,int mlen) {
	this(t, e, mlen, 0);
    }
    
    public TimelineMessage(long t,int e,int mlen,int EventID) {
	this(t, e, mlen, EventID, null);
    }
    
    public TimelineMessage(long t, int e, int mlen, int EventID, 
			   int destPEs[]) {
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

    public TimelineMessage(long t, int e, int mlen, int EventID, 
			   int numPEs) {
	Time=t;
	Entry=e;
	MsgLen=mlen;
	this.EventID = EventID;
	this.numPEs = numPEs;
	this.destPEs = null;
    }
}
