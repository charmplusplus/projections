package projections.analysis;

import java.io.IOException;

import projections.gui.MainWindow;
import projections.misc.LogEntry;

public class UsageCalc extends ProjDefs
{
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private int myRun = 0;

	private long beginTime,endTime;
	private long startTime;
	private int pnum;
	private int dataLen;
	private long packtime,packstarttime;
	private long unpacktime,unpackstarttime;
	private int numUserEntries;
	private double version;
	private int countflag=0;

	// curEntry is global because it has to deal with the relationship
	// between BEGIN_PROCESSING and END_PROCESSING events within the
	// same log file.
	//
	// it needs, however, to be reset between the reading of two log files.
	private int curEntry = -1;

	private boolean deltaEncoded = false;
	private void intervalCalc(float[][] data,int type, int entry, long time) {

		if (type != CREATION) {
			if (!(time>beginTime+1)) {
				time=beginTime;
				countflag = 0;
			} else {
				if (countflag != 2)
					countflag = 1;
			}
			if (time>endTime) {
				time=endTime;
				countflag = 2;
			} else {
				if(countflag != 0) {
					countflag =1;
				}
			}
		} else {
			if (countflag != 1) {
				time = 0;
			}
		}

		switch(type) {
		case BEGIN_PROCESSING:
			packtime = 0;
			unpacktime = 0;
			curEntry = MainWindow.runObject[myRun].getEntryIndex(entry);
			startTime = time;
			break;
		case END_PROCESSING:
			// curEntry == -1 means that there was no corresponding 
			// BEGIN_PROCESSING event, if so ignore the entrypoint
			if (curEntry == -1) break;
			curEntry = MainWindow.runObject[myRun].getEntryIndex(entry);
			if (curEntry != -1)
				data[0][curEntry] += 
					((time - startTime) - packtime - unpacktime);
			break;
		case CREATION:
			if(curEntry != -1){
				data[1][curEntry] += time;
			}
			break;
		case CREATION_MULTICAST:
			// do nothing for now
			break;
		case BEGIN_IDLE:
			startTime = time;
			break;
		case END_IDLE:
			// +2 places Idle time at the top of the usage profile display
			data[0][numUserEntries+2] += (time - startTime);
			break;
		case BEGIN_PACK:
			packstarttime = time;
			break;
		case END_PACK:
			// Packing is the first non-entry data item to be displayed
			// in the profile window.
			packtime += time - packstarttime;
			data[0][numUserEntries] += (time - packstarttime);
			/*
	    System.out.println("pack time " + (float)(time-packstarttime) +
			       " cumulative time " + data[0][numUserEntries]);
			 */
			break;
		case BEGIN_UNPACK:
			unpackstarttime = time;
			break;
		case END_UNPACK:
			// Unpacking is the second non-entry data item to be displayed
			// in the profile window.
			unpacktime += time - unpackstarttime;
			data[0][numUserEntries+1] += (time - unpackstarttime);
			break;
		default:
			/*ignore it*/
		}
	}

	public float[][] usage(int procnum, long begintime, 
			long endtime, double v) {
		version = v;
		beginTime = begintime;
		endTime = endtime;
		pnum = procnum;
		numUserEntries = MainWindow.runObject[myRun].getNumUserEntries();
		dataLen = numUserEntries + 4;

		GenericLogReader reader;
		LogEntry logEntry;

		float[][] data = new float[2][dataLen];
		// initialization
		for(int i=0;i<dataLen;i++){
			data[0][i] = (float )0.0;
			data[1][i] = (float )0.0;
		}

		reader = new GenericLogReader( procnum, version);
		logEntry = new LogEntry();
		curEntry = -1;

		startTime = 0;
		long time=0;
		boolean isProcessing = false;
		LogEntry lastBeginData = null;
		long prevTime = 0;
		try { 
			while (time<endTime) { //EOF exception terminates loop
				logEntry = reader.nextEvent();
				time = logEntry.time;
				switch(logEntry.type) {
				case BEGIN_IDLE: case END_IDLE:
					if (isProcessing) {
						intervalCalc(data, END_PROCESSING, lastBeginData.entry, logEntry.time);
						isProcessing = false;
						lastBeginData = null;
					}
					intervalCalc(data, logEntry.type, 0, time);
					break;
				case BEGIN_PACK: case END_PACK:
				case BEGIN_UNPACK: case END_UNPACK:
					intervalCalc(data, logEntry.type, 0, time);
					break;
				case BEGIN_PROCESSING:
					if (isProcessing) {
						intervalCalc(data, END_PROCESSING, lastBeginData.entry, logEntry.time);
					}
					intervalCalc(data, logEntry.type, 
							logEntry.entry, time);
					isProcessing = true;
					lastBeginData = logEntry;
					break;
				case END_PROCESSING:
					if (!isProcessing) {
						// bad, ignore.
						break;
					}
					intervalCalc(data, logEntry.type, 
							logEntry.entry, time);
					isProcessing = false;
					break;
				case BEGIN_TRACE:
					break;
				case END_TRACE:
					break;
				case MESSAGE_RECV:
					break;
				case CREATION:
					intervalCalc(data, logEntry.type, 0,
							logEntry.sendTime);
					break;
				case CREATION_MULTICAST:
					// read but do nothing for now.
					break;
				case USER_EVENT:
				case USER_EVENT_PAIR:
					// "uninteresting" events, "ignored"
					break;
				case ENQUEUE:
				case DEQUEUE:
					// "uninteresting" events, "ignored"
					break;
				case BEGIN_INTERRUPT:
				case END_INTERRUPT:
					// "uninteresting" events, "ignored"
					break;
				case END_COMPUTATION:
					// End computation is "uninteresting" but is
					// completely ignored because it does not
					// employ any delta encoding.
					break;
				default:
					// **CW** We can no longer ignore events we do not
					// care about. Delta encoding requires that every
					// event be processed.
					if (deltaEncoded) {
						System.out.println("Warning: Unknown Event! " +
								"This " +
						"can mess up delta encoding!");
					}
				break;
				}
			}
		} catch (EndOfLogSuccess e) {
			// do nothing
		} catch (IOException e) {
			System.out.println("Exception while reading log file " +
					pnum); 
		}
		for (int j=0; j<dataLen; j++) { //Scale times to percent
			// System.out.println("Data " + data[0][j] + " Send Time " + 
			// data[1][j]);
			data[0][j] = data[0][j] - data[1][j];
			data[0][j] = 
				(float )(100.0*data[0][j])/(endTime-beginTime);
			data[1][j] = 
				(float )(100.0*data[1][j])/(endTime-beginTime);
		}
		return data;
	}
}
