package projections.analysis;

import java.util.ArrayList;
import java.util.Stack;

import projections.Tools.Timeline.TimelineMessage;
import projections.misc.MiscUtil;

/** A class that represents an event from the log, Eventually an EntryMethod object or UserEventObject will be created from this data */
public class TimelineEvent implements Comparable
{
public long BeginTime;
public long EndTime;
public long RecvTime;
    public long cpuBegin, cpuEnd;
    public int EntryPoint, SrcPe, MsgLen;
    public int EventID; //seq no of processor
    public ObjectId id;
    public ArrayList<TimelineMessage> MsgsSent; 
    public ArrayList<PackTime> PackTimes;
    public int numPapiCounts = 0;
    public long papiCounts[];
    public Integer UserSpecifiedData;
    public long memoryUsage;
    
    public Stack callStack;

    //this indicates the name of the user event which is the most closely
    //associated with this TimelineEvent. This variable is determined by
    //the beginTime of this user event and the timelineevent. If the beginTIme
    //of the timelineevent is bigger than that of this user event, and the 
    //difference is less than USEREVENTMAXGAP, then this user event is 
    //associated with this timelineevent. The motivation to add this is to 
    //know the Compute ID in NAMD when creating the timeline view. --Chao Mei
    public String userEventName;
    protected static final int USEREVENTMAXGAP=3;

    private void setDefaultValues(){
    	BeginTime = -1;
    	EndTime = -1;
    	RecvTime = -1;
    	cpuBegin = -1;
    	cpuEnd = -1;
    	EntryPoint = -1;
    	SrcPe = -1;
    	MsgLen = -1;
    	EventID = -1;
    	id = null;
    	MsgsSent = null;
    	PackTimes = null;
    	numPapiCounts = 0;
        papiCounts = null;
    }
    
    


    protected TimelineEvent(long bt,long et, int ep,int pe, int mlen, long r, 
		     ObjectId d,int eventid, long cpubegin, long cpuend, 
		     int numPapiCounts, long papiCounts[])
{
	setDefaultValues();
	BeginTime=bt; EndTime=et;
	cpuBegin = cpubegin;
	cpuEnd = cpuend;
	EntryPoint=ep; SrcPe=pe; MsgLen=mlen;
        RecvTime = r;

	id = new ObjectId(d);
	EventID = eventid;
	this.numPapiCounts = numPapiCounts;
	this.papiCounts = papiCounts;

	userEventName = null;
}

	protected TimelineEvent(long bt,long et, int ep,int pe, int mlen, long r,
							int[] d,int eventid, long cpubegin, long cpuend,
							int numPapiCounts, long papiCounts[])
	{
		setDefaultValues();
		BeginTime=bt; EndTime=et;
		cpuBegin = cpubegin;
		cpuEnd = cpuend;
		EntryPoint=ep; SrcPe=pe; MsgLen=mlen;
		RecvTime = r;

		id = new ObjectId(d);
		EventID = eventid;
		this.numPapiCounts = numPapiCounts;
		this.papiCounts = papiCounts;

		userEventName = null;
	}


public TimelineEvent(){
	setDefaultValues();
}

protected TimelineEvent(long bt,long et, int ep,int pe, int mlen)
{
	setDefaultValues();
	BeginTime=bt; EndTime=et;
	EntryPoint=ep; SrcPe=pe; MsgLen=mlen;

	userEventName = null;
}
public TimelineEvent(long bt,long et, int ep,int pe)
{
	setDefaultValues();
	BeginTime=bt; EndTime=et;
	EntryPoint=ep; SrcPe=pe; MsgLen=0;

	userEventName = null;
}
protected void addMessage(TimelineMessage m)
{
	if (MsgsSent==null) MsgsSent=new ArrayList();
	MsgsSent.add(m);
}
protected void addPack(PackTime p)
{
	if (PackTimes==null) PackTimes=new ArrayList();
	PackTimes.add(p);
}

protected void compactLists()
{
	if (MsgsSent != null) MsgsSent.trimToSize();
	if (PackTimes != null) PackTimes.trimToSize();
}

@SuppressWarnings("ucd")
public int compareTo(Object o) {
	return MiscUtil.sign(this.BeginTime - ((TimelineEvent)o).BeginTime);
}
}
