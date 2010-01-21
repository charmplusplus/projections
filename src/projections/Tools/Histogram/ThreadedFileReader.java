package projections.Tools.Histogram;


import java.io.EOFException;

import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.gui.MainWindow;
import projections.misc.LogEntryData;




/** The reader threads for Histogram tool. */
class ThreadedFileReader extends Thread  {

	private int pe;
	private long startTime;
	private long endTime;
	private int myRun = 0;

	private int timeNumBins;
	private long timeBinSize;
	private long timeMinBinSize;
	private int msgNumBins;
	private long msgBinSize;
	private long msgMinBinSize;

	private double [][][] outputCounts;

	/** Construct a file reading thread that will generate histogram data for one PE. */
	protected ThreadedFileReader(double[][][] outputCounts, int pe, long startTime, long endTime, int timeNumBins, long timeBinSize, long timeMinBinSize, int msgNumBins, long msgBinSize, long msgMinBinSize){
		this.pe = pe;
		this.startTime = startTime;
		this.endTime = endTime;
		this.timeNumBins = timeNumBins;
		this.timeBinSize = timeBinSize;
		this.timeMinBinSize = timeMinBinSize;
		this.msgNumBins = msgNumBins;
		this.msgBinSize = msgBinSize;
		this.msgMinBinSize = msgMinBinSize;
		this.outputCounts = outputCounts;	
	}


	public void run() { 
		double [][][] myCounts = getCounts();

		// in synchronized manner accumulate into global counts:
		synchronized (outputCounts) {
			for(int i=0; i< outputCounts.length; i++){
				for(int j=0; j<outputCounts[i].length; j++){
					for(int k=0; k<outputCounts[i][j].length; k++){
						outputCounts[i][j][k] += myCounts[i][j][k];
					}
				}
			}
		}
		myCounts = null;
	}



	private double[][][] getCounts()
	{
		// Variables for use with the analysis
		long executionTime;
		long adjustedTime;
		long adjustedSize;

		int numEPs = MainWindow.runObject[myRun].getNumUserEntries();

		GenericLogReader r;
		double[][][] countData = new double[HistogramWindow.NUM_TYPES][][];

		// we create an extra bin to hold overflows.
		countData[HistogramWindow.TYPE_TIME] = new double[timeNumBins+1][numEPs];
		countData[HistogramWindow.TYPE_MSG_SIZE] = new double[msgNumBins+1][numEPs];


		// for maintaining "begin" entries for the data type we
		// wish to monitor.
		LogEntryData[] typeLogs = new LogEntryData[HistogramWindow.NUM_TYPES];
		for (int i=0; i<HistogramWindow.NUM_TYPES; i++) {
			typeLogs[i] = new LogEntryData();
		}
		// for maintaining interval-based events 
		boolean[] isActive = new boolean[HistogramWindow.NUM_TYPES];

		int curPeCount = 0;


		curPeCount++;
		r = new GenericLogReader(pe,
				MainWindow.runObject[myRun].getVersion());
		try {
			LogEntryData logdata = r.nextEventOnOrAfter(startTime);
			boolean done = false;
			while (!done) {
				switch (logdata.type) {
				case ProjDefs.BEGIN_PROCESSING:
					// NOTE: If prior BEGIN never got terminated,
					// simply drop the data given current tracing
					// scheme (ie. do nothing)
					if (logdata.time > endTime) {
						done = true;
					} else {
						// swap logdata (ie. note the BEGIN event)
						LogEntryData tmpLogPtr = logdata;
						logdata = typeLogs[HistogramWindow.TYPE_TIME];
						typeLogs[HistogramWindow.TYPE_TIME] = tmpLogPtr;
						isActive[HistogramWindow.TYPE_TIME] = true;
					}
					break;
				case ProjDefs.END_PROCESSING:
					if (!isActive[HistogramWindow.TYPE_TIME]) {
						// NOTE: No corresponding BEGIN, so this
						// instance of END must be ignored given
						// current tracing scheme.
						if (logdata.time > endTime) {
							done = true;
						}
						break;
					} else {
						if (logdata.entry != typeLogs[HistogramWindow.TYPE_TIME].entry) {
							// The events are mismatched! Clear all.
							// Possible under current tracing scheme.
							isActive[HistogramWindow.TYPE_TIME] = false;
							break;
						}
						// NOTE: Even if the END event happens past 
						// the range, it is recorded as the proper 
						// execution time of the event.
						executionTime = 
							logdata.time - typeLogs[HistogramWindow.TYPE_TIME].time;
						adjustedTime = executionTime - timeMinBinSize;
						// respect user threshold
						if (adjustedTime >= 0) {
							int targetBin = 
								(int)(adjustedTime/timeBinSize);
							if (targetBin >= timeNumBins) {
								targetBin = timeNumBins;
							}
							countData[HistogramWindow.TYPE_TIME][targetBin][logdata.entry] += 1.0;
						}
						isActive[HistogramWindow.TYPE_TIME] = false;
					}
					break;
				case ProjDefs.CREATION:
					if (logdata.time > endTime) {
						break;
					}
					// respect the user threshold.
					adjustedSize = logdata.msglen - msgMinBinSize;
					if (adjustedSize >= 0) {
						int targetBin = (int)(adjustedSize/msgBinSize);
						if (targetBin >= msgNumBins) {
							targetBin = msgNumBins;
						}
						countData[HistogramWindow.TYPE_MSG_SIZE][targetBin][logdata.entry]+=1.0;
					}
					break;
				}
				if (!done) {
					logdata = r.nextEvent();
				}
			}
		} catch(EOFException e) {
			// do nothing just reached end-of-file
		} catch(Exception e) {
			System.err.println("Exception " + e);
			e.printStackTrace();
			System.exit(-1);
		}

		return countData;
	}


}





