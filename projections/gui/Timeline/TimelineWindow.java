package projections.gui.Timeline;

import java.awt.BorderLayout;

import javax.swing.JLayeredPane;
import javax.swing.ToolTipManager;

import projections.gui.MainWindow;
import projections.gui.ProjectionsWindow;
 
public class TimelineWindow extends ProjectionsWindow implements MainHandler {

	private static final long serialVersionUID = 1L;

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;
  	
	public ScrollingPanel scrollingPanel;
	
	public LabelPanel labelPanel;
	public LayeredPanel axisPanel; 
	public MainPanel mainPanel;
	

	
	/**	 A layered panel which will contain the standard axisPanel 
	 * as well as an overlay on which the selection or highlights 
	 * can be drawn 
	 * 
	 * Depth=10 corresponds to the mainPanel
	 * Depth=20 corresponds to the Rubberband selection
	 * 
	 * */
	public JLayeredPane mainLayeredPanel; 
	

		
	/** The JPanel containing all the buttons */
	public WindowControls controls;
	
	TimelineWindow thisWindow;
	
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
		
		labelPanel = new LabelPanel(data, this);

		
		
		
		// Set the tooltip delay to 0, so the entry method objects display their bubbles more quickly
		 ToolTipManager.sharedInstance().setInitialDelay(0);
		
		
		// Construct the various layers, and the layout manager
		AxisPanel ap = new AxisPanel(data);
		AxisOverlayPanel op = new AxisOverlayPanel(data);
		AxisLayout lay = new AxisLayout(data,ap);
		// Create the layered panel containing our layers
		axisPanel = new LayeredPanel(data,ap,op,lay);
		
		
		mainPanel = new MainPanel(data, this);
//		MainOverlayPanel op2 = new MainOverlayPanel(data);
		MainLayout lay2 = new MainLayout(data);
//		mainPanel = new LayeredPanel(data,mp,op2,lay);
		
		
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
