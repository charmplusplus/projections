package projections.gui;

import projections.gui.graph.*;
import projections.misc.*;
import projections.analysis.*;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 *  Written by Chee Wai Lee
 *  3/27/2002
 *  Updated:
 *    2/4/2003 - Chee Wai Lee. Removed the confusing Controller model and
 *               integrated its basic functionality in the Window code.
 *
 *  MultiRunWindow presents the main display window for multi-run analysis
 *  data. It starts by presenting a file dialog for users to select a set
 *  of log files.
 *
 */

public class MultiRunWindow extends ProjectionsWindow
    implements ActionListener, ItemListener
{
    // Gui components
    private MultiRunControlPanel controlPanel;
    private MultiRunTables tablesPanel;
    private AreaGraphPanel graphPanel;

    private MultiRunDataAnalyzer analyzer;

    private JPanel mainPanel;

    // Layout variables. In this case, we want to keep them because we
    // will be switching between the table and graph view continuously.
    GridBagLayout      gbl;
    GridBagConstraints gbc;

    // data display modes
    private static final int MODE_GRAPH = 0;
    private static final int MODE_TABLE = 1;
    private int displayMode;

    private int selectedDataType;

    public MultiRunWindow(MainWindow parentWindow, Integer myWindowID) 
    {
	super(parentWindow, myWindowID);
	setBackground(Color.lightGray);
    }

    /**
     *  The MultiRunWindow is one of the special tools uses its own dialog.
     */
    public void showDialog() {
	showFileDialog();
    }

    public void showWindow() {
	showFileDialog();
    }

    public void getDialogData() {
	// do nothing since it will not use dialog.
    }

    public void close()
    {
	setVisible(false);
	dispose();
	parentWindow.closeChildWindow(myWindowID);
    }

    public void showFileDialog() {
	try {
	    ProjectionsFileChooser fc = 
		new ProjectionsFileChooser(this, "Multirun Analysis",
					   ProjectionsFileChooser.MULTIPLE_FILES);
	    MultiRunCallBack callback = new MultiRunCallBack(this, fc);
	    int returnVal = fc.showDialog(callback);
	    // the callback will decide what happens
	} catch (Exception e) {
	    System.out.println("Filechooser error. Please fix");
	    ProjectionsFileChooser.handleException(this, e);
	}
    }

    /**
     *  This method is called by MultiRunCallBack at the successful
     *  completion of the choosing of files in ProjectionsFileChooser.
     */
    public void beginAnalysis(ProjectionsFileChooser fc) {
	String stsFilenames[] = fc.userSelect_returnVal;
	try {
	    MultiRunData data = new MultiRunData(stsFilenames);
	    System.out.println("data read done");
	    analyzer = new MultiRunDataAnalyzer(data);

	    // setting default data type
	    selectedDataType = MultiRunData.TYPE_TIME;

	    // set up the graph and table panels using the analyzed data
	    createDisplayPanels();

	    // set up the window GUI for display
	    createLayout();
	    pack();
	    setTitle("Multi-Run Analysis");
	    setVisible(true);
	} catch (IOException e) {
	    System.err.println(e.toString());
	}
    }

    private void createLayout()
    {
	mainPanel = new JPanel();
	mainPanel.setBackground(Color.gray);

	// initially default to showing the graph view
	displayMode = MODE_GRAPH;

	controlPanel = new MultiRunControlPanel(this, selectedDataType);

	gbl = new GridBagLayout();
	gbc = new GridBagConstraints();

	mainPanel.setLayout(gbl);
	gbc.fill = GridBagConstraints.BOTH;

	Util.gblAdd(mainPanel, graphPanel, gbc, 0,0, 1,1, 1,1, 2,2,2,2); 
	Util.gblAdd(mainPanel, controlPanel, gbc, 0,1, 2,1, 1,0, 2,2,2,2); 

	setContentPane(mainPanel);
    }

    /**
     *	Setup both the graph and table display panels using
     *	the analyzer code. This method is unusual in that it is separate
     *  from the createLayout code but yet creates 2 GUI components to
     *  be used in createLayout.
     */
    private void createDisplayPanels() {
	// for graph mode
	MultiRunDataSource dataSource = 
	    analyzer.getDataSource(selectedDataType);
	MultiRunXAxis xAxis =
	    analyzer.getMRXAxisData();
	MultiRunYAxis yAxis =
	    analyzer.getMRYAxisData(selectedDataType);
	Graph graphCanvas =
	    new Graph(dataSource, xAxis, yAxis);
	graphPanel = new AreaGraphPanel(graphCanvas);
	// for table mode
	tablesPanel = new MultiRunTables(selectedDataType, analyzer);
    }

    /**
     *  This method should be called in response to an action that results
     *  from a user's request to change the display mode of the window.
     */
    private void setDisplayMode(int mode) {
	// optimization
	if (mode == displayMode) {
	    return;
	}
	displayMode = mode;
	switch (mode) {
	case MODE_GRAPH:
	    mainPanel.remove(tablesPanel);
	    Util.gblAdd(mainPanel, graphPanel, gbc,  0,0, 1,1, 1,1, 2,2,2,2);
	    mainPanel.validate();
	    graphPanel.repaint();
	    break;
	case MODE_TABLE:
	    mainPanel.remove(graphPanel);
	    Util.gblAdd(mainPanel, tablesPanel, gbc, 0,0, 1,1, 1,1, 2,2,2,2);
	    mainPanel.validate();
	    tablesPanel.repaint();
	    break;
	}
    }

    // listener codes

    public void itemStateChanged(ItemEvent e) {
	if (e.getSource() instanceof JRadioButton) {
	    JRadioButton button = (JRadioButton)e.getSource();
	    if (e.getStateChange() == ItemEvent.SELECTED) {
		if (button.getText().equals("Table")) {
		    setDisplayMode(MODE_TABLE);
		} else if (button.getText().equals("Graph")) {
		    setDisplayMode(MODE_GRAPH);
		} else {
		    selectedDataType = 
			controlPanel.getSelectedIdx(e.getItemSelectable());
		    // update graph
		    MultiRunDataSource dataSource = 
			analyzer.getDataSource(selectedDataType);
		    MultiRunXAxis xAxis =
			analyzer.getMRXAxisData();
		    MultiRunYAxis yAxis =
			analyzer.getMRYAxisData(selectedDataType);
		    graphPanel.setData(dataSource, xAxis, yAxis);
		    // update tables
		    tablesPanel.setType(selectedDataType);
		}
	    }
	}
    }

    public void actionPerformed(ActionEvent e) {
	// do nothing for now.
	if (e.getSource() instanceof JButton) {
	    JButton button = (JButton)e.getSource();
	    if (button.getText().equals("Close Window")) {
		close();
	    }
	}
    }
}
