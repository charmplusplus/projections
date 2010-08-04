package projections.Tools.Timeline;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.ToolTipManager;

import projections.analysis.PackTime;
import projections.analysis.TimedProgressThreadExecutor;
import projections.analysis.TimelineEvent;
import projections.gui.Analysis;
import projections.gui.ColorManager;
import projections.gui.MainWindow;
import projections.gui.OrderedIntList;
import projections.gui.OrderedUsageList;
import projections.misc.LogLoadException;


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
 * 		-- Fonts to be used for the axis and labelsTimelineWindow(MainWindow parentWindow) {
		super(parentWindow);
		
        thisWindow = this;
		
		data = new Data(this);
		
		labelPanel = new LabelPanel(data);
		
		// Construct the various layers, and the layout manager
		AxisPanel ap = new AxisPanel(data);
		AxisOverlayPanel op = new AxisOverlayPanel(data);
		AxisLayout lay = new AxisLayout(ap);
		// Create the layered panel containing our layers
		axisPanel = new LayeredPanel(ap,op,lay);
		ap.setOpaque(false);
		op.setOpaque(false);
		
		mainPanel = new MainPanel(data, this);
		
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
	protected static final int BlueGradientColors = 0;
	protected static final int RandomColors = 1;

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
	private LinkedList<Integer> peToLine;

	/** If true, color entry method invocations by Object ID */
	private boolean colorByObjectId;
	
	/** If true, color the entry method invocations by the memory used at that point in time */
	private boolean colorByMemoryUsage;

	/** If true, color the entry method invocations by a user supplied parameter(like timestep) */
	private boolean colorByUserSupplied;

	/** If true, color the entry method invocations by their entry method id */
	private boolean colorByEntryId;
	
	/** If true, color the entry method invocations by their entry method frequency */
	private boolean colorByEntryIdFreq;
	
	private int[]          entries;

	private Color[]        entryColor;
	public TreeMap<Integer, Color> entryColorsMapping = new TreeMap<Integer, Color>();

	/** A set of entry point ids that should be hidden */
	private Set<Integer> hiddenEntryPoints;

	/** A set of user events that should be hidden */
	private Set<Integer> hiddenUserEvents;

	private boolean hideUserSuppliedNotes = false;
	
	/** Each value of the TreeMap is a TreeSet (sorted list) of EntryMethodObject's .
	 *  Each key of the TreeMap is an Integer pe 
	 *  <Integer,LinkedList<EntryMethodObject> >
	 */
	protected Map<Integer,List<EntryMethodObject> > allEntryMethodObjects = new TreeMap<Integer,List<EntryMethodObject> >();

	/** Each value in this TreeMap is a TreeSet of UserEventObject's .
	 *  Each key of the TreeMap is an Integer pe
	 */
	protected Map<Integer, Set <UserEventObject> > allUserEventObjects = new TreeMap<Integer, Set <UserEventObject> >();

	/**
	 * Each value in this TreeMap is a TreeSet of the entry method's frequencies.
	 * Each key is an integer, the entry ID frequency
	 * Each value is a linked list of entry ID's that have the frequency defined by the key
	 */
	protected TreeMap<Integer, LinkedList<Integer>> frequencyTreeMap = new TreeMap<Integer, LinkedList<Integer>>();
	
	
	/**
	 * This is a Vector of the relative frequencies of the entry methods.  The entry
	 * method with the most frequencies will be the first item in the Vector 
	 */
	protected ArrayList<Integer> frequencyVector = new ArrayList<Integer>();
	
	/** processor usage indexed by PE */
	float[] processorUsage;

	/** idle usage indexed by PE */
	float[] idleUsage;

	/** pack usage indexed by PE */
	private float[] packUsage;

	/** entry usage list indexed by PE */
	private OrderedUsageList[] entryUsageList;

	/** The start time for the time range. */
	private long startTime; 

	/** The end time for the time range. */
	private long endTime;

	/** The old begin time, used to make loading of new ranges more
		efficient if we already have the object data loaded. Then we
		can just take a subset of our data instead of reloading it
		from the logs
	*/
	private long oldBT;
	/** The old end time */
	private long oldET; 
	
	/** The miniumum and maximum memory usage that have been seen so far */
	private long minMem, maxMem;

	/** The range of memory usage values that should be used when coloring based on mem usage */
	private long minMemColorRange, maxMemColorRange;
	
	/** Stores various lookup tables for messages and associated entry points */
	MessageStructures messageStructures;
	
	/** Determine whether pack times, idle regions, or message send ticks should be displayed */		
	private boolean showPacks, showIdle, showMsgs, showUserEvents;

	/** If true, we will not load the idle time regions in the timeline */
	private boolean skipIdleRegions;

	/** If true, we will not load the message send lines in the timeline */
	private boolean skipLoadingMessages;
	
	/** If true, we will not load the user events in the timeline */
	private boolean skipLoadingUserEvents;
	
	/** Only load entry methods that have  at least this duration */
	private long minEntryDuration;

	/** The font used by the LabelPanel */
	protected Font labelFont;
	/** The font used by the time labels on the TimelineAxisCanvas */
	protected Font axisFont;

	/** A set of objects for which we draw their creation message lines */
	protected Set<EntryMethodObject> drawMessagesForTheseObjects;
	/** A set of objects for which we draw their creation message lines in an alternate color */
	protected Set<EntryMethodObject> drawMessagesForTheseObjectsAlt;
	
		
	private boolean useCustomColors=false;
	
	/** A custom foreground color that can override the application wide background pattern. Used by NoiseMiner to set a white background */
	private Color customForeground;
	private Color customBackground;

	private int numUserEventRows = 1;
	boolean drawNestedUserEventRows = false;
	protected long minUserSupplied = 0;
	protected long maxUserSupplied = 0;
	
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

		viewType = ViewType.VIEW_NORMAL;
		
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
		startTime = 0;
		endTime = MainWindow.runObject[myRun].getTotalTime();

		drawMessagesForTheseObjects = new HashSet<EntryMethodObject>();
		drawMessagesForTheseObjectsAlt = new HashSet<EntryMethodObject>();
				
		allEntryMethodObjects = null;
		entries = new int[MainWindow.runObject[myRun].getNumUserEntries()];
		makeFrequencyMap(entries);
		makeFreqVector();
		entryColor = MainWindow.runObject[myRun].getEPColorMap();

		labelFont = new Font("SansSerif", Font.PLAIN, 12); 
		axisFont = new Font("SansSerif", Font.PLAIN, 10);

		highlightedObjects = new HashSet<Object>();
		
		colorByMemoryUsage = false;
		colorByObjectId = false;
		colorByUserSupplied = false;
		colorByEntryId = false;
		colorByEntryIdFreq = false;
			
		/// Default value for custom color (Normally not used)
		customForeground = Color.white;
		customBackground = Color.black;
		
		skipIdleRegions = false;
		skipLoadingMessages = false;
		skipLoadingUserEvents = false;
		minEntryDuration = 0;
		
		// Get the list of PEs to display
		loadGlobalPEList();
		
	}
	/** 
	 * Add the data for a new processor to this visualization
	 */
	protected void addProcessor(int pe){
		System.out.println("Add processor " + pe);
		Integer p = Integer.valueOf(pe);
		if(!peToLine.contains(p)){
			peToLine.addLast(p);
			System.out.println("Add processor " + pe + " to peToLine size=" + peToLine.size() );
			modificationHandler.notifyProcessorListHasChanged();
			storeRangeToPersistantStorage();
			displayMustBeRedrawn();
		}
		
	}
	

	/** If the timeline tool chooses a new time range or set of processors, then we should store the new configuration for use in future dialog boxes */
	private void storeRangeToPersistantStorage(){
		MainWindow.runObject[myRun].persistantRangeData.update(startTime(), endTime(), processorListOrdered());
	}


	/** Use the new set of PEs. The PEs will be stored internally in a Linked List */
	public void setProcessorList(OrderedIntList processorList){
		peToLine.clear();
		processorList.reset();
		int p = processorList.nextElement();
		Integer line = 0;
		while (p != -1) {
			Integer pe = Integer.valueOf(p);
			peToLine.addLast(pe);
			line ++;
			if(processorList.hasMoreElements())
				p = processorList.nextElement();
			else
				p = -1;
		}
	}


	/** Load the set of PEs found in MainWindow.runObject[myRun].getValidProcessorList() */
	private void loadGlobalPEList(){
		OrderedIntList processorList = MainWindow.runObject[myRun].getValidProcessorList().copyOf();
		setProcessorList(processorList);
	}
	

	/** Get the set of PEs as an OrderedIntList. The internal storage for the PE list is not a sorted list. */
	private OrderedIntList processorListOrdered(){
		OrderedIntList processorList = new OrderedIntList();

		Iterator<Integer> iter = peToLine.iterator();
		while(iter.hasNext()){
			Integer pe = iter.next();
			processorList.insert(pe);
		}

		return processorList;
	}

	
	
	
	/** Change the font sizes used */
	protected void setFontSizes(int labelFontSize, int axisFontSize, boolean useBoldForLabel){
		if(useBoldForLabel)
			labelFont = new Font("SansSerif", Font.BOLD, labelFontSize); 
		else
			labelFont = new Font("SansSerif", Font.PLAIN, labelFontSize); 

		axisFont = new Font("SansSerif", Font.PLAIN, axisFontSize);
	}


	protected long startTime(){
		return startTime;
	}


	
	/** Load the initial array of timeline objects 
	 * @param showProgress 
	 *  
	 *  @note if a new processor has been added, then 
	 *  	  this will not be called. the new proc's
	 *        data will be retrieved using getData()
	 *        which calls createTL() 
	 *        
	 * If the message send lines are needed immediately, no helper threads should be used(race condition)
	 *        
	 */
	protected void createTLOArray(boolean useHelperThreads, Component rootWindow, boolean showProgress)
	{
		
		// Kill off the secondary processing threads if needed
		messageStructures.kill();
		
		// Can we reuse our already loaded data?
		if(startTime >= oldBT && endTime <= oldET){
		
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
							if(obj.getEndTime() < startTime || obj.getBeginTime() > endTime){
								iter.remove();
							}
						}
					}

					// Drop elements from userEventsArray outside range
					if(allUserEventObjects.containsKey(pe)){

						Iterator<UserEventObject> iter2 = allUserEventObjects.get(pe).iterator();
						while(iter2.hasNext()){
							UserEventObject obj = iter2.next();
							if(obj.EndTime < startTime || obj.BeginTime > endTime){
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
		
		oldBT = startTime;
		oldET = endTime;
		
		
		//==========================================	
		// Do multithreaded file reading

	
			
		// Create a list of worker threads
		LinkedList<Runnable> readyReaders = new LinkedList<Runnable>();
		
		Iterator<Integer> peIter = peToLine.iterator();
		int pIdx=0;
		while(peIter.hasNext()){
			Integer pe = peIter.next();
			if(!allEntryMethodObjects.containsKey(pe)) {
				readyReaders.add(new ThreadedFileReader(pe,this));
			}
			pIdx++;
		}
	
		// Determine a component to show the progress bar with
		Component guiRootForProgressBar = null;
		if(rootWindow!=null && rootWindow.isVisible()) {
			guiRootForProgressBar = rootWindow;
		} else if(MainWindow.runObject[myRun].guiRoot!=null && MainWindow.runObject[myRun].guiRoot.isVisible()){
			guiRootForProgressBar = MainWindow.runObject[myRun].guiRoot;
		}

		// Pass this list of threads to a class that manages/runs the threads nicely
		TimedProgressThreadExecutor threadManager = new TimedProgressThreadExecutor("Loading Timeline in Parallel", readyReaders, guiRootForProgressBar, showProgress);
		threadManager.runAll();

		if(memoryUsageValid())
			System.out.println("memory usage seen in the logs ranges from : " + minMem/1024/1024 + "MB to " + maxMem/1024/1024 + "MB");
		
		//==========================================	
		//  Perform some post processing
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
					entryUsageList[pe.intValue()].insert(entryUsageArray[i]);
				}
			}
			
		} 

		// Spawn a thread that computes some secondary message related data structures
		messageStructures.create(useHelperThreads);
	
		
		
		printNumLoadedObjects();
	}

	
	
	private void printNumLoadedObjects(){
		int objCount = 0;
		
		Iterator<Integer> iter = allEntryMethodObjects.keySet().iterator();
		while(iter.hasNext()){
			Integer pe = iter.next();
			List<EntryMethodObject> list = allEntryMethodObjects.get(pe);
			objCount += list.size();
		}
		System.out.println("Displaying " + objCount + " entry method objects in the timeline visualization\n");
	}
	
	
	/** Relayout and repaint everything */
	protected void displayMustBeRedrawn(){
		if(modificationHandler != null){
			modificationHandler.refreshDisplay(true);
		}
	}

	/** repaint everything */
	protected void displayMustBeRepainted(){
		if(modificationHandler != null){
			modificationHandler.refreshDisplay(false);
		}
	}

	
	/** Add or Remove a new line to the visualization representing the sending of a message */
	private void toggleMessageSendLine(EntryMethodObject obj) {
		
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
	
	/** Add or Remove a new line to the visualization representing the entry methods called
	 *  by the object the mouse is over
	 */
	private void toggleMessageCalledByThisLine(EntryMethodObject obj) {
		List<TimelineMessage> tleMsg = obj.getTLmsgs();
		if (tleMsg!=null) {
			for (int i=0; i<tleMsg.size(); i++) { //when to empty the drawMsgsForTheseObjsAlt?
				Set<EntryMethodObject> entMethSet = this.messageStructures.getMessageToExecutingObjectsMap().get(tleMsg.get(i));
				if (entMethSet!=null) {
					Iterator<EntryMethodObject> iter = entMethSet.iterator();
					while (iter.hasNext()) {
						EntryMethodObject emo = iter.next();
						if (drawMessagesForTheseObjectsAlt.contains(emo))
							drawMessagesForTheseObjectsAlt.remove(emo);
						else
							drawMessagesForTheseObjectsAlt.add(emo);
					}
				}
			}
			displayMustBeRepainted();
		}
	}
	
	/**Remove all lines from forward and backward tracing from Timelines display*/
	public void removeLines() {
		drawMessagesForTheseObjectsAlt.clear();
		drawMessagesForTheseObjects.clear();
		displayMustBeRepainted();
	}
	
	/** Add a set of new lines to the visualization representing the sending of a message.
	 * @note the caller should call 	displayMustBeRepainted() after adding all desired messages
	 */
	protected void addMessageSendLineAlt(Set<EntryMethodObject> s) {
		drawMessagesForTheseObjectsAlt.addAll(s);
	}

	/** Add a set of objects for which we want their creation messages to be displayed
	 * @note the caller should call 	displayMustBeRepainted() after adding all desired messages
	 */
	protected void addMessageSendLine(Set<EntryMethodObject> s) {
		drawMessagesForTheseObjects.addAll(s);
	}
	
	
	protected void clearMessageSendLines() {
		drawMessagesForTheseObjects.clear();
		drawMessagesForTheseObjectsAlt.clear();
	}
	
	
	protected long endTime(){
		return endTime;
	}
	
	protected Color[] entryColor(){
		return entryColor;
	}

	protected Color getEntryColor(Integer id){
		return MainWindow.runObject[myRun].getEntryColor(id);
	}
	
	protected Color getUserEventColor(Integer id){
		return MainWindow.runObject[myRun].getUserEventColor(id);
	}
	
	public Color getBackgroundColor(){
		if(useCustomColors)
			return customBackground;
		else
			return MainWindow.runObject[myRun].background;
	}


	/** Load the Timeline data for one processor (pe)
	 * 
	 *  This method loads the timeline into: timelineUserEventObjectsArray, allEntryMethodObjects
	 *  
	 *  Note: This function must be thread safe.
	 *  
	 * */
	void getData(Integer pe)
	{
		// Sanity checks first
		if(pe >= MainWindow.runObject[myRun].getSts().getProcessorCount()){
			String err = "Your sts file only specifies " + MainWindow.runObject[myRun].getSts().getProcessorCount() + " PEs, but you are trying somehow to load pe " + pe;
			throw new RuntimeException(err);
		}
		
	
		LinkedList<TimelineEvent> tl = new LinkedList<TimelineEvent>();
		
		/** Stores all user events from the currently loaded PE/time range. It must be sorted,
		 *  so the nesting of bracketed user events can be efficiently processed.
		 *  */
		Set<UserEventObject> userEvents= new TreeSet<UserEventObject>();
		
		LinkedList<EntryMethodObject> perPEObjects = new LinkedList<EntryMethodObject>();
		
		
		try {
			if (MainWindow.runObject[myRun].hasLogData()) {
				MainWindow.runObject[myRun].logLoader.createtimeline(pe, startTime, endTime, tl, userEvents, minEntryDuration);
			} else {
				System.err.println("Error loadign log files!");
				return;
			}
		} catch (LogLoadException e) {
			System.err.println("LOG LOAD EXCEPTION");
			e.printStackTrace();
			return;
		}
		
		
		if(skipLoadingUserEvents()){
			userEvents.clear();
		}
		
		// Save perPEObjects and userEvents
		getDataSyncSaveObjectLists(pe, perPEObjects, userEvents);
		
		long minMemThisPE = Long.MAX_VALUE;
		long maxMemThisPE = 0;
		
		long minUserSuppliedThisPE = Long.MAX_VALUE;
		long maxUserSuppliedThisPE = 0;
		
		
		// process timeline events
		for(TimelineEvent tle : tl) {
			
			// Construct a list of messages sent by the object	
			ArrayList<TimelineMessage> msgs = new ArrayList<TimelineMessage>();
			if(tle.MsgsSent!=null && (!skipLoadingMessages()) ){
				msgs.addAll( tle.MsgsSent );
			}
			
			ArrayList<PackTime> packs = null;
			if (tle.PackTimes != null && ! skipLoadingMessages() ) {
				packs = tle.PackTimes;
			}
			
			EntryMethodObject obj = new EntryMethodObject(this, tle, msgs, packs, pe.intValue());
			if((obj.isIdleEvent() || obj.isUnaccountedTime() ) && skipLoadingIdleRegions()){
				// don't load this idle event because we are skipping them
			} else {
				perPEObjects.add(obj);
			}
			
			
			if(tle.memoryUsage!=0){
				if(tle.memoryUsage > maxMemThisPE)
					maxMemThisPE = tle.memoryUsage;
				if(tle.memoryUsage < minMemThisPE)
					minMemThisPE = tle.memoryUsage;
			}
			
			if(tle.UserSpecifiedData!=null){
				if(tle.UserSpecifiedData.intValue() > maxUserSuppliedThisPE)
					maxUserSuppliedThisPE = tle.UserSpecifiedData.intValue();
				if(tle.UserSpecifiedData.intValue() < minUserSuppliedThisPE)
					minUserSuppliedThisPE = tle.UserSpecifiedData.intValue();
			}
			
		
		}
		
		// Thread-safe merge of the min/max values
		getDataSyncSaveMemUsage(minMemThisPE, maxMemThisPE, minUserSuppliedThisPE, maxUserSuppliedThisPE);
		
	}
	
	

	/** Thread-safe storing of the userEvents and perPEObjects */
	synchronized private void getDataSyncSaveObjectLists(Integer pe, List<EntryMethodObject> perPEObjects, Set<UserEventObject> userEvents )  
	{
		// The user events are simply that which were produced by createtimeline
		allUserEventObjects.put(pe,userEvents);
		// The entry method objects must however be constructed
		allEntryMethodObjects.put(pe,perPEObjects);
	}
	
	/** Thread safe updating of the memory and user supplied ranges */
	synchronized private void getDataSyncSaveMemUsage(long minMemThisPE, long maxMemThisPE, long minUserSuppliedThisPE, long maxUserSuppliedThisPE)  
	{
		if(minMemThisPE < minMem)
			minMem = minMemThisPE;
		if(maxMemThisPE > maxMem)
			maxMem = maxMemThisPE;
		
		
		if(minUserSuppliedThisPE < minUserSupplied)
			minUserSupplied = minUserSuppliedThisPE;
		if(maxUserSuppliedThisPE > maxUserSupplied)
			maxUserSupplied = maxUserSuppliedThisPE;
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
	
	protected void increaseScaleFactor(){
		setScaleFactor( roundScale( getScaleFactor() *  scaleChangeFactor ));
	}
	
	protected void decreaseScaleFactor(){
		setScaleFactor( roundScale( getScaleFactor() / scaleChangeFactor ));
	}
	

	
	/**	 the width of the timeline portion that is drawn(fit so that labels are onscreen) */
	protected int lineWidth(int actualDisplayWidth) {
		return actualDisplayWidth - 2*offset();
	}

	protected int maxLabelLen(){
		return 70;
	}   

	/** Number of processors in the processor List */
	protected int numPs(){
		return peToLine.size();
	}
	/** The maximum processor index in the processor list, or -1 if null processor list */
	protected int numPEs(){
		return MainWindow.runObject[myRun].getNumProcessors();		
	}	

	/** the left/right margins for the timeline & axis. 
	 * Needed because text labels may extend before and 
	 * after the painted line 
	 */
	protected int offset(){
		if(viewType == ViewType.VIEW_MINIMAL)
			return maxLabelLen()/2;
		else
			return 5 + maxLabelLen()/2;

	}

	/** return pixel offset for left margin */
	private int leftOffset(){
		return offset();
	}


	private int topOffset(){
		switch(viewType){
		case VIEW_SUPERCOMPACT:
			return 0;
		case VIEW_MINIMAL:
		case VIEW_COMPACT:
			return 1;
		default :
			return 4;	
		}
	}
	
	private int bottomOffset(){
		switch(viewType){
		case VIEW_SUPERCOMPACT:
			return 0;
		case VIEW_MINIMAL:
		case VIEW_COMPACT:
			return 1;
		default	 :
			return 4;
		}
		
	}

	
	/** The width we should draw in, compensated for the scaling(zoom) factor 
	 * 
	 * 
	 * @note this should only be called by a layout manager that knows the size
	 * of the screen.
	 * 
	 * */
	protected int scaledScreenWidth(int actualDisplayWidth){
		mostRecentScaledScreenWidth = (int)(actualDisplayWidth * scaleFactor);
		return mostRecentScaledScreenWidth;
	}


	/** The height of the panel that should be used to draw the timelines  */
	protected int screenHeight(){
			int paddingForScrollbar = 25;
			return singleTimelineHeight()*numPs() + paddingForScrollbar;
	}


	/** The height of the timeline event object rectangles */
	protected int barheight(){
		switch(viewType){
		case VIEW_COMPACT :
			return 12;
		case VIEW_SUPERCOMPACT :
			return 1;
		default	 :
			return 16;
		}
	}
		
	/** Get the height required to draw a single PE's Timeline */
	public int singleTimelineHeight(){
		return topOffset() + userEventRectHeight() + barheight() + messageSendHeight() + bottomOffset();
	}

	
	
	/** get the height of each user event rectangle */
	protected int userEventRectHeight(){
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


	/** Choose a new time range to display. 
	 * 	Scale will be reset to zero, and
	 *  the old range will be recorded */
	protected void setNewRange(long beginTime, long endTime) {
		this.startTime = beginTime;
		this.endTime = endTime;
		setScaleFactor(1.0f);
		storeRangeToPersistantStorage();
	}


	public void setRange(long beginTime, long endTime){
		this.startTime = beginTime;
		this.endTime = endTime;
		storeRangeToPersistantStorage();
	}

	/** Set the scale factor. This will cause the handler to layout and repaint panels and update buttons */
	public void setScaleFactor(float scale_){
		scaleFactor = scale_;
		if (scaleFactor < 1.0) {
			scaleFactor = 1.0f;
		}
		displayMustBeRedrawn();
	}



	protected long totalTime(){
		return endTime-startTime;
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
	
	protected boolean selectionValid(){
		return (selection1>=0 && selection2>=0 && selection1!=selection2);
	}

	protected boolean highlightValid(){
		return (highlight>=0);
	}

	/** Invalidate the current selection
	 * 
	 * @note Call this when a window resizes or the selection no longer should be displayed
	 * 
	 */
	protected void invalidateSelection(){
		selection1=-1;
		selection2=-1;
		modificationHandler.refreshDisplay(false);
	}	


	/** Get the left selection coordinate 
	 * 
	 * @note This should only be called if selectionValid() already returns true
	 * */
	protected int leftSelection(){
		if(selection1 < selection2)
			return selection1;
		else
			return selection2;
	}

	/** Get the right selection coordinate 
	 * 
	 * @note This should only be called if selectionValid() already returns true
	 * */
	protected int rightSelection(){
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
	protected void removeHighlight() {
		highlight = -1;
		modificationHandler.refreshDisplay(false);
	}
	protected void setHighlight(int x) {
		highlight = x;
		modificationHandler.refreshDisplay(false);
	}
	protected int getHighlight() {
		return highlight;
	}
	protected double getHighlightTime() {
		return screenToTime(getHighlight());
	}
	protected double leftSelectionTime() {
		return screenToTime(leftSelection());
	}
	protected double rightSelectionTime() {
		return screenToTime(rightSelection());
	}

	/** Convert screen coordinates to time
	 * 
	 * @note requires that mostRecentScaledScreenWidth be correct prior to invocation,
	 * so you should call  scaledScreenWidth(int actualDisplayWidth) before this
	 */
	private long screenToTime(int xPixelCoord){
		double fractionAlongAxis = ((double) (xPixelCoord-leftOffset())) /
		((double)(mostRecentScaledScreenWidth-2*offset()));

		return Math.round(startTime + fractionAlongAxis*(endTime-startTime));	
	}



	/** Convert time to screen coordinate, The returned pixel is the central pixel for this time if a microsecond is longer than one pixel
	 * 
	 * @note requires that mostRecentScaledScreenWidth be correct prior to invocation,
	 * so you should call  scaledScreenWidth(int actualDisplayWidth) before this
	 */
	protected int timeToScreenPixelRight(double time) {
		double fractionAlongTimeAxis =  ((time+0.5-startTime)) /((endTime-startTime));
		return offset() + (int)Math.floor(fractionAlongTimeAxis*(mostRecentScaledScreenWidth-2*offset()));
	}
	
	/** Convert time to screen coordinate, The returned pixel is the leftmost pixel for this time if a microsecond is longer than one pixel
	 * 
	 * @note requires that mostRecentScaledScreenWidth be correct prior to invocation,
	 * so you should call  scaledScreenWidth(int actualDisplayWidth) before this
	 */
	protected int timeToScreenPixelLeft(double time) {
		double fractionAlongTimeAxis =  ((time-0.5-startTime)) /((endTime-startTime));
		return offset() + (int)Math.ceil(fractionAlongTimeAxis*(mostRecentScaledScreenWidth-2*offset()));
	}


	/** Convert time to screen coordinate, The returned pixel is the central pixel for this time if a microsecond is longer than one pixel */
	protected int timeToScreenPixel(double time) {
		double fractionAlongTimeAxis =  ((time-startTime)) /((endTime-startTime));
		return offset() + (int)(fractionAlongTimeAxis*(mostRecentScaledScreenWidth-2*offset()));
	}
	
	/** Convert time to screen coordinate, The returned pixel is the central pixel for this time if a microsecond is longer than one pixel */
	protected int timeToScreenPixel(double time, int assumedScreenWidth) {
		double fractionAlongTimeAxis =  ((time-startTime)) /((endTime-startTime));
		return offset() + (int)(fractionAlongTimeAxis*(assumedScreenWidth-2*offset()));
	}
	
	/** Convert time to screen coordinate, The returned pixel is the leftmost pixel for this time if a microsecond is longer than one pixel */
	protected int timeToScreenPixelLeft(double time, int assumedScreenWidth) {
		double fractionAlongTimeAxis =  ((time-0.5-startTime)) /((endTime-startTime));
		return offset() + (int)Math.ceil(fractionAlongTimeAxis*(assumedScreenWidth-2*offset()));
	}
	
	/** Convert time to screen coordinate, The returned pixel is the rightmost pixel for this time if a microsecond is longer than one pixel */
	protected int timeToScreenPixelRight(double time, int assumedScreenWidth) {
		double fractionAlongTimeAxis =  ( (time+0.5-startTime)) /((endTime-startTime));
		return offset() + (int)Math.floor(fractionAlongTimeAxis*(assumedScreenWidth-2*offset()));
	}
	
		

	/** Set the preferred position for the horizontal view or scrollbar  */
	public void setPreferredViewTimeCenter(double time) {
		if(time > startTime && time < endTime)
			preferredViewTime = time;
	}

	/** Get the preferred position for the horizontal view in screen pixels.
	 	@note must be called after scaledScreenWidth(newWidth)
	 */
	protected int getNewPreferredViewCenter(int newScreenWidth){
		double value = preferredViewTime;
		int coord = timeToScreenPixel(value,newScreenWidth);
		return coord;
	}

	
	/** Discard the previously stored desired view. Does NOT reset the view */
	protected void resetPreferredView(){
		preferredViewTime = -1.0;
	}	
	
	protected boolean hasNewPreferredView(){
		if (preferredViewTime >= 0.0 && scaleFactor > 1.0)
			return true;
		else
			return false;
	}

		
	private boolean keepViewCentered = false;
	/** Request that the layout manager not change the scrollbar position, but rather keep it centered on the same location */ 
	protected void keepViewCentered(boolean b){
		keepViewCentered = b;
	}	
	protected boolean keepViewCentered() {
		return keepViewCentered;
	}
	
	/** The height of the little line below the entry method designating a message send */
	protected int messageSendHeight() {
		if(this.useCompactView()){
			return 0;
		} else {
			return 5;
		}
	}
	/** The height of the rectangle that displays the message pack time below the entry method */
	protected int messagePackHeight() {
		return 3;
	}
	
	/** Do something when the user left clicks on an entry method object */
	protected void clickTraceSender(EntryMethodObject obj) {
		if(viewType != ViewType.VIEW_MINIMAL){
			addProcessor(obj.pCreation);
			toggleMessageSendLine(obj);
			if (traceMessagesForwardOnClick)
				toggleMessageCalledByThisLine(obj);
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
	
	/** Forward tracing by one step when left-clicking on an entry method */
	private boolean traceMessagesForwardOnClick;

	
	public static enum ViewType {
		/** The normal display mode */
		VIEW_NORMAL, 
		/** Compact the entry margins around the entry method objects, and eliminate user events */
		VIEW_COMPACT, 
		/** Compact the entry margins around the entry method objects, and eliminate user events */
		VIEW_SUPERCOMPACT, 
		/** Produce a single PE embedded Timeline for use in other tools (such as NoiseMiner) */
		VIEW_MINIMAL
	}
	
	/** Should we use a very compact view, with no message sends? */
	private ViewType viewType;

	int colorSchemeForUserSupplied;
	
	/** Clear any highlights created by HighlightObjects() */
	protected void clearObjectHighlights() {
		highlightedObjects.clear();
	}
	
	/** Highlight the given set of timeline objects */
	protected void highlightObjects(Set<Object> objects) {
		highlightedObjects.addAll(objects);
	}

	/** Determine if an object should be dimmed. 
	 * If there are any objects set to be highlighted, 
	 * all others will be dimmed 
	 */
	protected boolean isObjectDimmed(Object o){
		if(highlightedObjects.size() == 0)
			return false;
		else
			return ! highlightedObjects.contains(o);
	}
	
	protected boolean traceMessagesBackOnHover() {
		return traceMessagesBackOnHover;
	}
	
	protected boolean traceMessagesForwardOnHover() {
		return traceMessagesForwardOnHover;
	}
	
	protected boolean traceMessagesForwardOnClick() {
		return traceMessagesForwardOnClick;
	}
	
	protected boolean traceOIDOnHover() {
		return traceOIDOnHover;
	}
	
	public void setTraceMessagesBackOnHover(boolean traceMessagesOnHover) {
		this.traceMessagesBackOnHover = traceMessagesOnHover;
		
		if(traceMessagesOnHover)
			setToolTipDelayLarge();
		else
			setToolTipDelaySmall();
		
	}
	
	public void setTraceMessagesForwardOnHover(boolean traceMessagesForwardOnHover) {
		this.traceMessagesForwardOnHover = traceMessagesForwardOnHover;
		
		if(traceMessagesForwardOnHover)
			setToolTipDelayLarge();
		else
			setToolTipDelaySmall();
		
	}
		
	public void setTraceMessagesForwardOnClick(boolean traceMessagesForwardOnClick) {
		this.traceMessagesForwardOnClick = traceMessagesForwardOnClick;
	}
	
	public void setTraceOIDOnHover(boolean showOIDOnHover) {
		this.traceOIDOnHover = showOIDOnHover;
	}
	
	
	protected void setToolTipDelaySmall() {
		ToolTipManager.sharedInstance().setInitialDelay(0);
		ToolTipManager.sharedInstance().setDismissDelay(600000);	
	}
	

	private void setToolTipDelayLarge() {
		ToolTipManager.sharedInstance().setInitialDelay(2000);
		ToolTipManager.sharedInstance().setDismissDelay(10000);	
	}
	
	public Color getMessageColor() {
		return getForegroundColor();
	}
	
	public Color getMessageAltColor() {
		return Color.yellow;
	}

	protected void showUserEvents(boolean b) {
		showUserEvents = b;
	}

	protected boolean showUserEvents() {
		if(useCompactView())
			return false;
		else
			return showUserEvents;
	}

	
	protected void setColorByDefault() {
		colorByObjectId = false;
		colorByMemoryUsage=false;
		colorByUserSupplied=false;
		colorByEntryId = false;
		colorByEntryIdFreq = false;
		displayMustBeRepainted();
	}
	

	public void setColorForEntry(int id, Color c) {
		if(c !=null){
			MainWindow.runObject[myRun].setEntryColor(id, c);
			displayMustBeRepainted();
		}	 
	}	
	

	/** Color the events by memory usage if possible */
	protected void setColorByMemoryUsage() {
		if(memoryUsageValid()){
			/* Prompt for range of values to use for colors */
			new MemoryColorRangeChooser(this);
		} else {
			modificationHandler.displayWarning("No memory usage entries found. Use traceMemoryUsage() in the application");
		}		
	}
	
	
	/** After the user has chosen a memory range, then recolor stuff */
	protected void finalizeColorByMemoryUsage(){
		colorByMemoryUsage=true;
		colorByObjectId = false;
		colorByUserSupplied=false;
		colorByEntryId = false;
		colorByEntryIdFreq = false;
		displayMustBeRepainted();
	}

	public void setColorByUserSupplied(int colorScheme) {
		colorSchemeForUserSupplied=colorScheme;
		colorByUserSupplied=true;
		colorByObjectId = false;
		colorByMemoryUsage=false;
		colorByEntryId = false;
		colorByEntryIdFreq = false;
		displayMustBeRepainted();
	}

	protected void setColorByObjectID() {
		colorByObjectId = true;
		colorByMemoryUsage=false;
		colorByUserSupplied=false;
		colorByEntryId = false;
		colorByEntryIdFreq = false;
		displayMustBeRepainted();
	}
	
	
	public void setColorByUserSuppliedAndObjID(int colorScheme) {
		colorSchemeForUserSupplied=colorScheme;
		colorByUserSupplied=true;
		colorByObjectId = true;
		colorByMemoryUsage=false;
		colorByEntryId = false;
		colorByEntryIdFreq = false;
		displayMustBeRepainted();
	}
	
	public void setColorByUserSuppliedAndEID(int colorScheme) {
		colorSchemeForUserSupplied=colorScheme;
		colorByUserSupplied=true;
		colorByObjectId = false;
		colorByMemoryUsage=false;
		colorByEntryId = true;
		colorByEntryIdFreq = false;
		displayMustBeRepainted();
	}

    	public void setColorByEID() {
		colorByUserSupplied = false;
		colorByObjectId = false;
		colorByMemoryUsage = false;
		colorByEntryId = true;
		colorByEntryIdFreq = false;
		displayMustBeRepainted();
	}
    	public void setColorByEIDFreq() {
    		colorByUserSupplied = false;
    		colorByObjectId = false;
    		colorByMemoryUsage = false;
    		colorByEntryId = false;
    		colorByEntryIdFreq = true;
    		displayMustBeRepainted();
    	}
	
	
	protected boolean colorByEID() {
		return colorByEntryId;
	}
	
	protected boolean colorByEIDFreq() {
		return colorByEntryIdFreq;
	}

	protected boolean colorByOID() {
		return colorByObjectId;
	}
			
	protected boolean colorByUserSupplied() {
		return colorByUserSupplied;
	}

	protected boolean colorByMemoryUsage() {
		return colorByMemoryUsage;
	}
	
	/** Fixup the messages that were sent back in time, breaking the causality assumptions many hold to be true */
	protected void fixTachyons() {
		System.out.println("The fix tachyons feature is still experimental. It will probably not work well if new processors are loaded, or ranges are changed");
		
		int numIterations = 2*numPEs();
		long threshold_us = 10;
		
		System.out.println("Executing at most " + numIterations + " times or until no tachyons longer than " + threshold_us + "us are found");
		
		for(int iteration = 0; iteration < numIterations; iteration++){

			long minLatency = Integer.MAX_VALUE;
//			int minSender = -1;
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

//						int senderPE = m.srcPE;
						int executingPE = obj.pCurrent;

						if(minLatency> latency ){
							minLatency = latency;
//							minSender = senderPE;
							minDest = executingPE;	
						}
					}
				}
			}

//			System.out.println("Processor skew is greatest between " + sender + " and " + dest);

			System.out.println("Processor " + minDest + " is lagging behind by " + (-1*minLatency) + "us");

			// Adjust times for all objects and messages associated with processor dest
			Integer laggingPE = Integer.valueOf(minDest);
			
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
	
	public void setViewType(ViewType vt) {
		viewType = vt;
		displayMustBeRedrawn();
	}

	
	/** Should the message pack time regions be displayed */
	protected boolean showPacks() {
		if(useCompactView())
			return false;
		else
			return showPacks;
	}

	/** Should the message send points be displayed */
	protected boolean showMsgs() {
		if(useCompactView())
			return false;
		else
			return showMsgs;
	}
	
	/** Should the idle regions be displayed */
	protected boolean showIdle() {
		return showIdle;
	}
	
	protected void showIdle(boolean b) {
		showIdle = b;
	}
	
	protected void showPacks(boolean b) {
		showPacks = b;
	}
	
	protected void showMsgs(boolean b) {
		showMsgs = b;
	}
	
	protected boolean useCompactView() {
		return (viewType != ViewType.VIEW_NORMAL);
	}

	
	
	/** Determines which vertical position represents PE */
	private int whichTimelineVerticalPosition(int PE) {
		if(peToLine==null){
			throw new RuntimeException("peToLine is null");
		}
		if(!peToLine.contains(Integer.valueOf(PE))){
			throw new RuntimeException("peToLine does not contain pe " + PE);
		}
		return peToLine.indexOf(Integer.valueOf(PE));
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
	protected int whichPE(int verticalPosition) {
		Integer which = peToLine.get(verticalPosition);
		return which;
	}


	protected void movePEToLine(int PE, int newPos){
		Integer p = Integer.valueOf(PE);
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
	
	
	
	protected void printUserEventInfo(){

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
					Integer UserEventID = Integer.valueOf(obj.UserEventID); 

					long duration = EndTime-BeginTime;

					if(! min.containsKey(UserEventID)){
						min.put(UserEventID, new Long(duration));
						max.put(UserEventID, new Long(duration));
						total.put(UserEventID, new Long(duration));
						count.put(UserEventID, new Long(1));
						name.put(UserEventID, obj.getName());
					} else {

						if(min.get(UserEventID) > duration){
							min.put(UserEventID, Long.valueOf(duration));
						}

						if(max.get(UserEventID) < duration){
							max.put(UserEventID, Long.valueOf(duration));
						}

						total.put(UserEventID, total.get(UserEventID) + Long.valueOf(duration));
						count.put(UserEventID, count.get(UserEventID) + Long.valueOf(1));

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
	private int determineUserEventNestings(){
		
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
	protected void showNestedUserEvents(boolean b) {
		
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
	protected int entryMethodLocationTop(int pe) {
		int yidx = whichTimelineVerticalPosition(pe);
		return singleTimelineHeight()*yidx + topOffset() + userEventRectHeight();
	}
	
	/** The pixel height of the entry method object. This includes just the rectangular region and the descending message sends */
	protected int entryMethodLocationHeight() {
		return barheight()+messageSendHeight();
	}

	private int userEventLocationTop(int pe) {
		int yidx = whichTimelineVerticalPosition(pe);
		return singleTimelineHeight()*yidx + topOffset();
	}

	protected int userEventLocationBottom(int pe) {
		return userEventLocationTop(pe) + userEventRectHeight();
	}
	
	protected int horizontalLineLocationTop(int i) {
		return singleTimelineHeight()*i + topOffset() + userEventRectHeight() + (barheight()/2);		
	}
	
	/** The message send tick mark bottom point*/
	protected int messageSendLocationY(int pe) {
		int yidx = whichTimelineVerticalPosition(pe);
		return singleTimelineHeight()*yidx + topOffset() + userEventRectHeight() + barheight()+this.messageSendHeight();
	}
	
	/** The message send tick mark bottom point*/
	protected int messageRecvLocationY(int pe) {
		int yidx = whichTimelineVerticalPosition(pe);
		return singleTimelineHeight()*yidx + topOffset() + userEventRectHeight();
	}

	
	protected void dropPEsUnrelatedToPE(Integer pe) {
		dropPEsUnrelatedToObjects(allEntryMethodObjects.get(pe));
	}
	
	protected void dropPEsUnrelatedToObject(EntryMethodObject obj) {
		System.out.println("dropPEsUnrelatedToObject()");
		HashSet<EntryMethodObject> set = new HashSet<EntryMethodObject>();
		set.add(obj);
		dropPEsUnrelatedToObjects(set);
	}
	
	

	private void dropPEsUnrelatedToObjects(Collection<EntryMethodObject> objs) {
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
	private void dropPEsNotInList(Set<Integer> keepPEs){
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
		displayMustBeRedrawn();

	}

	
	/** Produce a map containing the ids and nicely mangled string names for each entry method */
	public Map<Integer, String> getEntryNames() {
		return MainWindow.runObject[myRun].getSts().getPrettyEntryNames();
	}
	
	
	/** Produce a hashmap containing the ids and nicely mangled string names for each user event */
	public Map<Integer, String> getUserEventNames() {
		return MainWindow.runObject[myRun].getSts().getUserEventNameMap();
	}


	
	protected void makeEntryVisibleID(Integer id){		
		makeEntryVisibleID(id, true);
	}
	protected void makeEntryInvisibleID(Integer id){		
		makeEntryInvisibleID(id, true);
	}
	
	/** Make visible the entry methods for this id */	
	protected void makeEntryVisibleID(Integer id, boolean redraw) {
		hiddenEntryPoints.remove(id);
		if(redraw)
			this.displayMustBeRedrawn();
	}	

	/** Hide the entry methods for this id */
	protected void makeEntryInvisibleID(Integer id, boolean redraw) {
		hiddenEntryPoints.add(id);
		if(redraw)
			this.displayMustBeRedrawn();
	}
	

	/** Make visible the entry methods for this id */	
	protected void makeUserEventVisibleID(Integer id) {
		hiddenUserEvents.remove(id);
		this.displayMustBeRedrawn();
	}	

	/** Hide the entry methods for this id */
	protected void makeUserEventInvisibleID(Integer id) {
		hiddenUserEvents.add(id);
		this.displayMustBeRedrawn();
	}

	
	
	
	
	protected void makeUserSuppliedNotesVisible() {
		hideUserSuppliedNotes = false;
		this.displayMustBeRedrawn();
	}	

	protected void makeUserSuppliedNotesInvisible() {
		hideUserSuppliedNotes = true;
		this.displayMustBeRedrawn();
	}
	
//	public boolean userSuppliedNotesVisible() {
//		return ! hideUserSuppliedNotes;	
//	}

	protected boolean userSuppliedNotesHidden() {
		return hideUserSuppliedNotes;	
	}

	
	protected boolean entryIsHiddenID(Integer id) {
		return hiddenEntryPoints.contains(id);
	}
	
	protected boolean entryIsVisibleID(Integer id) {
		return ! hiddenEntryPoints.contains(id);
	}
	
	protected boolean userEventIsHiddenID(Integer id) {
		return hiddenUserEvents.contains(id);
	}
	
	protected void skipLoadingIdleRegions(boolean b, boolean filterAlreadyLoaded) {
		skipIdleRegions = b;
		if(skipIdleRegions && filterAlreadyLoaded){
			pruneOutIdleRegions();	
		}
	}

	protected void skipLoadingMessages(boolean b, boolean filterAlreadyLoaded) {
		skipLoadingMessages = b;
		if(skipLoadingMessages && filterAlreadyLoaded){
			pruneOutMessages();	
		}
	}
	
	protected void skipLoadingUserEvents(boolean b) {
		skipLoadingUserEvents = b;
	}
	
	private boolean skipLoadingUserEvents(){
		return skipLoadingUserEvents;
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
				o.messages = null;
			}
		
		}
		messageStructures.clearAll();
		
		clearMessageSendLines();

		modificationHandler.notifyProcessorListHasChanged(); // Really it is the set of objects that has changed
		displayMustBeRedrawn();
	}
	private boolean skipLoadingIdleRegions() {
		return skipIdleRegions;
	}
	private boolean skipLoadingMessages() {
		return skipLoadingMessages;
	}

	protected long minMemMB() {
		return minMem / 1024 / 1024;
	}

	protected long maxMemMB() {
		return maxMem / 1024 / 1024;
	}	

	protected long minMemBColorRange() {
		return minMemColorRange;
	}

	protected long maxMemBColorRange() {
		return maxMemColorRange;
	}

	/** Set the memory usage range for the color gradient */
	protected void setMemColorRange(long minMemVal, long maxMemVal) {
		minMemColorRange = minMemVal;
		maxMemColorRange = maxMemVal;
	}
	
	protected void setFilterEntryShorterThan(long l) {
		minEntryDuration = l;
	}
	
	protected void displayLegend() {
 		if(memoryUsageValid()){
			new MemoryLegend(this);
		}

	}
	
	public void finalize() throws Throwable
	{
		disposeOfStructures();
		super.finalize(); //not necessary if extending Object.
	} 
	
	public void disposeOfStructures()
	{
		entries = null;
		entryColor = null;
		hiddenEntryPoints = null;
		allEntryMethodObjects = null;
		allUserEventObjects = null;
		processorUsage = null;
		packUsage = null;
		entryUsageList = null;
		messageStructures = null;
		drawMessagesForTheseObjects = null;
		drawMessagesForTheseObjectsAlt = null;
	}
	
	public void makeFrequencyMap(int[] frequencyOfEntries) {
		TreeMap<Integer, LinkedList<Integer>> mapToReturn = new TreeMap<Integer, LinkedList<Integer>>();
		for (int i=0; i< frequencyOfEntries.length; i++) {
			if(mapToReturn.containsKey(entries[i])) {
				mapToReturn.get(entries[i]).add(i);
			}
			else {
				LinkedList<Integer> ll = new LinkedList<Integer>();
				ll.add(i);
				mapToReturn.put(entries[i], ll);
			}
		}
		frequencyTreeMap = mapToReturn;
	}
	
	//Returns a vector of the entry methods sorted by their frequency, starting with the least frequent and ending
	//with the most frequent
	public void makeFreqVector() {
		ArrayList<Integer> vectorToReturn = new ArrayList<Integer>();
		Collection<LinkedList<Integer>> collec = frequencyTreeMap.values();
		Iterator<LinkedList<Integer>> iter = collec.iterator();
		
		while(iter.hasNext()) {
			LinkedList<Integer> tempLinkedL = iter.next();
			for(int i=0; i<tempLinkedL.size(); i++) {
				vectorToReturn.add(0, tempLinkedL.get(i));
			}
		}
		
		frequencyVector = vectorToReturn;
		
		//to add the int to the end of the Vector arrayToReturn.addElement(Integer??);
		
		//or iterate normal way through the treemap, then just add the integer to the
		//beginning of the arrayToReturn using arrayToReturn.add(0, Integer)
	}
	
	 public void setFrequencyColors() {
		 Analysis a = MainWindow.runObject[myRun];
		 a.activityColors = a.colorManager.defaultColorMap();
		 a.entryColors = ColorManager.entryColorsByFrequency(ColorManager.createComplementaryColorMap(entries.length), frequencyVector);
		 a.userEventColors = a.activityColors[a.USER_EVENTS];
		 a.functionColors = a.activityColors[a.FUNCTIONS];
	  }
	public ViewType getViewType() {
		return viewType;
	}
}
