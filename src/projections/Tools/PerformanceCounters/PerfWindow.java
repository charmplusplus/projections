package projections.Tools.PerformanceCounters;

import projections.analysis.Analysis;
import projections.analysis.TimedProgressThreadExecutor;
import projections.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;

public class PerfWindow extends GenericGraphWindow
implements ActionListener, Clickable
{

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;

	private double[][][] perfCounters;

	private ArrayList<Integer> histogram;

	private JPanel mainPanel;
	private JPanel graphPanel;
	private JPanel counterPanel;

	private JRadioButton[] counterButtons;

	private PerfWindow thisWindow;

	private SortedSet<Integer> peList;

	private int counterIndex = 0;
	private int numPerfCounts = MainWindow.runObject[myRun].getSts().getNumPerfCounts();
	private String[] perfCountNames = MainWindow.runObject[myRun].getSts().getPerfCountNames();


	public PerfWindow(MainWindow mainWindow) {
		super("Projections Performance Counters- " + MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
		mainPanel = new JPanel();
		setLayout(mainPanel);
		//getContentPane().add(mainPanel);
		createMenus();
		createLayout();
		// setPopupText("histArray");
		pack();
		thisWindow = this;
		showDialog();
	}

	public void repaint() {
		super.refreshGraph();
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

		else if (e.getSource() instanceof JRadioButton)
		{
			try {
				counterIndex = Integer.parseInt(e.getActionCommand());

				setDataSource("Performance Counters - " + perfCountNames[counterIndex], perfCounters[counterIndex],
						thisWindow);
				thisWindow.setVisible(true);
				thisWindow.repaint();
			}
			catch (NumberFormatException ex) {

			}
		}
	}


	public String[] getPopup(int xVal, int yVal){
		if( (xVal < 0) || (yVal <0))
			return null;

		Analysis a = MainWindow.runObject[myRun];

		String[] rString = new String[5];

		rString[0] = "Processor " + xVal;

		rString[1] = "EPid: " + a.getEntryNameByIndex(yVal);
		rString[2] = "Perf Counter: " + perfCountNames[counterIndex];
		rString[3] = "Count = " + perfCounters[counterIndex][xVal][yVal];
		rString[4] = "Processor = " + xAxis.getIndexName(xVal);

		return rString;
	}

	public void toolClickResponse(MouseEvent e, int xVal, int yVal) {
		parentWindow.addProcessor(xVal);	
	}

	public void toolMouseMovedResponse(MouseEvent e, int xVal, int yVal) {
	}

	protected void createMenus(){
		super.createMenus();
	}

	private void createLayout() {
		GridBagConstraints gbc = new GridBagConstraints();
		GridBagLayout gbl = new GridBagLayout();

		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.setLayout(gbl);

		graphPanel = getMainPanel();

		counterPanel = new JPanel();
		counterPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
				"Performance Counter Type"));

		ButtonGroup cbgCounterType = new ButtonGroup();
		counterButtons = new JRadioButton[numPerfCounts];
		for (int i = 0; i < numPerfCounts; ++i)
		{
			counterButtons[i] = new JRadioButton(perfCountNames[i]);
			counterButtons[i].setActionCommand(Integer.toString(i));
			if (i == 0)
				counterButtons[i].setSelected(true);
			cbgCounterType.add(counterButtons[i]);
			counterButtons[i].addActionListener(this);
			Util.gblAdd(counterPanel, counterButtons[i], gbc, i, 0, 1, 1, 1, 0);
		}

		Util.gblAdd(graphPanel, counterPanel, gbc, 0, 1, 1, 1, 1, 1);
		Util.gblAdd(mainPanel, graphPanel, gbc, 0,1, 1,1, 1,1);
	}

	public void setGraphSpecificData() {
		// do nothing. **CW** Reconsider such an interface requirement
		// for GenericGraphWindow.
	}

	
	public void showDialog() {
		if (dialog == null) {
			dialog = new RangeDialog(this, "select Range", null, false);
		}

		dialog.displayDialog();
		if (!dialog.isCancelled()){
			
			final SwingWorker worker =  new SwingWorker() {
				public Object doInBackground() {
					peList = new TreeSet<Integer>(dialog.getSelectedProcessors());
					getData(dialog.getStartTime(), dialog.getEndTime(), dialog.getSelectedProcessors());
					return null;
				}
				public void done() {
					setDataSource("Performance Counters - " + perfCountNames[counterIndex], perfCounters[counterIndex],
						thisWindow);
					setYAxis("Perf Counter Values", "");
					setXAxis("Processor", peList);
					thisWindow.setVisible(true);
					thisWindow.repaint();
				}
			};
			worker.execute();
		}
	}


	private void getData(long startTime, long endTime, SortedSet<Integer> pes){
		perfCounters = new double[numPerfCounts][pes.size()][];

		histogram = new ArrayList<Integer>();
		
		// Create a list of worker threads
		LinkedList<Runnable> readyReaders = new LinkedList<Runnable>();
		int pIdx = 0;
    	for(Integer nextPe : pes){
			readyReaders.add( new ThreadedFileReader(nextPe, pIdx, numPerfCounts, startTime, endTime, perfCounters) );
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
		TimedProgressThreadExecutor threadManager = new TimedProgressThreadExecutor("Loading Performance Counter Data in Parallel", readyReaders, guiRootForProgressBar, true);
		threadManager.runAll();

		for (Runnable reader : readyReaders) {
			try {
				reader.wait();
			}
			catch (InterruptedException ex)
			{}
		}


//		Iterator<Runnable> iter = readyReaders.iterator();

//		while(iter.hasNext()){
//			ThreadedFileReader t = (ThreadedFileReader) iter.next();
//		}
	}
}
