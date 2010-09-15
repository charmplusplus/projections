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
import java.util.Collection;
import java.util.logging.Level;

import javax.swing.JPanel;

import projections.Tools.Timeline.Data.ViewType;
import projections.gui.MainWindow;

/** Draws the left column of the timeline view. The labels such as "PE 0", "PE 1" */
class LabelPanel extends JPanel implements MouseListener, MouseMotionListener {

	// TODO: create a component for each displayed PE which contains 
	// a tooltip showing the confusing "(23, 20)" portion of the display
		
	private Data data;

	private int clickedOnPE;
	private Point mouseLast;
	
	protected LabelPanel(Data data)
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
		if(data.getViewType() == ViewType.VIEW_MINIMAL)
			return 60;
		else
			return 90;
	}
	
	public Dimension getPreferredSize() {
		return new Dimension(preferredWidth(), data.screenHeight());
	}	


	public void paintComponent(Graphics g)
	{

		final long startTime = System.nanoTime();

		
		g.setFont(data.labelFont);
		FontMetrics fm = g.getFontMetrics();

		g.setColor(data.getBackgroundColor());
		Rectangle clip = g.getClipBounds();
		g.fillRect(clip.x, clip.y, clip.width, clip.height);

		int upperRowToPaint = data.rowForPixel(clip.y);
		int lowerRowToPaint = data.rowForPixel(clip.y+clip.height-1);
				
		
		for (int verticalPosition=upperRowToPaint; verticalPosition<=lowerRowToPaint; verticalPosition++) {
			// Draw the labels onto the screen

			int pe = data.whichPE(verticalPosition);

			if(pe > -1){
				
				switch(data.getViewType()){
				case VIEW_SUPERCOMPACT:
					break;
				case VIEW_MINIMAL:
				case VIEW_COMPACT:
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
					break;
				default:
					
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

						String percentString = "(?,?)";
						if(data.idleUsage.length > pe && data.processorUsage.length>pe){
							percentString = "(" + (int)(100 - data.idleUsage[pe]) + ", " + (int)(data.processorUsage[pe]) + ")";
						}
						g.drawString(percentString, 15, data.singleTimelineHeight()/2 + verticalPosition*data.singleTimelineHeight() + fm.getHeight() + 2);
					}
					break;
				}
			}
		}

		
		final long endTime = System.nanoTime();
		final long duration = endTime - startTime;
		MainWindow.performanceLogger.log(Level.INFO,"Time to paint Label Panel: " + (duration/1000000) + " ms");
		
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

