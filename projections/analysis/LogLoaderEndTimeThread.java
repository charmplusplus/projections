package projections.analysis;

import java.io.EOFException;
import java.io.IOException;

import projections.gui.MainWindow;
import projections.misc.LogEntryData;


/** This thread's run() method will lookup the endtime for an input log file */
public class LogLoaderEndTimeThread  extends Thread {

	public String logName;
	public Long result;
	int myRun = 0;

	public LogLoaderEndTimeThread(String  _logName) {
		result = new Long(0);
		logName = _logName;
		myRun = 0;
	}

	/** Find the end time for the given logfile	*/
	public void run() {
		try {	  
			GenericLogReader reader = new GenericLogReader(logName, MainWindow.runObject[myRun].getVersion());
			while (true) {
				LogEntryData data = reader.nextEvent();
				if (data.time > result)
					result = data.time;
			}
		} catch (EOFException e) {
			// finished reading the file
		} catch (IOException e) {
			// Some error occured
			System.err.println("Error occurred while reading from " + logName);
		}		
	}

}
