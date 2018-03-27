package projections.Tools.Timeline;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
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
import java.util.SortedSet;

import java.util.Map.Entry;
import java.util.logging.Level;

import javax.swing.ToolTipManager;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import projections.Tools.Timeline.RangeQueries.Query1D;
import projections.Tools.Timeline.RangeQueries.RangeQueryTree;
import projections.analysis.Analysis;
import projections.analysis.PackTime;
import projections.analysis.TachyonShifts;
import projections.analysis.TimedProgressThreadExecutor;
import projections.analysis.TimelineEvent;
import projections.gui.ColorManager;
import projections.gui.ColorUpdateNotifier;
import projections.gui.EntryMethodVisibility;
import projections.gui.MainWindow;
import projections.misc.LogLoadException;
import projections.analysis.StsReader;


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
 * 
 *  For thread safety, all rendering calls or calls that update the data in this object must synchronize on this.
 *  However, no GUI calls (including progress bar updates) should occur within a synchronized region.
 *  The paintComponent methods also run in the SWING GUI thread, and they do synchronize on this. 
 * 
 * @author idooley et. al.
 *
 */

public class Data implements ColorUpdateNotifier, EntryMethodVisibility
{

	public enum ColorScheme {
		BlueGradientColors, RandomColors
	}

	public class SMPMsgGroup implements Comparable{
		public EntryMethodObject sendWPe;
		public EntryMethodObject sendCPe;
		public EntryMethodObject recvCPe;
		public EntryMethodObject recvWPe;
		
		@Override
		public int compareTo(Object o){
			SMPMsgGroup obj = (SMPMsgGroup)o;
/*
			//A simple way, but may not be that efficient because of the possible more
			//function calls
			int[] val=new int[4];
			val[0] = sendWPe.compareTo(obj.sendWPe);
			val[1] = sendCPe.compareTo(obj.sendCPe);
			val[2] = recvCPe.compareTo(obj.recvCPe);
			val[3] = recvWPe.compareTo(obj.recvWPe);
			
			for(int i=0; i<4; i++){
				if(val[i]!=0) return val[i];
			}
			return 0;
*/			
			int swval = sendWPe.compareTo(obj.sendWPe);
			if(swval == 0){
				int scval = sendCPe.compareTo(obj.sendCPe);
				if(scval == 0){
					int rcval = recvCPe.compareTo(obj.recvCPe);
					if(rcval == 0){
						return recvWPe.compareTo(obj.recvWPe);						
					}else
						return rcval;
				}else
					return scval;
			}else
				return swval;			
		}
		
		@Override
		public boolean equals(Object o){
			if(this == o) return true;
			if(!(o instanceof SMPMsgGroup)) return false;
			if(compareTo(o) == 0) return true;
			else return false;
		}
	}

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
	public ArrayList<Integer> peToLine;

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

	/** A set of entry point ids that should be hidden */
	private Set<Integer> hiddenEntryPoints;

	/** A set of user events that should be hidden */
	private Set<Integer> hiddenUserEvents;

	/** A synchronized collection of the Entry Methods for each PE */ 
	protected Map<Integer, Query1D<EntryMethodObject>> allEntryMethodObjects = Collections.synchronizedMap(new TreeMap<Integer,Query1D<EntryMethodObject> >());

	/** A synchronized collection of the User Events for each PE  */
	protected Map<Integer, Query1D<UserEventObject>> allUserEventObjects = Collections.synchronizedMap(new TreeMap<Integer, Query1D <UserEventObject> >());



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

	/** If true, we will display the top idle and entry times for this range */
	private boolean displayTopTimes;

	/** Only display the top X longest idle and entry times */
	private int amountTopTimes;

	/** Used to display the list of longest idle and entry times */
	private String topTimesText;

	/** The font used by the LabelPanel */
	protected Font labelFont;
	/** The font used by the time labels on the TimelineAxisCanvas */
	protected Font axisFont;

	/** The left/right margins for the timeline & axis */
	private int offset;

	/** A set of objects for which we draw their creation message lines */
	protected Set<EntryMethodObject> drawMessagesForTheseObjects;
	/** A set of objects for which we draw their creation message lines in an alternate color */
	protected Set<EntryMethodObject> drawMessagesForTheseObjectsAlt;
	
	protected SMPMsgGroup toPaintSMPMsgGrp;

	private boolean useCustomColors=false;

	/** A custom foreground color that can override the application wide background pattern. Used by NoiseMiner to set a white background */
	private Color customForeground;
	private Color customBackground;

	private int numUserEventRows = 1;
	private boolean drawNestedUserEventRows = false;
	private int numNestedIDs = -1;
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

		peToLine = new ArrayList<Integer>();

		messageStructures = new MessageStructures(this);

		hiddenEntryPoints = new TreeSet<Integer>();
		hiddenUserEvents = new TreeSet<Integer>();

		viewType = ViewType.VIEW_NORMAL;

		oldBT = -1;
		oldET = -1;

		processorUsage = null;

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
		toPaintSMPMsgGrp = new SMPMsgGroup();

		entries = new int[MainWindow.runObject[myRun].getNumUserEntries()];
		makeFrequencyMap(entries);
		makeFreqVector();

		labelFont = new Font("SansSerif", Font.PLAIN, 10); 
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
		displayTopTimes = false;
		minEntryDuration = 0;
		amountTopTimes = 0;

		setOffset();

		// Get the list of PEs to display
		loadGlobalPEList();

	}
	/** 
	 * Add the data for a new processor to this visualization
	 */
	protected void addProcessor(int pe){
		MainWindow.performanceLogger.log(Level.FINE,"Add processor " + pe);
		Integer p = Integer.valueOf(pe);
		if(!peToLine.contains(p)){
			peToLine.add(p);
			MainWindow.performanceLogger.log(Level.FINE,"Add processor " + pe + " to peToLine size=" + peToLine.size() );
			
			if(isSMPRun()){
				int commPE = getCommThdPE(p);
				if(!peToLine.contains(commPE)){
					peToLine.add(commPE);
					MainWindow.performanceLogger.log(Level.FINE,"Add processor " + commPE + " to peToLine size=" + peToLine.size() );
				}
			}
			
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
	public void setProcessorList(SortedSet<Integer> processorList){
		peToLine.clear();

		if(isSMPRun()){
			TreeSet<Integer> commPEs = new TreeSet<Integer>();
			commPEs.clear();
			int prevNID = -1;			
			int prevCommPE = -1;
			boolean isLastAdded = false;
			//insert the comm PE to proper position.
			for(Integer pe: processorList){
				if(isCommThd(pe)){
					//as comm thds are always at the end of this processorList 
					//(i.e. their PEs are always larger than workers' PEs.
					if(!isLastAdded && prevCommPE!=-1){
						peToLine.add(prevCommPE);
						commPEs.add(prevCommPE);
						isLastAdded = true;
					}
					if(commPEs.contains(pe)) continue;
					peToLine.add(pe);
					continue;
				}
				
				if(prevNID==-1){
					prevNID = getNodeID(pe);
					prevCommPE = getCommThdPE(pe);
					peToLine.add(pe);
				}else{
					int curNID = getNodeID(pe);
					if(curNID != prevNID){
						//encounter a new set of PEs of a node, add the comm thd
						//of the previous node
						peToLine.add(prevCommPE);
						commPEs.add(prevCommPE);
						
						prevNID = curNID;
						prevCommPE = getCommThdPE(pe);
					}
					peToLine.add(pe);
				}
			}
			if(!isLastAdded) peToLine.add(prevCommPE);
		}else{
			peToLine.addAll(processorList);
		}		
	}


	/** Load the set of PEs found in MainWindow.runObject[myRun].getValidProcessorList() */
	private void loadGlobalPEList(){
		SortedSet<Integer> processorList = new TreeSet<Integer>(MainWindow.runObject[myRun].getValidProcessorList());
		setProcessorList(processorList);
	}


	/** Get the set of PEs as an OrderedIntList. The internal storage for the PE list is not a sorted list. */
	private SortedSet<Integer> processorListOrdered(){
		return new TreeSet<Integer>(peToLine);
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
	 *  @note caller synchronizes on this
	 */
	protected void createTLOArray(boolean useHelperThreads, Component rootWindow, boolean showProgress)
	{

		// Kill off the secondary processing threads if needed
		messageStructures.kill();

		synchronized(this) {
			// Can we reuse our already loaded data?
			if(startTime >= oldBT && endTime <= oldET){
				// Remove any unused objects from our data structures 

				// scan through oldEntryMethodObjects, cleaning as we go
				Iterator<Entry<Integer, Query1D<EntryMethodObject>>> iter = allEntryMethodObjects.entrySet().iterator();
				while(iter.hasNext()){
					Entry<Integer, Query1D<EntryMethodObject>> e = iter.next();
					Integer pe = e.getKey();
					Query1D<EntryMethodObject> objs = e.getValue();

					if(peToLine.contains(pe)){
						objs.removeEntriesOutsideRange(startTime, endTime);
					} else {
						iter.remove(); // remove unused PE data
					}
				}

				// scan through allUserEventObjects, cleaning as we go
				Iterator<Entry<Integer, Query1D<UserEventObject>>> iter2 = allUserEventObjects.entrySet().iterator();
				while(iter2.hasNext()){
					Entry<Integer, Query1D<UserEventObject>> e = iter2.next();
					Integer pe = e.getKey();
					Query1D<UserEventObject> objs = e.getValue();

					if(peToLine.contains(pe)){
						objs.removeEntriesOutsideRange(startTime, endTime);
					} else {
						iter2.remove(); // remove unused PE data
					}				
				}


			}

			oldBT = startTime;
			oldET = endTime;

		}

		//==========================================	
		// Do multithreaded file reading



		// Create a list of worker threads
		LinkedList<Runnable> readyReaders = new LinkedList<Runnable>();

		for(Integer pe : peToLine){
			if(!allEntryMethodObjects.containsKey(pe)) {
				// No data exist for the pe, so load it
				readyReaders.add(new TimelineRunnableFileReader(pe,this));
			} else {
				// data exists for this pe already, but reload it from scratch
				allEntryMethodObjects.remove(pe);
				allUserEventObjects.remove(pe);
				readyReaders.add(new TimelineRunnableFileReader(pe,this));
			}
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


		synchronized(this){
			//==========================================	
			//  Perform some post processing

			int n = determineNumNestedIDs();
			// Only show nestedIDs if there are at least two of them
			if (n > 0)
				numNestedIDs = n;

			for (int e=0; e<MainWindow.runObject[myRun].getNumUserEntries(); e++) {
				entries[e] = 0;
			}

			processorUsage = new float[numPEs()];
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

			EntryMethodObject[] idleArray = new EntryMethodObject[amountTopTimes()];
			EntryMethodObject[] entryArray = new EntryMethodObject[amountTopTimes()];
			long[] objEndTimes = new long[amountTopTimes()];
			//long[] idleEndTimes = new long[amountTopTimes()];
			for(Integer pe : allEntryMethodObjects.keySet()) {	
				for(EntryMethodObject obj : allEntryMethodObjects.get(pe))
				{

					float usage = obj.getUsage();
					int entryIndex = obj.getEntryIndex();


					if (entryIndex >=0)
					{
						entries[entryIndex]++;
						processorUsage[pe.intValue()] += usage;
						packUsage[pe.intValue()] += obj.getPackUsage();
						entryUsageArray[entryIndex] += obj.getNonPackUsage();
						if (displayTopTimes())
						{
							for (int i = 0; i < amountTopTimes(); i++)//Finds the top X longest entry methods
							{
								if ((entryArray[i] == null) || ((obj.getEndTime() - obj.getBeginTime()) > (entryArray[i].getEndTime() - entryArray[i].getBeginTime())))
								{
									for (int j = amountTopTimes()-1; j > i; j--)
									{
										entryArray[j] = entryArray[j-1];
									}
									entryArray[i] = obj;
									break;
								}
							}
						}
					}
					else
					{
						idleUsage[pe.intValue()] += usage;
						if (displayTopTimes())
						{
							long objEndTimeLong = obj.getEndTime();//prevents bug where end time >= Long.MAX_VALUE
							if (objEndTimeLong > endTime())
							{
								objEndTimeLong = endTime();
							}
							for (int i = 0; i < amountTopTimes(); i++)//Finds the top X longest idle sessions
							{	
								if ((idleArray[i] == null) || ((objEndTimeLong - obj.getBeginTime()) > (objEndTimes[i] - idleArray[i].getBeginTime())))
								{
									for (int j = amountTopTimes()-1; j > i; j--)//doubly linked list would work better
									{
										idleArray[j] = idleArray[j-1];
										objEndTimes[j] = objEndTimes[j-1];
									}
									idleArray[i] = obj;
									objEndTimes[i] = objEndTimeLong;
									break;
								}
							}
						}
					}

				}
			}

			if (displayTopTimes())
			{			
				topTimesText = "<html>";
				topTimesText+="The longest idle times in descending order are: <br>";
				Set<EntryMethodObject> longestObjectsSet = new HashSet<EntryMethodObject>();
				for (int i = 0; i < amountTopTimes(); i++)
				{
					if ((idleArray[i] == null) || (idleArray[i].getBeginTime() == objEndTimes[i]))
					{
						break;
					}
					topTimesText+=(i+1 +": " + (objEndTimes[i] - idleArray[i].getBeginTime()));
					topTimesText+=(" Begin: " + idleArray[i].getBeginTime() + " End: " + objEndTimes[i] + " PE: " + idleArray[i].pe + "<br>");
					longestObjectsSet.add(idleArray[i]);
				}

				topTimesText+="<br>";
				topTimesText+=("The longest entry times in descending order are: <br>");
				for (int i = 0; i < amountTopTimes(); i++)
				{
					if (entryArray[i] == null) break;
					topTimesText+=(i+1 +": " + (entryArray[i].getEndTime() - entryArray[i].getBeginTime()));
					topTimesText+=(" Begin: " + entryArray[i].getBeginTime() + " End: " + entryArray[i].getEndTime());
					topTimesText+=(" PE: " + entryArray[i].pe + " Name: " + MainWindow.runObject[myRun].getEntryFullNameByID(entryArray[i].getEntryID()) + "<br>");
					longestObjectsSet.add(entryArray[i]);
				}
				topTimesText+="</html>";
				HashSet<Object> objsToHilite = new HashSet<Object>();
				objsToHilite.addAll(longestObjectsSet);
				highlightObjects(objsToHilite);
			}

			// Spawn a thread that computes some secondary message related data structures
			messageStructures.create(useHelperThreads);



			printNumLoadedObjects();
		}
	}

	public int getNumNestedIDs() {
		return numNestedIDs;
	}

	private void printNumLoadedObjects(){
		int objCount = 0;
		for(Query1D<EntryMethodObject> e : allEntryMethodObjects.values()){
			objCount += e.size();
		}
		MainWindow.performanceLogger.log(Level.INFO, "Displaying " + objCount + " entry method invocations in the timeline visualization");

		objCount = 0;
		for(Collection<UserEventObject> e : allUserEventObjects.values()){
			objCount += e.size();
		}
		MainWindow.performanceLogger.log(Level.INFO, "Displaying " + objCount + " user events in the timeline visualization");

	}


	/** Relayout and repaint everything */
	public void displayMustBeRedrawn(){
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
	
	protected boolean makeSMPMsgGroup(EntryMethodObject recvWPe){
		TimelineMessage createdMsg = recvWPe.creationMessage();

		//recvCPe is a msg recv operation on CommThd
		//trace back to its sender
		EntryMethodObject recvCPe = messageStructures.getMessageToSendingObjectsMap().get(createdMsg);
		if(!recvCPe.isCommThreadMsgRecv()) return false;
		createdMsg = recvCPe.creationMessage();
		if(createdMsg == null){
			//this should never happen!!
			return false;			 
		}
		//sendCPe should be a msg send operation on CommThd
		EntryMethodObject sendCPe = messageStructures.getMessageToSendingObjectsMap().get(createdMsg);
		if(sendCPe == null) {
			//this should never happen!!
			return false;
		}

		if(!isCommThd(sendCPe.pe)) {
			//this should not happen!
			return false;
		}

		createdMsg = sendCPe.creationMessage();
		if(createdMsg == null){
			//this should never happen!!
			return false;			 
		}

		EntryMethodObject sendWPe = messageStructures.getMessageToSendingObjectsMap().get(createdMsg);
		if(sendWPe == null) {
			//this should never happen!!
			return false;
		}
		
		toPaintSMPMsgGrp.recvWPe = recvWPe;
		toPaintSMPMsgGrp.recvCPe = recvCPe;
		toPaintSMPMsgGrp.sendCPe = sendCPe;
		toPaintSMPMsgGrp.sendWPe = sendWPe;
		return true;
	}

	/**Remove all lines from forward and backward tracing from Timelines display*/
	protected void removeLines() {
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

	public Color getEntryColor(Integer id){
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

	protected int getSMPNodeSize(){
		return MainWindow.runObject[myRun].getSts().getNodeSize();
	}
	protected boolean isCommThd(int pe){
		StsReader sts = MainWindow.runObject[myRun].getSts();
		int totalPes = sts.getProcessorCount();
		int totalNodes = sts.getSMPNodeCount();
		int nodesize = sts.getNodeSize();
		if(pe>=totalNodes*nodesize && pe<totalPes) return true;
		return false;
	}
	protected int getNodeID(int pe){
		StsReader sts = MainWindow.runObject[myRun].getSts();
		int totalPes = sts.getProcessorCount();
		int totalNodes = sts.getSMPNodeCount();
		int nodesize = sts.getNodeSize();
		if(pe>=totalNodes*nodesize && pe<totalPes) return pe-totalNodes*nodesize;
		return pe/nodesize;		
	}
	protected boolean isSMPRun(){
		return MainWindow.runObject[myRun].getSts().isSMPRun();
	}
	protected int getCommThdPE(int pe){
		int nid = getNodeID(pe);
		StsReader sts = MainWindow.runObject[myRun].getSts();
		return sts.getNodeSize()*sts.getSMPNodeCount()+nid;
		
	}

	protected String getPEString(int pe){
		if(isSMPRun()){
			int nid = getNodeID(pe);
			if(isCommThd(pe))
				return "CommP (N"+nid+")";
			else
				return "P"+pe+" (N"+nid+")";
		}else{
			return "PE "+pe;
		}
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
			String err = "The sts file only specifies " + MainWindow.runObject[myRun].getSts().getProcessorCount() + " PEs, but you are trying somehow to load pe " + pe;
			throw new RuntimeException(err);
		}


		Deque<TimelineEvent> tl = new ArrayDeque<TimelineEvent>();

		/** Stores all user events from the currently loaded PE/time range. It must be sorted,
		 *  so the nesting of bracketed user events can be efficiently processed.
		 *  */
		Query1D<UserEventObject> userEvents= new RangeQueryTree<UserEventObject>();

		Query1D<EntryMethodObject> perPEObjects = new RangeQueryTree<EntryMethodObject>();


		try {
			if (MainWindow.runObject[myRun].hasLogData()) {
				MainWindow.runObject[myRun].logLoader.createtimeline(pe, startTime, endTime, tl, userEvents, minEntryDuration);
			} else {
				System.err.println("Error loading log files!");
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

		allUserEventObjects.put(pe,userEvents);
		allEntryMethodObjects.put(pe,perPEObjects);
		
		//System.out.println("on pe "+pe+": "+tl.size()+" timeline objects");


		long minMemThisPE = Long.MAX_VALUE;
		long maxMemThisPE = 0;

		long minUserSuppliedThisPE = Long.MAX_VALUE;
		long maxUserSuppliedThisPE = 0;


		// process timeline events
		for(TimelineEvent tle : tl) {

			// Construct a list of messages sent by the object	
			ArrayList<TimelineMessage> msgs = null;
			if(tle.MsgsSent!=null && (!skipLoadingMessages()) ){
				msgs = tle.MsgsSent;
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



	/** A little object to help with synchronization of the updates to the memory structures */
	Object memUsageLock = new Object();

	/** Thread safe updating of the memory and user supplied ranges */
	private void getDataSyncSaveMemUsage(long minMemThisPE, long maxMemThisPE, long minUserSuppliedThisPE, long maxUserSuppliedThisPE)  
	{
		synchronized(memUsageLock){
			if(minMemThisPE < minMem)
				minMem = minMemThisPE;
			if(maxMemThisPE > maxMem)
				maxMem = maxMemThisPE;


			if(minUserSuppliedThisPE < minUserSupplied)
				minUserSupplied = minUserSuppliedThisPE;
			if(maxUserSuppliedThisPE > maxUserSupplied)
				maxUserSupplied = maxUserSuppliedThisPE;
		}
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
		Iterator<Query1D<UserEventObject>> iter = allUserEventObjects.values().iterator();
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
		return actualDisplayWidth - 2* getOffset();
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
	private void setOffset(){
		if(viewType == ViewType.VIEW_MINIMAL)
			offset = maxLabelLen()/2;
		else
			offset = 5 + maxLabelLen()/2;

	}

	protected int getOffset() {
		return offset;
	}

	/** return pixel offset for left margin */
	private int leftOffset(){
		return getOffset();
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
			return 15;
		}
	}

	/** Get the height required to draw a single PE's Timeline */
	public int singleTimelineHeight(){
		return topOffset() + totalUserEventRectsHeight() + barheight() + messageSendHeight() + bottomOffset();
	}



	/** get the height of the nested stack of user events */
	protected int totalUserEventRectsHeight(){
		return singleUserEventRectHeight() * getNumUserEventRows();
	}

	/** get the height of each user event rectangle */
	protected int singleUserEventRectHeight(){
		if(useCompactView())
			return 0;
		else if (this.drawNestedUserEventRows)
			return 12;
		else
			return 7;	
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
	long screenToTime(int xPixelCoord){
		double fractionAlongAxis = ((double) (xPixelCoord-leftOffset())) /
		((double)(mostRecentScaledScreenWidth-2* getOffset()));

		return Math.round(startTime + fractionAlongAxis*(endTime-startTime));	
	}



	/** Convert time to screen coordinate, The returned pixel is the central pixel for this time if a microsecond is longer than one pixel
	 * 
	 * @note requires that mostRecentScaledScreenWidth be correct prior to invocation,
	 * so you should call  scaledScreenWidth(int actualDisplayWidth) before this
	 */
	protected int timeToScreenPixelRight(double time) {
		double fractionAlongTimeAxis =  ((time+0.5-startTime)) /((endTime-startTime));
		return getOffset() + (int)Math.floor(fractionAlongTimeAxis*(mostRecentScaledScreenWidth-2* getOffset()));
	}

	/** Convert time to screen coordinate, The returned pixel is the leftmost pixel for this time if a microsecond is longer than one pixel
	 * 
	 * @note requires that mostRecentScaledScreenWidth be correct prior to invocation,
	 * so you should call  scaledScreenWidth(int actualDisplayWidth) before this
	 */
	protected int timeToScreenPixelLeft(double time) {
		double fractionAlongTimeAxis =  ((time-0.5-startTime)) /((endTime-startTime));
		return getOffset() + (int)Math.ceil(fractionAlongTimeAxis*(mostRecentScaledScreenWidth-2* getOffset()));
	}


	/** Convert time to screen coordinate, The returned pixel is the central pixel for this time if a microsecond is longer than one pixel */
	protected int timeToScreenPixel(double time) {
		double fractionAlongTimeAxis =  ((time-startTime)) /((endTime-startTime));
		return getOffset() + (int)(fractionAlongTimeAxis*(mostRecentScaledScreenWidth-2* getOffset()));
	}

	/** Convert time to screen coordinate, The returned pixel is the central pixel for this time if a microsecond is longer than one pixel */
	protected int timeToScreenPixel(double time, int assumedScreenWidth) {
		double fractionAlongTimeAxis =  ((time-startTime)) /((endTime-startTime));
		return getOffset() + (int)(fractionAlongTimeAxis*(assumedScreenWidth-2* getOffset()));
	}

	/** Convert time to screen coordinate, The returned pixel is the leftmost pixel for this time if a microsecond is longer than one pixel */
	protected int timeToScreenPixelLeft(double time, int assumedScreenWidth) {
		double fractionAlongTimeAxis =  ((time-0.5-startTime)) /((endTime-startTime));
		return getOffset() + (int)Math.ceil(fractionAlongTimeAxis*(assumedScreenWidth-2* getOffset()));
	}

	/** Convert time to screen coordinate, The returned pixel is the rightmost pixel for this time if a microsecond is longer than one pixel */
	protected int timeToScreenPixelRight(double time, int assumedScreenWidth) {
		double fractionAlongTimeAxis =  ( (time+0.5-startTime)) /((endTime-startTime));
		return getOffset() + (int)Math.floor(fractionAlongTimeAxis*(assumedScreenWidth-2* getOffset()));
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

	/** The height of the rectangle that indicates the entry
	 *  method is a msg recv on communication thread */
	protected int smpMessageRecvBarHeight(){
		if(viewType == ViewType.VIEW_NORMAL) {
			return 3;
		}else if(viewType == ViewType.VIEW_COMPACT){
			return 2;
		}else
			return 0;
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
	
	/** Highlight the message on critical path */
    private boolean traceCriticalPathOnHover;

	/** Highlight the message links forward from the object upon mouseover */
	private boolean traceMessagesForwardOnHover;

	/** Highlight the other entry method invocations upon mouseover */
	private boolean traceOIDOnHover;

	/** Forward tracing by one step when left-clicking on an entry method */
	private boolean traceMessagesForwardOnClick;

	/** Forward tracing by one step when left-clicking on an entry method */
	private boolean traceCriticalPathOnClick;

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

	ColorScheme colorSchemeForUserSupplied;

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

	protected boolean traceCriticalPathOnHover() {
		return traceCriticalPathOnHover;
	}
	//	protected boolean traceMessagesForwardOnClick() {
	//		return traceMessagesForwardOnClick;
	//	}

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

	public void setTraceCriticalPathOnHover(boolean traceCriticalPathOnHover) {
    
        this.traceCriticalPathOnHover = traceCriticalPathOnHover;

		if(traceCriticalPathOnHover)
			setToolTipDelayLarge();
		else
			setToolTipDelaySmall();

    }
	
    public void setTraceCriticalPathOnClick(boolean traceCriticalPathOnClick) {
		this.traceCriticalPathOnClick = traceCriticalPathOnClick;
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

	protected void restoreHighlights() {
		clearObjectHighlights();
		displayMustBeRepainted();
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

	public void setColorByUserSupplied(ColorScheme colorScheme) {
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


	public void setColorByUserSuppliedAndObjID(ColorScheme colorScheme) {
		colorSchemeForUserSupplied=colorScheme;
		colorByUserSupplied=true;
		colorByObjectId = true;
		colorByMemoryUsage=false;
		colorByEntryId = false;
		colorByEntryIdFreq = false;
		displayMustBeRepainted();
	}

	public void setColorByUserSuppliedAndEID(ColorScheme colorScheme) {
		colorSchemeForUserSupplied=colorScheme;
		colorByUserSupplied=true;
		colorByObjectId = false;
		colorByMemoryUsage=false;
		colorByEntryId = true;
		colorByEntryIdFreq = false;
		displayMustBeRepainted();
	}

	protected void setColorByEID() {
		colorByUserSupplied = false;
		colorByObjectId = false;
		colorByMemoryUsage = false;
		colorByEntryId = true;
		colorByEntryIdFreq = false;
		displayMustBeRepainted();
	}
	protected void setColorByEIDFreq() {
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
		System.out.println("The fix tachyons feature is still experimental. It may not work well if new processors are loaded, or ranges are changed");

		TachyonShifts tachyonShifts = MainWindow.runObject[myRun].tachyonShifts;
		
		synchronized(this){
			int numIterations = numPEs();
			long threshold_us = 10;

			System.out.println("Executing at most " + numIterations + " times or until no tachyons longer than " + threshold_us + "us are found");

			for(int iteration = 0; iteration < numIterations; iteration++) {
				long largestShift = 0;
                long largePe = -1;
				for(Entry<Integer, Query1D<EntryMethodObject> > e: allEntryMethodObjects.entrySet()){
					// For all PEs
					Integer pe = e.getKey();
					Query1D<EntryMethodObject> objs = e.getValue();

					long minLatency = Integer.MAX_VALUE;

					// Iterate through all entry methods, and compare their execution times to the message send times
					for(EntryMethodObject obj : objs){
						TimelineMessage m = obj.creationMessage();
						if(m!=null){
							long sendTime = m.Time;
							long executeTime = obj.getBeginTime();
							long latency = executeTime - sendTime;

							if(minLatency > latency ){
								minLatency = latency;
							}
						}
					}

					// Shift all events on the pe if it is lagging behind	
					long shift = -1*minLatency;
					if(shift > threshold_us){
//						System.out.println("Shifting Processor " + pe + " by " + shift + "us");
						allEntryMethodObjects.get(pe).shiftAllEntriesBy(shift);
						allUserEventObjects.get(pe).shiftAllEntriesBy(shift);
						tachyonShifts.accumulateTachyonShifts(shift, pe);
					    largePe = pe;
                    }
					if(shift > largestShift)
						largestShift = shift;

				}

				System.out.println("Tachyons: iteration " + iteration  + " largestShift= " + largestShift + " large PE= " + largePe);

				if(largestShift <= threshold_us) {
					System.out.println("No tachyons go back further than "+largestShift+" us");
					break;
				}

			}
		}
		tachyonShifts.writeTachyonShiftMap();
	}
	
	
	/** find the duration of the longest tachyon present in this data */
	public long findLargestTachyon(){
		long minLatency = Integer.MAX_VALUE;

		for(Entry<Integer, Query1D<EntryMethodObject> > e: allEntryMethodObjects.entrySet()){
			// For all PEs
			Integer pe = e.getKey();
			Query1D<EntryMethodObject> objs = e.getValue();

			// Iterate through all entry methods, and compare their execution times to the message send times
			for(EntryMethodObject obj : objs){
				TimelineMessage m = obj.creationMessage();
				if(m!=null){
					long sendTime = m.Time;
					long executeTime = obj.getBeginTime();
					long latency = executeTime - sendTime;

					if(minLatency > latency ){
						minLatency = latency;
					}
				}
			}
		}

		return -1*minLatency;
	}

	
	
	public void setViewType(ViewType vt) {
		viewType = vt;
		setOffset();
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

	private boolean useCompactView() {
		return (viewType != ViewType.VIEW_NORMAL);
	}



	/** Determines which vertical position represents PE */
	private int whichTimelineVerticalPosition(int PE) {
		if(peToLine==null){
			throw new RuntimeException("peToLine is null");
		}

		if(!peToLine.contains(PE)){
			throw new RuntimeException("peToLine does not contain pe " + PE);
		}
		return peToLine.indexOf(PE);
	}


	/** Determines the PE for a given vertical position */
	protected int whichPE(Integer verticalPosition) {
		if(verticalPosition < this.numPs() && verticalPosition >= 0){
			return peToLine.get(verticalPosition);
		} else {
			return -1;
		}
	}


	protected void movePEToLine(int PE, int newPos){
		if (newPos < peToLine.size()) {
			Integer p = Integer.valueOf(PE);
			peToLine.remove(p);
			peToLine.add(newPos, p);
		}
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


		for (Query1D<UserEventObject> query : allUserEventObjects.values()) {
			for (UserEventObject obj : query) {
				if (obj.type == UserEventObject.Type.PAIR) {
					long BeginTime = obj.beginTime;
					long EndTime = obj.endTime;
					Integer UserEventID = obj.userEventID;

					long duration = EndTime - BeginTime;

					if (!min.containsKey(UserEventID)) {
						min.put(UserEventID, duration);
						max.put(UserEventID, duration);
						total.put(UserEventID, duration);
						count.put(UserEventID, 1L);
						name.put(UserEventID, obj.getName());
					} else {
						if (min.get(UserEventID) > duration) {
							min.put(UserEventID, duration);
						}

						if (max.get(UserEventID) < duration) {
							max.put(UserEventID, duration);
						}

						total.put(UserEventID, total.get(UserEventID) + duration);
						count.put(UserEventID, count.get(UserEventID) + 1);
					}
				}
			}
		}

		for (int UserEventID : min.keySet()) {
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

	// Determine the total number of nested threads (e.g. virtual AMPI ranks)
	private int determineNumNestedIDs(){
		int numNestedIDs = 0;

		for (Collection<UserEventObject> c : allUserEventObjects.values()) {
			for (UserEventObject obj : c)
				numNestedIDs = Math.max(obj.getNestedID(), numNestedIDs);
		}
		return numNestedIDs+1;
	}

	// Determine how many rows are needed for displaying nested bracketed user
	// events, in case nested threads are used (e.g. virtual AMPI ranks).
	private int determineUserEventNestingsWithNestedIDs(){
		int nestedIDs_per_pe = (int)Math.ceil(numNestedIDs / numPEs());

		for (Collection<UserEventObject> c : allUserEventObjects.values()) {
			for (UserEventObject obj : c) {
				// Notify this event of its depth in the stack, relative to
				// the PE it is assigned to.
				obj.setNestedRow(obj.getNestedID() % nestedIDs_per_pe);
			}
		}

		return nestedIDs_per_pe;
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
				if(obj.type == UserEventObject.Type.PAIR){
					long BeginTime = obj.beginTime;
					long EndTime = obj.endTime;

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
			if (numNestedIDs > 0)
				setNumUserEventRows(determineUserEventNestingsWithNestedIDs());
			else
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

	/** What type of entity is rendered at a y-pixel offset from the top of a single PE's timeline? 
	 * 
	 *  TODO: fix this for nested user events
	 * 
	 * */
	protected RepresentedEntity representedAtPixelYOffsetInRow(int y){
		if(y < topOffset())
			return RepresentedEntity.NOTHING;
		else if(y<(topOffset() + totalUserEventRectsHeight()))
			return RepresentedEntity.USER_EVENT;
		else if(y < (topOffset() + totalUserEventRectsHeight()+entryMethodLocationHeight() ))
			return RepresentedEntity.ENTRY_METHOD;
		else 
			return RepresentedEntity.NOTHING;
	}

	public enum RepresentedEntity {
		ENTRY_METHOD, USER_EVENT, NOTHING
	}


	/** The pixel offset for the top of the entry method from the top of a single PE's timeline */
	protected int entryMethodLocationTop(int pe) {
		int yidx = whichTimelineVerticalPosition(pe);
		return singleTimelineHeight()*yidx + topOffset() + totalUserEventRectsHeight();
	}


	/** The pixel y-coordinate for the topmost pixel for a PE's timeline */
	protected int peTopPixel(int pe) {
		int yidx = whichTimelineVerticalPosition(pe);
		return singleTimelineHeight()*yidx + topOffset();
	}

	/** The pixel y-coordinate for the bottommost pixel for a PE's timeline */
	protected int peBottomPixel(int pe) {
		int yidx = whichTimelineVerticalPosition(pe);
		return singleTimelineHeight()*(yidx+1) + topOffset() - 1;
	}

	/** Which pe row is at pixel y coordinate. The result corresponds to an index into peToLine. */
	protected int rowForPixel(int y){
		return (y - topOffset()) / singleTimelineHeight();
	}

	/** Returns a list of processors that are rendered within the specified range of y pixel coordinates */
	Collection<Integer> processorsInPixelYRange(int y1, int y2){
		int r1 = rowForPixel(y1);
		int r2 = rowForPixel(y2);

		if (r1 < 0) { r1 = 0; }
		if (r2 >= peToLine.size() - 1) { r2 = peToLine.size() - 1; }
		return peToLine.subList(r1, r2 + 1);
	}


	/** The pixel height of the entry method object. This includes just the rectangular region and the descending message sends */
	protected int entryMethodLocationHeight() {
		return barheight()+messageSendHeight();
	}

	protected int userEventLocationTop(int pe) {
		int yidx = whichTimelineVerticalPosition(pe);
		return singleTimelineHeight()*yidx + topOffset();
	}

	protected int userEventLocationBottom(int pe) {
		return userEventLocationTop(pe) + totalUserEventRectsHeight();
	}

	protected int horizontalLineLocationTop(int i) {
		return singleTimelineHeight()*i + topOffset() + totalUserEventRectsHeight() + (barheight()/2);		
	}

	/** The message send tick mark bottom point*/
	protected int messageSendLocationY(int pe) {
		int yidx = whichTimelineVerticalPosition(pe);
		return singleTimelineHeight()*yidx + topOffset() + totalUserEventRectsHeight() + barheight()+this.messageSendHeight();
	}

	/** The message send tick mark bottom point*/
	protected int messageRecvLocationY(int pe) {
		int yidx = whichTimelineVerticalPosition(pe);
		return singleTimelineHeight()*yidx + topOffset() + totalUserEventRectsHeight();
	}

    protected void addNeighbors(Integer pe)
    {
        SortedSet<Integer> processorList = MainWindow.runObject[myRun].getValidProcessorList();
        int intpe = pe.intValue();
        for(int i=-4; i<4;i++)
        {
            int newPE = intpe+i;
            if(processorList.contains(newPE))
                addProcessor(i+intpe);
        }
    }


	protected void dropPEsUnrelatedToPE(Integer pe) {
		dropPEsUnrelatedToObjects(allEntryMethodObjects.get(pe));
	}

	protected void dropPEsUnrelatedToObject(EntryMethodObject obj) {
		MainWindow.performanceLogger.log(Level.INFO,"dropPEsUnrelatedToObject()");
		HashSet<EntryMethodObject> set = new HashSet<EntryMethodObject>();
		set.add(obj);
		dropPEsUnrelatedToObjects(set);
	}



	private void dropPEsUnrelatedToObjects(Collection<EntryMethodObject> objs) {
		synchronized(this) {
			MainWindow.performanceLogger.log(Level.INFO,"dropPEsUnrelatedToObjects()");
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

			for (EntryMethodObject method : allRelatedEntries) {
				relatedPEs.add(method.pe);
			}

			peToLine.retainAll(relatedPEs);
			modificationHandler.notifyProcessorListHasChanged();
			displayMustBeRedrawn();
		}
	}


	/** Produce a map containing the ids and nicely mangled string names for each entry method */
	public Map<Integer, String> getEntryNames() {
		return MainWindow.runObject[myRun].getSts().getPrettyEntryNames();
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




	//	
	//	protected void makeUserSuppliedNotesVisible() {
	//		hideUserSuppliedNotes = false;
	//		this.displayMustBeRedrawn();
	//	}	
	//
	//	protected void makeUserSuppliedNotesInvisible() {
	//		hideUserSuppliedNotes = true;
	//		this.displayMustBeRedrawn();
	//	}

	//	public boolean userSuppliedNotesVisible() {
	//		return ! hideUserSuppliedNotes;	
	//	}

	//	protected boolean userSuppliedNotesHidden() {
	//		return hideUserSuppliedNotes;	
	//	}


	protected boolean entryIsHiddenID(Integer id) {
		return hiddenEntryPoints.contains(id);
	}

	public boolean entryIsVisibleID(Integer id) {
		return ! hiddenEntryPoints.contains(id);
	}

	protected boolean userEventIsHiddenID(Integer id) {
		return hiddenUserEvents.contains(id);
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

	protected void displayTopTimes(boolean b){
		displayTopTimes = b;
	}

	private boolean displayTopTimes(){
		return displayTopTimes;
	}

	private int amountTopTimes(){
		return amountTopTimes;
	}

	protected void amountTopTimes(int i){
		amountTopTimes = i;
	}

	protected void displayTopTimesText() {
		if (topTimesText == null)//if the user didn't select to display the top N longest times, then tell them they need to first do that
		{
			JOptionPane.showMessageDialog(null, "You must first choose to display the longest idle and entry methods in the Ranges menu.", "Error", 				JOptionPane.ERROR_MESSAGE); 
		}
		else
		{
			JFrame frame = new JFrame("Top " + amountTopTimes() + " Idle and Entry Times");
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			JLabel lbl = new JLabel(topTimesText);
			JPanel pnl = new JPanel();
			pnl.add(lbl);
			JScrollPane scp = new JScrollPane(pnl, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scp.getVerticalScrollBar().setUnitIncrement(16);
			frame.add(scp);
			frame.setSize(500,400);
			frame.setVisible(true);
		}

	}

	private void pruneOutMessages() {
		MainWindow.performanceLogger.log(Level.INFO,"pruneOutMessages");
		Iterator<Integer> iter = allEntryMethodObjects.keySet().iterator();
		while(iter.hasNext()){
			Integer pe = iter.next();
			Query1D<EntryMethodObject> list = allEntryMethodObjects.get(pe);

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

	protected void setFrequencyColors() {
		Analysis a = MainWindow.runObject[myRun];
		a.activityColors = a.colorManager.defaultColorMap();
		a.entryColors = ColorManager.entryColorsByFrequency(ColorManager.createComplementaryColorMap(entries.length), frequencyVector);
		a.userEventColors = a.activityColors[Analysis.USER_EVENTS];
		a.functionColors = a.activityColors[Analysis.FUNCTIONS];
	}
	public ViewType getViewType() {
		return viewType;
	}

	public void colorsHaveChanged() {
		displayMustBeRepainted();
	}

	public int[] getEntriesArray() {
		return entries;
	}

	public boolean hasEntryList() {
		return true;
	}

	public boolean handleIdleOverhead() {
		return true;
	}

    public  EntryMethodObject   getPreviousEntry(EntryMethodObject currentObj, int currentPe) {
        Query1D<EntryMethodObject> objs=null;    
        EntryMethodObject       previous = null;
        Iterator<Entry<Integer, Query1D<EntryMethodObject>>> iter = allEntryMethodObjects.entrySet().iterator();
        while(iter.hasNext()){
            Entry<Integer, Query1D<EntryMethodObject>> e = iter.next();
            Integer pe = e.getKey();
            if( pe.intValue() == currentPe)
            {
                objs = e.getValue();
                for(EntryMethodObject obj : objs) {
                    if(obj.equals(currentObj))
                    {
                        break;
                    }
                    previous = obj;
                }

                break;
            }
        }
        return previous;
    }
}
