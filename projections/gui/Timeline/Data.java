package projections.gui.Timeline;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.*;

import java.awt.Component;
import java.awt.Font;
import java.awt.Color;
import projections.analysis.*;
import projections.gui.MainWindow;
import projections.gui.OrderedIntList;
import projections.gui.OrderedUsageList;
import projections.misc.*;


/**
 * This class is responsible for providing information on what should be visualized:
 * 		-- The time range
 *  	-- The scale factor(used when zooming)
 * 		-- The list of processors for which we draw timelines
 * 		-- Sets of the EntryMethodObject, UserEventObject, and Message lines that are to be displayed
 *      -- Information about entry method names
 *      -- whether idle time is displayed
 *      -- whether user events are displayed
 *      -- Various margins/offsets/insets used when painting
 * 		-- A selection and highlight
 * 
 *  Style information is also to be found here
 * 		-- Colors for the background/foreground
 * 		-- Fonts to be used for the axis and labels
 *
 *  Also many utility functions are here:
 *      -- Conversions between screen coordinates and times
 *  	-- Handling user selections/highlights
 *  
 *  Additionally some screwy things are here.
 *  
 *  
 *  This class requires a handler(which implements an appropriate interface) be provided. This
 *  handler is used when things change enough that a repaint or re-layout is required
 * 
 * @author idooley et. al.
 *
 */

public class Data
{
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;

	private MainHandler modificationHandler = null;

	/** A factor describin how zoomed in we are */
	private float scaleFactor = 1.0f;

	private double preferredViewTime = -1.0;

	/** The most recently known scaled screen width 
	 * 
	 * @note It is likely dangerous to use this unless you 
	 * really know what you are doing
	 * 
	 * */
	private int mostRecentScaledScreenWidth;

	/** The list of processors used in this tool. Can differ from those used in other tools */
	OrderedIntList processorList;
	/** The list of pes displayed, sorted in vertical ordering <Integer, Integer>*/
	HashMap peToLine;

	/** If true, color entry method invocations by Object ID */
	private boolean colorByObjectId;
	
	/** If true, color the entry method invocations by the memory used at that point in time */
	private boolean colorByMemoryUsage;

	/** If true, color the entry method invocations by a user supplied parameter(like timestep) */
	private boolean colorByUserSupplied;
	
	private int[]          entries;

	private Color[]        entryColor;

	/** Each value of the TreeMap is a TreeSet (sorted list) of EntryMethodObject's .
	 *  Each key of the TreeMap is an Integer pe 
	 *  <Integer,LinkedList<EntryMethodObject> >
	 */
	public TreeMap allEntryMethodObjects;

	/** Each value in this TreeMap is a TreeSet of UserEventObject's .
	 *  Each key of the TreeMap is an Integer pe
	 *  <Integer,LinkedList<UserEventObject> >
	 */
	public TreeMap allUserEventObjects;


	/** processor usage indexed by PE */
	float[] processorUsage;

	/** idle usage indexed by PE */
	float[] idleUsage;

	/** pack usage indexed by PE */
	float[] packUsage;

	/** entry usage list indexed by PE */
	OrderedUsageList[] entryUsageList;

	/** The start time for the time range. */
	private long beginTime; 

	/** The end time for the time range. */
	private long endTime;

	/** The old begin time, used to make loading of new ranges more
		efficient if we already have the object data loaded. Then we
		can just take a subset of our data instead of reloading it
		from the logs
	*/
	long oldBT;
	/** The old end time */
	long oldET; 
	
	/** The miniumum and maximum memory usage that have been seen so far */
	long minMem, maxMem;

	/** Stores various lookup tables for messages and associated entry points */
	MessageStructures messageStructures;
	
	/** Determine whether pack times, idle regions, or message send ticks should be displayed */		
	private boolean showPacks, showIdle, showMsgs, showUserEvents;


	/** The font used by the LabelPanel */
	public Font labelFont;
	/** The font used by the time labels on the TimelineAxisCanvas */
	public Font axisFont;

	/** If set to true we should try to use minimal margins around our drawings. */
	private boolean useMinimalView=false;

	/** If set to true we should try to use minimal margins and simpler text */
	public boolean useMinimalView(){
		return useMinimalView;
	}

	/** A set of objects for which we draw their creation message lines */
	public Set drawMessagesForTheseObjects;
	/** A set of objects for which we draw their creation message lines in an alternate color */
	public Set drawMessagesForTheseObjectsAlt;
	
	
	/** The line thickness for the mesg send lines */
	public float messageLineThickness=2.5f;
	
	private boolean useCustomColors=false;
	
	/** A custom foreground color that can override the application wide background pattern. Used by NoiseMiner to set a white background */
	private Color customForeground;
	private Color customBackground;
	

	/** A constructor that takes in a TimelineContainer(for handling some events) 
	 *  and provides sensible default values for various parameters 
	 * */
	public Data(MainHandler rh)
	{ 
		modificationHandler = rh;

		showPacks = false;
		showMsgs  = true;
		showIdle  = true;
		showUserEvents = true;
		
		processorList = MainWindow.runObject[myRun].getValidProcessorList();

		messageStructures = new MessageStructures(this);
		
		oldBT = -1;
		oldET = -1;

		processorUsage = null;
		entryUsageList = null;

		minMem = Integer.MAX_VALUE;
		maxMem = Integer.MIN_VALUE;	

		/** The selected time range for which we display the timeline.
		 *  The value is by default the entire range found in the log files
		 *  It is modified by Window when the user enters a new 
		 *  value in the "select ranges" dialog box.
		 */
		beginTime = 0;
		endTime = MainWindow.runObject[myRun].getTotalTime();

		drawMessagesForTheseObjects = new HashSet();
		drawMessagesForTheseObjectsAlt = new HashSet();
		
		allEntryMethodObjects = null;
		entries = new int[MainWindow.runObject[myRun].getNumUserEntries()];
		entryColor = MainWindow.runObject[myRun].getColorMap();

		labelFont = new Font("SansSerif", Font.PLAIN, 12); 
		axisFont = new Font("SansSerif", Font.PLAIN, 10);

		highlightedObjects = new HashSet();
		
		colorByMemoryUsage = false;
		colorByObjectId = false;
		colorByUserSupplied = false;
		
		
	}
	/** 
	 * Add the data for a new processor to this visualization
	 */
	public void addProcessor(int pCreation){
		int oldNumP = processorList.size();
		processorList.insert(pCreation);
		int newNumP = processorList.size();

		peToLine = null;
		
		if(oldNumP != newNumP)
			modificationHandler.notifyProcessorListHasChanged();
	
	}

	/** Change the font sizes used */
	public void setFontSizes(int labelFontSize, int axisFontSize, boolean useBoldForLabel){
		if(useBoldForLabel)
			labelFont = new Font("SansSerif", Font.BOLD, labelFontSize); 
		else
			labelFont = new Font("SansSerif", Font.PLAIN, labelFontSize); 

		axisFont = new Font("SansSerif", Font.PLAIN, axisFontSize);
	}


	public long beginTime(){
		return beginTime;
	}


	
	public boolean colorbyObjectId(){
		return colorByObjectId;
	}

	
	
	/** Load the initial array of timeline objects 
	 *  
	 *  @note if a new processor has been added, then 
	 *  	  this will not be called. the new proc's
	 *        data will be retrieved using getData()
	 *        which calls createTL() 
	 */
	public void createTLOArray()
	{
		
		// Kill off the secondary processing threads if needed
		messageStructures.kill();
		
		// Generate the index files
//		MainWindow.runObject[myRun].logLoader.createTimeIndexes(processorList);
		
		// Can we reuse our already loaded data?
		if(beginTime >= oldBT && endTime <= oldET){
		
			/** <Integer,LinkedList<EntryMethodObject>> */
			TreeMap oldEntryMethodObjects = allEntryMethodObjects;
			/** <Integer,LinkedList<UserEventObject>> */
			TreeMap oldUserEventObjects = allUserEventObjects;
			
			allEntryMethodObjects = new TreeMap();
			allUserEventObjects = new TreeMap();

			// Remove any unused objects from our data structures 
			// (the components in the JPanel will be regenerated later from this updated list)
			processorList.reset();
			int p = processorList.nextElement();
			while (p != -1) {
				Integer pe = new Integer(p);
					
				if(oldEntryMethodObjects.containsKey(pe)){
					// Reuse the already loaded data
					allEntryMethodObjects.put(pe, oldEntryMethodObjects.get(pe));
					allUserEventObjects.put(pe, oldUserEventObjects.get(pe));

					// Drop elements from mesgVector and allEntryMethodObjects outside range
					Iterator iter = ((LinkedList)allEntryMethodObjects.get(pe)).iterator();
					while(iter.hasNext()){
						EntryMethodObject obj = (EntryMethodObject) iter.next();
						if(obj.getEndTime() < beginTime || obj.getBeginTime() > endTime){
							iter.remove();
						}
					}

					// Drop elements from userEventsArray outside range
					Iterator iter2 = ((LinkedList)allUserEventObjects.get(pe)).iterator();
					while(iter2.hasNext()){
						UserEventObject obj = (UserEventObject) iter2.next();
						if(obj.EndTime < beginTime || obj.BeginTime > endTime){
							iter2.remove();
						}
					}
				}

				p = processorList.nextElement();
			}
			
		} else {
			// We need to reload everything
			allEntryMethodObjects = new TreeMap();
			allUserEventObjects = new TreeMap();
		}
		
		oldBT = beginTime;
		oldET = endTime;
		
		
		//==========================================	
		// Do multithreaded file reading
		// TODO this could be made faster for times where the set of processors to be loaded is non-contiguous

		Date startReadingTime  = new Date();
		
		processorList.reset();
		int numPs = processorList.size();
		ProgressMonitor progressBar = 
			new ProgressMonitor(MainWindow.runObject[myRun].guiRoot, "Reading timeline data in parallel","", 0, numPs+1);
		progressBar.setMillisToPopup(0);
		progressBar.setMillisToDecideToPopup(0);
		progressBar.setProgress(0);
				
		// Create worker threads
		LinkedList readyReaders = new LinkedList();
		
		for (int p=0; p<numPs; p++) {
			int pe = processorList.nextElement();
			if(!allEntryMethodObjects.containsKey(new Integer(pe))) {
				readyReaders.add(new ThreadedFileReader(pe,p,this));
			}
		}
				
		int totalToLoad = readyReaders.size();
		progressBar.setMaximum(totalToLoad);
		int numConcurrentThreads = 16;
		
		if(totalToLoad < numConcurrentThreads){
			numConcurrentThreads = totalToLoad;
		}
		
		// execute reader threads, a few at a time, until all have executed
		LinkedList spawnedReaders = new LinkedList();
		while(readyReaders.size() > 0 || spawnedReaders.size() > 0){

			//------------------------------------
			// spawn some threads
			ThreadedFileReader r;
			while(readyReaders.size()>0 && spawnedReaders.size()<numConcurrentThreads){
				r = (ThreadedFileReader) readyReaders.removeFirst(); // retrieve and remove from list
				spawnedReaders.add(r);
				r.start(); // will cause the run method to be executed
			}

			//------------------------------------
			// update the progress bar
			int doneCount = totalToLoad-readyReaders.size()-spawnedReaders.size();
			if (!progressBar.isCanceled()) {
				progressBar.setNote(doneCount+ " of " + totalToLoad);
				progressBar.setProgress(doneCount);
			} else {
				break;
			}

			//------------------------------------
			// wait on the threads to complete
			Iterator iter = spawnedReaders.iterator();
			int waitMillis = 1000; // wait for 1000 ms for the first thread, and 1 ms for each additional thread
			while(iter.hasNext()){
				r = (ThreadedFileReader) iter.next();
				try {
					r.join(waitMillis);
					waitMillis = 1;
					if(! r.isAlive()) {
						// Thread Finished
						iter.remove();
					} 
				}
				catch (InterruptedException e) {
					throw new RuntimeException("Thread was interrupted. This should not ever occur");
				}
			}
						
		}

		if(totalToLoad > 0){
			Date endReadingTime  = new Date();
			System.out.println("Time to read " + totalToLoad +  " input files(using up to " + numConcurrentThreads + " threads): " + ((double)(endReadingTime.getTime() - startReadingTime.getTime())/1000.0) + "sec");
		}
			
		if (!progressBar.isCanceled()) {
			progressBar.setNote("Finishing");
			progressBar.setProgress(numPs);
		}
		
		for (int e=0; e<MainWindow.runObject[myRun].getNumUserEntries(); e++) {
			entries[e] = 0;
		}
		
		processorUsage = new float[numPEs()];
		entryUsageList = new OrderedUsageList[numPEs()];
		float[] entryUsageArray = new float[MainWindow.runObject[myRun].getNumUserEntries()];
		idleUsage  = new float[numPEs()];
		packUsage  = new float[numPEs()];

		for (int i=0; i<MainWindow.runObject[myRun].getNumUserEntries(); i++) {
			entryUsageArray[i] = 0;
		}
		
		for (int p=0; p<numPEs(); p++) {
			processorUsage[p] = 0;
			idleUsage[p] = 0;
			packUsage[p] = 0;
		}

		Iterator pe_iter = allEntryMethodObjects.keySet().iterator();
		while(pe_iter.hasNext()){
			Integer pe = (Integer)pe_iter.next();
			LinkedList objs = (LinkedList)allEntryMethodObjects.get(pe);
			Iterator obj_iter = objs.iterator();
			while(obj_iter.hasNext()){
				EntryMethodObject obj = (EntryMethodObject) obj_iter.next();

				float usage = obj.getUsage();
				int entrynum = obj.getEntry();

				if (entrynum >=0) {
					entries[entrynum]++;
					processorUsage[pe.intValue()] += usage;
					packUsage[pe.intValue()] += obj.getPackUsage();
					entryUsageArray[entrynum] += obj.getNonPackUsage();
				} else {
					idleUsage[pe.intValue()] += usage;
				}
			}

			entryUsageList[pe.intValue()] = new OrderedUsageList();
			for (int i=0; i<MainWindow.runObject[myRun].getNumUserEntries(); i++) {
				if (entryUsageArray[i] > 0) {
					entryUsageList[pe.intValue()].insert(entryUsageArray[i], i);
				}
			}      
		} 

		updatePEVerticalOrdering();
		
		progressBar.close();
		
		// Spawn a thread that computes some secondary message related data structures
		messageStructures.create();
	
	}
	
	public void decreaseScaleFactor(){
		setScaleFactor( (float) ((int) (getScaleFactor() * 4) - 1) / 4 );
	}

	/** Relayout and repaint everything */
	private void displayMustBeRedrawn(){
		if(modificationHandler != null){
			modificationHandler.refreshDisplay(true);
		}
	}

	/** repaint everything */
	public void displayMustBeRepainted(){
		if(modificationHandler != null){
			modificationHandler.refreshDisplay(false);
		}
	}

	/** remove all the message send lines */
	public void clearAllLines() {
		drawMessagesForTheseObjects.clear();
		drawMessagesForTheseObjectsAlt.clear();
		displayMustBeRepainted();
	}
	
	/** Add or Remove a new line to the visualization representing the sending of a message */
	public void toggleMessageSendLine(EntryMethodObject obj) {
		
		TimelineMessage created_message = obj.creationMessage();

		if(created_message != null){
			
			if(drawMessagesForTheseObjects.contains(obj)){
				drawMessagesForTheseObjects.remove(obj);
			} else {
				drawMessagesForTheseObjects.add(obj);
			}
			
			displayMustBeRepainted();
			
		} else {
			modificationHandler.displayWarning("Message was sent from outside the current time range");
		}
	}
	
	/** Add a new line to the visualization representing the sending of a message.
	 * @note the caller should call 	displayMustBeRepainted() after adding all desired messages
	 */
	public void addMessageSendLine(EntryMethodObject obj) {
			drawMessagesForTheseObjects.add(obj);
	}
	
	public void addMessageSendLineAlt(Set s) {
		drawMessagesForTheseObjectsAlt.addAll(s);
	}

	/** Add a set of objects for which we want their creation messages to be displayed
	 * @note the caller should call 	displayMustBeRepainted() after adding all desired messages
	 */
	public void addMessageSendLine(Set s) {
		drawMessagesForTheseObjects.addAll(s);
	}
	
	/** Remove a set of objects so their creation messages are no longer displayed
	 * @note the caller should call 	displayMustBeRepainted() after removing all desired messages
	 */
	public void removeMessageSendLine(Set s) {
		drawMessagesForTheseObjects.removeAll(s);
		drawMessagesForTheseObjectsAlt.removeAll(s);
	}
	
	public void clearMessageSendLines() {
		drawMessagesForTheseObjects.clear();
		drawMessagesForTheseObjectsAlt.clear();
	}
	
	
	public long endTime(){
		return endTime;
	}
	public int[] entries(){
		return entries;
	}
	public Color[] entryColor(){
		return entryColor;
	}
	public Color getBackgroundColor(){
		if(useCustomColors)
			return customBackground;
		else
			return MainWindow.runObject[myRun].background;
	}


	/** Load the timeline for processor pe.
	 * 
	 *  This method loads the timeline into: timelineUserEventObjectsArray, allEntryMethodObjects, 
	 * 
	 * 
	 * */
	void getData(Integer pe)  
	{

		LinkedList tl = new LinkedList();
		LinkedList userEvents= new LinkedList();
				
		try {
			if (MainWindow.runObject[myRun].hasLogData()) {
				MainWindow.runObject[myRun].logLoader.createtimeline(pe.intValue(), beginTime, endTime, tl, userEvents);
			} else {
				System.err.println("createTL: No log files available!");
				return;
			}
		} catch (LogLoadException e) {
			System.err.println("LOG LOAD EXCEPTION");
			return;
		}
		
		// The user events are simply that which were produced by createtimeline
		allUserEventObjects.put(pe,userEvents);
		
		// The entry method objects must however be constructed
		allEntryMethodObjects.put(pe, new LinkedList());
		LinkedList perPEObjects = (LinkedList)allEntryMethodObjects.get(pe);
		
		// proc timeline events
		Iterator iter = tl.iterator();
		while(iter.hasNext()){
		
			TimelineEvent tle = (TimelineEvent)iter.next();
			
			// Construct a list of messages sent by the object
			Vector msglist = tle.MsgsSent;
			TreeSet msgs = new TreeSet();
			if(msglist!=null){
				msgs.addAll( msglist );
			}
			
			// Construct a list of message pack times for the object
			Vector packlist = tle.PackTimes;
			int numpacks;
			if (packlist == null) {
				numpacks = 0;
			} else {
				numpacks = packlist.size();
			}
			PackTime[] packs = new PackTime[numpacks];
			for (int p=0; p<numpacks; p++) {
				packs[p] = (PackTime)packlist.elementAt(p);
			}
		
			// Create the object itself
			perPEObjects.add(new EntryMethodObject(this, tle, msgs, packs, pe.intValue()));
	
			if(tle!=null && tle.memoryUsage!=null){
			  if(tle.memoryUsage.longValue() > maxMem)
				  maxMem = tle.memoryUsage.longValue();
			  if(tle.memoryUsage.longValue() < minMem)
				  minMem = tle.memoryUsage.longValue();
			}			
		
		}
		
		if(memoryUsageValid())
			System.out.println("memory usage seen in the logs ranges from : " + minMem/1024/1024 + "MB to " + maxMem/1024/1024 + "MB");
		
	}
	
	/** Did the logs we loaded so far contain any memory usage entries? */
	private boolean memoryUsageValid() {
		return maxMem != Integer.MIN_VALUE && minMem != Integer.MAX_VALUE && maxMem != 0;
	}

	public Color getForegroundColor(){
		if(useCustomColors)
			return customForeground;
		else
			return MainWindow.runObject[myRun].foreground;
	}


	public int getNumUserEvents() {
		Iterator iter = allUserEventObjects.values().iterator();
		int num = 0;
		while(iter.hasNext()){
			num += ((LinkedList)(iter.next())).size();
		}
		return num;
	}

	public float getScaleFactor(){
		return scaleFactor;
	}

	public void increaseScaleFactor(){
		setScaleFactor( (float) ((int) (getScaleFactor() * 4) + 1) / 4 );
	}

	
	/**	 the width of the timeline portion that is drawn(fit so that labels are onscreen) */
	public int lineWidth(int actualDisplayWidth) {
		return actualDisplayWidth - 2*offset();
	}

	public int maxLabelLen(){
		return 70;
	}   


	/** Number of processors in the processor List */
	public int numPs(){
		return processorList.size();
	}
	/** The maximum processor index in the processor list, or -1 if null processor list */
	public int numPEs(){
		return MainWindow.runObject[myRun].getNumProcessors();		
	}	

	/** the left/right margins for the timeline & axis. 
	 * Needed because text labels may extend before and 
	 * after the painted line 
	 */
	public int offset(){
		if(useMinimalView())
			return maxLabelLen()/2;
		else
			return 5 + maxLabelLen()/2;

	}


	public int leftOffset(){
		return offset();
	}

	public int rightOffset(){
		return offset();
	}

	public OrderedIntList processorList() {
		return processorList;
	}
	/** The width we should draw in, compensated for the scaling(zoom) factor 
	 * 
	 * 
	 * @note this should only be called by a layout manager that knows the size
	 * of the screen.
	 * 
	 * */
	public int scaledScreenWidth(int actualDisplayWidth){
		mostRecentScaledScreenWidth = (int)((float)actualDisplayWidth * scaleFactor);
		return mostRecentScaledScreenWidth;
	}


	/** The height of the panel that should be used to draw the timelines  */
	public int screenHeight(){
		if(useMinimalView())
			return singleTimelineHeight()*numPs();
		else
			return singleTimelineHeight()*numPs()+15;
	}


	/** The height of the timeline event object rectangles */
	public int barheight(){
		if(useCompactView())
			return 12;
		else
			return 16;
	}
		
	/** Get the height required to draw a single PE's Timeline */
	public int singleTimelineHeight(){
		if(useCompactView())
			return barheight()+1;
		else if(useMinimalView())
			return barheight() + 10;
		else
			return barheight() + 18;
	}

	
	
	/** get the height of each user event rectangle */
	public int userEventRectHeight(){
		if(useCompactView())
			return 0;
		else
			return 5;
	}
	

	public void setColors(Color backgroundColor, Color foregroundColor){
		customForeground = foregroundColor;
		customBackground = backgroundColor;
		useCustomColors = true;
		displayMustBeRedrawn();
	}
	public void setHandler(MainHandler rh)
	{ 
		modificationHandler = rh;
		displayMustBeRedrawn();
	}
	public void setProcessorList(OrderedIntList procs){
		processorList = procs.copyOf();
	}

	/** Choose a new time range to display. 
	 * 	Scale will be reset to zero, and
	 *  the old range will be recorded */
	public void setNewRange(long beginTime, long endTime) {
		this.beginTime = beginTime;
		this.endTime = endTime;
		setScaleFactor(1.0f);
	}


	public void setRange(long beginTime, long endTime){
		this.beginTime = beginTime;
		this.endTime = endTime;
	}

	/** Set the scale factor. This will cause the handler to layout and repaint panels and update buttons */
	public void setScaleFactor(float scale_){
		scaleFactor = scale_;
		if (scaleFactor < 1.0) {
			scaleFactor = 1.0f;
		}
		displayMustBeRedrawn();
	}


	/** an alias for beginTime() */
	public long startTime(){
		return beginTime();
	}


	public long totalTime(){
		return endTime-beginTime;
	}


	/** The pixel x-coordinates for the selection to be highlighted
	 * 
	 * @note the AxisOverlayPanel, MainOverlayPanel, or others may draw the 
	 *       highlight in any fashion they choose. The two selection points
	 *       are not guaranteed to be in sorted order.
	 *       
	 */
	private int selection1=-1, selection2=-1;
	private int highlight=-1;

	public boolean selectionValid(){
		return (selection1>=0 && selection2>=0 && selection1!=selection2);
	}

	public boolean highlightValid(){
		return (highlight>=0);
	}

	/** Invalidate the current selection
	 * 
	 * @note Call this when a window resizes or the selection no longer should be displayed
	 * 
	 */
	public void invalidateSelection(){
		selection1=-1;
		selection2=-1;
		modificationHandler.refreshDisplay(false);
	}	


	/** Get the left selection coordinate 
	 * 
	 * @note This should only be called if selectionValid() already returns true
	 * */
	public int leftSelection(){
		if(selection1 < selection2)
			return selection1;
		else
			return selection2;
	}

	/** Get the right selection coordinate 
	 * 
	 * @note This should only be called if selectionValid() already returns true
	 * */
	public int rightSelection(){
		if(selection1 < selection2)
			return selection2;
		else
			return selection1;
	}

	/** Get the width of the selection in pixels
	 * 
	 * @note This should only be called if selectionValid() already returns true
	 * */
	public int selectionWidth(){
		return rightSelection() - leftSelection();
	}

	/** Set the first selection boundary x screen/pixel coordinate */
	public void setSelection1(int value){
		selection1 = value;
		if(selectionValid())
			modificationHandler.refreshDisplay(false);
	}

	/** Set the second selection boundary x screen/pixel coordinate */
	public void setSelection2(int value){
		selection2 = value;
		if(selectionValid())
			modificationHandler.refreshDisplay(false);
	}
	public void removeHighlight() {
		highlight = -1;
		modificationHandler.refreshDisplay(false);
	}
	public void setHighlight(int x) {
		highlight = x;
		modificationHandler.refreshDisplay(false);
	}
	public int getHighlight() {
		return highlight;
	}
	public double getHighlightTime() {
		return screenToTime(getHighlight());
	}
	public double leftSelectionTime() {
		return screenToTime(leftSelection());
	}
	public double rightSelectionTime() {
		return screenToTime(rightSelection());
	}

	/** Convert screen coordinates to time
	 * 
	 * @note requires that mostRecentScaledScreenWidth be correct prior to invocation,
	 * so you should call  scaledScreenWidth(int actualDisplayWidth) before this
	 */
	public long screenToTime(int xPixelCoord){
		double fractionAlongAxis = ((double) (xPixelCoord-leftOffset())) /
		((double)(mostRecentScaledScreenWidth-2*offset()));

		return Math.round(beginTime + fractionAlongAxis*(endTime-beginTime));	
	}

	/** Convert time to screen coordinate, The returned pixel is the central pixel for this time if a microsecond is longer than one pixel
	 * 
	 * @note requires that mostRecentScaledScreenWidth be correct prior to invocation,
	 * so you should call  scaledScreenWidth(int actualDisplayWidth) before this
	 */
	public int timeToScreenPixel(double time) {
		double fractionAlongTimeAxis =  ((double) (time-beginTime)) /((double)(endTime-beginTime));
		return offset() + (int)Math.round(fractionAlongTimeAxis*(double)(mostRecentScaledScreenWidth-2*offset()));
	}
	
	/** Convert time to screen coordinate, The returned pixel is the central pixel for this time if a microsecond is longer than one pixel
	 * 
	 * @note requires that mostRecentScaledScreenWidth be correct prior to invocation,
	 * so you should call  scaledScreenWidth(int actualDisplayWidth) before this
	 */
	public int timeToScreenPixelRight(double time) {
		double fractionAlongTimeAxis =  ((double) (time+0.5-beginTime)) /((double)(endTime-beginTime));
		return offset() + (int)Math.floor((double)fractionAlongTimeAxis*(double)(mostRecentScaledScreenWidth-2*offset()));
	}
	
	/** Convert time to screen coordinate, The returned pixel is the leftmost pixel for this time if a microsecond is longer than one pixel
	 * 
	 * @note requires that mostRecentScaledScreenWidth be correct prior to invocation,
	 * so you should call  scaledScreenWidth(int actualDisplayWidth) before this
	 */
	public int timeToScreenPixelLeft(double time) {
		double fractionAlongTimeAxis =  ((time-0.5-(double)beginTime)) /((double)(endTime-beginTime));
		return offset() + (int)Math.ceil(fractionAlongTimeAxis*(double)(mostRecentScaledScreenWidth-2*offset()));
	}

	
	/** Convert time to screen coordinate, The returned pixel is the central pixel for this time if a microsecond is longer than one pixel */
	public int timeToScreenPixel(double time, int assumedScreenWidth) {
		double fractionAlongTimeAxis =  ((time-(double)beginTime)) /((double)(endTime-beginTime));
		return offset() + (int)(fractionAlongTimeAxis*(double)(assumedScreenWidth-2*offset()));
	}
	
	/** Convert time to screen coordinate, The returned pixel is the leftmost pixel for this time if a microsecond is longer than one pixel */
	public int timeToScreenPixelLeft(double time, int assumedScreenWidth) {
		double fractionAlongTimeAxis =  ((time-0.5-(double)beginTime)) /((double)(endTime-beginTime));
		return offset() + (int)Math.ceil(fractionAlongTimeAxis*(double)(assumedScreenWidth-2*offset()));
	}
	
	/** Convert time to screen coordinate, The returned pixel is the rightmost pixel for this time if a microsecond is longer than one pixel */
	public int timeToScreenPixelRight(double time, int assumedScreenWidth) {
		double fractionAlongTimeAxis =  ( (time+0.5-(double)beginTime)) /((double)(endTime-beginTime));
		return offset() + (int)Math.floor(fractionAlongTimeAxis*(double)(assumedScreenWidth-2*offset()));
	}
	
		

	/** Set the preferred position for the horizontal view or scrollbar  */
	public void setPreferredViewTimeCenter(double time) {
		if(time > beginTime && time < endTime)
			preferredViewTime = time;
	}

	/** Get the preferred position for the horizontal view in screen pixels.
	 	@note must be called after scaledScreenWidth(newWidth)
	 */
	public int getNewPreferredViewCenter(int newScreenWidth){
		double value = preferredViewTime;
		int coord = timeToScreenPixel(value,newScreenWidth);
		return coord;
	}

	
	/** Discard the previously stored desired view. Does NOT reset the view */
	public void resetPreferredView(){
		preferredViewTime = -1.0;
	}	
	
	public boolean hasNewPreferredView(){
		if (preferredViewTime >= 0.0 && scaleFactor > 1.0)
			return true;
		else
			return false;
	}

	
	/** Enable or disable the use of minimal margins and other features 
	 * 
	 * The mini-timelines used for the NoiseMiner Exemplar screen will set this to true
	 * */
	public void setUseMinimalMargins(boolean useMinimalMargins) {
		this.useMinimalView = useMinimalMargins;
	}
	
	
	private boolean keepViewCentered = false;
	/** Request that the layout manager not change the scrollbar position, but rather keep it centered on the same location */ 
	public void keepViewCentered(boolean b){
		keepViewCentered = b;
	}	
	public boolean keepViewCentered() {
		return keepViewCentered;
	}
	
	/** The height of the little line below the entry method designating a message send */
	public int messageSendHeight() {
		if(this.useCompactView()){
			return 0;
		} else {
			return 5;
		}
	}
	/** The height of the rectangle that displays the message pack time below the entry method */
	public int messagePackHeight() {
		return 3;
	}
	
	/** Do something when the user right clicks on an entry method object */
	public void entryMethodObjectRightClick(EntryMethodObject obj) {
		// pCreation, EventID, pCurrent, 
		if(! useMinimalView()){
			
			addProcessor(obj.pCreation);
			toggleMessageSendLine(obj);
			
		}		
	}

	
	/** A set of objects to highlight. The paintComponent() methods for the objects 
	 * should paint themselves appropriately after determining if they are in this set 
	 */
	private Set highlightedObjects;
 	
	/** Highlight the message links to the object upon mouseover */
	private boolean traceMessagesOnHover;
	
	/** Highlight the other entry method invocations upon mouseover */
	private boolean traceOIDOnHover;

	/** Should we use a very compact view, with no message sends, or user events */
	private boolean useCompactView;

	private Component guiRoot;
	
	/** Clear any highlights created by HighlightObjects() */
	public void clearObjectHighlights() {
		highlightedObjects.clear();
	}
	
	/** Highlight the given set of timeline objects */
	public void HighlightObjects(Set objects) {
		highlightedObjects.addAll(objects);
	}

	/** Determine if an object should be dimmed. 
	 * If there are any objects set to be highlighted, 
	 * all others will be dimmed 
	 */
	public boolean isObjectDimmed(Object o){
		if(highlightedObjects.size() == 0)
			return false;
		else
			return ! highlightedObjects.contains(o);
	}
		
	/** Determine if any objects should be dimmed. 
	 */
	public boolean isAnyObjectDimmed(){
		return highlightedObjects.size()>0;
	}
		
	

	public boolean traceMessagesOnHover() {
		return traceMessagesOnHover;
	}
	
	public boolean traceOIDOnHover() {
		return traceOIDOnHover;
	}
	
	public void setTraceMessagesOnHover(boolean traceMessagesOnHover) {
		this.traceMessagesOnHover = traceMessagesOnHover;
		
		if(traceMessagesOnHover)
			SetToolTipDelayLarge();
		else
			SetToolTipDelaySmall();
		
	}
		
	public void setTraceOIDOnHover(boolean showOIDOnHover) {
		this.traceOIDOnHover = showOIDOnHover;
	}
	
	
	public void SetToolTipDelaySmall() {
		ToolTipManager.sharedInstance().setInitialDelay(0);
		ToolTipManager.sharedInstance().setDismissDelay(600000);	
	}
	

	public void SetToolTipDelayLarge() {
		ToolTipManager.sharedInstance().setInitialDelay(2000);
		ToolTipManager.sharedInstance().setDismissDelay(10000);	
	}
	
	public Color getMessageColor() {
		return Color.white;
	}
	
	public Color getMessageAltColor() {
		return Color.yellow;
	}

	public void showUserEvents(boolean b) {
		showUserEvents = b;
	}

	public boolean showUserEvents() {
		if(useCompactView())
			return false;
		else
			return showUserEvents;
	}

	
	public void setColorByDefault() {
		colorByObjectId = false;
		colorByMemoryUsage=false;
		colorByUserSupplied=false;
		displayMustBeRepainted();
	}

	/** Color the events by memory usage if possible */
	public void setColorByMemoryUsage() {
		if(memoryUsageValid()){
			colorByMemoryUsage=true;
			colorByObjectId = false;
			colorByUserSupplied=false;
			displayMustBeRepainted();
		} else {
			modificationHandler.displayWarning("No memory usage entries found. Use traceMemoryUsage() and gnu malloc in the application");
		}
		
	}

	public void setColorByUserSupplied() {
		colorByUserSupplied=true;
		colorByObjectId = false;
		colorByMemoryUsage=false;
		displayMustBeRepainted();
	}

	public void setColorByIndex() {
		colorByObjectId = true;
		colorByMemoryUsage=false;
		colorByUserSupplied=false;
		displayMustBeRepainted();
	}
	
	public boolean colorByUserSupplied() {
		return colorByUserSupplied;
	}
	
	public boolean colorByMemoryUsage() {
		return colorByMemoryUsage;
	}
	
	/** Fixup the messages that were sent back in time, breaking the causality assumptions many hold to be true */
	public void fixTachyons() {
		System.out.println("The fix tachyons feature is still experimental. It will probably not work well if new processors are loaded, or ranges are changed");
		
		int numIterations = 100;
		long threshold_us = 10;
		
		System.out.println("Executing at most " + numIterations + " times or until no tachyons longer than " + threshold_us + "us are found");
		
		for(int iteration = 0; iteration < numIterations; iteration++){

			long minLatency = Integer.MAX_VALUE;
			int minSender = -1;
			int minDest = -1;
			
			// Iterate through all entry methods, and compare their execution times to the message send times
			Iterator pe_iter = allEntryMethodObjects.keySet().iterator();
			while(pe_iter.hasNext()){
				Integer pe =  (Integer) pe_iter.next();
				LinkedList objs = (LinkedList)allEntryMethodObjects.get(pe);
				Iterator obj_iter = objs.iterator();
				while(obj_iter.hasNext()){ 
					EntryMethodObject obj = (EntryMethodObject) obj_iter.next();
					
					TimelineMessage m = obj.creationMessage();
					if(m!=null){
						long sendTime = m.Time;
						long executeTime = obj.getBeginTime();

						long latency = executeTime - sendTime;

						int senderPE = m.srcPE;
						int executingPE = obj.pCurrent;

						if(minLatency> latency ){
							minLatency = latency;
							minSender = senderPE;
							minDest = executingPE;	
						}
					}
				}
			}

//			System.out.println("Processor skew is greatest between " + sender + " and " + dest);

			System.out.println("Processor " + minDest + " is lagging behind by " + (-1*minLatency) + "us");

			// Adjust times for all objects and messages associated with processor dest
			Integer laggingPE = new Integer(minDest);
			
			long shift = -1*minLatency;

			
			if(shift < threshold_us){
				System.out.println("No tachyons go back further than "+threshold_us+" us");
				break;
			}
				

			// Shift all events on the lagging pe	
			Iterator iter = ((LinkedList)allEntryMethodObjects.get(laggingPE)).iterator();
			while(iter.hasNext()){
				EntryMethodObject e = (EntryMethodObject) iter.next();
				e.shiftTimesBy(shift);

				// Shift all messages sent by the entry method
				Iterator msg_iter = e.messages.iterator();
				while(msg_iter.hasNext()){
					TimelineMessage msg = (TimelineMessage) msg_iter.next();
					msg.shiftTimesBy(shift);
				}

			}

			// Shift all user event objects on lagging pe
	
			iter = ((LinkedList)allUserEventObjects.get(laggingPE)).iterator();
			while(iter.hasNext()){
				UserEventObject obj = (UserEventObject) iter.next();
				obj.shiftTimesBy(shift);
			}

		}

		displayMustBeRedrawn();
	}
	public void setCompactView(boolean b) {
		useCompactView=b;
		displayMustBeRedrawn();
	}

	/** Should the message pack time regions be displayed */
	public boolean showPacks() {
		if(useCompactView())
			return false;
		else
			return showPacks;
	}

	/** Should the message send points be displayed */
	public boolean showMsgs() {
		if(useCompactView())
			return false;
		else
			return showMsgs;
	}
	
	/** Should the idle regions be displayed */
	public boolean showIdle() {
		return showIdle;
	}
	
	public void showIdle(boolean b) {
		showIdle = b;
	}
	
	public void showPacks(boolean b) {
		showPacks = b;
	}
	
	public void showMsgs(boolean b) {
		showMsgs = b;
	}
	
	public boolean useCompactView() {
		return useCompactView;
	}
	
	public Component guiRoot() {
		if(guiRoot != null)
			return guiRoot;
		else
			return MainWindow.runObject[myRun].guiRoot;
	}
	public void guiRoot(TimelineWindow timelineWindow) {
		guiRoot = timelineWindow;
	}
	
	
	/** Determines which vertical position represents PE */
	public int whichTimelineVerticalPosition(int PE) {
		return ((Integer)peToLine.get(new Integer(PE))).intValue();
		
	}
	
	/** Update the ordering of the PEs (vertical position ordering) */
	void updatePEVerticalOrdering(){

		/** <Integer, Integer> */
		HashMap oldPeToLine = peToLine;
		peToLine = new HashMap();
		
		// Create list of just the elements which we had before, and we still want
		processorList.reset();
		int p = processorList.nextElement();
		while (p != -1) {
			Integer pe = new Integer(p);
			if(oldPeToLine != null && oldPeToLine.containsKey(pe)){
				Integer pos = (Integer)oldPeToLine.get(pe);
				peToLine.put(pe, pos);
			}
			p = processorList.nextElement();
		}

		// Compact the list down because we may have lost some PEs
		for(int i=0;i<peToLine.size();i++){
			Integer pos = new Integer(i);
			if(peToLine.containsValue(pos)){
				// good continue
			} else {
				// shift all positions > i down by one
				Iterator iter = peToLine.keySet().iterator();
				while(iter.hasNext()){
					Integer pe = (Integer) iter.next();
					int line =  ((Integer)peToLine.get(pe)).intValue();
					if(line > i){
						peToLine.put(pe,new Integer(line-1));
					}
				}
			}
			
		}
				
		// Add the newly selected PEs
		processorList.reset();
		p = processorList.nextElement();
		int i=peToLine.size();
		while (p != -1) {
			Integer pe = new Integer(p);
			if(!peToLine.containsKey(pe)){
				peToLine.put(pe, new Integer(i));
				i++;
			}
			p = processorList.nextElement();
		}
		
	}
	
		
	/** Determines the PE for a given vertical position 
	 * 
	 * @note this may be slow, don't call frequently
	 * 
	 */
	public int whichPE(int verticalPosition) {
		Iterator iter = peToLine.keySet().iterator();
		while(iter.hasNext()){
			Integer pe = (Integer) iter.next();
			if( ((Integer)peToLine.get(pe)).intValue() == verticalPosition)
				return pe.intValue();
		}
		return -1;
	}


	public void dumpPEOrder(){
		Iterator iter = peToLine.keySet().iterator();
		while(iter.hasNext()){
			Integer pe = (Integer) iter.next();
			int line =  ((Integer)peToLine.get(pe)).intValue();
			System.out.println("pe " + pe +  " is at " + line);
		}
	}
	
	public void movePEToLine(int PE, int newPos){
		int oldPos = whichTimelineVerticalPosition(PE);

		if(newPos >= peToLine.size())
			newPos = peToLine.size()-1;
		
		if(oldPos < newPos){
			// The lines < oldPos don't move 
			// The lines inbetween shift down by one
			Iterator iter = peToLine.keySet().iterator();
			while(iter.hasNext()){
				Integer pe = (Integer) iter.next();
				int line =  ((Integer)peToLine.get(pe)).intValue();
				if(line > oldPos && line <= newPos){
					peToLine.put(pe,new Integer(line-1));
				}
			}
			// The lines > newPos don't move
		} else {
			// The lines < newPos don't move 
			// The lines inbetween shift up by one
			Iterator iter = peToLine.keySet().iterator();
			while(iter.hasNext()){
				Integer pe = (Integer) iter.next();
				int line =  ((Integer)peToLine.get(pe)).intValue();
				if(line < oldPos && line >= newPos){
					peToLine.put(pe,new Integer(line+1));
				}
			}
			// The lines < oldPos don't move
		}
		
		peToLine.put(new Integer(PE), new Integer(newPos));
		
		this.displayMustBeRedrawn();
	}
}
