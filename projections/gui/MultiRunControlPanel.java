package projections.gui;

import projections.misc.*;
import projections.analysis.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class MultiRunControlPanel extends Panel
{
    private CheckboxGroup displayModes;
    private Panel modePanel;
    private Checkbox textMode;
    private Checkbox graphMode;

    // various types of sorting. Right now restricted to unsorted and
    // sorted by significance.
    private CheckboxGroup sortModes;
    private Panel sortPanel;
    private Checkbox unsortedMode;
    private Checkbox significanceMode;
    private Checkbox growthMode;
    private Checkbox sizeMode;

    // various types of filtering. Right now restricted to no filters and
    // removing all zero EPs
    private CheckboxGroup filterModes;
    private Panel filterPanel;
    private Checkbox noFilterMode;
    private Checkbox nonZeroMode;

    // the following datatype components have to correspond to the 
    // tag order defined in MultiRunDataAnalyzer.
    // I don't know if it is possible to do this without hardcoding.
    private Checkbox dataTypes[];
    private Panel datatypePanel;
    private Checkbox sumData;
    private Checkbox avgData;
    private Checkbox minData;
    private Checkbox maxData;

    private Button done;
    private Button displayData;

    private Label cmdLineLabel;
    private TextField cmdLine;

    private MultiRunWindow mainWindow;
    private MultiRunController controller;

    public MultiRunControlPanel(MultiRunWindow NmainWindow,
				MultiRunController Ncontroller)
    {
	mainWindow = NmainWindow;
	controller = Ncontroller;

	controller.registerControl(this);

	setBackground(Color.lightGray);

	// data display modes
	displayModes = new CheckboxGroup();
	modePanel = new Panel();

	textMode = new Checkbox("text", displayModes, true);
	textMode.addItemListener(controller);
       
	graphMode = new Checkbox("graph", displayModes, false);
	graphMode.addItemListener(controller);

	modePanel.add(textMode);
	modePanel.add(graphMode);

	// sorting modes
	sortModes = new CheckboxGroup();
	sortPanel = new Panel();

	unsortedMode = new Checkbox("unsorted", sortModes, false);
	unsortedMode.addItemListener(controller);

	significanceMode = new Checkbox("sorted by significance",
					sortModes, true);
	significanceMode.addItemListener(controller);

	growthMode = new Checkbox("sorted by % growth",
				  sortModes, false);
	growthMode.addItemListener(controller);

	sizeMode = new Checkbox("sorted by size",
				sortModes, false);
	sizeMode.addItemListener(controller);

	sortPanel.add(unsortedMode);
	sortPanel.add(significanceMode);
	sortPanel.add(growthMode);
	sortPanel.add(sizeMode);

	// filtering modes
	filterModes = new CheckboxGroup();
	filterPanel = new Panel();
	
	noFilterMode = new Checkbox("no filter", filterModes, false);
	noFilterMode.addItemListener(controller);

	nonZeroMode = new Checkbox("non zero EPs", filterModes, true);
	nonZeroMode.addItemListener(controller);

	filterPanel.add(noFilterMode);
	filterPanel.add(nonZeroMode);

	// data type selectors
	datatypePanel = new Panel();
	sumData = new Checkbox("summation", true);
	avgData = new Checkbox("average", false);
	minData = new Checkbox("min", false);
	maxData = new Checkbox("max", false);
	dataTypes = new Checkbox[MultiRunDataAnalyzer.TOTAL_ANALYSIS_TAGS];
	dataTypes[0] = sumData;
	dataTypes[1] = avgData;
	dataTypes[2] = minData;
	dataTypes[3] = maxData;
	controller.registerDataTypes(dataTypes);
	datatypePanel.add(sumData);
	datatypePanel.add(avgData);
	datatypePanel.add(minData);
	datatypePanel.add(maxData);

	done = new Button("Close Window");
	done.setActionCommand(MultiRunController.CLOSE_WINDOW);
	done.addActionListener(controller);

	displayData = new Button("Display Selected EPs");
	displayData.setActionCommand(MultiRunController.DISPLAY_DATA);
	displayData.addActionListener(controller);

	GridBagLayout gbl      = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();

	setLayout(gbl);
	
	gbc.fill = GridBagConstraints.HORIZONTAL;

	Util.gblAdd(this, modePanel,     gbc, 0,0, 1,1, 1,0, 5,5,5,5);
	Util.gblAdd(this, filterPanel,   gbc, 1,0, 1,1, 1,0, 5,5,5,5);
	Util.gblAdd(this, sortPanel,     gbc, 0,1, 2,1, 1,0, 5,5,5,5);
	Util.gblAdd(this, datatypePanel, gbc, 0,2, 2,1, 1,0, 5,5,5,5);

	gbc.fill = GridBagConstraints.BOTH;
	
	Util.gblAdd(this, displayData,   gbc, 2,0, 1,1, 1,0, 5,2,5,2);
	Util.gblAdd(this, done,          gbc, 2,1, 1,1, 1,0, 5,2,5,2);
    }

    public void paint(Graphics g) {
	Color originalColor;

	super.paint(g);

	// paint black border boxes around checkbox groups
	originalColor = g.getColor();
	g.setColor(Color.black);

	// paint black border around display mode checkbox group
	g.drawRect(modePanel.getX()-1, modePanel.getY()-1,
		   modePanel.getWidth()+1, modePanel.getHeight()+1);

	// paint black border around filter mode checkbox group
	g.drawRect(filterPanel.getX()-1, filterPanel.getY()-1,
		   filterPanel.getWidth()+1, filterPanel.getHeight()+1);

	// paint black border around sort mode checkbox group
	g.drawRect(sortPanel.getX()-1, sortPanel.getY()-1,
		   sortPanel.getWidth()+1, sortPanel.getHeight()+1);

	// paint black border around datatype checkbox set
	g.drawRect(datatypePanel.getX()-1, datatypePanel.getY()-1,
		   datatypePanel.getWidth()+1, datatypePanel.getHeight()+1);

	g.setColor(originalColor);
    }

}


