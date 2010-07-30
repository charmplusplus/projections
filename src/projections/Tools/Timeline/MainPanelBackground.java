package projections.Tools.Timeline;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JPanel;

/**
 */

class MainPanelBackground extends JPanel {

	private Data data;
		
	protected MainPanelBackground(Data data){
		this.data = data;
	}
	
	/** Paint the panel, filling the entire panel's width */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		int width = getWidth();
		
		g.setColor(data.getBackgroundColor());
		Rectangle clipBounds = g.getClipBounds();
		g.fillRect(clipBounds.x,clipBounds.y,clipBounds.width,clipBounds.height);

		// Paint the selection region
		if(data.selectionValid()){
			// Draw a  background for the selected timelines
			g.setColor(new Color(100,100,100));
			g.fillRect(data.leftSelection(), 0,  data.rightSelection()-data.leftSelection(), getHeight()-1);
			
			// Draw vertical lines at the selection boundaries
			g.setColor(data.getForegroundColor());
			g.drawLine(data.leftSelection(),0, data.leftSelection(), getHeight()-1);
			g.drawLine(data.rightSelection(),0, data.rightSelection(), getHeight()-1);
		}
		
		// Paint the highlight where the mouse cursor was last seen
		if(data.highlightValid()){
			// Draw vertical line
			g.setColor(data.getForegroundColor());
			g.drawLine(data.getHighlight(),0, data.getHighlight(), getHeight()-1);
		}
		
		// Draw the horizontal line 
		if(data.getViewType() != Data.ViewType.VIEW_SUPERCOMPACT){
			g.setColor(new Color(128,128,128));
			for (int i=0; i<data.numPs(); i++) {

				int y = data.horizontalLineLocationTop(i);

				g.drawLine(0+data.offset(), y, width-data.offset(), y);
			}
		}		
	}
	
}
