package projections.analysis;

import projections.gui.*;
import projections.misc.*;

import java.io.*;
import java.util.*;

/**
 *  Written by Chee Wai Lee
 *  4/12/2002
 *
 *  GenericLogReader reads a log file and returns entry data one at a time.
 *  
 *  The general contract is that the caller supplies the Reader stream on
 *  object creation time (which means the input can actually come from a
 *  networked stream instead of from just a file).
 *
 */

public class GenericLogReader extends ProjectionsReader
    implements PointCapableReader
{
    private static final long INITIAL_STEP = 1024; // reasonable jump size

    private AsciiIntegerReader reader;
    private double version;

    // This flag is to handle truncated partial projections logs.
    // The flag is raised whenever Projections is forced to fake an
    // END_COMPUTATION event as a result of an unexpected EOF
    // exception. When this flag is detected again the next time
    // it is read, Projections will raise the exception as usual for
    // the calling routines to operate on transparently.
    //
    // Technically, lastRecordedTime is not required, but because this
    // class cannot control what client modules do to the "data" object
    // passed in, it is much safer to record the lastRecordedTime locally.
    private boolean fakedEndComputation = false;
    private long lastRecordedTime = 0;

    // indexed by EP with a Vector of run length encoding blocks.
    private Vector intervalData[];

    public GenericLogReader(String filename, double Nversion) {
	super(filename, String.valueOf(Nversion));
	try {
	    reader = new AsciiIntegerReader(new FileReader(filename));
	    version = Nversion;
	    reader.nextLine(); // skip over the header (already read)
	} catch (IOException e) {
	    System.err.println("Error reading file " + filename);
	}
    }

    public GenericLogReader(int peNum, double Nversion) {
	super(Analysis.getLogName(peNum), String.valueOf(Nversion));
	try {
	    reader = new AsciiIntegerReader(new FileReader(sourceString));
	    version = Nversion;
	    reader.nextLine(); // skip over the header (already read)
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
     *  resets the file stream. Not all streams in Java support the reset()
     *  method, so the only way is to close the stream and restart it.
     */
    public void reset() 
	throws IOException
    {
	reader.close();
	reader = new AsciiIntegerReader(new FileReader(sourceString));
	reader.nextLine();
    }

    // The LogEntryData object should be created by the calling method
    // and passed into nextEvent.
    public void nextEvent(LogEntryData data) 
	throws IOException, EOFException
    {
	try {
	    data.type = reader.nextInt();
	    switch (data.type) {
	    case BEGIN_IDLE: case END_IDLE: 
	    case BEGIN_PACK: case END_PACK:
	    case BEGIN_UNPACK: case END_UNPACK:
		data.time = reader.nextLong();
		data.pe = reader.nextInt();
		reader.nextLine(); // Skip over any garbage 
		break;
	    case CREATION:
		data.mtype = reader.nextInt();
		data.entry = reader.nextInt();
		data.time = reader.nextLong();
		data.event = reader.nextInt();
		data.pe = reader.nextInt();
		if (version >= 2.0) {
		    data.msglen = reader.nextInt();
		} else {
		    data.msglen = -1;
		}
		if (version >= 5.0) {
		    data.sendTime = reader.nextLong();
		}
		break;
	    case CREATION_MULTICAST:
		data.mtype = reader.nextInt();
		data.entry = reader.nextInt();
		data.time = reader.nextLong();
		data.event = reader.nextInt();
		data.pe = reader.nextInt();
		if (version >= 2.0) {
		    data.msglen = reader.nextInt();
		} else {
		    data.msglen = -1;
		}
		if (version >= 5.0) {
		    data.sendTime = reader.nextLong();
		}
		data.destPEs = new int[reader.nextInt()];
		for (int i=0;i<data.destPEs.length;i++) {
		    data.destPEs[i] = reader.nextInt();
		}
		break;
	    case BEGIN_PROCESSING: 
		data.mtype = reader.nextInt();
		data.entry = reader.nextInt();
		data.time = reader.nextLong();
		data.event = reader.nextInt();
		data.pe = reader.nextInt();
		if (version >= 2.0) {
		    data.msglen = reader.nextInt();
		} else {
		    data.msglen = -1;
		}
		if (version >= 4.0) {
		    data.recvTime  = reader.nextLong();
		    data.id[0] = reader.nextInt();
		    data.id[1] = reader.nextInt();
		    data.id[2] = reader.nextInt();
		}
		if (version >= 6.5) {
		    data.cpuStartTime = reader.nextLong();
		}
		if (version >= 6.6) {
		    data.numPerfCounts = reader.nextInt();
		    data.perfCounts = new long[data.numPerfCounts];
		    for (int i=0; i<data.numPerfCounts; i++) {
			data.perfCounts[i] = reader.nextLong();
		    }
		}
		reader.nextLine(); // Skip over any garbage 
		break;
	    case END_PROCESSING:
		data.mtype = reader.nextInt();
		data.entry = reader.nextInt();
		data.time = reader.nextLong();
		data.event = reader.nextInt();
		data.pe = reader.nextInt();
		if (version >= 2.0) {
		    data.msglen = reader.nextInt();
		} else {
		    data.msglen = -1;
		}
		if (version >= 6.5) {
		    data.cpuEndTime = reader.nextLong();
		}
		if (version >= 6.6) {
		    data.numPerfCounts = reader.nextInt();
		    data.perfCounts = new long[data.numPerfCounts];
		    for (int i=0; i<data.numPerfCounts; i++) {
			data.perfCounts[i] = reader.nextLong();
		    }
		}
		reader.nextLine(); // Skip over any garbage 
		break;
	    case BEGIN_TRACE: case END_TRACE:
		data.time = reader.nextLong();
		reader.nextLine(); // Skip over any garbage 
		break;
	    case BEGIN_FUNC:
		data.time = reader.nextLong();
		data.entry = reader.nextInt();
		data.lineNo = reader.nextInt();
		data.funcName = reader.nextString();
		// comment of interest, what "nextString" is doing in an
		// AsciiIntegerReader is beyond me, but I appreciate its
		// existance!
		reader.nextLine();
		break;
	    case END_FUNC:
		data.time = reader.nextLong();
		data.entry = reader.nextInt();
		reader.nextLine();
		break;
	    case MESSAGE_RECV:
		data.mtype = reader.nextInt();
		data.time = reader.nextLong();
		data.event = reader.nextInt();
		data.pe = reader.nextInt();
		data.msglen = reader.nextInt();
		reader.nextLine(); // Skip over any garbage 
		break;
	    case ENQUEUE: case DEQUEUE:
		data.mtype = reader.nextInt();
		data.time = reader.nextLong();
		data.event = reader.nextInt();
		data.pe = reader.nextInt();
		reader.nextLine(); // Skip over any garbage 
		break;
	    case BEGIN_INTERRUPT: case END_INTERRUPT:
		data.time = reader.nextLong();
		data.event = reader.nextInt();
		data.pe = reader.nextInt();
		reader.nextLine(); // Skip over any garbage 
		break;
	    case BEGIN_COMPUTATION: case END_COMPUTATION:
		data.time = reader.nextLong();
		reader.nextLine(); // Skip over any garbage 
		break;
	    case USER_EVENT:
		data.userEventID = reader.nextInt();
		// tentative hack for compatibility
		data.entry = data.userEventID; 
		data.time = reader.nextLong();
		data.event = reader.nextInt();
		data.pe = reader.nextInt();
		reader.nextLine(); // Skip over any garbage 
		break;
	    case USER_EVENT_PAIR:
		data.userEventID = reader.nextInt();
		// tentative hack for compatibility
		data.entry = data.userEventID;
		data.time = reader.nextLong();
		data.event = reader.nextInt();
		data.pe = reader.nextInt();
		reader.nextLine(); // Skip over any garbage 
		break;
	    default:
		data.type = -1;
		reader.nextLine(); // Skip over any garbage 
		break;
	    }
	    lastRecordedTime = data.time;
	} catch (EOFException e) {
	    if (fakedEndComputation || (data.type == END_COMPUTATION)) {
		// throw the exception as if this were a proper EOF
		throw e;
	    } else {
		// This is to deal with partial truncated projections logs.
		// Fake an END_COMPUTATION event. This should *NEVER* be
		// silent!!! A Warning *MUST* be sounded.
		fakedEndComputation = true;
		data.type = END_COMPUTATION;
		data.time = lastRecordedTime;
		System.err.println("[" + sourceString + "] " +
				   "WARNING: Partial or Corrupted " +
				   "Projections log. Faked END_COMPUTATION " +
				   "entry added for last recorded time of " +
				   data.time);
	    }
	}
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
    public void nextEventOnOrAfter(long timestamp, LogEntryData data) 
	throws IOException, EOFException
    {
	seqLookForNextEventOnOrAfter(timestamp, data);
	//	lookForEventOnOrAfter(timestamp, INITIAL_STEP, -1, 0, data);
    }
    
    private void lookForNextEventOnOrAfter(long timestamp, long seekpoint,
					   long lastTimestamp, 
					   long lastSeekpoint,
					   LogEntryData data)
	throws IOException, EOFException
    {
	reader.skip(seekpoint-lastSeekpoint);
	reader.nextLine();
	nextEvent(data);
	// skip unrecognized tags
	while (data.type == -1) {
	    nextEvent(data);
	}
	if (data.time < timestamp) {
	    
	} else if (data.time > timestamp) {
	    
	} else {
	    // found! just return!
	    return;
	}
    }

    // More precisely, the next RECOGNIZED event
    private void seqLookForNextEventOnOrAfter(long timestamp, 
					      LogEntryData data) 
	throws IOException, EOFException
    {
	while (true) {
	    nextEvent(data);
	    // skip unrecognized tags
	    while (data.type == -1) {
		nextEvent(data);
	    }
	    if (data.time >= timestamp) {
		// found! just return!
		return;
	    }
	}
    }

    /**
     *  nextEventOfType gets the next event of the eventType.
     */
    public void nextEventOfType(int eventType, LogEntryData data) 
	throws IOException, EOFException
    {
	while (true) {
	    nextEvent(data);
	    if (data.type == eventType) {
		return;
	    }
	}
    }

    public void nextEventOfTypeOnOrAfter(int eventType, long timestamp,
					 LogEntryData data)
	throws IOException, EOFException
    {
	while (true) {
	    nextEventOnOrAfter(timestamp, data);
	    if (data.type == eventType) {
		return;
	    }
	}
    }

    public void close()
        throws IOException
    {
        if (reader != null) {
            reader.close();
        }
    }
}
