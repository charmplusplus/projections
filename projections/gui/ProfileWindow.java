package projections.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.*;

import projections.analysis.*;

public class ProfileWindow extends ProjectionsWindow
    implements ActionListener, AdjustmentListener, ColorSelectable
{ 
    private static final int NUM_SYS_EPS = 3;
    
    private NoUpdatePanel        mainPanel, displayPanel;
    private Panel                labelCanvas2, titlePanel;
    private ProfileLabelCanvas   labelCanvas;
    private ProfileAxisCanvas    axisCanvas;
    private ProfileDisplayCanvas displayCanvas;
    private ProfileColorWindow   colorWindow;
    private Label lTitle, lTitle2;
    private Scrollbar HSB, VSB;
    private ProfileData data;
    private FloatTextField xScaleField, yScaleField;
    private Button bDecreaseX, bIncreaseX, bResetX;
    private Button bDecreaseY, bIncreaseY, bResetY;
    private Button bColors;
    private Button bPieChart;

    // a boolean variable that indicates if the colors have been set.
    // If so, we simply want to preserve the old settings and allow the
    // user to change them without forcing them back to the old values.
    // Right now, still a very ugly hack.
    private boolean colorsSet = false;
    private Color[][] colors;
    private ProfileObject[][] poArray;
    private float xscale=1, yscale=1;
    private float[][] avg;
    private float thresh;
    private int avgSize;
    
    private PieChartWindow pieChartWindow;

    private EntrySelectionDialog entryDialog;
   
    class NoUpdatePanel extends Panel
    {
	public void update(Graphics g)
	{
	    paint(g);
	}
    }

    public ProfileWindow(MainWindow parentWindow, Integer myWindowID)
    {
	super(parentWindow, myWindowID);
	
	addComponentListener(new ComponentAdapter()
	    {
		public void componentResized(ComponentEvent e)
		{
		    if(displayCanvas != null)
			{
			    setCursor(new Cursor(Cursor.WAIT_CURSOR));
			    setSizes();
			    setScales();
			    labelCanvas.makeNewImage();
			    axisCanvas.makeNewImage();
			    labelCanvas2.invalidate();
			    mainPanel.validate();
			    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}   
		}
	    });
	  
	setBackground(Color.lightGray);
	
	data = new ProfileData(this);
	
	setTitle("Projections Usage Profile");
	
	CreateMenus();
	CreateLayout();
	  
	pack();
	showDialog(); 
    }   

    public void actionPerformed(ActionEvent evt)
    {
	if (evt.getSource() instanceof Button) {
	    setCursor(new Cursor(Cursor.WAIT_CURSOR));
	    
	    Button b = (Button)evt.getSource();
	    
	    if (b == bDecreaseX || b == bIncreaseX || b == bResetX) {
		if (b == bDecreaseX) {
		    xscale = (float)((int)(xscale * 4)-1)/4;
		    if(xscale < 1.0)
			xscale = (float)1.0;
		} else if (b == bIncreaseX) {
		    xscale = (float)((int)(xscale * 4)+1)/4;
		} else if (b == bResetX) {
		    xscale = (float)1.0;
		}
		xScaleField.setText("" + xscale);
		setScales();
		labelCanvas.makeNewImage();
	    } else if (b == bDecreaseY || b == bIncreaseY || b == bResetY) {
		if (b == bDecreaseY) {
		    yscale = (float)((int)(yscale * 4)-1)/4;
		    if (yscale < 1.0)
			yscale = (float)1.0;
		} else if (b == bIncreaseY) {
		    yscale = (float)((int)(yscale * 4)+1)/4;
		} else if (b == bResetY) {
		    yscale = (float)1.0;
		}
		yScaleField.setText("" + yscale);
		setScales(); 
		axisCanvas.makeNewImage(); 
	    } else if (b == bPieChart) {
		pieChartWindow = 
		    new PieChartWindow(parentWindow, avg[0], 
				       avg[0].length, thresh, colors[0]);
	    } else if (b == bColors) {
		int noEPs = Analysis.getNumUserEntries();
		if (entryDialog == null) {
		    String typeLabelStrings[] = {"Entry Points"};
		    
		    boolean existsArray[][] = 
			new boolean[1][noEPs+NUM_SYS_EPS];
		    for (int i=0; i<noEPs+NUM_SYS_EPS; i++) {
			existsArray[0][i] = true;
		    }
		    
		    boolean stateArray[][] =
			new boolean[1][noEPs+NUM_SYS_EPS];
		    for (int i=0; i<noEPs+NUM_SYS_EPS; i++) {
			stateArray[0][i] = true;
		    }
		    
		    String entryNames[] =
			new String[noEPs+NUM_SYS_EPS];
		    for (int i=0; i<noEPs; i++) {
			entryNames[i] =
			    Analysis.getEntryName(i);
		    }
		    // cannot seem to avoid a hardcode
		    entryNames[noEPs] = "Pack Time";
		    entryNames[noEPs+1] = "Unpack Time";
		    entryNames[noEPs+2] = "Idle Time";
		    
		    entryDialog = 
			new EntrySelectionDialog(this, this,
						 typeLabelStrings,
						 stateArray, colors,
						 existsArray, entryNames);
		}
		entryDialog.showDialog();
	    }
	    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));   
	} else if (evt.getSource() instanceof MenuItem) {
	    String arg = ((MenuItem)evt.getSource()).getLabel();
	    if (arg.equals("Close")) {
		close();
	    } else if(arg.equals("Select Processors")) {
		showDialog();
	    }
	} else if (evt.getSource() instanceof FloatTextField) {
	    setCursor(new Cursor(Cursor.WAIT_CURSOR));
	    FloatTextField ftf = (FloatTextField)evt.getSource();
	    
	    if (ftf == xScaleField) {
		xscale = xScaleField.getValue();
		setScales();
		labelCanvas.makeNewImage();
	    } else if(ftf == yScaleField) {
		yscale = yScaleField.getValue();
		setScales();
		axisCanvas.makeNewImage();
	    } 
	    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));  
	}                  
    }   

    /**
     *  **CW** THIS IS A MAJOR CODE HACK!!!
     *  ONLY Usage Profile uses callbacks for color selection so far.
     */
    public void applyDialogColors() {
	MakePOArray(data.begintime, data.endtime);
    }
    
    public void adjustmentValueChanged(AdjustmentEvent evt)
    {
	Scrollbar sb = (Scrollbar)evt.getSource();
	displayCanvas.setLocation(-HSB.getValue(), -VSB.getValue());
	
	if (sb == HSB) {
	    labelCanvas.repaint();
	} else if (sb == VSB) {
	    axisCanvas.repaint();
	}  
    }   
    
    private void CreateLayout()
    {
	//// MAIN PANEL
	
	mainPanel    = new NoUpdatePanel();
	displayPanel = new NoUpdatePanel();
	
	labelCanvas    = new ProfileLabelCanvas(data);
	labelCanvas2   = new NoUpdatePanel();
	axisCanvas     = new ProfileAxisCanvas(data);
	displayCanvas  = new ProfileDisplayCanvas(data);
	
	HSB = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, 1);
	VSB = new Scrollbar(Scrollbar.VERTICAL, 0, 1, 0, 1);

	mainPanel.setLayout(null);
	mainPanel.setBackground(Color.black);
	mainPanel.add(labelCanvas);
	mainPanel.add(labelCanvas2);
	mainPanel.add(axisCanvas);
	mainPanel.add(displayPanel);
	mainPanel.add(HSB);
	mainPanel.add(VSB);
	  
	displayPanel.setLayout(null);
	displayPanel.add(displayCanvas);
	
	HSB.setBackground(Color.lightGray);
	HSB.addAdjustmentListener(this);
	
	VSB.setBackground(Color.lightGray);
	VSB.addAdjustmentListener(this);
	
	//// BUTTON PANEL
	  
	bColors   = new Button("Change Colors");
	bDecreaseY   = new Button("<<");
	bIncreaseY   = new Button(">>");
	bResetY      = new Button("Reset");
	bDecreaseX   = new Button("<<");
	bIncreaseX   = new Button(">>");
	bResetX      = new Button("Reset");
	bPieChart    = new Button("Pie Chart");
	
	bColors.addActionListener(this);
	bDecreaseY.addActionListener(this);
	bIncreaseY.addActionListener(this);
	bResetY.addActionListener(this);
	bDecreaseX.addActionListener(this);
	bIncreaseX.addActionListener(this);
	bResetX.addActionListener(this);
	bPieChart.addActionListener(this);
	  
	Label lXScale = new Label("X-SCALE: ", Label.CENTER);
	xScaleField   = new FloatTextField(xscale, 5);
	xScaleField.addActionListener(this);
	
	Label lYScale = new Label("Y-SCALE: ", Label.CENTER);
	yScaleField   = new FloatTextField(yscale, 5);
	yScaleField.addActionListener(this);
	
	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();
	
	gbc.fill = GridBagConstraints.BOTH;
	
	Panel buttonPanel = new Panel();
	buttonPanel.setLayout(gbl);
	  
	Util.gblAdd(buttonPanel, bDecreaseY,   gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(buttonPanel, lYScale,      gbc, 1,0, 1,1, 1,1);
	Util.gblAdd(buttonPanel, yScaleField,  gbc, 2,0, 1,1, 1,1);
	Util.gblAdd(buttonPanel, bIncreaseY,   gbc, 3,0, 1,1, 1,1);
	Util.gblAdd(buttonPanel, bResetY,      gbc, 4,0, 1,1, 1,1);
	Util.gblAdd(buttonPanel, new Label("  "), gbc, 5,0, 1,1, 1,1);
	Util.gblAdd(buttonPanel, bDecreaseX,   gbc, 6,0, 1,1, 1,1);
	Util.gblAdd(buttonPanel, lXScale,      gbc, 7,0, 1,1, 1,1);
	Util.gblAdd(buttonPanel, xScaleField, gbc, 8,0, 1,1, 1,1);
	Util.gblAdd(buttonPanel, bIncreaseX,   gbc, 9,0, 1,1, 1,1);
	Util.gblAdd(buttonPanel, bResetX,      gbc, 10,0, 1,1, 1,1);
	Util.gblAdd(buttonPanel, bPieChart,    gbc, 11,0, 1,1, 1,1);
	Util.gblAdd(buttonPanel, bColors,      gbc, 12,0, 1,1, 1,1);

	//// WINDOW
	Panel yLabelPanel = new Panel();
	yLabelPanel.setBackground(Color.black);
	yLabelPanel.setForeground(Color.white);
	Label yLabel = new Label("%", Label.CENTER);
	yLabelPanel.setLayout(gbl);
	Util.gblAdd(yLabelPanel, yLabel, gbc, 0,0, 1,1, 1,1);
	
	titlePanel = new Panel();
	titlePanel.setBackground(Color.black);
	titlePanel.setForeground(Color.white);
	lTitle = new Label("", Label.CENTER);
	lTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
	lTitle2 = new Label("", Label.CENTER);
	lTitle2.setFont(new Font("SansSerif", Font.BOLD, 16));
	titlePanel.setLayout(gbl);
	Util.gblAdd(titlePanel, lTitle, gbc, 0,0, 1,1, 1,1);
	Util.gblAdd(titlePanel, lTitle2, gbc, 0,1, 1,1, 1,1);
	
	JPanel p = new JPanel();
	getContentPane().add(p);
	p.setLayout(gbl);
	Util.gblAdd(p, yLabelPanel, gbc, 0,0, 1,2, 0,1);
	Util.gblAdd(p, titlePanel,  gbc, 1,0, 1,1, 1,0);
	Util.gblAdd(p, mainPanel,   gbc, 1,1, 1,1, 1,1);
	Util.gblAdd(p, buttonPanel, gbc, 0,2, 2,1, 1,0);
    }   

    private void CreateMenus()
    {
	MenuBar mbar = new MenuBar();
	
	mbar.add(Util.makeMenu("File", new Object[]
	    {
		"Select Processors",
		"Print Profile",
		null,
		"Close"
	    },
			       this));                   
	Menu helpMenu = new Menu("Help");
	mbar.add(Util.makeMenu(helpMenu, new Object[]
	    {
		"Index",
		"About"
	    },
			       this)); 
	mbar.setHelpMenu(helpMenu);
	setMenuBar(mbar);                                                     
    }   

    public int getHSBValue()
    {
	return HSB.getValue();
    }   

    public int getVSBValue()
    {
	return VSB.getValue();
    }   

    public void MakePOArray(long bt, long et)
    {
	int numEPs = Analysis.getNumUserEntries();

	// this block sets up the initial colors scheme (which will
	// later be changed based on the number of significant entry
	// methods.
	if (!colorsSet) {
	    colors = new Color[1][numEPs+NUM_SYS_EPS];
	    
	    for (int i=0; i<numEPs; i++) {
		// if the data is an entry method whose color is 
		// found in analysis, then use it.
		colors[0][i] = Analysis.getEntryColor(i);
	    }   
	    // Idle time is White and is placed at the top
	    // Pack time is black (to see the first division between entry
	    // data and non entry data).
	    // Unpack time is orange (to provide the constrast).
	    colors[0][numEPs] = Color.black;
	    colors[0][numEPs+1] = Color.orange;
	    colors[0][numEPs+2] = Color.white;
	}
	displayCanvas.removeAll();

	// extra column is that of the average data.
	data.numPs = data.plist.size()+1;
	poArray = new ProfileObject[data.numPs][];
	
	int numUserEntries = Analysis.getNumUserEntries();
	
	int curPe;
	// why +4 now and not +5?
	// the first row is for entry method execution time the second is for 
	//time spent sending messages in that entry method
	
	avg=new float[2][numUserEntries+NUM_SYS_EPS];
	double avgScale=1.0/data.plist.size();
	
	int poCount=1;
	int progressCount = 0;
	int nEl=data.plist.size();
	
	ProgressMonitor progressBar;
	progressBar = 
	    new ProgressMonitor(Analysis.guiRoot, 
				"Computing Usage Values",
				"", 0, nEl*2);
	progressBar.setProgress(0);
	// **CW** Hack for colors to work - 
	// Profile really should be cleanly rewritten.
	// split the original loop:
	// Phase 1a - compute average work
	// Phase 1b - assign colors based on average work
	// Phase 2 - create profile objects
	
	// *CW* *** New code ****
	// Phase 1a
	data.plist.reset();
	for (int i =0;i<avg[0].length;i++) {
	    avg[0][i] = 0.0f;
	    avg[1][i] = 0.0f;
	}
	while (data.plist.hasMoreElements()) {
	    curPe = data.plist.currentElement();
	    if (!progressBar.isCanceled()) {
		progressBar.setNote("Computing Average : " + curPe);
		progressBar.setProgress(progressCount);
	    } else {
		break;
	    }
	    
	    data.plist.nextElement();
	    
	    // the first row is for entry method execution time 
	    // the second is for time spent sending messages in 
	    // that entry method
	    float cur[][] =
		Analysis.GetUsageData(curPe,bt,et,data.phaselist);
	    for (int i=0;i<avg[0].length && i<cur[0].length;i++) {
		avg[0][i]+=(float)(cur[0][i]*avgScale);
		avg[1][i]+=(float)(cur[1][i]*avgScale);
	    }
	    progressCount++;
	}
	
	// Phase 1b
	progressBar.setNote("Assigning Colors");
	progressBar.setProgress(progressCount);
	Vector sigElements = new Vector();
	// we only wish to compute for EPs
	for (int i=0; i<numEPs; i++) {
	    // anything greater than 5% is "significant"
	    if (avg[0][i]+avg[1][i] > 1.0) {
		sigElements.add(new Integer(i));
	    }
	}
	
	// copy to an array for Color assignment (maybe that should be
	// a new interface for color assignment).
	int sigIndices[] = new int[sigElements.size()];
	for (int i=0; i<sigIndices.length; i++) {
	    sigIndices[i] = ((Integer)sigElements.elementAt(i)).intValue();
	}
	if (!colorsSet) {
	    Color[] newUserColors = 
		Analysis.createColorMap(numEPs, sigIndices);
	    // copy the new Colors into our color array. 
	    // (and let the system colors
	    // remain as they are)
	    for (int i=0; i<newUserColors.length; i++) {
		colors[0][i] = newUserColors[i];
	    }
	    colorsSet = true;
	}
	
	// Phase 2
	data.plist.reset();
	while (data.plist.hasMoreElements()) {
	    curPe = data.plist.currentElement();
	    if (!progressBar.isCanceled()) {
		progressBar.setNote("Reading Entry Point Usage : " +
				    curPe);
		progressBar.setProgress(progressCount);
	    } else {
		break;
	    }
	    data.plist.nextElement();
	    float cur[][]=Analysis.GetUsageData(curPe,bt,et,data.phaselist);
	    usage2po(cur,curPe,poCount++,colors[0]);
	    progressCount++;
	}
	usage2po(avg,-1,0,colors[0]);
	progressBar.close();
	
	String sTitle = "Profile of Usage for Processor";
	if(data.plist.size() > 1)
	    sTitle += "s";
	
	sTitle += " " + data.plist.listToString();
	lTitle.setText(sTitle);
	lTitle.invalidate();
	
	sTitle = "(Time " + U.t(bt) + " - " + U.t(et) + ")";
	lTitle2.setText(sTitle);
	lTitle2.invalidate();
	
	titlePanel.validate();
	
	setSizes();
	setScales();
	labelCanvas.makeNewImage();
	axisCanvas.makeNewImage();
	labelCanvas2.invalidate();
	mainPanel.validate();      		
    }

    private void setScales()
    {
	data.dcw = (int)(xscale * data.vpw);
	data.dch = (int)(yscale * data.vph);
	
	if(xscale > 1)
	    HSB.setVisible(true);
	else
	    HSB.setVisible(false);
	
	if(yscale > 1)
	    VSB.setVisible(true);
	else
	    VSB.setVisible(false);
	
	HSB.setMaximum(data.dcw);
	VSB.setMaximum(data.dch);  
	HSB.setVisibleAmount(data.vpw);
	VSB.setVisibleAmount(data.vph); 
	HSB.setBlockIncrement(data.vpw);
	VSB.setBlockIncrement(data.vph);
	
	displayCanvas.setBounds(0, 0, data.dcw, data.dch);
	
	double hscale = (double)(data.dch - data.offset)/100;
	float width   = (float)(data.dcw - 2*data.offset)/data.numPs;
	int w = (int)(Math.ceil(0.75*width));
	int ow = (int)((width - w)/2);
	  
	if (poArray != null) {
	    for (int p=0; p<data.numPs; p++) {
		int h = data.dch;
		double rem = 0.0;
		if (poArray[p] != null) {
		    for (int i=0; i<poArray[p].length; i++) {
			if (poArray[p][i] != null) {
			    double objhtD = (hscale*poArray[p][i].getUsage());
			    objhtD += rem;
			    
			    int   objht;
			    if(objhtD - (int)objhtD >= 0.5)
				objht = (int)objhtD + 1;
			    else
				objht = (int)objhtD;
			    rem = objhtD - objht;      
			    
			    poArray[p][i].setBounds((int)(width*p+
							  data.offset+ow), 
						    h - objht, w, objht);   
			    h -= objht;
			} else {
			    System.out.println("POARRAY[" + p + 
					       "][" + i + "} IS NULL");
			}      
		    }
		}
	    }
	} 
	displayCanvas.makeNewImage();           
    }   

    private void setSizes()
    {
	int acw, lch, sbh, sbw, mpw, mph, lch2;
	
	mpw = mainPanel.getSize().width;
	mph = mainPanel.getSize().height;
	
	acw  = 50;
	lch  = 30;
	lch2 = 30;
	sbh  = 20;
	sbw  = 20;
	  
	data.vpw = mpw - acw - sbw;
	data.vph = mph - lch - lch2 - sbh;
	
	data.dcw = (int)(xscale * data.vpw);
	data.dch = (int)(yscale * data.vph);
	  
	if(xscale > 1)
	    HSB.setVisible(true);
	else
	    HSB.setVisible(false);
	  
	if(yscale > 1)
	    VSB.setVisible(true);
	else
	    VSB.setVisible(false);
		 
	HSB.setMaximum(data.dcw);
	VSB.setMaximum(data.dch);   
	HSB.setBlockIncrement(data.vpw);
	VSB.setBlockIncrement(data.vph);            
	
	axisCanvas.setBounds   (0,       0,       acw, data.vph);
	displayPanel.setBounds (acw,     0,       data.vpw, data.vph);
	displayCanvas.setBounds(0, 0, data.dcw, data.dch);
	labelCanvas.setBounds  (acw,     data.vph,     data.vpw, lch);
	labelCanvas2.setBounds (acw, data.vph+lch,     data.vpw, lch2);
	
	VSB.setBounds          (mpw-sbw, 0,       sbw, data.vph);
	HSB.setBounds          (acw,     mph-sbh, data.vpw, sbh);
    }   

    public void showDialog()
    {
	if (dialog == null)
	    dialog = new RangeDialog(this, "Usage Profile");
	dialog.displayDialog();
	if (!dialog.isCancelled()) {
	    getDialogData();
	    // **CW** another major code hack!!
	    MakePOArray(data.begintime, data.endtime);
	    setVisible(true);
	}
    }   
    
    public void showWindow() {
	// do nothing for now
    }
    
    public void getDialogData() {
	data.plist = dialog.getValidProcessors();
	data.pstring = dialog.getValidProcessorString();
	data.begintime = dialog.getStartTime();
	data.endtime = dialog.getEndTime();
    }

    //Convert a usage profile (0..numUserEntries+4-1) to a po
    private void usage2po(float usg[][],int curPe,int poNo,Color[] colors)
    {
	int numUserEntries = Analysis.getNumUserEntries();
	String[][] names = Analysis.getEntryNames();
	int i,poindex=0,poLen=0;
	
	float thresh=0.01f;//Percent below which to ignore
	for(i=0;i<usg[0].length;i++){
	    if (usg[0][i]>thresh) 
		poLen++;
	    if (usg[1][i]>thresh) 
		poLen++;
	}
	//Drawing the entry point execution time
	poArray[poNo]=new ProfileObject[poLen];
	for (i=0; i<usg[0].length; i++) {
	    float usage = usg[0][i];
	    if (usage<=thresh) continue; //Skip this one-- it's tiny
	    int   entry = i;
	    String name;
	    if(entry < numUserEntries)
		name = names[entry][1] + "::" + names[entry][0];
	    else if(entry == numUserEntries+2)
		name = "IDLE";
	    else if(entry == numUserEntries)
		name = "PACKING";
	    else if(entry == numUserEntries+1)
		name = "UNPACKING";
	    else 
		break;
	    poArray[poNo][poindex] = new ProfileObject(usage, name, curPe);
	    displayCanvas.add(poArray[poNo][poindex]);
	    poArray[poNo][poindex].setForeground(colors[entry]);
	    poindex++;
	}
	if (Analysis.getVersion() > 4.9) {
	    //Drawing the entry point message sendTime
	    for (i=0; i<usg[1].length; i++) {
		float usage = usg[1][i];
		if (usage<=thresh) continue; //Skip this one-- it's tiny
		int   entry = i;
		String name;
		if (entry < numUserEntries) {
		    name = "Message Send Time: " + names[entry][1] + 
			"::" + names[entry][0];
		} else if (entry == numUserEntries+2) {
		    name = "Message Send Time: "+"IDLE";
		} else if (entry == numUserEntries) {
		    name = "Message Send Time: "+"PACKING";
		} else if(entry == numUserEntries+1) {
		    name = "Message Send Time: "+"UNPACKING";
		} else {
		    break;
		}
		poArray[poNo][poindex] = new ProfileObject(usage, name, curPe);
		displayCanvas.add(poArray[poNo][poindex]);
		poArray[poNo][poindex].setForeground(colors[entry]);
		poindex++;
	    }
	}
    }
}
