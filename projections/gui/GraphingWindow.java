// UNUSED FILE

//package projections.gui;
//
//import projections.analysis.*;
//import projections.misc.*;
//import projections.gui.graph.*;
//
//import java.awt.event.*;
//import javax.swing.*;
//
///**
// *  GraphingWindow (lousy name, to be changed)
// *  written by Chee Wai Lee
// *  2/21/2003
// *
// *  This tool will eventually replace GraphWindow and ProfileWindow.
// *
// */
//public class GraphingWindow
//    extends GenericGraphWindow
//{
//    private MainWindow mainWindow;
//
//    private double data[][];
//    protected IntervalRangeDialog dialog;
//
//    public GraphingWindow(MainWindow mainWindow, Integer myWindowID) {
//	super("Graph", mainWindow, myWindowID);
//	this.mainWindow = mainWindow;
//	showDialog();
//    }
//
//    protected void showDialog() {
//	if (dialog == null) {
//	    dialog = new IntervalRangeDialog(this, "Select Range");
//	}
//	int result = dialog.showDialog();
//	if (result == RangeDialog.DIALOG_OK) {
//	    dialog.setAllData();
//	    createLayout();
//	} else {
//	    close();
//	}
//    }
//
//    protected void setGraphSpecificData() {
//	setStackGraph(false);
//	setXAxis("Intervals", "");
//	setDataSource("Time spent by EP", data);
//	dataSource.setColors(Analysis.getColorMap());
//	setYAxis("Time", "ms");
//	System.out.println(Analysis.getColorMap().length);
//	System.out.println(data.length);
//	System.out.println(data[0].length);
//    }
//
//    private void createLayout() {
//	getContentPane().add(getMainPanel());
//	setupData();
//	// graph view (particularly the X axis fields) depend on data.
//	setGraphSpecificData();
//	// System.out.println("Data set by dialog");
//	pack();
//	setVisible(true);
//	refreshGraph();
//    }
//
//    /**
//     *  Grabs the appropriate data (reading the logs if necessary).
//     */
//    private void setupData() {
//	// right now, will only work with sumd files.
//	if (Analysis.hasSumDetailData()) {
//	    int numPEs = Analysis.getNumProcessors();
//	    int numEPs = Analysis.getNumUserEntries();
//	    int maxIntervals = 
//		Analysis.getNumIntervals();
//	    double intervalSize = Analysis.getIntervalSize();
//	    int startInterval =
//		(int)(startTime/(intervalSize*1000000));
//	    int endInterval =
//		(int)(endTime/(intervalSize*1000000));
//	    System.out.println(intervalSize);
//	    System.out.println(startTime + " " + endTime);
//	    System.out.println(startInterval + " " + endInterval);
//	    // unfortunately, the dimensions used in data construction is
//	    // the inverse of the dimensions used for graph display!!!
//	    double tempData[][];
//	    tempData = 
//		Analysis.getDataSummedAcrossProcessors(SumDetailReader.TOTAL_TIME,
//						       validPEs,
//						       startInterval, 
//						       endInterval);
//	    data = new double[endInterval-startInterval+1][numEPs];
//	    for (int ep=0; ep<tempData.length; ep++) {
//		for (int interval=0;interval<tempData[ep].length;interval++) {
//		    data[interval][ep] = tempData[ep][interval];
//		}
//	    }
//	}
//    }
//}
//
