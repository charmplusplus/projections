package projections.gui;

import projections.gui.graph.*;
import projections.misc.*;
import projections.analysis.*;

import java.io.*;
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
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	MultiRunWindow thisWindow;

    // Gui components
    private MultiRunControlPanel controlPanel;
    private MultiRunTables tablesPanel;
    private JDialog tablesWindow;
    private GraphPanel graphPanel;
    private Graph graphCanvas;
    ProjectionsFileChooser fc;

    MultiRunData data;
    MultiRunDataAnalyzer analyzer;

    private JPanel mainPanel;

    // Layout variables. In this case, we want to keep them because we
    // will be switching between the table and graph view continuously.
    GridBagLayout      gbl;
    GridBagConstraints gbc;

    int selectedDataType;

    protected void windowInit() {
	// do nothing. No initialization required.
    }

    public MultiRunWindow(MainWindow parentWindow, Integer myWindowID) 
    {
	super(parentWindow, myWindowID);
	thisWindow = this;
	setBackground(Color.lightGray);
	showDialog();
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
	fc = new ProjectionsFileChooser(this, "Multirun Analysis",
					ProjectionsFileChooser.MULTIPLE_FILES);
	fc.showDialog();
    }

    /**
     *  For now, this will have to do. In the future, perhaps an interface
     *  for windows using the ProjectionsFileChooser can be developed that
     *  will required the following method to be implemented from which
     *  ProjectionsFileChooser may call.
     */
    public void dialogCallback() {
	final SwingWorker worker = new SwingWorker() {
		public Object construct() {
		    try {
			data = 
			    new MultiRunData(fc.userSelect_returnVal);
			analyzer = new MultiRunDataAnalyzer(data);
			// setting default data type
			selectedDataType = MultiRunData.TYPE_TIME;
			
			// set up the graph and table panels using the 
			// analyzed data
			thisWindow.createDisplayPanels();
		    } catch (IOException e) {
			System.err.println(e.toString());
		    }
		    return null;
		}
		public void finished() {
		    // set up the window GUI for display
		    thisWindow.createLayout();
		    thisWindow.pack();
		    thisWindow.setTitle("Multiple Run Analysis");
		    thisWindow.setVisible(true);
		}
	    };
	worker.start();
    }

    void createLayout()
    {
	mainPanel = new JPanel();
	mainPanel.setBackground(Color.gray);

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
    void createDisplayPanels() {
	// for graph mode
	MultiRunDataSource dataSource = 
	    analyzer.getDataSource(selectedDataType);
	MultiRunXAxis xAxis =
	    analyzer.getMRXAxisData();
	MultiRunYAxis yAxis =
	    analyzer.getMRYAxisData(selectedDataType);
	graphCanvas =
	    new Graph(dataSource, xAxis, yAxis);
	graphPanel = new GraphPanel(graphCanvas);
	// for table mode
	tablesPanel = new MultiRunTables(selectedDataType, analyzer);
    }

    // listener codes

    public void itemStateChanged(ItemEvent e) {
	if (e.getSource() instanceof JRadioButton) {
	   // JRadioButton button = (JRadioButton)e.getSource();
	    if (e.getStateChange() == ItemEvent.SELECTED) {
		selectedDataType = 
		    controlPanel.getSelectedIdx(e.getItemSelectable());
		// update graph
		MultiRunDataSource dataSource = 
		    analyzer.getDataSource(selectedDataType);
		MultiRunXAxis xAxis =
		    analyzer.getMRXAxisData();
		MultiRunYAxis yAxis =
		    analyzer.getMRYAxisData(selectedDataType);
		graphCanvas.setData(dataSource, xAxis, yAxis);
		// update tables
		tablesPanel.setType(selectedDataType);
	    }
	}
    }

    public void actionPerformed(ActionEvent e) {
	// do nothing for now.
	if (e.getSource() instanceof JButton) {
	    JButton button = (JButton)e.getSource();
	    if (button.getText().equals("Close Window")) {
		close();
	    } else if (button.getText().equals("Display Tables")) {
		if (tablesWindow == null) {
		    tablesWindow = new JDialog(this);
		    tablesWindow.getContentPane().add(tablesPanel);
		    // **CW** stop gap solution ...
		    tablesWindow.setSize(new Dimension(500,300));
		    tablesWindow.setVisible(true);
		} else {
		    tablesWindow.setVisible(true);
		}
	    }
	}
    }
}
