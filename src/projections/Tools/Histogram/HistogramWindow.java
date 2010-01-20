package projections.Tools.Histogram;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.LinkedList;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import projections.analysis.ThreadManager;
import projections.gui.GenericGraphWindow;
import projections.gui.MainWindow;
import projections.gui.OrderedIntList;
import projections.gui.RangeDialog;
import projections.gui.U;
import projections.gui.Util;

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
	private int myRun = 0;

	protected static final int NUM_TYPES = 2;
	protected static final int TYPE_TIME = 0;
	protected static final int TYPE_MSG_SIZE = 1;

	// Gui components
	private JButton entrySelectionButton;
	private JButton epTableButton;

	private JRadioButton timeBinButton;
	private JRadioButton msgSizeBinButton;
	private ButtonGroup binTypeGroup;

	
	private BinDialogPanel binpanel;
	
	// Data maintained by HistogramWindow
	// countData is indexed by type, then by bin index followed by ep id.
	// NOTE: bin indices need not be of the same size
	private double[][][] counts;
	private int binType;

	private int timeNumBins;
	private long timeBinSize;
	private long timeMinBinSize;
	private int msgNumBins;
	private long msgBinSize;
	private long msgMinBinSize;

	private HistogramWindow thisWindow;

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
					
					counts = new double[HistogramWindow.NUM_TYPES][][];
					// we create an extra bin to hold overflows.
					int numEPs = MainWindow.runObject[myRun].getNumUserEntries();
					counts[HistogramWindow.TYPE_TIME] = new double[timeNumBins+1][numEPs];
					counts[HistogramWindow.TYPE_MSG_SIZE] = new double[msgNumBins+1][numEPs];

					
					// Create a list of worker threads
					LinkedList<Thread> readyReaders = new LinkedList<Thread>();

					OrderedIntList processorList = dialog.getSelectedProcessors();
					while (processorList.hasMoreElements()) {
						int nextPe = processorList.nextElement();
						readyReaders.add( new ThreadedFileReader(counts, nextPe, dialog.getStartTime(), dialog.getEndTime(), timeNumBins, timeBinSize, timeMinBinSize, msgNumBins, msgBinSize, msgMinBinSize));
					}

					// Determine a component to show the progress bar with
					Component guiRootForProgressBar = null;
					if(thisWindow!=null && thisWindow.isVisible()) {
						guiRootForProgressBar = thisWindow;
					} else if(MainWindow.runObject[myRun].guiRoot!=null && MainWindow.runObject[myRun].guiRoot.isVisible()){
						guiRootForProgressBar = MainWindow.runObject[myRun].guiRoot;
					}

					// Pass this list of threads to a class that manages/runs the threads nicely
					ThreadManager threadManager = new ThreadManager("Loading Histograms in Parallel", readyReaders, guiRootForProgressBar, true);
					threadManager.runThreads();
					
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




	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() instanceof JMenuItem) {
			JMenuItem m = (JMenuItem)e.getSource();
			if(m.getText().equals("Set Range"))
				showDialog();
			else if(m.getText().equals("Close"))
				close();
		} else if (e.getSource()  == timeBinButton) {
			binType = TYPE_TIME;
			setGraphSpecificData();
			refreshGraph();
		} else if (e.getSource()  ==  msgSizeBinButton) {
			binType = TYPE_MSG_SIZE;
			setGraphSpecificData();
			refreshGraph();
		} else if (e.getSource() == entrySelectionButton) {
			System.out.println("selecting entries for display");
		} else if (e.getSource() == epTableButton) {
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
			setXAxis("Bin Interval Size (" + U.humanReadableString(timeBinSize) + ")", "Time", timeMinBinSize, timeBinSize);
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
			bubbleText[2] = "Bin: " + U.humanReadableString(xVal*timeBinSize+timeMinBinSize) +
			" to " + U.humanReadableString((xVal+1)*timeBinSize+timeMinBinSize);
		} else {
			bubbleText[2] = "Bin: > " + U.humanReadableString(timeNumBins*timeBinSize+
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

	

	protected void createMenus() {
		super.createMenus();
		
		// Add in our own special menu
		menuBar.add(Util.makeJMenu("View", 
				new Object[]
				           {
				new JCheckBoxMenuItem("Show Longest EPs",true)
				           },
				           this));

	}
}
