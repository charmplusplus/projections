package projections.Tools.MessageSizeEvolution;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import projections.analysis.TimedProgressThreadExecutor;
import projections.gui.ChooseEntriesWindow;
import projections.gui.ColorUpdateNotifier;
import projections.gui.EntryMethodVisibility;
import projections.gui.ProjectionsWindow;
import projections.gui.MainWindow;
import projections.gui.RangeDialog;
import projections.gui.U;
import projections.gui.JPanelToImage;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.JOptionPane;
import javax.swing.JMenuItem;
import javax.swing.JMenu;

import java.awt.Component;
import java.awt.Color;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MessageSizeEvolutionWindow
        extends ProjectionsWindow
        implements ActionListener, EntryMethodVisibility, ColorUpdateNotifier {

    private int myRun = 0;

    //menu stuff
    private JMenuItem mClose;
    private JMenuItem mSaveScreenshot;

    private BinDialogPanel binpanel;

    private JFreeChart chart;
    private ChartPanel chartPanel;

    // epCounts is indexed by time bin, then msg bin, then entry method id.
    // The extra ep slot at the end holds events whose entry id is outside
    // the sts table; it is always displayed.
    private int[][][] epCounts;
    private int numEPs;
    private boolean[] epVisible;

    // What the chart shows: epCounts summed over the visible entry methods,
    // indexed by time bin then msg bin.
    private int[][] counts;

    private JButton selectEntriesButton;
    private JLabel entriesShownLabel;

    private int timeNumBins;
    private long timeBinSize;
    private long startTime;
    private int msgNumBins;
    private long msgBinSize;
    private long msgMinBinSize;
    private boolean msgLogScale;
    private boolean msgCreationEvent;

    private MessageSizeEvolutionWindow thisWindow;
    private final MainWindow mainWindow;

    private DecimalFormat _format;

    public MessageSizeEvolutionWindow(MainWindow mainWindow) {
        super(mainWindow);
        thisWindow = this;
        this.mainWindow = mainWindow;

        setTitle("Projections Message Size Evolution - " + MainWindow.runObject[myRun].getFilename() + ".sts");
        _format = new DecimalFormat();

        createMenus();
        pack();
        showDialog();
    }

    public void showDialog() {
        if (dialog == null) {
            binpanel = new BinDialogPanel();
            dialog = new RangeDialog(this, "Select Message Size Evolution Time Range", binpanel, false);
        }

        dialog.displayDialog();
        if (!dialog.isCancelled()) {
            final SwingWorker worker = new SwingWorker() {
                @Override
                protected Object doInBackground() throws Exception {
                    timeNumBins = binpanel.getTimeNumBins();
                    timeBinSize = binpanel.getTimeBinSize();
                    startTime = binpanel.getStartTime();
                    msgNumBins = binpanel.getMsgNumBins();
                    msgBinSize = binpanel.getMsgBinSize();
                    msgMinBinSize = binpanel.getMsgMinBinSize();
                    msgLogScale = binpanel.getMsgLogScale();
                    msgCreationEvent = binpanel.getMsgCreationEvent();
                    if (!msgLogScale && (timeBinSize == 0 || msgBinSize == 0)) {
                        //prevents dividing by zero
                        JOptionPane.showMessageDialog(null, "You cannot enter a bin size of zero.", "Error", JOptionPane.ERROR_MESSAGE);
                        System.out.println("You cannot enter a bin size of zero.");
                        return null;
                    } else if (msgLogScale && msgMinBinSize < 1) {
                        JOptionPane.showMessageDialog(null, "You cannot enter a starting bin size less than 1.", "Error", JOptionPane.ERROR_MESSAGE);
                        System.out.println("You cannot enter a starting bin size less than 1.");
                        return null;
                    }
                    numEPs = MainWindow.runObject[myRun].getNumUserEntries();
                    epVisible = new boolean[numEPs];
                    for (int ep = 0; ep < numEPs; ep++)
                        epVisible[ep] = true;
                    epCounts = new int[timeNumBins + 1][msgNumBins + 1][numEPs + 1];

                    // Create a list of worker threads
                    List<Runnable> readyReaders = new ArrayList<Runnable>(dialog.getSelectedProcessors().size());
                    for (Integer nextPe : dialog.getSelectedProcessors()) {
                        readyReaders.add(new ThreadedFileReader(epCounts, nextPe, startTime, dialog.getEndTime(), timeNumBins, timeBinSize, msgNumBins, msgBinSize, msgMinBinSize, msgLogScale, msgCreationEvent));
                    }

                    // Determine a component to show the progress bar with
                    Component guiRootForProgressBar = null;
                    if (thisWindow != null && thisWindow.isVisible())
                        guiRootForProgressBar = thisWindow;
                    else if (mainWindow != null && mainWindow.isVisible())
                        guiRootForProgressBar = mainWindow;
                    else if (MainWindow.runObject[myRun].guiRoot != null && MainWindow.runObject[myRun].guiRoot.isVisible())
                        guiRootForProgressBar = MainWindow.runObject[myRun].guiRoot;

                    // Pass this list of threads to a class that manages/runs the threads nicely
                    TimedProgressThreadExecutor threadManager = new TimedProgressThreadExecutor("Loading Message Size Evolution in Parallel", readyReaders, guiRootForProgressBar, true);
                    threadManager.runAll();

                    return null;
                }

                protected void done() {
                    // epCounts stays null when the bin sizes failed validation
                    if (epCounts != null)
                        createPlot();
                }
            };
            worker.execute();
        }
    }

    /** Sum the per entry method counts over the entry methods currently
     *  switched on, into the 2D array the chart is built from. The catch-all
     *  slot for events outside the sts table is always included. */
    private void aggregateVisibleCounts() {
        counts = new int[timeNumBins + 1][msgNumBins + 1];
        for (int i = 0; i < epCounts.length; i++) {
            for (int j = 0; j < epCounts[i].length; j++) {
                for (int ep = 0; ep <= numEPs; ep++) {
                    if (ep == numEPs || epVisible[ep])
                        counts[i][j] += epCounts[i][j][ep];
                }
            }
        }
    }

    private void createPlot() {
        thisWindow.setVisible(false);

        aggregateVisibleCounts();
        buildChart();

        Container windowPane = thisWindow.getContentPane();
        windowPane.removeAll();
        windowPane.setLayout(new BorderLayout());
        windowPane.add(chartPanel, BorderLayout.CENTER);

        selectEntriesButton = new JButton("Select Entry Methods");
        selectEntriesButton.addActionListener(this);
        entriesShownLabel = new JLabel();
        updateEntriesShownLabel();
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.add(selectEntriesButton);
        controlPanel.add(entriesShownLabel);
        windowPane.add(controlPanel, BorderLayout.SOUTH);

        thisWindow.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                fitChartToWindow();
            }
        });

        thisWindow.pack();
        thisWindow.setVisible(true);
    }

    /** Rebuild the chart in place after an entry method was switched on or
     *  off, leaving the window itself (and its size) alone. */
    private void redrawPlot() {
        aggregateVisibleCounts();
        Container windowPane = thisWindow.getContentPane();
        windowPane.remove(chartPanel);
        buildChart();
        windowPane.add(chartPanel, BorderLayout.CENTER);
        fitChartToWindow();
        updateEntriesShownLabel();
        windowPane.revalidate();
        windowPane.repaint();
    }

    /** The chart draws at the window's size rather than scaling a
     *  fixed-size rendering. */
    private void fitChartToWindow() {
        chartPanel.setMaximumDrawHeight(thisWindow.getHeight());
        chartPanel.setMaximumDrawWidth(thisWindow.getWidth());
        chartPanel.setMinimumDrawWidth(thisWindow.getWidth());
        chartPanel.setMinimumDrawHeight(thisWindow.getHeight());
    }

    private void updateEntriesShownLabel() {
        int present = 0, shown = 0;
        long[] totals = epMessageTotals();
        for (int ep = 0; ep < numEPs; ep++) {
            if (totals[ep] > 0) {
                present++;
                if (epVisible[ep])
                    shown++;
            }
        }
        entriesShownLabel.setText(shown == present ?
                "(all " + present + " entry methods with messages shown)" :
                "(" + shown + " of " + present + " entry methods with messages shown)");
    }

    /** Messages counted for each entry method over the whole loaded range. */
    private long[] epMessageTotals() {
        long[] totals = new long[numEPs];
        for (int i = 0; i < epCounts.length; i++)
            for (int j = 0; j < epCounts[i].length; j++)
                for (int ep = 0; ep < numEPs; ep++)
                    totals[ep] += epCounts[i][j][ep];
        return totals;
    }

    private void buildChart() {
        int[][] heatMap = new int[counts.length][];
        double maxVal = Double.MIN_VALUE, minVal = Double.MAX_VALUE;
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < counts.length; i++) {
            heatMap[i] = new int[counts[i].length];
            for (int j = counts[i].length - 1; j >= 0; j--) {
                int sum = counts[i][j];

                if (sum > maxVal)
                    maxVal = sum;
                if (sum < minVal)
                    minVal = sum;
                String msgKey;
                if (j == counts[i].length - 1) {
                    if (msgLogScale)
                        msgKey = " >= " + _format.format(msgMinBinSize * Math.pow(2, j));
                    else
                        msgKey = " >= " + _format.format((j * msgBinSize) + msgMinBinSize);
                } else {
                    if (msgLogScale)
                        msgKey = _format.format(msgMinBinSize * Math.pow(2, j)) + " - " + _format.format(msgMinBinSize * Math.pow(2, j + 1) - 1);
                    else
                        msgKey = _format.format((j * msgBinSize) + msgMinBinSize) + " - " + _format.format(((j + 1) * msgBinSize) + msgMinBinSize - 1);
                }
                dataset.addValue(timeBinSize,
                        U.humanReadableString((i * timeBinSize) + startTime),
                        msgKey
                );
                heatMap[i][counts[i].length - 1 - j] = sum;
            }
        }

        chart = ChartFactory.createStackedBarChart(
                "Message Size Evolution Chart",
                "Message Size",
                "Time",
                dataset,
                PlotOrientation.HORIZONTAL,
                false,
                true,
                false
        );

        CategoryPlot plot = chart.getCategoryPlot();
        StackedRenderer renderer = new StackedRenderer(heatMap, maxVal);
        renderer.setDefaultToolTipGenerator(new CustomToolTipGenerator(heatMap, timeNumBins, timeBinSize, startTime, msgNumBins, msgBinSize, msgMinBinSize, msgLogScale));
        plot.setRenderer(renderer);

        CustomRangeAxis rangeAxis = new CustomRangeAxis(startTime);
        plot.setRangeAxis(rangeAxis);
        plot.setBackgroundPaint(Color.WHITE);

        chartPanel = new ChartPanel(chart);
        chart.setBackgroundPaint(Color.LIGHT_GRAY);
    }

    protected void createMenus() {
        JMenuBar mbar = new JMenuBar();

        //File Menu
        JMenu fileMenu = new JMenu("File");

        mClose = new JMenuItem("Close");
        mClose.addActionListener(this);
        fileMenu.add(mClose);

        mbar.add(fileMenu);

        //Screenshot Menu
        JMenu saveMenu = new JMenu("Save To Image");

        mSaveScreenshot = new JMenuItem("Save Profile Chart");
        mSaveScreenshot.addActionListener(this);
        saveMenu.add(mSaveScreenshot);

        mbar.add(saveMenu);

        this.setJMenuBar(mbar);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        Object c = ae.getSource();
        if (c == mClose)
            this.close();
        else if (c == mSaveScreenshot && chartPanel != null)
            JPanelToImage.saveToFileChooserSelection(chart, chartPanel.getWidth(), chartPanel.getHeight(),
                    "Save Evolution Chart", "./MessageSizeEvolution.pdf");
        else if (c == selectEntriesButton)
            new ChooseEntriesWindow(this, true, this);
    }

    // ---- EntryMethodVisibility: the shared entry method chooser drives
    // ---- which entry methods' messages the chart is built from.

    /** Messages counted per entry method, so the chooser lists the busiest
     *  first with the counts in a column of their own. */
    public int[] getEntriesArray() {
        long[] totals = epMessageTotals();
        int[] entries = new int[numEPs];
        for (int ep = 0; ep < numEPs; ep++)
            entries[ep] = (totals[ep] >= Integer.MAX_VALUE) ?
                    Integer.MAX_VALUE : (int) totals[ep];
        return entries;
    }

    public boolean sortEntriesByCount() {
        return true;
    }

    public boolean hasEntryList() {
        return true;
    }

    /** Idle and overhead are times, not messages, so they have no place here. */
    public boolean handleIdleOverhead() {
        return false;
    }

    public boolean entryIsVisibleID(Integer id) {
        return !inRange(id) || epVisible[id];
    }

    public void makeEntryVisibleID(Integer id) {
        if (inRange(id))
            epVisible[id] = true;
    }

    public void makeEntryInvisibleID(Integer id) {
        if (inRange(id))
            epVisible[id] = false;
    }

    /** Idle and overhead come through as negative ids from the shared dialog;
     *  this tool does not show them. */
    private boolean inRange(Integer id) {
        return id != null && id >= 0 && id < numEPs;
    }

    public void displayMustBeRedrawn() {
        if (epCounts != null && chartPanel != null)
            redrawPlot();
    }

    /** The chart is a heat map, not colored by entry method, so a color
     *  change from the chooser has nothing to repaint here. */
    public void colorsHaveChanged() {
    }
}
