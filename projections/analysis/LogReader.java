package projections.analysis;

import java.io.*;
import javax.swing.*;

import projections.misc.*;
import projections.gui.*;

public class LogReader 
    extends ProjDefs
{
    //sysUsgData[SYS_Q] is length of message queue
    public static final int SYS_Q=0; 
    //sysUsgData[SYS_CPU] is percent processing time
    public static final int SYS_CPU=1; 
    //sysUsgData[SYS_IDLE] is percent idle time
    public static final int SYS_IDLE=2; 
    //Magic entry method (indicates "idle")
    public static final int IDLE_ENTRY=-1;
    //Number of creations bin 
    public static final int CREATE=0; 
    //Number of invocations bin
    public static final int PROCESS=1; 
    //Time (us) spent processing bin
    public static final int TIME=2; 

    private int[][][] sysUsgData;
    private int[][][][] userEntries;
    private int[][][][] categorized;
    private int numProcessors;
    private int numUserEntries; //Number of user entry points
    private long startTime; //Time the current entry method started
    private int currentEntry; //Currently executing entry method
    private int currentMtype; //Current message type
    private int interval;//Current interval number
    private int curPeIdx;//Current source processor #
    private int numIntervals;//Total number of intervals
    private long intervalSize;//Length of an interval (us)
    private int intervalStart;// start of range of interesting intervals
    private int intervalEnd; // end of range of interesting intervals

    private int processing;
    private boolean byEntryPoint;

    // **CW** 8/23/2005 Make LogReader use GenericLogReader instead
    private LogEntryData curData;
    private GenericLogReader reader;

    public long getIntervalSize() {
	return intervalSize;
    }
	
    public LogReader() {
	curData = new LogEntryData();
	// The GenericLogReaders for each pe should be created only
	// at read time.
    }

    /**
       Add the given amount of time to the current entry for interval j
    */
    final private void addToInterval(int extra,int j,boolean maxPercents)
    {
	if (processing<=0) {
	    return; //Not processing at all
	}
	if (j < intervalStart || j > intervalEnd) {  // Not within interest
	    return;
	}
	if (currentEntry == IDLE_ENTRY) { //Idle time
	    sysUsgData[SYS_IDLE][curPeIdx][j-intervalStart] += 
		maxPercents?100:extra;
	} else { //Processing an entry method
	    sysUsgData[SYS_CPU][curPeIdx][j-intervalStart] += 
		maxPercents?100:extra;
	    if (byEntryPoint) {
		userEntries[currentEntry][TIME][curPeIdx][j-intervalStart] += 
		    extra; 
		int catIdx = mtypeToCategoryIdx(currentMtype); 
		if (catIdx!=-1) {
		    categorized[catIdx][TIME][curPeIdx][j-intervalStart] += 
			extra;
		}
	    }
	}
	startTime+=extra;
    }

    /**
       Add this entry point to the counts
    */
    final private void count(int mtype,int entry,int TYPE)
    {
	if (!byEntryPoint) { 
	    return;
	}
	if (userEntries[entry][TYPE][curPeIdx]==null) {
	    userEntries[entry][TYPE][curPeIdx]=new int[numIntervals+1];
	}
	if (TYPE==PROCESS && 
	    userEntries[entry][TIME][curPeIdx] == null) { 
	    //Add a time array, too
	    userEntries[entry][TIME][curPeIdx]=new int[numIntervals+1];
	}
	userEntries[entry][TYPE][curPeIdx][interval-intervalStart]++;
	
	int catIdx=mtypeToCategoryIdx(mtype);
	if (catIdx!=-1) {
	    if (categorized[catIdx][TYPE][curPeIdx]==null) {
		categorized[catIdx][TYPE][curPeIdx]=new int[numIntervals+1];
		if (TYPE==PROCESS) { //Add a time array, too
		    categorized[catIdx][TIME][curPeIdx] =
			new int[numIntervals+1];
		}
	    }
	    categorized[catIdx][TYPE][curPeIdx][interval-intervalStart]++;
	}
    }

    /**
       This is called when a log entry crosses a timing interval boundary
    */
    private void fillToInterval(int newinterval)
    {
	if (interval>=newinterval) {
	    return; //Nothing to do in this case, we're already past!
	}
	
	//Finish off the current interval
	int extra = (int) ((interval+1)*intervalSize - startTime);
	addToInterval(extra,interval,false);

	//Convert system usage and idle time from us to percent of an interval
	rescale(interval);

	//Fill in any intervals we would skip over completely
	for (int j=interval+1; j<newinterval; j++) {
	    addToInterval((int)intervalSize,j,true);
	}

	interval = newinterval;
    }

    public int[][][][] getSystemMsgs() { 
	return categorized; 
    }

    public int[][][] getSystemUsageData() { 
	return sysUsgData; 
    }

    public int[][][][] getUserEntries() {
	return userEntries;
    }
    
    // the interval values used are absolute. Only when array access is
    // required would it be offset by intervalStart.
    private void intervalCalc(int type, int mtype, int entry, long time) 
    {
	fillToInterval((int)(time/intervalSize));
	switch (type) {
	case ENQUEUE:
	    if (interval >= intervalStart && interval <= intervalEnd) {
		// Lengthen system queue
		sysUsgData[SYS_Q][curPeIdx][interval-intervalStart]++;
	    }
	    break;
	case CREATION:
	    if (interval >= intervalStart && interval <= intervalEnd) {
		// Lengthen system queue
		sysUsgData[SYS_Q][curPeIdx][interval-intervalStart]++;
		count(mtype,entry,CREATE);
	    }
	    break;
	case CREATION_MULTICAST:
	    // do nothing for now.
	    break;
	case BEGIN_PROCESSING:
	    processing++;
	    startTime = time;
	    if (interval >= intervalStart && interval <= intervalEnd) {
		//Shorten system queue
		sysUsgData[SYS_Q][curPeIdx][interval-intervalStart]--;
		count(currentMtype = mtype,currentEntry = entry,PROCESS);
	    }
	    break;
	case END_PROCESSING:
	    if (mtype == -5) {
		// the "ignore" flag
		break;
	    }
	    if (interval >= intervalStart && interval <= intervalEnd) {
		addToInterval((int)(time-startTime),interval,false);
	    }
	    processing--;
	    break;
	case BEGIN_IDLE:
	    processing++;
	    startTime = time;
	    currentEntry = IDLE_ENTRY;
	    break;
	case END_IDLE:
	    if (interval >= intervalStart && interval <= intervalEnd) {
		addToInterval((int)(time-startTime),interval,false);
	    }
	    processing--;
	    break;
	case END_TRACE:
	    //Sayantan:This is supposed to make sure that the entries following
	    //the end_trace are traced as idle with 0 util, until the next begin_trace
	    currentEntry = IDLE_ENTRY;
	    break;
	default:
	    System.out.println("Unhandled type "+type+" in logreader!");
	    break;
	}
    }

    /**
       Maps a message type to a categorized system message number
    */
    final private int mtypeToCategoryIdx(int mtype) {
	switch (mtype) {
	case NEW_CHARE_MSG: 
	    return 0;
	case FOR_CHARE_MSG: 
	    return 1;
	case BOC_INIT_MSG: 
	    return 2;
	case LDB_MSG: 
	    return 3;
	case QD_BROADCAST_BOC_MSG: case QD_BOC_MSG:
	    return 4;
	default:
	    return -1;
	}
    }

    /**
       read log file, with one more parameter containing 
       a list of processors to read.
    */
    public void read(long reqIntervalSize,
		     int NintervalStart, int NintervalEnd,
		     boolean NbyEntryPoint, OrderedIntList processorList)
    {
	numProcessors = Analysis.getNumProcessors();
	numUserEntries = Analysis.getNumUserEntries();
	intervalSize = reqIntervalSize;
	intervalStart = NintervalStart;
	intervalEnd = NintervalEnd;
	numIntervals = intervalEnd - intervalStart + 1;
	byEntryPoint=NbyEntryPoint;

	// assume full range of processor if null
        if (processorList == null) {
	    processorList = new OrderedIntList();
	    for (int pe=0; pe<numProcessors; pe++) {
		processorList.insert(pe);
	    }
        } else {
	    // **CW** required to set numProcessors to the correct values.
	    numProcessors = processorList.size();
	}

	ProgressMonitor progressBar = 
	    new ProgressMonitor(Analysis.guiRoot, "Reading log files",
				"", 0, numProcessors);
	progressBar.setNote("Allocating Global Memory");
	progressBar.setProgress(0);
	sysUsgData = new int[3][numProcessors][];
	if (byEntryPoint) {
	    userEntries = new 
		int[numUserEntries][3][numProcessors][numIntervals];
	    categorized = new int[5][3][numProcessors][];
	}
        processorList.reset();
 	int curPe = processorList.nextElement();
	curPeIdx = 0;
	for (;curPe!=-1; curPe=processorList.nextElement()) {
	    progressBar.setProgress(curPeIdx);
	    progressBar.setNote("[PE: " + curPe + " ] Allocating Memory.");

	    // gzheng: allocate sysUsgData only when needed.
	    sysUsgData[0][curPeIdx] = new int [numIntervals+1];
	    sysUsgData[1][curPeIdx] = new int [numIntervals+1];
	    sysUsgData[2][curPeIdx] = new int [numIntervals+1];
	    progressBar.setNote("[PE: " + curPe + " ] Reading data.");
	    if (progressBar.isCanceled()) {
		// clear all data and return
		userEntries = null;
		categorized = null;
		sysUsgData = null;
		return;
	    }
	    processing = 0;
	    interval = 0;
	    currentEntry = -1;
	    startTime =0;
	    
	    int nLines = 2;
	    
	    reader = new GenericLogReader(curPe, Analysis.getVersion());
	    boolean isProcessing = false;
	    try { 
		while (true) { //EOFException will terminate loop
		    reader.nextEvent(curData);
		    nLines++;
		    switch (curData.type) {
		    case BEGIN_IDLE: case END_IDLE:
			intervalCalc(curData.type, 0, 0, curData.time);
			break;
		    case CREATION:
			intervalCalc(curData.type, curData.mtype, 
				     curData.entry, curData.time);
			break;
		    case BEGIN_PROCESSING: 
			if (isProcessing) {
			    // add a pretend end processing event.
			    intervalCalc(END_PROCESSING, 
					 curData.mtype, curData.entry,
					 curData.time);
			    // not necessarily needed.
			    isProcessing = false;
			}
			intervalCalc(curData.type, curData.mtype, 
				     curData.entry, curData.time);
			isProcessing = true;
			break;
		    case END_PROCESSING:
			if (!isProcessing) {
			    // bad, ignore. (the safe thing to do)
			    // HAVE to add a dummy end event.
			    // **HACK** use -5 as mtype number to
			    // indicate the data is to be dropped.
			    // (fillToIntervals still needs to make
			    // progress though)
			    intervalCalc(curData.type, -5, 
					 curData.entry, curData.time);
			} else {
			    intervalCalc(curData.type, curData.mtype, 
					 curData.entry, curData.time);
			}
			isProcessing = false;
			break;
		    case ENQUEUE:
			intervalCalc(curData.type, curData.mtype, 
				     0, curData.time);
			break;
		    case END_COMPUTATION:
			fillToInterval(numIntervals);
			break;
		    case BEGIN_TRACE:
		    	//Sayantan: I think we do-not really need to do anything
			//for begin_trace. However, that may not be the case for a 
			//complex series of begin and end traces
			break;
		    case END_TRACE:
		    	intervalCalc(curData.type,curData.mtype,0,curData.time);
			break;
		    }
		}
	    } catch (EOFException e) {
		// Do nothing
	    } catch (IOException e) {
		System.err.println("Error: Failure to read log file!");
		System.exit(-1);
	    }
            try {
                reader.close();
            } catch (IOException e) {
                System.err.println("Error! Failed to close reader!");
                System.exit(-1);
            }
	    curPeIdx++;
	} // for loop
	progressBar.close();
    }

    //Convert system usage and idle time from us to percent of an interval
    private void rescale(int j) {
	if (j < intervalStart || j > intervalEnd) {
	    return;
	}
	sysUsgData[SYS_CPU ][curPeIdx][j-intervalStart] =
	    (int)(sysUsgData[SYS_CPU ][curPeIdx][j-intervalStart]*100/intervalSize);
	sysUsgData[SYS_IDLE][curPeIdx][j-intervalStart] =
	    (int)(sysUsgData[SYS_IDLE][curPeIdx][j-intervalStart]*100/intervalSize);
    }

}
