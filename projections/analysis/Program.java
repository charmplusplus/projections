package projections.analysis;

import projections.misc.MyLinkedList;

class Program
{
public static final int MaxLabelLength = 1024;

public String Machine, BaseFile, TempFile;
public int NumPe, TotalChares, TotalEPS, TotalMsgs, TotalPseudos, TotalEvents,
	NumChares, NumEntries, NumPseudos;
public long Messages [], NumStages, TimeStep, BeginTime, EndTime;
public String [] Chare, EntryBOC, EntryChare;
public Chare ChareList [];
public long MsgTable [];
public Pseudo PseudoTable [];
public Pseudo PseudoList [];
public MyLinkedList EntryList;
public int EventTable [][][][];
public int SummaryTable [][][];
public int SystemTable [][][][];
public MyLinkedList Timeline;
public UsageInterval Usage [];

public Program ()
{
	NumEntries = NumPseudos = 0;
	BeginTime = Integer.MAX_VALUE;
	EndTime = Integer.MIN_VALUE;
	EntryList = new MyLinkedList ();
}

}
