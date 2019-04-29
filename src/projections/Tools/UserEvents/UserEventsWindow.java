package projections.Tools.UserEvents;

import projections.analysis.TimedProgressThreadExecutor;
import projections.gui.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.SortedSet;

/**
 * UserEventsWindow
 * by Chee Wai Lee
 * <p>
 * Will replace The old GraphWindow class once a framework for displaying
 * Legends are in place (and probably replace the name)
 */
public class UserEventsWindow extends GenericGraphWindow
        implements ActionListener, EntryMethodVisibility {

    private UserEventsWindow thisWindow;

    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    private static int myRun = 0;

    private JPanel mainPanel;
    private JPanel controlPanel;

    private IntervalChooserPanel intervalPanel;

    // data used for intervalgraphdialog
    private int startInterval;
    private int endInterval;
    private long intervalSize;
    private SortedSet<Integer> processorList;

    // meta data variables
    private int numActivities;
    // Normally derived from MainWindow.runObject[myRun].java
    private String activityNames[];

    // stored raw data
    private double[][] graphData;
    private double[][] timeSpent;
    private double[][] callRate;
    private boolean[] display_mask;
    private GenericGraphColorer colorer;

    // buttons to switch between displaying "time spent" vs "call rate"
    private JRadioButton timeSpentButton;
    private JRadioButton callRateButton;

    private JRadioButton microseconds;
    private JRadioButton milliseconds;
    private JRadioButton seconds;

    private String yAxisLabel = "Time (us)";
    private String yAxisUnits = "us";

    private double unitTime = 1;
    private String unitTimeStr = "ms";

    private static DecimalFormat decimalFormatter = new DecimalFormat("###,###.###");
    private static DecimalFormat scientificFormatter = new DecimalFormat("0.###E0");

    public UserEventsWindow(MainWindow mainWindow) {
        super("Projections User Events Tool - " +
                MainWindow.runObject[myRun].getFilename() + ".sts", mainWindow);
        // hardcode start. Usually derived from MainWindow.runObject[myRun].java
        numActivities = MainWindow.runObject[myRun].getNumUserDefinedEvents();
        activityNames = MainWindow.runObject[myRun].getUserEventNames();
        display_mask = new boolean[numActivities];
        for (int i = 0; i < numActivities; i++)
            display_mask[i] = true;
        colorer = new GenericGraphColorer() {
            private final Paint[] colorMap = ColorManager.createColorMap(numActivities);

            @Override
            public Paint[] getColorMap() {
                return colorMap;
            }
        };

        createMenus();
        if (mChooseColors.getActionListeners()[0] != null)
            mChooseColors.removeActionListener(mChooseColors.getActionListeners()[0]);
        mChooseColors.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                new ChooseUserEntriesWindow(thisWindow, thisWindow, colorer, MainWindow.runObject[myRun].getSts().getUserEventNameMap(), "Event", activityNames);
            }
        });
        createLayout();
        pack();
        thisWindow = this;
        showDialog();
    }

    private void createLayout() {
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
        Util.gblAdd(mainPanel, graphPanel, gbc, 0, 0, 1, 1, 1, 1);
        Util.gblAdd(mainPanel, controlPanel, gbc, 0, 1, 1, 1, 0, 0);

        ButtonGroup display_bg = new ButtonGroup();
        timeSpentButton = new JRadioButton("Time Spent", true);
        callRateButton = new JRadioButton("Call Rate", false);

        timeSpentButton.addActionListener(this);
        callRateButton.addActionListener(this);

        display_bg.add(timeSpentButton);
        display_bg.add(callRateButton);

        JPanel displayTypePanel = new JPanel();
        displayTypePanel.setLayout(gbl);
        displayTypePanel.add(timeSpentButton);
        displayTypePanel.add(callRateButton);

        Util.gblAdd(controlPanel, displayTypePanel, gbc, 0, 0, 1, 1, 0, 0);

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

        JPanel unitPanel = new JPanel();
        unitPanel.setLayout(gbl);
        unitPanel.add(microseconds);
        unitPanel.add(milliseconds);
        unitPanel.add(seconds);

        Util.gblAdd(controlPanel, unitPanel, gbc, 0, 1, 1, 1, 0, 0);
    }

    public void showDialog() {
        if (dialog == null) {
            intervalPanel = new IntervalChooserPanel();
            dialog = new RangeDialog(this, "Select Range", intervalPanel, false);
        }

        dialog.displayDialog();
        if (!dialog.isCancelled()) {
            intervalSize = intervalPanel.getIntervalSize();
            startInterval = (int) intervalPanel.getStartInterval();
            endInterval = (int) intervalPanel.getEndInterval();
            processorList = dialog.getSelectedProcessors();

            final SwingWorker worker = new SwingWorker() {
                public Object doInBackground() {
                    constructToolData();
                    return null;
                }

                public void done() {
                    milliseconds.setSelected(true);
                    unitTime = 1;
                    unitTimeStr = "ms";
                    timeSpentButton.setSelected(true);

                    // GUI code after Long non-gui code (above) is done.
                    setGraphSpecificData();
                    thisWindow.setVisible(true);
                }
            };
            worker.execute();
        }
    }

    private void constructToolData() {
        int count = 0;
        timeSpent = new double[processorList.size()][];
        callRate = new double[processorList.size()][];

        List<Runnable> readyReaders = new ArrayList<>(processorList.size());

        for (Integer pe : processorList) {
            readyReaders.add(new ThreadedFileReader(pe, count, startInterval, endInterval, intervalSize, timeSpent, callRate));
            count++;
        }

        // Determine a component to show the progress bar with
        Component guiRootForProgressBar = null;
        if (thisWindow != null && thisWindow.isVisible()) {
            guiRootForProgressBar = thisWindow;
        } else if (MainWindow.runObject[myRun].guiRoot != null && MainWindow.runObject[myRun].guiRoot.isVisible()) {
            guiRootForProgressBar = MainWindow.runObject[myRun].guiRoot;
        }

        // Pass this list of threads to a class that manages/runs the threads nicely
        TimedProgressThreadExecutor threadManager = new TimedProgressThreadExecutor("Loading User Events Data in Parallel", readyReaders, guiRootForProgressBar, true);
        threadManager.runAll();

        for (int peIdx = 0; peIdx < processorList.size(); peIdx++) {
            for (int eventIdx = 0; eventIdx < numActivities; eventIdx++) {
                callRate[peIdx][eventIdx] /= intervalSize;
            }
        }

        graphData = timeSpent;
    }

    protected void setGraphSpecificData() {
        setXAxis("Processors", processorList);
        setYAxisLabelAndUnits();
        setYAxis(yAxisLabel, yAxisUnits);
        setDataSource("User Events", graphData, colorer, this, display_mask);
        refreshGraph();
    }

    public String[] getPopup(int xVal, int yVal) {
        if ((xVal < 0) || (yVal < 0)) {
            return null;
        }
        String[] rString = new String[3];

        rString[0] = "Name: " + activityNames[yVal];
        rString[1] = "Time Spent: " + U.humanReadableString((long) (timeSpent[xVal][yVal]));
        rString[2] = String.format("Rate: %s calls/%s (%s calls)",
                formatNumber(callRate[xVal][yVal]),
                unitTimeStr,
                formatNumber(callRate[xVal][yVal] * intervalSize / unitTime));
        return rString;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JMenuItem) {
            String arg = ((JMenuItem) e.getSource()).getText();
            if (arg.equals("Close")) {
                close();
            } else if (arg.equals("Set Range")) {
                showDialog();
            }
        } else if (e.getSource() == timeSpentButton) {
            graphData = timeSpent;
            setGraphSpecificData();
        } else if (e.getSource() == callRateButton) {
            graphData = callRate;
            setGraphSpecificData();
        } else if (e.getSource() == microseconds) {
            scaleHistogramData(0.001);
            setGraphSpecificData();
        } else if (e.getSource() == milliseconds) {
            scaleHistogramData(1.0);
            setGraphSpecificData();
        } else if (e.getSource() == seconds) {
            scaleHistogramData(1000.0);
            setGraphSpecificData();
        }
    }

    private void scaleHistogramData(double newUnit) {
        double scale = newUnit / unitTime;
        for (int peIdx = 0; peIdx < processorList.size(); peIdx++) {
            for (int eventIdx = 0; eventIdx < numActivities; eventIdx++) {
                callRate[peIdx][eventIdx] *= scale;
            }
        }
        unitTime = newUnit;
        if (unitTime == 0.001) unitTimeStr = "us";
        else if (unitTime == 1.0) unitTimeStr = "ms";
        else unitTimeStr = "s";
    }

    private void setYAxisLabelAndUnits() {
        if (timeSpentButton.isSelected()) {
            yAxisLabel = "Time (us)";
            yAxisUnits = "us";
        } else if (callRateButton.isSelected()) {
            yAxisLabel = "Calls/" + unitTimeStr;
            yAxisUnits = "";
        }
    }

    @Override
    public void displayMustBeRedrawn() {
        setGraphSpecificData();
        refreshGraph();
    }

    @Override
    public boolean entryIsVisibleID(Integer id) {
        return display_mask[id];
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

    private static String formatNumber(double number) {
        String formatted = decimalFormatter.format(number);
        if (formatted.equals("0"))
            formatted = scientificFormatter.format(number);
        return formatted;
    }
}
