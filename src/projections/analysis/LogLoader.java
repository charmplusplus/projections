package projections.analysis;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import projections.Tools.Timeline.TimelineMessage;
import projections.Tools.Timeline.UserEventObject;
import projections.gui.MainWindow;
import projections.misc.LogEntryData;
import projections.misc.LogLoadException;

/** This class reads in .log files and turns them into a timeline.  */
public class LogLoader extends ProjDefs
{
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private int myRun = 0;

	private boolean ampiTraceOn = false;

	/**Determine the earliest begin event time for timeline range adjustment */
	public long determineEarliestBeginEventTime(SortedSet<Integer> selectedPEs, SortedSet<Integer> validPEs)
	{

		//==========================================
		// Do multithreaded file reading

		// Create a list of worker threads
		LinkedList workerThreads = new LinkedList();

		for(Integer pe : selectedPEs){
			if (validPEs.contains(pe)) workerThreads.add(new LogLoaderBeginEventThread(pe) );
		}

		if (workerThreads.size() == 0) return 0; // If user entered only invalid PEs, return default start time.

		// Pass this list of threads to a class that manages/runs the threads nicely
		TimedProgressThreadExecutor threadManager = new TimedProgressThreadExecutor("Computing Earliest Begin Event Time in Parallel", workerThreads, MainWindow.runObject[myRun].guiRoot, true);
		threadManager.runAll();


		Iterator iter = workerThreads.iterator();
		long earliestTimeFound = Long.MAX_VALUE;
		while(iter.hasNext()){
			LogLoaderBeginEventThread worker = (LogLoaderBeginEventThread) iter.next();
			if(worker.result < earliestTimeFound ){
				earliestTimeFound = worker.result;
			}
		}

		return earliestTimeFound;
	}

	/**Determine the latest end event time for timeline range adjustment */
	public long determineLatestEndEventTime(SortedSet<Integer> selectedPEs, SortedSet<Integer> validPEs)
	{

		//==========================================
		// Do multithreaded file reading

		// Create a list of worker threads
		LinkedList workerThreads = new LinkedList();

		for(Integer pe : selectedPEs){
			if (validPEs.contains(pe)) workerThreads.add(new LogLoaderEndEventThread(pe) );
		}

		if (workerThreads.size() == 0) return MainWindow.runObject[myRun].getTotalTime(); // If user entered only invalid PEs, return default end time.

		// Pass this list of threads to a class that manages/runs the threads nicely
		TimedProgressThreadExecutor threadManager = new TimedProgressThreadExecutor("Computing Latest End Event Time in Parallel", workerThreads, MainWindow.runObject[myRun].guiRoot, true);
		threadManager.runAll();


		Iterator iter = workerThreads.iterator();
		long latestTimeFound = Long.MIN_VALUE;
		while(iter.hasNext()){
			LogLoaderEndEventThread worker = (LogLoaderEndEventThread) iter.next();
			if(worker.result > latestTimeFound ){
				latestTimeFound = worker.result;
			}
		}

		return latestTimeFound;
	}

	/** Determine the max endtime from any trace file, by seeking to the end and looking at the last few records */
	public long determineEndTime(SortedSet<Integer> validPEs)
	{
				
		//==========================================	
		// Do multithreaded file reading

		// Create a list of worker threads
		LinkedList workerThreads = new LinkedList();

		for(Integer pe : validPEs){
			workerThreads.add(new LogLoaderEndTimeThread(pe) );
		}
	
		// Pass this list of threads to a class that manages/runs the threads nicely
		TimedProgressThreadExecutor threadManager = new TimedProgressThreadExecutor("Computing End Time in Parallel", workerThreads, MainWindow.runObject[myRun].guiRoot, true);
		threadManager.runAll();

		
		Iterator iter = workerThreads.iterator();
		long maxTimeFound = Long.MIN_VALUE;
		while(iter.hasNext()){
			LogLoaderEndTimeThread worker = (LogLoaderEndTimeThread) iter.next();
			if(worker.result > maxTimeFound ){
				maxTimeFound = worker.result;
			}
		}
		
		return maxTimeFound;
	}

	/**
	 * Author: Chao Mei
	 * The procThdVec contains different processing threads vector, 
	 * in which every processing threads
	 * have different id (specified by LogEntry's field "id") 
	 */
	public void createAMPIUsageProfile(int procId, long beginTime, 
			long endTime, List<AmpiProcessProfile> procThdVec)
	throws LogLoadException
	{
		GenericLogReader logFileRd = null;
		LogEntryData rawLogData = null;
		LogEntry curEntry = null;
		LogEntry prevEntry = null;

		/**
		 * Variables related to ampi support.
		 * Initially, I tend to maintain a local global variable of 
		 * current function. But it turns out
		 * to be unnecessary and possibly wrong because:
		 * 1. The current function is always the top element of 
		 *    curProcessing's auxCallFuncStack.
		 * 2. There are cases when a single processing only contains 
		 *    part of a function. This function's end
		 *    is in the later run of this processing. In log file, 
		 *    it looks like:
		 *    BEGIN_PROCESSING...BEGIN_FUNC..END_PROCESSING...
		 *    ...BEGIN_PROCESSING...END_FUNC...END_PROCESSING
		 *    In these cases, maintain the current function variable 
		 *    is somewhat difficult. But it will be easy
		 *    to use current processing's auxCallFuncStack to get the 
		 *    current running function.
		 */
		AmpiProcessProfile curProcessing = null;


		/**
		 * key = process' triple id
		 * value = an instance of AmpiProcessProfile
		 */
		Hashtable procThdMap = new Hashtable();

		try{
			logFileRd = new GenericLogReader(procId,MainWindow.runObject[myRun].getVersion());
			
			/** 
			 * seek the first BEGIN_PROCESSING within this time interval 
			 * and its timestamp >= beginTime         .
			 * Therefore, any functions that before the BEGIN_PROCESSING 
			 * is ignored.
			 * This could be somewhat an error. Consider a dummy 
			 * function LATER!
			 */
			while(true){
				rawLogData = logFileRd.nextEvent();
				curEntry = new LogEntry(rawLogData);
				if(curEntry.TransactionType==BEGIN_PROCESSING 
						&& curEntry.Entry!=-1
						&& curEntry.Time >= beginTime){
					curProcessing = new AmpiProcessProfile(curEntry.id);
					break;
				}
			}

			// Found the starting point, initialize the prevEntry
			prevEntry = curEntry;

			/** 
			 * Processing log file focusing on BEGIN/END_FUNC, 
			 * BEGIN/END_PROCESSING 
			 * Assumming following conditions:
			 * 1. BEGIN/END_PROCESSING cannot be overlapped
			 * 2. BEGIN/END_PACK/UNPACK cannot be overlapped
			 * 3. BEGIN/END_FUNCTION can be overlapped
			 * 4. In one processing interval, BEGIN/END_FUNC maynot be 
			 *    paired. But in terms of the whole processing
			 *    (a processing of same id may be divided into several 
			 *    pieces), BEGIN/END_FUNC must be paired
			 * 5. Currently, between the beginTime and endTime, 
			 *    BEGIN/END_FUNC are assumed to be paired.
			 */
			boolean reachEndTime = false;
			while(!reachEndTime){
				rawLogData = logFileRd.nextEvent();
				curEntry = new LogEntry(rawLogData);
				//something must be wrong with the log file
				if (curEntry.Entry == -1) 
					continue;
				switch(curEntry.TransactionType){
				case BEGIN_PROCESSING:{                
					if(curProcessing!=null){
						System.err.println("Error in parsing log file " +
								"as processing overlapped!");
						return;
					} else {
						// just start a new processing but need to 
						// check whether it is the same
						// processing that has been stored in the "procThdMap"
						AmpiProcessProfile tmp = 
							new AmpiProcessProfile(curEntry.id);
						AmpiProcessProfile storedProfile = 
							(AmpiProcessProfile)procThdMap.get(tmp.toHashKey());
						curProcessing = 
							(storedProfile==null ? tmp:storedProfile);
					}
					break;
				}
				case END_PROCESSING:{                
					/**
					 * Processing the end of a processing. If there're 
					 * functions within this processing
					 * push the process into the procThdMap. 
					 * Otherwise do nothing.
					 * Upto this point, curProcessing mustn't be null!
					 * Processing cannot overlap! i.e it will not appear 
					 * the sequence like:
					 * BEGIN_PROCESSING ... BEGIN_PROCESSING...
					 * END_PROCESSING...END_PROCESSING
					 * Compute the accumlated execution time for this 
					 * process (in terms of its ObjectId)
					 */
					if (curProcessing==null) {
						System.err.println("Error in parsing log file " +
						"as processing is not paired!");
						return;
					}
					/*
		    if (!curEntry.id.compare(curProcessing.getProcessID())){
		         System.err.println("Error in parsing log file " +
			                    "as processing overlapped!");
                        return;
                    }
					 */
					curProcessing.incrAccExecTime(curEntry.Time - 
							prevEntry.Time);
					if (!curProcessing.getAuxCallFuncStack().empty()) {
						AmpiFunctionData curFunc = 
							(AmpiFunctionData)curProcessing.getAuxCallFuncStack().peek();
						curFunc.incrAccExecTime(curEntry.Time - 
								prevEntry.Time);
					}

					// Only store the processing that hasn't been stored!
					if (procThdMap.get(curProcessing.toHashKey())==null) {
						procThdMap.put(curProcessing.toHashKey(), 
								curProcessing);
					}
					curProcessing = null;

					// The parsing will end only when it reaches 
					// completely paired BEGIN/END_PROCESSING
					if (curEntry.Time >= endTime) {
						reachEndTime = true;
					}
					break;
				}
				case BEGIN_FUNC: {                
					if (curProcessing==null) {
						System.err.println("Error in parsing log file " +
								"as a function is not in a " +
						"processing!");
						return;
					}

					// first compute the accumlated time for the current 
					// processing!
					curProcessing.incrAccExecTime(curEntry.Time - 
							prevEntry.Time);
					// second deal with the new function
					AmpiFunctionData curFunc = curEntry.ampiData;
					Stack auxStk = curProcessing.getAuxCallFuncStack();
					auxStk.push(curFunc);
					break;
				}
				case END_FUNC:{                
					if (curProcessing==null) {
						System.err.println("Error in parsing log file " +
								"as a function is not in a " +
						"processing!");
						return;
					}
					if (curProcessing.getAuxCallFuncStack().empty()) {
						System.err.println("Error in parsing log file as " +
								"a function is not paired " +
						"properly!");
						return;
					}
					AmpiFunctionData curFunc = 
						(AmpiFunctionData)curProcessing.getAuxCallFuncStack().peek();
					if (curFunc.FunctionID != curEntry.FunctionID) {
						System.err.println("Error in parsing log file as " +
								"a function is not paired " +
						"properly!");
						return;
					}
					curProcessing.incrAccExecTime(curEntry.Time - 
							prevEntry.Time);
					curFunc.incrAccExecTime(curEntry.Time - prevEntry.Time);
					// as the current function is completed, it is popped 
					// from the auxCallFuncStack and pushed to 
					// the final callFuncStack associated with curProcessing
					curProcessing.getAuxCallFuncStack().pop();
					curProcessing.getFinalCallFuncStack().push(curFunc);
					break;
				}
				case BEGIN_PACK:
				case BEGIN_UNPACK:
				case CREATION:{
					if (curProcessing==null) {
						break;
					}
					curProcessing.incrAccExecTime(curEntry.Time - 
							prevEntry.Time);
					if (curProcessing.getAuxCallFuncStack().empty()) {
						break;
					}
					AmpiFunctionData curFunc = 
						(AmpiFunctionData)curProcessing.getAuxCallFuncStack().peek();
					curFunc.incrAccExecTime(curEntry.Time - prevEntry.Time);
					break;
				}
				/**
				 * The cases: END_PACK, END_UNPACK 
				 * are neglected as their time is not contributed 
				 * to the total execution of 
				 * the current processing and the current function
				 */               
				default:
					break;
				}
				prevEntry = curEntry;
			}
		} catch (EndOfLogSuccess e) { 
			/*ignore*/ 
		} catch (FileNotFoundException E) {
			System.out.println("ERROR: couldn't open file " + 
					MainWindow.runObject[myRun].getLogName(procId));
		} catch (IOException E) {
			throw new LogLoadException(MainWindow.runObject[myRun].getLogName(procId));
		}

		// finally select the processes that have functions and 
		// push them to the procThdVec
		for (Enumeration e=procThdMap.keys(); e.hasMoreElements();) {
			AmpiProcessProfile p =
				(AmpiProcessProfile)procThdMap.get(e.nextElement());
			if (p.getFinalCallFuncStack().size()>0) {
				procThdVec.add(p);
			}
		}
	}

	/**
	 * Author: Chao Mei
	 * The procThdVec contains different processing threads vector, 
	 * in which every processing threads
	 * have different id (specified by LogEntry's field "id")
	 * This function's logical and data flow is same with 
	 * createAMPIUsageProfile.
	 * To some extent,createAMPIUsageProfile function can be implemented 
	 * by calling createAMPIFuncTimeProfile.
	 * However, as I was first asked to implement ampi function's 
	 * usage profile, I designed the implementation
	 * without considering the ampi function's time profile. Therefore, 
	 * both of these functions share very similar
	 * codes. If I have time later, I will rewrite the 
	 * createAMPIUsageProfile function by using this function as 
	 * the stub!!!
	 */
	public void createAMPIFuncTimeProfile(int procId, long beginTime, 
			long endTime, List<AmpiProcessProfile> procThdVec)
	throws LogLoadException        
	{

		GenericLogReader logFileRd = null;
		LogEntryData rawLogData = null;
		LogEntry curEntry = null;
		LogEntry prevEntry = null;

		/**
		 * Variables related to ampi support.
		 * Initially, I tend to maintain a local global variable of 
		 * current function. But it turns out
		 * to be unnecessary and possibly wrong because:
		 * 1. The current function is always the top element of 
		 *    curProcessing's auxCallFuncStack.
		 * 2. There are cases when a single processing only contains 
		 *    part of a function. This function's end
		 *    is in the later run of this processing. In log file, 
		 *    it looks like:
		 *    BEGIN_PROCESSING...BEGIN_FUNC..END_PROCESSING...
		 *    ...BEGIN_PROCESSING...END_FUNC...END_PROCESSING
		 *    In these cases, maintain the current function variable 
		 *    is somewhat difficult. But it will be easy
		 *    to use current processing's auxCallFuncStack to get the 
		 *    current running function.
		 */
		AmpiProcessProfile curProcessing = null;

		/**
		 * key = process' triple id
		 * value = an instance of AmpiProcessProfile
		 */
		Hashtable procThdMap = new Hashtable();

		try {
			logFileRd = new GenericLogReader(procId,MainWindow.runObject[myRun].getVersion());

			/** 
			 * seek the first BEGIN_PROCESSING within this time interval 
			 * and its timestamp >= beginTime         .
			 * Therefore, any functions that before the BEGIN_PROCESSING 
			 * is ignored.
			 * This could be somewhat an error. Consider a dummy 
			 * function LATER!
			 */
			while(true) {
				rawLogData = logFileRd.nextEvent();
				curEntry = new LogEntry(rawLogData);
				if (curEntry.TransactionType==BEGIN_PROCESSING 
						&& curEntry.Entry!=-1
						&& curEntry.Time >= beginTime) {
					curProcessing = 
						new AmpiProcessProfile(curEntry.id);
					break;
				}
			}

			// Found the starting point, initialize the prevEntry
			prevEntry = curEntry;

			/** 
			 * Processing log file focusing on BEGIN/END_FUNC, 
			 * BEGIN/END_PROCESSING 
			 * Assumming following conditions:
			 * 1. BEGIN/END_PROCESSING cannot be overlapped
			 * 2. BEGIN/END_PACK/UNPACK cannot be overlapped
			 * 3. BEGIN/END_FUNCTION can be overlapped
			 * 4. In one processing interval, BEGIN/END_FUNC maynot be paired.
			 *    But in terms of the whole processing
			 *    (a processing of same id may be divided into several 
			 *    pieces), BEGIN/END_FUNC must be paired
			 * 5. Currently, between the beginTime and endTime, 
			 *    BEGIN/END_FUNC are assumed to be paired.
			 */
			boolean reachEndTime = false;
			while (!reachEndTime) {
				rawLogData = logFileRd.nextEvent();
				curEntry = new LogEntry(rawLogData);
				//something must be wrong with the log file
				if (curEntry.Entry == -1) 
					continue;
				switch (curEntry.TransactionType) {
				case BEGIN_PROCESSING: {                
					if (curProcessing!=null) {
						System.err.println("Error in parsing log file " +
								"as processing overlapped!");
						return;
					} else {
						// just start a new processing but need to check 
						// whether it is the same
						// processing that has been stored in the "procThdMap"
						AmpiProcessProfile tmp = 
							new AmpiProcessProfile(curEntry.id);
						AmpiProcessProfile storedProfile = 
							(AmpiProcessProfile)procThdMap.get(tmp.toHashKey());
						curProcessing = 
							(storedProfile==null ? tmp:storedProfile);
					}
					break;
				}
				case END_PROCESSING: {                
					/**
					 * Processing the end of a processing. If there're 
					 * functions within this processing
					 * push the process into the procThdMap. Otherwise 
					 * do nothing.
					 * Upto this point, curProcessing mustn't be null!
					 * Processing cannot overlap! i.e it will not appear 
					 * the sequence like:
					 * BEGIN_PROCESSING ... BEGIN_PROCESSING...
					 * END_PROCESSING...END_PROCESSING
					 * Compute the accumlated execution time for this 
					 * process (in terms of its ObjectId)
					 */
					if (curProcessing==null) {
						System.err.println("Error in parsing log file as " +
						"processing is not paired!");
						return;
					}

					curProcessing.incrAccExecTime(curEntry.Time - 
							prevEntry.Time);
					if (!curProcessing.getAuxCallFuncStack().empty()) {
						AmpiFunctionData curFunc = 
							(AmpiFunctionData)curProcessing.getAuxCallFuncStack().peek();
						AmpiFunctionData.AmpiFuncExecInterval gap = 
							new AmpiFunctionData.AmpiFuncExecInterval(prevEntry.Time,curEntry.Time);
						curFunc.insertExecInterval(gap);
						curFunc.incrAccExecTime(curEntry.Time - 
								prevEntry.Time);
					}

					// Only store the processing that hasn't been stored!
					if (procThdMap.get(curProcessing.toHashKey())==null) {
						procThdMap.put(curProcessing.toHashKey(), 
								curProcessing);
					}
					curProcessing = null;

					// The parsing will end only when it reaches completely 
					// paired BEGIN/END_PROCESSING
					if (curEntry.Time >= endTime) {
						reachEndTime = true;
					}
					break;
				}
				case BEGIN_FUNC: {                
					if (curProcessing==null) {
						System.err.println("Error in parsing log file as " +
								"a function is not in a " +
						"processing!");
						return;
					}

					// first compute the accumlated time for the current 
					// processing!
					curProcessing.incrAccExecTime(curEntry.Time - 
							prevEntry.Time);
					// second deal with the new function
					AmpiFunctionData curFunc = curEntry.ampiData;
					Stack auxStk = curProcessing.getAuxCallFuncStack();
					auxStk.push(curFunc);
					break;
				}
				case END_FUNC: {                
					if (curProcessing==null) {
						System.err.println("Error in parsing log file as " +
								"a function is not in a " +
						"processing!");
						return;
					}
					if (curProcessing.getAuxCallFuncStack().empty()) {
						System.err.println("Error in parsing log file as " +
								"a function is not paired " +
						"properly!");
						return;
					}
					AmpiFunctionData curFunc = 
						(AmpiFunctionData)curProcessing.getAuxCallFuncStack().peek();
					if (curFunc.FunctionID != curEntry.FunctionID) {
						System.err.println("Error in parsing log file as " +
								"a function is not paired " +
						"properly!");
						return;
					}
					curProcessing.incrAccExecTime(curEntry.Time - 
							prevEntry.Time);
					AmpiFunctionData.AmpiFuncExecInterval gap = 
						new AmpiFunctionData.AmpiFuncExecInterval(prevEntry.Time,curEntry.Time);
					curFunc.insertExecInterval(gap);
					curFunc.incrAccExecTime(curEntry.Time - prevEntry.Time);
					// as the current function is completed, it is popped 
					// from the auxCallFuncStack and pushed to 
					// the final callFuncStack associated with curProcessing
					curProcessing.getAuxCallFuncStack().pop();
					curProcessing.getFinalCallFuncStack().push(curFunc);
					break;
				}
				case BEGIN_PACK:
				case BEGIN_UNPACK:
				case CREATION: {
					if (curProcessing==null) {
						break;
					}
					curProcessing.incrAccExecTime(curEntry.Time - 
							prevEntry.Time);
					if (curProcessing.getAuxCallFuncStack().empty()) {
						break;
					}
					AmpiFunctionData curFunc = 
						(AmpiFunctionData)curProcessing.getAuxCallFuncStack().peek();
					AmpiFunctionData.AmpiFuncExecInterval gap = 
						new AmpiFunctionData.AmpiFuncExecInterval(prevEntry.Time,curEntry.Time);
					curFunc.insertExecInterval(gap);
					curFunc.incrAccExecTime(curEntry.Time - prevEntry.Time);
					break;
				}
				/**
				 * The cases: END_PACK, END_UNPACK 
				 * are neglected as their time is not contributed to the 
				 * total execution of 
				 * the current processing and the current function
				 */               
				default:
					break;
				}
				prevEntry = curEntry;
			}
		} catch (EndOfLogSuccess e) { 
			/*ignore*/ 
		} catch (FileNotFoundException E) {
			System.out.println("ERROR: couldn't open file " + 
					MainWindow.runObject[myRun].getLogName(procId));
		} catch (IOException E) {
			throw new LogLoadException(MainWindow.runObject[myRun].getLogName(procId));
		}

		// finally select the processes that have functions and push 
		// them to the procThdVec
		for (Enumeration e=procThdMap.keys(); e.hasMoreElements();) {
			AmpiProcessProfile p = 
				(AmpiProcessProfile)procThdMap.get(e.nextElement());
			if (p.getFinalCallFuncStack().size()>0) {
				procThdVec.add(p);
			}
		}
	}

	/** Read the timeline for a single PE and return the result as a Collection of TimelineEvent's */
	public void createtimeline(int pe, long Begin, long End, Deque<TimelineEvent> timeline, Collection<UserEventObject>  userEventVector, long minEntryDuration)
	throws LogLoadException
	{
		
		long BeginTime = 0;

		long              Time        = Long.MIN_VALUE;
		LogEntry          LE          = null;
		TimelineEvent     TE          = null;
		// just for temp purposes
		UserEventObject         userEventObject   = null;  
		TimelineMessage   TM          = null;
		PackTime          PT          = null;
		boolean tempte;

		ArrayList<UserEventObject> userEventPairStarts = new ArrayList<UserEventObject>();

		// See if we need to use some predefined offsets to adjust the times read from the file:
		long shiftAmount = MainWindow.runObject[myRun].tachyonShifts.getShiftAmount(pe);
		
		// open the file
		GenericLogReader reader = new GenericLogReader(pe,MainWindow.runObject[myRun].getVersion());

		try {
			// to treat dummy thread EPs as a special-case EP
			//  **CW** I consider this a hack. A more elegant way must
			// be found design-wise.
			if (MainWindow.runObject[myRun].getNumFunctionEvents() > 0) {
				ampiTraceOn = true;
			}

			boolean isProcessing = false;
			LogEntry lastBeginEvent = null;
			TimelineEvent lastBeginTimelineEvent = null;
			
//			// We will lookup a good seek point from the index file
//			long offsetToBeginRecord = index.lookupIndexOffset(PeNum,Begin);
//			
//			// If we found a file offset to seek, we do the seek
//			if(offsetToBeginRecord != -1){
//				System.out.println("Found offset "+ offsetToBeginRecord + " in index file for pe "+PeNum);
//				reader.seek(offsetToBeginRecord);
//			}		
//			
			
			while (true) { //Seek to time Begin
				LogEntryData data = reader.nextEvent();
				LE = new LogEntry(data);
				if (LE.Time >= Begin) {
					break;
				}
				if (LE.Entry == -1) {
					continue;
				}
				// This is still not ideal. There are cases which may cause
				// a rogue begin event to have data dropped at the beginning.
				if ((LE.TransactionType == BEGIN_PROCESSING) && 
						(LE.Entry != -1)) {
					Time       = LE.Time - BeginTime;
					lastBeginEvent = LE;
				} else if ((LE.TransactionType == END_PROCESSING) &&
						(LE.Entry != -1)) {
					Time       = LE.Time - BeginTime;
					lastBeginEvent = null;
				} else if (LE.TransactionType == BEGIN_IDLE) {
					Time = LE.Time - BeginTime;
					lastBeginEvent = LE;
				} else if (LE.TransactionType == END_IDLE) {
					lastBeginEvent = null;
				}
			}
			
			
			
			
			if (Time == Long.MIN_VALUE) {
				Time = Begin;
			}
			if (LE.Time > End) {
				switch (LE.TransactionType) {
				case BEGIN_PROCESSING:
					// the whole line must be empty
					System.out.println("finished empty timeline for " + 
							pe);
					return;                              
				case END_PROCESSING:
					// the whole line is straddled by that single entry method
					// in this case, we know the actual bounds of the entry 
					// method
					if ((lastBeginEvent != null) &&
							(lastBeginEvent.TransactionType==BEGIN_PROCESSING) &&
							(lastBeginEvent.Entry == LE.Entry)) {
						timeline.add(TE=
							new TimelineEvent(lastBeginEvent.Time-BeginTime,
									LE.Time-BeginTime,
									lastBeginEvent.Entry,
									lastBeginEvent.Pe));
					}
					return;
				case BEGIN_IDLE:
					// the whole line must be empty
					System.out.println("finished empty timeline for " +
							pe);
					return;
				case END_IDLE:
					// the whole line is straddled by idle time.
					// we also know the complete bounds of the idle time.
					if ((lastBeginEvent != null) &&
							(lastBeginEvent.TransactionType==BEGIN_IDLE)) {
						timeline.add(TE=
							new TimelineEvent(lastBeginEvent.Time-BeginTime,
									LE.Time-BeginTime,
									-1, -1));
					}
					return;
				default:
					// some other event. If there is a lastBeginEvent, it
					// must straddle the time range, BUT we will not know
					// the actual end time. If not, the empty timeline is
					// returned.
					if (lastBeginEvent != null) {
						switch (lastBeginEvent.TransactionType) {
						case BEGIN_PROCESSING:
							timeline.add(TE=
								new TimelineEvent(lastBeginEvent.Time-BeginTime,
										End-BeginTime,
										lastBeginEvent.Entry,
										lastBeginEvent.Pe));
							break;
						case BEGIN_IDLE:
							timeline.add(TE=
								new TimelineEvent(lastBeginEvent.Time-BeginTime,
										End-BeginTime,
										-1, -1));
							break;
						}
					} else {
						System.out.println("finished empty timeline for " +
								pe);
					}
				return;
				}
			}
			//Throws EndOfLogException at end of file; break if past endTime
			CallStackManager cstack = new CallStackManager();
			LogEntry enclosingDummy = null;
			ObjectId tid = null;
			AmpiFunctionData ampiData = null;
			while(true) {
				if (LE.Entry != -1) {
					switch (LE.TransactionType) {
					case BEGIN_FUNC:
						lastBeginEvent = null;
						// Phase 1: Check stack for preceeding functions.
						//          If one is found, we need to "terminate"
						//          the timeline event associated with it.
						// **CW** Right now, there is an unavoidable bug
						// that enclosingDummy could be empty.

						// end previous function's (or Dummy Thread EP's)
						// timeline event.
						if (TE != null) {
							TE.EndTime = LE.Time - BeginTime;
							// If the entry was not long enough, remove it from the timeline
							if(TE.EndTime - TE.BeginTime < minEntryDuration){
								timeline.removeLast();
							}
							
						}

						// Phase 2: Handle current function. Note that the
						//          Function's messaging properties need
						//          to be suppressed.
						TE = new TimelineEvent();
						TE.isFunction = true;
						TE.BeginTime = LE.Time-BeginTime;
						TE.EntryPoint = LE.FunctionID;
						TE.EventID = -1; // no source.
						TE.id = enclosingDummy.id;
						cstack.push(LE.ampiData, TE.id.id[0], 
								TE.id.id[1], TE.id.id[2]);
						TE.callStack = 
							cstack.getStack(TE.id.id[0], TE.id.id[1], 
									TE.id.id[2]);
						timeline.add(TE);
						break;
					case END_FUNC:
						// Phase 1: End current function.
						if (TE != null) {
							TE.EndTime = LE.Time - BeginTime;
							cstack.pop(TE.id.id[0], TE.id.id[1], TE.id.id[2]);
							
							// If the entry was not long enough, remove it from the timeline
							if(TE.EndTime - TE.BeginTime < minEntryDuration){
								timeline.removeLast();
							}
							
						}
						TE = null;

						// Phase 2: "create" a new Begin for any previous
						//          functions or the dummy thread ep that
						//          is supposed to enclose it.
						tid = enclosingDummy.id;
						ampiData =
							(AmpiFunctionData)cstack.read(tid.id[0],
									tid.id[1],
									tid.id[2]);
						// Dealing with dummy thread ep
						if ((ampiData == null) ||
								(ampiData.FunctionID == 0)) {
							TE = new TimelineEvent(LE.Time-BeginTime,
									LE.Time-BeginTime,
									enclosingDummy.Entry, 
									enclosingDummy.Pe,
									enclosingDummy.MsgLen, 
									enclosingDummy.recvTime, 
									enclosingDummy.id,
									-1, // EventID no source.
									enclosingDummy.cpuBegin, 
									enclosingDummy.cpuEnd,
									enclosingDummy.numPapiCounts,
									enclosingDummy.papiCounts);
								timeline.add(TE);
						} else {
							// "create" previous function on stack.
							TE = new TimelineEvent();
							TE.isFunction = true;
							TE.BeginTime = LE.Time-BeginTime;
							TE.EntryPoint = ampiData.FunctionID;
							TE.EventID = -1; // no source.
							TE.id = tid;
							TE.callStack =
								cstack.getStack(TE.id.id[0], TE.id.id[1],
										TE.id.id[2]);
							timeline.add(TE);
						}
						break;
					case BEGIN_PROCESSING:
						
						lastBeginEvent = null;
						if (isProcessing) {
							// We add a "pretend" end event to accomodate
							// the prior begin processing event.
							if (TE != null) {
								TE.EndTime = LE.Time - BeginTime;
								// If the entry was not long enough, remove it from the timeline
								if(TE.EndTime - TE.BeginTime < minEntryDuration){
									timeline.removeLast();
								}
							}
							TE = null;
						}
						isProcessing = true;

						// Handle Dummy Thread EPs to see if we need to
						// resume a function entry.
						if (ampiTraceOn && (LE.Entry == 0)) {
							enclosingDummy = LE;
							tid = enclosingDummy.id;
							ampiData =
								(AmpiFunctionData)cstack.read(tid.id[0],
										tid.id[1],
										tid.id[2]);
							// only handle if there's a function. Otherwise
							// treat as normal Dummy Thread EP.
							if ((ampiData != null) &&
									(ampiData.FunctionID != 0)) {
								// "create" last function on stack. Note
								// that the enclosing dummy thread ep's
								// messaging properties need to be transfered
								// to the function's timeline event.
								TE = new TimelineEvent();
								TE.isFunction = true;
								TE.BeginTime = LE.Time-BeginTime;
								TE.EntryPoint = ampiData.FunctionID;
								TE.SrcPe = enclosingDummy.Pe;
								TE.EventID = enclosingDummy.EventID;
								TE.MsgLen = enclosingDummy.MsgLen;
								TE.RecvTime = enclosingDummy.recvTime;
								TE.id = tid;
								TE.callStack =
									cstack.getStack(TE.id.id[0], TE.id.id[1],
											TE.id.id[2]);
								timeline.add(TE);
								break;
							}
						}

						// Normal case of handling EPs
						TE = new TimelineEvent(LE.Time-BeginTime, 
								LE.Time-BeginTime,
								LE.Entry, LE.Pe,
								LE.MsgLen, LE.recvTime, 
								LE.id,LE.EventID,
								LE.cpuBegin, LE.cpuEnd,
								LE.numPapiCounts,
								LE.papiCounts);
						timeline.add(TE);
						lastBeginTimelineEvent = TE;
						break;
					case END_PROCESSING:
						// see if this is the first END_PROCESSING event
						// after the start time. If so, lastBeginEvent
						// is the matching pair.
						if ((lastBeginEvent != null) &&
								(lastBeginEvent.TransactionType==BEGIN_PROCESSING) &&
								(lastBeginEvent.Entry == LE.Entry)) {

							TE = new TimelineEvent(lastBeginEvent.Time-BeginTime,
									LE.Time-BeginTime,
									lastBeginEvent.Entry,
									lastBeginEvent.Pe);

							if(LE.Time - lastBeginEvent.Time >= minEntryDuration){
								// Just don't add this event if it is too small. We need to create the event because other following entries might refer to it???
								timeline.add(TE);
							}
							TE = null;
							isProcessing = false;
							break;
						}
						lastBeginEvent = null;
						// Handle Dummy Thread EPs to see if we need
						// to close off a currently operating function.
						if (ampiTraceOn && (LE.Entry == 0)) {
							tid = enclosingDummy.id;
							ampiData =
								(AmpiFunctionData)cstack.read(tid.id[0],
										tid.id[1],
										tid.id[2]);
							// only handle if there's a function. Otherwise
							// treat as normal Dummy Thread EP.
							if ((ampiData != null) &&
									(ampiData.FunctionID != 0)) {
								// end previous function's timeline event.
								if (TE != null) {
									TE.EndTime = LE.Time - BeginTime;
									// If the entry was not long enough, remove it from the timeline
									if(TE.EndTime - TE.BeginTime < minEntryDuration){
										timeline.removeLast();
									}
								}
								TE = null;
								enclosingDummy = null;
								break;
							}
						}

						// Normal case of handling EPs
						if (TE != null) {
							TE.EndTime = LE.Time - BeginTime;
							TE.cpuEnd = LE.cpuEnd;
							// If the entry was not long enough, remove it from the timeline
							if(TE.EndTime - TE.BeginTime < minEntryDuration){
								timeline.removeLast();
							}
							for (int i=0; i<LE.numPapiCounts; i++) {
								TE.papiCounts[i] = LE.papiCounts[i] -
								TE.papiCounts[i];
							}
							TE.compactLists();
						}
						TE = null;
						isProcessing = false;
						break;
					
					case USER_SUPPLIED:
						// Tag the last begin TimelineEvent with the user supplied value(likely a timestep number)
						if(LE.userSuppliedValue() != null && lastBeginTimelineEvent!=null)
							lastBeginTimelineEvent.UserSpecifiedData = LE.userSuppliedValue();
						break;
					case USER_SUPPLIED_BRACKETED_NOTE:
						UserEventObject note2 = new UserEventObject(pe, LE.Time-BeginTime, LE.Entry, LE.EventID, UserEventObject.Type.PAIR, LE.note);
						note2.beginTime = LE.Time;
						note2.endTime = LE.endTime;
						userEventVector.add(note2);
						break;
					case MEMORY_USAGE:
						if(LE.memoryUsage() != 0 && lastBeginTimelineEvent!=null)
							lastBeginTimelineEvent.memoryUsage = LE.memoryUsage();
						break;
						
					
					case CREATION:
						// see if this is the first CREATION event after
						// the start of time range. If so, create a block
						// based on the lastBeginEvent and attach the CREATION
						// event to that block.
						if ((lastBeginEvent != null) &&
								(lastBeginEvent.TransactionType==BEGIN_PROCESSING)) {
							TE = new TimelineEvent(lastBeginEvent.Time-BeginTime,
									End-BeginTime,
									lastBeginEvent.Entry,
									lastBeginEvent.Pe);
							timeline.add(TE);
							isProcessing = true;
						}
						lastBeginEvent = null;
						tempte = false;
						//Start a new dummy event
						if (TE == null) { 
							TE = new TimelineEvent(LE.Time-BeginTime,
									LE.Time-BeginTime,
									-2,LE.Pe,LE.MsgLen);
							timeline.add(TE);
							tempte = true;
						}
						TM = new TimelineMessage(pe, LE.Time - BeginTime,
								LE.Entry, LE.MsgLen,
								LE.EventID);
						TE.addMessage(TM);
						if (tempte) {
							TE = null;
						}
						break;
					case CREATION_BCAST:
						// see if this is the first CREATION_BCAST event 
						// after the start of time range. If so, create a 
						// block based on the lastBeginEvent and attach 
						// the CREATION event to that block.
						if ((lastBeginEvent != null) &&
								(lastBeginEvent.TransactionType ==
									BEGIN_PROCESSING)) {
							TE = new TimelineEvent(lastBeginEvent.Time -
									BeginTime,
									End - BeginTime,
									lastBeginEvent.Entry,
									lastBeginEvent.Pe);
							timeline.add(TE);
							isProcessing = true;
						}
						lastBeginEvent = null;
						tempte = false;
						if (TE == null) {
							TE = new TimelineEvent(LE.Time - BeginTime,
									LE.Time - BeginTime,
									-2, LE.Pe, LE.MsgLen);
							timeline.add(TE);
							tempte = true;
						}
						TM = new TimelineMessage(pe, LE.Time - BeginTime,
								LE.Entry, LE.MsgLen,
								LE.EventID, LE.numPEs);
						TE.addMessage(TM);
						if (tempte) {
							TE = null;
						}
						break;
					case CREATION_MULTICAST:
						// see if this is the first CREATION_MULTICAST event 
						// after
						// the start of time range. If so, create a block
						// based on the lastBeginEvent and attach the CREATION
						// event to that block.
						if ((lastBeginEvent != null) &&
								(lastBeginEvent.TransactionType==BEGIN_PROCESSING)) {
							TE = new TimelineEvent(lastBeginEvent.Time-BeginTime,
									End-BeginTime,
									lastBeginEvent.Entry,
									lastBeginEvent.Pe);
							timeline.add(TE);
							isProcessing = true;
						}
						lastBeginEvent = null;
						tempte = false;
						if (TE == null) {
							TE = new TimelineEvent(LE.Time-BeginTime,
									LE.Time-BeginTime,
									-2, LE.Pe, LE.MsgLen);
							timeline.add(TE);
							tempte = true;
						}
						TM = new TimelineMessage(pe, LE.Time - BeginTime,
								LE.Entry, LE.MsgLen,
								LE.EventID, LE.destPEs);
						TE.addMessage(TM);
						if (tempte) {
							TE = null;
						}
						break;
					case USER_EVENT:
						// don't mess with TE, that's just for EPs
						UserEventObject event = new UserEventObject(pe, LE.Time-BeginTime, LE.Entry, LE.EventID, UserEventObject.Type.SINGLE);
						userEventVector.add(event);
						break;
					case USER_SUPPLIED_NOTE:
						UserEventObject note = new UserEventObject(pe, LE.Time-BeginTime, LE.note);
						userEventVector.add(note);
						break;
					case USER_EVENT_PAIR:
						// **CW** UserEventPairs come in a two-line block
						// because of the way the tracing code is currently
						// written.
						userEventObject = new UserEventObject(pe, LE.Time-BeginTime,
								LE.Entry, LE.EventID,
								UserEventObject.Type.PAIR, LE.nestedID);
						// assume the end time to be the end of range
						// in case the ending userevent gets cut off.
						userEventObject.endTime = End;

						// Now, expect to read the second entry and handle
						// errors if necessary.
						LogEntryData data2 = reader.nextEvent();
						LE = new LogEntry(data2);

						if (LE.TransactionType != USER_EVENT_PAIR) {
							// DANGLING - throw away the old event
							// just pass the read data to the next
							// loop iteration.
							userEventObject = null;
							continue;
						}
						// MISMATCHED EVENT PAIRS - again, nullify
						// the first read event and pass the newly
						// read entry back through the loop
						if (userEventObject.charmEventID != LE.EventID || 
								userEventObject.userEventID != LE.Entry) {
							userEventObject = null;
							continue;
						} else {

							userEventObject.endTime = LE.Time-BeginTime;
							userEventVector.add(userEventObject);
							if(!timeline.isEmpty()) {
								//If the log is loaded somewhere in the middle where
								//user event happens before a timeline event, then the
								//timeline vector would be empty
								TimelineEvent curLastOne = timeline.getLast();
								long tleBeginTime = curLastOne.BeginTime;
								//System.out.println("TLE's begin: "+tleBeginTime+" user's begin: "+userEvent.BeginTime);
								if(tleBeginTime <= userEventObject.beginTime && 
										userEventObject.beginTime - tleBeginTime<= TimelineEvent.USEREVENTMAXGAP){
									curLastOne.userEventName = userEventObject.getName();
								}
								//System.out.println("Encountering user name: "+userEvent.Name);

							}
						}
						break;
					case BEGIN_USER_EVENT_PAIR:
						final UserEventObject temp = new UserEventObject(pe, LE.Time - BeginTime,
								LE.Entry, LE.EventID,
								UserEventObject.Type.PAIR, LE.nestedID);
						userEventPairStarts.add(temp);
						break;
					case END_USER_EVENT_PAIR:
						for (int i = 0; i < userEventPairStarts.size(); ++i) {
							final UserEventObject candidate = userEventPairStarts.get(i);
							if (candidate.userEventID == LE.Entry &&
									candidate.getNestedID() == LE.nestedID &&
									candidate.beginTime <= LE.Time - BeginTime) {
								userEventPairStarts.remove(i);
								candidate.endTime = LE.Time - BeginTime;
								userEventVector.add(candidate);
								if (!timeline.isEmpty()) {
									TimelineEvent curLastOne = timeline.getLast();
									long tleBeginTime = curLastOne.BeginTime;
									if (tleBeginTime <= candidate.beginTime &&
											candidate.beginTime - tleBeginTime <= TimelineEvent.USEREVENTMAXGAP) {
										curLastOne.userEventName = candidate.getName();
									}
								}
								break;
							}
						}
						break;
					case BEGIN_PACK:
						// see if this is the first BEGIN_PACK event 
						// after
						// the start of time range. If so, create a block
						// based on the lastBeginEvent and attach the PACK
						// event to that block.
						if ((lastBeginEvent != null) &&
								(lastBeginEvent.TransactionType==BEGIN_PROCESSING)) {
							TE = new TimelineEvent(lastBeginEvent.Time-BeginTime,
									End-BeginTime,
									lastBeginEvent.Entry,
									lastBeginEvent.Pe);
							timeline.add(TE);
							isProcessing = true;
						}
						lastBeginEvent = null;
						// Start a new dummy event
						if (TE == null) {
							TE = new TimelineEvent(LE.Time-BeginTime,
									LE.Time-BeginTime,-2,
									LE.Pe);
							timeline.add(TE);
						}
						TE.addPack (PT=new PackTime(LE.Time-BeginTime));
						break;
					case END_PACK:
						if (PT!=null) {
							PT.EndTime = LE.Time-BeginTime;
						}
						PT=null;
						if (TE != null) {
							if (TE.EntryPoint == -2) {
								TE=null;
							}
						}
						break;
					case BEGIN_IDLE:
						lastBeginEvent = null;
						if (MainWindow.IGNORE_IDLE) {
							break;
						}
						TE = new TimelineEvent(LE.Time - BeginTime,
								Long.MAX_VALUE,
								-1,-1); 
						timeline.add(TE);
						break;
					case END_IDLE:
						if (MainWindow.IGNORE_IDLE) {
							break;
						}
						// see if this is the first CREATION_MULTICAST event 
						// after
						// the start of time range. If so, create a block
						// based on the lastBeginEvent and attach the CREATION
						// event to that block.
						if ((lastBeginEvent != null) &&
								(lastBeginEvent.TransactionType == BEGIN_IDLE)) {
							TE = new TimelineEvent(lastBeginEvent.Time-BeginTime,
									End-BeginTime,
									-1, -1);
							timeline.add(TE);
							isProcessing = true;
						}
						lastBeginEvent = null;
						if (TE != null) {   
							TE.EndTime = LE.Time - BeginTime;
							// If the entry was not long enough, remove it from the timeline
							if(TE.EndTime - TE.BeginTime < minEntryDuration){
								timeline.removeLast();
							}
						}
						TE=null;
						break;
					}
				}
				LogEntryData data2 = reader.nextEvent();
				LE = new LogEntry(data2);
				// this will
				// END COMPUTATION event.
				if (LE.Entry != -1) {
					if ((LE.Time - BeginTime) > End) {
						break;
					}
				}
			}

			// check to see if we are stopping in the middle of a message.
			// if so, we need to keep reading to get its end time
			while (TE != null) {
				if (LE.Entry != -1) {
					if (LE.TransactionType == END_PROCESSING) {
						TE.EndTime = LE.Time - BeginTime;
						// If the entry was not long enough, remove it from the timeline
						if(TE.EndTime - TE.BeginTime < minEntryDuration){
							timeline.removeLast();
						}
						TE=null;
					}
				}
				LogEntryData data2 = reader.nextEvent();
				LE = new LogEntry(data2);
			}
		} catch (EndOfLogSuccess e) { 
			/* Reached end of the log file */ 
		} catch (IOException E) {
			throw new LogLoadException(MainWindow.runObject[myRun].getLogName(pe));
		}
		
		
		try {
			reader.close();
		} catch (IOException e1) {
			System.err.println("Error: could not close log file reader for processor " + pe );
		}		
		
		
	}
	
}
