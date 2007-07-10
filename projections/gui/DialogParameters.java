package projections.gui;

/**
 *  DialogParameters.java
 *  8/11/2006
 *
 *  This is essentially a "struct" object encapsulating the state of
 *  all possible dialog parameters that can be passed between tools
 *  and dialogs as well as between tools.
 *
 *  DESIGN DECISION: While this could (and maybe should) be implemented
 *  as a heirarchy of inherited classes, it is felt that having everything
 *  at one spot would make it much easier to update or add new dialogs.
 */

public class DialogParameters 
    implements Cloneable
{
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;

    // ***** Variables for RangeDialog (base)
    protected OrderedIntList validPEs;
    protected long startTime;
    protected long endTime;

    // ****** Variables unique to RangeDialog->IntervalRangeDialog
    protected long intervalSize;

    // ***** Variables unique to RangeDialog->BinDialog
    protected int timeNumBins;
    protected long timeBinSize;
    protected long timeMinBinSize;

    protected int msgNumBins;
    protected long msgBinSize;
    protected long msgMinBinSize;

    public DialogParameters() {
	// initialization of RangeDialog values
	validPEs = MainWindow.runObject[myRun].getValidProcessorList().copyOf();
	startTime = 0;
	endTime = MainWindow.runObject[myRun].getTotalTime();

	// initialization of IntervalRangeDialog values
	intervalSize = 1000;

	// initialization of BinDialog values
	// default from 0 to 10ms for time
	// default from 0 to 2000 bytes for messages
	timeNumBins = 100;
	timeBinSize = 100;
	timeMinBinSize = 0;
	msgNumBins = 200;
	msgBinSize = 100;
	msgMinBinSize = 0;
    }

    public Object clone() {
	DialogParameters returnVal = new DialogParameters();

	returnVal.validPEs = validPEs.copyOf();
	returnVal.startTime = startTime;
	returnVal.endTime = endTime;

	returnVal.intervalSize = intervalSize;

	returnVal.timeNumBins = timeNumBins;
	returnVal.timeBinSize = timeBinSize;
	returnVal.timeMinBinSize = timeMinBinSize;
	returnVal.msgNumBins = msgNumBins;
	returnVal.msgBinSize = msgBinSize;
	returnVal.msgMinBinSize = msgMinBinSize;
	
	return returnVal;
    }
}
