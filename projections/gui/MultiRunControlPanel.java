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

    private Checkbox dataTypes[];
    private Panel datatypePanel;
    private Checkbox sumData;
    private Checkbox avgData;

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

	// data type selectors
	datatypePanel = new Panel();
	sumData = new Checkbox("summation", true);
	avgData = new Checkbox("average", false);
	dataTypes = new Checkbox[MultiRunDataAnalyzer.TOTAL_ANALYSIS_TAGS];
	dataTypes[0] = sumData;
	dataTypes[1] = avgData;
	controller.registerDataTypes(dataTypes);
	datatypePanel.add(sumData);
	datatypePanel.add(avgData);

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

	Util.gblAdd(this, modePanel,    gbc, 0,0, 1,1, 1,0, 2,2,2,2);
	Util.gblAdd(this, datatypePanel, gbc, 0,1, 1,1, 1,0, 2,2,2,2);
	Util.gblAdd(this, displayData,  gbc, 1,0, 1,1, 1,0, 2,2,2,2);
	Util.gblAdd(this, done,         gbc, 1,1, 1,1, 1,1, 2,2,2,2);
    }
}
