package projections.Tools.MemoryUsage;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.analysis.TimedProgressThreadExecutor;
import projections.gui.IntervalChooserPanel;
import projections.gui.MainWindow;
import projections.gui.OrderedIntList;
import projections.gui.ProjectionsWindow;
import projections.gui.RangeDialog;
import projections.gui.U;
import projections.misc.LogEntryData;





// FIXME: This tool does not correctly handle the cases where the start time != 0. There are places where startInterval is not correctly added to / subtracted from a value.


public class MemoryUsageWindow extends ProjectionsWindow {

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private int myRun = 0;

	/** For each PE keep a plottable data structure */
	private TreeMap<Long,XYSeries> memorySamples; 
	
	private MemoryUsageWindow thisWindow;
	private MainWindow mainWindow;
	private JMenuBar mbar;
	private JMenuItem mShowPhaseInfo;
	private JMenuItem mViewDataAsText;
	private JMenuItem mViewPhaseInfoAsText;
	
	private Vector<String> availableStepStrings;
	private Vector<Long> availableStepTimes;

	private IntervalChooserPanel intervalPanel;
	
	private double timeScalingFactor = 1.0;
	private String timeUnits = "us-default";

	// Saved for further analysis and exporting of the data:
	private long intervalSize;
	private long startInterval;
	private long endInterval;
	/** For each PE keep data for further analysis */
	private TreeMap<Long, double[]> memoryData;

	
	
	public MemoryUsageWindow(MainWindow mainWindow)
	{
		super(mainWindow);
		thisWindow = this;
		this.mainWindow = mainWindow;

		memorySamples = new TreeMap<Long,XYSeries>(); 

		setForeground(Color.lightGray);
		setTitle("Memory Usage - " + MainWindow.runObject[myRun].getFilename() + ".sts");

		createMenus();
		pack();
		showDialog();
	}

	
	private class FrameWithText {
		FrameWithText(String text, String title){
						
			JFrame f = new JFrame();

			JTextArea textArea = new JTextArea(text);
			textArea.setFont(new Font("Arial", Font.PLAIN, 16));
			textArea.setLineWrap(true);
			textArea.setWrapStyleWord(true);

			JScrollPane areaScrollPane = new JScrollPane(textArea);
			areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			areaScrollPane.setPreferredSize(new Dimension(900, 600));

			f.getContentPane().setLayout(new BorderLayout());
			f.getContentPane().add(areaScrollPane, BorderLayout.CENTER);
			f.setTitle(title);
			f.pack();
			f.setVisible(true);
			
		}
	}
	

	private class ViewPhaseInfoAsTextDisplay implements ActionListener{
		ViewPhaseInfoAsTextDisplay(){ }

		public void actionPerformed(ActionEvent e) {
			String info = "Copy & paste into your favorite spreadsheet or plotting tool:\n\nTime\tMaximum memory Usage Across All PEs:\n";

			for(int i=0;i<availableStepTimes.size(); i++){
				info += "" + projections.gui.U.humanReadableString(availableStepTimes.elementAt(i)) + "\t" + availableStepStrings.elementAt(i) + "\n";
			}
			System.out.println("Phase Info:\n" + info + "\n");

			new FrameWithText(info, "Phase Information:");
			
		}
	}


	
	
	private class ViewMaxMemAsTextDisplay implements ActionListener{
		ViewMaxMemAsTextDisplay(){ }

		public void actionPerformed(ActionEvent e) {
			
			int numIntervals = (int) (endInterval - startInterval);
			double[] memMax = new double[numIntervals];
			Iterator<Long> pes = memoryData.keySet().iterator();
			while(pes.hasNext()){
				Long pe = pes.next();
				double[] data = memoryData.get(pe);
				for(int i=0; i<data.length; i++){
					if(memMax[i] < data[i]){
						memMax[i] = data[i];
					}
				}
			}

			
			String info = "Copy & paste into your favorite spreadsheet or plotting tool:\n\nTime\tMaximum memory Usage Across All PEs:\n";

			for(int i=0; i<numIntervals; i++){
				double time = (i + startInterval) * intervalSize * timeScalingFactor;
				if(memMax[i] > 0){
					info += "" + time + "\t" + memMax[i] + "\n";
				}
			}
			
			new FrameWithText(info, "Maximum Memory Usage:");

		}

	}


	
	
	private class PhaseInformationDisplay implements ActionListener{
		PhaseInformationDisplay(){ }

		public void actionPerformed(ActionEvent e) {
			String info = "";
			for(int i=0;i<availableStepTimes.size(); i++){
				info += "" + projections.gui.U.humanReadableString(availableStepTimes.elementAt(i)) + "\t" + availableStepStrings.elementAt(i) + "\n";
			}
			System.out.println("Phase Info:\n" + info + "\n");

			new FrameWithText(info, "Phase Information:");

		}

	}

	private void createMenus()
	{
		mbar = new JMenuBar();

		JMenu m1 = new JMenu("Phase Information");
		mShowPhaseInfo = new JMenuItem("Display Phase Information");
		m1.add(mShowPhaseInfo);
		mShowPhaseInfo.addActionListener(new PhaseInformationDisplay());

		JMenu m2 = new JMenu("Export Data");
		
		mViewDataAsText = new JMenuItem("Max Memory Usage");
		m2.add(mViewDataAsText);
		mViewDataAsText.addActionListener(new ViewMaxMemAsTextDisplay());

		mViewPhaseInfoAsText = new JMenuItem("Phase Information");
		m2.add(mViewPhaseInfoAsText);
		mViewPhaseInfoAsText.addActionListener(new ViewPhaseInfoAsTextDisplay());
		
		
		mbar.add(m1);
		mbar.add(m2);
		
		setJMenuBar(mbar);
	} 

	public void showDialog()
	{
		try {
			if (dialog == null) {
				intervalPanel = new IntervalChooserPanel();
				dialog = new RangeDialog(this, "Select Range", intervalPanel, false);
			}
			dialog.displayDialog();
			if (!dialog.isCancelled()) {
				final OrderedIntList pes = dialog.getSelectedProcessors();
				intervalSize = intervalPanel.getIntervalSize();
				startInterval = intervalPanel.getStartInterval();
				endInterval = intervalPanel.getEndInterval();

				final SwingWorker worker = new SwingWorker() {
					public Object doInBackground() {
						// Load memory usages here
						thisWindow.loadData(pes);
						return null;
					}

					public void done() {
						thisWindow.createPlot();
					}
				};
				worker.execute();
			}
		} catch (Exception e) { 
			e.printStackTrace();
		}
	}



	private void createPlot() {
		setVisible(false);

		Iterator<Long> pes = memorySamples.keySet().iterator();
		XYSeriesCollection seriesCollection = new XYSeriesCollection();
		int totalSampleCount = 0;
		while(pes.hasNext()){
			Long pe = pes.next();
			XYSeries series = memorySamples.get(pe);
			totalSampleCount += series.getItemCount();
			seriesCollection.addSeries(series);
		}

		if(totalSampleCount == 0){
			JOptionPane.showMessageDialog(this, "No memory usage entries found in log files. Add calls to traceMemoryUsage() in the application", "Warning", JOptionPane.WARNING_MESSAGE);
			Container windowPane = thisWindow.getContentPane();
			windowPane.removeAll();
			windowPane.setLayout(new BorderLayout());
			windowPane.add(new JLabel("No memory log entries found!"), BorderLayout.CENTER);
			setJMenuBar(mbar);
			setSize(400,400);
			validate();
			repaint();
			setVisible(true);
			return;
		}

		JFreeChart chart = ChartFactory.createScatterPlot(
				"Memory Usage (at " + U.humanReadableString(intervalSize) + " resolution)",
				"Time (" + timeUnits + ")",
				"MB",
				seriesCollection,
				PlotOrientation.VERTICAL,
				true,
				true,
				false
		) ;


		XYDotRenderer renderer = new XYDotRenderer();
		renderer.setDotWidth(2);
		renderer.setDotHeight(2);
		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setDomainGridlinesVisible(false);  
		plot.setRangeGridlinesVisible(true);  
		plot.setRenderer(renderer);

		

		// Add markers to mark each iteration:
		determineStepsFromPEZero();
		for(int i=0; i<availableStepTimes.size(); i++){
			ValueMarker m = new ValueMarker(availableStepTimes.elementAt(i) * timeScalingFactor, Color.black, new BasicStroke(1.0f) );
			//			m.setLabel(this.availableStepStrings.elementAt(i));
			plot.addDomainMarker(m);
		}

		// Put the chart in a JPanel that we can use inside our program's GUI
		ChartPanel chartpanel = new ChartPanel(chart);
		chart.setBackgroundPaint(Color.white);

		chartpanel.setPreferredSize(new Dimension(1100,700));

		Container windowPane = thisWindow.getContentPane();
		windowPane.removeAll();
		windowPane.setLayout(new BorderLayout());
		windowPane.add(chartpanel, BorderLayout.CENTER);
		thisWindow.setJMenuBar(mbar);

		thisWindow.pack();
		thisWindow.setVisible(true);
	}


	private void loadData(final OrderedIntList processorList) {
		
		// Determine how to scale the x axis values&units
		double timeSpan = (endInterval - startInterval) * intervalSize;
		if(timeSpan > 1000000*5){
			// units should be seconds
			timeScalingFactor = 0.000001;
			timeUnits = "s";	
		} else if (timeSpan > 1000 * 5){
			// units should be ms
			timeScalingFactor = 0.001;
			timeUnits = "ms";
		} else {
			// units should be us
			timeScalingFactor = 1.0;
			timeUnits = "us";
		}
		
		
		if( MainWindow.runObject[myRun].hasLogFiles() || MainWindow.runObject[myRun].hasSumDetailFiles() ) {
			// Do parallel loading because we have full logs

			// Create a list of worker threads
			LinkedList<Runnable> readyReaders = new LinkedList<Runnable>();

			while (processorList.hasMoreElements()) {
				int nextPe = processorList.nextElement();
				readyReaders.add( new ThreadedFileReader(nextPe, myRun, intervalSize, startInterval, endInterval, timeScalingFactor));
			}

			// Determine a component to show the progress bar with
			Component guiRootForProgressBar = null;
			if(thisWindow!=null && thisWindow.isVisible()) {
				guiRootForProgressBar = thisWindow;
			} else if(mainWindow!=null && mainWindow.isVisible()){
				guiRootForProgressBar = mainWindow;
			} else if(MainWindow.runObject[myRun].guiRoot!=null && MainWindow.runObject[myRun].guiRoot.isVisible()){
				guiRootForProgressBar = MainWindow.runObject[myRun].guiRoot;
			}

			// Pass this list of threads to a class that manages/runs the threads nicely
			TimedProgressThreadExecutor threadManager = new TimedProgressThreadExecutor("Loading Time Profile in Parallel", readyReaders, guiRootForProgressBar, true);
			threadManager.runAll();

			memorySamples = new TreeMap(); 
			memoryData = new TreeMap();
			
			Iterator<Runnable> titer = readyReaders.iterator();
			while(titer.hasNext()){
				ThreadedFileReader r = (ThreadedFileReader) titer.next();
				memorySamples.put(r.getPe(), r.getMemorySamples());
				memoryData.put(r.getPe(), r.getData());
			}


		}

	}


	private Vector<Long> determineStepsFromPEZero() {
		// Labels containing the user notes found in the log
		availableStepStrings = new Vector<String>();
		availableStepTimes = new Vector<Long>();

		if (!(MainWindow.runObject[myRun].hasLogData())){
			return availableStepTimes;
		}

		int pe = 0;
		GenericLogReader reader = new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());

		try{
			int c = 0;
			while (true) {
				LogEntryData data = reader.nextEvent();

				if(data.type == ProjDefs.USER_SUPPLIED_NOTE){
					if(data.note.contains("***")){
						String pruned = data.note.replace("*** ", "");
						availableStepStrings.add("" + (c++) + ": " + pruned);
						availableStepTimes.add(data.time);
					}
				}
			}

		} catch (EndOfLogSuccess e) {			
			// Successfully read log file
		} catch (Exception e) {
			System.err.println("Error occured while reading data for pe " + pe);
		}
		
		
		try {
			reader.close();
		} catch (IOException e1) {
			System.err.println("Error: could not close log file reader for processor " + pe );
		}
		return availableStepTimes;

	}




}
