package projections.gui.Timeline;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Iterator;

import javax.swing.*;


/** This class displays the timeline background and horizontal lines
 * 
 *  It paints itself with width = max(panel width, data.desiredDisplayWidth)
 *  This is so it will stretch horizontally with the window.
 */

public class MainPanel extends JPanel  implements Scrollable, MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;
	
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;
	
	int viewX, viewY;
	
	Data data;
	MainHandler handler;
	
	
	public MainPanel(Data data, MainHandler handler){
		this.handler = handler;
		this.data = data;

		setAutoscrolls(true); //enable synthetic drag events
		addMouseMotionListener(this); //handle mouse drags

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
			int y = data.singleTimelineHeight()/2 + i*data.singleTimelineHeight();
			g.drawLine(0+data.offset(), y, width-data.offset(), y);
		}


		// paint the message send lines 
		
		if (data.drawMessagesForTheseObjects.size()>0) {
			g.setColor(data.getForegroundColor());

			Dimension dim = getSize();

			Iterator iter = data.drawMessagesForTheseObjects.iterator();
			while(iter.hasNext()){
		
				Object o = iter.next();
							
				if(o instanceof EntryMethodObject){
	
					EntryMethodObject obj = (EntryMethodObject)o;
					
					if(obj.creationMessage() != null){

						int pCreation = obj.pCreation;
						int pExecution = obj.pCurrent;

						// Find the index for the PEs in the list of displayed PEs
						int startpe_index=0;
						int endpe_index=0;
						data.processorList().reset();
						for (int j=0;j<data.processorList().size();j++) {
							int pe = data.processorList().nextElement();
							if (pe == pCreation) {
								startpe_index = j;
							}
							if (pe == pExecution) {
								endpe_index = j;
							}
						}

						// Message Creation point
						int x1 = data.timeToScreenPixelLeft(obj.creationMessage().Time, getWidth());			
						double y1 = (double)data.singleTimelineHeight() * ((double)startpe_index + 0.5) + data.barheight()/2 + data.messageSendHeight();

						// Message executed (entry method starts) 
						int x2 =  data.timeToScreenPixel(obj.getBeginTime(), getWidth());
						double y2 = (double)data.singleTimelineHeight() * ((double)endpe_index + 0.5) - (data.barheight()/2);

						g.drawLine(x1,(int)y1,x2,(int)y2);

					}
				}

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

		// Add the entry method instances (EntryMethodObject) to the panel for displaying
		for (int p = 0; p < data.numPs(); p++) {
			for (int i = 0; i < data.tloArray[p].length; i++){
				data.tloArray[p][i].setWhichTimeline(p);
				this.add(data.tloArray[p][i]);
			
				// Register a mouse motion listener for dragging of the viewport

				// Only register it if we have not already registered it
				MouseMotionListener[] mml = data.tloArray[p][i].getMouseMotionListeners();
				boolean found = false;
				for(int mml_index=0;mml_index<mml.length;mml_index++){
					if(mml[mml_index]==this){
						found = true;
					}
				}
				if(!found){
					data.tloArray[p][i].addMouseListener(this);
					data.tloArray[p][i].addMouseMotionListener(this);
				}
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


	/** Handle dragging of panel if we are in a viewport */
	public void mouseDragged(MouseEvent e) {
		if(getParent() instanceof JViewport){
			JViewport jv = (JViewport)getParent();
			Point p = jv.getViewPosition();
			int newX = p.x - (e.getX()-viewX);
			int newY = p.y - (e.getY()-viewY);
			int maxX = getWidth() - jv.getWidth();
			int maxY = getHeight() - jv.getHeight();
			if (newX > maxX) newX = maxX;
			if (newY > maxY) newY = maxY;
			if (newX < 0) newX = 0;
			if (newY < 0) newY = 0;
			jv.setViewPosition(new Point(newX, newY));
		}
	}
	
	/** Handle dragging of panel if we are in a viewport */
    public void mousePressed(MouseEvent e) {
    	if(getParent() instanceof JViewport){
    		viewX = e.getX();	
    		viewY = e.getY();
    		setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    	}
    }

    /** Handle dragging of panel if we are in a viewport */
	public void mouseReleased(MouseEvent e) {
		if(getParent() instanceof JViewport){
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}
	
	public void mouseMoved(MouseEvent e) {
		// do nothing
	}


	public void mouseClicked(MouseEvent e) {
		// do nothing
		
	}

	public void mouseEntered(MouseEvent e) {
		// do nothing
	}

	public void mouseExited(MouseEvent e) {
		// do nothing
	}

}
