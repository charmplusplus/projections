package projections.Tools.Extrema;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.KMeansClustering;
import projections.analysis.ProjDefs;
import projections.analysis.ProjMain;
import projections.analysis.TimedProgressThreadExecutor;
import projections.gui.Clickable;
import projections.gui.GenericGraphColorer;
import projections.gui.GenericGraphWindow;
import projections.gui.MainWindow;
import projections.gui.RangeDialog;
import projections.gui.U;
import projections.gui.Util;
import projections.misc.LogEntry;

/**
 *  OutlierAnalysisWindow
 *  by Chee Wai Lee
 *  8/23/2006
 *
 */
public class ExtremaWindow extends GenericGraphWindow
implements ActionListener, ItemListener,
Clickable
{

	private ExtremaWindow thisWindow;

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;

	// private dialog data
	private int threshold;
	private int k;

	// Record which activity was chosen and is currently loaded
	private int selectedActivity;
	private int selectedAttribute;


	// control panel gui objects and support variables
	// **CW** Not so good for now, used by both Dialog and Window
	private String attributes[][] = {
			{	"Least Idle Time",
				"Most Idle Time",
			 	"Overhead",
				"Active Entry Methods",
				"Average Grain Size",
				"Extrema by Clustering",
				"Msgs Sent by Activity <not yet implemented>", 
				"Bytes Sent by Activity  <not yet implemented>"
			},
			{ 	"Utilization Percentage",
				"Utilization Percentage",
				"Time (us)",
				" ",
				"Time (us)",
				"Execution Time (us)",
				"Number of Messages",
				"Number of Bytes"
			},
			{ 	"us",
				"us",
				"us",
				"",
				"us",
				"us",
				"",
				""
			}
	};

	protected final static int ATTR_LEASTIDLE = 0;
	protected final static int ATTR_MOSTIDLE = 1;
	private final static int ATTR_OVERHEAD = 2;
	private final static int ATTR_ACTIVEENTRY = 3;
	private final static int ATTR_GRAINSIZE = 4;
	private final static int ATTR_CLUSTERING = 5;
	protected final static int ATTR_MSGSSENT = 6;
	protected final static int ATTR_BYTESSENT = 7;

		
	// derived data after analysis
	private LinkedList outlierList;

	// meta data variables
	// These will be determined at load time.
	private int numActivities;
	private int numSpecials;
	private double[][] graphData;
	private LinkedList<Integer> outlierPEs;


	public ExtremaWindow(MainWindow mainWindow) {
		super("Projections Extrema Analysis Tool - " + 
				MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);

		createMenus();
		createLayout();
		pack();
		thisWindow = this;
		outlierPEs = new LinkedList<Integer>();

		// special behavior if initially used (to load the raw 
		// online-generated outlier information). Quick and dirty, use
		// static variables ... not possible if multiple runs are supported.
		if (MainWindow.runObject[myRun].rcReader.RC_OUTLIER_FILTERED) {
			// Now, read the generated outlier stats, rankings and the top
			// [threshold] log files
			loadOnlineData(0, MainWindow.runObject[myRun].getTotalTime());
		} else {
			showDialog();
		}
	}

	private JButton bAddToTimelineJButton;
	private JTextField bNumOfPEsToAdd;

	private void createLayout() {
		bAddToTimelineJButton =  new JButton("Add Top Extrema PEs to Timeline: ");
		bAddToTimelineJButton.setToolTipText("The Timeline Tool must already be open!");
		bAddToTimelineJButton.addActionListener(new buttonHandler());
		
		bNumOfPEsToAdd =  new JTextField("5", 5);

		GridBagConstraints gbc = new GridBagConstraints();
		GridBagLayout gbl = new GridBagLayout();
		gbc.fill = GridBagConstraints.BOTH;
		getContentPane().setLayout(gbl);
		Util.gblAdd(getContentPane(), getMainPanel(),    gbc, 0,0, 2,1	, 1,1);
		Util.gblAdd(getContentPane(), bAddToTimelineJButton,      gbc, 0,1, 1,1, 0,0);
		Util.gblAdd(getContentPane(), bNumOfPEsToAdd,      gbc, 1,1, 1,1, 0,0);
	}


	private class buttonHandler implements ActionListener {
		public buttonHandler(){

		}

		public void actionPerformed(ActionEvent e) {
			int count = 0;
			if(e.getSource() == bAddToTimelineJButton){
				// load each outlier PE into the Timeline Window
				ListIterator<Integer> iter2 = outlierPEs.listIterator(outlierPEs.size());
				while(iter2.hasPrevious() && count < Integer.parseInt(bNumOfPEsToAdd.getText())){
					count++;
					int pe = iter2.previous();
					parentWindow.addProcessor(pe);
				}
			}
		}

	}


//
//	protected void createMenus(){
//		super.createMenus();
//	}

	private ExtremaDialogExtension outlierDialogPanel;


	public void showDialog() {
		if (dialog == null) {
			outlierDialogPanel = new ExtremaDialogExtension(attributes[0]);
			dialog = new RangeDialog(this, "Select Time Range", outlierDialogPanel, false);
		}
		dialog.displayDialog();
		if (!dialog.isCancelled()){
			SortedSet<Integer> selectedPEs = new TreeSet<Integer>(dialog.getSelectedProcessors());
			threshold = selectedPEs.size() < outlierDialogPanel.getThreshold() ? selectedPEs.size() : outlierDialogPanel.getThreshold();
			selectedActivity = outlierDialogPanel.getCurrentActivity();
			selectedAttribute = outlierDialogPanel.getCurrentAttribute();
			
			if (outlierDialogPanel.getK()>selectedPEs.size()) {
				k=selectedPEs.size();
				JOptionPane.showMessageDialog(this,
						"The number of clusters cannot be greater than the number \n" +
						"of processors.  Number of clusters has been set to number \n" +
						"of processors.", "Warning", 
						JOptionPane.WARNING_MESSAGE);
			}
			else
				k = outlierDialogPanel.getK();
			thisWindow.setVisible(false);

			final SwingWorker worker =  new SwingWorker() {
				public Object doInBackground() {
					constructToolData(dialog.getStartTime(), dialog.getEndTime());
					return null;
				}
				public void done() {
					// GUI code after Long non-gui code (above) is done.
					//outlierList may be null if threads are interrupted by cancel button
					if (outlierList != null) {
						setGraphSpecificData();
						thisWindow.setVisible(true);
					}
				}
			};
			worker.execute();


		}
	}

	
	/** A class that provides the colors for the display */
	public class NormalColorer implements GenericGraphColorer {
		int myRun = 0;
		int numActivities;
		int numSpecials;

		public NormalColorer(int numActivities, int numSpecials) {
			this.numActivities = numActivities;
			this.numSpecials = numSpecials;
		}

		public Paint[] getColorMap() {
			Paint[]  outColors = new Paint[numActivities+numSpecials];

			for (int i=0;i<numActivities; i++) {
				outColors[i] = MainWindow.runObject[myRun].getColorMap(selectedActivity)[i];
			}
			outColors[numActivities] = MainWindow.runObject[myRun].getOverheadColor();
			outColors[numActivities+1] = MainWindow.runObject[myRun].getIdleColor();
			
			return outColors;
		}
	}

	
	/** A class that provides the colors for the display */
	public class OnlineDataColorer implements GenericGraphColorer {
		int myRun = 0;
		int numActivities;

		public OnlineDataColorer(int numActivities) {
			this.numActivities = numActivities;
		}

		public Paint[] getColorMap() {
			int numSpecials = 1;
			Paint[]  outColors = new Paint[numActivities+numSpecials];
			for (int i=0;i<numActivities; i++) {
				outColors[i] = MainWindow.runObject[myRun].getColorMap(selectedActivity)[i];
			}
			outColors[numActivities] = MainWindow.runObject[myRun].getIdleColor();
			
			return outColors;
		}
	}

	
	private GenericGraphColorer colorer;

	private void constructToolData(final  long startTime, final long endTime ) {
		// construct the necessary meta-data given the selected activity
		// type.
		double[][] tempData;
		numActivities = MainWindow.runObject[myRun].getNumActivity(selectedActivity); 

		// Idle and overhead are always added to the chart.
		numSpecials = 2;
		colorer = new NormalColorer(numActivities,numSpecials);
		
		int numActivityPlusSpecial = numActivities+numSpecials;

		SortedSet<Integer> selectedPEs = new TreeSet<Integer>(dialog.getSelectedProcessors());
		int numPEs = selectedPEs.size();
		tempData = new double[numPEs][];

		if(MainWindow.runObject[myRun].hasLogData()){

			// Create a list of worker threads
			LinkedList<Runnable> readyReaders = new LinkedList<Runnable>();

			int pIdx=0;		
			
			for(Integer pe : selectedPEs){
				readyReaders.add( new ThreadedFileReader(pe, startTime, endTime, 
						numActivities, numActivityPlusSpecial, selectedActivity, selectedAttribute) );
				pIdx++;
			}


			// Determine a component to show the progress bar with
			Component guiRootForProgressBar = null;
			if(thisWindow!=null && thisWindow.isVisible()) {
				guiRootForProgressBar = thisWindow;
			} else if(MainWindow.runObject[myRun].guiRoot!=null && MainWindow.runObject[myRun].guiRoot.isVisible()){
				guiRootForProgressBar = MainWindow.runObject[myRun].guiRoot;
			}

			// Pass this list of threads to a class that manages/runs the threads nicely
			TimedProgressThreadExecutor threadManager = new TimedProgressThreadExecutor("Loading Extrema in Parallel", readyReaders, guiRootForProgressBar, true);
			threadManager.runAll();


			// Retrieve results from each thread, storing them into tempData
			int pIdx2=0;
			Iterator<Runnable> iter = readyReaders.iterator();
			while (iter.hasNext()) {
				ThreadedFileReader r = (ThreadedFileReader) iter.next();
				tempData[pIdx2] = r.myData;
				pIdx2++;
			}
		}
		else if(MainWindow.runObject[myRun].hasSumDetailData()){
			SortedSet<Integer> availablePEs = MainWindow.runObject[myRun].getValidProcessorList(ProjMain.SUMDETAIL);
			int intervalSize =(int) MainWindow.runObject[myRun].getSumDetailIntervalSize();
			int startInterval=(int)startTime/intervalSize;
			int endInterval=(int)endTime/intervalSize;
			
			MainWindow.runObject[myRun].LoadGraphData(intervalSize, startInterval, endInterval, false,availablePEs);

			double[][] sumDetailDataperproc = MainWindow.runObject[myRun].getSumDetailDataperproc();

	// Use sum files to get the idle data, sumDetail log files do not contain idle
	// data

			double scale = 100.0 / (1 * ((int) endTime - (int) startTime));
			double[] idleTemp = MainWindow.runObject[myRun].sumAnalyzer.getTotalIdlePercentageperproc(startInterval,endInterval);


			for(int i =0;i <sumDetailDataperproc.length;i++){
				double  lis[] =new double[sumDetailDataperproc[i].length+2]; 
				double sum=0.0;
				for(int j =0;j<sumDetailDataperproc[i].length;j++){
					lis[j]=sumDetailDataperproc[i][j]*scale;
					sum+=lis[j];
				}


				lis[sumDetailDataperproc[i].length] = idleTemp[i];
				lis[sumDetailDataperproc[i].length+1] = 100.0-sum-lis[sumDetailDataperproc[i].length];
				tempData[i]=lis;	
			}
		}

		// Compute Extrema elements depending on attribute type.
		// 
		// The final graph has 3 extra x-axis slots for 
		// 0) global average
		// 1) non-outlier average
		// 2) outlier average

		double[] tmpAvg = new double[numActivities+numSpecials];
//		double[] processorDiffs = new double[selectedPEs.size()];
		String[] peNames = new String[selectedPEs.size()];
//		double [] grainSize = new double[selectedPEs.size()];
		
		int[] sortedMap = new int[threshold];

		// **CWL** hack to deal with the difference between the way
		//   the reader reads data and the way we use the data.
		for (int p=0; p<tempData.length; p++) {
		    // swap the IDLE and OVERHEAD positions
		    double temp;
		    temp = tempData[p][numActivities+1];
		    tempData[p][numActivities+1] = tempData[p][numActivities];
		    tempData[p][numActivities] = temp;
		}

		// initialize processor names
		int pcount = 0;
		for(Integer pe : selectedPEs) {
		    peNames[pcount] = Integer.toString(pe);
		    pcount++;
		}

		// ********* Generate graph data **********

		// pass #1, determine global average
		for (int act=0; act<numActivities+numSpecials; act++) {
			for (int p=0; p<selectedPEs.size(); p++) {
				tmpAvg[act] += tempData[p][act];
			}
			tmpAvg[act] /= selectedPEs.size();
		}

		// pass #2, determine extrema (some attributes rely on
		//   tmpAvg calculated in pass #1).
		// Output = sortedMap
		computeExtremaMap(selectedAttribute,
				  tempData, tmpAvg,
				  numActivities, k, sortedMap);

		// take sortedMap to create the final array
		// and copy the data in.
		graphData = new double[threshold+3][numActivities+numSpecials];
		outlierList = new LinkedList();
		for (int ii=0; ii<threshold; ii++) {
		    int p = sortedMap[ii];		
		    for (int act=0; act<numActivities+numSpecials; act++) {
			graphData[ii+3][act] = tempData[p][act];
		    }
		    // add to outlier list reverse sorted by significance
		    String name = peNames[p];
		    outlierList.add(name);
		    Integer ival = Integer.parseInt(name);
		    outlierPEs.add(ival);
		}

		// fill global average bar
		graphData[0] = tmpAvg;
		// fill extrema average bar (but do not compute average yet)
		for (int act=0; act<numActivities+numSpecials; act++) {
		    for (int i=0; i<threshold; i++) {
			graphData[2][act] += tempData[sortedMap[i]][act];
		    }
		}
		// fill non-extrema average bar
		for (int act=0; act<numActivities+numSpecials; act++) {
		    graphData[1][act] += graphData[0][act]*selectedPEs.size() -
			graphData[2][act];
		    if (graphData[1][act] < 0) {
			graphData[1][act] = 0.0;
		    }
		}
		// now compute the average for extrema and non-extrema
		int offset = selectedPEs.size() - threshold;
		for (int act=0; act<numActivities+numSpecials; act++) {
		    if (offset != 0) {
			graphData[1][act] /= offset;
		    }
		    if (threshold != 0) {
			graphData[2][act] /= threshold;
		    }
		}

		// add the 3 special entries
		outlierList.addFirst("Out.");
		outlierList.addFirst("Non.");
		outlierList.addFirst("Avg");

	}

    // Output: sortedMap[] where sortedMap.length = threshold.
    private void computeExtremaMap(int selectedAttribute,
				   double data[][], 
				   double tmpAvg[],
				   int numActivities, int k,
				   int sortedMap[]) {
	int numPEs = data.length;
	int threshold = numPEs < sortedMap.length ? numPEs : sortedMap.length;
	int offset = numPEs - threshold;

	switch (selectedAttribute) {
	case ATTR_CLUSTERING: {
	    int clusterMap[] = new int[numPEs];  
	    
	    if(numPEs < 3){
	    	System.err.println("Not enough processors selected to perform clustering\n");	    	
	    	sortedMap = null;
	    	return;
	    }
	    
	    /* **CWL** This modification is used so the data agrees with
	       the results generated for my thesis. In this case, the
	       overhead value is ignored. Eventually, it is expected that
	       either Idle or Overhead will be eliminated as a result of
	       metric reduction due to correlation.

	    double modifiedData[][] = new double[numPEs][numActivities+1];
	    for (int p=0; p<numPEs; p++) {
		for (int act=0; act<numActivities; act++) {
		    modifiedData[p][act] = data[p][act];
		}
		modifiedData[p][numActivities] = data[p][numActivities+1];
	    }
	    */

	    double distanceFromClusterMean[] = new double[numPEs];
	    KMeansClustering.kMeans(data, k, clusterMap, 
				    distanceFromClusterMean);
	    selectRepresentatives(clusterMap, distanceFromClusterMean,
				  sortedMap);
	    break;
	}
	case ATTR_LEASTIDLE: {
	    double processorDiffs[] = new double[numPEs];
	    for (int p=0; p<numPEs; p++) {
		// induce a sort by decreasing idle time
		processorDiffs[p] = -data[p][numActivities+1];
	    }
	    int fullMap[] = new int[numPEs];
	    for (int i=0; i<numPEs; i++) {
		fullMap[i] = i;
	    }
	    bubbleSort(processorDiffs, fullMap);
	    // trim to threshold
	    for (int i=0; i<threshold; i++) {
		sortedMap[i] = fullMap[i+offset];
	    }
	    break;
	}
	case ATTR_MOSTIDLE: {
	    double processorDiffs[] = new double[numPEs];
	    for (int p=0; p<numPEs; p++) {
		// induce a sort by increasing idle time
		processorDiffs[p] = data[p][numActivities+1];
	    }
	    int fullMap[] = new int[numPEs];
	    for (int i=0; i<numPEs; i++) {
		fullMap[i] = i;
	    }
	    bubbleSort(processorDiffs, fullMap);
	    // trim to threshold
	    for (int i=0; i<threshold; i++) {
		sortedMap[i] = fullMap[i+offset];
	    }
	    break;
	}
	case ATTR_ACTIVEENTRY: {
	    double processorDiffs[] = new double[numPEs];
	    for (int p=0; p<numPEs; p++) {
		// active entry method
		for (int iact = 0; iact<numActivities; iact++) {
		    if (data[p][iact] > 0)
			processorDiffs[p]++;			
		}
	    }
	    int fullMap[] = new int[numPEs];
	    for (int i=0; i<numPEs; i++) {
		fullMap[i] = i;
	    }
	    bubbleSort(processorDiffs, fullMap);
	    // trim to threshold
	    for (int i=0; i<threshold; i++) {
		sortedMap[i] = fullMap[i+offset];
	    }
	    break;
	}
	case ATTR_OVERHEAD: {
	    double processorDiffs[] = new double[numPEs];
	    for (int p=0; p<numPEs; p++) {
		//black time totaltime - entrytime-idle time
		processorDiffs[p] = data[p][numActivities];
	    }
	    int fullMap[] = new int[numPEs];
	    for (int i=0; i<numPEs; i++) {
		fullMap[i] = i;
	    }
	    bubbleSort(processorDiffs, fullMap);
	    // trim to threshold
	    for (int i=0; i<threshold; i++) {
		sortedMap[i] = fullMap[i+offset];
	    }
	    break;
	}
	case ATTR_GRAINSIZE: {
	    double grainSize[] = new double[numPEs];
	    for (int p=0; p<numPEs; p++) {
		int __count_entries = 0;
		for (int iact = 0; iact<numActivities; iact++) {
		    if (data[p][iact] > 0)
			__count_entries++;
		    grainSize[p] += data[p][iact];
		}
		if (__count_entries>0)
		    grainSize[p] /= __count_entries;			
	    }
	    int fullMap[] = new int[numPEs];
	    for (int i=0; i<numPEs; i++) {
		fullMap[i] = i;
	    }
	    bubbleSort(grainSize, fullMap);
	    // trim to threshold
	    for (int i=0; i<threshold; i++) {
		sortedMap[i] = fullMap[i+offset];
	    }
	    break;
	}
	case ATTR_MSGSSENT: // fall thru
	case ATTR_BYTESSENT: {
	    double processorDiffs[] = new double[numPEs];
	    for (int p=0; p<numPEs; p++) {
		for (int act=0; act<numActivities; act++) {
		    processorDiffs[p] += 
			Math.abs(data[p][act] - tmpAvg[act]) * tmpAvg[act];
		}
	    }
	    int fullMap[] = new int[numPEs];
	    for (int i=0; i<numPEs; i++) {
		fullMap[i] = i;
	    }
	    bubbleSort(processorDiffs, fullMap);
	    // trim to threshold
	    for (int i=0; i<threshold; i++) {
		sortedMap[i] = fullMap[i+offset];
	    }
	    break;
	}
	}
    }

    // select the representatives given clusters discovered by the
    //     the kMeans algorithm. The "sorted" map is output as
    //     sortedMap.
    private void selectRepresentatives(int clusterMap[],
				       double distanceFromClusterMean[],
				       int sortedMap[]) {
	int clusterCounts[] = new int[this.k];
	int numNonZero = 0;
	int numElements = clusterMap.length;
	int threshold = sortedMap.length;

	for (int p=0; p<numElements; p++) {
	    clusterCounts[clusterMap[p]]++;
	}
	for (int k=0; k<this.k; k++) {
	    if (clusterCounts[k] > 0) {
		numNonZero++;
	    }
	}

	/*
	for (int p=0; p<numElements; p++) {
	    System.out.println("["+p+"] " + distanceFromClusterMean[p]);
	}
	*/

	// Distributing choices
	int numLeft = threshold; // book-keeping variable
//	int numReps = 0;
	int numOutliers = 0;
	// handle de-generate choices
	if (threshold > numNonZero) {
//	    numReps = numNonZero;
	    numOutliers = threshold - numNonZero; 
	} else {
//	    numReps = threshold;
	    numOutliers = 0;
	}
	int clusterRepCounts[] = new int[this.k];

	// Each non-empty cluster gets a representative (except for
	//   de-generate user choices)
	for (int k=0; k<this.k; k++) {
	    if ((numLeft > 0) && (clusterCounts[k] > 0)) {
		clusterRepCounts[k]++;
		numLeft--;
	    }
	}

	double clusterOutlierFractions[] = new double[this.k];
	int clusterOutlierCounts[] = new int[this.k];
	// Split up the outliers amongst the non empty clusters by proportion
	//   (approximately)
	for (int k=0; k<this.k; k++) {
	    if (clusterCounts[k] > 0) {
		clusterOutlierFractions[k] += 
		    numOutliers*(clusterCounts[k]/
				 (double)(numElements - numNonZero));
		// pick out the whole numbers
		clusterOutlierCounts[k] +=
		    (int)Math.floor(clusterOutlierFractions[k]);
		numLeft -= clusterOutlierCounts[k];
	    }
	}
	// Sanity Check
	if (numLeft > numNonZero) {
	    System.err.println("Error in cluster count division! " +
			       "Number left = " + numLeft + " with " +
			       numNonZero + " non-empty clusters.");
	    System.exit(-1);
	}
	// Distribute the leftovers to non-empty clusters on a 
	//    first-come-first-serve basis (not the best)
	for (int k=0; k<this.k; k++) {
	    if ((numLeft > 0) && (clusterCounts[k] > 0)) {
		clusterOutlierCounts[k]++;
		numLeft--;
	    }
	}
	
	// Now (bubble) sort the distances
	double distances[] = new double[numElements];
	int distancePeMap[] = new int[numElements];
	for (int p=0; p<numElements; p++) {
	    distances[p] = distanceFromClusterMean[p];
	    distancePeMap[p] = p;
	}
	bubbleSort(distances, distancePeMap);

	// Pick out the representatives from the sorted distances
	int sortedMapIdx = 0;
	for (int p=0; p<numElements; p++) {
	    int k = clusterMap[distancePeMap[p]];
	    if (clusterRepCounts[k] > 0) {
		sortedMap[sortedMapIdx++] = distancePeMap[p];
		clusterRepCounts[k]--;
	    }
	}

	// Pick out the outliers from the sorted distances
	sortedMapIdx = threshold-1;
	for (int p=numElements-1; p>=0; p--) {
	    int k = clusterMap[distancePeMap[p]];
	    if (clusterOutlierCounts[k] > 0) {
		sortedMap[sortedMapIdx--] = distancePeMap[p];
		clusterOutlierCounts[k]--;
	    }
	}

	/*
	for (int i=0; i<threshold; i++) {
	    System.out.println(sortedMap[i]);
	}
	*/
    }

    // data remains unchanged. map is modified.
    // Both data and map must have been initialized prior to invocation.
    private void bubbleSort(double data[], int map[]) {
	int numElements = data.length;
	if (numElements != map.length) {
	    System.err.println("Error: Extrema Tool - attempt to sort " +
			       "incompatible data. Please contact devs.");
	    System.exit(-1);
	}

	// make a copy of the original data
	double tmpData[] = new double[numElements];
	for (int i=0; i<numElements; i++) {
	    tmpData[i] = data[i];
	}

	for (int elt=numElements-1; elt>0; elt--) {
	    for (int i=0; i<elt; i++) {
		if (tmpData[i+1] < tmpData[i]) {
		    double temp = tmpData[i+1];
		    tmpData[i+1] = tmpData[i];
		    tmpData[i] = temp;
		    int tempI = map[i+1];
		    map[i+1] = map[i];
		    map[i] = tempI;
		}
	    }
	}
    }

	private void loadOnlineData(final long startTime, final long endTime) {
		final SwingWorker worker = new SwingWorker() {
			public Object doInBackground() {
				readOutlierStats(startTime, endTime);
				return null;
			}
			public void done() {
				setGraphSpecificData();
				thisWindow.setVisible(true);
			}
		};
		worker.execute();
	}

	// This method will read the stats file generated during online
	// outlier analysis which will then determine which processor's
	// log data to read.
	private void readOutlierStats(final long startTime, final long endTime) {
		numActivities = MainWindow.runObject[myRun].getNumActivity(selectedActivity); 

		colorer = new OnlineDataColorer(numActivities);
		
		graphData = new double[threshold+3][numActivities+numSpecials];

		// Read the stats file for global average data.
		String statsFilePath =
			MainWindow.runObject[myRun].getLogDirectory() + File.separator + 
			MainWindow.runObject[myRun].getOutlierFilename();
		try {
			BufferedReader InFile =
				new BufferedReader(new InputStreamReader(new FileInputStream(statsFilePath)));	
			String statsLine;
			statsLine = InFile.readLine();
			StringTokenizer st = new StringTokenizer(statsLine);
			for (int i=0; i<numActivities+numSpecials; i++) {
				graphData[0][i] = Double.parseDouble(st.nextToken());
			}

			// Now read the ranked list of processors and then taking the
			// top [threshold] number.
			statsLine = InFile.readLine();
			st = new StringTokenizer(statsLine);
			int offset = 0;
			SortedSet<Integer> peList = MainWindow.runObject[myRun].getValidProcessorList(ProjMain.LOG);
			if (peList.size() > threshold) {
				offset = peList.size() - threshold;
			}
			int nextPe = 0;
			ProgressMonitor progressBar =
				new ProgressMonitor(MainWindow.runObject[myRun].guiRoot, 
						"Reading log files",
						"", 0,
						threshold);
			progressBar.setNote("Reading");
			progressBar.setProgress(0);
			// clear offset values from the list on file
			for (int i=0; i<offset; i++) {
				st.nextToken();
			}
			outlierList = new LinkedList();
			// add the 3 special entries
			outlierList.add("Avg");    
			outlierList.add("Non.");
			outlierList.add("Out.");
			for (int i=0; i<threshold; i++) {
				nextPe = Integer.parseInt(st.nextToken());
				outlierList.add(nextPe + "");
				progressBar.setProgress(i);
				progressBar.setNote("[PE: " + nextPe +
						" ] Reading Data. (" + i + " of " +
						threshold + ")");
				if (progressBar.isCanceled()) {
					return;
				}
				readOnlineOutlierProcessor(nextPe,i+3, startTime, endTime);
			}
			progressBar.close();
		} catch (IOException e) {
			System.err.println("Error: Projections failed to read " +
					"outlier data file [" + statsFilePath +
			"].");
			System.err.println(e.toString());
			System.exit(-1);
		}	    
		// Calculate the outlier average. Non-outlier average will be
		// derived from the recorded global average and the outlier average.
		for (int act=0; act<numActivities+numSpecials; act++) {
			for (int i=0; i<threshold; i++) {
				graphData[2][act] += graphData[i+3][act];
			}
			// derive total contributed by non-outliers
			graphData[1][act] = 
				graphData[0][act]*MainWindow.runObject[myRun].getNumProcessors() -
				graphData[2][act];
			graphData[1][act] /= MainWindow.runObject[myRun].getNumProcessors() - threshold;
			graphData[2][act] /= threshold;
		}
	}

	private void readOnlineOutlierProcessor(int pe, int index, final long startTime, final long endTime) {
		GenericLogReader reader = 
			new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());
		try {
			LogEntry logData = new LogEntry();
			logData.time = 0;
			// Jump to the first valid event
			boolean markedBegin = false;
			boolean markedIdle = false;
			long beginBlockTime = 0;
			logData = reader.nextEventOnOrAfter(startTime);
			while (logData.time <= endTime) {
				if (logData.type == ProjDefs.BEGIN_PROCESSING) {
					// check pairing
					if (!markedBegin) {
						markedBegin = true;
					}
					beginBlockTime = logData.time;
				} else if (logData.type == ProjDefs.END_PROCESSING) {
					// check pairing
					// if End without a begin, just ignore
					// this event.
					if (markedBegin) {
						markedBegin = false;
						graphData[index][logData.entry] +=
							logData.time - beginBlockTime;
					}
				} else if (logData.type == ProjDefs.BEGIN_IDLE) {
					// check pairing
					if (!markedIdle) {
						markedIdle = true;
					}
					// NOTE: This code assumes that IDLEs cannot
					// possibly be nested inside of PROCESSING
					// blocks (which should be true).
					beginBlockTime = logData.time;
				} else if (logData.type ==
					ProjDefs.END_IDLE) {
					// check pairing
					if (markedIdle) {
						markedIdle = false;
						graphData[index][numActivities] +=
							logData.time - beginBlockTime;
					}
				}
				logData = reader.nextEvent();
			}
			reader.close();
		} catch (EndOfLogSuccess e) {
			// Reached end of the log file successfully
		} catch (IOException e) {
			System.err.println("Outlier Analysis: Error in reading log data for processor " + pe);
			System.err.println(e);
		}
		

		try {
			reader.close();
		} catch (IOException e1) {
			System.err.println("Error: could not close log file reader for processor " + pe );
		}
		
		
	}

	protected void setGraphSpecificData() {
		setXAxis("Notable PEs (Cluster Representatives and Extrema)", outlierList);
		setYAxis(attributes[1][selectedAttribute], 
				attributes[2][selectedAttribute]);
		setDataSource("Extrema: " + attributes[0][selectedAttribute] +
				" (" + threshold + 
				" Extrema PEs)", graphData, colorer, this);
		refreshGraph();
	}


	public String[] getPopup(int xVal, int yVal) {
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(3);
		if ((xVal < 0) || (yVal < 0)) {
			return null;
		}
		String[] rString = new String[3];
		if (xVal == 0) {
			rString[0] = "Global Average";
		} else if (xVal == 1) {
			rString[0] = "Non Outlier Average";
		} else if (xVal == 2) {
			rString[0] = "Outlier Average";
		} else {
			rString[0] = "Outlier Processor " +  (String)outlierList.get(xVal);
		}
		if (yVal == numActivities) {
			rString[1] = "Overhead";
		} else if (yVal == numActivities+1){
			rString[1] = "Idle";
		} else {
			rString[1] = "Activity: " + 
			MainWindow.runObject[myRun].getActivityNameByIndex(selectedActivity, yVal);
		}
		if (selectedActivity >= 2) {
			rString[2] = df.format(graphData[xVal][yVal]) + "";
		} else {
			rString[2] = U.humanReadableString((long)(graphData[xVal][yVal]));
		}
		return rString;
	}

	public void toolClickResponse(MouseEvent e, int xVal, int yVal) {
		/** only try to load bars that represent PEs */
		if(xVal > 2)
			parentWindow.addProcessor(Integer.parseInt((String)outlierList.get(xVal)));
	}


	public void toolMouseMovedResponse(MouseEvent e, int xVal, int yVal) {
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
			String arg = ((JMenuItem)e.getSource()).getText();
			if (arg.equals("Close")) {
				close();
			} else if(arg.equals("Set Range")) {
				showDialog();
			}
		}
	}

	public void itemStateChanged(ItemEvent e) {
		// do nothing.
	}



}
