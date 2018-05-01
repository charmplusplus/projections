package projections.Tools.NoiseMiner;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.TreeMap;

import projections.Tools.NoiseMiner.NoiseMiner.Cluster;
import projections.Tools.NoiseMiner.NoiseMiner.Duration;
import projections.Tools.NoiseMiner.NoiseMiner.Event;
import projections.Tools.NoiseMiner.NoiseMiner.EventWindow;
import projections.Tools.NoiseMiner.NoiseMiner.Histogram;
import projections.analysis.Analysis;
import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.analysis.TimelineEvent;
import projections.misc.LogEntryData;

class NoiseMinerThread implements Runnable {
	private int pe;
	private TreeMap h;
	private Analysis analysis;
	private NoiseMiner parent;
		
	private	Histogram h_pe;
	
	/** The results for one PE. After this thread has run, this should be merged together with those from other PEs */
	protected LinkedList results;
	protected long [] histogramToDisplay;
	
	
	protected NoiseMinerThread(int pe, Analysis analysis, NoiseMiner parent){
		this.setPe(pe);
		this.analysis = analysis;
		this.parent = parent;
	}

	public void run() {
		
//		System.out.println("PE " + pe + " loading");
		
		LogEntryData logdata = new LogEntryData();

		long previous_begin_time = -1;
		int previous_begin_entry = -1;
		
		int encountered_user_event = -1;
		
		/** Track whether there's no intermediate event in a black part */
		long previous_black_time = -1;

		int blackPartIdx = -2;		
		

		/** The histograms for this processor.
		 * @note the indices 0 to numEvents-1 are for the entry methods
		 *       while index numEvents is for the black regions(no entry method, non-idle). 
		 */ 


		/** Histograms for each type of event. Each entry is of type "Event" */
		h = new TreeMap();
			
		
		GenericLogReader reader = new GenericLogReader(getPe(), analysis.getVersion());

		try {

			logdata = reader.nextEventOnOrAfter(parent.getStartTime());
			while (logdata.time < parent.getEndTime()) {
				//System.out.println("Time=" + logdata.time + " event " + logdata.event);
				//Go through each log file and for each Creation
				//  in a Processing Block, update arrays
				if(logdata.type == ProjDefs.BEGIN_PROCESSING){
					previous_begin_time = logdata.time;
					previous_begin_entry = logdata.entry;

				} else if(logdata.type == ProjDefs.END_PROCESSING){
					// if we have seen the matching BEGIN_PROCESSING
					if(previous_begin_entry == logdata.entry){
						Event e = parent.new Event(logdata.entry, encountered_user_event);
						if(! h.containsKey(e)){
							h.put(e, parent.new Histogram());
						}							
						((Histogram)h.get(e)).insert(new TimelineEvent(previous_begin_time,logdata.time,-1,getPe()));
						encountered_user_event = -1;
					}
				} else if(logdata.type == ProjDefs.USER_EVENT || logdata.type == ProjDefs.USER_EVENT_PAIR){
					encountered_user_event = logdata.userEventID;
				}


				//process the black part
				//Four cases for a black part
				//1. END_PROCESSING to BEGIN_IDLE
				//2. END_PROCESSING to BEGIN_PROCESSING
				//3. END_IDLE to BEGIN_PROCESSING
				//4. END_IDLE to BEGIN_IDLE
				//we need to ensure there's no intermeidate events between the two events
				//so previous_black_time is used to ensure this if the value of it is not -1					
				if(logdata.type == ProjDefs.END_PROCESSING || logdata.type == ProjDefs.END_IDLE){
					previous_black_time = logdata.time;
				}else if(logdata.type == ProjDefs.BEGIN_PROCESSING || logdata.type == ProjDefs.BEGIN_IDLE){
					if(previous_black_time != -1){

						Event e = parent.new Event(blackPartIdx, -1);
						if(! h.containsKey(e)){
							h.put(e, parent.new Histogram());
						}							
						((Histogram)h.get(e)).insert(new TimelineEvent(previous_black_time, logdata.time, -1, getPe()));

					}
				}else{
					//other events
					previous_black_time = -1;
				}
				logdata = reader.nextEvent();				
			}
		}
		catch (EndOfLogSuccess e) {
			// successfully read log file
		}catch (IOException e) {
			// I doubt we'll ever get here
		}


		try {
			reader.close();
		} catch (IOException e1) {
			System.err.println("Error: could not close log file reader for processor " + pe );
		}
		
		

		// Generate clusters from each histogram
		// Merge all the normalized clusters for this pe
		// i.e. merge clusters from all entry methods
		h_pe = parent.new Histogram();

		Iterator iter = h.values().iterator();
		while(iter.hasNext()){
			Histogram hist = (Histogram) iter.next();
			hist.cluster();

			ListIterator itr = hist.clustersNormalized().listIterator();
			while(itr.hasNext()){
				Cluster c = (Cluster) itr.next();
				h_pe.insert(c);
			}
		}


		// Generate clusters for the processor
		h_pe.cluster();
		
		System.out.println("PE " + pe + " loaded");
		
		results = new LinkedList();
		
		int n = 1;
		while(h_pe.hasNthNoiseComponent(n)) {

			EventWindow ew = h_pe.nthNoise(n).events;
			long occurrences = h_pe.nthNoise(n).count();
			Duration duration = h_pe.nthNoise(n).mean();

			results.add(parent.new NoiseResult(duration, occurrences, pe, ew));

			n++;
		}
		
		
		histogramToDisplay = new long[parent.numDisplayBins];
		for(int i=0;i<parent.numDisplayBins;i++){
			histogramToDisplay[i] = 0;
		}
		
		int numOldBinsPerNewBin = parent.numOldBinsPerNewBin();
				
		Iterator eventTypeIter = h.keySet().iterator();
		while(eventTypeIter.hasNext()){
			Histogram hist = (Histogram) h.get(eventTypeIter.next());

			for(int i=0;i<parent.getNbins() ;i++){
				int newbin =  i / numOldBinsPerNewBin;
//				System.out.println("newbin="+newbin);
				histogramToDisplay[newbin] += hist.bin_count[i];
			}
			
		}
			
		h=null;
		h_pe = null;
//		System.out.println("PE " + pe + " done");
	}

	public void setPe(int pe) {
		this.pe = pe;
	}

	public int getPe() {
		return pe;
	}
}
