package projections.analysis;

import projections.misc.*;
import java.util.Vector;

public class TimelineEvent
{

public long BeginTime, EndTime;
public int EntryPoint, SrcPe;
public Vector MsgsSent;  // of class TimelineMessage
public Vector PackTimes; // of class PackTime

public TimelineEvent ()
{
}
public TimelineEvent(long bt,long et, int ep,int pe)
{
	BeginTime=bt; EndTime=et;
	EntryPoint=ep; SrcPe=pe;
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