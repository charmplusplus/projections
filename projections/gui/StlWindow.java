package projections.gui;

/**
 * Small Time Line (Stl) Display Panel
 * Orion Sky Lawlor, olawlor@acm.org, 2/9/2001
 *
 * A Stl compresses an entire parallel run into a single
 * image by coding processor utilization as color.
 * Since images are assembled pixel-by-pixel, this is
 * much faster than a timeline.
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import projections.misc.*;

public class StlWindow extends ProjectionsWindow
    implements MouseListener, ActionListener, ScalePanel.StatusDisplay, 
	       ItemListener
{
    private ScaleSlider hor,ver;
    private ScalePanel scalePanel;
    private StlPanel stl;
    private Label status;
    private boolean okorcancelled;

    // Modified to display data by entry method color. Mode panel.
    public static final int NUM_MODES = 2;
    public static final int MODE_UTILIZATION = 0;
    public static final int MODE_EP = 1;

    private int mode = MODE_UTILIZATION;

    private JPanel modePanel;
    private ButtonGroup modeGroup;
    private JRadioButton utilizationMode;
    private JRadioButton epMode;

    private ColorMap utilColorMap;

    private StlWindow thisWindow;

    // parameter variables
    public OrderedIntList validPEs;
    public long startTime;
    public long endTime;

    public StlWindow(MainWindow mainWindow, Integer myWindowID)
    {
	super(mainWindow, myWindowID);
	thisWindow = this;

	setBackground(Color.black);
	setForeground(Color.lightGray);
	setTitle("Projections-- Overview");
	
	createMenus();
	createLayout();
	pack();
	showDialog();
    }
   
//     public void basicOverview(){
// 	try{
// 	    thisWindow = this;

// 	    setBackground(Color.black);
// 	    setForeground(Color.lightGray);
	
// 	    createMenus();
// 	    createLayout();
// 	    pack();

// 	    OrderedIntList vpe = Analysis.getValidProcessorsList();
// 	    thisWindow.setStlPanelData();
// 	    stl.setData(vpe, 0, Analysis.getTotalTime());
// 	} catch (Exception e){
// 	    System.err.println("StlWindow->basicOverview: exception occured\n");
// 	    e.printStackTrace();
// 	}
//     } 
    
    private void createLayout()
    {
	GridBagLayout      gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();
	
	Container windowPane = this.getContentPane();
	windowPane.setLayout(gbl);
	
	JPanel displayPanel = new JPanel();
	displayPanel.setLayout(gbl);
	
	status = new Label("");
	status.setBackground(Color.black);
	status.setForeground(Color.lightGray);
	
	hor=new ScaleSlider(Scrollbar.HORIZONTAL);
	hor.addMouseListener(this);
	
	ver=new ScaleSlider(Scrollbar.VERTICAL);
	ver.addMouseListener(this);
	
	stl=new StlPanel();
	scalePanel=new ScalePanel(hor,ver,stl);

	gbc.fill = GridBagConstraints.BOTH;
	Util.gblAdd(displayPanel, scalePanel, gbc, 0,0, 1,1, 1,1);
	gbc.fill = GridBagConstraints.VERTICAL;
	Util.gblAdd(displayPanel, ver,        gbc, 1,0, 1,1, 0,1);
	gbc.fill = GridBagConstraints.HORIZONTAL;
	Util.gblAdd(displayPanel, hor,        gbc, 0,1, 1,1, 1,0);
	Util.gblAdd(displayPanel, status,     gbc, 0,2, 1,1, 1,0);
	
	// create mode panel
	modePanel = new JPanel();
	modeGroup = new ButtonGroup();
	utilizationMode = new JRadioButton("Utilization", true);
	utilizationMode.addItemListener(this);
	epMode = new JRadioButton("By EP Colors", false);
	epMode.addItemListener(this);
	modeGroup.add(utilizationMode);
	modeGroup.add(epMode);
	  
	gbc.fill = GridBagConstraints.HORIZONTAL;
	Util.gblAdd(modePanel, utilizationMode, gbc, 0,0, 1,1, 1,1, 1,1,1,1);
	Util.gblAdd(modePanel, epMode,          gbc, 1,0, 1,1, 1,1, 1,1,1,1);
	
	gbc.fill = GridBagConstraints.BOTH;
	Util.gblAdd(windowPane, displayPanel, gbc, 0,0, 1,1, 1,1, 1,1,1,1);
	Util.gblAdd(windowPane, modePanel,    gbc, 0,1, 1,1, 1,0, 1,1,1,1);
	scalePanel.setStatusDisplay(this);

	// Establishing the Utilization-only color map. 
	// This never changes from the get-go, so there's no reason (like 
	// in the previous code) to keep resetting it.
	utilColorMap = new ColorMap();
	utilColorMap.addBreak(0,0, 0,55, 70,255, 0,0); //Blue to red
	utilColorMap.addBreak(70,255, 0,0, 100,255, 255,255);//red to white
	// Overflow-- green. Should not happen for utilization.
	utilColorMap.addBreak(101,0, 255,0, 255,0, 255,0); 
	stl.setColorMap(utilColorMap);
    }  

    private void setStlPanelData(){
	double horSize, verSize;
	if (validPEs == null) {
	    horSize=Analysis.getTotalTime();
	    verSize=Analysis.getNumProcessors();
	} else {	
	    horSize = endTime-startTime;
	    if(horSize <= 0)
		horSize = Analysis.getTotalTime();
	    verSize = (double)validPEs.size();
	}	 
	scalePanel.setScales(horSize,verSize);
	
	double hMin=scalePanel.toSlider(1.0/horSize);
	double hMax=scalePanel.toSlider(0.01);//0.1ms fills screen
	hor.setMin(hMin); hor.setMax(hMax);
	hor.setValue(hMin);
	hor.setTicks(Math.floor(hMin),1);
	
	double vMin=scalePanel.toSlider(1.0/verSize);
	double vMax=scalePanel.toSlider(1.0);//One processor fills screen
	ver.setMin(vMin); ver.setMax(vMax);
	ver.setValue(vMin);
	ver.setTicks(Math.floor(vMin),1);
    }
   
    private void createMenus()
    {
        MenuBar mbar = new MenuBar();
	
        mbar.add(Util.makeMenu("File", new Object[]
	    {
		"Close"
	    }, this));
        mbar.add(Util.makeMenu("Modify", new Object[]
	    {
		"Set Range"
	    }, this));
	setMenuBar(mbar);
    } 



    public void showDialog()
    {
	try {
	    if (dialog == null) {
		dialog = 
		    new RangeDialog((ProjectionsWindow) this,
				    "Select Range");
	    } else {
		setDialogData();
	    }
	    dialog.displayDialog();
	    if (!dialog.isCancelled()) {
		getDialogData();
		final SwingWorker worker = new SwingWorker() {
			public Object construct() {
			    thisWindow.setVisible(false);
			    thisWindow.setStlPanelData();
			    stl.setData(validPEs,startTime,endTime); 
			    return null;
			}
			public void finished() {
			    thisWindow.setVisible(true);
			    thisWindow.repaint();
			}
		    };
		worker.start();
	    }
	} catch (Exception e) { 
	    e.printStackTrace();
	}
    }
   
    public void showWindow() {
	// do nothing for now
    }

    public void actionPerformed(ActionEvent evt)
    {
	if (evt.getSource() instanceof MenuItem) {
	    MenuItem mi = (MenuItem)evt.getSource();
	    String arg = mi.getLabel();
	    if(arg.equals("Close"))  {
		close();
	    }
	    if (arg.equals("Set Range")) {
		showDialog();
	    }
	}
    }  

    public void itemStateChanged(ItemEvent evt) {
	if (evt.getStateChange() == ItemEvent.SELECTED) {
	    JRadioButton button = (JRadioButton)evt.getItemSelectable();
	    if (button == utilizationMode) {
		mode = MODE_UTILIZATION;
	    } else if (button == epMode) {
		mode = MODE_EP;
	    }
	    // handle the effects of a mode change
	    stl.setMode(mode);
	}
    }
    
    public void mouseClicked(MouseEvent evt) {
    }

    public void mouseEntered(MouseEvent evt) {
	Object src=evt.getComponent();
	if (src==hor) setStatus("Click or drag to set the horizontal zoom");
	if (src==ver) setStatus("Click or drag to set the vertical zoom");
    }

    public void mouseExited(MouseEvent evt) {
	setStatus("");//Clear the old message
    }

    public void mousePressed(MouseEvent evt) {
    }

    public void mouseReleased(MouseEvent evt) {
    }

    public void setStatus(String msg) {
   	status.setText(msg);
    }
   
    public void getDialogData() {
	validPEs = dialog.getValidProcessors();
	startTime = dialog.getStartTime();
	endTime = dialog.getEndTime();
    }

    public void setDialogData() {
	dialog.setValidProcessors(validPEs);
	dialog.setStartTime(startTime);
	dialog.setEndTime(endTime);
	super.setDialogData();
    }
}
