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
import jnt.FFT.*;


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
 *  	For each processor:
 *        build a histogram for each event, including a window of recent events for each bin
 *        
 *        Normalize
 *        
 *        Merge across events, so each processor has a histogram of normalized event durations
 *        
 *        Cluster the single processor's 
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
	
	/** A class which keeps up to a maximum number of events in a queue. 
	 * Upon request, it can produce data about the frequencies of the events. 
	 *
	 */
	private class eventWindow{
		public TreeSet<Long> occurrences; // essentially a sorted list or heap
		private int max;
		private double period;
		private double prominentPeriod;
		
		eventWindow(int maxSize){
			occurrences = new TreeSet<Long>();
			max = maxSize;
			prominentPeriod = -1.0;
		}
		
		public void insert(long t){
			occurrences.add(t);
			if(occurrences.size() > max){
				occurrences.remove(occurrences.first());
			}
		}

		public void merge(eventWindow ew){
			Iterator<Long> itr = ew.occurrences.iterator();
			while(itr.hasNext()){
				Long v = itr.next();			
				occurrences.add(v);
				if(occurrences.size() > max){
					occurrences.remove(occurrences.first());
				}
			}
		}
		
		public int size(){
			return occurrences.size();
		}

		public long getFirst(){
			return occurrences.first();
		}	
			
		public long getLast(){
			return occurrences.last();
		}
		
		public long getPeriod(){
			return ((getLast()-getFirst()) / occurrences.size());
		}
		
		public void buildFFT(){
			int size=1024;
			
			long first = getFirst();
			long last = getLast();
			long range = last-first;
			
			float data[] = new float[size];
					
			for(int i=0;i<size;i++){
				data[i]=0.0f;
			}

			// Iterate through the events in the queue and fill in their time-domain values
			
			Iterator<Long> it = occurrences.iterator();
			System.out.println("first="+first+" last="+last+" range="+range);
			while(it.hasNext()){
				long t = it.next();
				double d2 = (double)(t-first) / (double)(last-first);
				int d = (int)(d2*(size-1));
				data[d] += 2.0;
				System.out.println("Inserting item to time-domain at array element " + d );
			}
			
			// Perform an FFT
			
			RealFloatFFT_Radix2 fft = new RealFloatFFT_Radix2(size);
			
			fft.transform(data, 0, 1);
			
			System.out.println("fft result: ");
			for(int i=1;i<size/2;i++){
				if( (data[i]*data[i] + data[size-i]*data[size-i]) > 1){
					period = ((double)(last-first)/(double)i) ;

					System.out.println("i="+ i + "   " + data[i] + "," + data[size-i] + " ^2=" + (data[i]*data[i] + data[size-i]*data[size-i]) + " period=" + period);
//				System.out.println("i="+ i + "   " + (data[i]*data[i] + data[size-i]*data[size-i]) );
				}
			}	
			System.out.println("");
			
			float largestVal = 0.0f;
			int largest = 0;
			for(int i=1;i<size/2;i++){
				float valSqr = data[i]*data[i] + data[size-i]*data[size-i]; 
				if( valSqr > largestVal ){
					largest = i;
					largestVal = valSqr;
					System.out.println("i="+ i + "   " + valSqr );
				}	
						
			}
			
			
			prominentPeriod = (double)(last-first)/(double)largest;
			prominentPeriod = period;
			System.out.println("Event occurs with predominant periodicity: " + period);

			
			
		}
			
					
	}
	
	
	
	
	
	
	
	
	private class Histogram{
		private long bin_count[]; // The number of values that fall in each bin
		private long bin_sum[]; // The sum of all values that fall in each bin
		
		private eventWindow bin_window[]; // A list of recent events in each bin
		private int eventsInBinWindow;
		
		
		private class Cluster{
			private long sum;
			private long count;
			eventWindow events;
			
			Cluster(long s, long c, eventWindow ew){
				sum=s;
				count=c;
				events = new eventWindow(eventsInBinWindow);
				events = ew;
			}
			
			public void merge(long s, long c, eventWindow ew){
				sum += s;
				count += c;
				events.merge(ew);				
			}
			
			long mean(){
				return sum/count;
			}
			long count(){
				return count;
			}
			long sum(){
				return sum;
			}
			
		}
		
		private ArrayList<Cluster> clusters; // For each cluster in this histogram  
		private ArrayList<Cluster> clustersNormalized; // For each cluster in this histogram  

		public ArrayList<Cluster> clustersNormalized(){
			return clustersNormalized;
		}
		
		public ArrayList<Cluster> clusters(){
			return clusters;
		}
		
		public Cluster primaryNoise(){
			return clusters.get(1);
		}
		public boolean hasPrimaryNoise(){
			return (clusters.size()>1);
		}
		
		private int nbins;
		private long binWidth;
		private long total_sum;
		private long total_count;
		private boolean used;
		
		
		public Histogram(){ 
			used=false;
			nbins = 20;
			binWidth = 400;
			//eventsInBinWindow = 40;
			bin_count = new long[nbins];
			bin_sum = new long[nbins];
			bin_window = new eventWindow[nbins];
			for(int i=0;i<nbins;i++){
				bin_count[i] = 0;
				bin_sum[i] = 0;
				bin_window[i] = new eventWindow(eventsInBinWindow);
			}
		}
		
		public void insert(long duration, long when){
			used=true;
			total_count ++;
			total_sum += duration;
			int which_bin = (int)(duration / binWidth);
			if(which_bin > nbins-1){
				which_bin = nbins-1;
			} 
			if(which_bin >= 0){
				bin_count[which_bin] ++;
				bin_sum[which_bin] += duration;
				bin_window[which_bin].insert(when);
			}
		}
		
		
		
		public void insert(long duration, long when, long occurrences){
			if(occurrences > 0){
				used=true;
				total_count += occurrences;
				total_sum += duration*occurrences;
				int which_bin = (int)(duration / binWidth);
				if(which_bin > nbins-1){
					which_bin = nbins-1;
				}
				if(which_bin >= 0){
					bin_count[which_bin] +=occurrences;
					bin_sum[which_bin] += duration*occurrences;
				}
			}
		}
		
		public String toString(){
			String s ="";
			if(used){
				for(int i=0;i<nbins;i++){
					s = s + bin_count[i] + "\t";
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
	
		public boolean haveManySamples(){
			return (total_count > 100);
		}
				
		public void cluster(){
			clusters = new ArrayList<Cluster>();
			clustersNormalized = new ArrayList<Cluster>();
			
			boolean done=false;
			long bin_data[] = new long[nbins+1]; // a local copy of the histogram data. Entries of this are zeroed out below
			bin_data[nbins] = 0; // padding the right side so the algorithm below is simpler
			for(int i=0;i<nbins;i++)
				bin_data[i] = bin_count[i];
						
			// Repeat the following until enough clusters are found:
			while(!done) { // continue until we break
			
				// Find the heaviest bin remaining
				long largest_val = bin_data[0];
				int largest = 0;
				for(int i=1;i<nbins;++i){
					if(bin_data[i] > largest_val){
						largest_val = bin_data[i];
						largest = i;						
					}
				}
				
				if(largest_val == 0){
					// No bins have any data left to be clustered
					done=true;
				} else {
					
					// Build a cluster around this largest value
					bin_data[largest] = 0; // mark this bin as already seen
					Cluster c = new Cluster(bin_sum[largest], bin_count[largest],bin_window[largest]);
								
					// scan to right until we hit a local minimum
					for(int j=largest+1;j<nbins && ((j==nbins-1) || (bin_data[j] >= bin_data[j+1])) && bin_data[j]!=0; j++) {
						// merge into cluster
						c.merge(bin_sum[j], bin_count[j], bin_window[j]);
						bin_data[j] = 0;	// mark this bin as already seen
					}
					
					// scan to left until we hit a local minimum
					for(int j=largest-1;j>=0 && ((j==0) || (bin_data[j] >= bin_data[j-1])) && bin_data[j]!=0; j--) {
						// merge into cluster
						c.merge(bin_sum[j], bin_count[j], bin_window[j]);
						bin_data[j] = 0;   // mark this bin as already seen
					}
					
					// Store this newly found cluster
					clusters.add(c);
				}
			}
			
			// Create normalized versions of clusters
			ListIterator<Cluster> i = clusters.listIterator();
			long baseMean = clusters.get(0).mean();
			while(i.hasNext()){
				Cluster c = i.next();
				clustersNormalized.add(new Cluster(c.sum()-baseMean*c.count(),c.count(),c.events));
			}
			
			System.out.println("Found the following clusters: " + clusters_toString() );
			System.out.println("Found the normalized clusters: " + clusters_toString_Normalized() );
			
		}

		
		public String clusters_toString(ArrayList<Cluster> clusters){
			String result = "";
			ListIterator<Cluster> i = clusters.listIterator();
			while(i.hasNext()){
				Cluster c = i.next();
				if(c.count() > 1){
					result = result + "{ duration= " + c.mean() + ", count=" + c.count() + " wes=" + c.events.size() + " } ";
				}
			}
			return result;
		}
		
		public String clusters_toString(){
			return clusters_toString(clusters);
		}
		
		public String clusters_toString_Normalized(){
			return clusters_toString(clustersNormalized);
		}
		
		
		public double binCenter(int whichBin){
			return whichBin*binWidth+binWidth/2;		
		}
		
		public double binLowerBound(int whichBin){
			return whichBin*binWidth;		
		}
		
		public double binUpperBound(int whichBin){
			return whichBin*binWidth+binWidth;		
		}
	
		public int findFirstLocalMax(){
			long previous = bin_count[0];
			long current = bin_count[1];

			if(current < previous){
				return 0;
			}
			
			int i;
			for(i=1;i<nbins-1;i++){
				long next = bin_count[i+1];
				if(current>=previous && current > next)
					return i;
				previous = current;
				current = next;				
			}
			// shouldn't get here
			return i;
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
		
		ProgressMonitor progressBar = new ProgressMonitor(parent, "Mining for Computational Noise","", 0, numPe);

		String[][] entryNames = Analysis.getEntryNames();
		
		// For each pe
		while (peList.hasMoreElements()) {
			int numEvents = 250;
			Histogram h[] = new Histogram[numEvents];
			for(int i=0;i<numEvents;i++){
				h[i] = new Histogram();	
			}
			
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
					if(logdata.type == BEGIN_PROCESSING){
						previous_time = logdata.time;
						previous_entry = logdata.entry;

					} else if(logdata.type == END_PROCESSING){
						// if we have seen the matching BEGIN_PROCESSING
						if(previous_entry == logdata.entry){
							long duration = logdata.time - previous_time;
							h[logdata.entry].insert(duration, previous_time);							
						}
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

			
			// Normalize(shifted) each event's histogram
			
			for(int i=0;i<numEvents;i++){
				if(h[i].haveManySamples()){
					h[i].cluster();
					System.out.println("Clusters for event:" + i + " pe:" + currPe + " are: " + h[i].clusters_toString() );
				}
			}
			
			
			// Merge all the normalized clustered events into a new histogram
			Histogram h_pe = new Histogram();
			for(int i=0;i<numEvents;i++){
				if(h[i].haveManySamples()){
					// for each normalized cluster
					ListIterator<Histogram.Cluster> itr = h[i].clusters().listIterator();
					while(itr.hasNext()){
						Histogram.Cluster c = itr.next();

						long occurrences = c.count();
						long mean = c.mean();
						long temp_when=0;
						h_pe.insert(mean, temp_when, occurrences);
					
					}
					
				}
			}


			h_pe.cluster();

		//	String entryName = entryNames[i][0];
			// entryName = entryName.split("\\(")[0];				

		//	System.out.println("Histogram for pe " + currPe + " is: " + h_pe);
			System.out.println("Clusters for pe " + currPe + " are: " + h_pe.clusters_toString() );

			if(h_pe.hasPrimaryNoise()){
				loggingText = loggingText + "The primary noise component on processor " + currPe + " has duration of about " + h_pe.primaryNoise().mean() + "us and occurs " + h_pe.primaryNoise().count() + " times\n";
			}
		}
		
		progressBar.close();
	}

	public String getText(){
		String s = "NoiseMiner\n";
		s = s + "Time range " + startTime + " to " + endTime + "\n";
		s = s + loggingText;
		// s = s + memoryUsageToString();
		return s;
	}



}		
