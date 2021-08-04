package projections.Tools.MessageSizeEvolution;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import projections.analysis.TimedProgressThreadExecutor;
import projections.gui.ProjectionsWindow;
import projections.gui.MainWindow;
import projections.gui.RangeDialog;
import projections.gui.U;
import projections.gui.JPanelToImage;

import javax.swing.JMenuBar;
import javax.swing.SwingWorker;
import javax.swing.JOptionPane;
import javax.swing.JMenuItem;
import javax.swing.JMenu;

import java.awt.Component;
import java.awt.Color;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MessageSizeEvolutionWindow
        extends ProjectionsWindow
        implements ActionListener {

    private int myRun = 0;

    //menu stuff
    private JMenuItem mClose;
    private JMenuItem mSaveScreenshot;

    private BinDialogPanel binpanel;

    private JFreeChart chart;

    // counts is indexed by msg bin index, then time bin index followed by ep id.
    // NOTE: bin indices need not be of the same size
    private int[][] counts;

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
                    counts = new int[timeNumBins + 1][msgNumBins + 1];

                    // Create a list of worker threads
                    List<Runnable> readyReaders = new ArrayList<Runnable>(dialog.getSelectedProcessors().size());
                    for (Integer nextPe : dialog.getSelectedProcessors()) {
                        readyReaders.add(new ThreadedFileReader(counts, nextPe, startTime, dialog.getEndTime(), timeNumBins, timeBinSize, msgNumBins, msgBinSize, msgMinBinSize, msgLogScale, msgCreationEvent));
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
                    createPlot();
                }
            };
            worker.execute();
        }
    }

    private void createPlot() {
        thisWindow.setVisible(false);

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

        final ChartPanel chartPanel = new ChartPanel(chart);
        chart.setBackgroundPaint(Color.LIGHT_GRAY);

        Container windowPane = thisWindow.getContentPane();
        windowPane.removeAll();
        windowPane.setLayout(new BorderLayout());
        windowPane.add(chartPanel, BorderLayout.CENTER);

        thisWindow.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                chartPanel.setMaximumDrawHeight(e.getComponent().getHeight());
                chartPanel.setMaximumDrawWidth(e.getComponent().getWidth());
                chartPanel.setMinimumDrawWidth(e.getComponent().getWidth());
                chartPanel.setMinimumDrawHeight(e.getComponent().getHeight());
            }
        });

        thisWindow.pack();
        thisWindow.setVisible(true);
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
        else if (c == mSaveScreenshot)
            // TODO: Fix rendering to vector formats, it currently seems to rasterize them
            JPanelToImage.saveToFileChooserSelection(thisWindow.getContentPane(), "Save Evolution Chart", "./MessageSizeEvolution.png");
    }
}
