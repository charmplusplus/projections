package projections.Tools.UserStatsPerPE;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.io.IOException;

import javax.swing.*;

import projections.gui.GenericGraphColorer;
import projections.gui.GenericGraphWindow;
import projections.gui.IntervalChooserPanel;
import projections.gui.MainWindow;
import projections.gui.RangeDialog;
import projections.gui.U;
import projections.gui.Util;
import projections.gui.ColorManager;
import projections.misc.LogEntryData;
import projections.analysis.ProjDefs;
import projections.analysis.GenericLogReader;
import projections.analysis.EndOfLogSuccess;
import projections.analysis.TimedProgressThreadExecutor;

/* UserStats Per PE Tool by Joshua lew 7/6/16. This tool plots every User Stat on a bar graph.
Each X tick is a different PE. The Popups provide additional info about each stat on each PE.
The Use can choose whether to plot max, min, avg */

public class UserStatsProcWindow extends GenericGraphWindow
implements ItemListener, ActionListener
{

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	private static int myRun = 0;

	// Sent External code commented out and may be implemented later

	private UserStatsProcWindow      thisWindow;

	//    private EntrySelectionDialog entryDialog;

	private JPanel	   mainPanel;
	private IntervalChooserPanel intervalPanel;

	private JPanel	   graphPanel;
	private JPanel     controlPanel;

	private JButton	   setRanges;
	//    private JButton	   epSelection;


	private SortedSet<Integer> processorList;


	private String[]	statNames;
	private double[][]	avgValue;
	private double[][]	maxValue;
	private double[][]	minValue;
	private int[][]		numCalls;
	private double[][]  statRate;

	private int 		numStats;
	private int 		numPEs;

	// format for output
	private DecimalFormat  _format;

	private JPanel	checkBoxPanel;
	private Checkbox statRateBox;
	private Checkbox avgValueBox;
	private Checkbox maxValueBox;
	private Checkbox minValueBox;

	private JRadioButton microseconds;
	private JRadioButton milliseconds;
	private JRadioButton seconds;

	private double unitTime = 1000.0;
	private String unitTimeStr = "ms";

	public UserStatsProcWindow(MainWindow mainWindow) {
		super("User Stats over Time Graph - " + MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);

		// Read in information about User Stats
		numStats = MainWindow.runObject[myRun].getNumUserDefinedStats();
		statNames = MainWindow.runObject[myRun].getUserStatNames();
		_format = new DecimalFormat("###,###.###");
		createMenus();
		createLayout();
		pack();
		thisWindow = this;

		showDialog();
	}

	//Set initial layout of window
	private void createLayout() {

		mainPanel = new JPanel();
		getContentPane().add(mainPanel);
		GridBagConstraints gbc = new GridBagConstraints();
		GridBagLayout gbl = new GridBagLayout();

		checkBoxPanel = new JPanel();
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.setLayout(gbl);


		// checkbox panel items
		CheckboxGroup cbg = new CheckboxGroup();
		statRateBox = new Checkbox("Statistic Rate", cbg, true);
		statRateBox.addItemListener(this);
		avgValueBox = new Checkbox("Average Value", cbg, false);
		avgValueBox.addItemListener(this);
		maxValueBox = new Checkbox("Max Value", cbg, false);
		maxValueBox.addItemListener(this);
		minValueBox = new Checkbox("Min Value", cbg, false);
		minValueBox.addItemListener(this);
		Util.gblAdd(checkBoxPanel, statRateBox, gbc, 0,0, 1,1, 1,1);
		Util.gblAdd(checkBoxPanel, avgValueBox, gbc, 1,0, 1,1, 1,1);
		Util.gblAdd(checkBoxPanel, maxValueBox, gbc, 2,0, 1,1, 1,1);
		Util.gblAdd(checkBoxPanel, minValueBox, gbc, 3,0, 1,1, 1,1);

        JPanel unitPanel = new JPanel();
        ButtonGroup unit_bg = new ButtonGroup();
        microseconds = new JRadioButton("Microseconds", false);
        milliseconds = new JRadioButton("Milliseconds", true);
        seconds = new JRadioButton("Seconds", false);

        microseconds.addActionListener(this);
        milliseconds.addActionListener(this);
        seconds.addActionListener(this);

        unit_bg.add(microseconds);
        unit_bg.add(milliseconds);
        unit_bg.add(seconds);
        unitPanel.add(microseconds);
        unitPanel.add(milliseconds);
        unitPanel.add(seconds);

		// control panel items
		setRanges = new JButton("Select New Range");
		setRanges.addActionListener(this);

		controlPanel = new JPanel();
		controlPanel.setLayout(gbl);
		Util.gblAdd(controlPanel, setRanges,   gbc, 0,0, 1,1, 0,0);

		graphPanel = getMainPanel();
		Util.gblAdd(mainPanel, graphPanel,     gbc, 0,1, 1,1, 1,1);
		Util.gblAdd(mainPanel, controlPanel,   gbc, 0,4, 1,0, 0,0);

		Util.gblAdd(mainPanel, checkBoxPanel,   gbc, 0,2, 1,1, 0,0);
	    Util.gblAdd(mainPanel, unitPanel,       gbc, 0,3, 1,1, 0,0);
	}

	//Set graph data.
	protected void setGraphSpecificData(){
		setXAxis("Processor",processorList );
		setYAxis("Statistic/ms", "");
		setDataSource("User Stats", statRate,new MyColorer(), this);
		refreshGraph();
	}

	//Show dialog and read in data based off user choices
	public void showDialog() {
		if (dialog == null) {
			dialog = new RangeDialog(this, "Select Range", null, false);
		}

		dialog.displayDialog();
		if (!dialog.isCancelled()) {
			//Read in information from dialog
			processorList = new TreeSet<Integer>(dialog.getSelectedProcessors());
			numPEs = 0;
			for(Integer pe : processorList)
				numPEs+=1;

			final SwingWorker worker =  new SwingWorker() {
				public Object doInBackground() {
					getData(dialog.getStartTime(),dialog.getEndTime());
					return null;
				}
				public void done() {
				    statRateBox.setState(true);
                    milliseconds.setSelected(true);
                    unitTime = 1000.0;
                    unitTimeStr = "ms";
					setGraphSpecificData();
					thisWindow.setVisible(true);
					thisWindow.repaint();
				}
			};
			worker.execute();
		}
	}

	/*Get data based off user choices. Note unlike UserStatsOverTime, this getData
	is only called once. If the dialog is opened again, the data is all refreshed and re evaluated  */
	private void getData(long startTime, long endTime) {

		//initialize data arrays. Note the array is [numPes][numStats]
		avgValue = new double[numPEs][numStats];
		maxValue = new double[numPEs][numStats];
		minValue = new double[numPEs][numStats];
		numCalls = new int[numPEs][numStats];
		statRate = new double[numPEs][numStats];

		int peIdx = 0;
		for(Integer pe : processorList) {

			GenericLogReader reader = new GenericLogReader( pe,
					MainWindow.runObject[myRun].getVersion());
			LogEntryData logData;

			// Skip to the first begin.
			try {
				logData = reader.nextEventOfType(ProjDefs.USER_STAT);

				while (logData.time < startTime) {
					logData = reader.nextEventOfType(ProjDefs.USER_STAT);
				}
				int statIndex;
				while (true) {

					//Read in index of stat in current LogEntry
					statIndex = MainWindow.runObject[myRun].getUserDefinedStatIndex(logData.userEventID);
					//Store the data accordingly
					avgValue[peIdx][statIndex] += logData.stat;
					statRate[peIdx][statIndex] += logData.stat;
					//keeps track of how many times this specific stat was updated on this specific PE
					numCalls[peIdx][statIndex]+=1;
					//Set min and max accordingly.
					if(numCalls[peIdx][statIndex]==1){
						minValue[peIdx][statIndex]=logData.stat;
						maxValue[peIdx][statIndex]= logData.stat;
					}
					else if (logData.stat<minValue[peIdx][statIndex])
						minValue[peIdx][statIndex] =logData.stat;
					else if (logData.stat>maxValue[peIdx][statIndex])
						maxValue[peIdx][statIndex] =logData.stat;

					//continue until end time is reached
					logData = reader.nextEventOfType(ProjDefs.USER_STAT);
					if (logData.time > endTime) {
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
			//increment peIdx and loop again
			peIdx+=1;

		}

		double interval = (endTime - startTime)/1000.0;
		/*To make avgValue, we must divide each element, which currently holds its sum,
		by the number of times it was called to get an avg value*/
		for(int i = 0; i<numPEs;i+=1) {

			for( int j = 0; j<numStats; j+=1) {
				avgValue[i][j] = avgValue[i][j] / numCalls[i][j];
				statRate[i][j] = statRate[i][j] / interval;
			}
		}
   	 }

	//return popup for current location.
	//xVal == PEIdx, yVal == StatIndex
	public String[] getPopup(int xVal, int yVal) {
		if( (xVal < 0) || (yVal <0))
			return null;


		String[] rString = new String[6];
		rString[0] = "Processor " + xAxis.getIndexName(xVal);
		rString[1] = "Name: " + statNames[yVal];
		rString[2] = "Call Rate: " + _format.format(statRate[xVal][yVal]) + " (per " + unitTimeStr + ")";
		rString[3] = "Avg Value: " +avgValue[xVal][yVal];
		rString[4] = "Max Value: " +maxValue[xVal][yVal];
		rString[5] = "Min Value: " +minValue[xVal][yVal];

		return rString;
	}

	//React to button presses appropriately
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			JButton b = (JButton)e.getSource();
			if (b == setRanges) {
				showDialog();
			}

		} else if (e.getSource() instanceof JMenuItem) {
			String arg = ((JMenuItem)e.getSource()).getText();
			if (arg.equals("Close")) {
				close();
			} else if(arg.equals("Set Range")) {
				showDialog();
			}
		}
        else if (e.getSource() == microseconds) {
            scaleHistogramData(1.0);
        }
        else if (e.getSource() == milliseconds) {
            scaleHistogramData(1000.0);
        }
        else if (e.getSource() == seconds) {
            scaleHistogramData(1000000.0);
        }
	}

    private void scaleHistogramData(double newUnit) {
        double scale = newUnit / unitTime;
        for (int pe = 0; pe < numPEs; pe++) {
            for (int stat = 0; stat < numStats; stat++) {
                statRate[pe][stat] *= scale;
            }
        }
        unitTime = newUnit;
        if (unitTime == 1.0) unitTimeStr = "us";
        else if (unitTime == 1000.0) unitTimeStr = "ms";
        else unitTimeStr = "s";

        if (statRateBox.getState()) {
            setYAxis("Statistic/" + unitTimeStr, "");
            setDataSource("Statistic Rate", statRate, new MyColorer(), this);
            super.refreshGraph();
        }
    }

	//Change the dataSource depending on which checkbox is currently clicked.
	public void itemStateChanged(ItemEvent ae){
		if(ae.getSource() instanceof Checkbox){
			setCursor(new Cursor(Cursor.WAIT_CURSOR));
			Checkbox cb = (Checkbox)ae.getSource();
			if (cb == statRateBox) {
			    setYAxis("Statistic/" + unitTimeStr, "");
			    setDataSource("Statistic Rate", statRate, new MyColorer(), this);
			    super.refreshGraph();
            } else if((cb == avgValueBox)) {
                setYAxis("Value", "");
				setDataSource("Average Value",avgValue ,new MyColorer(), this);
				super.refreshGraph();
			}else if(cb == maxValueBox){
                setYAxis("Value", "");
				setDataSource("Max Value",maxValue ,new MyColorer(), this);
				super.refreshGraph();
			}else if(cb == minValueBox){
                setYAxis("Value", "");
				setDataSource("Min Value", minValue,new MyColorer(), this);
				super.refreshGraph();
			}
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}

	public void repaint() {
		super.refreshGraph();
	}
	
	protected void createMenus(){
		super.createMenus();
	}

	/** A class that provides the colors for the display */
	public class MyColorer implements GenericGraphColorer {
		
		public Paint[] getColorMap() {

			
			Paint[]  outColors = ColorManager.createColorMap(numStats);

			return outColors;
		}
	}
}
