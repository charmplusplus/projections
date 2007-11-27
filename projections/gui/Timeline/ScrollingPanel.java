package projections.gui.Timeline;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.*;

/**
 * A scrolling panel that holds all the graphical pieces of the visualization
 * 
 */
public class ScrollingPanel extends JPanel  {

	private static final long serialVersionUID = 1L;

	JScrollPane scrollpane;
		
	JPanel mainPanel;
	LayeredPanel axisPanel;
	LabelPanel labelPanel;
	
	Data data;
	
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;
	
	
	/** Create the scrollable panel with the three provided panels. */
	public ScrollingPanel(Data data_, JPanel mainPanel_, LayeredPanel axisPanel_, LabelPanel labelPanel_) {
 
		data=data_;
		mainPanel=mainPanel_;
		axisPanel=axisPanel_;
		labelPanel=labelPanel_;	

		scrollpane = new JScrollPane();
		scrollpane.setLayout(new projections.gui.Timeline.TimelineScrollPaneLayout(data));
		
		mainPanel.setAutoscrolls(false);
		labelPanel.setAutoscrolls(false);
		axisPanel.setAutoscrolls(false);
		
		scrollpane.setViewportView(mainPanel);
		scrollpane.setRowHeaderView(labelPanel);
		scrollpane.setColumnHeaderView(axisPanel);
		
			
		scrollpane.setCorner(JScrollPane.UPPER_LEFT_CORNER, new Corner());
		scrollpane.setCorner(JScrollPane.LOWER_LEFT_CORNER, new Corner());
		scrollpane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, new Corner());
		scrollpane.setCorner(JScrollPane.LOWER_RIGHT_CORNER, new Corner());

		this.setLayout(new BorderLayout());
		this.add(scrollpane, BorderLayout.CENTER);
		
		scrollpane.getViewport().setBackground(data.getBackgroundColor());
		scrollpane.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE); // This should be tuned for performance
		
		axisPanel.setVisible(true);
		mainPanel.setVisible(true);
		labelPanel.setVisible(true);
		setVisible(true);
		
		// Set the tooltip delay to 0, so the entry method objects display their bubbles more quickly
		ToolTipManager.sharedInstance().setInitialDelay(0);
		ToolTipManager.sharedInstance().setDismissDelay(600000);		
		ToolTipManager.sharedInstance().registerComponent(mainPanel);
	}
	
	
	

	/** Resize my panels(required by interface, called by data object) */
	// The data object has been instructed to change the display width
	// The scale factor or window size has likely changed.
	// Do not call data.setScreenWidth() in here
	void refreshDisplay(boolean doRevalidate){

		if(doRevalidate){
			data.invalidateSelection();
			this.revalidate();
			scrollpane.revalidate();
			mainPanel.revalidate();
			axisPanel.revalidate();
			labelPanel.revalidate();
		}
		
		this.repaint();
		scrollpane.repaint();
		mainPanel.repaint();
		axisPanel.repaint();
		labelPanel.repaint();		  

	}


	/** A simple class for drawing the corners in the JScrollPane */
	public class Corner extends JComponent {
		private static final long serialVersionUID = 1L;
		protected void paintComponent(Graphics g) {
			g.setColor(data.getBackgroundColor());
			g.fillRect(0,0,getWidth(),getHeight());
		}
	}


	Dimension getPreferredScrollableViewportSize(){
		return getPreferredSize();	
	}

	
};
