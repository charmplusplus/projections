package projections.analysis;

public class TimelineMessage
{

public long Time;
public int Entry;
public int MsgLen;

public TimelineMessage(long t,int e,int mlen) {
	Time=t;
	Entry=e;
        MsgLen=mlen;
}
}
