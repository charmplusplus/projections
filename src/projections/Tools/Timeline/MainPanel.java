package projections.Tools.Timeline;

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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;

import projections.Tools.Timeline.RangeQueries.Query1D;
import projections.Tools.Timeline.RangeQueries.Range1D;
import projections.Tools.Timeline.RangeQueries.RangeQueryArrayList;
import projections.Tools.Timeline.RangeQueries.RangeQueryTree;
import projections.Tools.Timeline.RangeQueries.UnitTest.TestRange1DObject;


/** This class displays the timeline background and horizontal lines
 * 
 *  It paints itself with width = max(panel width, data.desiredDisplayWidth)
 *  This is so it will stretch horizontally with the window.
 */

public class MainPanel extends JPanel  implements Scrollable, MouseListener, MouseMotionListener 
{
	
	private int viewX, viewY;
	
	private Data data;
	private MainHandler handler;
	
	private Map<Integer,Query1D<EntryMethodObject>> entryMethodInvocationsForEachPE;
	private TreeMap<Integer, Query1D<UserEventObject>> userEventsToPaintForEachPE;
 
	
	public MainPanel(Data data, MainHandler handler){
		this.handler = handler;
		this.data = data;

		this.setFocusable(true);
		this.setFocusCycleRoot(true);
		this.setFocusTraversalPolicy(new NullFocusTraversalPolicy());
		
		setAutoscrolls(true); //enable synthetic drag events
		addMouseMotionListener(this); //handle mouse drags

		setLayout(new MainLayout(data));

		// Add the panel which will draw the message send lines on top of everything else
		add(new MainPanelForeground(data));
			
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
//		data.scaledScreenWidth(width);
		long leftClipTime = data.screenToTime(clip.x-5);
		long rightClipTime = data.screenToTime(clip.x+clip.width+5);
		
		
		// Draw entry method invocations
		int count1 = 0;
		for(Entry<Integer, Query1D<EntryMethodObject>> entry : entryMethodInvocationsForEachPE.entrySet()) {
			Integer pe = entry.getKey();
			
			if(data.peTopPixel(pe) <= clip.y+clip.height+5 && data.peBottomPixel(pe) >= clip.y-5){

				Query1D<EntryMethodObject> l = entry.getValue();
				l.setQueryRange(leftClipTime, rightClipTime);
				for(EntryMethodObject o : l){
					o.paintMe((Graphics2D) g, width);
					count1 ++;
				}
			}
		}
//		System.out.println("Should have just painted up to " + count1 + " entry method objects in MainPanel of size (" + getWidth() + "," + getHeight() + ") clip=(" + clip.x + "," + clip.y + "," + clip.width + "," + clip.height + ")");
		
		
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
//			System.out.println("Should have just painted up to " + count2 + " user events in MainPanel of size (" + getWidth() + "," + getHeight() + ") clip=(" + clip.x + "," + clip.y + "," + clip.width + "," + clip.height + ")");
		}
		
		
		final long endTime = System.nanoTime();
		final long duration = endTime - startTime;
		System.out.println("Time To Paint (" + count1 + " entry methods, " + count2 + " user events): " + (duration/1000000) + " ms");
		
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

		// Add the panel which will draw the message send lines on top of everything else
		add(new MainPanelForeground(data));
		
				

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
		System.out.println("Time To Build user event and entry method display spatial data structures: " + (duration/1000000) + " ms");

	
	
		
		// DEBUGGING:
//		System.out.println("entryMethodObjectsToPaintForEachPE.size()=" + entryMethodInvocationsForEachPE.size());
//		for(Entry<Integer, Query1D<EntryMethodObject>> entry : entryMethodInvocationsForEachPE.entrySet()) {
//
//			Integer pe = entry.getKey();
//			Query1D<EntryMethodObject> l = entry.getValue();
//
//			final long startTime1 = System.nanoTime();
//			final long endTime1;
//			try {
//				int count = 0;
//				for(EntryMethodObject o : l){
//					count++;
//				}
//			} finally {
//			  endTime1 = System.nanoTime();
//			}
//			final long duration1 = endTime1 - startTime1;
//			System.out.println("Time To Iterate Through entries: " + (duration1/1000000) + " ms");
//		}
		
		
//		
//		Iterator pe_iter = data.allEntryMethodObjects.values().iterator();
//		while(pe_iter.hasNext()){
//			LinkedList objs = (LinkedList) pe_iter.next();
//			Iterator obj_iter = objs.iterator();
//			while(obj_iter.hasNext()){
//				EntryMethodObject obj = (EntryMethodObject) obj_iter.next();
//
//
//				
//				// Register a mouse motion listener for dragging of the viewport
//
//				// Only register it if we have not already registered it
//				MouseMotionListener[] mml = obj.getMouseMotionListeners();
//				boolean found = false;
//				for(int mml_index=0;mml_index<mml.length;mml_index++){
//					if(mml[mml_index]==this){
//						found = true;
//					}
//				}
//				if(!found){
//					obj.addMouseListener(this);
//					obj.addMouseMotionListener(this);
//				}
//
//			}
//
//		}
//		
//		
		
		
//		MainPanelBackground b = new MainPanelBackground(data);
//		b.addMouseListener(this);
//		b.addMouseMotionListener(this);
//		add(b);
			
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
