package projections.gui.Timeline;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Stroke;

import javax.swing.*;


/** This class displays the timeline background and horizontal lines
 * 
 *  It paints itself with width = max(panel width, data.desiredDisplayWidth)
 *  This is so it will stretch horizontally with the window.
 */

public class MainPanel extends JPanel  implements Scrollable{

	private static final long serialVersionUID = 1L;
	
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;
	
	Data data;
	MainHandler handler;
	
	
	public MainPanel(Data data, MainHandler handler){
		this.handler = handler;
		this.data = data;
		setLayout(new MainLayout(data));
	}
	

	
	/** Used when painting an Image manually */
	public void paintComponentWithChildren(Graphics g){
		paintComponent(g);
		paintChildren(g);		
	}

	/** Paint the panel, filling the entire panel's width */
	protected void paintComponent(Graphics g) {

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
			g.setColor(Color.white);
			g.drawLine(data.leftSelection(),0, data.leftSelection(), getHeight()-1);
			g.drawLine(data.rightSelection(),0, data.rightSelection(), getHeight()-1);
		}
		
		// Paint the highlight where the mouse cursor was last seen
		if(data.highlightValid()){
			// Draw vertical line
			g.setColor(Color.white);
			g.drawLine(data.getHighlight(),0, data.getHighlight(), getHeight()-1);
		}
		
		// Draw the horizontal line 
		g.setColor(new Color(128,128,128));
		for (int i=0; i<data.numPs(); i++) {
			int y = 2 + data.singleTimelineHeight()/2 + i*data.singleTimelineHeight();
			g.drawLine(0+data.offset(), y, width-data.offset(), y);
		}

		drawAllLines(g);	

	}

	/** paint the message send lines */
	public void drawAllLines(Graphics g){

		if (!data.mesgCreateExecVector.isEmpty()) {
			g.setColor(data.getForegroundColor());

			Dimension dim = getSize();
			double calc_xscale = (double )(data.pixelIncrement(getWidth())/data.timeIncrement(getWidth()));
			double yscale = (double )dim.height/
			(double)(data.processorList().size());

			for (int i=0;i<data.mesgCreateExecVector.size();i++) {
				Line lineElement = 
					(Line)data.mesgCreateExecVector.elementAt(i);
				int startpe_position=0;
				int endpe_position=0;
				data.processorList().reset();
				for (int j=0;j<data.processorList().size();j++) {
					int pe = data.processorList().nextElement();
					if (pe == lineElement.pCreation) {
						startpe_position = j;
					}
					if (pe == lineElement.pCurrent) {
						endpe_position = j;
					}
				}

				int x1 = 
					(int)((double)(lineElement.creationtime - data.beginTime())*
							calc_xscale+data.offset());
				int x2 = 
					(int)((double)(lineElement.executiontime - data.beginTime())*
							calc_xscale+data.offset());
				int y1 = (int)(yscale * (double)startpe_position + 
						lineElement.obj.h+lineElement.obj.verticalInset+5+5);
				int y2 = (int)(yscale * (double)endpe_position +
						lineElement.obj.h);

				g.drawLine(x1,y1,x2,y2);
			}
		}

	}


	/** 
	 * Load or Reload the timeline objects from the data object's tloArray
	 *  
	 * @note This was formerly called procRangeDialog()
	 */
	public void loadTimelineObjects() {
		
		// keeplines describes if the lines from message creation
		// to execution are to be retained or not.
		setCursor(new Cursor(Cursor.WAIT_CURSOR));
	
		this.removeAll();
		data.createTLOArray();

		// Add the entry method (EntryMethodObject)
		for (int p = 0; p < data.numPs(); p++) {
			for (int i = 0; i < data.tloArray[p].length; i++){
				data.tloArray[p][i].setWhichTimeline(p);
				this.add(data.tloArray[p][i]);
			}
		}

		// Add each user event 
		if (data.timelineUserEventObjectsArray != null)
			for (int p = 0; p < data.numPs(); p++)
				if (data.timelineUserEventObjectsArray[p] != null)
					for (int i = 0; i < data.timelineUserEventObjectsArray[p].length; i++){
						data.timelineUserEventObjectsArray[p][i].setWhichTimeline(p);
						this.add(data.timelineUserEventObjectsArray[p][i]);
					}
		

		handler.setData(data);
		handler.refreshDisplay(true);
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}
	

	/** should be ignored */
	public Dimension getPreferredSize() {
		return new Dimension(getWidth(),data.screenHeight());
	}

	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction){
		return data.singleTimelineHeight();
	}

	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction){
		return 5*data.singleTimelineHeight();
	}

	public Dimension getPreferredScrollableViewportSize(){
		return getPreferredSize();
	}

	public boolean getScrollableTracksViewportWidth(){
		return false;
	}

	public boolean getScrollableTracksViewportHeight(){
		return false;
	}



	public Data getData() {
		return data;
	}


}
