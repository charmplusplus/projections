package projections.Tools.EntryMethodProfile;

import org.jfree.chart.title.TextTitle;
import projections.analysis.TimedProgressThreadExecutor;

import projections.gui.*;

import org.jfree.chart.*;
import org.jfree.chart.title.LegendTitle;
import org.jfree.ui.RectangleEdge;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class MethodProfileWindow extends ProjectionsWindow implements ActionListener
{
    private static int myRun = 0;

    private MethodProfileWindow thisWindow;
    private MainWindow mainWindow;

    private JMenuItem mClose;
    private JMenuItem mSaveScreenshot;

    private TreeMap<Long, double[]> LoadData;
    private TreeMap<Long, Integer> LastIndices;

    private long startTime;
    private long endTime;
    private int firstPE;
    private int lastPE;

    private JFreeChart chart;

    public MethodProfileWindow(MainWindow mainWindow) {
        super(mainWindow);
        thisWindow = this;
        this.mainWindow = mainWindow;

        LoadData = new TreeMap<Long, double[]>();
        LastIndices = new TreeMap<Long, Integer>();

        setForeground(Color.lightGray);
        setTitle("Entry Method Profile - " + MainWindow.runObject[myRun].getFilename() + ".sts");

        createMenus();
        pack();
        showDialog();
    }

    protected void createMenus() {
        JMenuBar mbar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");

        mClose = new JMenuItem("Close");
        mClose.addActionListener(this);
        fileMenu.add(mClose);

        mbar.add(fileMenu);

        // Screenshot Menu
        JMenu saveMenu = new JMenu("Save To Image");

        mSaveScreenshot = new JMenuItem("Save Profile Chart as JPG or PNG");
        mSaveScreenshot.addActionListener(this);
        saveMenu.add(mSaveScreenshot);

        mbar.add(saveMenu);

        this.setJMenuBar(mbar);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
//        super.actionPerformed(ae);
        Object c = ae.getSource();

        if(c == mClose) {
            this.close();
        } else if(c == mSaveScreenshot) {
            JPanelToImage.saveToFileChooserSelection(chart.createBufferedImage(1100, 700), "Save Profile Chart", "./EntryMethodProfile.png");
        }
    }

    public void setGraphSpecificData() {

    }

    public void showDialog() {
        try {
            if (dialog == null) {
                dialog = new RangeDialog(this, "Select Range", null, false);
            }
            dialog.displayDialog();
            if (!dialog.isCancelled()) {
                final SortedSet<Integer> pes = dialog.getSelectedProcessors();
                firstPE = pes.first();
                lastPE = pes.last();
                startTime = dialog.getStartTime();
                endTime = dialog.getEndTime();

                final SwingWorker worker = new SwingWorker() {
                    public Object doInBackground() {
                        // Load memory usages here
                        thisWindow.loadData(pes);
                        return null;
                    }

                    public void done() {
                        thisWindow.createPlot();
                    }
                };
                worker.execute();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createPlot() {
        setVisible(false);

        int lastIndex = -1;
        Iterator<Long> pes = LastIndices.keySet().iterator();
        while(pes.hasNext()) {
            Long pe = pes.next();
            int currIndex = LastIndices.get(pe);
            if(currIndex > lastIndex)
                lastIndex = currIndex;
        }

        double[] data = new double[lastIndex];
        pes = LoadData.keySet().iterator();
        while(pes.hasNext()) {
            Long pe = pes.next();
            double[] currData = LoadData.get(pe);
            int lastVal = Math.min(currData.length, lastIndex);
            for(int i = 0; i < lastVal; i++) {
                data[i] += currData[i];
            }
        }

        ArrayIndexComparator comparator = new ArrayIndexComparator(data);
        Integer[] indices = comparator.createIndexArray();
        Arrays.sort(indices, comparator);

        DefaultPieDataset dataset = new DefaultPieDataset( );
        for(int i = 0; i < lastIndex; i++) {
            int curr = indices[i];
            if(data[curr] == 0)
                break;
            dataset.setValue(MainWindow.runObject[myRun].getEntryNameByIndex(curr), data[curr]);
        }
        chart = ChartFactory.createPieChart(
                "Entry Method Profile",   // chart title
                dataset,                        // data
                false,                  // include legend
                true,
                false);

        PiePlot plot = (PiePlot)chart.getPlot();

        LegendItemCollection legendItemsOld = plot.getLegendItems();
        final LegendItemCollection legendItemsNew = new LegendItemCollection();
        for(int i = 0; i < Math.min(legendItemsOld.getItemCount(), 5); i++) {
            legendItemsNew.add(legendItemsOld.get(i));
        }
        LegendItemSource source = new LegendItemSource() {
            LegendItemCollection lic = new LegendItemCollection(); {
                lic.addAll(legendItemsNew);
            }
            public LegendItemCollection getLegendItems() {
                return lic;
            }
        };
        LegendTitle legendTitle = new LegendTitle(source);
        legendTitle.setPosition(RectangleEdge.BOTTOM);
        chart.addLegend(legendTitle);

        String titleText;
        if(lastPE == firstPE)
            titleText = "Processor: " + firstPE + "; ";
        else
            titleText = "Processor(s): " + firstPE + " - " + lastPE + "; ";
        titleText += "Time: " + U.humanReadableString(startTime) + " - " + U.humanReadableString(endTime);

        TextTitle choiceText = new TextTitle(titleText);
        choiceText.setPosition(RectangleEdge.TOP);
        chart.addSubtitle(choiceText);

        plot.setLabelGenerator(null);

        ChartPanel chartpanel = new ChartPanel(chart);
        chart.setBackgroundPaint(Color.white);

        chartpanel.setPreferredSize(new Dimension(1100,700));

        Container windowPane = thisWindow.getContentPane();
        windowPane.removeAll();
        windowPane.setLayout(new BorderLayout());
        windowPane.add(chartpanel, BorderLayout.CENTER);

        thisWindow.pack();
        thisWindow.setVisible(true);
    }

    private void loadData(final SortedSet<Integer> processorList) {
        if( MainWindow.runObject[myRun].hasLogFiles() || MainWindow.runObject[myRun].hasSumDetailFiles() ) {
            // Do parallel loading because we have full logs

            // Create a list of worker threads
            LinkedList<Runnable> readyReaders = new LinkedList<Runnable>();

            for(Integer nextPe : processorList){
                readyReaders.add( new ThreadedFileReader(nextPe, myRun, startTime, endTime));
            }

            // Determine a component to show the progress bar with
            Component guiRootForProgressBar = null;
            if(thisWindow!=null && thisWindow.isVisible()) {
                guiRootForProgressBar = thisWindow;
            } else if(mainWindow!=null && mainWindow.isVisible()){
                guiRootForProgressBar = mainWindow;
            } else if(MainWindow.runObject[myRun].guiRoot!=null && MainWindow.runObject[myRun].guiRoot.isVisible()){
                guiRootForProgressBar = MainWindow.runObject[myRun].guiRoot;
            }

            // Pass this list of threads to a class that manages/runs the threads nicely
            TimedProgressThreadExecutor threadManager = new TimedProgressThreadExecutor("Loading Entry Method Profile in Parallel", readyReaders, guiRootForProgressBar, true);
            threadManager.runAll();

            Iterator<Runnable> titer = readyReaders.iterator();
            while(titer.hasNext()){
                ThreadedFileReader r = (ThreadedFileReader) titer.next();
                LoadData.put(r.getPe(), r.getData());
                LastIndices.put(r.getPe(), r.getLastIndex());
            }
        }
    }
}