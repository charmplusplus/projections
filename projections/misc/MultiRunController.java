package projections.misc;

import projections.gui.*;
import projections.gui.graph.*;
import projections.analysis.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;

public class MultiRunController 
    implements ActionListener, ItemListener
{
    // public static definitions
    // 1) log types
    public static final int SUMMARY = 1;    
    public static final int LOG = 2;
    public static final int COUNTER = 3;

    // 2) pre-defined gui action commands
    public static final String CLOSE_WINDOW = "close window";
    public static final String DISPLAY_DATA = "display data";

    // gui components under control
    private MultiRunWindow mainWindow;
    private MultiRunDisplayPanel displayPanel = null;
    private MultiRunControlPanel controlPanel = null;
    private LegendPanel legendPanel = null;

    private Checkbox dataTypes[];

    private boolean textmode = true;

    // analysis model objects
    private MultiRunData rawdata;  // all data
    private MultiRunDataAnalyzer analyzer;

    int currentMap[] = null;
    boolean currentFilter[] = null;

    public MultiRunController(MultiRunWindow NmainWindow) {
	mainWindow = NmainWindow;
    }

    /**
     *  The preferred way
     */
    public void processUserInput(String stsFilenames[]) {
	try {
	    rawdata = new MultiRunData();
	    rawdata.initialize(stsFilenames);
	    analyzeRawData(rawdata);
	} catch (IOException e) {
	    System.err.println(e.toString());
	}
    }

    public void processUserInput(String basename,
				 String dataSetPathnames[],
				 boolean isDefaultRoot) {
	try {
	    rawdata = new MultiRunData();
	    rawdata.initialize(basename, dataSetPathnames, 
			       isDefaultRoot, SUMMARY);
	    analyzeRawData(rawdata);
	} catch (IOException e) {
	    System.err.println(e.toString());
	}
    }

    public void analyzeRawData(MultiRunData rawdata) {
	// create analyzer object to look at data and generate output
	analyzer = new MultiRunDataAnalyzer();
	analyzer.analyzeData(rawdata);
	
	// by default, ep list will be presented in an order sorted
	// by the analyzer's significance rating. Non-zero EPs are
	// also filtered away as a default.
	//
	// we do, however, want the entire array to be passed to
	// legendPanel.
	currentMap = analyzer.getSignificanceMap();
	currentFilter = analyzer.getNonZeroFilter();
	legendPanel.setData(analyzer.getMRLegendData(null, null),
			    analyzer.getData(MultiRunDataAnalyzer.ANALYZE_SUM, 
					     null, null).getColorMap(),
			    currentFilter,
			    currentMap);
    }
    
    public void registerDisplay(MultiRunDisplayPanel NdisplayPanel) {
	displayPanel = NdisplayPanel;
    }

    public void registerControl(MultiRunControlPanel NcontrolPanel) {
	controlPanel = NcontrolPanel;
    }

    public void registerLegend(LegendPanel NlegendPanel) {
	legendPanel = NlegendPanel;
    }

    public void registerDataTypes(Checkbox NdataTypes[]) {
	dataTypes = NdataTypes;
    }

    // Listener code for GUI components under its control

    public void actionPerformed(ActionEvent evt) {
	String actionCommand = evt.getActionCommand();

	if (actionCommand.equals(CLOSE_WINDOW)) {
	    mainWindow.Close();
	} else if (actionCommand.equals(DISPLAY_DATA)) {
	    displayData();
	}
    }

    public void itemStateChanged(ItemEvent evt) {

	if (evt.getSource() instanceof Checkbox) {
	    Checkbox chkbox = (Checkbox)evt.getSource();
	    String itemLabel = chkbox.getLabel();
	    if (itemLabel.equals("text")) {
		textmode = true;
		displayPanel.setDisplayMode(textmode);
	    } else if (itemLabel.equals("graph")) {
		textmode = false;
		displayPanel.setDisplayMode(textmode);
	    } else if (itemLabel.equals("unsorted")) {
		legendPanel.setSort(null);
		displayData();
	    } else if (itemLabel.equals("sorted by significance")) {
		legendPanel.setSort(analyzer.getSignificanceMap());
		displayData();
	    } else if (itemLabel.equals("sorted by % growth")) {
		legendPanel.setSort(analyzer.getGrowthMap());
		displayData();
	    } else if (itemLabel.equals("sorted by size")) {
		legendPanel.setSort(analyzer.getSizeMap());
		displayData();
	    } else if (itemLabel.equals("no filter")) {
		legendPanel.setFilter(null);
		displayData();
	    } else if (itemLabel.equals("non zero EPs")) {
		legendPanel.setFilter(analyzer.getNonZeroFilter());
		displayData();
	    }
	}
    }

    private void displayData() {
	boolean filter[];
	int map[];
	
	filter = legendPanel.getSelection();
	map = legendPanel.getLegendSort();
	
	int numDataTypes = 0;
	for (int i=0; i<MultiRunDataAnalyzer.TOTAL_ANALYSIS_TAGS; i++) {
	    if (dataTypes[i].getState()) {
		numDataTypes++;
	    }
	}
	MultiRunDataSource sources[];
	MultiRunYAxis yAxes[];
	sources = new MultiRunDataSource[numDataTypes];
	yAxes = new MultiRunYAxis[numDataTypes];
	int typeIdx = 0;
	for (int i=0; i<MultiRunDataAnalyzer.TOTAL_ANALYSIS_TAGS;i++) {
	    if (dataTypes[i].getState()) {
		sources[typeIdx] = analyzer.getData(i, filter, map);
		yAxes[typeIdx] = analyzer.getMRYAxisData(i, filter);
		typeIdx++;
	    }
	}
	
	displayPanel.setData(sources,
			     analyzer.getMRXAxisData(null),
			     yAxes,
			     analyzer.getMRLegendData(filter, map),
			     textmode);
    }
}



