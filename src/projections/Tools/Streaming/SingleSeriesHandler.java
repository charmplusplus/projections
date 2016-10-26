package projections.Tools.Streaming;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

import projections.ccs.CcsProgress;
import projections.ccs.CcsThread;

class SingleSeriesHandler {

	private CcsThread ccs;

	private List<Float> averageTimes;
	private List<Float> allTimes;

	private XYPlot plot;

	private String server;
	private String ccsHandler;

	private static boolean REDO_DISPLAY_EACH_TIME = false;

	private XYSeries dataSeries;
	private DefaultTableXYDataset dataset;
	
	

	private class progressHandler implements CcsProgress {
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
			System.out.println("SingleSeriesHandler Received " + data.length + " byte data array\n");
			int previousEntries = allTimes.size();

			if(ccsHandler.equals("CkPerfSummaryCcsClientCB")){
//
//				numData = data.length / 8;
//				for(int i=0; i<numData; i++){
//					double v = ByteParser.bytesToDouble(data, 8*i);
//					sum += v;
//					if(v >= 0.0){
//						allTimes.add(new Float(v));
//					}
//				}

			} else if(ccsHandler.equals("CkPerfSummaryCcsClientCB uchar")){

				numData = data.length / 1;
				for(int i=0; i<numData; i++){
					Float v = new Float(ByteParser.unsignedByteToInt(data[i]));

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
						s += allTimes.get(j+i);
					}
					double avg = s / numInputPerDataPoint;
					dataSeries.add(i,avg);

				}

			}

			float avg = (float) (sum / numData);

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

		averageTimes = new ArrayList<Float>();
		allTimes = new ArrayList<Float>();

		this.server = hostname;
		this.ccsHandler = ccsHandler;

		progressHandler h = new progressHandler();
		ccs = new CcsThread(h,server,port);

		createPlotInFrameJFreeChart();

		/** Create first request */
		ccs.addRequest(new dataRequest());

	}

	/** Generate the plot that will now be displayed */
	private void updatePlot(){

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
					sum += allTimes.get(i+j);
					count++;
				}
				newDataSeries.add(i,sum/count);
			}


			// Create a dataset
			dataset.removeAllSeries();
			dataset.addSeries(newDataSeries);

			plot.setDataset(dataset);
		}
	}


	/** Create a window with a simple plot in it. Uses the publicly available jfreechart package. */
	private void createPlotInFrameJFreeChart(){
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
