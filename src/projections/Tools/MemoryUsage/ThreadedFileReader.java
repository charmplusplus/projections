package projections.Tools.MemoryUsage;

import java.io.IOException;

import org.jfree.data.xy.XYSeries;

import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.gui.MainWindow;
import projections.misc.LogEntryData;


class ThreadedFileReader implements Runnable  {

	private int pe;
	private int myRun;
	private XYSeries series;
	private long intervalSize;
	private long startInterval;
	private long endInterval;
	private double timeScalingFactor;

	private double maxUsage[];
	
	/** Construct a file reading thread that will determine the best EP representative for each interval
	 *  
	 *  The resulting output data will be assigned into the array specified without synchronization
	 * @param utilizationData 
	 *  
	 *  */
	protected ThreadedFileReader(int pe, int myRun, long intervalSize, long startInterval, long endInterval, double timeScalingFactor){
		this.pe = pe;
		this.myRun = myRun;
		this.intervalSize = intervalSize;
		this.startInterval = startInterval;
		this.endInterval = endInterval;
		this.timeScalingFactor = timeScalingFactor;
	}

	public void run() { 
		GenericLogReader reader = new GenericLogReader(MainWindow.runObject[myRun].getLog(pe), pe, MainWindow.runObject[myRun].getVersion());

		int numIntervals = (int) (endInterval - startInterval);

		// First take data and put it into intervals.
		maxUsage = new double[numIntervals];

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
		catch (EndOfLogSuccess e) {
			// Done reading file
		} catch (IOException e) {
			// Error reading file
		}


		try {
			reader.close();
		} catch (IOException e1) {
			System.err.println("Error: could not close log file reader for processor " + pe );
		}
		
		// Put data from intervals into a time series
		series = new XYSeries("PE " + pe);

		for(int i=0;i<numIntervals;i++){
			long time = (startInterval + i) * intervalSize +  (intervalSize/2);
			if(maxUsage[i] > 0){
				series.add(time * timeScalingFactor, maxUsage[i]);
			} else {
				series.add(time * timeScalingFactor,null);
			}
		}


	}

	public double[] getData(){
		return maxUsage;
	}
	
	protected XYSeries getMemorySamples(){
		return series;
	}

	public long getPe() {
		return pe;
	}


}





