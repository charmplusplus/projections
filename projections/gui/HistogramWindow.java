package projections.gui;
import projections.misc.LogEntryData;
import projections.analysis.*;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 *  HistogramWindow
 *  modified by Chee Wai Lee
 *  2/23/2005
 */
public class HistogramWindow extends GenericGraphWindow 
implements ActionListener
{

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;

	public static final int NUM_TYPES = 2;
	public static final int TYPE_TIME = 0;
	public static final int TYPE_MSG_SIZE = 1;

	// Gui components
	JButton entrySelectionButton;
	JButton epTableButton;

	JRadioButton timeBinButton;
	JRadioButton msgSizeBinButton;
	ButtonGroup binTypeGroup;

	
	BinDialogPanel binpanel;
	
	// Data maintained by HistogramWindow
	// countData is indexed by type, then by bin index followed by ep id.
	// NOTE: bin indices need not be of the same size
	double[][][] counts;
	int binType;

	public int timeNumBins;
	public long timeBinSize;
	public long timeMinBinSize;
	public int msgNumBins;
	public long msgBinSize;
	public long msgMinBinSize;

	HistogramWindow thisWindow;

	private DecimalFormat _format;


	public HistogramWindow(MainWindow mainWindow)
	{
		super("Projections Histograms", mainWindow);
		thisWindow = this;

		binType = TYPE_TIME;
		_format = new DecimalFormat();

		setTitle("Projections Histograms - " + MainWindow.runObject[myRun].getFilename() + ".sts");

		createMenus();
		getContentPane().add(getMainPanel());

		pack();
		showDialog(); 
	}   

	public void close(){
		super.close();
	}
	


	/* 
	 *  Show the BinDialog 
	 */
	public void showDialog()
	{
		if (dialog == null) {
			binpanel = new BinDialogPanel();
			dialog = new RangeDialog(this, "Select Histogram Time Range", binpanel, false);
		}
		
		dialog.displayDialog();
		if (!dialog.isCancelled()) {
			final SwingWorker worker = new SwingWorker() {
				public Object doInBackground() {
					timeNumBins = binpanel.getTimeNumBins();
					timeBinSize = binpanel.getTimeBinSize();
					timeMinBinSize = binpanel.getTimeMinBinSize();
					msgNumBins = binpanel.getMsgNumBins();
					msgBinSize = binpanel.getMsgBinSize();
					msgMinBinSize = binpanel.getMsgMinBinSize();
					binType = binpanel.getSelectedType();
					counts = thisWindow.getCounts(dialog.getStartTime(), dialog.getEndTime(), dialog.getSelectedProcessors());
					return null;
				}
				protected void done() {
					// Make the gui status reflect what was chosen in the dialog box
					if(binType == TYPE_MSG_SIZE)
						msgSizeBinButton.setSelected(true);
					else
						timeBinButton.setSelected(true);			
					setGraphSpecificData();
					refreshGraph();
					thisWindow.setVisible(true);
				}
			};
			worker.execute();
		}
	}




	public void actionPerformed(ActionEvent evt)
	{
		if (evt.getSource() instanceof JMenuItem) {
			JMenuItem m = (JMenuItem)evt.getSource();
			if(m.getText().equals("Set Range"))
				showDialog();
			else if(m.getText().equals("Close"))
				close();
		} else if (evt.getSource()  == timeBinButton) {
			binType = TYPE_TIME;
			setGraphSpecificData();
			refreshGraph();
		} else if (evt.getSource()  ==  msgSizeBinButton) {
			binType = TYPE_MSG_SIZE;
			setGraphSpecificData();
			refreshGraph();
		} else if (evt.getSource() == entrySelectionButton) {
			System.out.println("selecting entries for display");
		} else if (evt.getSource() == epTableButton) {
			System.out.println("Showing out of range entries");
		}
	}
	

	protected JPanel getMainPanel()
	{
		JPanel mainPanel = new JPanel();

		GridBagConstraints gbc = new GridBagConstraints();
		GridBagLayout gbl = new GridBagLayout();

		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.setLayout(gbl);

		JPanel graphPanel = super.getMainPanel(); 

		JPanel buttonPanel = new JPanel();
		buttonPanel.setBorder(new TitledBorder(new LineBorder(Color.black), 
				"Histogram Controls"));

		entrySelectionButton = new JButton("Select Entries");
		entrySelectionButton.addActionListener(this);
		epTableButton = new JButton("Out-of-Range EPs");
		epTableButton.addActionListener(this);

		timeBinButton = new JRadioButton("Execution Time", true);
		timeBinButton.addActionListener(this);
		msgSizeBinButton = new JRadioButton("Message Size");
		msgSizeBinButton.addActionListener(this);

		binTypeGroup = new ButtonGroup();
		binTypeGroup.add(timeBinButton);
		binTypeGroup.add(msgSizeBinButton);

		buttonPanel.add(timeBinButton);
		buttonPanel.add(msgSizeBinButton);
//		buttonPanel.add(entrySelectionButton);
//		buttonPanel.add(epTableButton);

		Util.gblAdd(mainPanel, graphPanel,  gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(mainPanel, buttonPanel, gbc, 0,1, 1,1, 0,0);

		return mainPanel;
	}  

	protected void setGraphSpecificData(){
		if (binType == TYPE_TIME) {
			setXAxis("Bin Interval Size (" + U.t(timeBinSize) + ")", "Time", timeMinBinSize, timeBinSize);
			setYAxis("Number of Occurrences", "");
			setDataSource("Histogram", counts[TYPE_TIME], thisWindow);
		} else if (binType == TYPE_MSG_SIZE) {
			setXAxis("Bin Interval Size (" +  _format.format(msgBinSize) + " bytes)",  "", msgMinBinSize, msgBinSize);
			setYAxis("Number of Occurrences", "");
			setDataSource("Histogram", counts[TYPE_MSG_SIZE], thisWindow);
		}
	}

	protected void refreshGraph()
	{
		super.refreshGraph();
	}

	public String[] getPopup(int xVal, int yVal) {
		if (binType == TYPE_TIME) {
			return getTimePopup(xVal, yVal);
		} else if (binType == TYPE_MSG_SIZE) {
			return getMsgSizePopup(xVal, yVal);
		}
		return null;
	}

	private String[] getTimePopup(int xVal, int yVal) {
		String bubbleText[] = new String[3];

		bubbleText[0] = MainWindow.runObject[myRun].getEntryNameByIndex(yVal);
		bubbleText[1] = "Count: " + counts[TYPE_TIME][xVal][yVal];
		if (xVal < timeNumBins) {
			bubbleText[2] = "Bin: " + U.t(xVal*timeBinSize+timeMinBinSize) +
			" to " + U.t((xVal+1)*timeBinSize+timeMinBinSize);
		} else {
			bubbleText[2] = "Bin: > " + U.t(timeNumBins*timeBinSize+
					timeMinBinSize);
		}
		return bubbleText;
	}

	private String[] getMsgSizePopup(int xVal, int yVal) {
		String bubbleText[] = new String[3];

		bubbleText[0] = MainWindow.runObject[myRun].getEntryNameByIndex(yVal);
		bubbleText[1] = "Count: " + counts[TYPE_MSG_SIZE][xVal][yVal];
		if (xVal < msgNumBins) {
			bubbleText[2] = "Bin: " + 
			_format.format(xVal*msgBinSize+msgMinBinSize) +
			" bytes to " + _format.format((xVal+1)*msgBinSize+
					msgMinBinSize) +
					" bytes";
		} else {
			bubbleText[2] = "Bin: > " + 
			_format.format(msgNumBins*msgBinSize+msgMinBinSize)+" bytes";
		}
		return bubbleText;
	}

	double[][][] getCounts(long startTime, long endTime, OrderedIntList pes)
	{
		// Variables for use with the analysis
		long executionTime;
		long adjustedTime;
		long adjustedSize;

		int numEPs = MainWindow.runObject[myRun].getNumUserEntries();

		GenericLogReader r;
		double[][][] countData = new double[NUM_TYPES][][];

		// we create an extra bin to hold overflows.
		countData[TYPE_TIME] = new double[timeNumBins+1][numEPs];
		countData[TYPE_MSG_SIZE] = new double[msgNumBins+1][numEPs];


		// for maintaining "begin" entries for the data type we
		// wish to monitor.
		LogEntryData[] typeLogs = new LogEntryData[NUM_TYPES];
		for (int i=0; i<NUM_TYPES; i++) {
			typeLogs[i] = new LogEntryData();
		}
		// for maintaining interval-based events 
		boolean[] isActive = new boolean[NUM_TYPES];

		ProgressMonitor progressBar = new ProgressMonitor(this, "Reading log files", "", 0, pes.size());
		int curPeCount = 0;
		while (pes.hasMoreElements()) {
			int pe = pes.nextElement();
			if (!progressBar.isCanceled()) {
				progressBar.setNote("[PE: " + pe + " ] Reading data.");
				progressBar.setProgress(curPeCount);
			} else {
				progressBar.close();
			}
			curPeCount++;
			r = new GenericLogReader(MainWindow.runObject[myRun].getLogName(pe),
					MainWindow.runObject[myRun].getVersion());
			try {
				LogEntryData logdata = r.nextEventOnOrAfter(startTime);
				boolean done = false;
				while (!done) {
					switch (logdata.type) {
					case ProjDefs.BEGIN_PROCESSING:
						// NOTE: If prior BEGIN never got terminated,
						// simply drop the data given current tracing
						// scheme (ie. do nothing)
						if (logdata.time > endTime) {
							done = true;
						} else {
							// swap logdata (ie. note the BEGIN event)
							LogEntryData tmpLogPtr = logdata;
							logdata = typeLogs[TYPE_TIME];
							typeLogs[TYPE_TIME] = tmpLogPtr;
							isActive[TYPE_TIME] = true;
						}
						break;
					case ProjDefs.END_PROCESSING:
						if (!isActive[TYPE_TIME]) {
							// NOTE: No corresponding BEGIN, so this
							// instance of END must be ignored given
							// current tracing scheme.
							if (logdata.time > endTime) {
								done = true;
							}
							break;
						} else {
							if (logdata.entry != typeLogs[TYPE_TIME].entry) {
								// The events are mismatched! Clear all.
								// Possible under current tracing scheme.
								isActive[TYPE_TIME] = false;
								break;
							}
							// NOTE: Even if the END event happens past 
							// the range, it is recorded as the proper 
							// execution time of the event.
							executionTime = 
								logdata.time - typeLogs[TYPE_TIME].time;
							adjustedTime = executionTime - timeMinBinSize;
							// respect user threshold
							if (adjustedTime >= 0) {
								int targetBin = 
									(int)(adjustedTime/timeBinSize);
								if (targetBin >= timeNumBins) {
									targetBin = timeNumBins;
								}
								countData[TYPE_TIME][targetBin][logdata.entry] += 1.0;
							}
							isActive[TYPE_TIME] = false;
						}
						break;
					case ProjDefs.CREATION:
						if (logdata.time > endTime) {
							break;
						}
						// respect the user threshold.
						adjustedSize =
							logdata.msglen - msgMinBinSize;
						if (adjustedSize >= 0) {
							int targetBin = (int)(adjustedSize/msgBinSize);
							if (targetBin >= msgNumBins) {
								targetBin = msgNumBins;
							}
							countData[TYPE_MSG_SIZE][targetBin][logdata.entry]+=1.0;
						}
						break;
					}
					if (!done) {
						logdata = r.nextEvent();
					}
				}
			} catch(EOFException e) {
				// do nothing just reached end-of-file
			} catch(Exception e) {
				System.err.println("Exception " + e);
				e.printStackTrace();
				System.exit(-1);
			}
		}
		progressBar.close();
		return countData;
	}

	// override the super class' createMenus(), add any menu items in 
	// fileMenu if needed, add any new menus to the menuBar
	// then call super class' createMenus() to add the menuBar to the Window
	protected void createMenus()
	{
		fileMenu = Util.makeJMenu(fileMenu,
				new Object[]
				           {
				"Select Entry Points"
				           },
				           null,
				           this);
		menuBar.add(Util.makeJMenu("View", 
				new Object[]
				           {
				new JCheckBoxMenuItem("Show Longest EPs",true)
				           },
				           null,
				           this));
		super.createMenus();
	}
}
