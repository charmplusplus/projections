package projections.Tools.MemoryUsage;

import java.io.EOFException;
import java.io.IOException;

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
	double timeScalingFactor;

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
			long time = (startInterval + i) * intervalSize +  (intervalSize/2);
			if(maxUsage[i] > 0){
				series.add(time * timeScalingFactor, maxUsage[i]);
			} else {
				series.add(time * timeScalingFactor,null);
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





