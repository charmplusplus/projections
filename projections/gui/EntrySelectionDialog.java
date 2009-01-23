package projections.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *  EntrySelectionDialog
 *  written by Chee Wai Lee
 *  7/2/2002
 *
 *  This is a **first-cut** implementation of a entry method selection/color
 *  modification dialog for projections toolkits. This will involve the
 *  passing into the constructor all the stuff necessary.
 *
 *  Eventually, this will be streamlined to use the "toolkit data model" that
 *  I favor for all toolkits in projections. That would involve changes to
 *  those toolkits (which is not desired now).
 */
public class EntrySelectionDialog extends Frame 
   implements ActionListener
{

	private ColorSelectable callbackTarget;

    // indexed by type followed by the entry point index
    private ColorPanel[][] entryPointColors;
    private Checkbox[][]   entryPointSelection;
    
    // for specifying which EP's color panel is selected in order to apply
    // color changes through ColorSelectWindow.
    private ColorPanel selectedCP;
   
    private Button bAll, bClear, bSave, bCancel;

    private int numTypes;
    private String typeLabelStrings[];
    private int numEPs;
   
    private boolean stateArray[][];
    private Color colorArray[][];
    private boolean existsArray[][];
    private String entryNames[];

    // flag
    private boolean layoutComplete = false;

    /**
     *  This is a wrapper constructor for a non-callback based dialog.
     */

    public EntrySelectionDialog(Frame parent, 
				ColorSelectable callbackTarget,
				String typeLabelStrings[],
				boolean stateArray[][],
				Color colorArray[][],
				boolean existsArray[][],
				String entryNames[])
    {
	super();
	this.callbackTarget = callbackTarget;
	this.numTypes = typeLabelStrings.length;
	this.typeLabelStrings = typeLabelStrings;
	this.numEPs = stateArray[numTypes-1].length;
	this.stateArray = stateArray;
	this.colorArray = colorArray;
	this.existsArray = existsArray;
	this.entryNames = entryNames;

	entryPointColors = new ColorPanel[numTypes][numEPs];
	entryPointSelection = new Checkbox[numTypes][numEPs];
	
	addWindowListener(new WindowAdapter()
	    {                    
		public void windowClosing(WindowEvent e)
		{
		    closeDialog();
		}
	    });
    }
    
    public void showDialog() {
	if (layoutComplete) {
	    setVisible(true);
	} else {
	    setBackground(Color.lightGray);
	    setTitle("Select Display Items");
	    setLocation(0, 0);
	
	    createLayout();
	    pack();
	    setVisible(true);
	}
    }
    
    public void actionPerformed(ActionEvent evt)
    {
	if(evt.getSource() instanceof ColorPanel) {   
	    selectedCP = (ColorPanel)evt.getSource();

	    String selectTitle = "";

	    selectTitle = entryNames[selectedCP.getIndex()];
	    selectTitle += " : " + 
		typeLabelStrings[selectedCP.getType()];
	    /*
	    colorSelectWindow = 
		new ColorSelectWindow(this, selectedCP.getColor(),
				      selectTitle);
	    */
	    JColorChooser colorWindow = new JColorChooser();
	    Color returnColor =
		JColorChooser.showDialog(this, selectTitle,
				       selectedCP.getColor());
	    if (returnColor != null) {
		selectedCP.setColor(returnColor);
	    }
	} else if(evt.getSource() instanceof Button) {
	    Button b = (Button) evt.getSource();
	  
	    if (b == bAll || b == bClear) {
		boolean dest=(b==bAll);
		for (int type=0; type<numTypes; type++) {
		    for (int ep=0; ep<numEPs; ep++) {
			if (existsArray[type][ep]) {
			    entryPointSelection[type][ep].setState(dest);
			} 
		    }   
		}
	    } else if (b == bCancel) {
		closeDialog();   
	    }  else if (b == bSave) {
		for (int type=0; type<numTypes; type++) {
		    for (int ep=0; ep<numEPs; ep++) {
			if (existsArray[type][ep]) {
			    colorArray[type][ep] =
				entryPointColors[type][ep].getColor();
			    stateArray[type][ep] =
				entryPointSelection[type][ep].getState();
			}
		    }
		}
		if (callbackTarget != null) {
		    callbackTarget.applyDialogColors();
		}
		setVisible(false);
	    }
	}           
    }   

    void closeDialog()
    {
	// this is essentially a cancellation command.

	setVisible(false);
	// reset the values such that it is consistent with the data display
	// associated with the toolkit.
	for (int type=0; type<numTypes; type++) {
	    for (int ep=0; ep<numEPs; ep++) {
		if (existsArray[type][ep]) {
		    entryPointSelection[type][ep].setState(stateArray[type][ep]);
		    entryPointColors[type][ep].setColor(colorArray[type][ep]);
		}
	    }
	} 
	dispose();
    }   
    
    private void createLayout()
    {
	for (int type=0; type<numTypes; type++) {
	    for (int ep=0; ep<numEPs; ep++) {
		if (existsArray[type][ep]) {
		    entryPointSelection[type][ep] = new Checkbox();
		    entryPointSelection[type][ep].setState(stateArray[type][ep]);
		    entryPointColors[type][ep] = 
			new ColorPanel(type, ep, colorArray[type][ep]);
		    entryPointColors[type][ep].addActionListener(this);
		}
	    }
	}

	Panel mainPanel = new Panel();
	add("Center", mainPanel);
	mainPanel.setBackground(Color.gray);
	  
	GridBagLayout      gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();
	  
	mainPanel.setLayout(gbl);
	  
	gbc.fill = GridBagConstraints.BOTH;
	gbc.insets = new Insets(0, 2, 0, 2);

	Label lNote0 = new Label("USAGE NOTES:");
	Label lNote1 = 
	    new Label("- Modify colors by clicking on color button.");
	Label lNote2 = 
	    new Label("- Click on check box to select an EP to display.");
	Label lNote3 =
	    new Label("- EPs that are not used during execution " +
		      "are not presented here.");
	lNote0.setFont(new Font("SansSerif", Font.PLAIN, 10));
	lNote1.setFont(new Font("SansSerif", Font.PLAIN, 10));
	lNote2.setFont(new Font("SansSerif", Font.PLAIN, 10));
	lNote3.setFont(new Font("SansSerif", Font.PLAIN, 10));
	
	ScrollPane sp = new ScrollPane();  // contains the inner display area
  
	Panel displayPanel = new GrayPanel();
	displayPanel.setLayout(gbl);
	  
	Util.gblAdd(displayPanel, sp,     gbc,  0,0, 1,1, 1,1, 5,5, 0,5);
	Util.gblAdd(displayPanel, lNote0, gbc,  0,1, 1,1, 1,0, 0,5,-1,5);
	Util.gblAdd(displayPanel, lNote1, gbc,  0,2, 1,1, 1,0, 0,5,-1,5);
	Util.gblAdd(displayPanel, lNote2, gbc,  0,3, 1,1, 1,0, 0,5,-1,5);
	Util.gblAdd(displayPanel, lNote3, gbc,  0,4, 1,1, 1,0, 0,5, 5,5);

	// construct a column labelled at the top for each type
	Label typeLabels[] = new Label[numTypes];
	for (int type=0; type<numTypes; type++) {
	    typeLabels[type] = new Label(typeLabelStrings[type], Label.CENTER);
	}
   
	// Light Weight Panel - Do we really need this?  
	Panel contentPanel = new Panel();
	sp.add(contentPanel);
	contentPanel.setLayout(gbl);
	  
	gbc.fill = GridBagConstraints.BOTH;
	gbc.anchor = GridBagConstraints.CENTER;
	gbc.insets = new Insets(0, 2, 0, 2);

	// place column headers
	for (int type=0; type<numTypes; type++) {
	    Util.gblAdd(contentPanel, typeLabels[type], gbc,
			1+2*type,0, 2,1, 1,0);
	}
	  
	int ypos = 1;  // starting at line 2 for EP data
	Label tempLabel;
	gbc.fill = GridBagConstraints.BOTH;
	for (int ep=0; ep<numEPs; ep++) {
	    int xpos = 0;  // xposition for each line.
	    // print EP label if at least one type of the event exists
	    if (someExists(ep)) {
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.BOTH;
		tempLabel = new Label(entryNames[ep], Label.RIGHT);
		Util.gblAdd(contentPanel, tempLabel, gbc,
			    xpos++,ypos, 1,1, 1,0, 0,2,0,5);
	    } else {
		continue;
	    }
	    for(int type=0; type<numTypes; type++) {
		if (existsArray[type][ep]) {
		    gbc.fill = GridBagConstraints.VERTICAL;
		    gbc.anchor = GridBagConstraints.EAST;
		    Util.gblAdd(contentPanel, entryPointSelection[type][ep],
				gbc, xpos++, ypos, 1,1, 1,0, 0,0,0,0);
		    gbc.anchor = GridBagConstraints.WEST;
		    Util.gblAdd(contentPanel, entryPointColors[type][ep],
				gbc, xpos++, ypos, 1,1, 1,0, 0,0,0,0);
		} else {
		    xpos += 2;
		}
	    }
	    ypos++;
	}

	sp.validate();
	  
	Panel buttonPanel = new GrayPanel();

	buttonPanel.setLayout(new FlowLayout());
	bAll   = new Button("Select All");
	bClear = new Button("Clear All");
	bSave = new Button("Save and Exit");
	bCancel = new Button("Cancel");
	
	bAll.addActionListener(this);
	bClear.addActionListener(this);
	bSave.addActionListener(this);
	bCancel.addActionListener(this);
	
	buttonPanel.add(bAll);
	buttonPanel.add(bClear);
	buttonPanel.add(bSave);
	buttonPanel.add(bCancel);
	
	gbc.fill = GridBagConstraints.BOTH;
	Util.gblAdd(mainPanel, displayPanel, gbc, 0,0, 1,1, 1,1, 4,4,2,4);
	Util.gblAdd(mainPanel, buttonPanel,  gbc, 0,1, 1,1, 1,0, 2,4,4,4);

	layoutComplete = true;
    }  

    private boolean someExists(int epIdx) {
	for (int type=0; type<numTypes; type++) {
	    if (existsArray[type][epIdx]) {
		return true;
	    }
	}
	return false;
    }
}
