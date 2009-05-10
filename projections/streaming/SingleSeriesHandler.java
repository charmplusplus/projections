package projections.streaming;

import java.awt.BorderLayout;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

import projections.SamplePlots.MyActionHandler;
import projections.ccs.CcsProgress;
import projections.ccs.CcsThread;

public class SingleSeriesHandler {

	CcsThread ccs;

	Vector<Float> averageTimes;
	Vector<Float> allTimes;

	XYPlot plot;

	private String server;
	private int port;
	private String ccsHandler;

	static boolean REDO_DISPLAY_EACH_TIME = false;

	XYSeries dataSeries;
	DefaultTableXYDataset dataset;
	
	public static double arr2double (byte[] arr, int start) {
		int i = 0;
		int len = 8;
		int cnt = 0;
		byte[] tmp = new byte[len];
		for (i = start; i < (start + len); i++) {
			tmp[cnt] = arr[i];
			cnt++;
		}
		long accum = 0;
		i = 0;
		for ( int shiftBy = 0; shiftBy < 64; shiftBy += 8 ) {
			accum |= ( (long)( tmp[i] & 0xff ) ) << shiftBy;
			i++;
		}

		// Swap Endians if needed
		long b0 = (accum >>  0) & 0xff;
		long b1 = (accum >>  8) & 0xff;
		long b2 = (accum >> 16) & 0xff;
		long b3 = (accum >> 24) & 0xff;
		long b4 = (accum >> 32) & 0xff;
		long b5 = (accum >> 40) & 0xff;
		long b6 = (accum >> 48) & 0xff;
		long b7 = (accum >> 56) & 0xff;

		long swapped =  b0 << 56 | b1 << 48 | b2 << 40 | b3 << 32 | b4 << 24 | b5 << 16 | b6 <<  8 | b7 ;

		return Double.longBitsToDouble(swapped);
	}

	public static int unsignedByteToInt(byte b) {
		return (int) b & 0xFF;
	}


	public class progressHandler implements CcsProgress {
		public progressHandler(){

		}

		public void setText(String s) {
			System.out.println("CCS Message: " + s);
		}
	}


	private class dataRequest extends CcsThread.request{
		// store calling panel here if needed
		public dataRequest() {
			super(ccsHandler,0);
		}

		public void handleReply(byte[] data){
			int numData = 1;
			double sum = 0.0;
			System.out.println("Received " + data.length + " byte data array\n");
			int previousEntries = allTimes.size();

			if(ccsHandler.equals("CkPerfSummaryCcsClientCB")){

				numData = data.length / 8;
				for(int i=0; i<numData; i++){
					double v = arr2double (data, 8*i);
					sum += v;
					if(v >= 0.0){
						allTimes.add(new Float(v));
					}
				}

			} else if(ccsHandler.equals("CkPerfSummaryCcsClientCB uchar")){

				numData = data.length / 1;
				for(int i=0; i<numData; i++){
					Float v = new Float(unsignedByteToInt(data[i]));

					// Range of values supplied by this ccs handler is 0 to 200. Convert to percentages.
					v /= 2.0f;

					sum += v;
					if(v < 255.0){
						allTimes.add(v);
					}
				}

			}


			if(! REDO_DISPLAY_EACH_TIME){
				int numInputPerDataPoint = 100;
				if(allTimes.size()-previousEntries > numInputPerDataPoint * 100){
					previousEntries = allTimes.size() - numInputPerDataPoint * 100;
				}
				for(int i=previousEntries; (i+numInputPerDataPoint-1)<allTimes.size(); i+=numInputPerDataPoint){
					double s = 0.0;
					for(int j=0;j<numInputPerDataPoint;j++){
						s += allTimes.elementAt(j+i);		
					}
					double avg = s / numInputPerDataPoint;
					dataSeries.add(i,avg);

				}

			}

			float avg = (float) (sum / (float)numData);

			if(avg >= 0.0){
				averageTimes.add(avg);
				System.out.println("Average of " + numData + " input values received from ccs is " + avg + ". " + averageTimes.size() + " non-negative averages so far");
				updatePlot();
			}


			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// Request more data
			ccs.addRequest(this);
		}

	}


	/** Constructor */	
	SingleSeriesHandler(String hostname, int port, String ccsHandler){

		System.out.println("StreamingDataHandler constructor");

		averageTimes = new Vector<Float>();
		allTimes = new Vector<Float>();

		this.server = hostname;
		this.port = port;
		this.ccsHandler = ccsHandler;

		progressHandler h = new progressHandler();
		ccs = new CcsThread(h,server,port);

		createPlotInFrameJFreeChart();

		/** Create first request */
		ccs.addRequest(new dataRequest());

	}

	/** Generate the plot that will now be displayed */
	public void updatePlot(){

		if(REDO_DISPLAY_EACH_TIME){

			XYSeries newDataSeries = new XYSeries("Utilization For Each Sample", true, false);
			int numInputPerDataPoint = 20;
			int numPoints = 500;


			int start = 0;	
			int end = allTimes.size();
			if(allTimes.size() > numPoints*numInputPerDataPoint){
				start = allTimes.size() - numPoints*numInputPerDataPoint;
			}

			// average the points to reduce the plotted data.
			for(int i=start; i+numInputPerDataPoint-1<end; i+=numInputPerDataPoint){
				int count=0;
				double sum = 0.0;
				for(int j=0;j<numInputPerDataPoint;j++){
					sum += allTimes.elementAt(i+j);
					count++;
				}
				newDataSeries.add(i,sum/(double)count);
			}


			// Create a dataset
			dataset.removeAllSeries();
			dataset.addSeries(newDataSeries);

			plot.setDataset(dataset);
		}
	}


	/** Create a window with a simple plot in it. Uses the publicly available jfreechart package. */
	public void createPlotInFrameJFreeChart(){
		dataset = new DefaultTableXYDataset();

		if(! REDO_DISPLAY_EACH_TIME){
			// create data
			dataSeries = new XYSeries("Utilization For Each Sample", true, false);
			dataSeries.setMaximumItemCount(500); 
			dataset.addSeries(dataSeries);
		}

		// Create axis labels
		NumberAxis domainAxis = new NumberAxis("Samples");
		domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());       
		domainAxis.setAutoRange(true);
		domainAxis.setAutoRangeIncludesZero(false);
		NumberAxis rangeAxis = new NumberAxis("Utilization %");



		// Create renderer
		//		StackedXYBarRenderer renderer = new StackedXYBarRenderer();
		//		renderer.setDrawBarOutline(true);

		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesLinesVisible(0, true); 
		renderer.setSeriesShapesVisible(0,false);

		// Create the plot, using the renderer and the dataset and the axis
		plot = new XYPlot(dataset, domainAxis, rangeAxis, renderer);

		// Create a chart using the plot
		JFreeChart chart = new JFreeChart("Utilization over time", plot);

		// Put the chart in a JPanel that we can use inside our program's GUI
		ChartPanel chartpanel = new ChartPanel(chart);

		// Put the chartpanel in a new window(JFrame)
		JFrame window = new JFrame("Streaming Utilization Plot");
		window.setLayout(new BorderLayout());
		window.add(chartpanel, BorderLayout.CENTER);


		// Display the window	
		window.pack();
		window.setVisible(true);
	}



}
