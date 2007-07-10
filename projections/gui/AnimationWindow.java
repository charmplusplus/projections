package projections.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class AnimationWindow extends ProjectionsWindow
   implements ActionListener, AdjustmentListener
{
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;

    private AnimationColorBarPanel colorbarPanel;
    AnimationDisplayPanel  displayPanel;
    AnimationWindow thisWindow;
    
    JMenuBar mbar = new JMenuBar();

    private Button bPlusOne, bMinusOne, bAuto;
    private Button setRanges;
    private JTimeTextField delayField;
    private Panel statusPanel;
    private Panel titlePanel; 
    
    //private Label lTitle, lStatus;
    private Label lTitle, lStatus, lDelay;
    Scrollbar slider;
    
    int redrawDelay; //Real time between frames (ms)
    boolean keepAnimating;
    private AnimateThread thread;

    boolean layoutComplete = false;

    // basic parameter variables consistent with IntervalRangeDialog
    public long intervalSize;
    public OrderedIntList validPEs;
    public long startTime;
    public long endTime;
    
    class AnimateThread extends Thread
    {
	public AnimateThread()
	{
	    keepAnimating=true;
	}
   
	public void run()
	{
	    long finish = redrawDelay + System.currentTimeMillis();
	    while (keepAnimating) {
		long timeLeft = finish - System.currentTimeMillis();
		if (timeLeft > 0) {
		    try { //Give other threads a chance
			sleep(timeLeft);
		    } catch (InterruptedException E) {
		    	// ignore
		    }
		} else { //Advance to next frame
		    finish += redrawDelay;
		    changeCurI(displayPanel.getCurI() + 1);
		}   
	    }
	}  
    }       

    void windowInit() {
	// acquire data from Analysis
	intervalSize = 1000; // default to 1ms
	validPEs = MainWindow.runObject[myRun].getValidProcessorList();
        startTime = 0;
        endTime = MainWindow.runObject[myRun].getTotalTime();
    }
  
    public AnimationWindow(MainWindow mainWindow, Integer myWindowID)
    {
	super(mainWindow, myWindowID);
	setBackground(MainWindow.runObject[myRun].background);
	setTitle("Projections Animation - " + MainWindow.runObject[myRun].getFilename() + ".sts");
          
	thisWindow = this;
	showDialog();
    }   

    public void showWindow() {
	// do nothing for now
    }

    public void showDialog() {
	if (dialog == null) {
            dialog = new IntervalRangeDialog(this, "Select Animation Range");
	} else {
	    setDialogData();
	}
        dialog.displayDialog();
        if (!dialog.isCancelled()){
            getDialogData();
            final SwingWorker worker =  new SwingWorker() {
                    public Object construct() {
			if (thisWindow.layoutComplete) {
			    if (dialog.isModified()) {
				displayPanel.setParameters();
				slider.setValues(0, 1, 0,
						 displayPanel.getNumI());
			    }
			} else {
			    createMenus();
			    createLayout();
			}
                        return null;
                    }
                    public void finished() {
			if (thisWindow.layoutComplete) {
			    pack();
			    thisWindow.setVisible(true);
			} else {
			    thisWindow.repaint();
			}
                    }
                };
            worker.start();
        }
    }

    public void getDialogData() {
	IntervalRangeDialog dialog = (IntervalRangeDialog)this.dialog;
	intervalSize = dialog.getIntervalSize();
	validPEs = dialog.getValidProcessors();
        startTime = dialog.getStartTime();
        endTime = dialog.getEndTime();
    }

    public void setDialogData() {
	IntervalRangeDialog dialog = (IntervalRangeDialog)this.dialog;
	dialog.setIntervalSize(intervalSize);
	dialog.setValidProcessors(validPEs);
	dialog.setStartTime(startTime);
	dialog.setEndTime(endTime);
	super.setDialogData();
    }

    public void actionPerformed(ActionEvent evt)
    {
	if (evt.getSource()==delayField) {
	    // System.out.println("event on delay field");
	    // Get redraw in milliseconds
	    redrawDelay=(int)(delayField.getValue()/1000); 
	}else if(evt.getSource() instanceof Button) {
	    Button b = (Button)evt.getSource();
	    if (b == bPlusOne) {
		changeCurI(displayPanel.getCurI() + 1);
	    } else if (b == bMinusOne) {
		changeCurI(displayPanel.getCurI() - 1);
	    } else if (b == bAuto) {
		if (b.getLabel().equals("Auto")) {
		    b.setLabel("Stop");
		    keepAnimating=true;
		    thread = new AnimateThread();
		    thread.start();
		} else { // label is "Stop"
		    b.setLabel("Auto");
		    if (thread != null && thread.isAlive()) {
			keepAnimating=false;
			thread = null;
			changeCurI(displayPanel.getCurI());
		    }   
		}
	    } else if (b == setRanges) {
		showDialog();
	    }
        } else if (evt.getSource() instanceof JMenuItem) {
            String arg = ((JMenuItem)evt.getSource()).getText();
            if (arg.equals("Close")) {
                close();
            } else if(arg.equals("Select Processors")) {
                showDialog();
            }
        }
    }   
    
    public void adjustmentValueChanged(AdjustmentEvent e){
    	if(slider.getValueIsAdjusting()){
	    changeCurI(slider.getValue());
	}
    }

    void changeCurI(int i)
    {
	displayPanel.setCurI(i);
	setTitleInfo(displayPanel.getCurI()); 
	slider.setValue(i);
    }   

    protected void createMenus(){
        mbar.add(Util.makeJMenu("File", new Object[]
            {
                "Select Processors",
                null,
		"Close"
            },
                                null, this));
        mbar.add(Util.makeJMenu("Tools", new Object[]
            {
                "Change Colors",
            },
                                null, this));
        mbar.add(Util.makeJMenu("Help", new Object[]
            {
                "Index",
		"About"
            },
                                null, this));
        setJMenuBar(mbar);
    }

    void createLayout()
    {
	Panel mainPanel     = new Panel();
	titlePanel    = new Panel();
	statusPanel   = new Panel();
	Panel controlPanel  = new Panel();
	colorbarPanel = new AnimationColorBarPanel();
	displayPanel  = new AnimationDisplayPanel(this);
          
	bPlusOne  = new Button(">>");
	bMinusOne = new Button("<<");
	bAuto     = new Button("Auto");
          
	bPlusOne.addActionListener(this);
	bMinusOne.addActionListener(this);
	bAuto.addActionListener(this);
          
	redrawDelay=500;  // default delay value
	lDelay = new Label("Frame Refresh Delay:", Label.CENTER);
	delayField = new JTimeTextField("500 ms", 8);
	delayField.addActionListener(this);
	
	//sharon implementing slider bar
	slider = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0,
	displayPanel.getNumI());
	slider.addAdjustmentListener(this);
          
	setRanges = new Button("Set Ranges");
	setRanges.addActionListener(this);

	titlePanel.setBackground(MainWindow.runObject[myRun].background);
	titlePanel.setForeground(MainWindow.runObject[myRun].foreground);
	Font titleFont = new Font("SansSerif", Font.BOLD, 16);
	lTitle = new Label("", Label.CENTER);
	lTitle.setFont(titleFont);
	setTitleInfo(0);
	titlePanel.add(lTitle);
          
	lStatus = new Label("");
	statusPanel.add(lStatus, "Center");
	statusPanel.setBackground(Color.lightGray);
          
	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();
	gbc.fill = GridBagConstraints.BOTH;
          
	controlPanel.setLayout(gbl);
	Util.gblAdd(controlPanel, slider,     gbc, 0,0, 6,1, 1,1);
	Util.gblAdd(controlPanel, bMinusOne,  gbc, 0,1, 1,1, 1,1);
	Util.gblAdd(controlPanel, bPlusOne,   gbc, 1,1, 1,1, 1,1);
	Util.gblAdd(controlPanel, bAuto,      gbc, 2,1, 1,1, 1,1);
	Util.gblAdd(controlPanel, lDelay,     gbc, 3,1, 1,1, 1,1);
	Util.gblAdd(controlPanel, delayField, gbc, 4,1, 1,1, 1,1);
	Util.gblAdd(controlPanel, setRanges,  gbc, 5,1, 1,1, 1,1);
          
	mainPanel.setBackground(Color.gray);
	mainPanel.setLayout(gbl);
          
	Util.gblAdd(mainPanel,

 titlePanel,    gbc, 0,0, 1,1, 1,0);
	Util.gblAdd(mainPanel, displayPanel,  gbc, 0,1, 1,1, 1,1);
	Util.gblAdd(mainPanel, colorbarPanel, gbc, 0,2, 1,1, 1,0);
	Util.gblAdd(mainPanel, statusPanel,   gbc, 0,3, 1,1, 1,0);
	Util.gblAdd(mainPanel, controlPanel,  gbc, 0,4, 1,1, 1,0); 
          
	getContentPane().add(mainPanel,"Center");
	layoutComplete = true;
    }   

    public void setStatusInfo(int p, int i, int u)
    {
	String status;
	if (p < 0) {
	    status = "";
	} else {
	    status = "Processor " + p + ": Usage = " + u + 
		"% at " + U.t(startTime + i*intervalSize);
	}
	lStatus.setText(status);
	lStatus.invalidate();
	statusPanel.validate();
    }   

    public void setTitleInfo(int i)
    {
	String title = "Processor Usage at " + 
	    U.t(startTime + i*intervalSize) +
	    "(" + U.t(intervalSize) + " step)";
	lTitle.setText(title);
	lTitle.invalidate();
	titlePanel.validate();
    }   
}
