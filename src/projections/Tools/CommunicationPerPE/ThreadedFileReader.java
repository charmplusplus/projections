package projections.Tools.CommunicationPerPE;



import java.util.ArrayList;

import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.gui.MainWindow;
import projections.misc.LogEntryData;


/** The reader threads for Communication Per PE Tool. */
class ThreadedFileReader implements Runnable  {

	private int pe;
	private int pIdx;
	private long startTime;
	private long endTime;
	private int myRun = 0;
	private double[][] sentMsgCount;
	private double[][] sentByteCount;
	private double[][] receivedMsgCount;
	private double[][] receivedByteCount;
	private double[][] exclusiveRecv;
	private double[][] exclusiveBytesRecv;
	private int[][] hopCount;

	public ArrayList<Integer>	localHistogram = new ArrayList<Integer>();

	/** Construct a file reading thread that will generate data for one PE. */
	protected ThreadedFileReader(int pe, int pIdx, long startTime, long endTime, double[][] sentMsgCount, double[][] sentByteCount, double[][] receivedMsgCount, double[][] receivedByteCount, double[][] exclusiveRecv, double[][] exclusiveBytesRecv, int[][] hopCount ){
		this.pe = pe;
		this.pIdx = pIdx;
		this.startTime = startTime;
		this.endTime = endTime;
		
		this.sentMsgCount = sentMsgCount;
		this.sentByteCount = sentByteCount;
		this.receivedMsgCount = receivedMsgCount;
		this.receivedByteCount = receivedByteCount;
		this.exclusiveRecv = exclusiveRecv;
		this.exclusiveBytesRecv = exclusiveBytesRecv;
		this.hopCount = hopCount;
	}


	public void run() { 

		GenericLogReader glr = new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());
		
		int numEPs = MainWindow.runObject[myRun].getNumUserEntries();
		
		try {
			synchronized (sentMsgCount){
				sentMsgCount[pIdx] = new double[numEPs];
				sentByteCount[pIdx] = new double[numEPs];
				receivedMsgCount[pIdx] = new double[numEPs];
				receivedByteCount[pIdx] = new double[numEPs];
				exclusiveRecv[pIdx] = new double[numEPs];
				exclusiveBytesRecv[pIdx] = new double[numEPs];
				if (MainWindow.BLUEGENE) {
					hopCount[pIdx] = new int[numEPs];
				}
			}


			LogEntryData logdata = glr.nextEventOnOrAfter(startTime);
			// we'll just use the EOFException to break us out of
			// this loop :)
			while (true) {
				if (logdata.time > endTime) {
					if ((logdata.type == ProjDefs.CREATION) ||
							(logdata.type == ProjDefs.BEGIN_PROCESSING)) {
						// past endtime. no more to do.
						break;
					}
				}
				logdata = glr.nextEvent();
				if (logdata.type == ProjDefs.CREATION) {
					int EPid = MainWindow.runObject[myRun].getEntryIndex(logdata.entry);
					sentMsgCount[pIdx][EPid]++;
					sentByteCount[pIdx][EPid] += 
						logdata.msglen;
					localHistogram.add(new Integer(logdata.msglen));
				} else if ((logdata.type == ProjDefs.CREATION_BCAST) ||
						(logdata.type == 
							ProjDefs.CREATION_MULTICAST)) {
					int EPid = MainWindow.runObject[myRun].getEntryIndex(logdata.entry);
					sentMsgCount[pIdx][EPid]+= logdata.numPEs;
					sentByteCount[pIdx][EPid] +=
						(logdata.msglen * logdata.numPEs);
				} else if (logdata.type == ProjDefs.BEGIN_PROCESSING) {
					int EPid = MainWindow.runObject[myRun].getEntryIndex(logdata.entry);
					receivedMsgCount[pIdx][EPid]++;
					receivedByteCount[pIdx][EPid] += 
						logdata.msglen;
					// testing if the send was from outside the processor
					if (logdata.pe != pe) {
						exclusiveRecv[pIdx][EPid]++;
						exclusiveBytesRecv[pIdx][EPid] += 
							logdata.msglen;
						if (MainWindow.BLUEGENE) {
							hopCount[pIdx][EPid] +=
								CommWindow.manhattanDistance(pe,logdata.pe);
						}
					}
				}
			}
		} catch (java.io.EOFException e) {
			// Successfully reached end of log file
		} catch (java.io.IOException e) {
			System.out.println("Exception: " +e);
			e.printStackTrace();
		}


	}

}





