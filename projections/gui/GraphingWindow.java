package projections.gui;

import projections.analysis.*;
import projections.misc.*;
import projections.gui.graph.*;

import java.awt.event.*;
import javax.swing.*;

/**
 *  GraphingWindow (lousy name, to be changed)
 *  written by Chee Wai Lee
 *  2/21/2003
 *
 *  This tool will eventually replace GraphWindow and ProfileWindow.
 *
 */
public class GraphingWindow
    extends GenericGraphWindow
{
    private MainWindow mainWindow;

    private double data[][];

    public GraphingWindow(MainWindow mainWindow) {
	super("Graph");
	this.mainWindow = mainWindow;
	showDialog();
    }

    protected void showDialog() {
	if (dialog == null) {
	    dialog = new RangeDialog(this, "Select Range");
	}
	int result = dialog.showDialog();
	if (result == RangeDialog.DIALOG_OK) {
	    dialog.setAllData();
	    createLayout();
	} else {
	    close();
	}
    }

    protected void setGraphSpecificData() {
	setBarGraphType(Graph.STACKED);
	setXAxis("Intervals", "");
	setDataSource("Time spent by EP", data);
	setYAxis("Time", "ms");
    }

    private void createLayout() {
	getContentPane().add(getMainPanel());
	setupData();
	// graph view (particularly the X axis fields) depend on data.
	setGraphSpecificData();
	// System.out.println("Data set by dialog");
	pack();
	setVisible(true);
	refreshGraph();
    }

    /**
     *  Grabs the appropriate data (reading the logs if necessary).
     */
    private void setupData() {
	// right now, will only work with sumd files.
	if (Analysis.hasSumDetailData()) {
	    int numPEs = Analysis.getNumProcessors();
	    int numEPs = Analysis.getNumUserEntries();
	    // **CW** quite an ugly hack. 
	    // numIntervals here actually refers to the maximum number of
	    // intervals across all processors.
	    int numIntervals = 
		Analysis.getSumDetailNumIntervals();
	    data = new double[numIntervals][numEPs];
	    ProjectionsStatistics timeStats[][] = 
		new ProjectionsStatistics[numIntervals][numEPs];
	    for (int interval=0; interval<numIntervals; interval++) {
		for (int ep=0; ep<numEPs; ep++) {
		    timeStats[interval][ep] = new ProjectionsStatistics();
		}
	    }
	    for (int pe=0; pe<numPEs; pe++) {
		double tempData[][] = 
		    Analysis.getSumDetailData(pe,GenericSumDetailReader.TOTAL_TIME);
		// each sumd file has its own number of intervals
		// **CW** ANOTHER HACK!!!!!
		numIntervals = tempData[0].length;
		for (int interval=0; interval<numIntervals; interval++) {
		    for (int ep=0; ep<numEPs; ep++) {
			// sum detail data uses a reversed index format from
			// graph's DataSource2D objects.
			timeStats[interval][ep].accumulate(tempData[ep][interval]);
		    }
		}
	    }
	    for (int interval=0; interval<numIntervals; interval++) {
		for (int ep=0; ep<numEPs; ep++) {
		    data[interval][ep] = timeStats[interval][ep].getSum();
		}
	    }
	}
    }
}
