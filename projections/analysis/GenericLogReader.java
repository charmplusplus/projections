package projections.analysis;

import projections.misc.*;

import java.io.*;

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

public class GenericLogReader extends ProjDefs
{
    private static final long INITIAL_STEP = 1024; // reasonable jump size

    private String streamFilename;
    private AsciiIntegerReader reader;
    private double version;

    public GenericLogReader(String filename, double Nversion) {
	streamFilename = filename;
	try {
	    reader = new AsciiIntegerReader(new FileReader(filename));
	    version = Nversion;
	    reader.nextLine(); // skip over the useless header
	} catch (IOException e) {
	    System.err.println("Error reading file " + filename);
	}
    }

    /**
     *  resets the file stream. Not all streams in Java support the reset()
     *  method, so the only way is to close the stream and restart it.
     */
    public void reset() 
	throws IOException
    {
	reader.close();
	reader = new AsciiIntegerReader(new FileReader(streamFilename));
	reader.nextLine();
    }

    // The LogEntryData object should be created by the calling method
    // and passed into nextEvent.
    public void nextEvent(LogEntryData data) 
	throws IOException, EOFException
    {
	data.type = reader.nextInt();
	switch (data.type) {
	case BEGIN_IDLE: case END_IDLE:
	    data.time = reader.nextLong();
	    data.pe = reader.nextInt();
	    break;
	case CREATION:
	case BEGIN_PROCESSING:
	case END_PROCESSING:
	    data.mtype = reader.nextInt();
	    data.entry = reader.nextInt();
	    data.time = reader.nextLong();
	    data.event = reader.nextInt();
	    data.pe = reader.nextInt();
	    if (version > 1.0) {
		data.msglen = reader.nextInt();
	    } else {
		data.msglen = -1;
	    }
	    break;
	case ENQUEUE:
	    data.mtype = reader.nextInt();
	    data.time = reader.nextLong();
	    data.event = reader.nextInt();
	    data.pe = reader.nextInt();
	    break;
	case END_COMPUTATION:
	    data.time = reader.nextLong();
	    break;
	default:
	    data.type = -1;
	    reader.nextLine(); // Skip over any garbage 
	    break;
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
}
