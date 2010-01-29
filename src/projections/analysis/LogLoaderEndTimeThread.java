package projections.analysis;

import java.io.EOFException;
import java.io.IOException;

import projections.gui.MainWindow;
import projections.misc.LogEntryData;


/** This thread's run() method will lookup the endtime for an input log file */
class LogLoaderEndTimeThread  implements Runnable {

	protected Long result;
	private int myRun = 0;
	private int pe;

	protected LogLoaderEndTimeThread(int pe) {
		result = new Long(0);
		this.pe = pe;
	}

	/** Find the end time for the given logfile	*/
	public void run() {
		try {	  
			GenericLogReader reader = new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());
			while (true) {
				LogEntryData data = reader.nextEvent();
				if (data.time > result)
					result = data.time;
			}		
		} catch (EOFException e) {
			// finished reading the file
		} catch (IOException e) {
			// Some error occured, possibly log files were truncated or corrupted, or some file format has changed that we are yet unaware of
		}		
	}

}
