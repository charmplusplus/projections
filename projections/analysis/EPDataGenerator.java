package projections.analysis;

import java.io.*;

import projections.gui.*;
import projections.misc.*;

public class EPDataGenerator 
    extends ProjDefs
    implements EPNamdDefs
{
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;

    public EPDataGenerator(long data[][], OrderedIntList validPEs,
			   long startTime, long endTime) {
	OrderedIntList tmpPEs = validPEs.copyOf();
	GenericLogReader reader;
	LogEntryData logData = new LogEntryData();
	int numUserEPs = MainWindow.runObject[myRun].getNumUserEntries();
	int numUserEvents = MainWindow.runObject[myRun].getNumUserDefinedEvents();

	// flags
	boolean processing = false;
	boolean packing = false;
	boolean unpacking = false;
	boolean inIdle = false;

	// cross-event data
	long beginTime = 0;
	long userBeginTime = 0;
	long packStartTime = 0;
	long unpackStartTime = 0;
	long subtractTime = 0;

	// read each prescribed PE
	while (tmpPEs.hasMoreElements()) {
	    int pe = tmpPEs.nextElement();
	    reader = new GenericLogReader(MainWindow.runObject[myRun].getLogName(pe),
					  MainWindow.runObject[myRun].getVersion());
	    // data reset for every new PE file
	    processing = false;
	    packing = false;
	    unpacking = false;
	    inIdle = false;
	    beginTime = 0;
	    packStartTime = 0;
	    unpackStartTime = 0;
	    subtractTime = 0;
	    try {
		while (true) {
		    reader.nextEvent(logData);
		    // ignore all events outside time range
		    if (logData.time >= startTime && logData.time <= endTime) {
			switch (logData.type) {
			case BEGIN_IDLE:
			    // new begins always clobber old begins
			    inIdle = true;
			    subtractTime = 0;
			    beginTime = logData.time;
			    break;
			case END_IDLE:
			    // end after end is ignored.
			    if (inIdle) {
				data[TIME_DATA][numUserEPs+numUserEvents] 
				    += (logData.time-beginTime) - subtractTime;
				inIdle = false;
			    }
			    break;
			case BEGIN_PACK:
			    packing = true;
			    packStartTime = logData.time;
			    break;
			case END_PACK:
			    if (packing) {
				data[TIME_DATA][numUserEPs+numUserEvents+1]
				    += (logData.time - packStartTime);
				if (processing) {
				    subtractTime += logData.time-packStartTime;
				}
				packing = false;
			    }
			    break;
			case BEGIN_UNPACK:
			    unpacking = true;
			    unpackStartTime = logData.time;
			    break;
			case END_UNPACK:
			    if (unpacking) {
				data[TIME_DATA][numUserEPs+numUserEvents+2]
				    += (logData.time - unpackStartTime);
				if (processing) {
				    subtractTime+=logData.time-unpackStartTime;
				}
				unpacking = false;
			    }
			    break;
			case BEGIN_PROCESSING:
			    processing = true;
			    subtractTime = 0;
			    beginTime = logData.time;
			    break;
			case END_PROCESSING:
			    if (processing) {
				data[TIME_DATA][logData.entry]
				    += (logData.time-beginTime) - subtractTime;
				processing = false;
			    }
			    break;
			case USER_EVENT_PAIR:
			    // another USER_EVENT_PAIR is expected.
			    int eventID = logData.userEventID;
			    userBeginTime = logData.time;
			    reader.nextEvent(logData);
			    while (logData.userEventID != eventID) {
				// nested user events, ignore
				reader.nextEvent(logData);
			    }
			    data[TIME_DATA][numUserEPs+
					   MainWindow.runObject[myRun].getUserDefinedEventIndex(logData.userEventID)] += (logData.time - userBeginTime);
			    if (processing | inIdle) {
				subtractTime += logData.time - userBeginTime;
			    }
			    break;
			default:
			    // non interesting event, ignore
			    break;
			}
		    }
		}
	    } catch (EOFException e) {
		// do nothing. Reaching an EOF is an indication that the
		// log file is done and we can move on.
	    } catch (Exception e) {
		System.err.println("Exception found. Bad");
	    }
	}
    }
}
