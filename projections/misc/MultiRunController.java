package projections.misc;

import projections.gui.*;
import projections.analysis.*;

import java.io.*;

public class MultiRunController {

    // public static definitions
    // 1) log types
    public static final int SUMMARY = 1;    

    // gui components under control
    private MultiRunWindow mainWindow;
    private MultiRunDisplayPanel displayPanel = null;
    private MultiRunControlPanel controlPanel = null;

    // analysis model objects
    private static MultiRunData rawdata;  // all data
    private static MultiRunDataAnalyzer analyzer;

    public MultiRunController(MultiRunWindow NmainWindow) {
	mainWindow = NmainWindow;
    }

    public void processUserInput(String basename,
				 String dataSetPathnames[],
				 boolean isDefaultRoot) {
        try {
            // create accumulator object to read raw data
	    // ***CURRENT IMP*** for now, it will only read summary files
	    // eventually, it will read log files on an as-needed basis.
            rawdata = new MultiRunData();
            rawdata.initialize(basename, dataSetPathnames, 
			       isDefaultRoot, SUMMARY);

            // create analyzer object to look at data and generate output
            analyzer = new MultiRunDataAnalyzer();
            analyzer.analyzeData(rawdata);

            displayPanel.setData(analyzer.getData(MultiRunDataAnalyzer.ANALYZE_SUM),
				 analyzer.getMRXAxisData(),
				 analyzer.getMRYAxisData(MultiRunDataAnalyzer.ANALYZE_SUM),
				 analyzer.getMRLegendData());
	} catch (java.io.IOException e) {
            System.err.println(e.toString());
        }
    }

    public void registerDisplay(MultiRunDisplayPanel NdisplayPanel) {
	displayPanel = NdisplayPanel;
    }

    public void registerControl(MultiRunControlPanel NcontrolPanel) {
	controlPanel = NcontrolPanel;
    }

    public void printData(Writer writer) {
	displayPanel.printData(writer);
    }
}


