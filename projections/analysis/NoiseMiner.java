package projections.analysis;

import projections.misc.*;
import projections.gui.*;

import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
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
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;

	private int numPe;		     //Number of processors

	private long startTime;	     //Interval begin
	private long endTime;	     //Interval end
	private OrderedIntList peList;   //List of processors

	private String loggingText;

	private Duration osQuanta;
	
	private int eventsInBinWindow=50;

	/** A distance used to merge similar clusters, currently clusters are merged if the difference in their durations is 40% */
	protected double peMergeDistance = 0.15;

	/** The proportion of the total runtime that a result must have to be important*/
	protected double importanceCutoff = 0.8;

	
	/** Number of bins in each histogram */
	int nbins = 5001;
	/** temporal width of each histogram bin (microseconds)*/
	Duration binWidth = new Duration(10); 
	
	/** A list of noise result components
	 * 
	 *  */
	private LinkedList finalResults;

	private class Time {

		protected double d; // stores duration in us
		
		public Time(){	
			d = 0.0;
		}
		
		public Time(long us){
			d = us;
		}
		
		public Time(Time p){
			d = p.d;
		}
		
		public Time(double us){
			d = us;
		}
		
		public void set_ms(double p){
			d = p * 1000.0;
		}
		public void set_us(double p){
			d = p;
		}
		public void set(Duration p){
			d = p.d;
		}
		public double us(){
			return d;
		}
		public double ms(){
			return d/1000.0;
		}
		
		public String toString(){
			if(d > 1000.0){
//				return String.format("%.2f(ms)", d/1000.0 );
				return ""+(d/1000.0)+"(ms)";
			} else {
//				return String.format("%.2f(us)", d );
				return ""+d+"(us)";
			}
			
		}
		
		public void add(Time p){
			d += p.d;
		}
		
		/** add a given amount of microseconds to this Duration */
		public void add_us(double p){
			d += p;
		}
		
		/** add a given amount of microseconds to this Duration */
		public void add_us(long p){
			d += p;
		}
			
	}

	/** essentially an alias to Time class */
	private class Duration extends Time{
		public Duration(){	
			d = 0.0;
		}

		public Duration(long us){
			d = us;
		}

		public Duration(Time p){
			d = p.d;
		}
		
		public Duration(Time start, Time end){
			d = end.d - start.d;
		}

		public Duration(long start_us, long end_us){
			d = end_us - start_us;
		}

		public Duration(double start_us, double end_us){
			d = end_us - start_us;
		}
		
		public Duration(double us){
			d = us;
		}

	}
	
	
	/** A resulting cluster of noise occurrences
	 * <Integer>
	 *  */
	public class NoiseResult implements Comparable{
		public TreeSet pes; // list of processors on which the event occurs
		public Duration duration; // The amount of time spend in the noise
		public long occurrences; // The number of times this event occurred
		public EventWindow ew; 
		
		
		public Duration period(){
			return ew.period();
		}
		  
		public NoiseResult(Duration d, long o, int pe, EventWindow ew){
			pes = new TreeSet();
			this.ew = ew;
			pes.add(new Integer(pe));
			//periodicity_FFT = p_fft;
			duration = d;
			occurrences = o;
		}

		public String pe_toString(){
			String s = "";
			Iterator itr = pes.iterator();
			while(itr.hasNext()){
				Integer v = (Integer) itr.next();
				s = s + v;
				if(itr.hasNext()){
					s = s + ", ";
				}
			}
			return s;
		}

		/** A distance measure between this and another NoiseResult. Incorporates the duration */
		public double distance(NoiseResult nr){
			double result = 0.0;
//			result += 0.1 * (Math.abs((double)periodicity.us() - (double)nr.periodicity.us()) / Math.max((double)periodicity.us(),(double)nr.periodicity.us()));
			result += 1.0 * (Math.abs((double)duration.us() - (double)nr.duration.us()) / Math.max((double)duration.us(),(double)nr.duration.us()));
			return result;
		}

		public void merge(NoiseResult nr){
			pes.addAll(nr.pes);
			duration.set_us((duration.us() * occurrences + nr.duration.us() * nr.occurrences) / (occurrences+nr.occurrences));
			occurrences += nr.occurrences;
			ew.merge(nr.ew);
		}


		public int compareTo(Object other){
//			assert(other != null);
//			assert(other instanceof NoiseResult);
			NoiseResult nr = (NoiseResult)other;

//			double d = nr.duration.us()*nr.occurrences-duration.us()*occurrences;
			double d = nr.duration.us()*Math.log(nr.occurrences)-duration.us()*Math.log(nr.occurrences);
			
			if(d<0){
				return -1;
			} else if(d>0) {
				return 1;
			} else {
				return 0;
			}

		}

	}

	/** cluster the results across all processors by merging similar ones 
	 * 
	 * */
	private LinkedList clusterResultsAcrossProcs(LinkedList results){
		System.out.println("clusterResultsAcrossProcs() input size="+results.size());
		

		LinkedList newResults = new LinkedList();

		Iterator itr = results.iterator();
		while(itr.hasNext()){
			NoiseResult v = (NoiseResult) itr.next();

			// Iterate through the clusters we've created so far and merge this one if similar
			Iterator itr2 = newResults.iterator();
			boolean inserted = false;
			while(itr2.hasNext() && inserted==false){
				NoiseResult c = (NoiseResult) itr2.next();
				if(c.distance(v) < peMergeDistance){
					c.merge(v);
					inserted = true;
				}
			}
			// If the cluster is unique, add it
			if(inserted == false)
				newResults.add(v);
		}
		
		System.out.println("Merged "+results.size() + " clusters across all processors into " + newResults.size() + " resulting clusters");
		
		return newResults;
	}



	/** A class which keeps up to a maximum number of events in a queue.
	 * Upon request, it can produce data about the frequencies of the events.
	 *
	 */
	public class EventWindow{
		/** <TimelineEvent> */
		public TreeSet occurrences; // essentially a sorted list or heap
		private int max;

		EventWindow(int maxSize){
			/** <TimelineEvent> */
			occurrences = new TreeSet();
			max = maxSize;
		}

		/** Need to keep track of the time, duration, and event id, PE of the event, not just the time */
		public void insert(TimelineEvent e){
			occurrences.add(e);
			if(occurrences.size() > max){
				occurrences.remove(occurrences.first());
			}
		}

		public void merge(EventWindow ew){
			/** <TimelineEvent> */
			Iterator itr = ew.occurrences.iterator();
			while(itr.hasNext()){
				TimelineEvent v = (TimelineEvent) itr.next();
				occurrences.add(v);
				if(occurrences.size() > max){
					occurrences.remove(occurrences.first());
				}
			}
		}

		public int size(){
			return occurrences.size();
		}

		public TimelineEvent getFirst(){
			return (TimelineEvent) occurrences.first();
		}

		public TimelineEvent getLast(){
			return (TimelineEvent) occurrences.last();
		}

		/** find the average period between the events in the window */
		public Duration period(){
			Duration d = new Duration();
			d.set_us((getLast().BeginTime-getFirst().BeginTime) / occurrences.size());
			return d;
		}
		
//		public Duration periodFromFFT(){
//			return prominentPeriod;
//		}
//		
//		public void buildFFT(){
//			int size=1024;
//
//			Duration first = new Duration(getFirst());
//			Duration last = new Duration(getLast());
//			Duration range = new Duration(first.us(),last.us());
//			float sum = 1.0f;
//
//			float data[] = new float[size];
//
//			for(int i=0;i<size;i++){
//				data[i]=0.0f;
//			}
//
//			// Iterate through the events in the queue and fill in their time-domain values
//
//			Iterator<Long> it = occurrences.iterator();
//			while(it.hasNext()){
//				long t = it.next();
//				double d2 = (double)(t-first.us()) / (double)(last.us()-first.us());
//				int d = (int)(d2*(size-1));
//				data[d] += 1.0;
//				sum+=1.0;
//			}
//
//			// Remove DC offset
//			for(int i=0;i<size;i++){
//				data[i] -= sum/((float)size);
//			}
//
//
//			// Perform an FFT
//			RealFloatFFT_Radix2 fft = new RealFloatFFT_Radix2(size);
//
//			fft.transform(data, 0, 1);
//		
//			float largestVal = 0.0f;
//			int largest = 0;
//			
//			for(int i=1;i<size/2;i++){
//			
//				float valSqr = data[i]*data[i] + data[size-i]*data[size-i];
//				if( valSqr > largestVal ){
//					largest = i;
//					largestVal = valSqr;
//					prominentPeriod.set_us(((double)(last.us()-first.us())/(double)i)); // period associated with this fft entry
//				}
//				
//			}
//
//		}

	}



	/**
	 *  Filter out clusters that do not have sufficient contribution to overall computation time 
	 *  If the ratio of the duration to the periodicity is not greater than the cutoff the cluster is ignored.
	 *  This removes clusters that only add a tiny portion of noise infrequently.
     * 
	 */
	public LinkedList filterResults(LinkedList results){
		int keepCount = 0;
		int dropCount = 0;
		
		LinkedList newResults = new LinkedList();

		Iterator itr = results.iterator();
		while(itr.hasNext()) {
			NoiseResult v = (NoiseResult) itr.next();
		
			// Only consider clusters with more than two events, otherwise periodicity is undefined
			if(v.occurrences > 5 ){ 
				// determine the importance of this cluster
				double importance = v.duration.us() / v.period().us() * Math.log(v.duration.us()) ;
				System.out.println("importance="+importance + " occurrences="+v.occurrences);	

				if(importance > importanceCutoff){
					newResults.add(v);
					keepCount ++;
				} else {
					dropCount ++;
				}
			} else {
				dropCount++;
			}
			
			
		}
		
		System.out.println("Filtering out "+dropCount+" of "+ (dropCount+keepCount) + " results");

		return newResults;			
	}






	private class Histogram{
		private long bin_count[]; //< The number of values that fall in each bin
		private Duration bin_sum[]; //< The sum of all values that fall in each bin

		private EventWindow bin_window[]; //< A list of recent events in each bin
		
		/** The sum of the durations of all events seen so far */
		private Duration cummulativeEventDurations;

		/** The number of events that this histogram has seen */
		private long eventsSeenSoFar;


		private ArrayList clusters; // For each cluster in this histogram
		private ArrayList clustersNormalized; // For each cluster in this histogram


		public int countEvents(){
			int c=0;
			for(int i=0;i<nbins;i++){
				c+=bin_window[i].size();
			}
			return c;
		}

		public ArrayList clustersNormalized(){
			return clustersNormalized;
		}

		public ArrayList clusters(){
			return clusters;
		}

		public Cluster primaryNoise(){
			return (Cluster) clustersNormalized.get(1);
		}
		public boolean hasPrimaryNoise(){
			return (clustersNormalized.size()>1);
		}

		public Cluster secondaryNoise(){
			return (Cluster) clustersNormalized.get(2);
		}
		public boolean hasSecondaryNoise(){
			return (clustersNormalized.size()>2);
		}

		// Find the nth most important noise components
		public Cluster nthNoise(int n){
			return (Cluster) clustersNormalized.get(n);
		}
		public boolean hasNthNoiseComponent(int n){
			return (clustersNormalized.size()>n);
		}


		public Histogram(){
			cummulativeEventDurations = new Duration(0);
			
			bin_count = new long[nbins];
			bin_sum = new Duration[nbins];
			bin_window = new EventWindow[nbins];
			for(int i=0;i<nbins;i++){
				bin_count[i] = 0;
				bin_sum[i] = new Duration(0.0);
				bin_window[i] = new EventWindow(eventsInBinWindow);
			}
		}

		public void insert(TimelineEvent event) {
			Duration duration = new Duration(event.EndTime-event.BeginTime);
			eventsSeenSoFar ++;
			cummulativeEventDurations.add(duration);
			int which_bin = (int)(duration.us() / binWidth.us());
			if(which_bin > nbins-1){
				which_bin = nbins-1;
			}
			if(which_bin >= 0){
				bin_count[which_bin] ++;
				bin_sum[which_bin].add(duration);
				bin_window[which_bin].insert(event);
			}
		}


		/**  Insert a cluster to this histogram, including its event window. Put the whole cluster into the
		 *   bin matching the cluster mean
		 */
		public void insert(Cluster c){
			if(c.count() > 0){
				eventsSeenSoFar += c.count();
				cummulativeEventDurations.add(c.sum());
				int which_bin = (int)(c.mean().us() / binWidth.us());
				if(which_bin > nbins-1){
					which_bin = nbins-1;
				}
				if(which_bin >= 0){
					bin_count[which_bin] += c.count();
					bin_sum[which_bin].add(c.sum());
					bin_window[which_bin].merge(c.events);
				}
			}
		}




		public String toString(){
			String s ="";
			if(used()){
				for(int i=0;i<nbins;i++){
					s = s + bin_count[i] + "\t";
				}
			}
			else{
				s = "unused";
			}

			return s;
		}

		/** Does this histogram has any data in it? */
		private boolean used() {
			return cummulativeEventDurations.us()>0;
		}

		public boolean isUsed(){
			return cummulativeEventDurations.us()>0;
		}

	
		/** Generate the clusters and normalized clusters, no filtering is performed */
		public void cluster(){
			clusters = new ArrayList();
			clustersNormalized = new ArrayList();

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

			/** @note  Note for normalizing clusters:
			 The clusters will be each normalized to the largest peak found in the histogram
			 This corresponds to the first cluster in the list "clusters"
			 It is possible that clusters will have means less than the normalization mean,
			 signifying that they happen before the main peak. This is slightly problematic.
			 For now we just drop any clusters that occur before the first main one.
            */
			
			if(clusters.size() > 0){
 				ListIterator i = clusters.listIterator();
				Duration baseMean = ((Cluster) clusters.get(0)).mean();

				while(i.hasNext()){
					Cluster c = (Cluster) i.next();
					if(c.sum().us()-baseMean.us()*c.count() >= 0.0){
						clustersNormalized.add(new Cluster(c.sum().us()-baseMean.us()*c.count(),c.count(),c.events));
					}
				}
			}

		}


		public String clusters_toString(ArrayList clusters){
			String result = "";
			ListIterator i = clusters.listIterator();
			while(i.hasNext()){
				Cluster c = (Cluster) i.next();
				result = result + "{ duration= " + c.mean() + ", count=" + c.count() + " wes=" + c.events.size() + " } ";
			}
			return result;
		}

		public String clusters_toString(){
			return clusters_toString(clusters);
		}

		public String clusters_toString_Normalized(){
			return clusters_toString(clustersNormalized);
		}


		public Duration binCenter(int whichBin){
			return new Duration((whichBin+0.5)*binWidth.us());
		}

		public Duration binLowerBound(int whichBin){
			return new Duration(whichBin*binWidth.us());
		}

		public Duration binUpperBound(int whichBin){
			return new Duration((whichBin+0.5)*binWidth.us());
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
		startTime = startInterval;
		endTime = endInterval;

		osQuanta = new Duration();
		osQuanta.set_ms(100); // @todo. This should be recovered from a new type of entry in the projection log. linux 2.16 default is 100ms . This should be updated

		loggingText = "";
	}


	/** Do the gathering and processing of the data */
	public void gatherData(Component parent)
	{
		GenericLogReader LogFile;
		LogEntryData logdata = new LogEntryData();

		int currPeIndex = 0;
		int currPe;

		long previous_begin_time=-1;
		int  previous_begin_entry=-1;

		/** Track whether there's no intermediate event in a black part */
		long previous_black_time = -1;

		ProgressMonitor progressBar = new ProgressMonitor(parent, "Mining for Computational Noise","", 0, numPe);

		/** Number of entry methods in this set of trace logs */
		int numEvents = MainWindow.runObject[myRun].getEntryCount();
		
		int blackPartIdx = numEvents;		
		
		LinkedList results = new LinkedList();
		
		// For each pe
		while (peList.hasMoreElements()) {
			
			System.gc();

			/** The histograms for this processor.
			 * @note the indices 0 to numEvents-1 are for the entry methods
			 *       while index numEvents is for the black regions(no entry method, non-idle). 
			 */ 
			Histogram h[] = new Histogram[numEvents+1];
			for(int i=0;i<h.length;i++){
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

			LogFile = new GenericLogReader(MainWindow.runObject[myRun].getLogName(currPe), MainWindow.runObject[myRun].getVersion());

			try {

				LogFile.nextEventOnOrAfter(startTime, logdata);
				while (logdata.time < endTime) {
					//System.out.println("Time=" + logdata.time + " event " + logdata.event);
					//Go through each log file and for each Creation
					//  in a Processing Block, update arrays
					if(logdata.type == BEGIN_PROCESSING){
						previous_begin_time = logdata.time;
						previous_begin_entry = logdata.entry;

					} else if(logdata.type == END_PROCESSING){
						// if we have seen the matching BEGIN_PROCESSING
						if(previous_begin_entry == logdata.entry){

							h[logdata.entry].insert(new TimelineEvent(previous_begin_time,logdata.time,-1,currPe));
					
						}
					}

					//process the black part
					//Four cases for a black part
					//1. END_PROCESSING to BEGIN_IDLE
					//2. END_PROCESSING to BEGIN_PROCESSING
					//3. END_IDLE to BEGIN_PROCESSING
					//4. END_IDLE to BEGIN_IDLE
					//we need to ensure there's no intermeidate events between the two events
					//so previous_black_time is used to ensure this if the value of it is not -1					
					if(logdata.type == END_PROCESSING || logdata.type == END_IDLE){
					    previous_black_time = logdata.time;
					}else if(logdata.type == BEGIN_PROCESSING || logdata.type == BEGIN_IDLE){
					    if(previous_black_time != -1){
						h[blackPartIdx].insert(new TimelineEvent(previous_black_time, logdata.time, -1, currPe));
					    }
					}else{
					    //other events
					    previous_black_time = -1;
					}
					LogFile.nextEvent(logdata);				
				}
			}
			catch (IOException e)
			{
				// I doubt we'll ever get here
			}

			currPeIndex++;

			
			// Generate clusters from each histogram
			for(int i=0;i<h.length;i++){
				h[i].cluster();
			}


			// Merge all the normalized clusters for this pe
			// i.e. merge clusters from all entry methods
			Histogram h_pe = new Histogram();
			for(int i=0;i<h.length;i++){
				// for each normalized cluster
		
				ListIterator itr = h[i].clustersNormalized().listIterator();
				while(itr.hasNext()){
					Cluster c = (Cluster) itr.next();
					h_pe.insert(c);
				}
			}

			// Generate clusters for the processor
			h_pe.cluster();


			int n = 1;
			while(h_pe.hasNthNoiseComponent(n)){
//				h_pe.nthNoise(n).events.buildFFT();

				EventWindow ew = h_pe.nthNoise(n).events;
				long occurrences = h_pe.nthNoise(n).count();
				Duration duration = h_pe.nthNoise(n).mean();
				
				Duration periodicity = ew.period();
//				Duration periodicity_fft = ew.periodFromFFT();
				
				results.add(new NoiseResult(duration, occurrences, currPe, ew));

				n++;
			}

		}// end for each pe

		
		// cluster all result records across all processors
		finalResults = clusterResultsAcrossProcs(results);

		results = null;

		finalResults = filterResults(finalResults);
		
		progressBar.close();
	}

	public String getText(){
		String s = "NoiseMiner Text Report:\n";
		s = s + "Time range " + startTime + " to " + endTime + "\n";
		s = s + loggingText;
		return s;
	}

	
	/** An extended JButton that also contains a reference to some NoiseMiner results */
	public class NoiseResultButton extends JButton {
		public NoiseResult nr;

		public NoiseResultButton(String label, NoiseResult nr){
			super(label);
			this.nr = nr;
		}
		
		/** Create a nice window displaying timelines for the entries in nr */
		public void display(){
			NoiseMinerExemplarTimelineWindow nmetw = new NoiseMinerExemplarTimelineWindow(nr);  
			nmetw.setSize(1000,600);
			nmetw.setVisible(true);
		}
	}
	
	
	private class Cluster{
		private Duration sum;
		private long count;
		EventWindow events;
	
		Cluster(Duration s, long c, EventWindow ew){
			sum = new Duration(s);
			count=c;
//			assert(c>=0);
			events = new EventWindow(eventsInBinWindow);
			events.merge(ew);
		}
		
		Cluster(double s_us, long c, EventWindow ew){
//			assert s_us>=0 : ("s_us=" + s_us);
			sum= new Duration(s_us);
			count=c;
//			assert(c>=0);
			events = new EventWindow(eventsInBinWindow);
			events.merge(ew);
		}
		
		
		public void merge(Duration s, long c, EventWindow ew){
			sum.add(s);
			count += c;
//			assert(c>=0);
			events.merge(ew);
		}
	
		public void merge(Cluster c){
			sum.add(c.sum);
			count += c.count;
//			assert(count>=0.0);
			events.merge(c.events);
		}
	
		Duration mean(){
//			assert(sum.us()/count>=0.0);
			Duration d = new Duration(sum.us()/count);
			return d;
		}
		long count(){
//			assert(count>=0);
			return count;
		}
		Duration sum(){
//			assert(sum.d>=0.0);
			return sum;
		}
	}

	/** Create an array of objects representing the internal noise components */
	public Vector getResultsTable(){
		Collections.sort(finalResults);

		// scan through results to find only the ones with periodicity close to or longer than the OS time quanta

		int numResultRows = finalResults.size();

		Vector resultTable = new Vector();


		Iterator itr = finalResults.iterator();

		while(itr.hasNext()){
			NoiseResult v = (NoiseResult) itr.next();
			
			Vector row = new Vector();
			
			row.add(new String("" + v.duration ));
			row.add(new String("" + v.pe_toString() ));
			row.add(new String("" + v.occurrences ));
			row.add(new String("" + v.period()));
			
			if(v.period().us() < osQuanta.us()*0.8 ) {
				row.add(new String("internal"));
			} else {
				row.add(new String("external"));
			}

			row.add(new NoiseResultButton("view",v)); // will turn into a button
			
			resultTable.add(row);
				
		}


		// If we have no data, put a string into the table
		if(resultTable.size() == 0){
			Vector row = new Vector();
			row.add(new String("No Noise Components Found"));
			row.add(new String("n/a"));
			row.add(new String("n/a"));
			row.add(new String("n/a"));
			row.add(new String("n/a"));
			resultTable.add(row);
		}

		return resultTable;
	}

	
}
