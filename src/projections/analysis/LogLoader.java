package projections.analysis;


import java.io.IOException;
import java.util.*;

import projections.Tools.Timeline.TimelineMessage;
import projections.Tools.Timeline.UserEventObject;
import projections.gui.MainWindow;
import projections.misc.LogEntry;
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
			TimelineEvent lastBeginTimelineEvent = null;

			// Seek to time Begin
			LE = reader.nextEventOnOrAfter(Begin);
			LogEntry lastBeginEvent = reader.getLastOpenBE();
			
			if (LE.time > End) {
				switch (LE.type) {
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
							(lastBeginEvent.type ==BEGIN_PROCESSING) &&
							(lastBeginEvent.entry == LE.entry)) {
						timeline.add(TE=
							new TimelineEvent(lastBeginEvent.time,
									LE.time,
									lastBeginEvent.entry,
									lastBeginEvent.pe));
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
							(lastBeginEvent.type ==BEGIN_IDLE)) {
						timeline.add(TE=
							new TimelineEvent(lastBeginEvent.time,
									LE.time,
									Analysis.IDLE_ENTRY_POINT, -1));
					}
					return;
				default:
					// some other event. If there is a lastBeginEvent, it
					// must straddle the time range, BUT we will not know
					// the actual end time. If not, the empty timeline is
					// returned.
					if (lastBeginEvent != null) {
						switch (lastBeginEvent.type) {
						case BEGIN_PROCESSING:
							timeline.add(TE=
								new TimelineEvent(lastBeginEvent.time,
										End,
										lastBeginEvent.entry,
										lastBeginEvent.pe));
							break;
						case BEGIN_IDLE:
							timeline.add(TE=
								new TimelineEvent(lastBeginEvent.time,
										End,
										Analysis.IDLE_ENTRY_POINT, -1));
							break;
						}
					} else {
						System.out.println("finished empty timeline for " +
								pe);
					}
				return;
				}
			}
			// Throws EndOfLogException at end of file; break if past endTime
			// Note that the LE object is reused, filled with new data every iteration.
			// Ensure that stale data from a past read for another type is not used.
			CallStackManager cstack = new CallStackManager();
			ObjectId tid = null;
			while(true) {
				if (LE.entry != Analysis.IDLE_ENTRY_POINT) {
					switch (LE.type) {
					case BEGIN_PROCESSING:
						
						lastBeginEvent = null;
						if (isProcessing) {
							// We add a "pretend" end event to accomodate
							// the prior begin processing event.
							if (TE != null) {
								TE.EndTime = LE.time;
								// If the entry was not long enough, remove it from the timeline
								if(TE.EndTime - TE.BeginTime < minEntryDuration){
									timeline.removeLast();
								}
							}
							TE = null;
						}
						isProcessing = true;

						TE = new TimelineEvent(LE.time,
								LE.time,
								LE.entry, LE.pe,
								LE.msglen, LE.recvTime,
								LE.id,LE.event,
								LE.cpuStartTime, LE.cpuEndTime,
								LE.numPerfCounts,
								LE.perfCounts);
						timeline.add(TE);
						lastBeginTimelineEvent = TE;
						break;
					case END_PROCESSING:
						// see if this is the first END_PROCESSING event
						// after the start time. If so, lastBeginEvent
						// is the matching pair.
						if ((lastBeginEvent != null) &&
								(lastBeginEvent.type ==BEGIN_PROCESSING) &&
								(lastBeginEvent.entry == LE.entry)) {

							TE = new TimelineEvent(lastBeginEvent.time,
									LE.time,
									lastBeginEvent.entry,
									lastBeginEvent.pe);

							if(LE.time - lastBeginEvent.time >= minEntryDuration){
								// Just don't add this event if it is too small. We need to create the event because other following entries might refer to it???
								timeline.add(TE);
							}
							TE = null;
							isProcessing = false;
							break;
						}
						lastBeginEvent = null;
						
						if (TE != null) {
							TE.EndTime = LE.time;
							TE.cpuEnd = LE.cpuEndTime;
							// If the entry was not long enough, remove it from the timeline
							if(TE.EndTime - TE.BeginTime < minEntryDuration){
								timeline.removeLast();
							}
							for (int i = 0; i<LE.numPerfCounts; i++) {
								TE.papiCounts[i] = LE.perfCounts[i] -
								TE.papiCounts[i];
							}
							TE.compactLists();
						}
						TE = null;
						isProcessing = false;
						break;
					
					case USER_SUPPLIED:
						// Tag the last begin TimelineEvent with the user supplied value(likely a timestep number)
						if(LE.userSupplied != null && lastBeginTimelineEvent!=null)
							lastBeginTimelineEvent.UserSpecifiedData = LE.userSupplied;
						break;
					case USER_SUPPLIED_BRACKETED_NOTE:
						UserEventObject note2 = new UserEventObject(pe, LE.time, LE.entry, LE.event, UserEventObject.Type.PAIR, LE.note);
						note2.beginTime = LE.time;
						note2.endTime = LE.endTime;
						userEventVector.add(note2);
						break;
					case MEMORY_USAGE:
						if(LE.memoryUsage != 0 && lastBeginTimelineEvent!=null)
							lastBeginTimelineEvent.memoryUsage = LE.memoryUsage;
						break;
						
					
					case CREATION:
						// see if this is the first CREATION event after
						// the start of time range. If so, create a block
						// based on the lastBeginEvent and attach the CREATION
						// event to that block.
						if ((lastBeginEvent != null) &&
								(lastBeginEvent.type ==BEGIN_PROCESSING)) {
							TE = new TimelineEvent(lastBeginEvent.time,
									End,
									lastBeginEvent.entry,
									lastBeginEvent.pe);
							timeline.add(TE);
							isProcessing = true;
						}
						lastBeginEvent = null;
						tempte = false;
						//Start a new dummy event
						if (TE == null) { 
							TE = new TimelineEvent(LE.time,
									LE.time,
									Analysis.OVERHEAD_ENTRY_POINT,LE.pe,LE.msglen);
							timeline.add(TE);
							tempte = true;
						}
						TM = new TimelineMessage(pe, LE.time,
								LE.entry, LE.msglen,
								LE.event);
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
								(lastBeginEvent.type ==
									BEGIN_PROCESSING)) {
							TE = new TimelineEvent(lastBeginEvent.time,
									End,
									lastBeginEvent.entry,
									lastBeginEvent.pe);
							timeline.add(TE);
							isProcessing = true;
						}
						lastBeginEvent = null;
						tempte = false;
						if (TE == null) {
							TE = new TimelineEvent(LE.time,
									LE.time,
									Analysis.OVERHEAD_ENTRY_POINT, LE.pe, LE.msglen);
							timeline.add(TE);
							tempte = true;
						}
						TM = new TimelineMessage(pe, LE.time,
								LE.entry, LE.msglen,
								LE.event, LE.numPEs);
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
								(lastBeginEvent.type ==BEGIN_PROCESSING)) {
							TE = new TimelineEvent(lastBeginEvent.time,
									End,
									lastBeginEvent.entry,
									lastBeginEvent.pe);
							timeline.add(TE);
							isProcessing = true;
						}
						lastBeginEvent = null;
						tempte = false;
						if (TE == null) {
							TE = new TimelineEvent(LE.time,
									LE.time,
									Analysis.OVERHEAD_ENTRY_POINT, LE.pe, LE.msglen);
							timeline.add(TE);
							tempte = true;
						}
						TM = new TimelineMessage(pe, LE.time,
								LE.entry, LE.msglen,
								LE.event, LE.destPEs);
						TE.addMessage(TM);
						if (tempte) {
							TE = null;
						}
						break;
					case USER_EVENT:
						// don't mess with TE, that's just for EPs
						UserEventObject event = new UserEventObject(pe, LE.time, LE.entry, LE.event, UserEventObject.Type.SINGLE);
						userEventVector.add(event);
						break;
					case USER_SUPPLIED_NOTE:
						UserEventObject note = new UserEventObject(pe, LE.time, LE.note);
						userEventVector.add(note);
						break;
					case USER_EVENT_PAIR:
						// **CW** UserEventPairs come in a two-line block
						// because of the way the tracing code is currently
						// written.
						userEventObject = new UserEventObject(pe, LE.time,
								LE.entry, LE.event,
								UserEventObject.Type.PAIR, LE.nestedID);
						// assume the end time to be the end of range
						// in case the ending userevent gets cut off.
						userEventObject.endTime = End;

						// Now, expect to read the second entry and handle
						// errors if necessary.
						LE = reader.nextEvent(LE);

						if (LE.type != USER_EVENT_PAIR) {
							// DANGLING - throw away the old event
							// just pass the read data to the next
							// loop iteration.
							userEventObject = null;
							continue;
						}
						// MISMATCHED EVENT PAIRS - again, nullify
						// the first read event and pass the newly
						// read entry back through the loop
						if (userEventObject.charmEventID != LE.event ||
								userEventObject.userEventID != LE.entry) {
							userEventObject = null;
							continue;
						} else {

							userEventObject.endTime = LE.time ;
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
						final UserEventObject temp = new UserEventObject(pe, LE.time,
								LE.entry, LE.event,
								UserEventObject.Type.PAIR, LE.nestedID);
						userEventPairStarts.add(temp);
						break;
					case END_USER_EVENT_PAIR:
						for (int i = 0; i < userEventPairStarts.size(); ++i) {
							final UserEventObject candidate = userEventPairStarts.get(i);
							if (candidate.userEventID == LE.entry &&
									candidate.getNestedID() == LE.nestedID &&
									candidate.beginTime <= LE.time) {
								userEventPairStarts.remove(i);
								candidate.endTime = LE.time;
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
								(lastBeginEvent.type ==BEGIN_PROCESSING)) {
							TE = new TimelineEvent(lastBeginEvent.time,
									End,
									lastBeginEvent.entry,
									lastBeginEvent.pe);
							timeline.add(TE);
							isProcessing = true;
						}
						lastBeginEvent = null;
						// Start a new dummy event
						if (TE == null) {
							TE = new TimelineEvent(LE.time,
									LE.time,Analysis.OVERHEAD_ENTRY_POINT,
									LE.pe);
							timeline.add(TE);
						}
						TE.addPack (PT=new PackTime(LE.time));
						break;
					case END_PACK:
						if (PT!=null) {
							PT.EndTime = LE.time;
						}
						PT=null;
						if (TE != null) {
							if (TE.EntryPoint == Analysis.OVERHEAD_ENTRY_POINT) {
								TE=null;
							}
						}
						break;
					case BEGIN_IDLE:
						lastBeginEvent = null;
						if (MainWindow.IGNORE_IDLE) {
							break;
						}
						TE = new TimelineEvent(LE.time,
								Long.MAX_VALUE,
								Analysis.IDLE_ENTRY_POINT,-1); 
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
								(lastBeginEvent.type == BEGIN_IDLE)) {
							TE = new TimelineEvent(lastBeginEvent.time,
									End,
									Analysis.IDLE_ENTRY_POINT, -1);
							timeline.add(TE);
							isProcessing = true;
						}
						lastBeginEvent = null;
						if (TE != null) {   
							TE.EndTime = LE.time;
							// If the entry was not long enough, remove it from the timeline
							if(TE.EndTime - TE.BeginTime < minEntryDuration){
								timeline.removeLast();
							}
						}
						TE=null;
						break;
					}
				}
				LE = reader.nextEvent(LE);
				// this will
				// END COMPUTATION event.
				if (LE.entry != Analysis.IDLE_ENTRY_POINT) {
					if (LE.time > End) {
						break;
					}
				}
			}

			// check to see if we are stopping in the middle of a message.
			// if so, we need to keep reading to get its end time
			while (TE != null) {
				if (LE.entry != Analysis.IDLE_ENTRY_POINT) {
					if (LE.type == END_PROCESSING) {
						TE.EndTime = LE.time;
						// If the entry was not long enough, remove it from the timeline
						if(TE.EndTime - TE.BeginTime < minEntryDuration){
							timeline.removeLast();
						}
						TE=null;
					}
				}
				LE = reader.nextEvent();
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
