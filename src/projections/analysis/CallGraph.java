package projections.analysis;

import java.awt.Component;
import java.io.IOException;

import javax.swing.ProgressMonitor;

import projections.gui.MainWindow;
import projections.gui.OrderedIntList;
import projections.misc.LogEntryData;

/**
 *  Written by Samir Mirza
 *  6/29/2005
 *  7/18/2005 Editted
 *
 *  CallGraph produces 2D arrays detailing messages sent between entry methods.
 *  First half of each array deals with sends, second half deals with receives.
 *  Used by CallGraphWindow.java in gui folder.
 *
 */

public class CallGraph extends ProjDefs
{
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private int myRun = 0;

	private int numPe;		             //Number of processors
	private int numEPs;	   	             //Number of entry methods
	private double[][] messageArray;         //Array for EP message count to be stored
	private double[][] byteArray;            //Array for EP message byte count to be stored
	private double[][] externalMessageArray; //Array for external EP message count to be stored
	private double[][] externalByteArray;    //Array for external EP byte count to be stored
	private long startTime;	             //Interval begin
	private long endTime;	             //Interval end
	private OrderedIntList peList;           //List of processors


	public CallGraph(int startInterval, int endInterval, 
			long intervalSize, OrderedIntList processorList)
	{
		//Initialize class variables
		peList = processorList;
		numPe = peList.size();
		numEPs = MainWindow.runObject[myRun].getNumUserEntries();
		startTime = startInterval*intervalSize;
		endTime = endInterval*intervalSize;
		messageArray = new double[endInterval-startInterval+1][numEPs*2];
		byteArray = new double[endInterval-startInterval+1][numEPs*2];
		externalMessageArray = new double[endInterval-startInterval+1][numEPs*2];
		externalByteArray = new double[endInterval-startInterval+1][numEPs*2];	
	}

	public void GatherData(Component parent)
	{
		GenericLogReader LogFile;
		LogEntryData logdata;

		int currPeArrayIndex = 0;
		int currPe;

		ProgressMonitor progressBar =
			new ProgressMonitor(parent, "Reading log files",
					"", 0, numPe);

		while (peList.hasMoreElements()) {
			currPe = peList.nextElement();
			if (!progressBar.isCanceled()) {
				progressBar.setNote("[PE: " + currPe + "] Reading data.");
				progressBar.setProgress(currPeArrayIndex+1);
			}
			else {
				progressBar.close();
				break;
			}
			LogFile = new GenericLogReader(currPe, MainWindow.runObject[myRun].getVersion());

			try
			{
				logdata = LogFile.nextEventOnOrAfter(startTime);

				//Now we have entered into the time interval
				while (logdata.time < endTime) {

					if (logdata.type == CREATION) {  // Message being sent
						int destEP = logdata.entry;
						int currTimeInterval = getInterval(logdata.time);

						// Update message and byte sent arrays
						messageArray[currTimeInterval][destEP]++;
						byteArray[currTimeInterval][destEP]+=logdata.msglen;

						// May implement Sent External later on
					} else if ((logdata.type == CREATION_BCAST) ||
							(logdata.type == CREATION_MULTICAST)) {
						int destEP = logdata.entry;
						int currTimeInterval = getInterval(logdata.time);
						messageArray[currTimeInterval][destEP] += 
							logdata.numPEs;
						byteArray[currTimeInterval][destEP] +=
							(logdata.msglen * logdata.numPEs);
					} else if (logdata.type == BEGIN_PROCESSING) {  // Starting new entry method
						int currEPindex = MainWindow.runObject[myRun].getEntryIndex(logdata.entry);
						int srcPe = logdata.pe;
						int currTimeInterval = getInterval(logdata.time);

						// Update message and byte received arrays
						messageArray[currTimeInterval][currEPindex+numEPs]++;
						byteArray[currTimeInterval][currEPindex+numEPs]+=logdata.msglen;

						if (currPe != srcPe) {
							// Update message and byte received external arrays
							externalMessageArray[currTimeInterval][currEPindex+numEPs]++;
							externalByteArray[currTimeInterval][currEPindex+numEPs]+=logdata.msglen;
						}
					}

					logdata = LogFile.nextEvent();
				}
			}
			catch (IOException e)
			{
			}
			currPeArrayIndex++;
		}
		progressBar.close();
	}

	public double[][] getMessageArray()
	{
		return messageArray;
	}

	public double[][] getByteArray()
	{
		return byteArray;
	}

	public double[][] getExternalMessageArray()
	{
		return externalMessageArray;
	}

	public double[][] getExternalByteArray()
	{
		return externalByteArray;
	}

	private int getInterval(long currTime)
	{
		//Return correct place in the array based on current time stamp
		float x = currTime-startTime;
		x = x/(endTime-startTime);
		return ( (int)(x*messageArray.length) );
	}   
}