package projections.Tools.Timeline;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;

/** Draws the left column of the timeline view. The labels such as "PE 0", "PE 1" */
public class LabelPanel extends JPanel implements MouseListener, MouseMotionListener {

	// TODO: create a component for each displayed PE which contains 
	// a tooltip showing the confusing "(23, 20)" portion of the display
		
	private Data data;

	int clickedOnPE;
	Point mouseLast;
	
	public LabelPanel(Data data)
	{
		setOpaque(true);	
		this.data = data;
		
		addMouseMotionListener(this); //handle mouse drags
		addMouseListener(this);
		
		clickedOnPE = -1;
		
	}
	
	/** Determine the preferred width for this panel, respecting the data.useMinimalMargins flag 
	 * 
	 * @note if data.useMinimalMargins is true then only the PE number will be printed without the idle %
	 * */
	private int preferredWidth(){
		if(data.useMinimalView())
			return 60;
		else
			return 90;
	}
	
	public Dimension getPreferredSize() {
		return new Dimension(preferredWidth(), data.screenHeight());
	}	


	protected void paintComponent(Graphics g)
	{
		// Let UI delegate paint first 
		// (including background filling, if I'm opaque)
		super.paintComponent(g); 
		// paint my contents next....

		g.setFont(data.labelFont);
		FontMetrics fm = g.getFontMetrics();

		g.setColor(data.getBackgroundColor());
		Rectangle clipBounds = g.getClipBounds();
		g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);

	
		for (int verticalPosition=0; verticalPosition<data.numPs(); verticalPosition++) {
			// Draw the labels onto the screen
			g.setColor(data.getForegroundColor());

			int pe = data.whichPE(verticalPosition);

			if(pe > -1){

				if(data.useMinimalView() || data.useCompactView()){
					// A simpler version (right justified, bold larger PE label, no idle percentage)

					if(clickedOnPE == pe){
						// don't draw the old location
					}
					else if(clickedOnPE >=0 && 	verticalPosition == mouseLast.y / data.singleTimelineHeight()){
						// draw the PE we are dragging around here
						g.setColor(Color.red);
						String peString = "PE "+clickedOnPE;
						int stringWidth = fm.stringWidth(peString);			
						g.drawString(peString, preferredWidth()-stringWidth, fm.getHeight()/2+data.singleTimelineHeight()/2 + verticalPosition*data.singleTimelineHeight());
					}
					else {
						String peString = "PE "+pe;
						int stringWidth = fm.stringWidth(peString);			
						g.drawString(peString, preferredWidth()-stringWidth, fm.getHeight()/2+data.singleTimelineHeight()/2 + verticalPosition*data.singleTimelineHeight());
					}

				} else {

					// The full version
					if(clickedOnPE == pe){
						// don't draw the old location
					}
					else if(clickedOnPE >=0 && 	verticalPosition == mouseLast.y / data.singleTimelineHeight()){
						// draw the PE we are dragging around here
						g.setColor(Color.red);

						String peString = "PE "+ clickedOnPE;
						g.drawString(peString, 10, data.singleTimelineHeight()/2 + verticalPosition*data.singleTimelineHeight());

						String percentString = "(" + (int)(100 - data.idleUsage[clickedOnPE]) + ", " + (int)(data.processorUsage[clickedOnPE]) + ")";
						g.drawString(percentString, 15, data.singleTimelineHeight()/2 + verticalPosition*data.singleTimelineHeight() + fm.getHeight() + 2);
					}
					else {
						g.setColor(data.getForegroundColor());

						String peString = "PE "+ pe;
						g.drawString(peString, 10, data.singleTimelineHeight()/2 + verticalPosition*data.singleTimelineHeight());

						String percentString = "(" + (int)(100 - data.idleUsage[pe]) + ", " + (int)(data.processorUsage[pe]) + ")";
						g.drawString(percentString, 15, data.singleTimelineHeight()/2 + verticalPosition*data.singleTimelineHeight() + fm.getHeight() + 2);
					}
				}
			}
		}

	}

	public void mouseClicked(MouseEvent e) {	
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		// remember where the mouse was last seen
		mouseLast = e.getPoint();
		// Determine which timeline this is associated with
		int whichLine = mouseLast.y / data.singleTimelineHeight();
		clickedOnPE = data.whichPE(whichLine);
		this.repaint();
	}

	public void mouseReleased(MouseEvent e) {
		// Determine which timeline this is associated with
		int whichLine = e.getPoint().y / data.singleTimelineHeight();
		data.movePEToLine(clickedOnPE, whichLine);
		clickedOnPE = -1;
	}

	public void mouseDragged(MouseEvent e) {
		// remember where the mouse was last seen
		mouseLast = e.getPoint();
		repaint();
			
	}

	public void mouseMoved(MouseEvent e) {
	}


}

