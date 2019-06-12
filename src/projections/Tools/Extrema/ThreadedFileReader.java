package projections.Tools.Extrema;


import java.io.IOException;

import projections.analysis.Analysis;
import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.gui.MainWindow;
import projections.misc.LogEntry;


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
			new GenericLogReader( pe, MainWindow.runObject[myRun].getVersion());


		try {
			if (selectedActivity == Analysis.USER_EVENTS) {
				LogEntry logData;
				LogEntry logDataEnd;

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
					if (eventIndex != -1) {
                        myData[eventIndex] += logDataEnd.time - logData.time;
                    }
					logData = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
					logDataEnd = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR );
					if (logDataEnd.time > endTime) {
						break;
					}
				}
			} else {

				boolean isProcessing = false;
				LogEntry prevBeginProc = null;
				LogEntry prevBeginIdle = null;

				while (true) {
					LogEntry logData = reader.nextEvent();

					switch (logData.type) {

					case ProjDefs.CREATION:
						int eventIndex = logData.entry;
						if (selectedAttribute == ExtremaWindow.ATTR_MSGSSENT) {
							myData[eventIndex]++;
						} else if (selectedAttribute == ExtremaWindow.ATTR_BYTESSENT) {
							myData[eventIndex] += logData.msglen;
						}	
						break;

					case ProjDefs.BEGIN_PROCESSING:
						if (isProcessing)
						{
						// We add a "pretend" end event to accomodate
						// the prior begin processing event.
						if (selectedAttribute == 0 || selectedAttribute == 1 || selectedAttribute == 4 || selectedAttribute == 5 || selectedAttribute == 6 || selectedAttribute == 7)
							{
								if (logData.time <= endTime && logData.time >= startTime)
								{
									if (prevBeginProc.time < startTime) prevBeginProc.time = startTime;
									myData[prevBeginProc.entry] += logData.time - prevBeginProc.time;
								}
							}
						}
						isProcessing = true;
						prevBeginProc = logData;
						break;

					case ProjDefs.END_PROCESSING:
						if(isProcessing){
							if (selectedAttribute == 0 || selectedAttribute == 1 || selectedAttribute == 4||  selectedAttribute == 5||selectedAttribute == 6|| selectedAttribute == 7) {
								if (logData.time <= endTime && logData.time >= startTime) {
									if (prevBeginProc.time < startTime)
										prevBeginProc.time = startTime;
									myData[logData.entry] += logData.time - prevBeginProc.time;
								}
							}
							prevBeginProc = null;	
						}
						isProcessing = false;
						break;

					case ProjDefs.BEGIN_IDLE:
						// Assume Idles are never nested
						if (isProcessing)
						{
						// We add a "pretend" end event to accomodate
						// the prior begin processing event.
						if (selectedAttribute == 0 || selectedAttribute == 1 || selectedAttribute == 4 || selectedAttribute == 5 || selectedAttribute == 6 || selectedAttribute == 7)
							{
								if (logData.time <= endTime && logData.time >= startTime)
								{
									if (prevBeginProc.time < startTime) prevBeginProc.time = startTime;
									myData[prevBeginProc.entry] += logData.time - prevBeginProc.time;
									prevBeginProc = null;
									isProcessing = false;
								}
							}	
						}
						isProcessing = false;
						prevBeginIdle = logData;
						break;

					case ProjDefs.END_IDLE:
						//selectedAttribute 2 and 3 are not yet implemented
						if (selectedAttribute == 0 || selectedAttribute == 1 || selectedAttribute == 4||  selectedAttribute == 5||selectedAttribute == 6 || selectedAttribute == 7) {
							if (logData.time <= endTime && logData.time >= startTime) {
								if (prevBeginIdle.time < startTime)
									prevBeginIdle.time = startTime;
								myData[numActivities] += logData.time - prevBeginIdle.time;
							}
						}
						prevBeginIdle = null;
						break;

					case ProjDefs.END_COMPUTATION:
						if (isProcessing)
						{
							// If the last begin_processing has no end event,
							// add a "pretend" end event.
							if (selectedAttribute == 0 || selectedAttribute == 1 || selectedAttribute == 4||  selectedAttribute == 5||selectedAttribute == 6|| selectedAttribute == 7) {
								if (logData.time <= endTime && logData.time >= startTime) {
									if (prevBeginProc.time < startTime)
										prevBeginProc.time = startTime;
									myData[prevBeginProc.entry] += logData.time - prevBeginProc.time;
								}
							}
							prevBeginProc = null;
						}
						isProcessing = false;
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





