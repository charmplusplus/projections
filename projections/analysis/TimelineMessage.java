package projections.analysis;

public class TimelineMessage
{

public long Time;
public int Entry;
public int MsgLen;
public int EventID;

public TimelineMessage(long t,int e,int mlen) {
	Time=t;
	Entry=e;
        MsgLen=mlen;
}
public TimelineMessage(long t,int e,int mlen,int EventID) {
	Time=t;
	Entry=e;
        MsgLen=mlen;
	this.EventID = EventID;
}

}
