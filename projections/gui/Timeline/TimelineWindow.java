package projections.gui.Timeline;

import java.awt.BorderLayout;

import javax.swing.ToolTipManager;

import projections.gui.MainWindow;
import projections.gui.ProjectionsWindow;
 


/**
 * The main window for the Timeline Projections Tool
 * 
 * This window uses:
 * 		a ScrollingPanel for the graphical portion of the display
 * 		a WindowControls panel for the buttons and checkboxes and JLabels
 * 
 * Many of the decisions about the rendering are stored in 'data'
 * 
 * 
 * 
 * @author idooley2
 *
 */
public class TimelineWindow extends ProjectionsWindow implements MainHandler {

	private static final long serialVersionUID = 1L;

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;

	
	/** The panel that ties together three other panels with some scrollbars
	 * @note internally it contains a JScrollPane with a custom layout manager */
	public ScrollingPanel scrollingPanel;
	
	/** The panel on the left that displays strings like "PE 0 (20%,40%)" */
	public LabelPanel labelPanel;
	/** The panel on top that draws a scale for the time dimension */
	public LayeredPanel axisPanel; 
	/** The panel that draws the main portion of the window, the timelines */
	public MainPanel mainPanel;
	
		
	/** The JPanel containing all the buttons and labels */
	public WindowControls controls;
	
	/** A reference to this object for use by event listener inner classes */
	TimelineWindow thisWindow;

	/** A structure that stores the information necessary to render everything */
    Data data;

	
    /** called when the display must be redrawn, sometimes as a callback from data object */
	public void refreshDisplay(boolean doRevalidate){
		System.out.println("TimelineWindow refreshDisplay()");

		// Set the values from the buttons
		if(data.selectionValid()){
			controls.setSelectedTime(data.leftSelectionTime(),data.rightSelectionTime());
		} else {
			controls.unsetSelectedTime();
		}
		
		
		if(data.highlightValid()){
			controls.setHighlightTime(data.getHighlightTime());
		} else {
			controls.unsetHighlightTime();
		}
		
		controls.updateScaleField();
		
		// Revalidate/Repaint
		scrollingPanel.refreshDisplay(doRevalidate);
	}
	
	

	/** WHOLE CONSTRUCTOR IS NOT CALLED BEFORE windowInit() */
	public TimelineWindow(MainWindow parentWindow, Integer myWindowID) {
		super(parentWindow, myWindowID);
		System.out.println("Continue Window constructor");
		
        thisWindow = this;
		
		data = new Data(this);
		
		labelPanel = new LabelPanel(data);

		
		
		
		// Set the tooltip delay to 0, so the entry method objects display their bubbles more quickly
		 ToolTipManager.sharedInstance().setInitialDelay(0);
		
		
		// Construct the various layers, and the layout manager
		AxisPanel ap = new AxisPanel(data);
		AxisOverlayPanel op = new AxisOverlayPanel(data);
		AxisLayout lay = new AxisLayout(data,ap);
		// Create the layered panel containing our layers
		axisPanel = new LayeredPanel(data,ap,op,lay);
		
		
		mainPanel = new MainPanel(data, this);
		
		scrollingPanel = new ScrollingPanel(data, mainPanel, axisPanel, labelPanel);

		controls = new WindowControls(this, data);

		thisWindow.getContentPane().setLayout(new BorderLayout());	
		thisWindow.getContentPane().add(scrollingPanel, BorderLayout.CENTER);
		thisWindow.getContentPane().add(controls, BorderLayout.SOUTH);
		
		setTitle("Projections Timelines - "
				+ MainWindow.runObject[myRun].getFilename() + ".sts");
		controls.CreateMenus();

		showDialog();
		
	}

	
	
	protected void showWindow(){
		// do nothing
		System.out.println("Begin Window ShowWindow()");
		
	}


	protected void showDialog() {
		controls.showDialog();
	}

	/** Set the values in the User Event Table window */
	public void setData(Data data){
		controls.userEventWindowSetData();
	}

	
	protected void getDialogData(){
		System.out.println("Begin Window getDialogData()");
		
		data.setProcessorList(dialog.getValidProcessors());
        data.setRange(dialog.getStartTime(),dialog.getEndTime());
		
	}
	
    public void setDialogData() {
		System.out.println("Begin Window setDialogData()");
     
    	dialog.setValidProcessors(data.processorList());
    	dialog.setStartTime(data.startTime());
    	dialog.setEndTime(data.endTime());
        
    	super.setDialogData();
    }
	
	public void addProcessor(int p) {
		data.addProcessor(p); // FIXME Potential Bug: Should this be done after the next line ????
		mainPanel.loadTimelineObjects(false);
	}

	protected void windowInit() {
		System.out.println("Begin Window windowInit()");
		System.out.println("MainWindow.runObject[myRun]="+ MainWindow.runObject[myRun]);
		System.out.println("data="+data);
		
		data = new Data(this);
				
		data.setProcessorList(MainWindow.runObject[myRun].getValidProcessorList());
		data.setRange(0, MainWindow.runObject[myRun].getTotalTime());

	}
    
}
