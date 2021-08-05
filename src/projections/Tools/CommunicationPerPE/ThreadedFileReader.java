package projections.Tools.CommunicationPerPE;



import java.io.IOException;
import java.util.ArrayList;

import projections.Tools.ThreadedFileReaderBase;
import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.gui.MainWindow;
import projections.misc.LogEntry;
import projections.analysis.StsReader;

/** The reader threads for Communication Per PE Tool. */
class ThreadedFileReader extends ThreadedFileReaderBase implements Runnable  {

	private int pe;
	private int pIdx;
	private long startTime;
	private long endTime;
	private double[][] sentMsgCount;
	private double[][] sentByteCount;
	private double[][] receivedMsgCount;
	private double[][] receivedByteCount;
	private double[][] externalRecv;
	private double[][] externalBytesRecv;
	private int[][] hopCount;
	
	private boolean isCommThd;

	public ArrayList<Integer>	localHistogram = new ArrayList<Integer>();

	/** Construct a file reading thread that will generate data for one PE. */
	protected ThreadedFileReader(int pe, int pIdx, long startTime, long endTime, double[][] sentMsgCount, double[][] sentByteCount, double[][] receivedMsgCount, double[][] receivedByteCount, double[][] externalRecv, double[][] externalBytesRecv, int[][] hopCount ){
		this.pe = pe;
		this.pIdx = pIdx;
		this.startTime = startTime;
		this.endTime = endTime;
		
		this.sentMsgCount = sentMsgCount;
		this.sentByteCount = sentByteCount;
		this.receivedMsgCount = receivedMsgCount;
		this.receivedByteCount = receivedByteCount;
		this.externalRecv = externalRecv;
		this.externalBytesRecv = externalBytesRecv;
		this.hopCount = hopCount;
		
		StsReader sts = MainWindow.runObject[myRun].getSts();
		int totalPes = sts.getProcessorCount();
		int totalNodes = sts.getSMPNodeCount();
		int nodesize = sts.getNodeSize();
		if(pe>=totalNodes*nodesize && pe<totalPes)
			isCommThd = true;
		else
			isCommThd = false;
	}


	public void run() { 

		GenericLogReader reader = new GenericLogReader( pe, MainWindow.runObject[myRun].getVersion());
		
		int numEPs = MainWindow.runObject[myRun].getNumUserEntries();
		
		try {
			synchronized (sentMsgCount){
				sentMsgCount[pIdx] = new double[numEPs];
				sentByteCount[pIdx] = new double[numEPs];
				receivedMsgCount[pIdx] = new double[numEPs];
				receivedByteCount[pIdx] = new double[numEPs];
				externalRecv[pIdx] = new double[numEPs];
				externalBytesRecv[pIdx] = new double[numEPs];
				if (MainWindow.BLUEGENE) {
					hopCount[pIdx] = new int[numEPs];
				}
			}

			LogEntry logdata = reader.nextEventOnOrAfter(startTime);
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
				if (logdata.type == ProjDefs.CREATION) {
					int EPid = MainWindow.runObject[myRun].getEntryIndex(logdata.entry);					
					sentMsgCount[pIdx][EPid]++;
					sentByteCount[pIdx][EPid] += 
						logdata.msglen;
					localHistogram.add(logdata.msglen);
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
						externalRecv[pIdx][EPid]++;
						externalBytesRecv[pIdx][EPid] +=
							logdata.msglen;
						if (MainWindow.BLUEGENE) {
							hopCount[pIdx][EPid] +=
								CommWindow.manhattanDistance(pe,logdata.pe);
						}
					}
					
					if(isCommThd){
						//process the messages' count for communication threads
						//as the messages to be sent are treated same as messages
						//that are recved. Such emulation of message recv is used to track
						//the time taken by comm thread to call the underlying system's 
						//send operation and when this message is going to be sent by
						//comm thread. An example of this is MPI-SMP comm thread
						//trace. So we have to subtract those msgs that are sent to external
						//charm smp nodes. -Chao Mei
						int pcreation = logdata.pe;						
						if(pcreation!=pe && isSameNode(pe, pcreation)){
							receivedMsgCount[pIdx][EPid]--;
							receivedByteCount[pIdx][EPid] -= logdata.msglen;
						}						
					}

				}
				logdata = reader.nextEvent();
			}
			/**/
		} catch (EndOfLogSuccess e) {
			// Successfully reached end of log file, divide counts by the time interval to compute rates
			double timeInterval = (endTime - startTime)/1000.0;

			for (int ep = 0; ep < numEPs; ep++) {
				sentMsgCount[pIdx][ep] /= timeInterval;
				sentByteCount[pIdx][ep] /= timeInterval;
				receivedMsgCount[pIdx][ep] /= timeInterval;
				receivedByteCount[pIdx][ep] /= timeInterval;
				externalRecv[pIdx][ep] /= timeInterval;
				externalBytesRecv[pIdx][ep] /= timeInterval;
			}
		} catch (IOException e) {
			System.out.println("Exception: " +e);
			e.printStackTrace();
		}


		try {
			reader.close();
		} catch (IOException e1) {
			System.err.println("Error: could not close log file reader for processor " + pe );
		}
	}

}





