package projections.analysis;

import projections.misc.*;
import java.util.Vector;

public class TimelineEvent
{
 
public long BeginTime, EndTime, RecvTime;
    public long cpuBegin, cpuEnd;
public int EntryPoint, SrcPe, MsgLen;
public int EventID; //seq no of processor
public ObjectId id;
public Vector MsgsSent;  // of class TimelineMessage
public Vector PackTimes; // of class PackTime
    public int numPapiCounts = 0;
    public long papiCounts[];

public TimelineEvent ()
{
}

public TimelineEvent(long bt,long et, int ep,int pe, int mlen, long r, 
		     ObjectId d, long cpubegin, long cpuend, 
		     int numPapiCounts, long papiCounts[])
{
	BeginTime=bt; EndTime=et;
	cpuBegin = cpubegin;
	cpuEnd = cpuend;
	EntryPoint=ep; SrcPe=pe; MsgLen=mlen;
        RecvTime = r;
	id = new ObjectId(d);
	this.numPapiCounts = numPapiCounts;
	this.papiCounts = papiCounts;
}

public TimelineEvent(long bt,long et, int ep,int pe, int mlen, long r, 
		     ObjectId d,int eventid, long cpubegin, long cpuend, 
		     int numPapiCounts, long papiCounts[])
{
	BeginTime=bt; EndTime=et;
	cpuBegin = cpubegin;
	cpuEnd = cpuend;
	EntryPoint=ep; SrcPe=pe; MsgLen=mlen;
        RecvTime = r;
	id = new ObjectId(d);
	EventID = eventid;
	this.numPapiCounts = numPapiCounts;
	this.papiCounts = papiCounts;
}



public TimelineEvent(long bt,long et, int ep,int pe, int mlen)
{
	BeginTime=bt; EndTime=et;
	EntryPoint=ep; SrcPe=pe; MsgLen=mlen;
}
public TimelineEvent(long bt,long et, int ep,int pe)
{
	BeginTime=bt; EndTime=et;
	EntryPoint=ep; SrcPe=pe; MsgLen=0;
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
}
