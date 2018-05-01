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
			while(true) {
				if (LE.Entry != -1) {
					switch (LE.TransactionType) {
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
