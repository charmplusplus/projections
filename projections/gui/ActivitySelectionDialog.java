// UNUSED FILE

//package projections.gui;
//
//import java.awt.*;
//import java.awt.event.*;
//import javax.swing.*;
//
//import projections.analysis.*;
//
///**
// *  ActivitySelectionDialog
// *  written by Chee Wai Lee
// *  7/14/2004
// */
//public class ActivitySelectionDialog extends ColorWindowFrame 
//   implements ActionListener
//{
//    private ColorSelectWindow colorSelectWindow;
//    private ColorSelectable callbackTarget;
//    private ActivityManager activityManager;
//
//    private JCheckBox[]   activitySelection;
//    private ColorPanel[]  activityColors;
//
//    // for specifying which EP's color panel is selected in order to apply
//    // color changes through ColorSelectWindow.
//    private ColorPanel selectedCP;
//
//    private JTabbedPane displayPanel;
//
//    // control panel buttons
//    private JPanel controlPanel;
//    private JButton bAll, bClear, bSave, bCancel;
//
//    private int numActivities;
//   
//    private boolean stateArray[];  // user selection choices
//    private boolean existsArray[]; // availability filter
//
//    // flag
//    private boolean layoutComplete = false;
//
//    /**
//     *  This is a wrapper constructor for a non-callback based dialog.
//     */
//    public ActivitySelectionDialog(Frame parent, ActivityManager manager)
//    {
//	this(parent, null, manager);
//    }
//
//    public ActivitySelectionDialog(Frame parent, 
//				   ColorSelectable callbackTarget,
//				   ActivityManager manager)
//    {
//	super(parent);
//	this.callbackTarget = callbackTarget;
//	this.activityManager = manager;
//
//	activityColors = new ColorPanel[numActivities];
//	activitySelection = new JCheckBox[numActivities];
//	
//	addWindowListener(new WindowAdapter()
//	    {                    
//		public void windowClosing(WindowEvent e)
//		{
//		    closeDialog();
//		}
//	    });
//    }
//    
//    public void showDialog() {
//	if (layoutComplete) {
//	    setVisible(true);
//	} else {
//	    setBackground(Color.lightGray);
//	    setTitle("Select Display Items");
//	    setLocation(0, 0);
//	
//	    createLayout();
//	    pack();
//	    setVisible(true);
//	}
//    }
//    
//    private void closeDialog()
//    {
//	// this is essentially a cancellation command.
//
//	setVisible(false);
//	// reset the values such that it is consistent with the data display
//	// associated with the toolkit.
//	for (int type=0; type<numTypes; type++) {
//	    for (int ep=0; ep<numActivities; ep++) {
//		if (existsArray[ep]) {
//		    activitySelection[ep].setState(stateArray[ep]);
//		    activityColors[ep].setColor(colorArray[ep]);
//		}
//	    }
//	} 
//	dispose();
//    }   
//    
//    private void createLayout()
//    {
//	JPanel mainPanel = new Panel();
//	getContentPane().add("Center", mainPanel);
//	mainPanel.setBackground(Color.gray);
//	  
//	GridBagLayout      gbl = new GridBagLayout();
//	GridBagConstraints gbc = new GridBagConstraints();
//	  
//	mainPanel.setLayout(gbl);
//	  
//	gbc.fill = GridBagConstraints.BOTH;
//	gbc.anchor = GridBagConstraints.CENTER;
//	gbc.insets = new Insets(0, 2, 0, 2);
//
//	Panel buttonPanel = new GrayPanel();
//
//	buttonPanel.setLayout(new FlowLayout());
//	bAll   = new JButton("Select All");
//	bClear = new JButton("Clear All");
//	bSave = new JButton("Save and Exit");
//	bCancel = new JButton("Cancel");
//	
//	bAll.addActionListener(this);
//	bClear.addActionListener(this);
//	bSave.addActionListener(this);
//	bCancel.addActionListener(this);
//	
//	buttonPanel.add(bAll);
//	buttonPanel.add(bClear);
//	buttonPanel.add(bSave);
//	buttonPanel.add(bCancel);
//	
//	gbc.fill = GridBagConstraints.BOTH;
//	Util.gblAdd(mainPanel, displayPanel, gbc, 0,0, 1,1, 1,1, 4,4,2,4);
//	Util.gblAdd(mainPanel, buttonPanel,  gbc, 0,1, 1,1, 1,0, 2,4,4,4);
//
//	layoutComplete = true;
//    }  
//
//    private JScrollPane createDisplayPanel()
//    {
//	JScrollPane sp = new JScrollPane();// contains the inner display area
//
//	for (int type=0; type<numTypes; type++) {
//	    for (int ep=0; ep<numActivities; ep++) {
//		if (existsArray[ep]) {
//		    activitySelection[ep] = new JCheckBox();
//		    activitySelection[ep].setState(stateArray[ep]);
//		    activityColors[ep] = 
//			new ColorPanel(type, ep, colorArray[ep]);
//		    activityColors[ep].addActionListener(this);
//		}
//	    }
//	}
//
//	GridBagLayout      gbl = new GridBagLayout();
//	GridBagConstraints gbc = new GridBagConstraints();
//	  
//	JPanel display = new JPanel();
//	display.setLayout(gbl);
//	  
//	gbc.fill = GridBagConstraints.BOTH;
//	gbc.insets = new Insets(0, 2, 0, 2);
//
//	Label typeLabels[] = new Label[numTypes];
//	for (int type=0; type<numTypes; type++) {
//	    typeLabels[type] = new Label(typeLabelStrings[type], Label.CENTER);
//	}
//   
//	sp.add(display);
//	  
//	gbc.fill = GridBagConstraints.BOTH;
//	gbc.anchor = GridBagConstraints.CENTER;
//	gbc.insets = new Insets(0, 2, 0, 2);
//
//	// place column headers
//	for (int type=0; type<numTypes; type++) {
//	    Util.gblAdd(contentPanel, typeLabels[type], gbc,
//			1+2*type,0, 2,1, 1,0);
//	}
//	  
//	int ypos = 1;  // starting at line 2 for EP data
//	Label tempLabel;
//
//	for (int ep=0; ep<numActivities; ep++) {
//	    int xpos = 0;  // xposition for each line.
//	    // print EP label if at least one type of the event exists
//	    if (someExists(ep)) {
//		gbc.anchor = GridBagConstraints.CENTER;
//		gbc.fill = GridBagConstraints.BOTH;
//		tempLabel = new Label(entryNames[ep], Label.RIGHT);
//		Util.gblAdd(contentPanel, tempLabel, gbc,
//			    xpos++,ypos, 1,1, 1,0, 0,2,0,5);
//	    } else {
//		continue;
//	    }
//	    for(int type=0; type<numTypes; type++) {
//		if (existsArray[ep]) {
//		    gbc.fill = GridBagConstraints.VERTICAL;
//		    gbc.anchor = GridBagConstraints.EAST;
//		    Util.gblAdd(contentPanel, activitySelection[ep],
//				gbc, xpos++, ypos, 1,1, 1,0, 0,0,0,0);
//		    gbc.anchor = GridBagConstraints.WEST;
//		    Util.gblAdd(contentPanel, activityColors[ep],
//				gbc, xpos++, ypos, 1,1, 1,0, 0,0,0,0);
//		} else {
//		    xpos += 2;
//		}
//	    }
//	    ypos++;
//	}
//
//	sp.validate();
//	return display;
//    }
//
//    public void actionPerformed(ActionEvent evt)
//    {
//	if(evt.getSource() instanceof ColorPanel) {   
//	    selectedCP = (ColorPanel)evt.getSource();
//
//	    String selectTitle = "";
//
//	    selectTitle = entryNames[selectedCP.getIndex()];
//	    selectTitle += " : " + 
//		typeLabelStrings[selectedCP.getType()];
//	    JColorChooser colorWindow = new JColorChooser();
//	    Color returnColor =
//		colorWindow.showDialog(this, selectTitle,
//				       selectedCP.getColor());
//	    if (returnColor != null) {
//		selectedCP.setColor(returnColor);
//	    }
//	} else if(evt.getSource() instanceof JButton) {
//	    JButton b = (JButton) evt.getSource();
//	  
//	    if (b == bAll || b == bClear) {
//		boolean dest=(b==bAll);
//		for (int type=0; type<numTypes; type++) {
//		    for (int ep=0; ep<numActivities; ep++) {
//			if (existsArray[ep]) {
//			    activitySelection[ep].setState(dest);
//			} 
//		    }   
//		}
//	    } else if (b == bCancel) {
//		closeDialog();   
//	    }  else if (b == bSave) {
//		for (int type=0; type<numTypes; type++) {
//		    for (int ep=0; ep<numActivities; ep++) {
//			if (existsArray[ep]) {
//			    colorArray[ep] =
//				activityColors[ep].getColor();
//			    stateArray[ep] =
//				activitySelection[ep].getState();
//			}
//		    }
//		}
//		if (callbackTarget != null) {
//		    callbackTarget.applyDialogColors();
//		}
//		hide();
//	    }
//	}           
//    }   
//
//    private boolean someExists(int epIdx) {
//	for (int type=0; type<numTypes; type++) {
//	    if (existsArray[epIdx]) {
//		return true;
//	    }
//	}
//	return false;
//    }
//}
