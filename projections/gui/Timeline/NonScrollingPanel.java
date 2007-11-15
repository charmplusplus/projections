package projections.gui.Timeline;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.*;

import projections.gui.OrderedIntList;


/**
 * A panel that displays a baby timeline view that does not provide scrolling or zooming.
 * 
 * @note this can be used to generate baby timline pieces to be shown in other tools.
 * 
 * Currently this supports only a single processor timeline, but it could be easily extended.
 * 
 * @author idooley2
 *
 */
public class NonScrollingPanel extends JPanel implements MainHandler{

	private static final long serialVersionUID = 1L;

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;

	
	NonScrollingPanel thisPanel;
	
	long startTime;
	long endTime;
	int PE;

	private MainPanel displayPanel;
	private LabelPanel labelPanel;
	private AxisPanel axisPanel;
	private Data data;

	/** Create the scrollable panel with the three provided panels. */
	public NonScrollingPanel(long startTime_, long endTime_, int PE_, Color background, Color foreground, boolean useMinimalMargins) {
		thisPanel = this;

		startTime=startTime_;
		endTime=endTime_;
		PE=PE_;

		OrderedIntList validPEs = new OrderedIntList();
		validPEs.insert(PE);
		
		// setup the Data for this panel 
		data = new Data(null);
		data.setProcessorList(validPEs);
		data.setRange(startTime, endTime);
		data.setUseMinimalMargins(useMinimalMargins);
		data.setFontSizes(12, 10, true);
		data.showIdle = true;
		data.showPacks = true;

		
		if(background != null && foreground != null)
			data.setColors(background,foreground);
		
		// create a MainPanel for it	
		displayPanel = new MainPanel(data, this);
		displayPanel.loadTimelineObjects();
				
		labelPanel = new LabelPanel(data);
		
		axisPanel = new AxisPanel(data);
		
		NonScrollingLayout lay = new NonScrollingLayout();
		lay.setAxis(axisPanel);
		lay.setLabel(labelPanel);
		lay.setMain(displayPanel);
		
		this.setLayout(lay);
		this.add(displayPanel);
		this.add(axisPanel);
		this.add(labelPanel);
		
		data.setHandler(this);
		
		// Set the tooltip delay to 0, so the entry method objects display their bubbles more quickly
		ToolTipManager.sharedInstance().setInitialDelay(0);
		ToolTipManager.sharedInstance().setDismissDelay(600000);	
	}


	public void paintComponent(Graphics g){
		g.setColor(data.getBackgroundColor());
		g.fillRect(0,0,getWidth(),getHeight());		
	}
	
	
	public void addProcessor(int p){
		// do nothing, required by interface we implement
	}
	
	/** Resize my panels(required by interface, called by data object) */
	// The data object has been instructed to change the display width
	// The scale factor or window size has likely changed.
	// Do not call data.setScreenWidth() in here
	public void refreshDisplay(boolean doRevalidate){

		if(doRevalidate){
			if(axisPanel!=null)
				axisPanel.revalidate();
			if(displayPanel!=null)
				displayPanel.revalidate();
			if(labelPanel!=null)
				labelPanel.revalidate();
			if(thisPanel!=null)
				thisPanel.revalidate();
		}
		if(axisPanel!=null)
			axisPanel.repaint();
		if(displayPanel!=null)
			displayPanel.repaint();
		if(labelPanel!=null)
			labelPanel.repaint();
		if(thisPanel!=null)
			thisPanel.repaint();

	}
	
	public void setData(Data data){
		this.data = data;
	}


	/** Required by interface MainHandler. This one does nothing */
	public void notifyProcessorListHasChanged() {
		// Do nothing
	}

	
}
