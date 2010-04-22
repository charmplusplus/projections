package projections.Testing;



import java.io.IOException;

import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.gui.MainWindow;
import projections.misc.LogEntryData;


/** The reader threads for Scan Logs tool. */
class ThreadedFileReader implements Runnable  {

	private int pe;
	private int myRun;
	private double[] result;

	protected ThreadedFileReader(int pe, int myRun, double [] result){
		this.pe = pe;
		this.myRun = myRun;
		this.result = result;
	}


	public void run() { 
		LoadGraphDataForOnePe(pe);
	}


	private double fakeCounter;

	private void LoadGraphDataForOnePe(int pe) 
	{
		GenericLogReader reader = new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());

		try {	  
			fakeCounter = 0.0;
			while (true) {
				LogEntryData data = reader.nextEvent();
				fakeCounter += data.time;
			}

		} catch (EndOfLogSuccess e) {			
			// Successfully read log file
		} catch (Exception e) {
			System.err.println("Error occured while reading data for pe " + pe);
		}

		try {
			reader.close();
		} catch (IOException e1) {
			System.err.println("Error: could not close log file reader for processor " + pe );
		}		

		synchronized(result){
			result[0] += fakeCounter;
		}
		
		
	}


}





