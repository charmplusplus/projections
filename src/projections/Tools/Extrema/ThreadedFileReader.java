package projections.Tools.Extrema;


import java.io.IOException;

import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.gui.Analysis;
import projections.gui.MainWindow;
import projections.misc.LogEntryData;


/** The reader threads for Extrema tool. */
class ThreadedFileReader implements Runnable  {

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;

	private int pe;
	private long startTime;
	private long endTime;
	private int numActivities;
	private int numActivityPlusSpecial;

	private int selectedActivity;
	private int selectedAttribute;

	double[] myData;


	protected ThreadedFileReader(int pe, long startTime2, long endTime2, int numActivities, int numActivityPlusSpecial, int selectedActivity, int selectedAttribute){
		this.pe = pe;
		this.startTime = startTime2;
		this.endTime = endTime2;
		this.numActivities = numActivities;
		this.numActivityPlusSpecial = numActivityPlusSpecial;
		this.selectedActivity = selectedActivity;
		this.selectedAttribute = selectedAttribute;
	}


	public void run() { 

		myData = new double[numActivityPlusSpecial];


		// Construct tempData (read) array hereColorSelectable
		//
		// **NOTE** We really need a generic interface to a "data"
		// object. Re-writing the reading code each time for each
		// tool is starting to get really painful.
		//
		// Right now, we restrict ourselves to reading logs (since
		// we wanna support User Events, the nature of which 
		// unfortunately requires us to write a different read loop
		// for it.
		GenericLogReader reader = 
			new GenericLogReader(MainWindow.runObject[myRun].getLog(pe), pe, MainWindow.runObject[myRun].getVersion());


		try {
			if (selectedActivity == Analysis.USER_EVENTS) {
				LogEntryData logData;
				LogEntryData logDataEnd;

				logData = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
				logDataEnd = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR );

				while (logData.time < startTime) {
					logData = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
					logDataEnd = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR );
				}
				int eventIndex = 0;
				while (true) {
					// process pair read previously
					eventIndex = 
						MainWindow.runObject[myRun].getUserDefinedEventIndex(logData.userEventID);
					myData[eventIndex] += logDataEnd.time - logData.time;
					logData = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
					logDataEnd = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR );
					if (logDataEnd.time > endTime) {
						break;
					}
				}
			} else {

				int nestingLevel = 0;
				LogEntryData prevBeginProc = null;
				LogEntryData prevBeginIdle = null;

				while (true) {
					LogEntryData logData = reader.nextEvent();	

					switch (logData.type) {

					case ProjDefs.CREATION:
						int eventIndex = logData.entry;
						if (selectedAttribute == 2) {
							myData[eventIndex]++;
						} else if (selectedAttribute == 3) {
							myData[eventIndex] += logData.msglen;
						}	
						break;

					case ProjDefs.BEGIN_PROCESSING:
						nestingLevel++;
						if(nestingLevel == 1){
							prevBeginProc = logData;
						}
						break;

					case ProjDefs.END_PROCESSING:
						nestingLevel--;
						if(nestingLevel == 0){
							if (selectedAttribute == 0 || selectedAttribute == 1 || selectedAttribute == 4||  selectedAttribute == 5||selectedAttribute == 6|| selectedAttribute == 7) {
								myData[logData.entry] += logData.time - prevBeginProc.time;
							}
							prevBeginProc = null;	
						} else if(nestingLevel < 0){
							nestingLevel = 0; // Reset to 0 because we didn't get to see an appropriate matching BEGIN_PROCESSING.
							prevBeginProc = null;
						}
						break;

					case ProjDefs.BEGIN_IDLE:
						// Assume Idles are never nested
						prevBeginIdle = logData;
						break;

					case ProjDefs.END_IDLE:
						if (selectedAttribute == 0 || selectedAttribute == 1 || selectedAttribute == 4||  selectedAttribute == 5||selectedAttribute == 6 || selectedAttribute == 7) {
							myData[numActivities] += logData.time - prevBeginIdle.time;
						}
						prevBeginIdle = null;
						break;

					}

				}

			}
		} catch (EndOfLogSuccess e) {
			// Successfully read the log file, attempt to close it
		} catch (IOException e) {
			System.err.println("Outlier Analysis: Error in reading log data for processor " + pe);
			System.err.println(e);
		}

		try {
			reader.close();
		} catch (IOException e1) {
			System.err.println("Error: could not close log file reader for processor " + pe );
		}
		

		// compute overhead time
		myData[numActivities+1] = endTime - startTime;
		for(int e=0; e<numActivities+1; e++){
			myData[numActivities+1] -= myData[e];
		}


		if(selectedAttribute == ExtremaWindow.ATTR_LEASTIDLE || selectedAttribute == ExtremaWindow.ATTR_MOSTIDLE){
			// Scale raw data into percentages
			for(int e=0; e< myData.length; e++){
				myData[e] = myData[e] * 100.0 / (double)(endTime - startTime);
			}
		}

	}


}





