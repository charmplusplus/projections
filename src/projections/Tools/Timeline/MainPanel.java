package projections.Tools.Timeline;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.ToolTipManager;

import projections.Tools.Timeline.Data.representedEntity;
import projections.Tools.Timeline.RangeQueries.Query1D;
import projections.Tools.Timeline.RangeQueries.Range1D;
import projections.Tools.Timeline.RangeQueries.RangeQueryArrayList;
import projections.Tools.Timeline.RangeQueries.RangeQueryTree;
import projections.Tools.Timeline.RangeQueries.UnitTest.TestRange1DObject;
import projections.gui.MainWindow;


/** This class displays the timeline background, horizontal lines, entry method invocations, and user events
 * 
 *  It paints itself with width = max(panel width, data.desiredDisplayWidth)
 *  This is so it will stretch horizontally with the window.
 *  
 *  
 *  For speed, special data structures are used to store the entry method invocations and user events. 
 *  Historically these were just added to this JPanel as subcomponents, but this was too expensive if more 
 *  than a few hundred thousand were added.
 *  
 *  Now the code is a bit more complex to handle the painting and mouseover events, but it is faster!
 *  
 *  
 */

public class MainPanel extends JPanel  implements Scrollable, MouseListener, MouseMotionListener 
{
	private int viewX, viewY;
	
	private Data data;
	private MainHandler handler;
	
	/** A queriable time based datastructure that holds all the entry method invocations that will be drawn */
	private Map<Integer,Query1D<EntryMethodObject>> entryMethodInvocationsForEachPE;

	/** A queriable time based datastructure that holds all the user events that will be drawn */
	private TreeMap<Integer, Query1D<UserEventObject>> userEventsToPaintForEachPE;
 
	public MainPanel(Data data, MainHandler handler){
		this.handler = handler;
		this.data = data;

		this.setFocusable(true);
		this.setFocusCycleRoot(true);
		this.setFocusTraversalPolicy(new NullFocusTraversalPolicy());
		
		setAutoscrolls(true); //enable synthetic drag events

		addMouseMotionListener(this); 
		addMouseListener(this);
		
		
		// Tell the tooltip manager that we have something to display
        ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
        toolTipManager.registerComponent(this);
		
	}
	
	public class PeRenderer implements Runnable {
		Graphics g;
		Query1D<EntryMethodObject> l;
		int width;
		
		public PeRenderer(Graphics g, Query1D<EntryMethodObject> l, int width) {
			this.g = g.create();
			this.l = l;
			this.width = width;
		}
		
		public void run() {
			for(EntryMethodObject o : l){
				o.paintMe((Graphics2D) g, width);
//				count1 ++;
			}
		}
		
	}
	

	/** Paint the panel, filling the entire panel's width */
	public void paintComponent(Graphics g) {
		
		final long startTime = System.nanoTime();

		super.paintComponent(g);
		
		paintBackground(g);
		
		// Paint all entry method objects:
		int width;
		int height;
		Insets insets = getInsets();
		width = getWidth() - (insets.left + insets.right);
		height = getHeight() - (insets.top + insets.bottom);

		// Find time ranges to draw
		Rectangle clip = g.getClipBounds();
		long leftClipTime = data.screenToTime(clip.x-5);
		long rightClipTime = data.screenToTime(clip.x+clip.width+5);
		
		
		
		
		// Draw entry method invocations
		int count1 = -1;

		ArrayList<Thread> threads = new ArrayList();
		for(Entry<Integer, Query1D<EntryMethodObject>> entry : entryMethodInvocationsForEachPE.entrySet()) {
			Integer pe = entry.getKey();
			
			if(data.peTopPixel(pe) <= clip.y+clip.height+5 && data.peBottomPixel(pe) >= clip.y-5){

				Query1D<EntryMethodObject> l = entry.getValue();
				l.setQueryRange(leftClipTime, rightClipTime);
				PeRenderer pr = new PeRenderer(g, l, width);
				pr.run();
				
//				Thread t = new Thread(pr);
//				threads.add(t);
//				t.run();
//				
				for(EntryMethodObject o : l){
					o.paintMe((Graphics2D) g, width);
					count1 ++;
				}
			}
		}

		// Wait for threads to finish rendering
		for(Thread t : threads){
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
//		MainWindow.performanceLogger.log(Level.INFO,"Should have just painted up to " + count1 + " entry method objects in MainPanel of size (" + getWidth() + "," + getHeight() + ") clip=(" + clip.x + "," + clip.y + "," + clip.width + "," + clip.height + ")" );
		
		
		
		// Draw user events
		int count2 = 0;
		if(data.showUserEvents()){
			count2 = 0;
			for(Entry<Integer, Query1D<UserEventObject>> entry : userEventsToPaintForEachPE.entrySet()) {
				Integer pe = entry.getKey();

				if(data.peTopPixel(pe) <= clip.y+clip.height+5 && data.peBottomPixel(pe) >= clip.y-5){

					Query1D<UserEventObject> l = entry.getValue();
					l.setQueryRange(leftClipTime, rightClipTime);
					for(UserEventObject o : l){
						o.paintMe((Graphics2D) g, width, data);
						count2 ++;
					}
				}
			}
			MainWindow.performanceLogger.log(Level.INFO,"Should have just painted up to " + count2 + " user events in MainPanel of size (" + getWidth() + "," + getHeight() + ") clip=(" + clip.x + "," + clip.y + "," + clip.width + "," + clip.height + ")");
		}
		
		
		final long endTime = System.nanoTime();
		final long duration = endTime - startTime;
		MainWindow.performanceLogger.log(Level.INFO,"Time To Paint (" + count1 + " entry methods, " + count2 + " user events): " + (duration/1000000) + " ms");
		
		
		paintMessageSendLines(g, data.getMessageColor(), data.getBackgroundColor(), data.drawMessagesForTheseObjects);
		paintMessageSendLines(g, data.getMessageAltColor(), data.getBackgroundColor(), data.drawMessagesForTheseObjectsAlt);
		
	}


	
	/** Paint the panel, filling the entire panel's width */
	public void paintBackground(Graphics g) {
	
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

	private void paintMessageSendLines(Graphics g, Color c, Color bgColor, Set drawMessagesForObjects){
		Graphics2D g2d = (Graphics2D) g;
		// paint the message send lines
		if (drawMessagesForObjects.size()>0) {
			Iterator iter = drawMessagesForObjects.iterator();
			while(iter.hasNext()){
				Object o = iter.next();
				if(o instanceof EntryMethodObject){
					EntryMethodObject obj = (EntryMethodObject)o;
					if(obj.creationMessage() != null){
						int pCreation = obj.pCreation;
						int pExecution = obj.pe;
						
						// Message Creation point
						int x1 = data.timeToScreenPixel(obj.creationMessage().Time);			
						double y1 = data.messageSendLocationY(pCreation);
						// Message executed (entry method starts) 
						int x2 =  data.timeToScreenPixel(obj.getBeginTime());
						double y2 = data.messageRecvLocationY(pExecution);

						// Draw thick background Then thin foreground
						g2d.setPaint(bgColor);
						g2d.setStroke(new BasicStroke(4.0f));
						g2d.drawLine(x1,(int)y1,x2,(int)y2);
						g2d.setPaint(c);
						g2d.setStroke(new BasicStroke(2.0f));
						g2d.drawLine(x1,(int)y1,x2,(int)y2);
					}
				}
			}
		}

	}
	
	


	/** Dynamically generate the tooltip mouseover text when needed */
	@Override
	public String getToolTipText(MouseEvent evt){
		SpecialMouseHandler o = getObjectRenderedAtEvtLocation(evt);
		if(o != null){
			return o.getToolTipText();
		} else {
			return null;
		}
		
	}
	
	
	public void disposeOfStructures(){
		handler = null;
		removeAll();
		data.disposeOfStructures();
	}
	
	
	/** 
	 * Load or Reload the timeline objects from the data object's tloArray.
	 *  
	 *  if useHelperThreads is true, the information about messages sent will be offloaded to a separate thread. 
	 *  This will allow the results to be returned faster, but if the message information is needed immedeately, 
	 *  this should be set to false.
	 *  
	 * @note This was formerly called procRangeDialog()
	 */
	public void loadTimelineObjects(boolean useHelperThreads, Component rootWindow, boolean showProgress) {
		
		// keeplines describes if the lines from message creation
		// to execution are to be retained or not.
		setCursor(new Cursor(Cursor.WAIT_CURSOR));
		
		this.removeAll();
		data.createTLOArray(useHelperThreads, rootWindow, showProgress);


		final long startTime = System.nanoTime();

		// Add each user event 
		userEventsToPaintForEachPE = new TreeMap<Integer,Query1D<UserEventObject>>();	
		for(Entry<Integer, Set<UserEventObject>>  e : data.allUserEventObjects.entrySet()) {
			RangeQueryTree l = new RangeQueryTree(e.getValue());
			userEventsToPaintForEachPE.put(e.getKey(), l);
		}

		// Add the entry method invocations
		entryMethodInvocationsForEachPE = new TreeMap<Integer,Query1D<EntryMethodObject>>();	
		for(Entry<Integer, List<EntryMethodObject>> e : data.allEntryMethodObjects.entrySet()) {
			RangeQueryTree l = new RangeQueryTree(e.getValue());
			entryMethodInvocationsForEachPE.put(e.getKey(), l);
		}
		final long endTime = System.nanoTime();
		final long duration = endTime - startTime;
		MainWindow.performanceLogger.log(Level.INFO,"Time To Build user event and entry method display spatial data structures: " + (duration/1000000) + " ms");

		
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
		data.displayMustBeRepainted();
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
	
	
	/** The MainPanel can draw entry method objects and user event objects. This interface provides methods that handle GUI feedback to those objects. */
	public interface SpecialMouseHandler {

		public void mouseClicked(MouseEvent evt, JPanel parent, Data data);
		
		public String getToolTipText();

		public void mouseEntered(MouseEvent evt);
		public void mouseMoved(MouseEvent evt);
		public void mouseExited(MouseEvent evt);
	}

	
	private SpecialMouseHandler getObjectRenderedAtEvtLocation(MouseEvent evt){
		// Check to see which pe we are on

		final int whichPERow = evt.getY() / data.singleTimelineHeight();
		final int verticalOffsetWithinRow = evt.getY() % data.singleTimelineHeight();

		final int PE = data.whichPE(whichPERow);

		if(PE >= 0){
			final long time = data.screenToTime(evt.getX());

			representedEntity what = data.representedAtPixelYOffsetInRow(verticalOffsetWithinRow);

			if(what == representedEntity.ENTRY_METHOD){

				// Find an entry method invocation that occurred at this time, and display its tooltip instead
				Query1D<EntryMethodObject> a = entryMethodInvocationsForEachPE.get(PE);
				Iterator<EntryMethodObject> b = a.iterator(time, time);

				// Iterate through all the things matching this timestamp so we can get the last one (which should be painted in front)
				EntryMethodObject o = null;
				while(b.hasNext()){
					o = b.next();
				}

				if(o != null){
					return o;
				}
			} else if(what == representedEntity.USER_EVENT){

				// Find an entry method invocation that occurred at this time, and display its tooltip instead
				Query1D<UserEventObject> a = userEventsToPaintForEachPE.get(PE);
				Iterator<UserEventObject> b = a.iterator(time, time);

				// Iterate through all the things matching this timestamp so we can get the last one (which should be painted in front)
				UserEventObject o = null;
				while(b.hasNext()){
					o = b.next();
				}
				if(o != null){
					return o;
				}
			}
		}

		return null;
	}
	
	

	public void mouseClicked(MouseEvent evt) {
		SpecialMouseHandler o = getObjectRenderedAtEvtLocation(evt);
		if(o != null) {
			o.mouseClicked(evt, this, data);
		}
	}

	
	SpecialMouseHandler currentMouseTrackedObject;
	
	public void mouseEntered(MouseEvent evt) {
		// Start tracking which of our underlying rendered objects is currently under the cursor
		currentMouseTrackedObject = getObjectRenderedAtEvtLocation(evt);
		
	}

	public void mouseMoved(MouseEvent evt) {
		// Continue tracking which of our underlying rendered objects is currently under the cursor

		// First find what object is under the cursor:
		SpecialMouseHandler underCursor = getObjectRenderedAtEvtLocation(evt);
		
		// Cursor enters object from nothing
		if(underCursor != null && currentMouseTrackedObject == null){
			underCursor.mouseEntered(evt);
			currentMouseTrackedObject = underCursor;
		}
				
		// Cursor still on same object
		else if(underCursor != null && underCursor == currentMouseTrackedObject){
			currentMouseTrackedObject.mouseMoved(evt);
		}
		
		// Cursor left object for another one
		else if(underCursor != null && underCursor != currentMouseTrackedObject){		
			currentMouseTrackedObject.mouseExited(evt);
			underCursor.mouseEntered(evt);
			currentMouseTrackedObject = underCursor;
		}
				
		// Cursor left object for nothing
		else if(underCursor == null && currentMouseTrackedObject != null){
			currentMouseTrackedObject.mouseExited(evt);
			currentMouseTrackedObject = null;
		} 
		else {
			// Should never get here
			currentMouseTrackedObject = null;
		}
		
	}
	
	public void mouseExited(MouseEvent evt) {
		// End tracking which of our underlying rendered objects is currently under the cursor
		if(currentMouseTrackedObject != null){
			currentMouseTrackedObject.mouseExited(evt);
		}
		currentMouseTrackedObject = null;
	}


}
