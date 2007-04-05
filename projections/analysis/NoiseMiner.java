package projections.analysis;

import projections.misc.*;
import projections.gui.*;
import projections.analysis.*;

import java.io.*;
import java.util.*;
import java.text.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 *  @class NoiseMiner
 *  @author Isaac Dooley
 *  
 *  A stream mining algorithm that will attempt to find patterns indicating OS Interference 
 *  or Computational Noise. These can occur in entry methods with abnormally high durations
 *  or between events where there are gaps. We will attempt to distinguish between processors
 *  and sources of noise based on the durations and occurence rates.
 *  
 *  In order to be fast we will use little memory, and only take a single pass through the 
 *  provided data window.
 *  
 */

public class NoiseMiner extends ProjDefs
{
	
    private int numPe;		     //Number of processors
    private int numEPs;	   	     //Number of entry methods
    private double[][] byteSum;	     //Array for EP message byte sum to be stored
    private double[][] msgCount;     //Array for EP message count to be stored
    private double[][] sumSquares;   //Array for EP sum of byte counts squared
    private int[][] minStats;	     //Array for EP min stats to be stored
    private int[][] maxStats;	     //Array for EP max stats to be stored
    private double[][] varStats;     //Array for EP variance stats to be stored
    private boolean[] exists;	     //Array to remember if sourceEP sent any messages
    private long startTime;	     //Interval begin
    private long endTime;	     //Interval end
    private OrderedIntList peList;   //List of processors
    private DecimalFormat _format;   //Format for output
    
	
    public NoiseMiner(long startInterval, long endInterval, 
                     OrderedIntList processorList)
    {
        //Initialize class variables
		peList = processorList;
		numPe = peList.size();
		numEPs = Analysis.getNumUserEntries();
		startTime = startInterval;
		endTime = endInterval;
		byteSum = new double[numEPs][numEPs];
		msgCount = new double[numEPs][numEPs];
		sumSquares = new double[numEPs][numEPs];
		minStats = new int[numEPs][numEPs];
		maxStats = new int[numEPs][numEPs];
		varStats = new double[numEPs][numEPs];
		exists = new boolean[numEPs];
        _format = new DecimalFormat("###,###.###");
    }
    
    public void GatherData(Component parent)
    {
        GenericLogReader LogFile;
		LogEntryData logdata = new LogEntryData();
		
		int currPeIndex = 0;
		int currPe;
		int sourceEP;
	
        ProgressMonitor progressBar =
	    new ProgressMonitor(parent, "Mining for Computational Noise","", 0, numPe);
	
	while (peList.hasMoreElements()) {
	    currPe = peList.nextElement();
	    if (!progressBar.isCanceled()) {
		progressBar.setNote("[PE: " + currPe + " ] Reading data.");
		progressBar.setProgress(currPeIndex+1);
	    }
	    else {
		progressBar.close();
		break;
	    }
	    LogFile = new GenericLogReader(Analysis.getLogName(currPe), 
	                                   Analysis.getVersion());    
	   
            try
	    {
		LogFile.nextEventOnOrAfter(startTime, logdata);
		//Now we have entered into the time interval

		Stack creationStack = new Stack();
		while ( (logdata.time<endTime)&&(logdata.type!=BEGIN_PROCESSING) ) {
		    //Account for any Creations encountered after a BP but before an EP
		    // Basically, the start interval begins inside a Processing Block
		    if (logdata.type == CREATION) {
			creationStack.push(new Integer(logdata.entry));
			creationStack.push(new Integer(logdata.msglen));
		    }
		    if (logdata.type == END_PROCESSING) {
		        sourceEP = logdata.entry;
			while(!creationStack.empty()) {
			    int msglen = ((Integer)creationStack.pop()).intValue();
			    int destEP = ((Integer)creationStack.pop()).intValue();
			    if ( (minStats[sourceEP][destEP]>msglen) ||
			         (minStats[sourceEP][destEP]==0) )
			        minStats[sourceEP][destEP] = msglen;
			    if (maxStats[sourceEP][destEP]<msglen)
			        maxStats[sourceEP][destEP] = msglen;
			    msgCount[sourceEP][destEP]++;
			    byteSum[sourceEP][destEP]+=(double)msglen;
			    sumSquares[sourceEP][destEP]+=(double)msglen*(double)msglen;
			    if (!exists[sourceEP])
			        exists[sourceEP]=true;
			}
			LogFile.nextEvent(logdata);
			break;
		    }
		    LogFile.nextEvent(logdata);
		}

		while (logdata.time < endTime) {
	            //Go through each log file and for each Creation
		    //  in a Processing Block, update arrays
		    if (logdata.type == BEGIN_PROCESSING) {
		        //Starting new entry method
			sourceEP = logdata.entry;
		        LogFile.nextEvent(logdata);
			while ( (logdata.type != END_PROCESSING) &&
			        (logdata.type != END_COMPUTATION) &&
				(logdata.time < endTime) ) {
			    if (logdata.type == CREATION) {
			        int msglen = logdata.msglen;
			        int destEP = logdata.entry;
			        if ( (minStats[sourceEP][destEP]>msglen) ||
			             (minStats[sourceEP][destEP]==0) )
			            minStats[sourceEP][destEP] = msglen;
			        if (maxStats[sourceEP][destEP]<msglen)
			            maxStats[sourceEP][destEP] = msglen;
			        msgCount[sourceEP][destEP]++;
			        byteSum[sourceEP][destEP]+=(double)msglen;
			        sumSquares[sourceEP][destEP]+=(double)msglen*(double)msglen;
			        if (!exists[sourceEP])
			            exists[sourceEP]=true;
			    }
			    LogFile.nextEvent(logdata);
		        }
		    }
		    LogFile.nextEvent(logdata);
		}
	    }
	    catch (IOException e)
	    {
	    }
	    currPeIndex++;
	}
	
	// Update Variance Array
	for (int srcEP=0; srcEP<numEPs; srcEP++) {
	    if (exists[srcEP]) {
	        for (int destEP=0; destEP<numEPs; destEP++) {
		    if (msgCount[srcEP][destEP]==1)
		        varStats[srcEP][destEP] = 0;
		    else if (msgCount[srcEP][destEP]>1) {
		        double mean = byteSum[srcEP][destEP]/msgCount[srcEP][destEP];
			//Variance = (sumofsquares - 2*mean*sum + mean*mean*count) / (count-1)
		        varStats[srcEP][destEP] = ((sumSquares[srcEP][destEP]
			                           - 2.0*mean*byteSum[srcEP][destEP]
						   + mean*mean*msgCount[srcEP][destEP])
						   /(msgCount[srcEP][destEP]-1.0));
		    }
		}
	    }
	}
	    
	progressBar.close();
    }

    public String getText(){
    	return "NoiseMiner Coming Soon!";
    }
    
    public String[][] getCallTableText(boolean epDetailToggle, boolean statsToggle)
    {
        // length is number of lines
        int length = 0;
	for (int sourceEP=0; sourceEP<numEPs; sourceEP++) {
	    if (exists[sourceEP]) {
	        length++;
		for (int destEP=0; destEP<numEPs; destEP++) {
		    if (msgCount[sourceEP][destEP] > 0)
		        length+=2;
		}
		length+=2; //for 2 line spaces between source EPs
	    }
	}
	
        String[][] text = new String[length][1];
	int lengthCounter = 0;
	
        for (int sourceEP=0; sourceEP<numEPs; sourceEP++) {
            if (exists[sourceEP]) {
	        
		if (epDetailToggle==true) { //need ep detail
	            text[lengthCounter][0] = Analysis.getEntryChareName(sourceEP) + "::" +
		                             Analysis.getEntryName(sourceEP);
		}
		else { //don't need ep detail
		    String s = Analysis.getEntryName(sourceEP);
		    int parenthIndex = s.indexOf('(');
		    if (parenthIndex != -1) //s has parenthesis
		        s = s.substring(0, parenthIndex);
		    text[lengthCounter][0] = s;
		}
		    
		lengthCounter++;
		
		for (int destEP=0; destEP<numEPs; destEP++) {
		    if (msgCount[sourceEP][destEP] > 0) {
		    
		        if (epDetailToggle==true) { //need ep detail
		            text[lengthCounter][0] = "        " + Analysis.getEntryChareName(destEP) + "::" +
		                                     Analysis.getEntryName(destEP);
			}
			else { //don't need ep detail
		            String s = Analysis.getEntryName(destEP);
		            int parenthIndex = s.indexOf('(');
		            if (parenthIndex != -1) //s has parenthesis
		                s = s.substring(0, parenthIndex);
		            text[lengthCounter][0] = "        " + s;
			}
			
			lengthCounter++;
			
			if (statsToggle==true) { //stats needed
			    text[lengthCounter][0] = "                " + 
			                             "Msg's Rec'd=" + _format.format(msgCount[sourceEP][destEP]) + 
						     "  Bytes Rec'd=" + _format.format(byteSum[sourceEP][destEP]) +
						     "  Min=" + _format.format(minStats[sourceEP][destEP]) +
						     "  Max=" + _format.format(maxStats[sourceEP][destEP]) +
						     "  Mean=" +
						     _format.format(byteSum[sourceEP][destEP]/msgCount[sourceEP][destEP]) +
						     "  Variance=" + _format.format(varStats[sourceEP][destEP]);
			}
			else { //stats not needed
			    text[lengthCounter][0] = "";
			}
			
			lengthCounter++;
		    }
	        }
		text[lengthCounter][0] = "";
		lengthCounter++;
		text[lengthCounter][0] = "";
		lengthCounter++;
	    }
	}
	
	return text;
    }
    
    
    
}		
