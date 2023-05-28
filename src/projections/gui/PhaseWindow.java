package projections.gui;

import projections.Tools.TimeProfile.ThreadedFileReader;
import projections.analysis.PhaseHistory;
import projections.analysis.Pair;
import projections.analysis.TimedProgressThreadExecutor;
import projections.analysis.ProjMain;
import projections.analysis.LogReader;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.JLabel;
import javax.swing.SwingWorker;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.GridLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import java.io.File;
import java.io.IOException;

import java.util.SortedSet;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PhaseWindow
        extends GenericGraphWindow
        implements ActionListener, Clickable {

    private final static int special = 2;
    private static int myRun = 0;

    private MainWindow mainWindow;
    private PhaseWindow thisWindow;
    private PhaseHistory history;
    private List<Pair> historyList;
    private String processorListString;

    private JPanel mainPanel;
    private JButton removeButton, addButton, saveButton;
    private JCheckBox showMarkersCheckBox;
    private JCheckBox analyzeSlopesCheckBox;
    private JCheckBox hideMouseoversCheckBox;
    private IntervalChooserPanel intervalPanel;
    private TimeTextField startTimeField;
    private TimeTextField endTimeField;
    private JTextField nameField;

    private JList phaseList;
    private DefaultListModel listModel;
    private boolean phaseListEmpty;
    private int phaseListIndex;

    private int numEPs;
    private boolean[] stateArray;

    private long intervalSize;
    private int startInterval;
    private int endInterval;
    private long startTime;
    private double[][] graphData;
    private double[][] outputData;
    private SortedSet<Integer> processorList;
    private TreeMap<Double, String> phaseMarkers = new TreeMap<Double, String>();

    private boolean startFlag;
    private boolean displaySlopes;

    PhaseWindow(MainWindow mainWindow, int index) {
        super("Projections Phase Window - " + MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
        this.mainWindow = mainWindow;
        this.phaseListIndex = index;

        numEPs = MainWindow.runObject[myRun].getNumUserEntries();
        stateArray = new boolean[numEPs + special];
        historyList = new ArrayList<Pair>();
        history = new PhaseHistory(MainWindow.runObject[myRun].getLogDirectory() + File.separator);
        if (phaseListIndex != -1) {
            for (int i = 0; i < history.getNumPhases(phaseListIndex); i++) {
                Pair curr = new Pair(history.getStartOfPhase(phaseListIndex, i), history.getEndOfPhase(phaseListIndex, i));
                historyList.add(curr);
            }
        }

        mainPanel = new JPanel();
        getContentPane().add(mainPanel);

        createLayout();
        pack();
        thisWindow = this;
        startFlag = true;
        displaySlopes = false;
        thisWindow.setLocationRelativeTo(null);
        showDialog();
    }

    @SuppressWarnings("unchecked")
    private void createLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout gbl = new GridBagLayout();

        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(2, 2, 2, 2);
        mainPanel.setLayout(gbl);

        // control panel items
        showMarkersCheckBox = new JCheckBox("Show Iteration/Phase Markers");
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

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(gbl);
        controlPanel.setBorder(new LineBorder(Color.black));
        Util.gblAdd(controlPanel, showMarkersCheckBox, gbc, 0, 0, 1, 1, 0, 0);
        Util.gblAdd(controlPanel, analyzeSlopesCheckBox, gbc, 1, 0, 1, 1, 0, 0);
        Util.gblAdd(controlPanel, hideMouseoversCheckBox, gbc, 2, 0, 1, 1, 0, 0);

        listModel = new DefaultListModel();
        if (phaseListIndex == -1) {
            phaseListEmpty = true;
            listModel.addElement("");
        } else {
            phaseListEmpty = false;
            for (int i = 0; i < history.getNumPhases(phaseListIndex); i++)
                listModel.addElement(history.getPhaseString(phaseListIndex, i));
        }

        phaseList = new JList(listModel);
        phaseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        phaseList.setBorder(new TitledBorder(new LineBorder(Color.black), "Phase List"));
        removeButton = new JButton("Remove Selected Phase");
        removeButton.addActionListener(this);
        removeButton.setHorizontalAlignment(JButton.CENTER);

        JPanel removePhasePanel = new JPanel();
        removePhasePanel.setLayout(gbl);
        Util.gblAdd(removePhasePanel, removeButton, gbc, 0, 0, 1, 1, 0, 0);

        JLabel startTimeLabel = new JLabel("Start Time of New Phase:");
        startTimeField = new TimeTextField(" ", 12);
        JLabel endTimeLabel = new JLabel("End Time of New Phase:");
        endTimeField = new TimeTextField(" ", 12);
        addButton = new JButton("Add New Phase");
        addButton.addActionListener(this);

        JPanel addPhasePanel = new JPanel();
        addPhasePanel.setLayout(gbl);
        addPhasePanel.setBorder(new TitledBorder(new LineBorder(Color.black), "New Phase Info"));
        Util.gblAdd(addPhasePanel, startTimeLabel, gbc, 0, 0, 1, 1, 1, 1);
        Util.gblAdd(addPhasePanel, startTimeField, gbc, 1, 0, 1, 1, 1, 1);
        Util.gblAdd(addPhasePanel, endTimeLabel, gbc, 2, 0, 1, 1, 1, 1);
        Util.gblAdd(addPhasePanel, endTimeField, gbc, 3, 0, 1, 1, 1, 1);
        Util.gblAdd(addPhasePanel, addButton, gbc, 4, 0, 1, 1, 0, 0);

        JLabel nameLabel = new JLabel("Name of Phase Config:");
        nameField = new JTextField("", 12);
        if (phaseListIndex != -1) {
            nameLabel.setText("Rename Phase Config");
            nameField.setText(history.getPhaseConfigName(phaseListIndex));
        }
        saveButton = new JButton("Save Phase Config To Disk");
        saveButton.addActionListener(this);

        JPanel savePhasePanel = new JPanel();
        savePhasePanel.setLayout(gbl);
        savePhasePanel.setBorder(new LineBorder(Color.black));
        Util.gblAdd(savePhasePanel, nameLabel, gbc, 0, 1, 1, 1, 1, 1);
        Util.gblAdd(savePhasePanel, nameField, gbc, 1, 1, 1, 1, 1, 1);
        Util.gblAdd(savePhasePanel, saveButton, gbc, 2, 1, 1, 1, 0, 0);

        JPanel graphPanel = getMainPanel();
        Util.gblAdd(mainPanel, graphPanel, gbc, 0, 0, 1, 1, 1, 1);
        Util.gblAdd(mainPanel, controlPanel, gbc, 0, 1, 1, 1, 0, 0);
        Util.gblAdd(mainPanel, phaseList, gbc, 0, 2, 1, 1, 1, 0);
        Util.gblAdd(mainPanel, removePhasePanel, gbc, 0, 3, 1, 1, 1, 0);
        Util.gblAdd(mainPanel, addPhasePanel, gbc, 0, 4, 1, 1, 1, 0);
        Util.gblAdd(mainPanel, savePhasePanel, gbc, 0, 5, 1, 1, 1, 0);
    }

    public void setGraphSpecificData() {

    }

    public void showDialog() {
        if (dialog == null) {
            intervalPanel = new IntervalChooserPanel();
            dialog = new RangeDialog(this, "Select Range for Phase Chooser", intervalPanel, true);
        }
        dialog.displayDialog();
        if (!dialog.isCancelled()) {
            startInterval = (int) intervalPanel.getStartInterval();
            endInterval = (int) intervalPanel.getEndInterval();
            processorList = dialog.getSelectedProcessors();
            processorListString = Util.listToString(processorList);
            startTime = dialog.getStartTime();
            if (MainWindow.runObject[myRun].hasLogFiles()) {
                intervalSize = intervalPanel.getIntervalSize();
            } else {
                startInterval = 0;
                endInterval = (int) MainWindow.runObject[myRun].getSumDetailNumIntervals() - 1;
                intervalSize = (long) MainWindow.runObject[myRun].getSumDetailIntervalSize();
            }

            final SwingWorker worker = new SwingWorker() {
                public Object doInBackground() {
                    phaseMarkers.clear();

                    int numIntervals = endInterval - startInterval + 1;
                    graphData = new double[numIntervals][numEPs + special]; //entry number + idle

                    int numProcessors = processorList.size();
                    int numUserEntries = MainWindow.runObject[myRun].getNumUserEntries();

                    if (MainWindow.runObject[myRun].hasLogFiles()) {
                        // Do parallel loading because we have full logs

                        // Create a list of worker threads
                        LinkedList<Runnable> readyReaders = new LinkedList<Runnable>();

                        // Create multiple result arrays to reduce contention for accumulating
                        int numResultAccumulators = 8;
                        double[][][] graphDataAccumulators = new double[numResultAccumulators][numIntervals][numEPs + special];

                        int pIdx = 0;
                        for (Integer pe : processorList) {
                            readyReaders.add(new ThreadedFileReader(pe, intervalSize, myRun,
                                    startInterval, endInterval, phaseMarkers,
                                    graphDataAccumulators[pIdx % numResultAccumulators]));
                            pIdx++;
                        }

                        // Determine a component to show the progress bar with
                        Component guiRootForProgressBar = null;
                        if (thisWindow != null && thisWindow.isVisible()) {
                            guiRootForProgressBar = thisWindow;
                        } else if (mainWindow != null && mainWindow.isVisible()) {
                            guiRootForProgressBar = mainWindow;
                        } else if (MainWindow.runObject[myRun].guiRoot != null && MainWindow.runObject[myRun].guiRoot.isVisible()) {
                            guiRootForProgressBar = MainWindow.runObject[myRun].guiRoot;
                        }

                        // Pass this list of threads to a class that manages/runs the threads nicely
                        TimedProgressThreadExecutor threadManager = new TimedProgressThreadExecutor("Loading Time Profile in Parallel", readyReaders, guiRootForProgressBar, true);
                        threadManager.runAll();

                        // Merge resulting graphData structures together.
                        for (int a = 0; a < numResultAccumulators; a++) {
                            for (int i = 0; i < numIntervals; i++) {
                                for (int j = 0; j < numEPs + special; j++) {
                                    graphData[i][j] += graphDataAccumulators[a][i][j];
                                }
                            }
                        }
                    } else if (MainWindow.runObject[myRun].hasSumDetailFiles()) {
                        // Do serial file reading because all we have is the sum files
                        SortedSet<Integer> availablePEs =
                                MainWindow.runObject[myRun].getValidProcessorList(ProjMain.SUMDETAIL);
                        MainWindow.runObject[myRun].LoadGraphData(intervalSize, 0, numIntervals - 1, false, availablePEs);
                        int[][] sumDetailData = MainWindow.runObject[myRun].getSumDetailData();

                        for (int i = 0; i < numIntervals; i++) {
                            for (int j = 0; j < numEPs; j++) {
                                graphData[i][j] += sumDetailData[i][j];
                            }
                        }
                    } else if (MainWindow.runObject[myRun].hasSumFiles()) {
                        // Do serial file reading because all we have is the sum files

                        // The data we use is
                        // 	systemUsageData[2][*][*]
                        // userEntryData[*][LogReader.TIME][*][*]

                        int[][][] systemUsageData = new int[3][][];
                        systemUsageData[2] = new int[numProcessors][];

                        int[][][][] userEntryData = new int[numUserEntries][][][];
                        for (int n = 0; n < numUserEntries; n++) {
                            userEntryData[n] = new int[3][][];
                            userEntryData[n][LogReader.TIME] = new int[numProcessors][];
                        }

                        int[][] temp = MainWindow.runObject[myRun].sumAnalyzer.getSystemUsageData(startInterval, endInterval, intervalSize);
                        systemUsageData[1] = new int[processorList.size()][endInterval - startInterval + 1];

                        int pIdx = 0;
                        for (Integer pe : processorList) {
                            systemUsageData[1][pIdx] = temp[pe];
                            pIdx++;
                        }

                        // Extract data and put it into the graph
                        for (int peIdx = 0; peIdx < numProcessors; peIdx++) {
                            for (int ep = 0; ep < numEPs; ep++) {
                                int[][] entryData = userEntryData[ep][LogReader.TIME];
                                for (int interval = 0; interval < numIntervals; interval++) {
                                    graphData[interval][ep] += entryData[peIdx][interval];
                                    graphData[interval][numEPs] -= entryData[peIdx][interval]; // overhead = -work time
                                }
                            }

                            //YS add for idle time SYS_IDLE=2
                            int[][] idleData = systemUsageData[2]; //percent
                            for (int interval = 0; interval < numIntervals; interval++) {
                                if (idleData[peIdx] != null && idleData[peIdx].length > interval) {
                                    graphData[interval][numEPs + 1] += idleData[peIdx][interval] * 0.01 * intervalSize;
                                    graphData[interval][numEPs] -= idleData[peIdx][interval] * 0.01 * intervalSize; //overhead = - idle time
                                    graphData[interval][numEPs] += intervalSize;
                                }
                            }
                        }
                    }//end of summary

                    // Scale raw data into percents
                    for (int interval = 0; interval < graphData.length; interval++) {
                        for (int e = 0; e < graphData[interval].length; e++) {
                            graphData[interval][e] = graphData[interval][e] * 100.0 / ((double) intervalSize * (double) numProcessors);
                        }
                    }
                    //Bilge
                    if (MainWindow.runObject[myRun].hasSumDetailFiles()) {
                        //idle time calculation for sum detail
                        int[] idlePercentage = MainWindow.runObject[myRun].sumAnalyzer.getTotalIdlePercentage();
                        for (int i = 0; i < numIntervals; i++) {
                            graphData[i][numEPs + 1] = idlePercentage[i];
                        }
                        //overhead time calculation for sum detail
                        for (int i = 0; i < numIntervals; i++) {
                            graphData[i][numEPs] = 100;
                            for (int j = 0; j < numEPs; j++) {
                                graphData[i][numEPs] -= graphData[i][j];
                            }
                            graphData[i][numEPs] -= graphData[i][numEPs + 1];
                        }

                    }

                    // Filter Out any bad data
                    for (int interval = 0; interval < graphData.length; interval++) {
                        boolean valid = true;
                        double sumForInterval = 0.0;
                        for (int e = 0; e < graphData[interval].length; e++) {
                            sumForInterval += graphData[interval][e];
                            if (graphData[interval][e] < 0.0) {
                                valid = false;
                            }
                        }
                        if (sumForInterval > 105.0) {
                            valid = false;
                        }

                        if (!valid) {
                            System.err.println("Time Profile found bad data for interval " + interval + ". The data for bad intervals will be zero-ed out. This problem is either a log file corruption issue, or a bug in Projections.");
                            for (int e = 0; e < graphData[interval].length; e++) {
                                graphData[interval][e] = 0.0;
                            }
                        }

                    }
                    // set the exists array to accept non-zero
                    // entries only have initial state also
                    // display all existing data. Only do this
                    // once in the beginning
                    if (startFlag) {
                        for (int ep = 0; ep < numEPs + special; ep++) {
                            for (int interval = 0; interval < endInterval - startInterval + 1; interval++) {
                                if (graphData[interval][ep] > 0) {
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
        for (int ep = 0; ep < numEPs + special; ep++) {
            if (stateArray[ep]) {
                outSize++;
            }
        }
        if (outSize > 0) {
            // actually create and fill the data and color array
            int numIntervals = endInterval - startInterval + 1;
            outputData = new double[numIntervals][outSize];
            for (int i = 0; i < numIntervals; i++) {
                int count = 0;
                for (int ep = 0; ep < numEPs + special; ep++) {
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

        // find the ep corresponding to the yVal
        int count = 0;
        String epName = "";
        String epClassName = "";
        for (int ep = 0; ep < numEPs; ep++) {
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
                U.humanReadableString((xVal + startInterval) * intervalSize) + " to " +
                U.humanReadableString((xVal + startInterval + 1) * intervalSize);
        rString[1] = "Chare Name: " + epClassName;
        rString[2] = "Entry Method: " + epName;
        rString[3] = "Execution Time = " + U.humanReadableString((long) (outputData[xVal][yVal]));
        //deal with idle and overhead time
        if (yVal == outputData[xVal].length - 2) {
            rString[1] = "";
            rString[2] = "Overhead";
            rString[3] = "Time = " + U.humanReadableString((long) (outputData[xVal][yVal]));
        } else if (yVal == outputData[xVal].length - 1) {
            rString[1] = "";
            rString[2] = "Idle time";
            rString[3] = "Time = " + U.humanReadableString((long) (outputData[xVal][yVal]));
        }
        return rString;
    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == analyzeSlopesCheckBox) {
            if (analyzeSlopesCheckBox.isSelected()) {
                displaySlopes = true;
                graphCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            } else {
                displaySlopes = false;
                graphCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                graphCanvas.clearPolynomial();
            }
        } else if (e.getSource() == showMarkersCheckBox) {
            graphCanvas.showMarkers(showMarkersCheckBox.isSelected());
        } else if (e.getSource() == hideMouseoversCheckBox) {
            graphCanvas.showBubble(!hideMouseoversCheckBox.isSelected());
        } else if (e.getSource() instanceof JMenuItem) {
            String arg = ((JMenuItem) e.getSource()).getText();
            if (arg.equals("Close")) {
                close();
            } else if (arg.equals("Set Range")) {
                showDialog();
            }
        } else if (e.getSource() == removeButton) {
            if (phaseListEmpty) {
                JOptionPane.showMessageDialog(this,
                        "Need at least one phase to be removed.",
                        "Empty Phase List",
                        JOptionPane.ERROR_MESSAGE);
            } else if (phaseList.isSelectionEmpty()) {
                JOptionPane.showMessageDialog(this, "Select a Phase from the list to be removed.");
            } else {
                historyList.remove(phaseList.getSelectedIndex());
                listModel.removeElementAt(phaseList.getSelectedIndex());
                if (phaseList.getModel().getSize() == 0) {
                    listModel.addElement("");
                    phaseListEmpty = true;
                }
            }
        } else if (e.getSource() == addButton) {
            long startTime = startTimeField.getValue(), endTime = endTimeField.getValue();
            if (startTime >= endTime) {
                JOptionPane.showMessageDialog(this, "Start Time must be less than End Time.");
            } else {
                Pair curr = new Pair(startTime, endTime);
                historyList.add(curr);
                if (phaseListEmpty) {
                    listModel.removeAllElements();
                    phaseListEmpty = false;
                }
                StringBuilder phaseString = new StringBuilder();
                phaseString
                        .append(U.humanReadableString(curr.getStart()))
                        .append(" to ")
                        .append(U.humanReadableString(curr.getEnd()));
                if (processorListString.length() > 10)
                    phaseString
                            .append(" Procs:")
                            .append(processorListString.substring(0, 10))
                            .append("...");
                else
                    phaseString.append(" Procs:").append(processorListString);

                listModel.addElement(phaseString.toString());
            }
        } else if (e.getSource() == saveButton) {
            if (phaseListIndex == -1 && nameField.getText().equals("")) {
                JOptionPane.showMessageDialog(this, "Must specify the name of the Phase Config.");
            } else {
                if (phaseListIndex == -1)
                    history.add(historyList, nameField.getText(), processorListString);
                else {
                    if (nameField.getText().equals(""))
                        history.update(phaseListIndex, historyList, null, processorListString);
                    else
                        history.update(phaseListIndex, historyList, nameField.getText(), processorListString);
                }
                try {
                    history.save();
                    if (phaseListIndex == -1) {
                        historyList.clear();
                        listModel.removeAllElements();
                        listModel.addElement("");
                        phaseListEmpty = true;
                    } else
                        super.close();
                } catch (IOException o) {
                    System.out.println("Error saving history to disk: " + o.toString());
                }
            }
        }
    }

    private void createPolynomial(int xVal, int yVal) {
        int numIntervals = endInterval - startInterval + 1;

        // We approximate the derivatives by using values from xVal - 2 to xVal + 2
        if (xVal < 2 || yVal < 0 || xVal >= numIntervals - 2) {
            return;
        }

        // extract the curve that sits:
        // above the EP utilization + overhead
        // but below the idle time
        double[] nonIdle = new double[numIntervals];
        for (int i = 0; i < numIntervals; i++) {
            nonIdle[i] = 0.0;
            for (int ep = 0; ep < numEPs + 1; ep++) {
                nonIdle[i] += graphData[i][ep];
            }
        }

        // Lookup the y value on this curve for where the user clicked
        double y = nonIdle[xVal];
        double slopeA = (nonIdle[xVal + 1] - nonIdle[xVal - 1]) / 2.0;
        double slopeB = (nonIdle[xVal + 2] - nonIdle[xVal - 2]) / 4.0;
        double slopeC = (slopeA + slopeB) / 2.0;

        // And to get some y=mx+b style coefficients, we need to do a little math:
        double[] coefficients = new double[2];
        coefficients[0] = -1.0 * slopeC * xVal + y; // y intercept of line
        coefficients[1] = slopeC;

        graphCanvas.addPolynomial(coefficients);
    }

    public void toolMouseMovedResponse(MouseEvent e, int xVal, int yVal) {
        if (displaySlopes)
            createPolynomial(xVal, yVal);
    }

    public void toolClickResponse(MouseEvent e, int xVal, int yVal) {
        if (displaySlopes)
            JPanelToImage.saveToFileChooserSelection(graphCanvas, "Save Screenshot Image", "./TimeProfileScreenshot.png");
    }
}
