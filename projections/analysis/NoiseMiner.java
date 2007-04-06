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

	private long startTime;	     //Interval begin
	private long endTime;	     //Interval end
	private OrderedIntList peList;   //List of processors

	private long baselineMemoryUsage;
	private long highWatermarkMemoryUsage;

	private String loggingText;
	
	public NoiseMiner(long startInterval, long endInterval, 
			OrderedIntList processorList)
	{
		//Initialize class variables
		peList = processorList;
		numPe = peList.size();
		numEPs = Analysis.getNumUserEntries();
		startTime = startInterval;
		endTime = endInterval;

		baselineMemoryUsage = java.lang.Runtime.getRuntime().totalMemory();
		highWatermarkMemoryUsage = baselineMemoryUsage;
		
		loggingText = "";
	}

	private void checkMemoryUsage(){
		long m = java.lang.Runtime.getRuntime().totalMemory();
		if(m > highWatermarkMemoryUsage){
			highWatermarkMemoryUsage = m;
		}
	}

	public String memoryUsageToString(){
		String s = "Max memory used by NoiseMiner: " + ((highWatermarkMemoryUsage - baselineMemoryUsage)/1024/1024) + " MB\n";
		return s;
	}
	
	public void gatherData(Component parent)
	{
		GenericLogReader LogFile;
		LogEntryData logdata = new LogEntryData();

		int currPeIndex = 0;
		int currPe;
		int sourceEP;

		ProgressMonitor progressBar = new ProgressMonitor(parent, "Mining for Computational Noise","", 0, numPe);

		// For each pe
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
			LogFile = new GenericLogReader(Analysis.getLogName(currPe), Analysis.getVersion());    
		
			int count=0;
			try {
			
				LogFile.nextEventOnOrAfter(startTime, logdata);

				while (logdata.time < endTime) {
/*					//Go through each log file and for each Creation
					//  in a Processing Block, update arrays
					if (logdata.type == BEGIN_PROCESSING) {
						//Starting new entry method
						sourceEP = logdata.entry;	
						LogFile.nextEvent(logdata);
						while ( (logdata.type != END_PROCESSING) && (logdata.type != END_COMPUTATION) && (logdata.time < endTime) ) {
						
							int msglen = logdata.msglen;
							int destEP = logdata.entry;
							loggingText = loggingText + "Log entry: Type=" + logdata.type + " len=" + msglen + " destEP=" + destEP + "\n";
						
							LogFile.nextEvent(logdata);
						}
					}
*/
					count ++;

					LogFile.nextEvent(logdata);
					
				}
			
				
			}
			catch (IOException e)
			{
			}
	
			loggingText = loggingText + "Found " + count + " events in the specified time range on pe=" + currPe + "\n";
			checkMemoryUsage();
			currPeIndex++;
		}	

		progressBar.close();
	}

	public String getText(){
		String s = "NoiseMiner Coming Soon!\n";
		s = s + loggingText + memoryUsageToString();
		return s;
	}



}		
