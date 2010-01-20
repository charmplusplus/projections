package projections.Testing;


import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.gui.MainWindow;
import projections.misc.LogEntryData;
import java.io.EOFException;


/** The reader threads for Scan Logs tool. */
class ThreadedFileReader extends Thread  {

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


	double fakeCounter;

	private void LoadGraphDataForOnePe(int pe) 
	{
		try {	  
			GenericLogReader reader = new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());
			fakeCounter = 0.0;

			while (true) {
				LogEntryData data = reader.nextEvent();

				fakeCounter += data.time;

			}

		} catch (EOFException e) {			
			// I guess we are done now

			synchronized(result){
				result[0] += fakeCounter;
			}

		}catch (Exception e) {
			System.err.println("Error occured while reading data for pe " + pe);
		}

	}


}





