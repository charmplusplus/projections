package projections.gui;

import java.awt.Paint;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.SortedSet;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.misc.LogEntryData;

/**
 *  UserEventsWindow
 *  by Chee Wai Lee
 *
 *  Will replace The old GraphWindow class once a framework for displaying
 *  Legends are in place (and probably replace the name)
 */
class UserEventsWindow extends GenericGraphWindow
		implements ActionListener, EntryMethodVisibility
{

	private UserEventsWindow thisWindow;

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;

	private IntervalChooserPanel intervalPanel;

	// data used for intervalgraphdialog
	private int startInterval;
	private int endInterval;
	private long intervalSize;
	private SortedSet<Integer> processorList;

	// meta data variables
	private int numActivities;
	private int numPEs;
	// Normally derived from MainWindow.runObject[myRun].java
	private String activityNames[];

	// stored raw data
	private double[][] graphData;
	private double[][] graphData_display;
	private long[][] numCalls;
	private boolean[] display_mask;
	private GenericGraphColorer colorer;
    
	UserEventsWindow(MainWindow mainWindow) {
		super("Projections User Events Tool - " +
				MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
		thisWindow = this;
		// hardcode start. Usually derived from MainWindow.runObject[myRun].java
		numActivities = MainWindow.runObject[myRun].getNumUserDefinedEvents();
		activityNames = MainWindow.runObject[myRun].getUserEventNames();
		display_mask = new boolean[numActivities];
		for(int i = 0; i < numActivities; i++)
			display_mask[i] = true;
		colorer = new GenericGraphColorer() {
			private final Paint[] colorMap = ColorManager.createColorMap(numActivities);
			@Override
			public Paint[] getColorMap() {
				return colorMap;
			}
		};
		createMenus();
		if(mChooseColors.getActionListeners()[0] != null)
			mChooseColors.removeActionListener(mChooseColors.getActionListeners()[0]);
		mChooseColors.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				new ChooseUserEntriesWindow(thisWindow, thisWindow, colorer, MainWindow.runObject[myRun].getSts().getUserEventNameMap(), "Event", activityNames);
			}
		});
		createLayout();
		pack();
		showDialog();
	}

	private void createLayout() {
		JPanel mainPanel;
		JPanel controlPanel;

		mainPanel = new JPanel();
		getContentPane().add(mainPanel);

		GridBagConstraints gbc = new GridBagConstraints();
		GridBagLayout gbl = new GridBagLayout();
	
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.setLayout(gbl);

		// Assume no special control features for now
		controlPanel = new JPanel();
		controlPanel.setLayout(gbl);

		JPanel graphPanel = getMainPanel();
		Util.gblAdd(mainPanel, graphPanel, gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(mainPanel, controlPanel, gbc, 0,1, 1,0, 0,0);
	}

	public void showDialog() {
		if (dialog == null) {
			intervalPanel = new IntervalChooserPanel();
			dialog = new RangeDialog(this, "Select Range", intervalPanel, false);
		}

		dialog.displayDialog();
		if (!dialog.isCancelled()){
			intervalSize = intervalPanel.getIntervalSize();
			startInterval = (int)intervalPanel.getStartInterval();
			endInterval = (int)intervalPanel.getEndInterval();
			processorList = dialog.getSelectedProcessors();
			numPEs = processorList.size();
			graphData = new double[numPEs][];
			graphData_display = new double[numPEs][];
			numCalls = new long[numPEs][];
			for(int i = 0; i < numPEs; i++) {
				graphData[i] = new double[numActivities];
				graphData_display[i] = new double[numActivities];
				numCalls[i] = new long[numActivities];
			}
			final SwingWorker worker =  new SwingWorker() {
				public Object doInBackground() {
					constructToolData();
					return null;
				}
				public void done() {
					displayMustBeRedrawn();
					thisWindow.setVisible(true);
				}
			};
			worker.execute();
		}
	}

	private void constructToolData() {
		int count = 0;
		ProgressMonitor progressBar =
				new ProgressMonitor(MainWindow.runObject[myRun].guiRoot,
						"Reading log files",
						"", 0,
						processorList.size());
		progressBar.setNote("Reading");
		progressBar.setProgress(0);

		for(Integer pe : processorList) {
			progressBar.setProgress(count);
			progressBar.setNote("[PE: " + pe + " ] Reading Data.");
			if (progressBar.isCanceled()) {
				return;
			}

			// READ - nothing here
			GenericLogReader reader = new GenericLogReader( pe,
					MainWindow.runObject[myRun].getVersion());
			LogEntryData logData;
			LogEntryData logDataEnd;

			// Skip to the first begin.
			try {
				logData = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
				logDataEnd = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);

				while (logData.time < startInterval*intervalSize) {
					logData = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
					logDataEnd = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
				}
				int eventIndex;
				while (true) {
					// process pair read previously
					eventIndex = MainWindow.runObject[myRun].getUserDefinedEventIndex(logData.userEventID);
					graphData[count][eventIndex] +=
							logDataEnd.time - logData.time;
					numCalls[count][eventIndex]++;
					logData = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
					logDataEnd = reader.nextEventOfType(ProjDefs.USER_EVENT_PAIR);
					if (logDataEnd.time > endInterval*intervalSize) {
						break;
					}
				}
			}catch (EndOfLogSuccess e) {
				// do nothing
			} catch (IOException e) {
				System.out.println("Exception while reading log file " + pe);
			}


			try {
				reader.close();
			} catch (IOException e1) {
				System.err.println("Error: could not close log file reader for processor " + pe );
			}


			count++;
		}
		progressBar.close();
	}

	protected void setGraphSpecificData() {
		setXAxis("Processors", processorList);
		setYAxis("Time (us)", "us");
		setDataSource("User Events", graphData_display, colorer, this);
	}

	private void calcDisplayData() {
		for(int i = 0; i < numPEs; i++) {
			for(int j = 0; j < numActivities; j++) {
				if(display_mask[j])
					graphData_display[i][j] = graphData[i][j];
				else
					graphData_display[i][j] = 0;
			}
		}
	}

	public String[] getPopup(int xVal, int yVal) {
		if ((xVal < 0) || (yVal < 0)) {
			return null;
		}
		String[] rString = new String[3];
	
		rString[0] = "Name: " + activityNames[yVal];
		rString[1] = "Time Spent: " + U.humanReadableString((long)(graphData[xVal][yVal]));
		rString[2] = "Count: " + numCalls[xVal][yVal];
		return rString;
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

	public void refreshGraph() {
		super.refreshGraph();
	}

	protected void createMenus() {
		super.createMenus();
	}

	@Override
	public void displayMustBeRedrawn() {
		calcDisplayData();
		setGraphSpecificData();
		refreshGraph();
	}

	@Override
	public boolean entryIsVisibleID(Integer id) {
		return true;
	}

	@Override
	public int[] getEntriesArray() {
		return null;
	}

	@Override
	public void makeEntryInvisibleID(Integer id) {
		display_mask[id] = false;
	}

	@Override
	public void makeEntryVisibleID(Integer id) {
		display_mask[id] = true;
	}

	@Override
	public boolean hasEntryList() {
		return false;
	}

	@Override
	public boolean handleIdleOverhead() {
		return false;
	}
}
