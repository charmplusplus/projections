package projections.analysis;

import java.io.IOException;

import projections.gui.MainWindow;
import projections.misc.LogEntry;


/** This thread's run() method will lookup the endtime for an input log file */
class LogLoaderEndTimeThread  implements Runnable {

	protected long result;
	private int myRun = 0;
	private int pe;

	protected LogLoaderEndTimeThread(int pe) {
		result = 0L;
		this.pe = pe;
	}

	/** Find the end time for the given logfile	*/
	public void run() {
		GenericLogReader reader = new GenericLogReader( pe, MainWindow.runObject[myRun].getVersion());

		try {	  
			while (true) {
				LogEntry data = reader.nextEvent();
				if (data.time > result)
					result = data.time;
			}		
		} catch (EndOfLogSuccess e) {
			// finished reading the file
		} catch (IOException e) {
			// Some error occurred, possibly log files were truncated or corrupted, or some file format has changed that we are yet unaware of
		}
		

		try {
			reader.close();
		} catch (IOException e1) {
			System.err.println("Error: could not close log file reader for processor " + pe );
		}
		
	}

}
