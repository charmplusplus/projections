package projections.Tools.Timeline;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.ToolTipManager;

import projections.Tools.Timeline.Data.RepresentedEntity;
import projections.Tools.Timeline.RangeQueries.Query1D;
import projections.Tools.Timeline.Data.SMPMsgGroup;
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
 *  @note Now the code is a bit more complex to handle the painting and mouseover events, but it is faster!
 *  
 */

public class MainPanel extends JPanel  implements Scrollable, MouseListener, MouseMotionListener 
{
	/** Should the painting be performed in multiple threads */
	private static final boolean RenderInParallel = false;

	/** A thread pool for use in rendering if RenderInParallel is true */
	private static ExecutorService threadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


	/** Stores information about mouse dragging in window */
	private int viewX, viewY;


	/** The backing model/view information for the Timeline */
	private Data data;
	private MainHandler handler;	

	/** Construct a main Timeline Panel */
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

		this.setOpaque(true);		
	}


	/** A little class to help render a sub-region of the graphics */
	public class SliceRenderer implements Runnable {
		private Graphics2D g;
		private int destinationX;
		private int destinationY;
		private BufferedImage b;
		public long paintedEntities;


		public SliceRenderer(Graphics2D g, BufferedImage b, int destinationX, int destinationY) {
			this.g = g;
			this.b = b;
			this.destinationX = destinationX;
			this.destinationY = destinationY;
		}

		public void run() {
			paintedEntities = paintAll(g);
		}

	}


	/** Paint the entire opaque panel*/
	public void paintComponent(Graphics g) {
		synchronized(data){
			if(RenderInParallel && data.numPs()>1){
				paintInParallel((Graphics2D)g);
			} else {
				paintSequentially((Graphics2D)g);
			}
		}
	}

	/** Directly paint the whole requested clipped region from within this thread */
	public void paintSequentially(Graphics2D g){
		final long startTime = System.nanoTime();
		Rectangle clip = g.getClipBounds();		
		MainWindow.performanceLogger.log(Level.INFO,"Rendering MainPanel Sequentially with clip: " + clip.x + "," +clip.y + " " + clip.width + "," + clip.height);

		paintAll(g);

		final long endTime = System.nanoTime();
		final long duration = endTime - startTime;
		MainWindow.performanceLogger.log(Level.INFO,"Time To Paint (sequential version): " + (duration/1000000) + " ms");

	}

	/** Use the thread pool to render slices (rows) of the display */
	public void paintInParallel(Graphics g){

		final long startTime = System.nanoTime();

		Rectangle clip = g.getClipBounds();

		MainWindow.performanceLogger.log(Level.INFO,"Rendering MainPanel with clip: " + clip.x + "," +clip.y + " " + clip.width + "," + clip.height);

		int piecesToRender = Runtime.getRuntime().availableProcessors();

		int heightOfEachSlice = clip.height/piecesToRender + 1;

		ArrayList<SliceRenderer> renderers = new ArrayList<SliceRenderer>();
		ArrayList<Future> futures = new ArrayList<Future>();

		for(int i=0; i<piecesToRender; i++){
			// Slice the clipped region into piecesToRender pieces
			int startYPixel = i*(heightOfEachSlice);
			int endYPixel = (1+i)*(heightOfEachSlice) - 1;
			if(endYPixel > clip.height-1)
				endYPixel = clip.height-1;

			int heightOfSlice = endYPixel - startYPixel + 1;		

			//			System.out.println("slice: startYPixel="+startYPixel+" endYPixel="+endYPixel+" heightOfSlice="+heightOfSlice + " clip.height=" + clip.height);

			if(heightOfSlice > 0){
				BufferedImage b = new BufferedImage(clip.width, heightOfSlice, BufferedImage.TYPE_INT_RGB);
				Graphics2D g2d = b.createGraphics();
				g2d.translate(-clip.x, -clip.y-startYPixel);

				g2d.setClip(clip.x, clip.y+startYPixel, clip.width, heightOfSlice);

				SliceRenderer s = new SliceRenderer(g2d, b, clip.x, clip.y+startYPixel);
				renderers.add(s);
				Future f = threadExecutor.submit(s);
				futures.add(f);
			}
		}


		// Await completion of all threads
		for(Future f : futures){
			try {
				f.get(); // wait for the Runnable to complete
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(100);
			} catch (ExecutionException e) {
				e.printStackTrace();
				System.exit(200);
			} 
		}


		// Paint final results based on renderings of each thread
		int count = 0;
		for(SliceRenderer s : renderers) {
			g.drawImage(s.b, s.destinationX, s.destinationY, null);
			count += s.paintedEntities;
		}


		final long endTime = System.nanoTime();
		final long duration = endTime - startTime;
		MainWindow.performanceLogger.log(Level.INFO,"Time To Paint " + count + " entities using " + piecesToRender +  " threads: " + (duration/1000000) + " ms");

	}

	/**
	 * Paint the desired clipped region for this component 
	 * 
	 * @return Number of entities rendered (for performance tuning)
	 */
	public long paintAll(Graphics2D g){
		paintBackground(g);
		Rectangle clip = g.getClipBounds();

		// Find time ranges to draw
		long leftClipTime = data.screenToTime(clip.x-5);
		long rightClipTime = data.screenToTime(clip.x+clip.width+5);

		// Draw entry method invocations
		int count1 = -1;

		// Determine which PEs are within the clip range:
		Collection<Integer> pesToRender = data.processorsInPixelYRange(clip.y, clip.y+clip.height-1);

		for(Integer pe : pesToRender){
			Query1D<EntryMethodObject> l = data.allEntryMethodObjects.get(pe);

			if(l != null){ // FIXME: this shouldn't be here but is to fix a race condition with reloading ranges once something is displayed
				Iterator<EntryMethodObject> iter = l.iterator(leftClipTime, rightClipTime);
				int maxFilledX = 0; // Tracks maximum x index of filled pixels for this PE, used to optimize away
				                    // drawing of multiple overlapping single pixel entry methods
				while(iter.hasNext()){
					EntryMethodObject o = iter.next();
					int emoMaxFilledX = o.paintMe((Graphics2D) g, getWidth(), maxFilledX);
					if (emoMaxFilledX > maxFilledX) {
						maxFilledX = emoMaxFilledX;
						count1++;
					}
				}
			}
		}



		// FIXME make this a range query of some sort so that only the appropriate PEs are painted
		// Draw user events
		int count2 = 0;
		if(data.showUserEvents()){
			count2 = 0;

			for(Integer pe : pesToRender){
				Query1D<UserEventObject> l = data.allUserEventObjects.get(pe);

				if(l != null){ // FIXME: this shouldn't be here but is to fix a race condition with reloading ranges once something is displayed
					Iterator<UserEventObject> iter = l.iterator(leftClipTime, rightClipTime);
					while(iter.hasNext()){
						UserEventObject o = iter.next();
						o.paintMe((Graphics2D) g, getWidth(), data);
						count2 ++;
					}
				}

			}
		}

		paintMessageSendLines(g, data.getMessageColor(), data.getBackgroundColor(), data.drawMessagesForTheseObjects);
		paintMessageSendLines(g, data.getMessageAltColor(), data.getBackgroundColor(), data.drawMessagesForTheseObjectsAlt);		

		return count1 + count2;
	}



	/** Paint the background, filling the entire clipping area */
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

				g.drawLine(0+data.getOffset(), y, width-data.getOffset(), y);
			}
		}		
	}

	/** Paint lines to represent messages */
	private void paintMessageSendLines(Graphics g, Color c, Color bgColor, Set<EntryMethodObject> drawMessagesForObjects){
		Graphics2D g2d = (Graphics2D) g;
		// paint the message send lines
		if (drawMessagesForObjects.size()>0) {
			for(EntryMethodObject obj : drawMessagesForObjects){
				TimelineMessage createdMsg = obj.creationMessage();
				if(createdMsg != null){					
					int pCreation = obj.pCreation;
					int pExecution = obj.pe;
					
					boolean smpMsgGrpFound = false;
					if(data.isSMPRun()){
						smpMsgGrpFound = data.makeSMPMsgGroup(obj);						
					}

					if(smpMsgGrpFound){
						int x1, x2;
						double y1, y2;
						EntryMethodObject recvObj;
						TimelineMessage oneMsg;
						int sendPe, recvPe;

						recvObj = data.toPaintSMPMsgGrp.recvWPe;
						oneMsg = recvObj.creationMessage();
						sendPe = recvObj.pCreation;
						recvPe = recvObj.pe;
						x1 = data.timeToScreenPixel(oneMsg.Time);			
						y1 = data.messageSendLocationY(sendPe);
						
						// Message executed (entry method starts) 
						x2 =  data.timeToScreenPixel(recvObj.getBeginTime());
						y2 = data.messageRecvLocationY(recvPe);

						// Draw thick background Then thin foreground recvCPe->recvWPe
						g2d.setPaint(bgColor);
						g2d.setStroke(new BasicStroke(4.0f));
						g2d.drawLine(x1,(int)y1,x2,(int)y2);
						g2d.setPaint(c);
						g2d.setStroke(new BasicStroke(2.0f));
						g2d.drawLine(x1,(int)y1,x2,(int)y2);

						recvObj = data.toPaintSMPMsgGrp.recvCPe;
						oneMsg = recvObj.creationMessage();
						sendPe = recvObj.pCreation;
						recvPe = recvObj.pe;
						x1 = data.timeToScreenPixel(oneMsg.Time);			
						y1 = data.messageSendLocationY(sendPe);
						
						// Message executed (entry method starts) 
						x2 =  data.timeToScreenPixel(recvObj.getBeginTime());
						y2 = data.messageRecvLocationY(recvPe);

						// Draw thick background Then thin foreground sendCPe->recvCPe
						g2d.setPaint(bgColor);
						g2d.setStroke(new BasicStroke(4.0f));
						g2d.drawLine(x1,(int)y1,x2,(int)y2);
						g2d.setPaint(c);
						g2d.setStroke(new BasicStroke(2.0f));
						g2d.drawLine(x1,(int)y1,x2,(int)y2);

						recvObj = data.toPaintSMPMsgGrp.sendCPe;
						oneMsg = recvObj.creationMessage();
						sendPe = recvObj.pCreation;
						recvPe = recvObj.pe;
						x1 = data.timeToScreenPixel(oneMsg.Time);			
						y1 = data.messageSendLocationY(sendPe);
						
						// Message executed (entry method starts) 
						x2 =  data.timeToScreenPixel(recvObj.getBeginTime());
						y2 = data.messageRecvLocationY(recvPe);

						// Draw thick background Then thin foreground sendWPe->recvCPe
						g2d.setPaint(bgColor);
						g2d.setStroke(new BasicStroke(4.0f));
						g2d.drawLine(x1,(int)y1,x2,(int)y2);
						g2d.setPaint(c);
						g2d.setStroke(new BasicStroke(2.0f));
						g2d.drawLine(x1,(int)y1,x2,(int)y2);						
					}else{
						// Message Creation point
						int x1 = data.timeToScreenPixel(createdMsg.Time);			
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


	/** 
	 * Load or Reload the timeline objects from the data object's tloArray.
	 *  
	 *  if useHelperThreads is true, the information about messages sent will be offloaded to a separate thread. 
	 *  This will allow the results to be returned faster, but if the message information is needed immedeately, 
	 *  this should be set to false.
	 *  
	 * @note caller must synchronize on data to prevent race conditions between rendering and modifying data
	 */
	public void loadTimelineObjects(boolean useHelperThreads, Component rootWindow, boolean showProgress) {

		// keeplines describes if the lines from message creation
		// to execution are to be retained or not.
		//		setCursor(new Cursor(Cursor.WAIT_CURSOR));

		data.createTLOArray(useHelperThreads, rootWindow, showProgress);

		handler.setData(data);
		//		handler.refreshDisplay(true);
		//		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

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

		// Sometimes it is hard to mouseover a single pixel wide object, so we expand searches until we find something nearby if necessary

		for(int verticalSlackPixels = 0; verticalSlackPixels< 5; verticalSlackPixels++){
			int verticalSlack = 0;
			switch(verticalSlackPixels){
			case 0:
				verticalSlack = 0;    // first look at actual coordinate
				break;
			case 1:
				verticalSlack = -1;   // Then look one pixel above ...
				break;
			case 2:
				verticalSlack = 1;
				break;
			case 3:
				verticalSlack = -2;
				break;
			case 4:
				verticalSlack = 2;
				break;
			}

			// Check to see which pe we are on
			final int whichPERow = (evt.getY()+verticalSlack) / data.singleTimelineHeight();
			final int verticalOffsetWithinRow = (evt.getY()+verticalSlack) % data.singleTimelineHeight();

			final int PE = data.whichPE(whichPERow);
			RepresentedEntity what = data.representedAtPixelYOffsetInRow(verticalOffsetWithinRow);
			if(PE >= 0){
				// Also allow a little slack horizontally within each PEs row
				for(int slackPixels = 0; slackPixels < 3; slackPixels++){

					final long timeL = data.screenToTime(evt.getX()   - slackPixels);
					final long timeR = data.screenToTime(evt.getX()+1 + slackPixels);

					if(what == RepresentedEntity.ENTRY_METHOD){

						// Find an entry method invocation that occurred at this time, and display its tooltip instead
						Query1D<EntryMethodObject> a = data.allEntryMethodObjects.get(PE);
						// FIXME: Somehow there may be a race condition where the number of processors changes, but data is not yet loaded.
						// unfortunately we can't synchronize on data because this executes in some GUI thread which had better not block, as it may block the progress bar which blocks the loading of data
						if(a == null)
							return null;

						Iterator<EntryMethodObject> b = a.iterator(timeL, timeR);

						// Iterate through all the things matching this timestamp so we can get the last one (which should be painted in front)
						EntryMethodObject frontmostVisibleObject = null;
						while(b.hasNext()){
							EntryMethodObject o = b.next();
							if(o.isDisplayed())
								frontmostVisibleObject = o;
						}

						if(frontmostVisibleObject != null){
							return frontmostVisibleObject;
						}

					}
					else if(what == RepresentedEntity.USER_EVENT){

						// Find an entry method invocation that occurred at this time, and display its tooltip instead
						Query1D<UserEventObject> a = data.allUserEventObjects.get(PE);

						// FIXME: Somehow there may be a race condition where the number of processors changes, but data is not yet loaded.
						if(a == null)
							return null;

						Iterator<UserEventObject> b = a.iterator(timeL, timeR);
						int row = data.getNumUserEventRows() - (verticalOffsetWithinRow / data.singleUserEventRectHeight()) - 1;

						// Iterate through all the things matching this timestamp so we can get the last one (which should be painted in front)
						UserEventObject o = null;
						while(b.hasNext()){
							o = b.next();
							if (o.getNestedRow() == row)
								return o;
						}
						if(o != null){
							return o;
						}
					}
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
