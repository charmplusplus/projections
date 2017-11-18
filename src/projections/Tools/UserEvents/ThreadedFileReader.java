package projections.Tools.UserEvents;

import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.gui.MainWindow;
import projections.misc.LogEntryData;

import java.io.IOException;

class ThreadedFileReader implements Runnable {

	private int pe;
	private int pIdx;
	private int startInterval;
	private int endInterval;
	private long intervalSize;
	private int myRun = 0;
	private double[][] timeSpent;
	private double[][] callRate;

	protected ThreadedFileReader(int pe, int pIdx, int startInterval, int endInterval, long intervalSize, double[][] timeSpent, double[][] callRate) {
		this.pe = pe;
		this.pIdx = pIdx;
		this.startInterval = startInterval;
		this.endInterval = endInterval;
		this.intervalSize = intervalSize;

		this.timeSpent = timeSpent;
		this.callRate = callRate;
	}

	@Override
	public void run() {
		// READ - nothing here
		GenericLogReader reader = new GenericLogReader( pe, MainWindow.runObject[myRun].getVersion());
		LogEntryData logData;
		LogEntryData logDataEnd;

		int numActivities = MainWindow.runObject[myRun].getNumUserDefinedEvents();

		// Skip to the first begin.
		try {

			synchronized (timeSpent) {
				timeSpent[pIdx] = new double[numActivities];
				callRate[pIdx] = new double[numActivities];
			}

			logData = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
			logDataEnd = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);

			while (logData.time < startInterval*intervalSize) {
				logData = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
				logDataEnd = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
			}
			int eventIndex = 0;
			while (true) {
				// process pair read previously
				eventIndex = MainWindow.runObject[myRun].getUserDefinedEventIndex(logData.userEventID);
				// check that this index is valid, in case logData.userEventID doesn't actually exist
				if (eventIndex != -1) {
					timeSpent[pIdx][eventIndex] +=
							logDataEnd.time - logData.time;
					callRate[pIdx][eventIndex]++;
				}
				logData = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
				logDataEnd = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
				if (logDataEnd.time > endInterval*intervalSize) {
					break;
				}
			}
		}catch (EndOfLogSuccess e) {
			// do nothing
		} catch (IOException e) {	// <-- didn't catch a null pointer exception, perhaps change to Exception?
			System.out.println("Exception while reading log file " + pe);
		}


		try {
			reader.close();
		} catch (IOException e1) {
			System.err.println("Error: could not close log file reader for processor " + pe );
		}
	}
}
