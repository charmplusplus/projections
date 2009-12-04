package projections.Tools.Timeline;

import java.awt.BorderLayout;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ToolTipManager;

/**
 * A scrolling panel that holds all the graphical pieces of the visualization
 * 
 */
public class ScrollingPanel extends JPanel {

	JScrollPane scrollpane;
		
	MainPanel mainPanel;
	LayeredPanel axisPanel;
	LabelPanel labelPanel;
	
	Data data;
	
	
	/** Create the scrollable panel with the three provided panels. */
	public ScrollingPanel(Data data_, MainPanel mainPanel_, LayeredPanel axisPanel_, LabelPanel labelPanel_) {
 
		data=data_;
		mainPanel=mainPanel_;
		axisPanel=axisPanel_;
		labelPanel=labelPanel_;	

		scrollpane = new JScrollPane();
		scrollpane.setLayout(new TimelineScrollPaneLayout(data));
		
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

		scrollpane.setBackground(data.getBackgroundColor());

		scrollpane.getHorizontalScrollBar().setBackground(data.getBackgroundColor());
		scrollpane.getHorizontalScrollBar().setOpaque(true);
		
		setLayout(new BorderLayout());
		add(scrollpane, BorderLayout.CENTER);
		
		scrollpane.getViewport().setBackground(data.getBackgroundColor());
		scrollpane.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE); // This should be tuned for performance
		
		axisPanel.setVisible(true);
		mainPanel.setVisible(true);
		labelPanel.setVisible(true);
		setVisible(true);
		
		// Set the tooltip delay to 0, so the entry method objects display their bubbles more quickly
		data.SetToolTipDelaySmall();
		ToolTipManager.sharedInstance().registerComponent(mainPanel);
	}
	
	
	
	void updateBackgroundColor(){
		this.setBackground(data.getBackgroundColor());
		scrollpane.setBackground(data.getBackgroundColor());
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
		protected void paintComponent(Graphics g) {
			// Let UI delegate paint first 
		    // (including background filling, if I'm opaque)
		    super.paintComponent(g); 
		    // paint my contents next....
			g.setColor(data.getBackgroundColor());
			g.fillRect(0,0,getWidth(),getHeight());
		}
	}
	
	
//	/** Paint the panel */
//	@Override public void paintComponent(Graphics g) {
//		System.out.println("paintComponent ScrollingPanel");
//		super.paintComponent(g);
//	
//		scrollpane.paintComponents(g);
//		
//		g.setColor(Color.yellow);
//		g.fillRect(0,0, getWidth(), getHeight());
//		
//	}
//		
//	
//
//	@Override public void update(Graphics g){
//		paintComponent(g);
//	}
	
}
