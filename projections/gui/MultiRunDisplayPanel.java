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
 */
public class MultiRunDisplayPanel extends Container 
{
    boolean hasData = false;
    boolean currentTextmode;

    // gui components
    private GridBagConstraints gbc;
    private MultiRunWindow mainWindow;
    private MultiRunController controller;

    private Panel mainPanel;

    private Panel textPanel;
    private TextArea displayArea;

    private GraphPanel graphPanel;
    private Panel graphComponent;
    private Graph graphCanvas;
    private Canvas emptyGraphCanvas;

    // data items
    private MultiRunDataSource dataSources[];
    private MultiRunXAxis xAxisData;
    private MultiRunYAxis yAxisDatas[];
    private String legendData[];

    private MultiRunTextRenderer renderer;

    public MultiRunDisplayPanel(MultiRunWindow NmainWindow,
				MultiRunController Ncontroller) {
	mainWindow = NmainWindow;
	controller = Ncontroller;
	renderer = new MultiRunTextRenderer();

	controller.registerDisplay(this);

	setBackground(Color.lightGray);

	textPanel = new Panel();
	displayArea = new TextArea("",40,120,TextArea.SCROLLBARS_BOTH);
	// use a fixed width font
	displayArea.setFont(Font.decode("monospaced"));
	textPanel.add(displayArea);

	emptyGraphCanvas = new Canvas();
	emptyGraphCanvas.setBackground(Color.black);
	// make initial empty graph canvas the same size as the text area
	emptyGraphCanvas.setSize(displayArea.getSize());
	graphComponent = new Panel();
	graphComponent.add(emptyGraphCanvas);
	graphComponent.setSize(displayArea.getSize());

	graphCanvas = new Graph();
	graphPanel = new GraphPanel(graphCanvas);

	// default to text display
	currentTextmode = true;

        GridBagLayout      gbl = new GridBagLayout();
        gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.BOTH;

	this.setLayout(gbl);

	Util.gblAdd(this, textPanel, gbc, 0,0, 1,1, 1,1, 2,2,2,2);
    }

    public void setDisplayMode(boolean textmode) {
	displayData(textmode);
    }

    public void setData(MultiRunDataSource NdataSources[], 
			MultiRunXAxis NxAxis, MultiRunYAxis NyAxes[],
			String[] NlegendData,
			boolean textmode) {
	// in the most general case, data may need to be maintained.
	dataSources = NdataSources; 
	xAxisData = NxAxis;
	yAxisDatas = NyAxes;
	legendData = NlegendData;

	updateComponents();
	// if setting data for the first time, 
	if (hasData == false) {
	    graphComponent.remove(emptyGraphCanvas);
	    graphComponent.add(graphPanel);
	    hasData = true;
	}
	displayData(textmode);
    }

    private void updateComponents() {
	// update text component
	try {
	    MultiRunTextAreaWriter writer = 
		new MultiRunTextAreaWriter(displayArea);
	    renderer.generateOutput(writer,
				    dataSources,
				    legendData,
				    xAxisData.getIndexNames());
	    writer.close();
	} catch (IOException e) {
	    System.err.println("Error writing data to stream!");
	}
	// update graph component
	// the graph has no capability of showing more than one type of
	// data, hence, it will always show the first type.
	graphCanvas.setData(dataSources[0], xAxisData, yAxisDatas[0]);
    }

    private void displayData(boolean textmode)
    {
	if (textmode) {
	    // text mode
	    if (textmode != currentTextmode) {
		// need to re-introduce text components into the panel
		this.remove(graphComponent);
		Util.gblAdd(this, textPanel, gbc, 0,0, 1,1, 1,1, 2,2,2,2);
		currentTextmode = textmode;
	    }
	} else {
	    // graphics mode
	    if (textmode != currentTextmode) {
		// need to re-introduce graph components into the panel
		this.remove(textPanel);
		Util.gblAdd(this, graphComponent, gbc, 0,0, 1,1, 1,1, 2,2,2,2);
		currentTextmode = textmode;
	    }
	}
	repaint();
    }

    // for output to other sources
    // ***CURRENT IMP*** not implemented yet
    public void displayData(Writer writer) 
    {
    }
}
