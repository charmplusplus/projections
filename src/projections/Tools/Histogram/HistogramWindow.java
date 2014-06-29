package projections.Tools.Histogram;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingWorker;
import javax.swing.JOptionPane;

import projections.analysis.TimedProgressThreadExecutor;
import projections.gui.ChooseEntriesWindow;
import projections.gui.EntryMethodVisibility;
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
implements ActionListener, EntryMethodVisibility
{

        // Temporary hardcode. This variable will be assigned appropriate
        // meaning in future versions of Projections that support multiple
        // runs.
        private int myRun = 0;

        protected static final int NUM_TYPES = 4;
        protected static final int TYPE_TIME = 0;
        protected static final int TYPE_MSG_SIZE = 1;
    	protected static final int TYPE_ACCTIME = 2;
	protected static final int TYPE_IDLE_PERC = 3;

    	protected static final int TYPE_ALL_ENTRIES = 1000;
    	protected static final int TYPE_CHOOSE_ENTRIES = 1001;
    	protected static final int TYPE_LONGEST_ENTRIES = 1002;

    // Gui components
        private JButton entrySelectionButton;
        private JButton epTableButton;

        private JRadioButton timeBinButton;
        private JRadioButton timeAccumulateBinButton;
        private JRadioButton msgSizeBinButton;
	private JRadioButton idleButton;
        private ButtonGroup binTypeGroup;

    	private JRadioButton   allEntriesButton;
    	private JRadioButton   chooseEntriesButton;
    	private JRadioButton   longestEntryButton;

    	private ButtonGroup entryTypeGroup;

        private BinDialogPanel binpanel;

        // Data maintained by HistogramWindow
        // countData is indexed by type, then by bin index followed by ep id.
        // NOTE: bin indices need not be of the same size

    	private int numEPs;

    	private double[][][] counts;
        private double[][][] counts_display;
    	private boolean[] display_mask;

        private int binType;
    	private int entryDisplayType;

        private int timeNumBins;
        private long timeBinSize;
        private long timeMinBinSize;
        private int msgNumBins;
        private long msgBinSize;
        private long msgMinBinSize;
	private int idleNumBins;
	private long idleBinSize;
	private long idleMinBinSize;

        private HistogramWindow thisWindow;

        private DecimalFormat _format;

    	/* YH Sun  total execution time */
    	private double[][] executionTime;
    	private double totalExecutionTime;
    	private double longestEntryTime;
    	private int longestEntryIndex;
    	private double maxAccEntryTime;
    	private int maxAccEntryIndex;

        public HistogramWindow(MainWindow mainWindow)
        {
                super("Projections Histograms", mainWindow);
                thisWindow = this;

                binType = TYPE_TIME;
                entryDisplayType = TYPE_ALL_ENTRIES;
        	_format = new DecimalFormat();

                setTitle("Projections Histograms - " + MainWindow.runObject[myRun].getFilename() + ".sts");

                createMenus();
                if (mChooseColors.getActionListeners()[0]!=null)
                        mChooseColors.removeActionListener(mChooseColors.getActionListeners()[0]);
                mChooseColors.addActionListener(new MenuHandler() {
                        public void actionPerformed(ActionEvent e) {
                                new ChooseEntriesWindow(thisWindow, true, thisWindow);
                        }
                });
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
                if (dialog == null)
		{
                        binpanel = new BinDialogPanel();
                        dialog = new RangeDialog(this, "Select Histogram Time Range", binpanel, false);
                }

                dialog.displayDialog();
                if (!dialog.isCancelled())
		{
                        final SwingWorker worker = new SwingWorker()
			{
                                public Object doInBackground()
				{
                                        timeNumBins = binpanel.getTimeNumBins();
                                        timeBinSize = binpanel.getTimeBinSize();
                                        timeMinBinSize = binpanel.getTimeMinBinSize();
                                        msgNumBins = binpanel.getMsgNumBins();
                                        msgBinSize = binpanel.getMsgBinSize();
                                        msgMinBinSize = binpanel.getMsgMinBinSize();
					idleNumBins = binpanel.getIdleNumBins();
					idleBinSize = binpanel.getIdleBinSize();
					idleMinBinSize = binpanel.getIdleMinBinSize();
					if (timeBinSize == 0 || msgBinSize == 0 || idleBinSize == 0)
					{
						//prevents dividing by zero
						JOptionPane.showMessageDialog(null, "You cannot enter a bin size of zero.", "Error", JOptionPane.ERROR_MESSAGE);
						System.out.println("You cannot enter a bin size of zero.");
						return null;
					}
                                        binType = binpanel.getSelectedType();
                                        counts = new double[HistogramWindow.NUM_TYPES][][];
                                        counts_display = new double[HistogramWindow.NUM_TYPES][][];
                                        // we create an extra bin to hold overflows.
                                        //YSun Changed
                                        //numEPs = MainWindow.runObject[myRun].getNumUserEntries();
                                        numEPs = MainWindow.runObject[myRun].getNumUserEntries()+1;
                                        counts[HistogramWindow.TYPE_TIME] = new double[timeNumBins+1][numEPs];
                                        counts[HistogramWindow.TYPE_ACCTIME] = new double[timeNumBins+1][numEPs];
                                        counts[HistogramWindow.TYPE_MSG_SIZE] = new double[msgNumBins+1][numEPs];
					counts[HistogramWindow.TYPE_IDLE_PERC] = new double[idleNumBins+1][numEPs];

                    			counts_display[HistogramWindow.TYPE_TIME] = new double[timeNumBins+1][numEPs];
                                        counts_display[HistogramWindow.TYPE_ACCTIME] = new double[timeNumBins+1][numEPs];
                                        counts_display[HistogramWindow.TYPE_MSG_SIZE] = new double[msgNumBins+1][numEPs];
					counts_display[HistogramWindow.TYPE_IDLE_PERC] = new double[idleNumBins+1][numEPs];

                    			display_mask = new boolean[numEPs];

                    			for(int _i=0; _i<numEPs; _i++)
                    			{
                        			display_mask[_i] = true;
                    			}

                    			executionTime = new double[4][numEPs];
                                        // Create a list of worker threads
                                        LinkedList<Runnable> readyReaders = new LinkedList<Runnable>();

                                        for(Integer nextPe : dialog.getSelectedProcessors())
					{
                                                readyReaders.add( new ThreadedFileReader(counts, nextPe, dialog.getStartTime(), dialog.getEndTime(), timeNumBins, timeBinSize, timeMinBinSize, msgNumBins, msgBinSize, msgMinBinSize, idleNumBins, idleBinSize, idleMinBinSize, executionTime));
                                        }

                                        // Determine a component to show the progress bar with
                                        Component guiRootForProgressBar = null;
                                        if(thisWindow!=null && thisWindow.isVisible())
					{
                                                guiRootForProgressBar = thisWindow;
                                        }
					else if(MainWindow.runObject[myRun].guiRoot!=null && MainWindow.runObject[myRun].guiRoot.isVisible())
					{
                                                guiRootForProgressBar = MainWindow.runObject[myRun].guiRoot;
                                        }

                                        // Pass this list of threads to a class that manages/runs the threads nicely
                                        TimedProgressThreadExecutor threadManager = new TimedProgressThreadExecutor("Loading Histograms in Parallel", readyReaders, guiRootForProgressBar, true);
                                        threadManager.runAll();

                                        return null;
                                }
                                
				protected void done()
				{
                                        // Make the gui status reflect what was chosen in the dialog box
                                        if(binType == TYPE_MSG_SIZE)
                                                msgSizeBinButton.setSelected(true);
                                        else if (binType == TYPE_TIME)
					{
                                                timeBinButton.setSelected(true);
                    			}
					else if (binType == TYPE_ACCTIME)
                    			{
                        			timeAccumulateBinButton.setSelected(true);
                    			}
					else if (binType == TYPE_IDLE_PERC)
					{
						idleButton.setSelected(true);
					}
                    			totalExecutionTime = 0;
                    			for(int _i=0; _i<executionTime[0].length; _i++)
                    			{
                        			totalExecutionTime += executionTime[0][_i];
                        			if(executionTime[0][_i] > 0)
                            			System.out.println(" Entry method:" + MainWindow.runObject[myRun].getEntryNameByIndex(_i) + "  time: " + executionTime[0][_i] + "\t max time=" + executionTime[1][_i] + "\t total frequency=" +executionTime[3][_i]  );
                        			if(longestEntryTime < executionTime[1][_i])
                        			{
                            				longestEntryTime = executionTime[1][_i];
                            				longestEntryIndex = _i;
                        			}
                        			if(maxAccEntryTime < executionTime[0][_i])
                        			{
                            				maxAccEntryTime = executionTime[0][_i];
                            				maxAccEntryIndex = _i;
                        			}
                    			}
                    			//System.out.println(" Total execution time :" + totalExecutionTime + "\t max EntryMethod time:"+ maxEntryTime);
                    			System.out.println(" Total execution time :" + totalExecutionTime + "\t max EntryMethod:" + MainWindow.runObject[myRun].getEntryNameByIndex(maxAccEntryIndex) + ", time:"+ maxAccEntryTime +"\n Longest Entry method is:" + MainWindow.runObject[myRun].getEntryNameByIndex(longestEntryIndex) + ", time is:"+longestEntryTime);

                    			calcDisplayData();
                    			setGraphSpecificData();
                                        refreshGraph();
                                        thisWindow.setVisible(true);
                                }
                        };
                        worker.execute();
                }
        }


	public void calcDisplayData()
    	{
        	for(int i=0; i<HistogramWindow.NUM_TYPES; i++)
        	{
			int bound = counts[i].length;
            		for(int j=0; j<bound; j++)
            		{
                		for(int m=0; m<numEPs; m++)
                		{
                    			if(display_mask[m])
					{
                        			counts_display[i][j][m] = counts[i][j][m];
                    			}
					else
                        		{
						counts_display[i][j][m] = 0;
					}
                		}
            		}
        	}
    	}

        public void actionPerformed(ActionEvent e)
        {
                if (e.getSource() instanceof JMenuItem)
		{
                        JMenuItem m = (JMenuItem)e.getSource();
                        if(m.getText().equals("Set Range"))
			{
                                showDialog();
			}                        
			else if(m.getText().equals("Close"))
			{
                                close();
			}
                }
		else if (e.getSource()  == timeBinButton)
		{
                        binType = TYPE_TIME;
                        setGraphSpecificData();
                        refreshGraph();
                }
		else if(e.getSource() == timeAccumulateBinButton)
		{
 		        binType = TYPE_ACCTIME;
                        setGraphSpecificData();
                        refreshGraph();
        	}
		else if (e.getSource()  ==  msgSizeBinButton)
		{
                        binType = TYPE_MSG_SIZE;
                        setGraphSpecificData();
                        refreshGraph();
                } 
		else if (e.getSource() == idleButton)
		{
			binType = TYPE_IDLE_PERC;
			setGraphSpecificData();
			refreshGraph();
		}
		  else if (e.getSource() == entrySelectionButton)
		{
                        System.out.println("selecting entries for display");
                }
		else if (e.getSource() == epTableButton)
		{
                        System.out.println("Showing out of range entries");
                }
		else if(e.getSource() == allEntriesButton)
        	{
            		System.out.println("Before in type"+entryDisplayType + "Switching to" + TYPE_ALL_ENTRIES);
            		if(entryDisplayType != TYPE_ALL_ENTRIES)
            		{
                		entryDisplayType = TYPE_ALL_ENTRIES;
                		for(int m=0; m<numEPs; m++)
                		{
                    			display_mask[m] = true;
                		}
                		calcDisplayData();
            		}
            		setGraphSpecificData();
            		refreshGraph();
		}
		else if (e.getSource() ==  chooseEntriesButton)
        	{
            		entryDisplayType = TYPE_CHOOSE_ENTRIES;
            		setGraphSpecificData();
            		refreshGraph();
        	}
		else if(e.getSource() == longestEntryButton)
        	{
                	System.out.println("Before in type"+entryDisplayType + "Switching to" + TYPE_LONGEST_ENTRIES);
            		if(entryDisplayType != TYPE_LONGEST_ENTRIES)
            		{
                		entryDisplayType = TYPE_LONGEST_ENTRIES;
                		for(int m=0; m<numEPs; m++)
                		{
                    			display_mask[m] = false;
                		}
                		display_mask[maxAccEntryIndex] = true;
                		calcDisplayData();
            		}
            		setGraphSpecificData();
            		refreshGraph();
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
        	//buttonPanel.setBorder(new TitledBorder(new LineBorder(Color.black),
                //		"Histogram Controls"));

                entrySelectionButton = new JButton("Select Entries");
                entrySelectionButton.addActionListener(this);
                epTableButton = new JButton("Out-of-Range EPs");
                epTableButton.addActionListener(this);

        	JPanel displayTypePanel = new JPanel();
        	displayTypePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Display Type"));
        	displayTypePanel.setLayout(gbl);

        	timeBinButton = new JRadioButton("Execution Time", true);
                timeBinButton.addActionListener(this);
                timeAccumulateBinButton = new JRadioButton("Accumulate Execution Time", true);
                timeAccumulateBinButton.addActionListener(this);
                msgSizeBinButton = new JRadioButton("Message Size");
                msgSizeBinButton.addActionListener(this);
		idleButton = new JRadioButton("Idle Percentages");
		idleButton.addActionListener(this);

                binTypeGroup = new ButtonGroup();
                binTypeGroup.add(timeBinButton);
                binTypeGroup.add(timeAccumulateBinButton);
                binTypeGroup.add(msgSizeBinButton);
		binTypeGroup.add(idleButton);

                displayTypePanel.add(timeBinButton);
                displayTypePanel.add(timeAccumulateBinButton);
                displayTypePanel.add(msgSizeBinButton);
		displayTypePanel.add(idleButton);
        	buttonPanel.add(displayTypePanel);


        	JPanel entryTypePanel = new JPanel();
        	entryTypePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Entry Type"));
        	entryTypePanel.setLayout(gbl);
        	allEntriesButton = new JRadioButton("All Entries", true);
        	allEntriesButton.addActionListener(this);
        	chooseEntriesButton = new JRadioButton("Choose Entries...");
        	chooseEntriesButton.addActionListener(this);
        	longestEntryButton = new JRadioButton("Longest Entry");
        	longestEntryButton.addActionListener(this);

        	entryTypeGroup = new ButtonGroup();
                entryTypeGroup.add(allEntriesButton);
                entryTypeGroup.add(chooseEntriesButton);
                entryTypeGroup.add(longestEntryButton);

        	entryTypePanel.add(allEntriesButton);
        	entryTypePanel.add(chooseEntriesButton);
        	entryTypePanel.add(longestEntryButton);
        	buttonPanel.add(entryTypePanel);

        	//buttonPanel.add(entrySelectionButton);
		//buttonPanel.add(epTableButton);

                Util.gblAdd(mainPanel, graphPanel,  gbc, 0,0, 1,1, 1,1);
                Util.gblAdd(mainPanel, buttonPanel, gbc, 0,1, 1,1, 0,0);

                return mainPanel;
        }

        protected void setGraphSpecificData()
	{
                if (binType == TYPE_TIME)
		{
                        setXAxis("Entry Method Duration (at " + U.humanReadableString(timeBinSize) + " resolution)", "Time", timeMinBinSize, timeBinSize);
                        setYAxis("Number of Occurrences", "");
                        setDataSource("Histogram", counts_display[TYPE_TIME], thisWindow);
                        for(int i=0; i<timeNumBins+1; i++)
                        {
                            System.out.println(" ocurrence " + i + " : " + counts_display[TYPE_TIME][i][numEPs-1]);
                        }
                }
		if (binType == TYPE_ACCTIME)
		{
			setXAxis("Entry Method Duration (at " + U.humanReadableString(timeBinSize) + " resolution)", "Time", timeMinBinSize, timeBinSize);
                        setYAxis("Time in Bin range (us)", "");
                        setDataSource("Histogram", counts_display[TYPE_ACCTIME], thisWindow);
                        for(int i=0; i<timeNumBins+1; i++)
                        {
                            System.out.println(" ocurrence " + i + " : " + counts_display[TYPE_ACCTIME][i][numEPs-1]);
                        }
	        }
		else if (binType == TYPE_MSG_SIZE)
		{
                        setXAxis("Message Size (at " +  _format.format(msgBinSize) + " byte resolution)",  "", msgMinBinSize, msgBinSize);
                        setYAxis("Number of Occurrences", "");
                        setDataSource("Histogram", counts_display[TYPE_MSG_SIZE], thisWindow);
                }
		else if (binType == TYPE_IDLE_PERC)
		{
			setXAxis("Idle Percentages (at " + _format.format(idleBinSize) + "% resolution)", "", idleMinBinSize, idleBinSize);
			setYAxis("Number of Processors", "");
			setDataSource("Histogram", counts_display[TYPE_IDLE_PERC], thisWindow);
		}
        }

        protected void refreshGraph()
        {
                super.refreshGraph();
        }

        public String[] getPopup(int xVal, int yVal)
	{
                if (binType == TYPE_TIME)
		{
                        return getTimePopup(xVal, yVal);
                }
		else if (binType == TYPE_ACCTIME)
		{
            		return getACCTimePopup(xVal, yVal);
        	}
		else if (binType == TYPE_MSG_SIZE)
		{
            		return getMsgSizePopup(xVal, yVal);
                }
		else if (binType == TYPE_IDLE_PERC)
		{
			return getIdlePopup(xVal, yVal);
		}
                return null;
        }

        private String[] getTimePopup(int xVal, int yVal)
	{
                DecimalFormat df = new DecimalFormat("#.##");
        	String bubbleText[] = new String[5];

                bubbleText[0] = MainWindow.runObject[myRun].getEntryNameByIndex(yVal);
                bubbleText[1] = "Count: " + counts_display[TYPE_TIME][xVal][yVal];
                bubbleText[2] = "Time:"+counts_display[TYPE_ACCTIME][xVal][yVal];
                bubbleText[3] = "Time Percentage:"+(df.format((counts_display[TYPE_ACCTIME][xVal][yVal]/totalExecutionTime)*100))+"%";
                if (xVal < timeNumBins)
		{
                        bubbleText[4] = "Bin: " + U.humanReadableString(xVal*timeBinSize+timeMinBinSize) +
                        " to " + U.humanReadableString((xVal+1)*timeBinSize+timeMinBinSize);
                } 
		else
		{
                        bubbleText[4] = "Bin: > " + U.humanReadableString(timeNumBins*timeBinSize+
                                        timeMinBinSize);
                }
                return bubbleText;
        }

	private String[] getACCTimePopup(int xVal, int yVal)
	{
		DecimalFormat df = new DecimalFormat("#.##");
                String bubbleText[] = new String[5];

                bubbleText[0] = MainWindow.runObject[myRun].getEntryNameByIndex(yVal);
                bubbleText[1] = "Time: " + counts_display[TYPE_ACCTIME][xVal][yVal];
                bubbleText[2] = "Count:"+counts_display[TYPE_TIME][xVal][yVal];
                bubbleText[3] = "Time Percentage:"+(df.format((counts_display[TYPE_ACCTIME][xVal][yVal]/totalExecutionTime)*100))+"%";
                if (xVal < timeNumBins)
		{
                        bubbleText[4] = "Bin: " + U.humanReadableString(xVal*timeBinSize+timeMinBinSize) +
                        " to " + U.humanReadableString((xVal+1)*timeBinSize+timeMinBinSize);
                }
		else
		{
                        bubbleText[4] = "Bin: > " + U.humanReadableString(timeNumBins*timeBinSize+
                                        timeMinBinSize);
                }
                return bubbleText;
        }

        private String[] getMsgSizePopup(int xVal, int yVal)
	{
                String bubbleText[] = new String[3];

                bubbleText[0] = MainWindow.runObject[myRun].getEntryNameByIndex(yVal);
                bubbleText[1] = "Count: " + counts_display[TYPE_MSG_SIZE][xVal][yVal];
                if (xVal < msgNumBins)
		{
                        bubbleText[2] = "Bin: " +
                        _format.format(xVal*msgBinSize+msgMinBinSize) +
                        " bytes to " + _format.format((xVal+1)*msgBinSize+
                                        msgMinBinSize) +
                                        " bytes";
                }
		else
		{
                        bubbleText[2] = "Bin: > " +
                        _format.format(msgNumBins*msgBinSize+msgMinBinSize)+" bytes";
                }
                return bubbleText;
        }

	private String[] getIdlePopup(int xVal, int yVal)
	{
		String bubbleText[] = new String[3];

		bubbleText[0] = "Idle Percentage";
		bubbleText[1] = "Count: " + counts_display[TYPE_IDLE_PERC][xVal][yVal];
		if (xVal < idleNumBins)
		{
			bubbleText[2] = "Bin: " +
			_format.format(xVal*idleBinSize+idleMinBinSize) +
			"% to " + _format.format((xVal+1)*idleBinSize+
			idleMinBinSize) + "%";
		}
		else
		{
			bubbleText[2] = "Bin: > " + 
			_format.format(idleNumBins*idleBinSize+idleMinBinSize) + "%";
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

        public void displayMustBeRedrawn() {
                calcDisplayData();
                setGraphSpecificData();
                refreshGraph();
        }

        public boolean entryIsVisibleID(Integer id) {
                return true;
        }

        public int[] getEntriesArray() {
                return null;
        }

        public void makeEntryInvisibleID(Integer id) {
                display_mask[id] = false;
        }

        public void makeEntryVisibleID(Integer id) {
                display_mask[id] = true;
        }

        public boolean hasEntryList() {
                return false;
        }
        
        public boolean handleIdleOverhead() {
        	return false;
        }
}
