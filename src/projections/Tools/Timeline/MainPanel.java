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

import projections.Tools.Timeline.RangeQueries.Range1D;
import projections.Tools.Timeline.RangeQueries.RangeQueryArrayList;
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
	
	private Map<Integer,RangeQueryArrayList<EntryMethodObject>> entryMethodObjectsToPaintForEachPE;
	
	
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
		
		
		
		int count = 0;
		for(Entry<Integer, RangeQueryArrayList<EntryMethodObject>> entry : entryMethodObjectsToPaintForEachPE.entrySet()) {
			Integer pe = entry.getKey();
			
			if(data.peTopPixel(pe) <= clip.y+clip.height+5 && data.peBottomPixel(pe) >= clip.y-5){

				RangeQueryArrayList<EntryMethodObject> l = entry.getValue();
				l.setQueryRange(leftClipTime, rightClipTime);
				for(EntryMethodObject o : l){
					o.paintMe((Graphics2D) g, width);
					count ++;
				}
			}
		}
		System.out.println("Should have just painted up to " + count + " entry method objects in MainPanel of size (" + getWidth() + "," + getHeight() + ") clip=(" + clip.x + "," + clip.y + "," + clip.width + "," + clip.height + ")");
		
		
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
		
		// Add the entry method instances (EntryMethodObject) to the panel for displaying
				


		entryMethodObjectsToPaintForEachPE = new TreeMap<Integer,RangeQueryArrayList<EntryMethodObject>>();	
		for(Integer pe : data.allEntryMethodObjects.keySet()) {
			RangeQueryArrayList l = new RangeQueryArrayList();
			entryMethodObjectsToPaintForEachPE.put(pe, l);
			for(EntryMethodObject obj : data.allEntryMethodObjects.get(pe)){
				l.add(obj);
			}
		}
		
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
		

		// Add each user event 
		/** <LinkedList<UserEventObject>> */
		Iterator <Set <UserEventObject> > iter = data.allUserEventObjects.values().iterator();
		while(iter.hasNext()){
			/** <UserEventObject> */
			Iterator <UserEventObject> ue_iter = iter.next().iterator();
			while(ue_iter.hasNext()){
				UserEventObject ueo = ue_iter.next();
				this.add(ueo);						
			}
		}
		
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
