package projections.gui;

import projections.gui.graph.*;
import projections.misc.*;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

/**
 *  Written by Chee Wai Lee
 *  4/4/2002
 * 
 *  MultiRunDisplayPanel contains the data display visual object for multiple
 *  run analysis.
 *
 *  ***CURRENT IMP*** no associated legend as yet. 
 *
 */
public class MultiRunDisplayPanel extends Container 
{
    // settings
    private boolean textMode = true; // default to text display

    // gui components
    private MultiRunWindow mainWindow;
    private MultiRunController controller;

    private Panel mainPanel;
    private TextArea displayArea;

    // data items
    private MultiRunDataSource dataSource;
    private MultiRunXAxis xAxisData;
    private MultiRunYAxis yAxisData;
    private String legendData[];

    private MultiRunTextRenderer renderer;
    private boolean textmode = true;

    public MultiRunDisplayPanel(MultiRunWindow NmainWindow,
				MultiRunController Ncontroller) {
	mainWindow = NmainWindow;
	controller = Ncontroller;
	renderer = new MultiRunTextRenderer();

	controller.registerDisplay(this);

	setBackground(Color.lightGray);

	////// Main Panel
	mainPanel = new Panel();
	displayArea = new TextArea("",40,120,TextArea.SCROLLBARS_BOTH);
	// use a fixed width font
	displayArea.setFont(Font.decode("monospaced"));

	// default to text display
	mainPanel.add(displayArea);

        GridBagLayout      gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.BOTH;

	this.setLayout(gbl);

	Util.gblAdd(this, mainPanel, gbc, 0,0, 1,1, 2,1, 2,2,2,2);
    }

    public void setData(MultiRunDataSource NdataSource, 
			MultiRunXAxis NxAxis, MultiRunYAxis NyAxis,
			String[] NlegendData) {
	// in the most general case, data may need to be maintained.
	dataSource = NdataSource; 
	xAxisData = NxAxis;
	yAxisData = NyAxis;
	legendData = NlegendData;
	if (textmode) {
	    printData();
	} else {
	    // display onto graph
	}
    }

    public void printData()
    {
	try {
	    // ***CURRENT IMP*** hardcode for now
	    MultiRunDataSource allData[] = new MultiRunDataSource[1];
	    allData[0] = dataSource;
	    MultiRunTextAreaWriter writer = 
		new MultiRunTextAreaWriter(displayArea);
	    renderer.generateOutput(writer,
				    allData,
				    legendData,
				    xAxisData.getIndexNames());
	    writer.close();
	} catch (IOException e) {
	    System.err.println("Error writing data to stream!");
	}
    }

    public void printData(Writer writer) 
    {
    }
}
