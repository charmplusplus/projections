package projections.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.InputMismatchException;
import java.util.zip.GZIPInputStream;

import projections.gui.MainWindow;
import projections.analysis.StsReader;
import projections.misc.LogEntryData;

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
 */

public class GenericLogReader extends ProjectionsReader
implements PointCapableReader
{
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;
	
	/** How many bytes should be read at a time from the file. This should be big enough to keep the disks from thrashing. */
	private int bufferSize = 256*1024;
	
	private double version;

	private BufferedReader reader;
	
	/** Technically, lastRecordedTime is not required, but because this 
	 * class cannot control what client modules do to the "data" object 
	 * passed in, it is much safer to record the lastRecordedTime locally. 
	 */	
	private long lastRecordedTime = 0; 

	/** Book-keeping data. Used for consistency when event-blocks 
	 * happen to straddle user-specified time-boundaries. */
	private LogEntryData lastBeginEvent = null;

	private boolean endComputationOccurred;
	
	

	long shiftAmount = 0;

	/** Create a reader for the text log file or a compressed version of it ending in ".gz" */
	public GenericLogReader(int peNum, double Nversion) {
		this(peNum, Nversion, MainWindow.runObject[myRun]);
	}

	public GenericLogReader(int peNum, double Nversion, Analysis analysis) {
		super(analysis.getLog(peNum), String.valueOf(Nversion));

		sourceFile = analysis.getLog(peNum);
		shiftAmount = analysis.tachyonShifts.getShiftAmount(peNum);

		lastBeginEvent = new LogEntryData();
		lastBeginEvent.setValid(false);
		endComputationOccurred = false;

		reader = createBufferedReader(sourceFile);
		version = Nversion;
		try {
			reader.readLine(); // skip over the header (already read)
		} catch (IOException e) {
			System.err.println("Error reading file");
		}
	}


	/** Try to load the log file or a corresponding compressed version ending in ".gz" */
	private BufferedReader createBufferedReader(File file) {
		BufferedReader r = null;
		String filename = file.getAbsolutePath();
		String s3 = filename.substring(filename.length()-3); // last 3 characters of filename
		
		try {
			if(s3.compareTo(".gz")==0){
				// Try loading the gz version of the log file
				InputStream fis = new FileInputStream(file);
				InputStream gis = new GZIPInputStream(fis);
				r = new BufferedReader(new InputStreamReader(gis), bufferSize);
			} else {
				// Try loading the log file uncompressed
				FileReader fr = new FileReader(file);
				r = new BufferedReader(fr, bufferSize);			
			}

		} catch (IOException e2) {
			System.err.println("Error reading file " + filename);
			return null;
		}
		
		return r;

	}
	
	
	/** Check if the log file or a corresponding compressed version ending in ".gz" is readable */
	protected boolean checkAvailable() {
		return sourceFile.canRead();
	}

	
	/** Intepret a user's note string. For example, the user string could have substrings such as "<EP 10>" which should be replaced by the name of entry method 10. */
	private String interpretNote(String input){
		Analysis a = MainWindow.runObject[myRun];
		String modified = input;
		if(modified.contains("<EP")){
			int numEntries = a.getEntryCount();
			for(int i=0; i<numEntries; i++){
				String name = a.getEntryFullNameByID(i);
				modified = modified.replace("<EP " + i + ">", name);
			}		
		}
		return modified;
	}

	public LogEntryData nextProcessingEvent() throws IOException, EndOfLogSuccess
	{
		LogEntryData data = new LogEntryData();

		while (true) {
			String line = reader.readLine();

			if (line == null || endComputationOccurred) {
				throw new EndOfLogSuccess();
			}

			AsciiLineParser sc = new AsciiLineParser(line);

			data.type = (int) sc.nextLong();

			switch (data.type) {
				case BEGIN_PROCESSING:
					data.mtype = (int) sc.nextLong();
					data.entry = (int) sc.nextLong();
					data.time = sc.nextLong() + shiftAmount;
					data.event = (int) sc.nextLong();
					data.pe = (int) sc.nextLong();
					if (version >= 2.0) {
						data.msglen = (int) sc.nextLong();
					} else {
						data.msglen = -1;
					}
					if (version >= 4.0) {
						data.recvTime = sc.nextLong() + shiftAmount;
						data.id[0] = (int) sc.nextLong();
						data.id[1] = (int) sc.nextLong();
						data.id[2] = (int) sc.nextLong();
					}
					if (version >= 7.0) {
						data.id[3] = (int) sc.nextLong();
					}
					if (version >= 6.5) {
						data.cpuStartTime = sc.nextLong() + shiftAmount;
					}

					return data;
				case END_PROCESSING:
					data.mtype = (int) sc.nextLong();
					data.entry = (int) sc.nextLong();
					data.time = sc.nextLong() + shiftAmount;
					data.event = (int) sc.nextLong();
					data.pe = (int) sc.nextLong();
					if (version >= 2.0) {
						data.msglen = (int) sc.nextLong();
					} else {
						data.msglen = -1;
					}
					if (version >= 6.5) {
						data.cpuEndTime = sc.nextLong() + shiftAmount;
					}
					return data;
				default:
					break;
			}
		}
	}
	

	/** 
	 * Create a new LogEntryData by reading/parsing the next line from the log 
	 * 
	 * Upon reaching the end of the file, a fake END_COMPUTATION event will be produced if none was found in the log file.
	 * After the end of the file is reached and after some END_COMPUTATION has been returned, an EndOfLogException will be thrown
	 * 
	 * If any problem is detected when reading within a line, an IOException is produced
	 * 
	 * */
	public LogEntryData nextEvent() throws InputMismatchException, IOException, EndOfLogSuccess
	{
		LogEntryData data = new LogEntryData();
		StsReader stsinfo = MainWindow.runObject[myRun].getSts();
		
		String line = reader.readLine();
	    
		if (line == null)
			throw new EndOfLogSuccess();
		
		AsciiLineParser sc = new AsciiLineParser(line);

		// We can't keep reading once we've past the END_COMPUTATION record
		if(endComputationOccurred){
			throw new EndOfLogSuccess();
		}

		// If at end of file and we haven't s3een an END_COMPUTATION yet
		if(line == null){
			// Generate a fake END_COMPUTATION if no legitimate one was found
			// This is to deal with partial truncated projections logs.
			endComputationOccurred = true;
			data.type = END_COMPUTATION;
			data.time = lastRecordedTime;
			System.err.println("[" + sourceFile.getAbsolutePath() + "] WARNING: Partial or Corrupted Projections log. Faked END_COMPUTATION entry added for last recorded time of " +	data.time);
			return data;			
		}

		data.type = (int) sc.nextLong();
		switch (data.type) {
		case BEGIN_IDLE:
			lastBeginEvent.time = data.time = sc.nextLong() + shiftAmount;
			lastBeginEvent.pe = data.pe = (int) sc.nextLong();
			lastBeginEvent.setValid(true);
			break;
		case END_IDLE: 
			data.time = sc.nextLong() + shiftAmount;
			data.pe = (int) sc.nextLong();
			lastBeginEvent.setValid(false);
			break;
		case BEGIN_PACK: case END_PACK:
		case BEGIN_UNPACK: case END_UNPACK:
			data.time = sc.nextLong() + shiftAmount;
			data.pe = (int) sc.nextLong();
			break;
		case USER_SUPPLIED:
			data.userSupplied = new Integer((int) sc.nextLong());
			break;
		case USER_SUPPLIED_NOTE:
			data.time = sc.nextLong() + shiftAmount;
			int strLen = (int)sc.nextLong(); // strlen
			data.note = interpretNote(sc.nextString(strLen));
			break;
		case USER_SUPPLIED_BRACKETED_NOTE:
			data.time = sc.nextLong() + shiftAmount;
			data.endTime = sc.nextLong();
			data.userEventID = (int) sc.nextLong();
			data.entry = data.userEventID;
			int brStrLen = (int)sc.nextLong(); // strlen
			data.note = interpretNote(sc.nextString(brStrLen)); 
			break;
		case MEMORY_USAGE:
			data.memoryUsage = sc.nextLong();
			data.time = sc.nextLong() + shiftAmount;
			break;
		case CREATION:
			data.mtype = (int) sc.nextLong();
			data.entry = (int) sc.nextLong();
			data.time = sc.nextLong() + shiftAmount;
			data.event = (int) sc.nextLong();
			data.pe = (int) sc.nextLong();
			if (version >= 2.0) {
				data.msglen = (int) sc.nextLong();
			} else {
				data.msglen = -1;
			}
			if (version >= 5.0) {
				data.sendTime = sc.nextLong() + shiftAmount;
			}
			break;
		case CREATION_BCAST:
			data.mtype = (int) sc.nextLong();
			data.entry = (int) sc.nextLong();
			data.time = sc.nextLong() + shiftAmount;
			data.event = (int) sc.nextLong();
			data.pe = (int) sc.nextLong();
			if (version >= 2.0) {
				data.msglen = (int) sc.nextLong();
			} else {
				data.msglen = -1;
			}
			if (version >= 5.0) {
				data.sendTime = sc.nextLong() + shiftAmount;
			}
			data.numPEs = (int) sc.nextLong();
			break;
		case CREATION_MULTICAST:
			data.mtype = (int) sc.nextLong();
			data.entry = (int) sc.nextLong();
			data.time = sc.nextLong() + shiftAmount;
			data.event = (int) sc.nextLong();
			data.pe = (int) sc.nextLong();
			if (version >= 2.0) {
				data.msglen = (int) sc.nextLong();
			} else {
				data.msglen = -1;
			}
			if (version >= 5.0) {
				data.sendTime = sc.nextLong() + shiftAmount;
			}
			data.numPEs = (int) sc.nextLong();
			data.destPEs = new int[data.numPEs];
			for (int i=0;i<data.numPEs;i++) {
				data.destPEs[i] = (int) sc.nextLong();
			}
			break;
		case BEGIN_PROCESSING: 
			lastBeginEvent.mtype = data.mtype = (int) sc.nextLong();
			lastBeginEvent.entry = data.entry = (int) sc.nextLong();
			lastBeginEvent.time = data.time = sc.nextLong() + shiftAmount;
			lastBeginEvent.event = data.event = (int) sc.nextLong();
			lastBeginEvent.pe = data.pe = (int) sc.nextLong();
			if (version >= 2.0) {
				lastBeginEvent.msglen = data.msglen = (int) sc.nextLong();
			} else {
				lastBeginEvent.msglen = data.msglen = -1;
			}
			if (version >= 4.0) {
				lastBeginEvent.recvTime = data.recvTime = sc.nextLong() + shiftAmount;
				lastBeginEvent.id[0] = data.id[0] = (int) sc.nextLong();
				lastBeginEvent.id[1] = data.id[1] = (int) sc.nextLong();
				lastBeginEvent.id[2] = data.id[2] = (int) sc.nextLong();
			}
			if (version >= 7.0) {
				lastBeginEvent.id[3] = data.id[3] = (int) sc.nextLong();
			}
			if (version >= 6.5) {
				lastBeginEvent.cpuStartTime = data.cpuStartTime = sc.nextLong() + shiftAmount;
			}
			if (version >= 6.6) {
				//lastBeginEvent.numPerfCounts = data.numPerfCounts = (int) sc.nextLong();
				lastBeginEvent.numPerfCounts = data.numPerfCounts = stsinfo.getNumPerfCounts();
				lastBeginEvent.perfCounts = new long[data.numPerfCounts];
				data.perfCounts = new long[data.numPerfCounts];
				for (int i=0; i<data.numPerfCounts; i++) {
					lastBeginEvent.perfCounts[i] = data.perfCounts[i] = sc.nextLong();
				}
			}
			lastBeginEvent.setValid(true);
			break;
		case END_PROCESSING:
			data.mtype = (int) sc.nextLong();
			data.entry = (int) sc.nextLong();
			data.time = sc.nextLong() + shiftAmount;
			data.event = (int) sc.nextLong();
			data.pe = (int) sc.nextLong();
			if (version >= 2.0) {
				data.msglen = (int) sc.nextLong();
			} else {
				data.msglen = -1;
			}
			if (version >= 6.5) {
				data.cpuEndTime = sc.nextLong() + shiftAmount;
			}
			if (version >= 6.6) {
				//data.numPerfCounts = (int) sc.nextLong();
				data.numPerfCounts = stsinfo.getNumPerfCounts();
				data.perfCounts = new long[data.numPerfCounts];
				for (int i=0; i<data.numPerfCounts; i++) {
					data.perfCounts[i] = sc.nextLong();
				}
			}
			break;
		case BEGIN_TRACE: 
			data.time = sc.nextLong() + shiftAmount;
			// invalidates the last Begin Event. BEGIN_TRACE happens
			// in the context of an entry method that is *not* traced.
			// Hence when a BEGIN_TRACE event is encountered, no
			// information is actually known about the entry method
			// context.
			lastBeginEvent.setValid(false);
			break;
		case END_TRACE:
			data.time = sc.nextLong() + shiftAmount;
			// END_TRACE happens in the context of an existing
			// entry method and hence should logically "end" it.
			// This means any client taking note of END_TRACE must
			// take into account lastBeginEvent in order to get
			// reasonable data.
			break;
		case BEGIN_FUNC:
			data.time = sc.nextLong() + shiftAmount;
			data.entry = (int) sc.nextLong();
			data.lineNo = (int) sc.nextLong();
			data.funcName = "";//sc.nextLine();
			break;
		case END_FUNC:
			data.time = sc.nextLong() + shiftAmount;
			data.entry = (int) sc.nextLong();
			break;
		case MESSAGE_RECV:
			data.mtype = (int) sc.nextLong();
			data.time = sc.nextLong() + shiftAmount;
			data.event = (int) sc.nextLong();
			data.pe = (int) sc.nextLong();
			data.msglen = (int) sc.nextLong();
			break;
		case ENQUEUE: case DEQUEUE:
			data.mtype = (int) sc.nextLong();
			data.time = sc.nextLong() + shiftAmount;
			data.event = (int) sc.nextLong();
			data.pe = (int) sc.nextLong();
			break;
		case BEGIN_INTERRUPT: case END_INTERRUPT:
			data.time = sc.nextLong() + shiftAmount;
			data.event = (int) sc.nextLong();
			data.pe = (int) sc.nextLong();
			break;
		case BEGIN_COMPUTATION:
			data.time = sc.nextLong() + shiftAmount;
			break;
		case END_COMPUTATION:
			data.time = sc.nextLong() + shiftAmount;
			endComputationOccurred = true;
			break;
		case USER_EVENT:
			data.userEventID = (int) sc.nextLong();
			data.entry = data.userEventID; 
			data.time = sc.nextLong() + shiftAmount;
			data.event = (int) sc.nextLong();
			data.pe = (int) sc.nextLong();
			break;
		case USER_EVENT_PAIR:
			data.userEventID = (int) sc.nextLong();
			data.entry = data.userEventID;
			data.time = sc.nextLong() + shiftAmount;
			data.event = (int) sc.nextLong();
			data.pe = (int) sc.nextLong();

			// Charm++ before 6.8 did not have a nestedID for USER_EVENT_PAIR
			if (sc.hasNextField())
				data.nestedID = (int) sc.nextLong();
			break;
		case USER_STAT:
			data.time = sc.nextLong() + shiftAmount; //Wall Time
			data.userTime = sc.nextDouble();	//User time
			data.stat = sc.nextDouble();
			data.pe = (int) sc.nextLong();
			data.userEventID = (int) sc.nextLong();
			break;
		default:
			data.type = -1;
		break;

		}

		lastRecordedTime = data.time;
		return data;

	}

	/**
	 * Find the next event on or after the given timestamp. 
	 *
	 *  An EndOfLogException indicates that no such event was found.
	 */
	public LogEntryData nextEventOnOrAfter(long timestamp) 
	throws IOException, EndOfLogSuccess
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
	 *  Return the next log event with the given eventType.
	 */
	public LogEntryData nextEventOfType(int eventType) 
	throws IOException, EndOfLogSuccess
	{
		LogEntryData data = new LogEntryData();
		while (true) {
			data = nextEvent();
			if (data.type == eventType) {
				return data;
			}
		}
	}

	
	public LogEntryData getLastBE() {
		if (lastBeginEvent.isValid()) {
			return lastBeginEvent;
		}
		return null;
	}


	public void close() throws IOException {
		if (reader != null) {
			reader.close();
		}
	}


}
