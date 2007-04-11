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
 *  
 *  
 *  The mining algorithm performs the following steps
 *  
 *  	maintain for each event a histogram 
 *  	
 *  
 *  
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

	private class Histogram{
		private long bins[];
		private int nbins;
		private long total_sum;
		private long total_count;
		private boolean used;
		
		public Histogram(){ 
			used=false;
//			System.out.println("Constructor for Histogram");
			nbins = 25;
			bins = new long[nbins];
			for(int i=0;i<nbins;i++){
				bins[i] = 0;
			}
		}
		
		public void insert(long value){
			used=true;
			total_count ++;
			total_sum += value;
			int which_bin = (int)(value / 1000l);
			if(which_bin > nbins-1){
				which_bin = nbins-1;
			}
//			System.out.println("which_bin=" + which_bin);
			bins[which_bin] ++;
		}
		
		public String toString(){
			String s ="";
			if(used){
				for(int i=0;i<nbins;i++){
					s = s + bins[i] + ", ";
				}
			}
			else{
				s = "unused";
			}
				
			return s;
		}
		
		public boolean isUsed(){
			return used;
		}
		
	}
	
	
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
		
		long previous_time=-1;
		int  previous_entry=-1;
		
		int numHistograms = 250;
		Histogram h[] = new Histogram[numHistograms];
		for(int i=0;i<numHistograms;i++){
			h[i] = new Histogram();	
		}
		
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
					//System.out.println("Time=" + logdata.time + " event " + logdata.event);
					//Go through each log file and for each Creation
					//  in a Processing Block, update arrays
					if(logdata.type == BEGIN_COMPUTATION){
//						System.out.println("Found BEGIN_COMPUTATION");
					} else if(logdata.type == END_COMPUTATION){
//						System.out.println("Found END_COMPUTATION");
					} else if(logdata.type == BEGIN_IDLE){
//						System.out.println("Found BEGIN_IDLE");
					} else if(logdata.type == END_IDLE){
//						System.out.println("Found END_IDLE");
					} else if(logdata.type == USER_EVENT_PAIR){
//						System.out.println("Found USER_EVENT_PAIR");
					} else if(logdata.type == CREATION){
//						System.out.println("Found CREATION");
					} else if(logdata.type == BEGIN_PROCESSING){
//						 System.out.println("Found BEGIN_PROCESSING for event " + logdata.event + " at time " + logdata.time + " for entry method " + logdata.entry);
						previous_time = logdata.time;
						previous_entry = logdata.entry;
					
					} else if(logdata.type == END_PROCESSING){
						// if we have seen the matching BEGIN_PROCESSING
						if(previous_entry == logdata.entry){
							long duration = logdata.time - previous_time;
//							if(duration > 2000)
//								System.out.println("Entry " + logdata.entry + " for event " + logdata.event + " took " + duration + " us");						
							h[logdata.entry].insert(duration);
						}
						
						
//						 System.out.println("Found END_PROCESSING for event " + logdata.event + " at time " + logdata.time);
					} else if(logdata.type == BEGIN_UNPACK){
//						System.out.println("Found BEGIN_UNPACK");
					} else if(logdata.type == END_UNPACK){
//						System.out.println("Found END_UNPACK");
					} else if(logdata.type == CREATION_MULTICAST){
//						System.out.println("Found CREATION_MULTICAST");
					} else if(logdata.type == BEGIN_PACK){
//						System.out.println("Found BEGIN_PACK");
					} else if(logdata.type == END_PACK){
//						System.out.println("Found END_PACK");
					} else if(logdata.type == USER_EVENT){
//						System.out.println("Found USER_EVENT");
					} else {
//						System.out.println("Found unknown log entry #" + logdata.type);
					}

					LogFile.nextEvent(logdata);
					count ++;
				}


			}

			catch (IOException e)
			{
			}

			loggingText = loggingText + "Found " + count + " events in the specified time range on pe=" + currPe + "\n";
			checkMemoryUsage();
			currPeIndex++;
		}	

		for(int i=0;i<numHistograms;i++){
			if(h[i].isUsed()){
				System.out.println("Histograms for " + i + " is: " + h[i]);
			}
		}
		progressBar.close();
	}

	public String getText(){
		String s = "NoiseMiner Coming Soon!\n";
		s = s + "Time range " + startTime + " to " + endTime + "\n";
		s = s + loggingText + memoryUsageToString();
		return s;
	}



}		
