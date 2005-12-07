package projections.analysis;

import java.io.*;
import java.util.*;

import javax.swing.*;

import projections.misc.*;
import projections.gui.*;

public class PoseDopReader
{
    // This is a simple data indexed by interval id
    private static final int NUM_SIM_STATES = 2;
    private static final int FORWARD_SIM_PROGRESS = 0;
    private static final int NO_SIM_PROGRESS = 1;

    private int[][] dopRealTime;
    private int[] dopVirtTime;

    private BufferedReader reader;
    private ParseTokenizer tokenizer;

    private long totalRealTime;
    private long totalVirtualTime;

    private OrderedIntList validPEs;

    // **************************************************************
    // * The default constructor will check to see if Pose end times
    // * are available in the "pgm.projrc" file and determine end
    // * times if necessary.
    // **************************************************************
    public PoseDopReader() {
	validPEs =
	    Analysis.getValidProcessorList(Analysis.DOP);
    }
    
    public long getTotalRealTime() {
	if (totalRealTime > 0) {
	    return totalRealTime;
	} else {
	    computeEndTimes();
	}
	return totalRealTime;
    }

    public long getTotalVirtualTime() {
	if (totalVirtualTime > 0) {
	    return totalVirtualTime;
	} else {
	    computeEndTimes();
	}
	return totalVirtualTime;
    }

    /**
     *  This routine computes both real and virtual end times
     */
    private void computeEndTimes() {
	int curPe;
	int curPeIdx = 0;
	int numProcessors = validPEs.size();
	
	double eventEnd;
	long virtualEnd;
	
	ProgressMonitor progressBar =
	    new ProgressMonitor(Analysis.guiRoot, "Computing End Times",
				"", 0, numProcessors);
	validPEs.reset();
	long eventCount = 0;
	curPe = validPEs.nextElement();
	while (curPe != -1) {
	    progressBar.setProgress(curPeIdx);
	    progressBar.setNote("[PE: " + curPe + "] Getting End Time ...");
	    try {
		reader = 
		    new BufferedReader(new FileReader(Analysis.getPoseDopName(curPe)));
		initTokenizer(reader);
		// read all lines (no choice in dop format)
		while (true) {
		    try {
			// read a line. Format is fixed.
			// 2 doubles, 2 integers
			eventEnd = tokenizer.nextNumber("Fake Start event"); 
			eventEnd = tokenizer.nextNumber("end of event"); 
			virtualEnd = (long)tokenizer.nextNumber("Fake S VT");
			virtualEnd = (long)tokenizer.nextNumber("end VT");

			if ((long)(eventEnd*1.0e6) > totalRealTime) {
			    totalRealTime = (long)(eventEnd*1.0e6);
			}
			if (virtualEnd > totalVirtualTime) {
			    totalVirtualTime = virtualEnd;
			}
			tokenizer.nextToken(); // clear the EOL
		    } catch (IOException e) {
			break; // Abuse IO Exception. done! exit the loop.
		    }
		}
	    } catch (IOException e) {
		// other failure in IO or unexpected format error, abort
		System.err.println(e.toString());
		System.exit(-1);
	    }
	    curPe = validPEs.nextElement();
	    curPeIdx++;
	}
	progressBar.close();
    }

    public void read(long realIntervalSize, 
		     int realStartInterval, int realEndInterval,
		     long virtIntervalSize,
		     int virtStartInterval, int virtEndInterval,
		     OrderedIntList processorList) {
	int curPe;
	int curPeIdx = 0;
	int numProcessors = processorList.size();

	long eventStart;
	long eventEnd;
	long virtualStart;
	long virtualEnd;

	long realStartTime = (long)(realStartInterval*realIntervalSize);
	long realEndTime = (long)(realEndInterval*realIntervalSize);
	long virtStartTime = (long)(virtStartInterval*virtIntervalSize);
	long virtEndTime = (long)(virtEndInterval*virtIntervalSize);

	// **FIXME** Hardcode 2 types = 0 - Real time progress
	//                              1 - Real time no progress
	// **FIXME** Hardcode assumption of maximum of 10 seconds of 
	//           "real" (overlapped) execution or 10,000,000 units
	//           of virtual time. This caps memory usage to 120MB.
	//           Longer runs will require multi-pass capability.
	//
	// tempStorage stores the number of changes at each lowest unit
	// of time.
	int tempRealData[][] = new int[NUM_SIM_STATES][10000000];
	int tempVirtData[] = new int[10000000];
	int realState;

	long eventCount = 0;

	ProgressMonitor progressBar = 
	    new ProgressMonitor(Analysis.guiRoot, "Reading dop files",
				"", 0, numProcessors);
	curPe = processorList.nextElement();
	while (curPe != -1) {
	    progressBar.setProgress(curPeIdx);
	    progressBar.setNote("[PE: " + curPe + "] Reading ...");
	    try {
		reader = 
		    new BufferedReader(new FileReader(Analysis.getPoseDopName(curPe)));
		initTokenizer(reader);
		// read all lines (no choice in dop format)
		while (true) {
		    try {
			// read a line. Format is fixed.
			// 2 doubles, 2 integers
			eventStart = 
			    (long)(tokenizer.nextNumber("start of event")*
				   1.0e6); 
			eventEnd = 
			    (long)(tokenizer.nextNumber("end of event")*
				   1.0e6);
			virtualStart = (long)tokenizer.nextNumber("start VT");
			virtualEnd = (long)tokenizer.nextNumber("end VT");

			// ******** Determine Real Time State *********
			if (virtualEnd - virtualStart > 0) {
			    realState = FORWARD_SIM_PROGRESS;
			} else {
			    realState = NO_SIM_PROGRESS;
			}

			// NOTE: Real times are handled differently from
			//       Virtual times (point-based vs 
			//       "interval"-based). Start times and End times
			//       contribute to the simultanuity factor of
			//       their specified time-point (positively and
			//       negatively respectively) *EXCEPT* when
			//       the Start and End times happen to be 
			//       simultaneous. In the latter case, the End 
			//       time contributes negatively to the subsequent
			//       time-point instead. (ie. we do not end up
			//       wiping out "zero" length events).
			// 
			//       Hence the following example is
			//       true:
			//                   |---------------|  e1
			//       |-----------|                  e2
			//                   |                  e3
			// 
			//           e3 is simultaneous with e1
			//       but e2 is independent of both e1 and e3
			// 
			// Process Real Time
			if ((eventStart < realStartTime) &&
			    (eventEnd >= realStartTime)) {
			    tempRealData[realState][0]++;
			    if (eventEnd <= realEndTime) {
				tempRealData[realState][(int)(eventEnd-realStartTime)]--;
			    }
			} else if (eventStart >= realStartTime) {
			    if (eventStart <= realEndTime) {
				tempRealData[realState][(int)(eventStart-realStartTime)]++;
			    }
			    if ((eventEnd == eventStart) &&
				(eventEnd < realEndTime)) {
				tempRealData[realState][(int)(eventEnd-realStartTime+1)]--;
			    } else if (eventEnd <= realEndTime) {
				tempRealData[realState][(int)(eventEnd-realStartTime)]--;
			    }
			}
			
			// ******** Determine Virtual Time State *********

			// NOTE: Pose generates -1 virtual time events for 
			//       events that are part of the same object
			//       and hence can never overlap for virtual
			//       time. So, we drop these events for the
			//       purposes of virtual time.
			if ((virtualStart == -1) || (virtualEnd == -1)) {
			    tokenizer.nextToken();
			    continue;
			}

			if ((virtualStart < virtStartTime) &&
			    (virtualEnd >= virtStartTime)) {
			    tempVirtData[0]++;
			    if (virtualEnd < virtEndTime) {
				tempVirtData[(int)(virtualEnd-virtStartTime+1)]--;
			    }
			} else if (virtualStart >= virtStartTime) {
			    if (virtualStart <= virtEndTime) {
				tempVirtData[(int)(virtualStart-virtStartTime)]++;
			    }
			    if (virtualEnd < virtEndTime) {
				tempVirtData[(int)(virtualEnd-virtStartTime+1)]--;
			    }
			}

			tokenizer.nextToken(); // clear the EOL
		    } catch (IOException e) {
			break; // Abuse IO Exception. done! exit the loop.
		    }
		}
	    } catch (IOException e) {
		// other failure in IO or unexpected format error, abort
		System.err.println(e.toString());
		System.exit(-1);
	    }
	    curPe = processorList.nextElement();
	    curPeIdx++;
	}
	progressBar.close();

	// ************ POST PROCESSING PHASE ************
	// Place raw counts into averaged form
	// in the appropriate intervals.

	// initialize the data array
	int numIntervals = 
	    realEndInterval - realStartInterval + 1;
	int numVTIntervals = 
	    virtEndInterval - virtStartInterval + 1;
	dopRealTime = new int[numIntervals][NUM_SIM_STATES];
	dopVirtTime = new int[numVTIntervals];

	
	// Processing Real Time Events
	long runningCount[] = new long[NUM_SIM_STATES];
	for (int i=0; i<numIntervals; i++) {
	    for (int state=0; state<NUM_SIM_STATES; state++) {
		double intervalSum[] = new double[NUM_SIM_STATES];
		for (int j=0; j<realIntervalSize; j++) {
		    runningCount[state] +=
			tempRealData[state][(int)(i*realIntervalSize+j)];
		    intervalSum[state] += runningCount[state];
		}
		dopRealTime[i][state] = 
		    (int)Math.floor(intervalSum[state]/realIntervalSize);
	    }
	    // System.out.println(i + " " + 
	    //	       (dopRealTime[i][0] + dopRealTime[i][1]));
	}
	
	// Processing Virtual Time Events
	long runningVTCount = 0;
	for (int i=0; i<numVTIntervals; i++) {
	    double intervalSum = 0.0;
	    for (int j=0; j<virtIntervalSize; j++) {
		runningVTCount += 
		    tempVirtData[(int)(i*virtIntervalSize+j)];
		intervalSum += runningVTCount;
	    }
	    dopVirtTime[i] = 
		(int)Math.floor(intervalSum/virtIntervalSize);
	    // System.out.println(i + " " + dopVirtTime[i]);
	}
    }
    
    public int[][] getRealTimeDopData() {
	return dopRealTime;
    }

    public int[] getVirtualTimeDopData() {
	return dopVirtTime;
    }

    private void initTokenizer(BufferedReader reader) {
	tokenizer=new ParseTokenizer(reader);
	tokenizer.parseNumbers();
	tokenizer.eolIsSignificant(true);
	tokenizer.whitespaceChars('/','/'); 
	tokenizer.whitespaceChars(':',':');
	tokenizer.whitespaceChars('[','[');
	tokenizer.whitespaceChars(']',']');
	tokenizer.wordChars('a','z');
	tokenizer.wordChars('A','Z');
    }
}
