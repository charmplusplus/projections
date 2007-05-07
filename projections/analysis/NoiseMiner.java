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
import java.lang.*;


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
 *  @todo Find non-idle non-event stretches as well
 *  @todo Eliminate any noise components that are just associated with a single event
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

	private String loggingText;
	
	private long osQuanta_us;

	private final double peMergeDistance = 0.2;
	
	private LinkedList<NoiseResult> results;
	private LinkedList<NoiseResult> resultsClustered;
	
	public long osQuanta_us(){
		return osQuanta_us;
	}
	public long osQuanta_ms(){
		return osQuanta_us/1000;
	}
	
	private class NoiseResult implements Comparable{
		public LinkedList<Integer> pes; // list of processors on which the event occurs
		public double periodicity; // The average period determined by the span of the window
		public double periodicity_FFT; // The primary peak in the FFT
		public long duration; // The amount of time spend in the noise
		public long occurrences; // The number of times this event occurred per processor
		
		public NoiseResult(long d, long o, double p, double p_fft, int pe){
			pes = new LinkedList<Integer>();
			pes.add(pe);
			System.out.println("HHHHH: adding pe, pes.size=" + pes.size());
			periodicity = p;
			periodicity_FFT = p_fft;
			duration = d;
			occurrences = o;	
		}
		
		public String pe_toString(){
			String s = "";
			Iterator<Integer> itr = pes.iterator();
			while(itr.hasNext()){
				int v = itr.next();			
				s = s + v;
				if(itr.hasNext()){
					s = s + ", ";
				}
			}
			System.out.println("pes.size()=" + pes.size() + " string= " + s);
			return s;
		}
		
		/** Determine how different this and another NoiseResult are */
		public double distance(NoiseResult nr){
			double result = 0.0;
			
			result += 0.5 * (Math.abs((double)periodicity - (double)nr.periodicity) / (double)periodicity);
				
			result += 1.0 * (Math.abs((double)duration - (double)nr.duration) / Math.max((double)duration,(double)nr.duration));
			
			System.out.println("III: " + result + " duration=" + (double)duration + " nrd=" + (double)nr.duration);
			return result;				
		}
		
		public void merge(NoiseResult nr){
			pes.addAll(nr.pes);
			periodicity = (periodicity * occurrences + nr.periodicity * nr.occurrences) / (occurrences+nr.occurrences);
			periodicity_FFT = (periodicity_FFT * occurrences + nr.periodicity_FFT * nr.occurrences) / (occurrences+nr.occurrences);
			duration = (duration * occurrences + nr.duration * nr.occurrences) / (occurrences+nr.occurrences);
			occurrences += nr.occurrences;
		}
		
		
		public int compareTo(Object other){
			assert(other != null);
			assert(other instanceof NoiseResult);
			NoiseResult nr = (NoiseResult)other;
			
			return (int)(nr.duration*nr.occurrences-duration*occurrences);
			
		}
		
	}
	
	/** cluster the results by merging similar ones */
	private void clusterResults(){
		resultsClustered = new LinkedList<NoiseResult>();
		
		Iterator<NoiseResult> itr = results.iterator();
		while(itr.hasNext()){
			NoiseResult v = itr.next();			
			
			// Iterate through clusters, and merge this one in if it is close enough
			Iterator<NoiseResult> itr2 = resultsClustered.iterator();
			boolean inserted = false;
			while(itr2.hasNext() && inserted==false){
				NoiseResult c = itr2.next();			
				if(c.distance(v) < peMergeDistance){
					c.merge(v);
					inserted = true;
				}
			}
			if(inserted == false)
				resultsClustered.add(v);
				
		}
			
			
	}
	
	
	
	/** A class which keeps up to a maximum number of events in a queue. 
	 * Upon request, it can produce data about the frequencies of the events. 
	 *
	 */
	private class eventWindow{
		public TreeSet<Long> occurrences; // essentially a sorted list or heap
		private int max;
		private double period;
		private double prominentPeriod_us;
		
		eventWindow(int maxSize){
//			System.out.println("eventWindow("+maxSize+")");
			occurrences = new TreeSet<Long>();
			max = maxSize;
			prominentPeriod_us = -1.0;
		}
		
		public void insert(long t){
			occurrences.add(t);
			if(occurrences.size() > max){
//				occurrences.remove(occurrences.first());
			}
		}

		public void merge(eventWindow ew){
//			System.out.println("merge  my size="+occurrences.size() + " other=" + ew.occurrences.size());
			Iterator<Long> itr = ew.occurrences.iterator();
			while(itr.hasNext()){
				Long v = itr.next();			
				occurrences.add(v);
				if(occurrences.size() > max){
					occurrences.remove(occurrences.first());
				}
			}
//			System.out.println("new size=" + occurrences.size());
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

		/** find the average period between the events in the window */
		public long getPeriod(){
			return ((getLast()-getFirst()) / occurrences.size());
		}
		
		public double periodFromFFT_us(){
			return prominentPeriod_us;
		}
		
		public double periodFromFFT_ms(){
			return prominentPeriod_us / 1000.0;
		}
		
		public void buildFFT(){
			int size=1024;
			
			long first = getFirst();
			long last = getLast();
			long range = last-first;
			float sum = 1.0f;
			
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
				data[d] += 1.0;
				sum+=1.0;
//				System.out.println("Inserting item to time-domain at array element " + d );
			}
			
			// Remove DC offset
			for(int i=0;i<size;i++){
				data[i] -= sum/((float)size);
			}
			
			
			// Perform an FFT
			RealFloatFFT_Radix2 fft = new RealFloatFFT_Radix2(size);
			
			fft.transform(data, 0, 1);
			
			System.out.println("fft result: ");
			for(int i=1;i<size/2;i++){
				if( (data[i]*data[i] + data[size-i]*data[size-i]) > 1){
					period = ((double)(last-first)/(double)i) ; // period associated with this fft entry

					System.out.println("i="+ i + "   " + data[i] + "," + data[size-i] + " ^2=" + (data[i]*data[i] + data[size-i]*data[size-i]) + " period=" + period);
				System.out.println("i="+ i + "   " + (data[i]*data[i] + data[size-i]*data[size-i]) );
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
//					System.out.println("i="+ i + "   " + valSqr );
				}	
						
			}
			
			prominentPeriod_us = (double)(last-first)/(double)largest;
			prominentPeriod_us = period;
			System.out.println("Event occurs with predominant FFT periodicity: " + period);			
			
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
				eventsInBinWindow = 80;
				events = new eventWindow(eventsInBinWindow);
				events.merge(ew);
			}
			
			public void merge(long s, long c, eventWindow ew){
				sum += s;
				count += c;
//				System.out.println("Cluster::merge() adding "+ ew.size() + " , old size=" + events.size());
				events.merge(ew);				
//				System.out.println("\t\t new size="+events.size());

			}
			
			public void merge(Cluster c){
				sum += c.sum;
				count += c.count;
				events.merge(c.events);
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
		
		
		public int countEvents(){
			int c=0;
			for(int i=0;i<nbins;i++){
				c+=bin_window[i].size();
			}
			System.out.println("total events in all bin windows:" + c);
			return c;
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
			return clustersNormalized.get(1);
		}
		public boolean hasPrimaryNoise(){
			return (clustersNormalized.size()>1);
		}
		
		public Cluster secondaryNoise(){
			return clustersNormalized.get(2);
		}
		public boolean hasSecondaryNoise(){
			return (clustersNormalized.size()>2);
		}

		// Find the nth most important noise components
		public Cluster nthNoise(int n){
			return clustersNormalized.get(n);
		}
		public boolean hasnthNoise(int n){
			return (clustersNormalized.size()>n);
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
			eventsInBinWindow = 80;
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
					bin_window[which_bin].insert(when);
				}
			}
		}
		
		/**  Insert a cluster to this histogram, including its event window. Put the whole cluster into the 
		 *   bin matching the cluster mean 
		 */
		public void insert(Cluster c){
			if(c.count() > 0){
				used=true;
				total_count += c.count();
				total_sum += c.sum();
				int which_bin = (int)(c.mean() / binWidth);
				if(which_bin > nbins-1){
					which_bin = nbins-1;
				}
				if(which_bin >= 0){
					bin_count[which_bin] += c.count();
					bin_sum[which_bin] += c.sum();
					bin_window[which_bin].merge(c.events);
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
					
//					System.out.println("grow left right");
					
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
				
		results = new LinkedList<NoiseResult>();
		
		osQuanta_us = 100000; // linux 2.16 default is 100ms . This should be updated

		loggingText = "";
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
			currPeIndex++;

			// print each event's histogram
			
			for(int i=0;i<numEvents;i++){
				if(h[i].haveManySamples()){
					h[i].cluster();
//					System.out.println("Clusters for event:" + i + " pe:" + currPe + " are: " + h[i].clusters_toString() );
				}
			}
			
		
			// Merge all the events across this pe
			System.out.println("Creating histogram for pe");
			Histogram h_pe = new Histogram();
			for(int i=0;i<numEvents;i++){
				if(h[i].haveManySamples()){
					// for each normalized cluster
					ListIterator<Histogram.Cluster> itr = h[i].clustersNormalized().listIterator();
					while(itr.hasNext()){
						Histogram.Cluster c = itr.next();

						long occurrences = c.count();
						long mean = c.mean();
						long temp_when=0;
//						System.out.println("Adding the cluster with occur:" + occurrences);
						h_pe.insert(c);
					
					}
				}
			}

//			System.out.println("The histogram is now:"+ h_pe.toString());
//			System.out.println("h_pe : ");
//			h_pe.countEvents();
			
			h_pe.cluster();

			//	String entryName = entryNames[i][0];
			// entryName = entryName.split("\\(")[0];				

		//	System.out.println("Histogram for pe " + currPe + " is: " + h_pe);
//			System.out.println("Clusters for pe " + currPe + " are: " + h_pe.clusters_toString() );

			// Look at each noise component and create an result record
			
			int n = 1;
			while(h_pe.hasnthNoise(n)){
				h_pe.nthNoise(n).events.buildFFT();
				
				long duration = h_pe.nthNoise(n).mean();
				long occurrences = h_pe.nthNoise(n).count();
				double periodicity_us = h_pe.nthNoise(n).events.getPeriod();
				double periodicity_ms = periodicity_us / 1000.0; 
				double periodicity_fft_us = h_pe.nthNoise(n).events.periodFromFFT_us();
				double periodicity_fft_ms = periodicity_fft_us / 1000.0;
				
				if(occurrences > 5){
					loggingText = loggingText + "Noise component " + n + " on processor " + currPe + " has duration of about " + duration + "us and occurs " + occurrences + " times. The recent events appear to have a periodicity of about " + periodicity_ms + "ms" + ". The FFT determined the primary period=" + periodicity_fft_ms	+ "ms\n";					
					results.add(new NoiseResult(duration, occurrences, periodicity_ms, periodicity_fft_ms, currPe));
				}
				
//				if(periodicity_us < osQuanta_us ){
//					loggingText = loggingText + "Because the OS timeslice quanta are significantly longer than the time between occurrences of this primary noise component, it is likely the noise component is produced internally to the program. If the event durations for this program are regular and have durations that don't vary greatly, then the communication layer is a likely suspect!\n";
//				} else {
//					loggingText = loggingText + "It is likely that the primary noise source is external to the application\n";
//				}

				n++;
			}
			
		}// end for each pe
		
		// cluster all result records across all processors
		clusterResults();
		
		progressBar.close();
	}

	public String getText(){
		String s = "NoiseMiner Text Report:\n";
		s = s + "Time range " + startTime + " to " + endTime + "\n";
		s = s + loggingText;
		return s;
	}
	
	/** Create an array of objects representing the internal noise components */
	public Vector getResultsTableInternal(){
		Collections.sort(resultsClustered);
		
		// scan through results to find only the ones with periodicity close to or longer than the OS time quanta 
			
		int numResultRows = resultsClustered.size();
			
		Vector resultTable = new Vector();

		Iterator<NoiseResult> itr = resultsClustered.iterator();
		while(itr.hasNext()){
			NoiseResult v = itr.next();		

			if(v.periodicity < osQuanta_ms()*0.8 ) {
			
				Vector row = new Vector();
				String periodicity = String.format("%.1f", v.periodicity );
				String periodicity_fft = String.format("%.1f", v.periodicity_FFT);
	
				row.add(new String("" + v.duration ));
				row.add(new String("" + v.pe_toString() ));
				row.add(new String("" + v.occurrences ));
				row.add(new String("" + periodicity));
				row.add(new String("" + periodicity_fft));
				
				resultTable.add(row);
			}
		}
		
		return resultTable;
	}

	/** Create an array of objects representing the external noise components */
	public Vector getResultsTableExternal(){
		Collections.sort(resultsClustered);
		
		// scan through results to find only the ones with periodicity close to or longer than the OS time quanta 
			
		int numResultRows = resultsClustered.size();
			
		Vector resultTable = new Vector();

		Iterator<NoiseResult> itr = resultsClustered.iterator();
		while(itr.hasNext()){
			NoiseResult v = itr.next();		

			if(v.periodicity >= osQuanta_ms()*0.8) {
			
				Vector row = new Vector();
				String periodicity = String.format("%.1f", v.periodicity );
				String periodicity_fft = String.format("%.1f", v.periodicity_FFT);
	
				row.add(new String("" + v.duration ));
				row.add(new String("" + v.pe_toString() ));
				row.add(new String("" + v.occurrences ));
				row.add(new String("" + periodicity));
				row.add(new String("" + periodicity_fft));
				
				resultTable.add(row);
			}
		}
		
		return resultTable;
	}

}		
