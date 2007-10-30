package projections.analysis;

import projections.misc.*;
import projections.gui.*;

import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
//import java.math.BigInteger;
//import jnt.FFT.*;


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
 *  @todo Find non-idle non-event stretches as well
 *  @todo Eliminate any noise components that are just associated with a single event
 *  @todo add higher order approximation of periodicity
 *  @todo add a max,min periodicity to give a sanity check on the window
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

	protected double peMergeDistance = 0.2;

	private LinkedList<NoiseResult> results;
	private LinkedList<NoiseResult> resultsClustered;

	private class Time {

		protected double d; // stores duration in us
		
		public Time(){	
			d = 0.0;
			assert(d>=0.0);
		}
		
		public Time(long us){
			d = us;
			assert(d>=0.0);
		}
		
		public Time(Time p){
			d = p.d;
			assert(d>=0.0);
		}
		
		public Time(double us){
			d = us;
			assert(d>=0.0);
		}
		
		public void set_ms(double p){
			d = p * 1000.0;
			assert(d>=0.0);
		}
		public void set_us(double p){
			d = p;
			assert(d>=0.0);
		}
		public void set(Duration p){
			d = p.d;
			assert(d>=0.0);
		}
		public double us(){
			assert(d>=0.0);
			return d;
		}
		public double ms(){
			assert(d>=0.0);
			return d/1000.0;
		}
		
		public String toString(){
			assert(d>=0.0);
			if(d > 1000.0){
				return String.format("%.2f(ms)", d/1000.0 );
			} else {
				return String.format("%.2f(us)", d );
			}
				
		}
		
		public void add(Time p){
			d += p.d;
			assert(d>=0.0);
		}
		
		/** add a given amount of microseconds to this Duration */
		public void add_us(double p){
			d += p;
			assert(d>=0.0);
		}
		
		/** add a given amount of microseconds to this Duration */
		public void add_us(long p){
			d += p;
			assert(d>=0.0);
		}
			
	}

	/** essentially an alias to Time class */
	private class Duration extends Time{
		public Duration(){	
			d = 0.0;
		}

		public Duration(long us){
			d = us;
			assert(d>=0.0);
		}

		public Duration(Time p){
			d = p.d;
			assert(d>=0.0);
		}
		
		public Duration(Time start, Time end){
			d = end.d - start.d;
			assert(d>=0.0);
		}

		public Duration(long start_us, long end_us){
			d = end_us - start_us;
			assert(d>=0.0);
		}

		public Duration(double start_us, double end_us){
			d = end_us - start_us;
			assert(d>=0.0);
		}
		
		public Duration(double us){
			d = us;
			assert d>=0.0 : ("us=" + us);
		}

	}
	
	/** A resulting cluster of noise occurrences */
	private class NoiseResult implements Comparable{
		public LinkedList<Integer> pes; // list of processors on which the event occurs
		public Duration periodicity; // The average period determined by the span of the window
		//public Duration periodicity_FFT; // The primary peak in the FFT
		public Duration duration; // The amount of time spend in the noise
		public long occurrences; // The number of times this event occurred per processor
		public eventWindow ew; 
		
		
		public NoiseResult(Duration d, long o, Duration p, int pe, eventWindow ew){
			pes = new LinkedList<Integer>();

			this.ew = ew;
			
			pes.add(pe);
			periodicity = p;
			//periodicity_FFT = p_fft;
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
			return s;
		}

		/** A distance measure between this and another NoiseResult. Incorporates both periodicity and duration */
		public double distance(NoiseResult nr){
			double result = 0.0;
			result += 0.5 * (Math.abs((double)periodicity.us() - (double)nr.periodicity.us()) / (double)periodicity.us());
			result += 1.0 * (Math.abs((double)duration.us() - (double)nr.duration.us()) / Math.max((double)duration.us(),(double)nr.duration.us()));
			return result;
		}

		public void merge(NoiseResult nr){
			pes.addAll(nr.pes);
			periodicity.set_us((periodicity.us() * occurrences + nr.periodicity.us() * nr.occurrences) / (occurrences+nr.occurrences));
			//periodicity_FFT.set_us((periodicity_FFT.us() * occurrences + nr.periodicity_FFT.us() * nr.occurrences) / (occurrences+nr.occurrences));
			duration.set_us((duration.us() * occurrences + nr.duration.us() * nr.occurrences) / (occurrences+nr.occurrences));
			occurrences += nr.occurrences;
		}


		public int compareTo(Object other){
			assert(other != null);
			assert(other instanceof NoiseResult);
			NoiseResult nr = (NoiseResult)other;

			double d = nr.duration.us()*nr.occurrences-duration.us()*occurrences;

			if(d<0){
				return -1;
			} else if(d>0) {
				return 1;
			} else {
				return 0;
			}

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
		public TreeSet<TimelineEvent> occurrences; // essentially a sorted list or heap
		private int max;
		protected Duration period;
		protected Duration prominentPeriod;

		eventWindow(int maxSize){
			occurrences = new TreeSet<TimelineEvent>();
			max = maxSize;
			period = new Duration();
			prominentPeriod = new Duration();
		}

		/** Need to keep track of the time, duration, and event id, PE of the event, not just the time */
		public void insert(TimelineEvent e){
			occurrences.add(e);
			if(occurrences.size() > max){
				occurrences.remove(occurrences.first());
			}
		}

		public void merge(eventWindow ew){
			Iterator<TimelineEvent> itr = ew.occurrences.iterator();
			while(itr.hasNext()){
				TimelineEvent v = itr.next();
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
			return occurrences.first();
		}

		public TimelineEvent getLast(){
			return occurrences.last();
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








	private class Histogram{
		private long bin_count[]; // The number of values that fall in each bin
		private Duration bin_sum[]; // The sum of all values that fall in each bin

		private eventWindow bin_window[]; // A list of recent events in each bin
		int eventsInBinWindow;

		private int nbins;
		private Duration binWidth;
		private Duration total_sum;
		private long total_count;
		private boolean used;
		

		private class Cluster{
			private Duration sum;
			private long count;
			eventWindow events;

			Cluster(Duration s, long c, eventWindow ew){
				sum = new Duration(s);
				count=c;
				assert(c>=0);
				eventsInBinWindow = 50;
				events = new eventWindow(eventsInBinWindow);
				events.merge(ew);
			}
			
			Cluster(double s_us, long c, eventWindow ew){
				assert s_us>=0 : ("s_us=" + s_us);
				sum= new Duration(s_us);
				count=c;
				assert(c>=0);
				eventsInBinWindow = 50;
				events = new eventWindow(eventsInBinWindow);
				events.merge(ew);
			}
			
			
			public void merge(Duration s, long c, eventWindow ew){
				sum.add(s);
				count += c;
				assert(c>=0);
				events.merge(ew);
			}

			public void merge(Cluster c){
				sum.add(c.sum);
				count += c.count;
				assert(count>=0.0);
				events.merge(c.events);
			}

			Duration mean(){
				assert(sum.us()/count>=0.0);
				Duration d = new Duration(sum.us()/count);
				return d;
			}
			long count(){
				assert(count>=0);
				return count;
			}
			Duration sum(){
				assert(sum.d>=0.0);
				return sum;
			}
		}

		public int countEvents(){
			int c=0;
			for(int i=0;i<nbins;i++){
				c+=bin_window[i].size();
			}
//			System.out.println("total events in all bin windows:" + c);
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
		public boolean hasNthNoiseComponent(int n){
			return (clustersNormalized.size()>n);
		}





		public Histogram(){
			used=false;
			nbins = 20;
			binWidth = new Duration(500);
			total_sum = new Duration(0);
			
			eventsInBinWindow = 80;
			bin_count = new long[nbins];
			bin_sum = new Duration[nbins];
			bin_window = new eventWindow[nbins];
			for(int i=0;i<nbins;i++){
				bin_count[i] = 0;
				bin_sum[i] = new Duration(0.0);
				bin_window[i] = new eventWindow(eventsInBinWindow);
			}
		}

		public void insert(TimelineEvent event) {
			Duration duration = new Duration(event.EndTime-event.BeginTime);
			used=true;
			total_count ++;
			total_sum.add(duration);
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


//		public void insert(Duration duration, long when, long occurrences){
//			if(occurrences > 0){
//				used=true;
//				total_count += occurrences;
//				total_sum.add_us(duration.us()*occurrences);
//				int which_bin = (int)(duration.us() / binWidth.us());
//				if(which_bin > nbins-1){
//					which_bin = nbins-1;
//				}
//				if(which_bin >= 0){
//					bin_count[which_bin] +=occurrences;
//					bin_sum[which_bin].add_us(duration.us()*occurrences);
//					bin_window[which_bin].insert(when);
//				}
//			}
//		}

		/**  Insert a cluster to this histogram, including its event window. Put the whole cluster into the
		 *   bin matching the cluster mean
		 */
		public void insert(Cluster c){
			if(c.count() > 0){
				used=true;
				total_count += c.count();
				total_sum.add(c.sum());
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
			return (total_count > 200);
		}


		/**
		 *  Filter out clusters that do not have sufficient contribution to overall computation time 
		 *  If the ratio of the duration to the periodicity is not greater than the cutoff the cluster is ignored.
		 *  This removes clusters that only add a tiny portion of noise infrequently.
     *  NOTE, THIS IS CURRENTLY BROKEN
		 */
		public void filterNormalizedClusters(double cutoff_contribution){
			// Create normalized versions of clusters
			ArrayList<Cluster> clustersFiltered = new ArrayList<Cluster>();
			ListIterator<Cluster> i = clustersNormalized.listIterator();
			while(i.hasNext()){
				Cluster c = i.next();
				if(c.count() > 0){
					Duration periodicity = new Duration(c.events.period());
					Duration duration = new Duration(c.mean());
					if( duration.us() / periodicity.us() > cutoff_contribution ){
//            System.out.println("Keeping cluster with duration=" + duration.us() + " and periodicity " + periodicity.us() );
            clustersFiltered.add(c);
					}
          else {
//            System.out.println("Dropping cluster with duration=" + duration.us() + " and periodicity " + periodicity.us() ); 
            // clustersFiltered.add(c); 
          }
				}
			}
			clustersNormalized = clustersFiltered;
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

			/** @note  Note for normalizing clusters:
			 The clusters will be each normalized to the largest peak found in the histogram
			 This corresponds to the first cluster in the list "clusters"
			 It is possible that clusters will have means less than the normalization mean,
			 signifying that they happen before the main peak. This is slightly problematic.
			 For now we just drop any clusters that occur before the first main one.
            */
			
			if(clusters.size() > 0){
        //				System.out.println("clusters.size()=" + clusters.size());
				ListIterator<Cluster> i = clusters.listIterator();
				Duration baseMean = clusters.get(0).mean();

				while(i.hasNext()){
					Cluster c = i.next();
					if(c.sum().us()-baseMean.us()*c.count() >= 0.0){
						clustersNormalized.add(new Cluster(c.sum().us()-baseMean.us()*c.count(),c.count(),c.events));
					}
				}
			}

		}


		public String clusters_toString(ArrayList<Cluster> clusters){
			String result = "";
			ListIterator<Cluster> i = clusters.listIterator();
			while(i.hasNext()){
				Cluster c = i.next();
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

		results = new LinkedList<NoiseResult>();

		osQuanta = new Duration();
		osQuanta.set_ms(100); // @todo. This should be recovered from a new type of entry in the projection log. linux 2.16 default is 100ms . This should be updated

		loggingText = "";
	}


	public void gatherData(Component parent)
	{
		GenericLogReader LogFile;
		LogEntryData logdata = new LogEntryData();

		int currPeIndex = 0;
		int currPe;

		long previous_begin_time=-1;
		long previous_end_time=-1;
		int  previous_begin_entry=-1;
		int  previous_end_entry=-1;

		ProgressMonitor progressBar = new ProgressMonitor(parent, "Mining for Computational Noise","", 0, numPe);

		String[][] entryNames = MainWindow.runObject[myRun].getEntryNames(); // needed to determine number of events
		int numEvents = entryNames.length;
		
		// For each pe

		long total_count=0;
		while (peList.hasMoreElements()) {
			
			System.gc();

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

							h[logdata.entry].insert(new TimelineEvent(previous_begin_time,logdata.time,-1,logdata.pe));
							
							total_count++;
						}
					}

					LogFile.nextEvent(logdata);
					
				}


			}

			catch (IOException e)
			{
				// I doubt we'll ever get here
			}

			currPeIndex++;

			// print each event's histogram

			for(int i=0;i<numEvents;i++){
				if(h[i].haveManySamples() || true){
					h[i].cluster();
          //					System.out.println("Clusters for event:" + i + " pe:" + currPe + " are: " + h[i].clusters_toString() );
				}
			}


			// Merge all the events across this pe
//			System.out.println("Creating histogram for pe");
			Histogram h_pe = new Histogram();
			for(int i=0;i<numEvents;i++){
				if(h[i].haveManySamples()){
					// for each normalized cluster
					ListIterator<Histogram.Cluster> itr = h[i].clustersNormalized().listIterator();
					while(itr.hasNext()){
						Histogram.Cluster c = itr.next();
						h_pe.insert(c);
					}
				}
			}


			h_pe.cluster();

			//	String entryName = entryNames[i][0];
			// entryName = entryName.split("\\(")[0];


			// Look at each noise component and create an result record


			// filter the per-processor clusters
      //      h_pe.filterNormalizedClusters(0.0001);

			int n = 1;
			while(h_pe.hasNthNoiseComponent(n)){
//				h_pe.nthNoise(n).events.buildFFT();

				eventWindow ew = h_pe.nthNoise(n).events;
				
				long occurrences = h_pe.nthNoise(n).count();
				
				Duration duration = h_pe.nthNoise(n).mean();
				
				Duration periodicity = ew.period();
//				Duration periodicity_fft = ew.periodFromFFT();
				
				if(occurrences > 5){
					loggingText = loggingText + "Noise component " + n + " on processor " + currPe + " has duration of about " + duration + " and occurs " + occurrences + " times with periodicity " + periodicity + "\n";

					results.add(new NoiseResult(duration, occurrences, periodicity, /* periodicity_fft,*/ currPe, ew));
				}

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

			if(v.periodicity.us() < osQuanta.us()*0.8 ) {

				Vector row = new Vector();

				row.add(new String("" + v.duration ));
				row.add(new String("" + v.pe_toString() ));
				row.add(new String("" + v.occurrences ));
				row.add(new String("" + v.periodicity));
//				row.add(new String("" + v.periodicity_FFT));
//				row.add(new JButton("Test")); // button to go here
				
				resultTable.add(row);
			}
		}


		// If we have no data, put a string into the table
		if(resultTable.size() == 0){
			Vector row = new Vector();
			row.add(new String("No Noise Components Found"));
			row.add(new String("n/a"));
			row.add(new String("n/a"));
			row.add(new String("n/a"));
//			row.add(new String("n/a"));
			resultTable.add(row);
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

			if(v.periodicity.us() >= osQuanta.us()*0.8) {

				Vector row = new Vector();

				row.add(new String("" + v.duration ));
				row.add(new String("" + v.pe_toString() ));
				row.add(new String("" + v.occurrences ));
				row.add(new String("" + v.periodicity));
//				row.add(new String("" + v.periodicity_FFT));

				resultTable.add(row);
			}
		}

		// If we have no data, put a string into the table
		if(resultTable.size() == 0){
			Vector row = new Vector();
			row.add(new String("No Noise Components Found"));
			row.add(new String("n/a"));
			row.add(new String("n/a"));
			row.add(new String("n/a"));
//			row.add(new String("n/a"));
			resultTable.add(row);
		}

		return resultTable;
	}

}
