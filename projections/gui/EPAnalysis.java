package projections.gui;

import java.awt.*;
import java.awt.event.*;

import projections.analysis.*;

public class EPAnalysis 
    extends ProjectionsWindow  // dummy, should actually be an adapter
    implements EPNamdDefs
{
    private OrderedIntList validPEs;
    private long startTime;
    private long endTime;

    // indexed by data type followed by ep id summed across
    // processors.
    private long data[][];
    private String userEventNames[];
    private String entryNames[][];
    // indexed by data type followed by the category type summed across
    // processors
    private double categoryData[][];

    private MainWindow mainWindow;

    public EPAnalysis(MainWindow mainWindow) {
	this.mainWindow = mainWindow;
	// do nothing. Is an empty window with a RangeDialog.
	showDialog();
    }

    void showDialog() {
	if (dialog == null) {
	    dialog = new RangeDialog(this, "Select Range");
	}
	int status = dialog.showDialog();
//	dialog.displayDialog();
//	if (isDialogCancelled) {
	if (status == RangeDialog.DIALOG_CANCELLED){
	    // close the EPAnalysis window
	    dialog = null;
	    closeWindow();
	    return;
	}
	generateEPData();
	closeWindow();
    }

    public void setProcessorRange(OrderedIntList validPEs) {
	this.validPEs = validPEs;
    }

    public void setStartTime(long time) {
	this.startTime = time;
    }

    public void setEndTime(long time) {
	this.endTime = time;
    }

    private void closeWindow() {
	mainWindow.CloseEPAnalysis();
    }

    private void generateEPData() {
	// initialize data size (ignore category sizes defined in namddefs)
	data = new long[NUM_TYPE][Analysis.getNumUserEntries()+
				 Analysis.getNumUserDefinedEvents()+
				 NUM_SYS_EPS];
	userEventNames = Analysis.getUserEventNames();
	entryNames = Analysis.getEntryNames();

	categoryData = new double[NUM_TYPE][NUM_CATEGORY];

	EPDataGenerator generator = new EPDataGenerator(data, validPEs,
							startTime, endTime);
	// sort data into categories
	for (int i=0; i<Analysis.getNumUserEntries(); i++) {
	    if (i >= 71 && i <= 74) {
		// compute EP
		categoryData[TIME_DATA][COMPUTE_CAT] += 
		    data[TIME_DATA][i]/1000000.0;
	    } else if (i == 0) {
		// integrate type (dummy_thread_ep)
		categoryData[TIME_DATA][INTEGRATE_CAT] +=
		    data[TIME_DATA][i]/1000000.0;
	    } else if (i == 79) {
		// messaging type
		categoryData[TIME_DATA][MSG_CAT] +=
		    data[TIME_DATA][i]/1000000.0;
	    } else if (i == 84 || (i >= 89 && i <= 91)) {
		// message processing type
		categoryData[TIME_DATA][MSG_PROC_CAT] +=
		    data[TIME_DATA][i]/1000000.0;
	    } else if (i == 138 || i == 139 ||
		       (i >= 141 && i <= 149)) {
		// pme type
		categoryData[TIME_DATA][PME_CAT] +=
		    data[TIME_DATA][i]/1000000.0;
	    } else {
		categoryData[TIME_DATA][OTHER_CAT] +=
		    data[TIME_DATA][i]/1000000.0;
	    }
	}
	for (int i=0; i<Analysis.getNumUserDefinedEvents(); i++) {
	    // all belong to the message category
	    categoryData[TIME_DATA][MSG_CAT] +=
		data[TIME_DATA][Analysis.getNumUserEntries()+i]/1000000.0;
	}
	categoryData[TIME_DATA][IDLE_CAT] +=
	    data[TIME_DATA][Analysis.getNumUserEntries()+
			   Analysis.getNumUserDefinedEvents()]/1000000.0;
	// pack and unpack goes under messages
	categoryData[TIME_DATA][MSG_CAT] +=
	    data[TIME_DATA][Analysis.getNumUserEntries()+
			   Analysis.getNumUserDefinedEvents()+1]/1000000.0;
	categoryData[TIME_DATA][MSG_CAT] +=
	    data[TIME_DATA][Analysis.getNumUserEntries()+
			   Analysis.getNumUserDefinedEvents()+2]/1000000.0;

	// now output to screen (for now to terminal, later to file)
	// generate names first then data in a row-wise manner
	// print PE column as well.
	System.out.print("Num PEs\t");
	System.out.print("Compute Time\t");
	System.out.print("Integration Time\t");
	System.out.print("PME Work\t");
	System.out.print("Communication Time\t");
	System.out.print("Message Processing Time\t");
	System.out.print("Other\t");
	System.out.println("Idle Time");

	System.out.print(Analysis.getNumProcessors() + "\t");
	for (int i=0; i<NUM_CATEGORY-1; i++) {
	    System.out.print(categoryData[TIME_DATA][i] + "\t");
	}
	System.out.println(categoryData[TIME_DATA][NUM_CATEGORY-1]);
    }
}

