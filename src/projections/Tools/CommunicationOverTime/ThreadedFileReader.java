package projections.Tools.CommunicationOverTime;

import java.io.IOException;

import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.gui.MainWindow;
import projections.misc.LogEntryData;


/** The reader threads for Communication over Time Tool. 
 * 
 *  Written by Samir Mirza, Isaac Dooley, and possibly others
 * 
 */
class ThreadedFileReader implements Runnable  {

	private int pe;
	private long startInterval;
	private long endInterval;
	private long intervalSize;

	private int myRun = 0;

    private int PesPerNode = 28;
	// Global data that must be safely accumulated into:
	private double[][] globalMessagesSend;
	private double[][] globalMessagesRecv;
	private double[][] globalBytesSend;
	private double[][] globalBytesRecv;
	private double[][] globalExternalMessageRecv;
	private double[][] globalExternalBytesRecv;
	private double[][] globalExternalNodeMessageRecv;
	private double[][] globalExternalNodeBytesRecv;
	

	/** Construct a file reading thread that will generate data for one PE. */
	protected ThreadedFileReader(int pe, long intervalSize, long startInterval, long endInterval, double[][] globalMessagesSend,double[][] globalMessagesRecv, double[][] globalBytesSend, double [][] globalBytesRecv, double[][] globalExternalMessageRecv, double[][] globalExternalBytesRecv ){
		this.pe = pe;
		this.startInterval = startInterval;
		this.endInterval = endInterval;
		this.intervalSize = intervalSize;

		this.globalMessagesSend = globalMessagesSend;
		this.globalMessagesRecv = globalMessagesRecv;
		this.globalBytesSend = globalBytesSend;
		this.globalBytesRecv = globalBytesRecv;
		this.globalExternalMessageRecv = globalExternalMessageRecv;
		this.globalExternalBytesRecv = globalExternalBytesRecv;
	}

    protected ThreadedFileReader(int pe, long intervalSize, long startInterval, long endInterval, double[][] globalMessagesSend,double[][] globalMessagesRecv, double[][] globalBytesSend, double [][] globalBytesRecv, double[][] globalExternalMessageRecv, double[][] globalExternalBytesRecv,
        double[][] globalExternalNodeMessageRecv, double[][] globalExternalNodeBytesRecv){
		this.pe = pe;
		this.startInterval = startInterval;
		this.endInterval = endInterval;
		this.intervalSize = intervalSize;

		this.globalMessagesSend = globalMessagesSend;
		this.globalMessagesRecv = globalMessagesRecv;
		this.globalBytesSend = globalBytesSend;
		this.globalBytesRecv = globalBytesRecv;
		this.globalExternalMessageRecv = globalExternalMessageRecv;
		this.globalExternalBytesRecv = globalExternalBytesRecv;
		this.globalExternalNodeMessageRecv = globalExternalNodeMessageRecv;
		this.globalExternalNodeBytesRecv = globalExternalNodeBytesRecv;
	}



	public void run() { 

		GenericLogReader reader = new GenericLogReader( pe, MainWindow.runObject[myRun].getVersion());
		//Initialize class variables
		int numEPs = MainWindow.runObject[myRun].getNumUserEntries();
		int numIntervals = (int) (endInterval-startInterval+1);
		double[][] localMessagesSend = new double[numIntervals][numEPs];
		double[][] localMessagesRecv = new double[numIntervals][numEPs];

		double[][] localBytesSend = new double[numIntervals][numEPs];
		double[][] localBytesRecv = new double[numIntervals][numEPs];

		double[][] localExternalMessageRecv = new double[numIntervals][numEPs];
		double[][] localExternalBytesRecv = new double[numIntervals][numEPs];	

		double[][] localExternalNodeMessageRecv = new double[numIntervals][numEPs];
		double[][] localExternalNodeBytesRecv = new double[numIntervals][numEPs];	

		try	{

			while(true){
				LogEntryData logdata = reader.nextEvent();

				//Now we have entered into the time interval

				if (logdata.type == ProjDefs.CREATION) {  // Message being sent
					int destEP = logdata.entry;
					int timeInterval = getInterval(logdata.time);

					// Update message and byte sent arrays
					if(timeInterval >= 0 && timeInterval < numIntervals){
						localMessagesSend[timeInterval][destEP]++;
						localBytesSend[timeInterval][destEP]+=logdata.msglen;
					}
					// May implement Sent External later on
				} else if ((logdata.type ==  ProjDefs.CREATION_BCAST) ||
						(logdata.type ==  ProjDefs.CREATION_MULTICAST)) {
					int destEP = logdata.entry;
					int timeInterval = getInterval(logdata.time);
					if(timeInterval >= 0 && timeInterval < numIntervals){
						localMessagesSend[timeInterval][destEP] += logdata.numPEs;
						localBytesSend[timeInterval][destEP] +=	(logdata.msglen * logdata.numPEs);
					}
				} else if (logdata.type ==  ProjDefs.BEGIN_PROCESSING) {  // Starting new entry method
					int currEPindex = MainWindow.runObject[myRun].getEntryIndex(logdata.entry);
					int srcPe = logdata.pe;
					int timeInterval = getInterval(logdata.time);
					if(timeInterval >= 0 && timeInterval < numIntervals){
						// Update message and byte received arrays
						localMessagesRecv[timeInterval][currEPindex]++;
						localBytesRecv[timeInterval][currEPindex]+=logdata.msglen;

						if (pe != srcPe) {
							// Update message and byte received external arrays
							localExternalMessageRecv[timeInterval][currEPindex]++;
							localExternalBytesRecv[timeInterval][currEPindex]+=logdata.msglen;
						}
                        if(pe/PesPerNode != srcPe/PesPerNode)
                        {
                            // Update message and byte received external arrays
							localExternalNodeMessageRecv[timeInterval][currEPindex]++;
							localExternalNodeBytesRecv[timeInterval][currEPindex]+=logdata.msglen;

                        }
					}
				}

			}

		} catch (EndOfLogSuccess e) {
			// Successfully reached end of log file
			
			double intervalSizeMs = intervalSize/1000.0;
			// Calculate the rate using milliseconds
			for (int interval = 0; interval < numIntervals; interval++) {
				for (int ep = 0; ep < numEPs; ep++) {
					localMessagesSend[interval][ep] /= intervalSizeMs;
					localMessagesRecv[interval][ep] /= intervalSizeMs;

					localBytesSend[interval][ep] /= intervalSizeMs;
					localBytesRecv[interval][ep] /= intervalSizeMs;

					localExternalMessageRecv[interval][ep] /= intervalSizeMs;
					localExternalBytesRecv[interval][ep] /= intervalSizeMs;

					localExternalNodeMessageRecv[interval][ep] /= intervalSizeMs;
					localExternalNodeBytesRecv[interval][ep] /= intervalSizeMs;
				}
			}
		} catch (java.io.IOException e) {
			System.out.println("Exception: " +e);
			e.printStackTrace();
		}

		try {
			reader.close();
		} catch (IOException e1) {
			System.err.println("Error: could not close log file reader for processor " + pe );
		}

		// Accumulate into global results. This must be done safely as many threads will all use these same arrays
		synchronized (globalMessagesSend) {
			for(int i=0; i< numIntervals; i++){
				for(int j=0; j< numEPs; j++){
					globalMessagesSend[i][j] += localMessagesSend[i][j];
				}
			}
		}
		
		synchronized (globalMessagesRecv) {
			for(int i=0; i< numIntervals; i++){
				for(int j=0; j< numEPs; j++){
					globalMessagesRecv[i][j] += localMessagesRecv[i][j];
				}
			}
		}
		
		synchronized (globalBytesSend) {
			for(int i=0; i< numIntervals; i++){
				for(int j=0; j< numEPs; j++){
					globalBytesSend[i][j] += localBytesSend[i][j];
				}
			}
		}
		
		synchronized (globalBytesRecv) {
			for(int i=0; i< numIntervals; i++){
				for(int j=0; j< numEPs; j++){
					globalBytesRecv[i][j] += localBytesRecv[i][j];
				}
			}
		}
		
		synchronized (globalExternalMessageRecv) {
			for(int i=0; i< numIntervals; i++){
				for(int j=0; j< numEPs; j++){
					globalExternalMessageRecv[i][j] += localExternalMessageRecv[i][j];
				}
			}
		}
		
		synchronized (globalExternalBytesRecv) {
			for(int i=0; i< numIntervals; i++){
				for(int j=0; j< numEPs; j++){
					globalExternalBytesRecv[i][j] += localExternalBytesRecv[i][j];
				}
			}
		}
		
        synchronized (globalExternalNodeMessageRecv) {
			for(int i=0; i< numIntervals; i++){
				for(int j=0; j< numEPs; j++){
					globalExternalNodeMessageRecv[i][j] += localExternalNodeMessageRecv[i][j];
				}
			}
		}
		
		synchronized (globalExternalNodeBytesRecv) {
			for(int i=0; i< numIntervals; i++){
				for(int j=0; j< numEPs; j++){
					globalExternalNodeBytesRecv[i][j] += localExternalNodeBytesRecv[i][j];
				}
			}
		}
		

		
	}



	private int getInterval(long currTime)
	{
		long ginterval = currTime/intervalSize;
		return (int) (ginterval - startInterval);
	}   



}





