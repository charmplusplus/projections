package projections.analysis;

import java.util.*;
import java.lang.Comparable;

public class TimelineEvent implements Comparable
{
 
public long BeginTime;
public long EndTime;
public long RecvTime;
    public long cpuBegin, cpuEnd;
    public int EntryPoint, SrcPe, MsgLen;
    public int EventID; //seq no of processor
    public ObjectId id;
    public Vector MsgsSent;  // of class TimelineMessage
    public Vector PackTimes; // of class PackTime
    public int numPapiCounts = 0;
    public long papiCounts[];

    public boolean isFunction = false;
    public Stack callStack;

    //this indicates the name of the user event which is the most closely
    //associated with this TimelineEvent. This variable is detemined by
    //the beginTime of this user event and the timelineevent. If the beginTIme
    //of the timelineevent is bigger than that of this user event, and the 
    //difference is less than USEREVENTMAXGAP, then this user event is 
    //associated with this timelineevent. The motivation to add this is to 
    //know the Compute ID in NAMD when creating the timeline view. --Chao Mei
    public String userEventName;
    public static final int USEREVENTMAXGAP=3;

    public void setDefaultValues(){
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
    
    
    
public TimelineEvent(long bt,long et, int ep,int pe, int mlen, long r, 
		     ObjectId d, long cpubegin, long cpuend, 
		     int numPapiCounts, long papiCounts[])
{
	setDefaultValues();
	BeginTime=bt; EndTime=et;
	cpuBegin = cpubegin;
	cpuEnd = cpuend;
	EntryPoint=ep; SrcPe=pe; MsgLen=mlen;
        RecvTime = r;
	id = new ObjectId(d);
	this.numPapiCounts = numPapiCounts;
	this.papiCounts = papiCounts;

	userEventName = null;
}

public TimelineEvent(long bt,long et, int ep,int pe, int mlen, long r, 
		     ObjectId d,int eventid, long cpubegin, long cpuend, 
		     int numPapiCounts, long papiCounts[])
{
	setDefaultValues();
	BeginTime=bt; EndTime=et;
	cpuBegin = cpubegin;
	cpuEnd = cpuend;
	EntryPoint=ep; SrcPe=pe; MsgLen=mlen;
        RecvTime = r;

        System.out.println("RecvTime in constructor = "+RecvTime);
	id = new ObjectId(d);
	EventID = eventid;
	this.numPapiCounts = numPapiCounts;
	this.papiCounts = papiCounts;

	userEventName = null;
}


public TimelineEvent(){
	setDefaultValues();
}

public TimelineEvent(long bt,long et, int ep,int pe, int mlen)
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
public void addMessage(TimelineMessage m)
{
	if (MsgsSent==null) MsgsSent=new Vector();
	MsgsSent.addElement(m);
}
public void addPack(PackTime p)
{
	if (PackTimes==null) PackTimes=new Vector();
	PackTimes.addElement(p);
}

public int compareTo(Object o) {
	return (int)(this.BeginTime - ((TimelineEvent)o).BeginTime );
}
}
