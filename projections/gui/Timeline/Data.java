package projections.gui.Timeline;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.*;

import java.awt.Component;
import java.awt.Font;
import java.awt.Color;

import projections.analysis.PackTime;
import projections.analysis.ThreadManager;
import projections.analysis.TimelineEvent;
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
	public static final int BlueGradientColors = 0;
	public static final int RandomColors = 1;

	// meaning in future versions of Projections that support multiple
	// Temporary hardcode. This variable will be assigned appropriate
	// runs.
	int myRun = 0;

	private MainHandler modificationHandler = null;
	
	/** A factor describing how zoomed in we are */
	private float scaleFactor = 1.0f;

	private double preferredViewTime = -1.0;

	/** The most recently known scaled screen width 
	 * 
	 * @note It is likely dangerous to use this unless you 
	 * really know what you are doing
	 * 
	 * */
	private int mostRecentScaledScreenWidth;
	
	/** The list of pes displayed, in display order*/
	LinkedList<Integer> peToLine;

	/** If true, color entry method invocations by Object ID */
	private boolean colorByObjectId;
	
	/** If true, color the entry method invocations by the memory used at that point in time */
	private boolean colorByMemoryUsage;

	/** If true, color the entry method invocations by a user supplied parameter(like timestep) */
	private boolean colorByUserSupplied;

	/** If true, color the entry method invocations by their entry method id */
	private boolean colorByEntryId;
	
	private int[]          entries;

	private Color[]        entryColor;

	/** A set of entry point ids that should be hidden */
	Set<Integer> hiddenEntryPoints;

	/** A set of user events that should be hidden */
	Set<Integer> hiddenUserEvents;

	private boolean hideUserSuppliedNotes = false;
	
	/** Each value of the TreeMap is a TreeSet (sorted list) of EntryMethodObject's .
	 *  Each key of the TreeMap is an Integer pe 
	 *  <Integer,LinkedList<EntryMethodObject> >
	 */
	public Map<Integer,List<EntryMethodObject> > allEntryMethodObjects = new TreeMap<Integer,List<EntryMethodObject> >();

	/** Each value in this TreeMap is a TreeSet of UserEventObject's .
	 *  Each key of the TreeMap is an Integer pe
	 */
	public Map<Integer, Set <UserEventObject> > allUserEventObjects = new TreeMap<Integer, Set <UserEventObject> >();


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

	/** If these are true, we will not load the idle time regions in the timeline */
	private boolean skipIdleRegions;

	/** If these are true, we will not load the message send lines in the timeline */
	private boolean skipLoadingMessages;
	

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
	public Set<EntryMethodObject> drawMessagesForTheseObjects;
	/** A set of objects for which we draw their creation message lines in an alternate color */
	public Set<EntryMethodObject> drawMessagesForTheseObjectsAlt;
	
		
	private boolean useCustomColors=false;
	
	/** A custom foreground color that can override the application wide background pattern. Used by NoiseMiner to set a white background */
	private Color customForeground;
	private Color customBackground;

	private int numUserEventRows = 1;
	boolean drawNestedUserEventRows = false;
	public long minUserSupplied = 0;
	public long maxUserSupplied = 0;
	
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
		
		peToLine = new LinkedList<Integer>();
		
		messageStructures = new MessageStructures(this);
		
		hiddenEntryPoints = new TreeSet<Integer>();
		hiddenUserEvents = new TreeSet<Integer>();

		
		oldBT = -1;
		oldET = -1;

		processorUsage = null;
		entryUsageList = null;

		minMem = Integer.MAX_VALUE;
		maxMem = Integer.MIN_VALUE;	

		minUserSupplied = Integer.MAX_VALUE;
		maxUserSupplied = Integer.MIN_VALUE;	
		
		/** The selected time range for which we display the timeline.
		 *  The value is by default the entire range found in the log files
		 *  It is modified by Window when the user enters a new 
		 *  value in the "select ranges" dialog box.
		 */
		beginTime = 0;
		endTime = MainWindow.runObject[myRun].getTotalTime();

		drawMessagesForTheseObjects = new HashSet<EntryMethodObject>();
		drawMessagesForTheseObjectsAlt = new HashSet<EntryMethodObject>();
				
		allEntryMethodObjects = null;
		entries = new int[MainWindow.runObject[myRun].getNumUserEntries()];
		entryColor = MainWindow.runObject[myRun].getColorMap();

		labelFont = new Font("SansSerif", Font.PLAIN, 12); 
		axisFont = new Font("SansSerif", Font.PLAIN, 10);

		highlightedObjects = new HashSet<Object>();
		
		colorByMemoryUsage = false;
		colorByObjectId = false;
		colorByUserSupplied = false;
			
		/// Default value for custom color (Normally not used)
		customForeground = Color.white; 
		customBackground = Color.black;
		
		skipIdleRegions = false;
		skipLoadingMessages = false;

		
		// Get the list of PEs to display
		loadGlobalPEList();
		
	}
	/** 
	 * Add the data for a new processor to this visualization
	 */
	public void addProcessor(int pCreation){
		Integer p = new Integer(pCreation);
		if(!peToLine.contains(p)){
			peToLine.addLast(p);
			modificationHandler.notifyProcessorListHasChanged();
		}
	}
	

	/** Use the new set of PEs. The PEs will be stored internally in a Linked List */
	public void setProcessorList(OrderedIntList processorList){
		peToLine.clear();
		processorList.reset();
		int p = processorList.nextElement();
		Integer line = 0;
		while (p != -1) {
			Integer pe = new Integer(p);
			peToLine.addLast(pe);
			line ++;
			p = processorList.nextElement();
		}
	}


	/** Load the set of PEs found in MainWindow.runObject[myRun].getValidProcessorList() */
	private void loadGlobalPEList(){
		OrderedIntList processorList = MainWindow.runObject[myRun].getValidProcessorList();
		setProcessorList(processorList);
	}
	

	/** Get the set of PEs as an OrderedIntList. The internal storage for the PE list is not a sorted list. */
	public OrderedIntList processorListOrdered(){
		OrderedIntList processorList = new OrderedIntList();

		Iterator<Integer> iter = peToLine.iterator();
		while(iter.hasNext()){
			Integer pe = iter.next();
			processorList.insert(pe);
			System.out.println("processorList " + pe);
		}

		return processorList;
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


	
	/** Load the initial array of timeline objects 
	 *  
	 *  @note if a new processor has been added, then 
	 *  	  this will not be called. the new proc's
	 *        data will be retrieved using getData()
	 *        which calls createTL() 
	 *        
	 * If the message send lines are needed immedeately, no helper threads should be used(race condition)
	 *        
	 */
	public void createTLOArray(boolean useHelperThreads, Component rootWindow)
	{
		
		// Kill off the secondary processing threads if needed
		messageStructures.kill();
		
		// Can we reuse our already loaded data?
		if(beginTime >= oldBT && endTime <= oldET){
		
			Map<Integer, List<EntryMethodObject> > oldEntryMethodObjects = allEntryMethodObjects;

			Map<Integer, Set <UserEventObject> > oldUserEventObjects = allUserEventObjects;
			
			allEntryMethodObjects = new TreeMap<Integer, List<EntryMethodObject>>();
			allUserEventObjects = new TreeMap<Integer, Set<UserEventObject>>();

			// Remove any unused objects from our data structures 
			// (the components in the JPanel will be regenerated later from this updated list)
		
			Iterator<Integer> peIter = peToLine.iterator();
			while(peIter.hasNext()){
				Integer pe = peIter.next();
					
				if(oldEntryMethodObjects.containsKey(pe)){
					// Reuse the already loaded data
					allEntryMethodObjects.put(pe, oldEntryMethodObjects.get(pe));
					allUserEventObjects.put(pe, oldUserEventObjects.get(pe));

					// Drop elements from mesgVector and allEntryMethodObjects outside range
					if(allEntryMethodObjects.containsKey(pe)){
						List<EntryMethodObject> objs = allEntryMethodObjects.get(pe);
						Iterator<EntryMethodObject> iter = objs.iterator();
						while(iter.hasNext()){
							EntryMethodObject obj = iter.next();
							if(obj.getEndTime() < beginTime || obj.getBeginTime() > endTime){
								iter.remove();
							}
						}
					}

					// Drop elements from userEventsArray outside range
					if(allUserEventObjects.containsKey(pe)){

						Iterator<UserEventObject> iter2 = allUserEventObjects.get(pe).iterator();
						while(iter2.hasNext()){
							UserEventObject obj = iter2.next();
							if(obj.EndTime < beginTime || obj.BeginTime > endTime){
								iter2.remove();
							}
						}
					}
					
				}

			}
			
		} else {
			// We need to reload everything
			allEntryMethodObjects = new TreeMap<Integer, List<EntryMethodObject>>();
			allUserEventObjects = new TreeMap<Integer, Set<UserEventObject>>();
		}
		
		oldBT = beginTime;
		oldET = endTime;
		
		
		//==========================================	
		// Do multithreaded file reading

		Date startReadingTime  = new Date();
	
			
		// Create a list of worker threads
		LinkedList<ThreadedFileReader> readyReaders = new LinkedList<ThreadedFileReader>();
		
		Iterator<Integer> peIter = peToLine.iterator();
		int pIdx=0;
		while(peIter.hasNext()){
			Integer pe = peIter.next();
			if(!allEntryMethodObjects.containsKey(pe)) {
				readyReaders.add(new ThreadedFileReader(pe,pIdx,this));
			}
			pIdx++;
		}
	
		// Pass this list of threads to a class that manages/runs the threads nicely
		if(rootWindow==null)
			rootWindow = MainWindow.runObject[myRun].guiRoot;
		
		ThreadManager threadManager = new ThreadManager("Loading Files in Parallel", readyReaders, rootWindow);
		threadManager.runThreads();

		
		//==========================================	
		//  Perform some post processing
		
//		if(threadManager.numInitialThreads > 0){
//			Date endReadingTime  = new Date();
//			System.out.println("Time to read " + threadManager.numInitialThreads +  " input files(using " + threadManager.numConcurrentThreads + " concurrent threads): " + ((double)(endReadingTime.getTime() - startReadingTime.getTime())/1000.0) + "sec");
//		}
			
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

		Iterator<Integer> pe_iter = allEntryMethodObjects.keySet().iterator();
		while(pe_iter.hasNext()){
			Integer pe = pe_iter.next();
			List<EntryMethodObject> objs = allEntryMethodObjects.get(pe);
			Iterator<EntryMethodObject> obj_iter = objs.iterator();
			while(obj_iter.hasNext()){
				EntryMethodObject obj = obj_iter.next();

				float usage = obj.getUsage();
				int entryIndex = obj.getEntryIndex();

				if (entryIndex >=0) {
					entries[entryIndex]++;
					processorUsage[pe.intValue()] += usage;
					packUsage[pe.intValue()] += obj.getPackUsage();
					entryUsageArray[entryIndex] += obj.getNonPackUsage();
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

		// Spawn a thread that computes some secondary message related data structures
		messageStructures.create(useHelperThreads);
	
		printNumLoadedObjects();
	}

	
	
	public void printNumLoadedObjects(){
		int objCount = 0;
		
		Iterator<Integer> iter = allEntryMethodObjects.keySet().iterator();
		while(iter.hasNext()){
			Integer pe = iter.next();
			List<EntryMethodObject> list = allEntryMethodObjects.get(pe);
			objCount += list.size();
		}
		System.out.println("Displaying " + objCount + " objects in the timline visualization\n");
	}
	
	
	/** Relayout and repaint everything */
	public void displayMustBeRedrawn(){
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
	
	/** Add a set of new lines to the visualization representing the sending of a message.
	 * @note the caller should call 	displayMustBeRepainted() after adding all desired messages
	 */
	public void addMessageSendLineAlt(Set<EntryMethodObject> s) {
		drawMessagesForTheseObjectsAlt.addAll(s);
	}

	/** Add a set of objects for which we want their creation messages to be displayed
	 * @note the caller should call 	displayMustBeRepainted() after adding all desired messages
	 */
	public void addMessageSendLine(Set<EntryMethodObject> s) {
		drawMessagesForTheseObjects.addAll(s);
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

	public Color getEntryColor(Integer id){
		return MainWindow.runObject[myRun].getEntryColor(id);
	}
	
	public Color getUserEventColor(Integer id){
		return MainWindow.runObject[myRun].getUserEventColor(id);
	}
	
	public Color getBackgroundColor(){
		if(useCustomColors)
			return customBackground;
		else
			return MainWindow.runObject[myRun].background;
	}


	/** Load the timeline for processor pe.
	 * 
	 *  This method loads the timeline into: timelineUserEventObjectsArray, allEntryMethodObjects
	 *  
	 *  This function must be thread safe
	 *  
	 * */
	void getData(Integer pe)
	{
		LinkedList<TimelineEvent> tl = new LinkedList<TimelineEvent>();
		
		/** Stores all user events from the currently loaded PE/time range. It must be sorted,
		 *  so the nesting of bracketed user events can be efficiently processed.
		 *  */
		Set<UserEventObject> userEvents= new TreeSet<UserEventObject>();
		
		LinkedList<EntryMethodObject> perPEObjects = new LinkedList<EntryMethodObject>();
		
		
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
		
		// Save perPEObjects and userEvents
		getDataSyncSaveObjectLists(pe, perPEObjects, userEvents);
		
		long minMemThisPE = Long.MAX_VALUE;
		long maxMemThisPE = 0;
		
		long minUserSuppliedThisPE = Long.MAX_VALUE;
		long maxUserSuppliedThisPE = 0;
		
		
		// proc timeline events
		Iterator<TimelineEvent> iter = tl.iterator();
		while(iter.hasNext()){
		
			TimelineEvent tle = iter.next();
			
			// Construct a list of messages sent by the object
			Vector<TimelineMessage> msglist = tle.MsgsSent;
			TreeSet<TimelineMessage> msgs = new TreeSet<TimelineMessage>();
			if(msglist!=null && (!skipLoadingMessages()) ){
				msgs.addAll( msglist );
			}
			
			// Construct a list of message pack times for the object
			Vector<PackTime> packlist = tle.PackTimes;
			int numpacks;
			if (packlist == null || skipLoadingMessages() ) {
				numpacks = 0;
			} else {
				numpacks = packlist.size();
			}
			PackTime[] packs = new PackTime[numpacks];
			for (int p=0; p<numpacks; p++) {
				packs[p] = packlist.elementAt(p);
			}
		
		
			EntryMethodObject obj = new EntryMethodObject(this, tle, msgs, packs, pe.intValue());
			if((obj.isIdleEvent() || obj.isUnaccountedTime() ) && skipLoadingIdleRegions()){
				// don't load this idle event because we are skipping them
			} else {
				perPEObjects.add(obj);
			}
			
			
			if(tle.memoryUsage!=null){
				if(tle.memoryUsage.longValue() > maxMemThisPE)
					maxMemThisPE = tle.memoryUsage.longValue();
				if(tle.memoryUsage.longValue() < minMemThisPE)
					minMemThisPE = tle.memoryUsage.longValue();
			}
			
			if(tle.UserSpecifiedData!=null){
				if(tle.UserSpecifiedData.intValue() > maxUserSuppliedThisPE)
					maxUserSuppliedThisPE = tle.UserSpecifiedData.intValue();
				if(tle.UserSpecifiedData.intValue() < minUserSuppliedThisPE)
					minUserSuppliedThisPE = tle.UserSpecifiedData.intValue();
			}
			
		
		}
		
		// save the time range
		getDataSyncSaveMemUsage(minMemThisPE, maxMemThisPE, minUserSuppliedThisPE, maxUserSuppliedThisPE);
		
	}
	
	

	/** Thread-safe storing of the userEvents and perPEObjects */
	synchronized void getDataSyncSaveObjectLists(Integer pe, List<EntryMethodObject> perPEObjects, Set<UserEventObject> userEvents )  
	{
		// The user events are simply that which were produced by createtimeline
		allUserEventObjects.put(pe,userEvents);
		// The entry method objects must however be constructed
		allEntryMethodObjects.put(pe,perPEObjects);
	}
	
	/** Thread safe updating of the memory and user supplied ranges */
	synchronized void getDataSyncSaveMemUsage(long minMemThisPE, long maxMemThisPE, long minUserSuppliedThisPE, long maxUserSuppliedThisPE)  
	{
		if(minMemThisPE < minMem)
			minMem = minMemThisPE;
		if(maxMemThisPE > maxMem)
			maxMem = maxMemThisPE;
		
		
		if(minUserSuppliedThisPE < minUserSupplied)
			minUserSupplied = minUserSuppliedThisPE;
		if(maxUserSuppliedThisPE > maxUserSupplied)
			maxUserSupplied = maxUserSuppliedThisPE;

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
		Iterator<Set<UserEventObject>> iter = allUserEventObjects.values().iterator();
		int num = 0;
		while(iter.hasNext()){
			num += iter.next().size();
		}
		return num;
	}

	public float getScaleFactor(){
		return scaleFactor;
	}
	
	/* Two clicks zooms by a factor of two: */
	private float scaleChangeFactor = (float)Math.pow(2.0,1.0/2);
	private float roundScale(float scl) {
		int i=(int)(scl+0.5f);
		if (Math.abs(i-scl)<0.01) return i; /* round to int */
		else return scl; /*  leave as float */
	}
	
	public void increaseScaleFactor(){
		setScaleFactor( roundScale( getScaleFactor() *  scaleChangeFactor ));
	}
	
	public void decreaseScaleFactor(){
		setScaleFactor( roundScale( getScaleFactor() / scaleChangeFactor ));
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
		return peToLine.size();
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


	public int topOffset(){
		if(useMinimalView() || useCompactView())
			return 1;
		else
			return 4;
	}
	
	public int bottomOffset(){
		if(useMinimalView() || useCompactView())
			return 1;
		else
			return 4;
	}

	
	/** The width we should draw in, compensated for the scaling(zoom) factor 
	 * 
	 * 
	 * @note this should only be called by a layout manager that knows the size
	 * of the screen.
	 * 
	 * */
	public int scaledScreenWidth(int actualDisplayWidth){
		mostRecentScaledScreenWidth = (int)(actualDisplayWidth * scaleFactor);
		return mostRecentScaledScreenWidth;
	}


	/** The height of the panel that should be used to draw the timelines  */
	public int screenHeight(){
			int paddingForScrollbar = 55;
			return singleTimelineHeight()*numPs() + paddingForScrollbar;
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
		return topOffset() + userEventRectHeight() + barheight() + messageSendHeight() + bottomOffset();
	}

	
	
	/** get the height of each user event rectangle */
	public int userEventRectHeight(){
		if(useCompactView())
			return 0;
		else if (this.drawNestedUserEventRows)
			return 12*getNumUserEventRows();
		else
			return 8*getNumUserEventRows();	
	}
	


	public void setHandler(MainHandler rh)
	{ 
		modificationHandler = rh;
		displayMustBeRedrawn();
	}
	
//	public void setProcessorList(OrderedIntList procs){
//		
//	}

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
	public int timeToScreenPixelRight(double time) {
		double fractionAlongTimeAxis =  ((time+0.5-beginTime)) /((endTime-beginTime));
		return offset() + (int)Math.floor(fractionAlongTimeAxis*(mostRecentScaledScreenWidth-2*offset()));
	}
	
	/** Convert time to screen coordinate, The returned pixel is the leftmost pixel for this time if a microsecond is longer than one pixel
	 * 
	 * @note requires that mostRecentScaledScreenWidth be correct prior to invocation,
	 * so you should call  scaledScreenWidth(int actualDisplayWidth) before this
	 */
	public int timeToScreenPixelLeft(double time) {
		double fractionAlongTimeAxis =  ((time-0.5-beginTime)) /((endTime-beginTime));
		return offset() + (int)Math.ceil(fractionAlongTimeAxis*(mostRecentScaledScreenWidth-2*offset()));
	}


	/** Convert time to screen coordinate, The returned pixel is the central pixel for this time if a microsecond is longer than one pixel */
	public int timeToScreenPixel(double time) {
		double fractionAlongTimeAxis =  ((time-beginTime)) /((endTime-beginTime));
		return offset() + (int)(fractionAlongTimeAxis*(mostRecentScaledScreenWidth-2*offset()));
	}
	
	/** Convert time to screen coordinate, The returned pixel is the central pixel for this time if a microsecond is longer than one pixel */
	public int timeToScreenPixel(double time, int assumedScreenWidth) {
		double fractionAlongTimeAxis =  ((time-beginTime)) /((endTime-beginTime));
		return offset() + (int)(fractionAlongTimeAxis*(assumedScreenWidth-2*offset()));
	}
	
	/** Convert time to screen coordinate, The returned pixel is the leftmost pixel for this time if a microsecond is longer than one pixel */
	public int timeToScreenPixelLeft(double time, int assumedScreenWidth) {
		double fractionAlongTimeAxis =  ((time-0.5-beginTime)) /((endTime-beginTime));
		return offset() + (int)Math.ceil(fractionAlongTimeAxis*(assumedScreenWidth-2*offset()));
	}
	
	/** Convert time to screen coordinate, The returned pixel is the rightmost pixel for this time if a microsecond is longer than one pixel */
	public int timeToScreenPixelRight(double time, int assumedScreenWidth) {
		double fractionAlongTimeAxis =  ( (time+0.5-beginTime)) /((endTime-beginTime));
		return offset() + (int)Math.floor(fractionAlongTimeAxis*(assumedScreenWidth-2*offset()));
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
	public void clickTraceSender(EntryMethodObject obj) {
		if(! useMinimalView()){
			addProcessor(obj.pCreation);
			toggleMessageSendLine(obj);
		}		
	}

	
	/** A set of objects to highlight. The paintComponent() methods for the objects 
	 * should paint themselves appropriately after determining if they are in this set 
	 */
	private Set<Object> highlightedObjects;
 	
	/** Highlight the message links to the object upon mouseover */
	private boolean traceMessagesBackOnHover;
	
	/** Highlight the message links forward from the object upon mouseover */
	private boolean traceMessagesForwardOnHover;
	
	/** Highlight the other entry method invocations upon mouseover */
	private boolean traceOIDOnHover;

	/** Should we use a very compact view, with no message sends, or user events */
	private boolean useCompactView;

	private Component guiRoot;
	int colorSchemeForUserSupplied;
	
	/** Clear any highlights created by HighlightObjects() */
	public void clearObjectHighlights() {
		highlightedObjects.clear();
	}
	
	/** Highlight the given set of timeline objects */
	public void HighlightObjects(Set<Object> objects) {
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
	
	public boolean traceMessagesBackOnHover() {
		return traceMessagesBackOnHover;
	}
	
	public boolean traceMessagesForwardOnHover() {
		return traceMessagesForwardOnHover;
	}
	
	public boolean traceOIDOnHover() {
		return traceOIDOnHover;
	}
	
	public void setTraceMessagesBackOnHover(boolean traceMessagesOnHover) {
		this.traceMessagesBackOnHover = traceMessagesOnHover;
		
		if(traceMessagesOnHover)
			SetToolTipDelayLarge();
		else
			SetToolTipDelaySmall();
		
	}
	
	public void setTraceMessagesForwardOnHover(boolean traceMessagesForwardOnHover) {
		this.traceMessagesForwardOnHover = traceMessagesForwardOnHover;
		
		if(traceMessagesForwardOnHover)
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
		colorByEntryId = false;
		displayMustBeRepainted();
	}

	/** Color the events by memory usage if possible */
	public void setColorByMemoryUsage() {
		if(memoryUsageValid()){
			colorByMemoryUsage=true;
			colorByObjectId = false;
			colorByUserSupplied=false;
			colorByEntryId = false;
			displayMustBeRepainted();
		} else {
			modificationHandler.displayWarning("No memory usage entries found. Use traceMemoryUsage() and gnu malloc in the application");
		}
		
	}

	public void setColorByUserSupplied(int colorScheme) {
		colorSchemeForUserSupplied=colorScheme;
		colorByUserSupplied=true;
		colorByObjectId = false;
		colorByMemoryUsage=false;
		colorByEntryId = false;
		displayMustBeRepainted();
	}

	public void setColorByObjectID() {
		colorByObjectId = true;
		colorByMemoryUsage=false;
		colorByUserSupplied=false;
		colorByEntryId = false;
		displayMustBeRepainted();
	}
	
	
	public void setColorByUserSuppliedAndObjID(int colorScheme) {
		colorSchemeForUserSupplied=colorScheme;
		colorByUserSupplied=true;
		colorByObjectId = true;
		colorByMemoryUsage=false;
		colorByEntryId = false;
		displayMustBeRepainted();
	}
	
	public void setColorByUserSuppliedAndEID(int colorScheme) {
		colorSchemeForUserSupplied=colorScheme;
		colorByUserSupplied=true;
		colorByObjectId = false;
		colorByMemoryUsage=false;
		colorByEntryId = true;
		displayMustBeRepainted();
	}
	
	
	public boolean colorByEID() {
		return colorByEntryId;
	}
	

	public boolean colorByOID() {
		return colorByObjectId;
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
			Iterator<Integer> pe_iter = allEntryMethodObjects.keySet().iterator();
			while(pe_iter.hasNext()){
				Integer pe = pe_iter.next();
				List<EntryMethodObject> objs = allEntryMethodObjects.get(pe);
				Iterator<EntryMethodObject> obj_iter = objs.iterator();
				while(obj_iter.hasNext()){ 
					EntryMethodObject obj = obj_iter.next();
					
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
			Iterator<EntryMethodObject> iter = (allEntryMethodObjects.get(laggingPE)).iterator();
			while(iter.hasNext()){
				EntryMethodObject e = iter.next();
				e.shiftTimesBy(shift);

				// Shift all messages sent by the entry method
				Iterator<TimelineMessage> msg_iter = e.messages.iterator();
				while(msg_iter.hasNext()){
					TimelineMessage msg = msg_iter.next();
					msg.shiftTimesBy(shift);
				}

			}

			// Shift all user event objects on lagging pe
	
			Iterator<UserEventObject> iter2 = allUserEventObjects.get(laggingPE).iterator();
			while(iter2.hasNext()){
				UserEventObject u = iter2.next();
				u.shiftTimesBy(shift);
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
	
	
	public void guiRoot(TimelineWindow timelineWindow) {
		guiRoot = timelineWindow;
	}
	
	
	/** Determines which vertical position represents PE */
	public int whichTimelineVerticalPosition(int PE) {
		if(peToLine==null){
			throw new RuntimeException("peToLine is null");
		}
		if(!peToLine.contains(new Integer(PE))){
			throw new RuntimeException("peToLine does not contain pe " + PE);
		}
		return peToLine.indexOf(new Integer(PE));
	}
	
	/** Update the ordering of the PEs (vertical position ordering) */
//	void updatePEVerticalOrdering(){
//=
//		// Add the newly selected PEs
//		processorList.reset();
//		int p = processorList.nextElement();
//		while (p != -1) {
//			Integer pe = new Integer(p);
//			
//
//			
//			p = processorList.nextElement();
//		}
//		
//	}
		
	/** Determines the PE for a given vertical position 
	 * 
	 * @note this may be slow, don't call frequently
	 * 
	 */
	public int whichPE(int verticalPosition) {
		Integer which = peToLine.get(verticalPosition);
		return which;
	}


	public void movePEToLine(int PE, int newPos){
		Integer p = new Integer(PE);
		peToLine.remove(p);
		peToLine.add(newPos, p);
		this.displayMustBeRedrawn();
	}

	

	
	public void setBackgroundColor(Color c) {
		customBackground = c;
		useCustomColors = true;
		displayMustBeRedrawn();
	}
	
	
	public void setForegroundColor(Color c) {
		customForeground = c;
		useCustomColors = true;
		displayMustBeRedrawn();
	}
	
	
	public void setColors(Color backgroundColor, Color foregroundColor){
		setBackgroundColor(backgroundColor);
		setForegroundColor(foregroundColor);
	}
	
	
	
	public void printUserEventInfo(){

		System.out.println("printUserEventInfo()");
		
		HashMap<Integer, Long> min = new HashMap<Integer, Long>();
		HashMap<Integer, Long> max = new HashMap<Integer, Long>();
		HashMap<Integer, Long> total = new HashMap<Integer, Long>();
		HashMap<Integer, Long> count = new HashMap<Integer, Long>();
		HashMap<Integer, String> name = new HashMap<Integer, String>();

		
		Iterator<Integer> iter = allUserEventObjects.keySet().iterator();
		while(iter.hasNext()){
			Integer pe = iter.next();
			
			Iterator<UserEventObject> eventiter = allUserEventObjects.get(pe).iterator();
			while(eventiter.hasNext()){
				UserEventObject obj = eventiter.next();
				if(obj.Type == UserEventObject.PAIR){
					long BeginTime = obj.BeginTime;
					long EndTime = obj.EndTime;
					Integer UserEventID = new Integer(obj.UserEventID); 

					long duration = EndTime-BeginTime;

					if(! min.containsKey(UserEventID)){
						min.put(UserEventID, new Long(duration));
						max.put(UserEventID, new Long(duration));
						total.put(UserEventID, new Long(duration));
						count.put(UserEventID, new Long(1));
						name.put(UserEventID, obj.getName());
					} else {

						if(min.get(UserEventID) > duration){
							min.put(UserEventID, new Long(duration));
						}

						if(max.get(UserEventID) < duration){
							max.put(UserEventID, new Long(duration));
						}

						total.put(UserEventID, total.get(UserEventID) + new Long(duration));
						count.put(UserEventID, count.get(UserEventID) + new Long(1));

					}

				}
				
			}
			
		}
		
		iter = min.keySet().iterator();
		while(iter.hasNext()){
			Integer UserEventID = iter.next();

			double avg = total.get(UserEventID).doubleValue() /	count.get(UserEventID).doubleValue();
			
			System.out.print("User Event #" + UserEventID + "  \"" + name.get(UserEventID) + "\"");
			System.out.print("    count = " + count.get(UserEventID));
			System.out.print("    min   = " + min.get(UserEventID) + " us");
			System.out.print("    max   = " + max.get(UserEventID) + " us");
			System.out.print("    avg   = " + avg + " us");
			System.out.print("    total = " + total.get(UserEventID) + " us");
			System.out.println();
		}
	}
	
	
	
	
	/** Determine how many rows are needed for displaying nested bracketed user events. 
	 *  Choose the new size for the User Event rows, and cause a redraw
	 * 
	 *  Determine the depth for each User Event, and store it in the event itself.
	 */
	public int determineUserEventNestings(){
		
		// create a stack of endtimes
		

		int maxDepth = 0;
		
		Iterator<Integer> iter = allUserEventObjects.keySet().iterator();
		while(iter.hasNext()){
			Integer pe = iter.next();
			
			Stack <Long> activeEndTimes = new Stack<Long>();
			
			// The iterator must go in order of start times(It will as long as allUserEventObjects.get(pe) is a TreeSet
			Iterator eventiter = allUserEventObjects.get(pe).iterator();
			while(eventiter.hasNext()){
				UserEventObject obj = (UserEventObject) eventiter.next();
				if(obj.Type == UserEventObject.PAIR){
					long BeginTime = obj.BeginTime;
					long EndTime = obj.EndTime;

					// pop all user events from the stack if their endtime is earlier than this one's start time
					while(activeEndTimes.size()>0 && activeEndTimes.peek() <= BeginTime){
						activeEndTimes.pop();
					}
					
					// push this event onto the stack
					activeEndTimes.push(EndTime);

					
					// Notify this event of its depth in the stack
					obj.setNestedRow(activeEndTimes.size()-1);

					
					if(activeEndTimes.size() > maxDepth)
						maxDepth = activeEndTimes.size();

				}

			}

		}
		return maxDepth;
	}

	/** Enable or disable the displaying of multiple rows of nested user events */
	public void showNestedUserEvents(boolean b) {
		
		drawNestedUserEventRows = b;
		if(b == true){
			setNumUserEventRows(determineUserEventNestings());
		} else {
			setNumUserEventRows(1);
		}
		
		displayMustBeRedrawn();
		
	}

	public void setNumUserEventRows(int numUserEventRows) {
		this.numUserEventRows = numUserEventRows;
	}

	public int getNumUserEventRows() {
		return numUserEventRows;
	}

	/** The pixel offset for the top of the entry method from the top of a single PE's timeline */
	public int entryMethodLocationTop(int pe) {
		int yidx = whichTimelineVerticalPosition(pe);
		return singleTimelineHeight()*yidx + topOffset() + userEventRectHeight();
	}
	
	/** The pixel height of the entry method object. This includes just the rectangular region and the descending message sends */
	public int entryMethodLocationHeight() {
		return barheight()+messageSendHeight();
	}

	public int userEventLocationTop(int pe) {
		int yidx = whichTimelineVerticalPosition(pe);
		return singleTimelineHeight()*yidx + topOffset();
	}

	public int userEventLocationBottom(int pe) {
		return userEventLocationTop(pe) + userEventRectHeight();
	}
	
	public int horizontalLineLocationTop(int i) {
		return singleTimelineHeight()*i + topOffset() + userEventRectHeight() + (barheight()/2);		
	}
	
	/** The message send tick mark bottom point*/
	public int messageSendLocationY(int pe) {
		int yidx = whichTimelineVerticalPosition(pe);
		return singleTimelineHeight()*yidx + topOffset() + userEventRectHeight() + barheight()+this.messageSendHeight();
	}
	
	/** The message send tick mark bottom point*/
	public int messageRecvLocationY(int pe) {
		int yidx = whichTimelineVerticalPosition(pe);
		return singleTimelineHeight()*yidx + topOffset() + userEventRectHeight();
	}

	
	public void dropPEsUnrelatedToPE(Integer pe) {
		dropPEsUnrelatedToObjects(allEntryMethodObjects.get(pe));
	}
	
	public void dropPEsUnrelatedToObject(EntryMethodObject obj) {
		System.out.println("dropPEsUnrelatedToObject()");
		HashSet<EntryMethodObject> set = new HashSet<EntryMethodObject>();
		set.add(obj);
		dropPEsUnrelatedToObjects(set);
	}
	
	

	public void dropPEsUnrelatedToObjects(Collection<EntryMethodObject> objs) {
		System.out.println("dropPEsUnrelatedToObjects()");
		HashSet<EntryMethodObject> allRelatedEntries = new HashSet<EntryMethodObject>();

		// Find all entry method invocations related to this one
		Iterator<EntryMethodObject> objIter = objs.iterator();
		while(objIter.hasNext()){
			EntryMethodObject obj = objIter.next();
			allRelatedEntries.add(obj);
			allRelatedEntries.addAll(obj.traceForwardDependencies());
			allRelatedEntries.addAll(obj.traceBackwardDependencies());
		}
		
		// Find all PEs related to this object
		HashSet<Integer> relatedPEs = new HashSet<Integer>();
			
		Iterator<EntryMethodObject> iter = allRelatedEntries.iterator();
		while(iter.hasNext()){
			EntryMethodObject o = iter.next();
			relatedPEs.add(o.pCurrent); 
		}
		
		dropPEsNotInList(relatedPEs);
	}
	
	
	
	// Drop timelines from any PEs not in the provided list
	void dropPEsNotInList(Set<Integer> keepPEs){
		// Drop any PEs not in the list
		Set<Integer> currentPEs = new HashSet<Integer>();
		currentPEs.addAll(peToLine);

		Iterator<Integer> currPEiter = currentPEs.iterator();
		while(currPEiter.hasNext()){
			Integer p = currPEiter.next();
			if(keepPEs.contains(p)){
				// Keep this PE 
			} else {
				// Drop this PE
				peToLine.remove(p);		
			}
		}
		
		modificationHandler.notifyProcessorListHasChanged();
	}

	
	/** Produce a hashmap containing the ids and nicely mangled string names for each entry method */
	public Hashtable<Integer, String> getEntryNames() {
		
		Map<Integer, String> entryNames = MainWindow.runObject[myRun].getSts().getEntryNames();
		Map<Integer, String> entryChareNames = MainWindow.runObject[myRun].getSts().getEntryChareNames();

		Hashtable<Integer, String> result = new Hashtable<Integer, String>();
		
		Iterator<Integer> iter = entryNames.keySet().iterator();
		while(iter.hasNext()){
			Integer id = iter.next();
			result.put(id,entryNames.get(id) + "::" + entryChareNames.get(id));
		}
		
		return result;	
	}
	
	
	/** Produce a hashmap containing the ids and nicely mangled string names for each user event */
	public Map<Integer, String> getUserEventNames() {
		return MainWindow.runObject[myRun].getSts().getUserEventNameMap();
	}


	
	public void makeEntryVisibleID(Integer id){		
		makeEntryVisibleID(id, true);
	}
	public void makeEntryInvisibleID(Integer id){		
		makeEntryInvisibleID(id, true);
	}
	
	/** Make visible the entry methods for this id */	
	public void makeEntryVisibleID(Integer id, boolean redraw) {
		hiddenEntryPoints.remove(id);
		if(redraw)
			this.displayMustBeRedrawn();
	}	

	/** Hide the entry methods for this id */
	public void makeEntryInvisibleID(Integer id, boolean redraw) {
		hiddenEntryPoints.add(id);
		if(redraw)
			this.displayMustBeRedrawn();
	}
	

	/** Make visible the entry methods for this id */	
	public void makeUserEventVisibleID(Integer id) {
		hiddenUserEvents.remove(id);
		this.displayMustBeRedrawn();
	}	

	/** Hide the entry methods for this id */
	public void makeUserEventInvisibleID(Integer id) {
		hiddenUserEvents.add(id);
		this.displayMustBeRedrawn();
	}

	
	
	
	
	public void makeUserSuppliedNotesVisible() {
		hideUserSuppliedNotes = false;
		this.displayMustBeRedrawn();
	}	

	public void makeUserSuppliedNotesInvisible() {
		hideUserSuppliedNotes = true;
		this.displayMustBeRedrawn();
	}
	
	public boolean userSuppliedNotesVisible() {
		return ! hideUserSuppliedNotes;	
	}

	public boolean userSuppliedNotesHidden() {
		return hideUserSuppliedNotes;	
	}

	
	public boolean entryIsHiddenID(Integer id) {
		return hiddenEntryPoints.contains(id);
	}
	
	public boolean entryIsVisibleID(Integer id) {
		return ! hiddenEntryPoints.contains(id);
	}
	
	public boolean userEventIsHiddenID(Integer id) {
		return hiddenUserEvents.contains(id);
	}
	
	public boolean userEventIsVisibleID(Integer id) {
		return ! hiddenUserEvents.contains(id);
	}
	
	public void skipLoadingIdleRegions(boolean b) {
		skipIdleRegions = b;
		if(skipIdleRegions){
			pruneOutIdleRegions();	
		}
	}

	public void skipLoadingMessages(boolean b) {
		skipLoadingMessages = b;
		if(skipLoadingMessages){
			pruneOutMessages();	
		}
	}

	
	/** Remove from allEntryMethodObjects any idle EntryMethodObjects */
	private void pruneOutIdleRegions() {
		System.out.println("pruneOutIdleRegions");
		Iterator<Integer> iter = allEntryMethodObjects.keySet().iterator();
		while(iter.hasNext()){
			Integer pe = iter.next();
			List<EntryMethodObject> list = allEntryMethodObjects.get(pe);
			
			Iterator<EntryMethodObject> iter2 = list.iterator();
			while(iter2.hasNext()){
				EntryMethodObject o = iter2.next();
				if(o.isIdleEvent()){
					iter2.remove();
				}
			}
		}
		
		modificationHandler.notifyProcessorListHasChanged(); // Really it is the set of objects that has changed

		displayMustBeRedrawn();
		
	}
	
	
	private void pruneOutMessages() {
		System.out.println("pruneOutMessages");
		Iterator<Integer> iter = allEntryMethodObjects.keySet().iterator();
		while(iter.hasNext()){
			Integer pe = iter.next();
			List<EntryMethodObject> list = allEntryMethodObjects.get(pe);
			
			Iterator<EntryMethodObject> iter2 = list.iterator();
			while(iter2.hasNext()){
				EntryMethodObject o = iter2.next();
				o.messages = new TreeSet<TimelineMessage>();
			}
		
		}
		messageStructures.clearAll();
		
		clearMessageSendLines();

		modificationHandler.notifyProcessorListHasChanged(); // Really it is the set of objects that has changed
		
		displayMustBeRedrawn();
	}
	public boolean skipLoadingIdleRegions() {
		return skipIdleRegions;
	}
	public boolean skipLoadingMessages() {
		return skipLoadingMessages;
	}
	
}
