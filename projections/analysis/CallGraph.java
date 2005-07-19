package projections.analysis;

import projections.misc.*;
import projections.gui.*;
import projections.analysis.*;

import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Array;
import java.util.ArrayList;

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
	numEPs = Analysis.getNumUserEntries();
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
	LogEntryData logdata = new LogEntryData();
	
	int currPeArrayIndex = 0;
	int currPe;
	
        ProgressMonitor progressBar =
	    new ProgressMonitor(parent, "Reading log files",
				"", 0, numPe);
	
	while (peList.hasMoreElements()) {
	    currPe = peList.nextElement();
	    if (!progressBar.isCanceled()) {
		progressBar.setNote("Reading data for PE " + currPe);
		progressBar.setProgress(currPeArrayIndex+1);
	    }
	    else {
		progressBar.close();
		break;
	    }
	    LogFile = new GenericLogReader(Analysis.getLogName(currPe), Analysis.getVersion());
	
	    try
	    {
		LogFile.nextEventOnOrAfter(startTime, logdata);

		//Now we have entered into the time interval
		while (logdata.time < endTime) {
		    
		    if (logdata.type == CREATION) {  // Message being sent
		        int destEP = logdata.entry;
			int currTimeInterval = getInterval(logdata.time);
			
			// Update message and byte sent arrays
			messageArray[currTimeInterval][destEP]++;
			byteArray[currTimeInterval][destEP]+=logdata.msglen;
			
			// May implement Sent External later on
		    }

		    else if (logdata.type == BEGIN_PROCESSING) {  // Starting new entry method
		        int currEP = logdata.entry;
		        int srcPe = logdata.pe;
		        int currTimeInterval = getInterval(logdata.time);
			
			// Update message and byte received arrays
			messageArray[currTimeInterval][currEP+numEPs]++;
			byteArray[currTimeInterval][currEP+numEPs]+=logdata.msglen;
			
			if (currPe != srcPe) {
			    // Update message and byte received external arrays
			    externalMessageArray[currTimeInterval][currEP+numEPs]++;
			    externalByteArray[currTimeInterval][currEP+numEPs]+=logdata.msglen;
			}
		    }
		    
		    LogFile.nextEvent(logdata);
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
    
/*
    // Old Algorithm focusing on sourceEP
		while (logdata.time < endTime) {
		    
		    if (logdata.type == CREATION) {  // CREATION encountered outside a PROCESSING block
		        // Logic here?
			
			// Need to somehow update message and byte sent external arrays
			
			// Need to account for case where creations are actually followed
			// by an END_PROCESSING and are thus part of a block
		    }

		    else if (logdata.type == BEGIN_PROCESSING) {  // Starting new entry point
		        currEP = logdata.entry;
		        srcPe = logdata.pe;
		        currTimeInterval = getInterval(logdata.time);
			
			// Update message and byte received arrays
			messageArray[currTimeInterval][currEP+numEPs]++;
			byteArray[currTimeInterval][currEP+numEPs]+=logdata.msglen;
			
			if (currPe != srcPe) {
			    // Update message and byte received external arrays
			    externalMessageArray[currTimeInterval][currEP+numEPs]++;
			    externalByteArray[currTimeInterval][currEP+numEPs]+=logdata.msglen;
			}
			
		        LogFile.nextEvent(logdata);
			
			while ( (logdata.type!=END_PROCESSING)&&(logdata.type!=END_COMPUTATION)&&
			        (logdata.time<endTime) ) {			    
			    if (logdata.type == CREATION) {
			        // Update message and byte sent arrays
				currTimeInterval = getInterval(logdata.time);
			        messageArray[currTimeInterval][currEP]++;
				byteArray[currTimeInterval][currEP] += logdata.msglen;
				
				// Need to somehow update message and byte sent external arrays
			    }
			    LogFile.nextEvent(logdata);
		        }
		    }
		    
		    LogFile.nextEvent(logdata);
		}
*/

/*        
    // Outdated
    public void PrintOutput(double[][] arr)
    {
        try
	{
    	    //Write array to file
            BufferedWriter output = new BufferedWriter(new FileWriter("output.txt"));
	    output.write("CALLGRAPH OUTPUT FOR " + "..." + ".sts -\n");
            for (int i=0; i<arr.length; i++) {
	        output.write("\n");
	        output.write("Activity in time interval " + (startInterval+intervalSize*i) +
		             "-" + ( (startInterval+intervalSize*i)+intervalSize-1 ) + ":\n");
		output.write("    Source EP's:\n");
		for (int j=0; j<arr[0].length/2; j++) {
		    if (arr[i][j] > 0)
		        output.write("        EP #" + j + " sent " + arr[i][j] + " messages\n");
	        }
		output.write("    Destination EP's:\n");
		for (int j=arr[0].length/2; j<arr[0].length; j++) {
		    if (arr[i][j] > 0)
		        output.write("        EP #" + (j-arr[0].length/2) +
			             " received " + arr[i][j] + " messages\n");
	        }
	    }
            output.close();
	}
        catch (IOException e)
	{
	}
    }
*/
    

/*
    // first callgraph
    private void GatherData()
    {
        try
	{
	    //Go through each log file and for each Creation, mark
	    //  the Source EP and the Destination EP in the array
	    LogEntryData data = new LogEntryData();
	    int sourceEP;
	    for (int i=0; i<numPe; i++) {
	        GenericLogReader LogFile = new GenericLogReader(StsFile.getLogName(i), 
		                                                Analysis.getVersion());
		while (true) {
		    LogFile.nextEvent(data);
		    if (data.type == BEGIN_PROCESSING) {
		        //Starting new entry method
			sourceEP = data.entry;
		        LogFile.nextEvent(data);
			while ( (data.type != END_PROCESSING) &&
			        (data.type != END_COMPUTATION) ) {
			    if (data.type == CREATION)
			        arr[sourceEP][data.entry]++;
			    LogFile.nextEvent(data);
		        }
		    }
		    if (data.type == END_COMPUTATION) {
		        //End of log file
		        break;
		    }
		}
	    }
	    
	    PrintOutput();
	}
	catch (IOException e) {}
    }
*/

/*        
    public static void main(String[] args)
    {
	if (args.length != 4)
	    System.err.println("Error! Please specify the correct parameters");
	else {
	    File sourceFile = new File(args[0]);
	    if (sourceFile.canRead() == false)
	        System.err.println("Error! Sts File does not exist!");
	    else
                new CallGraph(args[0], Integer.parseInt(args[1]),
		              Integer.parseInt(args[2]), Integer.parseInt(args[3]));
	}
    }
*/
