package projections.analysis;

public class TimelineMessage
{
    public long Time;
    public int Entry;
    public int MsgLen;
    public int EventID;
    public int destPEs[];
    
    public TimelineMessage(long t,int e,int mlen) {
	this(t, e, mlen, 0);
    }
    
    public TimelineMessage(long t,int e,int mlen,int EventID) {
	this(t, e, mlen, EventID, null);
    }
    
    public TimelineMessage(long t, int e, int mlen, int EventID, int destPEs[]) {
	Time=t;
	Entry=e;
	MsgLen=mlen;
	this.EventID = EventID;
	this.destPEs = destPEs;
    }
}
