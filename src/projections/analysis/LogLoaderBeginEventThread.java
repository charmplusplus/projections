package projections.analysis;

import java.io.IOException;

import projections.gui.MainWindow;
import projections.misc.LogEntryData;
import projections.analysis.ProjDefs;


/** This thread's run() method will lookup the begin computation time for an input log file */
class LogLoaderBeginEventThread  implements Runnable {

	protected Long result;
	private int myRun = 0;
	private int pe;

	protected LogLoaderBeginEventThread(int pe) {
		result = new Long(Long.MAX_VALUE);
		this.pe = pe;
	}

	/** Find the earliest time for the given logfile	*/
	public void run() {
		GenericLogReader reader = new GenericLogReader( pe, MainWindow.runObject[myRun].getVersion());

		try {	  
			while (true) {
				LogEntryData data = reader.nextEvent();
				if (data.isBeginType() && (data.time < result))
				{
					result = data.time;
					break;
				}
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
