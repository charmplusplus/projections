package projections.Tools.PerformanceCounters;


import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.gui.MainWindow;
import projections.misc.LogEntry;

import java.io.IOException;
import java.util.ArrayList;

/** The reader threads for Communication Per PE Tool. */
class ThreadedFileReader implements Runnable  {

	private int pe;
	private int pIdx;
	private long startTime;
	private long endTime;
	private int myRun = 0;
	private int numPerfCounts = 0;
	private double[][][] perfCounters;

	/** Construct a file reading thread that will generate data for one PE. */
	protected ThreadedFileReader(int pe, int pIdx, int numPerfCounts, long startTime, long endTime, double[][][] perfCounters) {
		this.pe = pe;
		this.pIdx = pIdx;
		this.numPerfCounts = numPerfCounts;
		this.startTime = startTime;
		this.endTime = endTime;
		this.perfCounters = perfCounters;
	}

	public void run() {
		GenericLogReader reader = new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());

		int numEPs = MainWindow.runObject[myRun].getNumUserEntries();
		try {
			synchronized (perfCounters) {
				for (int i = 0; i < numPerfCounts; ++i) {
					perfCounters[i][pIdx] = new double[numEPs];
				}
			}

			LogEntry logdata = reader.nextEventOnOrAfter(startTime);
			// Get the last open begin event if it's of type BEGIN_PROCESSING
			LogEntry lastBegin = reader.getLastOpenBE();
			if (lastBegin != null && lastBegin.type != ProjDefs.BEGIN_PROCESSING) {
				lastBegin = null;
			}
			// we'll just use the EndOfLogException to break us out of
			// this loop :)
			while (true) {
				if (logdata.time > endTime) {
					if ((logdata.type == ProjDefs.CREATION) ||
							(logdata.type == ProjDefs.BEGIN_PROCESSING)) {
						// past endtime. no more to do.
						break;
					}
				}
				if (logdata.type == ProjDefs.END_PROCESSING) {
					int EPid = MainWindow.runObject[myRun].getEntryIndex(logdata.entry);
					for (int i = 0; i < numPerfCounts; ++i) {
						// Note that reader.getLastOpenBE() cannot be used here because when an END_PROCESSING event is
						// read, it closes the last BEGIN_PROCESSING event, so that function will return null since
						// no open event exists anymore. Thus, we track it using lastBegin instead.
						perfCounters[i][pIdx][EPid] += logdata.perfCounts[i] - lastBegin.perfCounts[i];
					}
				} else if (logdata.type == ProjDefs.BEGIN_PROCESSING) {
					lastBegin = logdata;
				}
				logdata = reader.nextEvent();
			}
		} catch (EndOfLogSuccess e) {
			// Successfully reached end of log file
		} catch (IOException e) {
			System.out.println("Exception: " + e);
			e.printStackTrace();
		}

		try {
			reader.close();
		} catch (IOException e1) {
			System.err.println("Error: could not close log file reader for processor " + pe);
		}
	}
}
