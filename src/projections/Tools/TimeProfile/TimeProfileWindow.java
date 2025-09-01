package projections.Tools.TimeProfile;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;

import projections.analysis.LogReader;
import projections.analysis.ProjMain;
import projections.analysis.TimedProgressThreadExecutor;
import projections.gui.Clickable;
import projections.gui.GenericGraphWindow;
import projections.gui.IntervalChooserPanel;
import projections.gui.JPanelToImage;
import projections.gui.MainWindow;
import projections.gui.RangeDialog;
import projections.gui.TimeProfileColorer;
import projections.gui.U;
import projections.gui.Util;

/**
 *  TimeProfileWindow
 *  by Chee Wai Lee
 *	updated by Isaac Dooley
 * 
 */
public class TimeProfileWindow extends GenericGraphWindow
implements ActionListener, Clickable
{

	private TimeProfileWindow thisWindow;
	private MainWindow mainWindow;

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;

//	private EntrySelectionDialog entryDialog;

	private JPanel mainPanel;
	private JPanel controlPanel;
//	private JButton epSelection;
	private JButton setRanges;

	private IntervalChooserPanel intervalPanel;

	private JMenuItem mDisplayLegend;
	private JMenuItem mDisplayLegendFull;

	private JCheckBox showMarkersCheckBox;
	private JCheckBox analyzeSlopesCheckBox;
	private JCheckBox hideMouseoversCheckBox;

	private long intervalSize;
	private int startInterval;
	private int endInterval;

	private long startTime;

	private boolean displaySlopes = false;

	// Markers that are drawn at certain times (doubles in units of x axis bins) to identify phases or iterations
	private TreeMap<Double, String> phaseMarkers = new TreeMap<Double, String>();

	
	// data used for intervalgraphdialog
	private SortedSet<Integer> processorList;

	// data required for entry selection dialog
	private int numEPs;
	//YSun add
//	private String typeLabelNames[] = {"Entry Points"};
	private boolean stateArray[];
	private boolean existsArray[];
	private String entryNames[];

	// stored raw data
	private double[][] graphData;

	// output arrays
	private double[][] outputData;

	// flag signifying callgraph has just begun
	private boolean	   startFlag;

	// the following data are statically known and can be initialized
	// here overhead, idle time
	private final static int special = 2;

	public TimeProfileWindow(MainWindow mainWindow) {
		super("Projections Time Profile Graph - " + MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);

		this.mainWindow = mainWindow;

		numEPs = MainWindow.runObject[myRun].getNumUserEntries();
		stateArray = new boolean[numEPs+special];
		existsArray = new boolean[numEPs+special];
		entryNames = new String[numEPs+special];
		for (int ep=0; ep<numEPs; ep++) {
			entryNames[ep] = MainWindow.runObject[myRun].getEntryNameByIndex(ep);
		}
		//YSun idle time
		//for(int ep = numEPs; ep<numEPs+special; ep++)

		entryNames[numEPs] = "Overhead";
		entryNames[numEPs+1] = "Idle";


		mainPanel = new JPanel();
		getContentPane().add(mainPanel);

		createMenus();
		createLayout();
		pack();
		thisWindow = this;
		startFlag = true;
		thisWindow.setLocationRelativeTo(null);
		showDialog();
	}

	protected void createMenus(){
		super.createMenus();

		JMenu legendMenu = new JMenu("Legend");
		mDisplayLegend = new JMenuItem("Display Legend");
		mDisplayLegend.addActionListener(this);
		legendMenu.add(mDisplayLegend);
		mDisplayLegendFull = new JMenuItem("Display Legend with Full EP Names");
		mDisplayLegendFull.addActionListener(this);
		legendMenu.add(mDisplayLegendFull);


		menuBar.add(legendMenu);

	}

	private void createLayout() {
		GridBagConstraints gbc = new GridBagConstraints();
		GridBagLayout gbl = new GridBagLayout();

		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.setLayout(gbl);

		// control panel items
//		epSelection = new JButton("Select Entry Points");
//		epSelection.addActionListener(this);
		setRanges = new JButton("Select New Range");
		setRanges.addActionListener(this);

		showMarkersCheckBox =  new JCheckBox("Show Iteration/Phase Markers");
		showMarkersCheckBox.setSelected(false);
		showMarkersCheckBox.setToolTipText("Draw vertical lines at time associated with any user supplied notes containing\"***\"?");
		showMarkersCheckBox.addActionListener(this);
		
		analyzeSlopesCheckBox = new JCheckBox("Analyze slope");
		analyzeSlopesCheckBox.setToolTipText("Select a point on the graph to measure the slope");
		analyzeSlopesCheckBox.addActionListener(this);

		hideMouseoversCheckBox = new JCheckBox("Hide Mouseovers");
		hideMouseoversCheckBox.setSelected(false);
		hideMouseoversCheckBox.setToolTipText("Disable the displaying of information associated with the data under the mouse pointer.");
		hideMouseoversCheckBox.addActionListener(this);

		controlPanel = new JPanel();
		controlPanel.setLayout(gbl);
//		Util.gblAdd(controlPanel, epSelection,    gbc, 0,0, 1,1, 0,0);
		Util.gblAdd(controlPanel, setRanges,      gbc, 0,0, 1,1, 0,0);
		Util.gblAdd(controlPanel, showMarkersCheckBox, gbc, 3,0, 1,1, 0,0);
		Util.gblAdd(controlPanel, analyzeSlopesCheckBox, gbc, 4,0, 1,1, 0,0);
		Util.gblAdd(controlPanel, hideMouseoversCheckBox, gbc, 5,0, 1,1, 0,0);

		JPanel graphPanel = getMainPanel();
		Util.gblAdd(mainPanel, graphPanel, gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(mainPanel, controlPanel, gbc, 0,1, 1,0, 0,0);
	}

	public void setGraphSpecificData() {

	}


	private static class SortableEPs implements Comparable{
		private double value;
		private String name;
		private Paint paint;

		private SortableEPs(double value, String name, Paint paint){
			this.value = value;
			this.name = name;
			this.paint = paint;
		}

		public int compareTo(Object o) {
			SortableEPs other = (SortableEPs) o;
			if(other.value < value)
				return -1;
			else if(other.value > value)
				return 1;
			else 
				return other.name.compareTo(name);
		}

	}

	private void generateLegend(boolean useShortenedNames){

		List<SortableEPs> l = new ArrayList<SortableEPs>();

		// Accumulate data shown in graph
		double[] sums = new double[numEPs+2];
		double grandTotal = 0.0;
		for(int i=0; i<graphData.length; i++){
			for(int ep=0; ep<graphData[i].length; ep++){
				sums[ep] += graphData[i][ep];
				grandTotal += graphData[i][ep];
			}
		}


		// Put data into list	
		for (int ep=0; ep<numEPs; ep++) {
			if(useShortenedNames)
				l.add(new SortableEPs(sums[ep], MainWindow.runObject[myRun].getPrettyEntryNameByIndex(ep), MainWindow.runObject[myRun].getEPColorMap()[ep]));
			else
				l.add(new SortableEPs(sums[ep], MainWindow.runObject[myRun].getEntryNameByIndex(ep), MainWindow.runObject[myRun].getEPColorMap()[ep]));

		}

		l.add(new SortableEPs(sums[numEPs], "Overhead", MainWindow.runObject[myRun].getOverheadColor()));
		l.add(new SortableEPs(sums[numEPs+1], "Idle", MainWindow.runObject[myRun].getIdleColor()));


		// sort list 
		java.util.Collections.sort(l);


		// Extract data from list into structures for legend
		List<String>  names = new ArrayList<String>();
		List<Paint>  paints = new ArrayList<Paint>();

		Iterator<SortableEPs> iter = l.iterator();
		while(iter.hasNext()){
			SortableEPs s = iter.next();
			if(s.value > grandTotal * 0.005){
				names.add(s.name);
				paints.add(s.paint);
			}
		}


		// Display the legend
		new Legend("Legend", names, paints);

	}

	public void showDialog() {
		if (dialog == null) {
			if (MainWindow.runObject[myRun].hasLogFiles())
				intervalPanel = new IntervalChooserPanel();
			else // Intervals are fixed for the summary modes, so don't display choosable interval panel
				intervalPanel = null;
			dialog = new RangeDialog(this, "Select Range", intervalPanel, false);
		}

		dialog.displayDialog();
		if (!dialog.isCancelled()){
            processorList = dialog.getSelectedProcessors();
            startTime = dialog.getStartTime();

			if (MainWindow.runObject[myRun].hasLogFiles()) {
				intervalSize = intervalPanel.getIntervalSize();
				startInterval = (int)intervalPanel.getStartInterval();
				endInterval = (int)intervalPanel.getEndInterval();

			} else { // sum detail mode
				intervalSize = (long) MainWindow.runObject[myRun].getSumDetailIntervalSize();
				startInterval = (int) (startTime / intervalSize);
				final long endTime = dialog.getEndTime();
				// For intervalSize of 1, endTime of 2 should give endInterval of 1 ([1,2)), endTime of 2.5 should give
				// endInterval of 2 ([2, 3)), so take ceil and subtract one
				endInterval = (int) Math.ceil(((double)endTime) / intervalSize) - 1;
			}

            System.out.println("Props: intervalSize:"+intervalSize+"- startInterval:"+startInterval+"- endInterval:"+endInterval+"- startTime:" + startTime);

			final SwingWorker worker =  new SwingWorker() {
				public Object doInBackground() {
					phaseMarkers.clear();
					
					int numIntervals = endInterval-startInterval+1; 
					graphData = new double[numIntervals][numEPs+special]; //entry number + idle

					int numProcessors = processorList.size();
					int numUserEntries = MainWindow.runObject[myRun].getNumUserEntries();

					if(MainWindow.runObject[myRun].hasLogFiles()) { //Bilge
						// Do parallel loading because we have full logs

						// Create a list of worker threads
						LinkedList<Runnable> readyReaders = new LinkedList<Runnable>();

						// Create multiple result arrays to reduce contention for accumulating
						int numResultAccumulators = 8;
						double[][][] graphDataAccumulators = new double[numResultAccumulators][numIntervals][numEPs+special];


						int pIdx=0;		
						for(Integer pe : processorList){
							readyReaders.add( new ThreadedFileReader(pe, intervalSize, myRun, 
									startInterval, endInterval, phaseMarkers, 
									graphDataAccumulators[pIdx%numResultAccumulators]) );
							pIdx++;
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


						// Merge resulting graphData structures together.
						for(int a=0; a< numResultAccumulators; a++){
							for(int i=0;i<numIntervals;i++){
								for(int j=0;j<numEPs+special;j++){
									graphData[i][j] += graphDataAccumulators[a][i][j];
								}
							}	
						}


					}
                    else if( MainWindow.runObject[myRun].hasSumDetailFiles()) //Bilge
                    {
					    // Do serial file reading because all we have is the sum files	    	
                        //System.out.println("hasSumDetailFiles - LOAD DATA. numIntervals: " + numIntervals);
                        MainWindow.runObject[myRun].LoadGraphData(intervalSize, startInterval, endInterval, false,
                                processorList);

                        int[][] sumDetailData = MainWindow.runObject[myRun].getSumDetailData_interval_EP();

                        for(int i=0;i<numIntervals;i++){
						    //for(int j=0;j<numEPs+special;j++){
                            for(int j=0;j<numEPs;j++){
                                graphData[i][j] += sumDetailData[i][j];
                                    //if(sumDetailData[i][j] != 0)
                                        //System.out.println("X: " + sumDetailData[i][j]);
                                //graphData[i][j] = 1;
                            }
						}
                        //Use sum files to get the idle data, sumDetail log files do not contain idle data
                        int[][] idleTemp = MainWindow.runObject[myRun].sumAnalyzer.getSystemUsageData(startInterval, endInterval, intervalSize);

                        // Determine a component to show the progress bar with
						Component guiRootForProgressBar = null;
						if(thisWindow!=null && thisWindow.isVisible()) {
							guiRootForProgressBar = thisWindow;
						} else if(mainWindow!=null && mainWindow.isVisible()){
							guiRootForProgressBar = mainWindow;
						} else if(MainWindow.runObject[myRun].guiRoot!=null && MainWindow.runObject[myRun].guiRoot.isVisible()){
							guiRootForProgressBar = MainWindow.runObject[myRun].guiRoot;
						}

                    }
					else if( MainWindow.runObject[myRun].hasSumFiles()){
						// Do serial file reading because all we have is the sum files	    	

						// The data we use is
						// 	systemUsageData[2][*][*]
						// userEntryData[*][LogReader.TIME][*][*]

						int[][][]  systemUsageData = new int[3][][];
						systemUsageData[2] = new int[numProcessors][];

						//			    	int[][][][] systemMsgsData = new int[5][3][numProcessors][];
						//						int[][][][] systemMsgsData = null;

						int[][][][] userEntryData  = new int[numUserEntries][][][];
						for(int n=0;n<numUserEntries;n++){
							userEntryData[n]  = new int[3][][];
							userEntryData[n][LogReader.TIME] = new int[numProcessors][];
						}

						int[][] temp = MainWindow.runObject[myRun].sumAnalyzer.getSystemUsageData(startInterval, endInterval, intervalSize);
						systemUsageData[1] = new int[processorList.size()][endInterval-startInterval+1];
						
						int pIdx=0;
						for(Integer pe : processorList) {
							systemUsageData[1][pIdx] = temp[pe];
							pIdx++;
						} 


						// Extract data and put it into the graph
						for(int peIdx=0; peIdx< numProcessors; peIdx++){
							for (int ep=0; ep<numEPs; ep++) {
								int[][] entryData = userEntryData[ep][LogReader.TIME];
								for (int interval=0; interval<numIntervals; interval++) {
									graphData[interval][ep] += entryData[peIdx][interval];
									graphData[interval][numEPs] -= entryData[peIdx][interval]; // overhead = -work time
								}
							}

							//YS add for idle time SYS_IDLE=2
							int[][] idleData = systemUsageData[2]; //percent
							for (int interval=0; interval<numIntervals; interval++) {
								if(idleData[peIdx] != null && idleData[peIdx].length>interval){
									graphData[interval][numEPs+1] += idleData[peIdx][interval] * 0.01 * intervalSize;
									graphData[interval][numEPs] -= idleData[peIdx][interval] * 0.01 * intervalSize; //overhead = - idle time
									graphData[interval][numEPs] += intervalSize;  
								}
							}
						}					
					}//end of summary

					// Scale raw data into percents
					for (int interval=0; interval<graphData.length; interval++) {
						for(int e=0; e< graphData[interval].length; e++){
							graphData[interval][e] = graphData[interval][e] * 100.0 / ((double)intervalSize * (double)numProcessors);
						}
					}
                    //Bilge
                    if( MainWindow.runObject[myRun].hasSumDetailFiles()){
                        //idle time calculation for sum detail
                        double[] idlePercentage = MainWindow.runObject[myRun].sumAnalyzer.getTotalIdlePercentagePerInterval();
                        for(int i=0;i<numIntervals;i++){
                            graphData[i][numEPs+1] = idlePercentage[i];
                        }
                        //overhead time calculation for sum detail
                        for(int i=0;i<numIntervals;i++){
                            graphData[i][numEPs] = 100;
                            for(int j=0;j<numEPs;j++){
                                graphData[i][numEPs] -= graphData[i][j];
                            }
                            graphData[i][numEPs] -= graphData[i][numEPs+1];
                        }

                    }

					// Filter Out any bad data
					for (int interval=0; interval<graphData.length; interval++) {
						boolean valid = true;
						double sumForInterval = 0.0;
						for(int e=0; e< graphData[interval].length; e++){
							sumForInterval += graphData[interval][e];
							if(graphData[interval][e] < 0.0){
								valid = false;
							}
						}
						if(sumForInterval > 105.0){
							valid = false;
						}

						if(!valid){
							System.err.println("Time Profile found bad data for interval " + interval + ". The data for bad intervals will be zero-ed out. This problem is either a log file corruption issue, or a bug in Projections.");
							for(int e=0; e< graphData[interval].length; e++){
								graphData[interval][e] = 0.0;
							}
						}

					}
					// set the exists array to accept non-zero
					// entries only have initial state also 
					// display all existing data. Only do this 
					// once in the beginning
					if (startFlag) {
						for (int ep=0; ep<numEPs+special; ep++) {
							for (int interval=0; 
							interval<endInterval-startInterval+1;
							interval++) {
								if (graphData[interval][ep] > 0) {
									existsArray[ep] = true;
									stateArray[ep] = true;
									break;
								}
							}
						}
					}
					if (startFlag)
						startFlag = false;

					return null;
				}
				public void done() {
					setOutputGraphData();
					thisWindow.setVisible(true);                        
				}
			};
			worker.execute();
		}
	}

	private void setOutputGraphData() {
		// need first pass to decide the size of the outputdata
		int outSize = 0;
		for (int ep=0; ep<numEPs+special; ep++) {
			if (stateArray[ep]) {
				outSize++;
			}
		}
		if (outSize > 0) {
			// actually create and fill the data and color array
			int numIntervals = endInterval-startInterval+1;
			outputData = new double[numIntervals][outSize];
			for (int i=0; i<numIntervals; i++) {
				int count = 0;
				for (int ep=0; ep<numEPs+special; ep++) {
					if (stateArray[ep]) {
						outputData[i][count] = graphData[i][ep];
					}
				}

			}

			setYAxis("Percentage Utilization", "%");
			String xAxisLabel = "Time (" + U.humanReadableString(intervalSize) + " resolution)";
			setXAxis(xAxisLabel, "Time", startTime, intervalSize);
			setDataSource("Time Profile", outputData, new TimeProfileColorer(outSize, numIntervals, numEPs, outputData, graphData, stateArray), thisWindow);
			graphCanvas.setMarkers(phaseMarkers);
			refreshGraph();
		}
	}

	public String[] getPopup(int xVal, int yVal) {
		if ((xVal < 0) || (yVal < 0)) {
			return null;
		}

		/******************* YSun change here */
		// find the ep corresponding to the yVal
		int count = 0;
		String epName = "";
		String epClassName = "";
		for (int ep=0; ep<numEPs; ep++) {
			if (stateArray[ep]) {
				if (count++ == yVal) {
					epName = MainWindow.runObject[myRun].getEntryNameByIndex(ep);
					epClassName = MainWindow.runObject[myRun].getEntryChareNameByIndex(ep);
					break;
				}
			}
		}
		String[] rString = new String[4];

		rString[0] = "Time Interval: " + 
		U.humanReadableString((xVal+startInterval)*intervalSize) + " to " +
		U.humanReadableString((xVal+startInterval+1)*intervalSize);
		rString[1] = "Chare Name: " + epClassName;
		rString[2] = "Entry Method: " + epName;
		rString[3] = "Execution Time = " + U.humanReadableString((long)(outputData[xVal][yVal]));
		//deal with idle and overhead time
		if(yVal == outputData[xVal].length -2)
		{
			rString[1] = "";
			rString[2] = "Overhead";
			rString[3] = "Time = " + U.humanReadableString((long)(outputData[xVal][yVal]));
		}else if(yVal == outputData[xVal].length -1)
		{
			rString[1] = "";
			rString[2] = "Idle time";
			rString[3] = "Time = " + U.humanReadableString((long)(outputData[xVal][yVal]));
		}
		return rString;
	}	

	public void actionPerformed(ActionEvent e) {
//		if (e.getSource() == epSelection) {
//			if (entryDialog == null) {
//				entryDialog = 
//					new EntrySelectionDialog(this,
//							typeLabelNames,
//							stateArray,colorArray,
//							existsArray,entryNames);
//			}
//			entryDialog.showDialog();
//		} else 
			
			
		if (e.getSource() == analyzeSlopesCheckBox) {
			if(analyzeSlopesCheckBox.isSelected()){
				displaySlopes = true;
				graphCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));	
			} else {
				displaySlopes = false;
				graphCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));	
				graphCanvas.clearPolynomial();
			}
		} else if (e.getSource() == showMarkersCheckBox){
			graphCanvas.showMarkers(showMarkersCheckBox.isSelected());
		} else if (e.getSource() == hideMouseoversCheckBox) {
			graphCanvas.showBubble(! hideMouseoversCheckBox.isSelected());
		} else if (e.getSource() == setRanges) {
			showDialog();
		} else if(e.getSource() == mDisplayLegend){
			generateLegend(true);
		} else if(e.getSource() == mDisplayLegendFull){
			generateLegend(false);
		} else if (e.getSource() instanceof JMenuItem) {
			String arg = ((JMenuItem)e.getSource()).getText();
			if (arg.equals("Close")) {
				close();
			} else if(arg.equals("Set Range")) {
				showDialog();
			}
		}
	}




	private void createPolynomial(int xVal, int yVal){
		int numIntervals = endInterval-startInterval+1; 

		// We approximate the derivatives by using values from xVal-2 to xVal+2
		if(xVal < 2 || yVal < 0 || xVal >= numIntervals-2){
			return;
		}

		// extract the curve that sits:
		// above the EP utilization + overhead 
		// but below the idle time
		double[] nonIdle = new double[numIntervals];
		for(int i=0; i<numIntervals; i++){
			nonIdle[i] = 0.0;
			for(int ep=0; ep<numEPs+1; ep++){
				nonIdle[i] += graphData[i][ep];
			}
		}

		// Sum up values to find total for the bar where the user clicked
		double total = 0.0;
		for(int ep=0; ep<graphData[xVal].length; ep++){
			total += graphData[xVal][ep];
		}			

		// Lookup the y value on this curve for where the user clicked
		double y = nonIdle[xVal];

		double slopeA = (nonIdle[xVal+1] - nonIdle[xVal-1])/2.0;
		double slopeB = (nonIdle[xVal+2] - nonIdle[xVal-2])/4.0;
		double slopeC = (slopeA+slopeB)/2.0;

		// And to get some y=mx+b style coefficients, we need to do a little math:
		double[] coefficients = new double[2];
		coefficients[0] = -1.0*slopeC*xVal+y; // y intercept of line
		coefficients[1] = slopeC;

		graphCanvas.addPolynomial(coefficients);

	}


	public void toolMouseMovedResponse(MouseEvent e, int xVal, int yVal) {
		if(displaySlopes){
			createPolynomial(xVal, yVal);
		}
	}	

	public void toolClickResponse(MouseEvent e, int xVal, int yVal) {

		if(displaySlopes){
			// create a screenshot of the 
			JPanelToImage.saveToFileChooserSelection(graphCanvas, "Save Screenshot Image", "./TimeProfileScreenshot.png");
		}

	}

}
