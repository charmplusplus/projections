package projections.analysis;

import projections.misc.*;

public class TimelineEvent
{

public long BeginTime, EndTime;
public int EntryPoint, SrcPe;
public MyLinkedList MsgsSent;  // of class TimelineMessage
public MyLinkedList PackTimes; // of class PackTime

public TimelineEvent ()
{
	MsgsSent = PackTimes = null;
}

}

