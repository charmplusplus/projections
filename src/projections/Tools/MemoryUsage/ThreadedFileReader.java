package projections.Tools.MemoryUsage;

import java.io.EOFException;
import java.io.IOException;
import java.util.TreeMap;

import org.jfree.data.xy.XYSeries;

import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.gui.MainWindow;
import projections.misc.LogEntryData;


class ThreadedFileReader extends Thread  {

	private int pe;
	private int myRun;
	private XYSeries series;
	long intervalSize;
	long startInterval;
	long endInterval;

	/** Construct a file reading thread that will determine the best EP representative for each interval
	 *  
	 *  The resulting output data will be assigned into the array specified without synchronization
	 * @param utilizationData 
	 *  
	 *  */
	protected ThreadedFileReader(int pe, int myRun, long intervalSize, long startInterval, long endInterval){
		this.pe = pe;
		this.myRun = myRun;
		this.intervalSize = intervalSize;
		this.startInterval = startInterval;
		this.endInterval = endInterval;
	}

	private TreeMap<Long,Long> memorySamples;

	public void run() { 
		memorySamples = new TreeMap<Long,Long>();
		GenericLogReader reader = new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());

		int numIntervals = (int) (endInterval - startInterval);

		// First take data and put it into intervals.
		double maxUsage[] = new double[numIntervals];

		int count = 0;
		try {
			while (true) {
//				System.out.println("c before " + (count));
				LogEntryData data = reader.nextEvent();
//				System.out.println("c after " + (count));
				count++;

				double memMB = (double)data.memoryUsage / 1048576.0;
				int interval = (int) (data.time / intervalSize);			

				if(interval < endInterval && interval >= startInterval && data.type == ProjDefs.MEMORY_USAGE){
					if(memMB > maxUsage[interval])
						maxUsage[interval] = memMB;
				}

			}
		}
		catch (EOFException e) {
			// Done reading file
//			System.out.println("EOFException c after " + (count));
		} catch (IOException e) {
			// Error reading file
//			System.out.println("IOException c after " + (count));
		}


		// Put data from intervals into a time series
		series = new XYSeries("PE " + pe);

		for(int i=0;i<numIntervals;i++){
			if(maxUsage[i] > 0){
				long time = (startInterval + i) * intervalSize +  (intervalSize/2);
				series.add(time, maxUsage[i]);
			}
		}


	}

	protected XYSeries getMemorySamples(){
		return series;
	}

	public long getPe() {
		return pe;
	}


}





