package projections.Tools.Timeline;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ToolTipManager;

/**
 * A scrolling panel that holds all the graphical pieces of the visualization
 * 
 */
class ScrollingPanel extends JPanel implements MouseWheelListener {

	private JScrollPane scrollpane;
		
	private MainPanel mainPanel;
	private AxisPanel axisPanel;
	private LabelPanel labelPanel;
	
	private Data data;
	
	
	/** Create the scrollable panel with the three provided panels. */
	protected ScrollingPanel(Data data_, MainPanel mainPanel_, AxisPanel axisPanel_, LabelPanel labelPanel_) {
 
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

//		scrollpane.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE); // This should be tuned for performance
		scrollpane.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE); // This should be tuned for performance

		// Add listener to enable Ctrl+scroll to zoom
		mainPanel.addMouseWheelListener(this);
		
		axisPanel.setVisible(true);
		mainPanel.setVisible(true);
		labelPanel.setVisible(true);
		setVisible(true);
		
		// Set the tooltip delay to 0, so the entry method objects display their bubbles more quickly
		data.setToolTipDelaySmall();
		ToolTipManager.sharedInstance().registerComponent(mainPanel);
	}

	/**
	 * Zoom when Ctrl+scroll happens over the main view
	 * @param e Details of the mouse wheel event
	 */
	public void mouseWheelMoved(MouseWheelEvent e) {
		if (e.isControlDown()) {
			data.keepViewCentered(true);
			if (e.getWheelRotation() < 0) {
				data.increaseScaleFactor();
			} else {
				data.decreaseScaleFactor();
			}
		} else {
			scrollpane.dispatchEvent(e);
		}
	}

	void updateBackgroundColor(){
		scrollpane.getHorizontalScrollBar().setBackground(data.getBackgroundColor());
		scrollpane.getViewport().setBackground(data.getBackgroundColor());
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
			scrollpane.invalidate();
			mainPanel.invalidate();
			axisPanel.invalidate();
			labelPanel.invalidate();
			this.revalidate();
		}
		
		// Repaint all subcomponents
		this.repaint();
		scrollpane.repaint();
		mainPanel.repaint();
		axisPanel.repaint();
		labelPanel.repaint();		  
		
	}

	/** A simple class for drawing the corners in the JScrollPane */
	private class Corner extends JPanel {
		public void paintComponent(Graphics g) {
			g.setColor(data.getBackgroundColor());
			g.fillRect(0,0,getWidth(),getHeight());
		}
	}
	
}
