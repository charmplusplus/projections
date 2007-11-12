package projections.gui.Timeline;

import java.util.Iterator;
import java.util.Vector;
import javax.swing.*;
import java.awt.Font;
import java.awt.Color;
import java.awt.Dimension;
import projections.analysis.*;
import projections.gui.MainWindow;
import projections.gui.OrderedIntList;
import projections.gui.OrderedUsageList;
import projections.gui.Util;
import projections.misc.*;

public class Data
{
	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;

	private MainHandler modificationHandler = null;


//	/** The portion of the screen dedicated to the visible portion of the main panel */
//	private int screenWidth;


	private float scaleFactor = 1.0f;

	private double preferredViewTime = -1.0;

	/** The most recently known scaled screen width 
	 * 
	 * @note It is likely dangerous to use this unless you 
	 * really know what you are doing
	 * 
	 * */
	private int mostRecentScaledScreenWidth;

	/** The height of the objects drawn in the timeline ? */
	private int barheight;

	private OrderedIntList processorList;


	private OrderedIntList oldplist;

	private String oldpstring;

	// boolean for testing if entries are to be colored by Object ID
	private boolean colorbyObjectId;

	private int[]          entries;

	private Color[]        entryColor;

	public EntryMethodObject[][] tloArray;

	public Vector [] mesgVector;

	public Vector [] oldmesgVector;

	UserEventObject[][] timelineUserEventObjectsArray = null;

	float[] processorUsage;

	float[] idleUsage;

	float[] packUsage;

	OrderedUsageList[] entryUsageList;

	/** The times for the displayed time region */
	private long beginTime; 

	private long endTime;

	/** The old begin time, used to make loading of new ranges more
		efficient if we already have the object data loaded. Then we
		can just take a subset of our data instead of reloading it
		from the logs
	*/
	long oldBT;
	/** The old end time */
	long oldET; 

	boolean showPacks, showIdle, showMsgs;



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

	/** A set of lines representing message sends */
	// TODO: update this to refer to the objects themselves instead, because these won't survive a change in the scale factor or window resize
	public Vector mesgCreateExecVector;
	
	/** The line thickness for the mesg send lines */
	public float messageLineThickness=2.5f;
	
	private boolean useCustomColors=false;
	
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

		processorList = MainWindow.runObject[myRun].getValidProcessorList();

		oldBT = -1;
		oldET = -1;
		oldplist = null;
		oldpstring = null;

		barheight = 20;

		processorUsage = null;
		entryUsageList = null;


		/** The selected time range for which we display the timeline.
		 *  The value is by default the entire range found in the log files
		 *  It is modified by Window when the user enters a new 
		 *  value in the "select ranges" dialog box.
		 */
		beginTime = 0;
		endTime = MainWindow.runObject[myRun].getTotalTime();

		mesgCreateExecVector = new Vector();

		tloArray = null;
		mesgVector = null;
		entries = new int[MainWindow.runObject[myRun].getNumUserEntries()];
		entryColor = MainWindow.runObject[myRun].getColorMap();

		labelFont = new Font("SansSerif", Font.PLAIN, 10); 
		axisFont = new Font("SansSerif", Font.PLAIN, 10);

	}
	/** Currently called from Window:addProcessor() from EntryMethodObject upon clicking to draw the "message sent" 
	 * line when a prcossor is not yet loaded 
	 */
	public void addProcessor(int pCreation){
		oldplist = processorList.copyOf();
		processorList.insert(pCreation);
		displayMustBeRedrawn();
	}

	/** Change the font sizes used */
	public void setFontSizes(int labelFontSize, int axisFontSize, boolean useBoldForLabel){
		if(useBoldForLabel)
			labelFont = new Font("SansSerif", Font.BOLD, labelFontSize); 
		else
			labelFont = new Font("SansSerif", Font.PLAIN, labelFontSize); 

		axisFont = new Font("SansSerif", Font.PLAIN, axisFontSize);
	}

	public int barheight(){
		return barheight;
	}
	public long beginTime(){
		return beginTime;
	}

	public void clearAllLines() {
		if (tloArray != null) {
			for (int i=0;i<tloArray.length;i++) {
				if (tloArray[i] != null)
					for (int j=0;j<tloArray[i].length;j++) {
						if (tloArray[i][j]!= null) {
							tloArray[i][j].clearCreationLine();
						}
					}
			}
		}
		mesgCreateExecVector.clear();
	}
	public boolean colorbyObjectId(){
		return colorbyObjectId;
	}

	/****************** Timeline ******************/
	public Vector createTL(int p, long bt, long et, 
			Vector timelineEvents, Vector userEvents) {
		try {
			if (MainWindow.runObject[myRun].hasLogData()) {
				return MainWindow.runObject[myRun].logLoader.createtimeline(p, bt, et, 
						timelineEvents,
						userEvents);
			} else {
				System.err.println("createTL: No log files available!");
				return null;
			}
		} catch (LogLoadException e) {
			System.err.println("LOG LOAD EXCEPTION");
			return null;
		}
	}
	/** Create the array of timeline objects */
	public void createTLOArray()
	{

		EntryMethodObject[][] oldtloArray = tloArray;
		UserEventObject[][] oldUserEventsArray = timelineUserEventObjectsArray;
		oldmesgVector = mesgVector;
		mesgVector = new Vector[maxPs()+1]; // want an array of size 1 if pe list={0}
		for(int i=0; i<maxPs()+1; i++){
			mesgVector[i] = null;
		}

		tloArray = new EntryMethodObject[processorList.size()][];
		timelineUserEventObjectsArray = new UserEventObject[processorList.size()][];

		
		if (oldtloArray != null && beginTime >= oldBT && endTime <= oldET) {

			int oldp, newp;
			int oldpindex=0, newpindex=0;

			processorList.reset();
			oldplist.reset();

			newp = processorList.nextElement();
			oldp = oldplist.nextElement();
			while (newp != -1) {
				while (oldp != -1 && oldp < newp) {
					oldp = oldplist.nextElement();
					oldpindex++;
				}   
				if (oldp == -1)
					break;
				if (oldp == newp) {
					if (beginTime == oldBT && endTime == oldET) {
						tloArray[newpindex] = oldtloArray[oldpindex];
						assert(oldUserEventsArray != null);
						timelineUserEventObjectsArray[newpindex] = 
							oldUserEventsArray[oldpindex];
						mesgVector[oldp] = oldmesgVector[newp];
					} else {
						// copy timelineobjects from larger array into 
						// smaller array
						int n;
						int oldNumItems = oldtloArray[oldpindex].length;
						int newNumItems = 0;
						int startIndex  = 0;
						int endIndex    = oldNumItems - 1;

						// calculate which part of the old array to copy
						for (n=0; n<oldNumItems; n++) {
							if (oldtloArray[oldpindex][n].getEndTime() < 
									beginTime) { 
								startIndex++; 
							} else { 
								break; 
							}
						}
						for (n=oldNumItems-1; n>=0; n--) {
							if (oldtloArray[oldpindex][n].getBeginTime() > 
							endTime) { 
								endIndex--; 
							} else { 
								break; 
							}
						}
						newNumItems = endIndex - startIndex + 1;

						// copy the array
						tloArray[newpindex] = new EntryMethodObject[newNumItems];
						mesgVector[newp] = new Vector();
						for (n=0; n<newNumItems; n++) {
							tloArray[newpindex][n] = 
								oldtloArray[oldpindex][n+startIndex];
							tloArray[newpindex][n].setUsage();
							tloArray[newpindex][n].setPackUsage();
							for (int j=0;
							j<tloArray[newpindex][n].messages.length;
							j++) {
								mesgVector[newp].addElement(tloArray[newpindex][n].messages[j]);
							}
						}
						// copy user events from larger array into smaller array
						if (oldUserEventsArray != null && 
								oldUserEventsArray[oldpindex] != null) {
							oldNumItems = oldUserEventsArray[oldpindex].length;
							newNumItems = 0;
							startIndex = 0;
							endIndex = oldNumItems -1;

							// calculate which part of the old array to copy
							for (n=0; n<oldNumItems; n++) {
								if (oldUserEventsArray[oldpindex][n].EndTime < 
										beginTime) { 
									startIndex++; 
								} else { 
									break; 
								}
							}
							for (n=oldNumItems-1; n>=0; n--) {
								if (oldUserEventsArray[oldpindex][n].BeginTime >
							endTime) { 
									endIndex--; 
								} else { 
									break; 
								}
							}
							newNumItems = endIndex - startIndex + 1;

							// copy the array
							timelineUserEventObjectsArray[newpindex] = 
								new UserEventObject[newNumItems];
							for (n=0; n<newNumItems; n++) {
								timelineUserEventObjectsArray[newpindex][n] = 
									oldUserEventsArray[oldpindex][startIndex+n];
							}
						}
					}
				}                                       
				newp = processorList.nextElement();
				newpindex++;
			}   
			oldtloArray = null;
			oldUserEventsArray = null;
		}


		int pnum;
		processorList.reset();
		int numPEs = processorList.size();
		ProgressMonitor progressBar = 
			new ProgressMonitor(MainWindow.runObject[myRun].guiRoot, "Reading timeline data",
					"", 0, numPEs);
		progressBar.setProgress(0);
		for (int p=0; p<numPEs; p++) {
			if (!progressBar.isCanceled()) {
				progressBar.setNote(p + " of " + numPEs);
				progressBar.setProgress(p);
			} else {
				break;
			}
			pnum = processorList.nextElement();
			if (tloArray[p] == null) { 
				tloArray[p] = getData(pnum, p);
			}
		}
		progressBar.close();
		for (int e=0; e<MainWindow.runObject[myRun].getNumUserEntries(); e++) {
			entries[e] = 0;
		}
		processorUsage = new float[tloArray.length];
		entryUsageList = new OrderedUsageList[tloArray.length];
		float[] entryUsageArray = new float[MainWindow.runObject[myRun].getNumUserEntries()];
		idleUsage  = new float[tloArray.length];
		packUsage  = new float[tloArray.length];
		for (int p=0; p<tloArray.length; p++) {
			processorUsage[p] = 0;
			idleUsage[p] = 0;
			packUsage[p] = 0;
			for (int i=0; i<MainWindow.runObject[myRun].getNumUserEntries(); i++) {
				entryUsageArray[i] = 0;
			}
			for (int n=0; n<tloArray[p].length; n++) {
				float usage = tloArray[p][n].getUsage();
				int entrynum = tloArray[p][n].getEntry();
				
				if (entrynum >=0) {
					entries[entrynum]++;
					processorUsage[p] += usage;
					packUsage[p] += tloArray[p][n].getPackUsage();
					entryUsageArray[entrynum] += tloArray[p][n].getNetUsage();
				} else {
					idleUsage[p] += usage;
				}
			}
			
			entryUsageList[p] = new OrderedUsageList();
			for (int i=0; i<MainWindow.runObject[myRun].getNumUserEntries(); i++) {
				if (entryUsageArray[i] > 0) {
					entryUsageList[p].insert(entryUsageArray[i], i);
				}
			}      
		} 
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

	/** Add or Remove a new line to the visualization representing the sending of a message */
	public void toggleConnectingLine(int pCreation,long creationtime,
			int pCurrent,long executiontime,
			EntryMethodObject objCurrent) {

		
		// Search to see if we already have this line or not
		// If we find the line, we just remove it and return
		Iterator iter = mesgCreateExecVector.iterator();		
		while(iter.hasNext()) {
			Line line = (Line ) iter.next();
			if (line.pCurrent == pCurrent && line.executiontime == executiontime) {
				mesgCreateExecVector.remove(line);
				displayMustBeRepainted();
				return;
			}
		}
		
		// We didn't find the line yet, so add it
		
		Line line = new Line(pCreation,pCurrent,objCurrent,
				creationtime,executiontime);
		mesgCreateExecVector.add(line);
				
		displayMustBeRepainted();
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

	// index into userEventArray
	private EntryMethodObject[] getData(int pnum, int index)  
	{

		Vector tl, msglist, packlist;
		TimelineEvent tle;

		int numItems;
		int numMsgs, numpacks;
		tl = new Vector();
		Vector userEvents = new Vector();
		mesgVector[pnum] = new Vector();
		createTL(pnum, beginTime, endTime, tl, userEvents);
		// proc userEvents
		int numUserEvents = userEvents.size();
		if (numUserEvents > 0) {
			timelineUserEventObjectsArray[index] = new UserEventObject[numUserEvents];
			for (int i=0; i<numUserEvents; i++) {
				timelineUserEventObjectsArray[index][i] = 
					(UserEventObject) userEvents.elementAt(i);
			}
		} else { 
			// probably already numm
			timelineUserEventObjectsArray[index] = null; 
		} 

		// proc timeline events
		numItems = tl.size();   
		EntryMethodObject[] tlo = new EntryMethodObject[numItems];
		for (int i=0; i<numItems; i++) {
			tle   = (TimelineEvent)tl.elementAt(i);
			msglist = tle.MsgsSent;
			if (msglist == null) {
				numMsgs = 0;
			} else {
				numMsgs = msglist.size();
			}

			TimelineMessage[] msgs = new TimelineMessage[numMsgs];
			for (int m=0; m<numMsgs; m++) {
				assert(msglist != null);
				msgs[m] = (TimelineMessage)msglist.elementAt(m);
				mesgVector[pnum].addElement(msglist.elementAt(m));
			}	
			packlist = tle.PackTimes;
			if (packlist == null) {
				numpacks = 0;
			} else {
				numpacks = packlist.size();
			}
			PackTime[] packs = new PackTime[numpacks];
			for (int p=0; p<numpacks; p++) {
				assert(packlist!=null);
				packs[p] = (PackTime)packlist.elementAt(p);
			}
			tlo[i] = new EntryMethodObject(this, tle, msgs, packs, pnum);
		}
		return tlo;
	}
	public Color getForegroundColor(){
		if(useCustomColors)
			return customForeground;
		else
			return MainWindow.runObject[myRun].foreground;
	}


	public int getNumUserEvents() {
		if (timelineUserEventObjectsArray == null) { return 0; }
		int num = 0;
		for (int i=0; i<timelineUserEventObjectsArray.length; i++) {
			if (timelineUserEventObjectsArray[i] != null) { num += timelineUserEventObjectsArray[i].length; }
		}
		return num;
	}

	public float getScaleFactor(){
		return scaleFactor;
	}

	public void increaseScaleFactor(){
		setScaleFactor( (float) ((int) (getScaleFactor() * 4) + 1) / 4 );
	}
	public int 	labelIncrement(int actualDisplayWidth) {
		return  (int) Util.getBestIncrement((int)(Math.ceil(maxLabelLen() / pixelIncrement(actualDisplayWidth))));
	}

//	public Window timelineWindow;

	/**	 the width of the timeline portion that is drawn(fit so that labels are onscreen) */
	public int lineWidth(int actualDisplayWidth) {
		return actualDisplayWidth - 2*offset();
	}

	public int maxLabelLen(){
		return 70;
	}   

	/** Determine the number of ticks we can display on the timeline in the given sized window */
	public int numIntervals(int actualDisplayWidth){
		return (int) Math.ceil(totalTime() / timeIncrement(actualDisplayWidth)) + 1;
	}   

	/** Number of processors in the processor List */
	public int numPs(){
		return processorList.size();
	}
	/** The maximum processor index in the processor list, or -1 if null processor list */
	public int maxPs(){
		int largestSeen = -1;

		if(processorList!=null){
			processorList.reset();	
			while(processorList.hasMoreElements()){
				int p = processorList.nextElement();
				if (p>largestSeen){
					largestSeen=p; 
				}
			}
		}

		return largestSeen;
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


	public OrderedIntList oldplist(){
		return oldplist;	
	}



	public String oldpstring(){
		return oldpstring;
	}

	public double pixelIncrement(int actualDisplayWidth){
		return  (double) lineWidth(actualDisplayWidth) / (double)numIntervals(actualDisplayWidth);
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


	/** Get the height required to draw a single PE's Timeline */
	public int singleTimelineHeight(){
		if(useMinimalView())
			return barheight + 4;
		else
			return barheight + 20;
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

		// Record the old time range
		this.oldBT = this.beginTime;
		this.oldET = this.endTime;

		this.beginTime = beginTime;
		this.endTime = endTime;

		if (processorList == null) {
			oldplist = null;
		} else {
			oldplist = processorList.copyOf();
		}

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

	/** Some time measure per pixel ??? */
	public int timeIncrement(int actualDisplayWidth){
		return Util.getBestIncrement( (int) Math.ceil(5 / ( (double) lineWidth(actualDisplayWidth) / totalTime() )  ) );
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
	public double screenToTime(int xPixelCoord){
		double fractionAlongAxis = ((double) (xPixelCoord-leftOffset())) /
		((double)(mostRecentScaledScreenWidth-2*offset()));

		return beginTime + fractionAlongAxis*(endTime-beginTime);	
	}

	/** Convert time to screen coordinate
	 * 
	 * @note requires that mostRecentScaledScreenWidth be correct prior to invocation,
	 * so you should call  scaledScreenWidth(int actualDisplayWidth) before this
	 */
	public int timeToScreenPixel(double startTime) {
		double fractionAlongTimeAxis =  ((double) (startTime-beginTime)) /((double)(endTime-beginTime));
		return offset() + (int)(fractionAlongTimeAxis*(double)(mostRecentScaledScreenWidth-2*offset()));
	}

	public int timeToScreenPixel(double startTime, int assumedScreenWidth) {
		double fractionAlongTimeAxis =  ((double) (startTime-beginTime)) /((double)(endTime-beginTime));
		return offset() + (int)(fractionAlongTimeAxis*(double)(assumedScreenWidth-2*offset()));
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


}
