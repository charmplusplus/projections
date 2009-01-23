package projections.analysis;

import projections.gui.*;
import projections.misc.*;

import java.io.*;

/**
 *  Written by Chee Wai Lee  4/12/2002
 *  Rewritten by Isaac 12/17/2008
 *  
 *  GenericLogReader reads a log file and returns entry data one at a time.
 *  
 *  The general contract is that the caller supplies the Reader stream on
 *  object creation time (which means the input can actually come from a
 *  networked stream instead of from just a file).
 *  
 *   
 *
 */

public class GenericLogReader extends ProjectionsReader
implements PointCapableReader
{
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	static int myRun = 0;

	private double version;

	private BufferedReader reader;
	
	// Technically, lastRecordedTime is not required, but because this
	// class cannot control what client modules do to the "data" object
	// passed in, it is much safer to record the lastRecordedTime locally.
	//
	private long lastRecordedTime = 0;

	// Book-keeping data. Used for consistency when event-blocks happen
	// to straddle user-specified time-boundaries.
	//
	private LogEntryData lastBeginEvent = null;

	private boolean endComputationOccurred;

	public GenericLogReader(String filename, double Nversion) {
		super(filename, String.valueOf(Nversion));
		lastBeginEvent = new LogEntryData();
		lastBeginEvent.setValid(false);
		endComputationOccurred = false;
		try {
			reader = new BufferedReader(new FileReader(filename));
			version = Nversion;
			reader.readLine(); // skip over the header (already read)
		} catch (IOException e) {
			System.err.println("Error reading file " + filename);
		}
	}

	public GenericLogReader(int peNum, double Nversion) {
		super(MainWindow.runObject[myRun].getLogName(peNum), String.valueOf(Nversion));
		lastBeginEvent = new LogEntryData();
		lastBeginEvent.setValid(false);
		endComputationOccurred = false;
		try {
			reader = new BufferedReader(new FileReader(sourceString));
			version = Nversion;
			reader.readLine(); // skip over the header (already read)
		} catch (IOException e) {
			System.err.println("Error reading file " + sourceString);
		}
	}

	protected boolean checkAvailable() {
		File sourceFile = new File(sourceString);
		return sourceFile.canRead();
	}

	public void readStaticData() {
		// do nothing for now (since the original code ignored the header)
		// **CW** must change later (for versioning control) of course ...
	}


	/** 
	 * Create a new LogEntryData by reading/parsing the next line from the log 
	 * 
	 * Upon reaching the end of the file, a fake END_COMPUTATION event will be produced if none was found in the log file.
	 * After the end of the file is reached and after some END_COMPUTATION has been returned, an EOFException will be thrown
	 * 
	 * If any problem is detected when reading within a line, an IOException is produced
	 * 
	 * */
	public LogEntryData nextEvent() 
	throws IOException, EOFException
	{
		LogEntryData data = new LogEntryData();

		String line = reader.readLine();
		AsciiLineParser parser = new AsciiLineParser(line);

		// If actually at end of file
		if(line == null){
			// Generate a fake END_COMPUTATION if no legitimate one was found
			// Otherwise, signal that we have reached end of file by throwing an EOFException
			if (! endComputationOccurred){
				// Fake an END_COMPUTATION event. This should *NEVER* be
				// silent!!! A Warning *MUST* be sounded.
				// This is to deal with partial truncated projections logs.
				endComputationOccurred = true;
				data.type = END_COMPUTATION;
				data.time = lastRecordedTime;
				System.err.println("[" + sourceString + "] WARNING: Partial or Corrupted Projections log. Faked END_COMPUTATION entry added for last recorded time of " +	data.time);
			} else {
				throw new EOFException();
			}
		}		


		data.type = parser.nextInt();
		switch (data.type) {
		case BEGIN_IDLE:
			lastBeginEvent.time = data.time = parser.nextLong();
			lastBeginEvent.pe = data.pe = parser.nextInt();
			lastBeginEvent.setValid(true);
			break;
		case END_IDLE: 
			data.time = parser.nextLong();
			data.pe = parser.nextInt();
			lastBeginEvent.setValid(false);
			break;
		case BEGIN_PACK: case END_PACK:
		case BEGIN_UNPACK: case END_UNPACK:
			data.time = parser.nextLong();
			data.pe = parser.nextInt();
			break;
		case USER_SUPPLIED:
			data.userSupplied = new Integer(parser.nextInt());
			break;
		case USER_SUPPLIED_NOTE:
			data.time = new Integer(parser.nextInt());
			Integer strlen = new Integer(parser.nextInt());
			data.note = new String(parser.restOfLine());
			break;
		case MEMORY_USAGE:
			data.memoryUsage = new Integer(parser.nextInt());
			break;
		case CREATION:
			data.mtype = parser.nextInt();
			data.entry = parser.nextInt();
			data.time = parser.nextLong();
			data.event = parser.nextInt();
			data.pe = parser.nextInt();
			if (version >= 2.0) {
				data.msglen = parser.nextInt();
			} else {
				data.msglen = -1;
			}
			if (version >= 5.0) {
				data.sendTime = parser.nextLong();
			}
			break;
		case CREATION_BCAST:
			data.mtype = parser.nextInt();
			data.entry = parser.nextInt();
			data.time = parser.nextLong();
			data.event = parser.nextInt();
			data.pe = parser.nextInt();
			if (version >= 2.0) {
				data.msglen = parser.nextInt();
			} else {
				data.msglen = -1;
			}
			if (version >= 5.0) {
				data.sendTime = parser.nextLong();
			}
			data.numPEs = parser.nextInt();
			break;
		case CREATION_MULTICAST:
			data.mtype = parser.nextInt();
			data.entry = parser.nextInt();
			data.time = parser.nextLong();
			data.event = parser.nextInt();
			data.pe = parser.nextInt();
			if (version >= 2.0) {
				data.msglen = parser.nextInt();
			} else {
				data.msglen = -1;
			}
			if (version >= 5.0) {
				data.sendTime = parser.nextLong();
			}
			data.numPEs = parser.nextInt();
			data.destPEs = new int[data.numPEs];
			for (int i=0;i<data.numPEs;i++) {
				data.destPEs[i] = parser.nextInt();
			}
			break;
		case BEGIN_PROCESSING: 
			lastBeginEvent.mtype = data.mtype = parser.nextInt();
			lastBeginEvent.entry = data.entry = parser.nextInt();
			lastBeginEvent.time = data.time = parser.nextLong();
			lastBeginEvent.event = data.event = parser.nextInt();
			lastBeginEvent.pe = data.pe = parser.nextInt();
			if (version >= 2.0) {
				lastBeginEvent.msglen = data.msglen = parser.nextInt();
			} else {
				lastBeginEvent.msglen = data.msglen = -1;
			}
			if (version >= 4.0) {
				lastBeginEvent.recvTime = data.recvTime = 
					parser.nextLong();
				lastBeginEvent.id[0] = data.id[0] = parser.nextInt();
				lastBeginEvent.id[1] = data.id[1] = parser.nextInt();
				lastBeginEvent.id[2] = data.id[2] = parser.nextInt();
			}
			if (version >= 7.0) {
				lastBeginEvent.id[3] = data.id[3] = parser.nextInt();
			}
			if (version >= 6.5) {
				lastBeginEvent.cpuStartTime = data.cpuStartTime = 
					parser.nextLong();
			}
			if (version >= 6.6) {
				lastBeginEvent.numPerfCounts = data.numPerfCounts = 
					parser.nextInt();
				lastBeginEvent.perfCounts = new long[data.numPerfCounts];
				data.perfCounts = new long[data.numPerfCounts];
				for (int i=0; i<data.numPerfCounts; i++) {
					lastBeginEvent.perfCounts[i] = data.perfCounts[i] = 
						parser.nextLong();
				}
			}
			lastBeginEvent.setValid(true);
			break;
		case END_PROCESSING:
			data.mtype = parser.nextInt();
			data.entry = parser.nextInt();
			data.time = parser.nextLong();
			data.event = parser.nextInt();
			data.pe = parser.nextInt();
			if (version >= 2.0) {
				data.msglen = parser.nextInt();
			} else {
				data.msglen = -1;
			}
			if (version >= 6.5) {
				data.cpuEndTime = parser.nextLong();
			}
			if (version >= 6.6) {
				data.numPerfCounts = parser.nextInt();
				data.perfCounts = new long[data.numPerfCounts];
				for (int i=0; i<data.numPerfCounts; i++) {
					data.perfCounts[i] = parser.nextLong();
				}
			}
			break;
		case BEGIN_TRACE: 
			data.time = parser.nextLong();
			// invalidates the last Begin Event. BEGIN_TRACE happens
			// in the context of an entry method that is *not* traced.
			// Hence when a BEGIN_TRACE event is encountered, no
			// information is actually known about the entry method
			// context.
			lastBeginEvent.setValid(false);
			break;
		case END_TRACE:
			data.time = parser.nextLong();
			// END_TRACE happens in the context of an existing
			// entry method and hence should logically "end" it.
			// This means any client taking note of END_TRACE must
			// take into account lastBeginEvent in order to get
			// reasonable data.
			break;
		case BEGIN_FUNC:
			data.time = parser.nextLong();
			data.entry = parser.nextInt();
			data.lineNo = parser.nextInt();
			data.funcName = parser.restOfLine();
			break;
		case END_FUNC:
			data.time = parser.nextLong();
			data.entry = parser.nextInt();
			break;
		case MESSAGE_RECV:
			data.mtype = parser.nextInt();
			data.time = parser.nextLong();
			data.event = parser.nextInt();
			data.pe = parser.nextInt();
			data.msglen = parser.nextInt();
			break;
		case ENQUEUE: case DEQUEUE:
			data.mtype = parser.nextInt();
			data.time = parser.nextLong();
			data.event = parser.nextInt();
			data.pe = parser.nextInt();
			break;
		case BEGIN_INTERRUPT: case END_INTERRUPT:
			data.time = parser.nextLong();
			data.event = parser.nextInt();
			data.pe = parser.nextInt();
			break;
		case BEGIN_COMPUTATION:
			data.time = parser.nextLong();
			break;
		case END_COMPUTATION:
			data.time = parser.nextLong();
			endComputationOccurred = true;
			break;
		case USER_EVENT:
			data.userEventID = parser.nextInt();
			data.entry = data.userEventID; 
			data.time = parser.nextLong();
			data.event = parser.nextInt();
			data.pe = parser.nextInt();
			break;
		case USER_EVENT_PAIR:
			data.userEventID = parser.nextInt();
			data.entry = data.userEventID;
			data.time = parser.nextLong();
			data.event = parser.nextInt();
			data.pe = parser.nextInt();
			break;
		default:
			data.type = -1;
		break;

		}

		lastRecordedTime = data.time;

		return data;

	}

	/**
	 *
	 *  ***CURRENT IMP*** exponential search too hard, using sequential
	 *
	 *  eventOnOrAfter takes a timestamp and uses an exponential search 
	 *  to locate the event. The trouble is that a seek backwards requires 
	 *  resetting the stream.
	 *
	 *  It looks for recognized events. The current recognition scheme 
	 *  overlooks a lot of important events!
	 *
	 *  An EOFException indicates that no such event was found.
	 */
	public LogEntryData nextEventOnOrAfter(long timestamp) 
	throws IOException, EOFException
	{
		return seqLookForNextEventOnOrAfter(timestamp);
	}

	// More precisely, the next RECOGNIZED event
	private LogEntryData seqLookForNextEventOnOrAfter(long timestamp)
	throws IOException, EOFException
	{
		LogEntryData data = new LogEntryData();
		while (true) {
			data = nextEvent();
			// skip unrecognized tags
			while (data.type == -1) {
				data = nextEvent();
			}
			if (data.time >= timestamp) {
				// found!
				return data;
			}
		}
	}

	/**
	 *  nextEventOfType gets the next event of the eventType.
	 */
	public LogEntryData nextEventOfType(int eventType) 
	throws IOException, EOFException
	{
		LogEntryData data = new LogEntryData();
		while (true) {
			data = nextEvent();
			if (data.type == eventType) {
				return data;
			}
		}
	}

//	public LogEntryData nextEventOfTypeOnOrAfter(int eventType, long timestamp)
//	throws IOException, EOFException
//	{
//		LogEntryData data = new LogEntryData();
//
//		while (true) {
//			data = nextEventOnOrAfter(timestamp);
//			if (data.type == eventType) {
//				return data;
//			}
//		}
//	}

//	public LogEntryData nextBeginEvent()
//	throws IOException, EOFException
//	{
//		LogEntryData data = new LogEntryData();
//
//		while (true) {
//			data = nextEvent();
//			if (data.isBeginType()) {
//				return data;
//			}
//		}
//	}

//	public void nextEndEvent(LogEntryData data)
//	throws IOException, EOFException
//	{
//		while (true) {
//			data = nextEvent();
//			if (data.isEndType()) {
//				return;
//			}
//		}
//	}
//
//	public LogEntryData nextBeginEventOnOrAfter(long timestamp)
//	throws IOException, EOFException
//	{
//		LogEntryData data = new LogEntryData();
//
//		while (true) {
//			data = nextEventOnOrAfter(timestamp);
//			if (data.isBeginType()) {
//				return data;
//			}
//		}
//	}

//	public LogEntryData nextEndEventOnOrAfter(long timestamp)
//	throws IOException, EOFException
//	{
//		LogEntryData data = new LogEntryData();
//
//		while (true) {
//			data = nextEventOnOrAfter(timestamp);
//			if (data.isEndType()) {
//				return data;
//			}
//		}
//	}

	public LogEntryData getLastBE() {
		if (lastBeginEvent.isValid()) {
			return lastBeginEvent;
		}
		return null;
	}


	public void close()
	throws IOException
	{
		if (reader != null) {
			reader.close();
		}
	}

}
