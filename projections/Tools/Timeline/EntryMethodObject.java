package projections.Tools.Timeline;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import projections.analysis.AmpiFunctionData;
import projections.analysis.ObjectId;
import projections.analysis.PackTime;
import projections.analysis.TimelineEvent;
import projections.gui.MainWindow;
import projections.gui.U;

public class EntryMethodObject extends JComponent implements Comparable, MouseListener, ActionListener
{

	private MessageWindow msgwindow;
	private long beginTime, endTime, recvTime;
	private long cpuTime;
	private long cpuBegin, cpuEnd;
	private int entry;
	private int entryIndex;
	private int msglen;
	int EventID;
	private ObjectId tid; 
	int pCurrent;
	int pCreation;
	
	
	final static String popupChangeColor = "Change Entry Point Color";
	final static String popupShowDetails = "Show details";
	final static String popupTraceSender = "Trace message to sender";
	final static String popupDropPEsForObject = "Drop all PEs unrelated to this entry method";
	final static String popupDropPEsForPE = "Drop all PEs unrelated to entry methods on this PE";
	
	
	/** Data specified by the user, likely a timestep. Null if nonspecified */
	Integer userSuppliedData;
	
	/** Memory usage at some point in this entry method. Null if nonspecified */
	Integer memoryUsage;
	
	/** The duration of the visible portion of this event */
	private double  usage;
	private float packusage;
	private long packtime;
	
	
	/** Pixel coordinate of left side of object */
	private int leftCoord=0;

	private int rightCoord=0;
	
	
	private String tleUserEventName;


	private Data data = null;
	
	/** A set of TimelineMessage's */
	public Set<TimelineMessage> messages;
	
	private PackTime[] packs;

	private int numPapiCounts = 0;
	private long papiCounts[];

	private boolean isFunction = false;

	private static DecimalFormat format_ = new DecimalFormat();
	private AmpiFunctionData funcData[];

	public EntryMethodObject(Data data,  TimelineEvent tle, 
			TreeSet<TimelineMessage> msgs, PackTime[] packs,
			int p1)
	{
		setFocusable(false); // optimization for speed
		setVisible(true);
		setOpaque(false);

		
		setBackground(MainWindow.runObject[data.myRun].background);
		setForeground(MainWindow.runObject[data.myRun].foreground);
		
		this.data = data;
		beginTime = tle.BeginTime;
		endTime   = tle.EndTime;
		cpuBegin  = tle.cpuBegin;
		cpuEnd    = tle.cpuEnd;
		cpuTime   = cpuEnd - cpuBegin;
		entry     = tle.EntryPoint;
		entryIndex = MainWindow.runObject[data.myRun].getEntryIndex(entry);
		messages  = msgs; // Set of TimelineMessage
		this.packs= packs;
		pCurrent  = p1;
		pCreation = tle.SrcPe;
		EventID = tle.EventID;
		msglen = tle.MsgLen;
		recvTime = tle.RecvTime;
		if (tle.id != null) {
			tid = new ObjectId(tle.id);
		} else {
			tid = new ObjectId();
		}
		userSuppliedData = tle.UserSpecifiedData;
		memoryUsage = tle.memoryUsage;
		
		tleUserEventName = tle.userEventName;

		numPapiCounts = tle.numPapiCounts;
		papiCounts    = tle.papiCounts;

		isFunction = tle.isFunction;

		format_.setGroupingUsed(true);
	
		setUsage();
		setPackUsage();

		if (isFunction) {
			// copy the callstack	
			funcData = new AmpiFunctionData[tle.callStack.size()];
			tle.callStack.copyInto(funcData);
					
		} else if (tle.EntryPoint >= 0) {
		}
		
		updateToolTipText();
		
		if(!isIdleEvent()){
			addMouseListener(this);
		}
					
	} 
	
	
	/** Set the tooltip to a nicely formatted representation of this object */
	public void updateToolTipText(){

		// Construct a nice informative html formatted string about this entry method object. 
		// This string is displayed on mouseover(by setting it as this component's tooltip)
		String infoString = "";

		// **CW** special treatment for functions. There really should
		// be a general way of dealing with this.
		if (isFunction) {			
			infoString += "<i>Function</i>: " + MainWindow.runObject[data.myRun].getFunctionName(entry) + "<br>";
			infoString += "<i>Begin Time</i>: " + format_.format(beginTime) + "<br>";
			infoString += "<i>End Time</i>: " + format_.format(endTime) + "<br>";
			infoString += "<i>Total Time</i>: " + U.humanReadableString(endTime-beginTime) + "<br>";
			infoString += "<i>Msgs created</i>: " + messages.size() + "<br>";
			infoString += "<i>Id</i>: " + tid.id[0] + ":" + tid.id[1] + ":" + tid.id[2] + "<br>";
			infoString += "<hr><br><i>Function Callstack</i>:<br>";

			// look at the call stack
			for(int i=0;i<funcData.length;i++){
				AmpiFunctionData functionData = funcData[i];
				infoString += "<i>[Func]</i>: " + MainWindow.runObject[data.myRun].getFunctionName(functionData.FunctionID) + "<br>";
				infoString += "&nbsp&nbps&nbsp&nbps<i>line</i>:" + functionData.LineNo + " <i>file</i>: " + functionData.sourceFileName + "<br>";
			}
		} else if (entry >= 0) {

			infoString += "<b>" + MainWindow.runObject[data.myRun].getEntryFullNameByID(entry) + "</b><br><br>"; 

			if(msglen > 0) {
				infoString += "<i>Msg Len</i>: " + msglen + "<br>";
			}
			
			infoString +=  "<i>Begin Time</i>: " + format_.format(beginTime);
			if (cpuTime > 0) 
				infoString +=  " (" + format_.format(cpuBegin) + ")";
			infoString += "<br>";
			
			infoString +=  "<i>End Time</i>: " + format_.format(endTime) ;
			if (cpuTime > 0)
				infoString +=  " (" + format_.format(cpuEnd) + ")";
			infoString += "<br>";
			
			infoString +=  "<i>Total Time</i>: " + U.humanReadableString(endTime-beginTime);
			if (cpuTime > 0)
				infoString +=  " (" + U.humanReadableString(cpuTime) + ")";
			infoString += "<br>";
			
			infoString +=  "<i>Packing</i>: " + U.humanReadableString(packtime);
			if (packtime > 0)
				infoString +=  " (" + (100*(float)packtime/(endTime-beginTime+1)) + "%)";
			infoString += "<br>";
			
			infoString += "<i>Msgs created</i>: " + messages.size() + "<br>";
			infoString += "<i>Created by processor</i>: " + pCreation + "<br>";
			infoString += "<i>Id</i>: " + tid.id[0] + ":" + tid.id[1] + ":" + tid.id[2] + "<br>";
			if(tleUserEventName!=null)
				infoString += "<i>Associated User Event</i>: "+tleUserEventName+ "<br>";
			
			if(recvTime > 0){
				infoString += "<i>Recv Time</i>: " + recvTime + "<br>";
			}	
			
			if (numPapiCounts > 0) {
				infoString += "<i>*** PAPI counts ***</i>" + "<br>";
				for (int i=0; i<numPapiCounts; i++) {
					infoString += MainWindow.runObject[data.myRun].getPerfCountNames()[i] + " = " + format_.format(papiCounts[i]) + "<br>";
				}
			}
		} else if (entry == -1) {
			infoString += "<b>Idle Time</b><br><br>";
			infoString += "<i>Begin Time</i>: " + format_.format(beginTime)+ "<br>";
			infoString += "<i>End Time</i>: " + format_.format(endTime) + "<br>";
			infoString += "<i>Total Time</i>: " + U.humanReadableString(endTime-beginTime) + "<br>";
		} else if (entry == -2) {
			infoString += "<i>Unaccounted Time</i>" + "<br>";
			
			infoString +=  "<i>Begin Time</i>: " + format_.format(beginTime);
			if (cpuTime > 0) 
				infoString +=  " (" + format_.format(cpuBegin) + ")";
			infoString += "<br>";
			
			infoString +=  "<i>End Time</i>: " + format_.format(endTime);
			if (cpuTime > 0) 
				infoString +=  " (" + format_.format(cpuEnd) + ")";
			infoString += "<br>";
			
			infoString +=  "<i>Total Time</i>: " + U.humanReadableString(endTime-beginTime);
			if (cpuTime > 0) 
				infoString +=  " (" + (cpuTime) + ")";
			infoString += "<br>";
			
			infoString +=  "<i>Packing</i>: " + U.humanReadableString(packtime);
			if (packtime > 0) 
				infoString +=  " (" + (100*(float)packtime/(endTime-beginTime+1)) + "%)";
			infoString += "<br>";
			
			infoString += "<i>Num Msgs created</i>: " + messages.size() + "<br>";
		}

		if(userSuppliedData != null){
			infoString += "<i>User Supplied Parameter(timestep):</i> " + userSuppliedData.intValue() + "<br>";
		}
			
		if(memoryUsage != null){
			infoString += "<i>Memory Usage:</i> " + memoryUsage.intValue()/1024/1024 + " MB<br>";
		}
		
		setToolTipText("<html><body>" + infoString + "</html></body>");
		
	}
	

	/** paint an entry method that tapers to a point at its left side */
	private void drawLeftArrow(Graphics g, Color c, int startY, int h)
	{
		int[] xpts = {5, 0, 5};
		int[] ypts = {startY, startY+h/2, startY+h-1};

		g.setColor(c);
		g.fillPolygon(xpts, ypts, 3);

		g.setColor(c.brighter());
		g.drawLine(xpts[0], ypts[0], xpts[1], ypts[1]);

		g.setColor(c.darker());
		g.drawLine(xpts[1], ypts[1], xpts[2], ypts[2]);   
	}   
	
	/** paint an entry method that tapers to a point at its right side */
	private void drawRightArrow(Graphics g, Color c, int startY, int h, int right)
	{
		int[] xpts = {right-6, right, right-6};
		int[] ypts = {startY, startY+h/2, startY+h-1};

		g.setColor(c);
		g.fillPolygon(xpts, ypts, 3);

		g.setColor(c.brighter());
		g.drawLine(xpts[0], ypts[0], xpts[1], ypts[1]);

		g.setColor(c.darker());
		g.drawLine(xpts[1], ypts[1], xpts[2], ypts[2]);
	}   

	public long getBeginTime()
	{
		return beginTime;
	}   

	public long getEndTime()
	{
		return endTime;
	}   

	public int getEntryID()
	{
		return entry;
	}   

	public int getEntryIndex()
	{
		return MainWindow.runObject[data.myRun].getEntryIndex(entry);
	}   
	
	/** Return a set of messages for this entry method */
	public Set<TimelineMessage> getMessages()
	{
		return messages;
	}   

	public Dimension getMinimumSize()
	{
		return new Dimension(getSize().width, getSize().height);
	}   

	
	public float getNonPackUsage()
	{
		return (float)usage - packusage;
	}   

	public int getNumMsgs()
	{
		if(messages == null)
			return 0;
		else
			return messages.size();
	}   

	public float getPackUsage()
	{
		return packusage;
	}   

	public int getPCreation()
	{
		return pCreation;
	}   

	public int getPCurrent()
	{
		return pCurrent;
	}   

	public float getUsage()
	{
		return (float)usage;
	}   

	
	public void mouseClicked(MouseEvent evt)
	{
		if (entry >= 0) {
			if (evt.getModifiers()==MouseEvent.BUTTON1_MASK) {
				// Left Click
				data.clickTraceSender(this);	
			} else {	
				// non-left click: display popup menu
				JPopupMenu popup = new JPopupMenu();
				JMenuItem menuItem;
		        
				menuItem = new JMenuItem(popupShowDetails);
				menuItem.addActionListener(this);
				popup.add(menuItem);
		        
		        menuItem = new JMenuItem(popupTraceSender);
		        menuItem.addActionListener(this);
		        popup.add(menuItem);
		        
		        menuItem = new JMenuItem(popupChangeColor);
		        menuItem.addActionListener(this);
		        popup.add(menuItem);
		        
		        menuItem = new JMenuItem(popupDropPEsForObject);
		        menuItem.addActionListener(this);
		        popup.add(menuItem);

		        menuItem = new JMenuItem(popupDropPEsForPE);
		        menuItem.addActionListener(this);
		        popup.add(menuItem);
		            
		        popup.show(this, evt.getX(), evt.getY());			
			}
		}
	} 
	

	/** Iteratively trace the upstream messages that 
	 * led to this entry method, without loading any 
	 * additional processor timelines 
	 * 
	 * return a set of entry method objects and 
	 * TimelineMessage objects associated with the 
	 * trace
	 * 
	 */
	public Set<EntryMethodObject> traceBackwardDependencies(){
		synchronized(data.messageStructures){
			HashSet<EntryMethodObject> v = new HashSet<EntryMethodObject>();
			if(data.traceMessagesBackOnHover()){
				EntryMethodObject obj = this;

				boolean done = false;
				while(!done){
					done = true;
					v.add(obj);

					if (obj.entry != -1 && obj.pCreation <= data.numPEs() ){
						// Find message that created the object
						TimelineMessage created_message = obj.creationMessage();
						if(created_message != null){
							// Find object that created the message
							obj = data.messageStructures.getMessageToSendingObjectsMap().get(created_message);
							if(obj != null){
								done = false;
							}

						}
					}
				}
			}
			return v;
		}
	}


	/** Trace one level of message sends forward from this object
	 *
	 *  @note This uses an inefficient algorithm which could be sped up by using more suitable data structures
	 *
	 */
	public Set<EntryMethodObject> traceForwardDependencies(){
		HashSet<EntryMethodObject> v = new HashSet<EntryMethodObject>();
		
		LinkedList<EntryMethodObject> toExamine = new LinkedList<EntryMethodObject>();
		
		toExamine.add(this);
		
		while(toExamine.size()>0){
			EntryMethodObject current = toExamine.poll();
			v.add(current);
						
			// For all loaded EntryMethodObjects, see if they match any of the sends from this object
			Iterator<Integer> iter = data.allEntryMethodObjects.keySet().iterator();
			while(iter.hasNext()){
				Integer pe = iter.next();
				List<EntryMethodObject> entryMethods = data.allEntryMethodObjects.get(pe);

				Iterator<EntryMethodObject> j = entryMethods.iterator();
				while(j.hasNext()){
					EntryMethodObject obj = j.next();

					// If any of the messages sent by this object created the EntryMethodObject obj
					TimelineMessage m = obj.creationMessage();
					// a message found on pe=obj.pCreation with eventID==obj.EventID

					if(m!=null && current.messages.contains(m))
						toExamine.add(obj);
					
				}

			}
			if(data.traceMessagesForwardOnHover() == false){
				break; // only go one step forward from the current object
			}
		}
		
		v.remove(this);
		
		return v;
	}

	
	
	/** Return the message that caused the entry method to execute. Complexity=O(1) time */
	public TimelineMessage creationMessage(){
		synchronized(data.messageStructures){
			if(data == null)
				return null;
			else if(pCreation<0)
				return null;
			else if(data.messageStructures.getEventIDToMessageMap() == null)
				return null;
			else if(pCreation >= data.messageStructures.getEventIDToMessageMap().length)
				return null;
			else if(data.messageStructures.getEventIDToMessageMap()[pCreation] == null)
				return null;
			else
				return (TimelineMessage) data.messageStructures.getEventIDToMessageMap()[pCreation].get(new Integer(EventID));

		}
	}
	
	

	public void mouseEntered(MouseEvent evt)
	{   
		boolean needRepaint = false;
		
		// Highlight the messages linked to this object
		if(data.traceMessagesBackOnHover() || data.traceMessagesForwardOnHover()){
						
			Set<EntryMethodObject> fwd = traceForwardDependencies(); // this function acts differently depending on data.traceMessagesForwardOnHover()
			Set<EntryMethodObject> back = traceBackwardDependencies();// this function acts differently depending on data.traceMessagesBackOnHover()
				
			HashSet<Object> fwdGeneric = new HashSet<Object>();
			HashSet<Object> backGeneric =  new HashSet<Object>();
			fwdGeneric.addAll(fwd); // this function acts differently depending on data.traceMessagesForwardOnHover()
			backGeneric.addAll(back); // this function acts differently depending on data.traceMessagesBackOnHover()
						
			// Highlight the forward and backward messages
			data.clearMessageSendLines();
			data.addMessageSendLine(back);
			data.addMessageSendLineAlt(fwd);
			
			// highlight the objects as well
			data.HighlightObjects(fwdGeneric);
			data.HighlightObjects(backGeneric);
			
			needRepaint=true;
		}
			
		
		// Highlight any Entry Method invocations for the same chare array element
		if(data.traceOIDOnHover()){
			synchronized(data.messageStructures){
			Set allWithSameId = (Set) data.messageStructures.getOidToEntryMethodObjectsMap().get(tid);
			data.HighlightObjects(allWithSameId);
			needRepaint=true;
			}
		}	

		
		if(needRepaint)
			data.displayMustBeRepainted();
		
	}   


	public void mouseExited(MouseEvent evt)
	{
		boolean needRepaint = false;
		
		if(data.traceMessagesBackOnHover() || data.traceMessagesForwardOnHover()){
			data.clearObjectHighlights();
			data.clearMessageSendLines();
			needRepaint=true;
		}
		if(data.traceOIDOnHover()){
			data.clearObjectHighlights();	
			needRepaint=true;
		}
				
		if(needRepaint)
			data.displayMustBeRepainted();
		
	}   


	public void mousePressed(MouseEvent evt)
	{
		// ignore 	
	}   

	public void mouseReleased(MouseEvent evt)
	{
		// ignore 	
	}   

	private void OpenMessageWindow()
	{
		if(msgwindow == null) {
			msgwindow = new MessageWindow(this);
			Dimension d = msgwindow.getPreferredSize();
			msgwindow.setSize(480, d.width);
		}

		msgwindow.setVisible(true);
	} 
	
	/** Is this an idle event */
	public boolean isIdleEvent(){
		return (entry==-1);
	}
	
	public boolean isUnaccountedTime(){
		return (entry == -2);
	}

	public void paintComponent(Graphics g)
	{     
		super.paintComponent(g);
		
		// If this is an idle time region, we may not display it
		if 	(isIdleEvent() && data.showIdle() == false) 
			return;
		if (isIdleEvent() && MainWindow.IGNORE_IDLE)
			return;
		if(data.entryIsHiddenID(this.getEntryID()))
			return;
		

		// Determine the base color
		Color c = determineColor();


		// Dim this object if we want to focus on some objects (for some reason or another)
		if(data.isObjectDimmed(this))
			c = c.darker().darker();
		
			
		// Determine the coordinates and sizes of the components of the graphical representation of the object
		int rectWidth = getWidth();
		int rectHeight = data.barheight();

	

		// The distance from the top or bottom to the rectangle
		int verticalInset = 0;

		// Idle regions are thinner vertically
		if(entryIndex==-1){
			rectHeight -= 6;
			verticalInset += 3;
		}
		
		int left  = 0;
		int right = rectWidth-1;
			
		
		if(beginTime < data.startTime())
		{
			drawLeftArrow(g, c, verticalInset, rectHeight);
			rectWidth -= 5;
			left = 5;
		}

		if(endTime > data.endTime())
		{
			drawRightArrow(g, c, verticalInset, rectHeight, rectWidth);
			rectWidth -= 5;
			right = rectWidth-6;
		}

		// Paint the main rectangle for the object, as long as it is not a skinny idle event
		g.setColor(c);
		if(rectWidth > 1 || entryIndex!=-1)
			g.fillRect(left, verticalInset, rectWidth, rectHeight);


		// Paint the edges of the rectangle lighter/darker to give an embossed look
		if(rectWidth > 2 && !data.colorByMemoryUsage())
		{
			g.setColor(c.brighter());
			g.drawLine(left, verticalInset, right, verticalInset);
			if(left == 0)
				g.drawLine(0, verticalInset, 0, verticalInset+rectHeight-1);

			g.setColor(c.darker());
			g.drawLine(left, verticalInset+rectHeight-1, right, verticalInset+rectHeight-1);
			if(right == rectWidth-1)
				g.drawLine(rectWidth-1, verticalInset, rectWidth-1, verticalInset+rectHeight-1);
		}


		/* 

		   Paint the message packing area

		   The packing rectangle goes from the leftmost pixel associated with the 
		   packBeginTime to the rightmost pixel associated with the packEndTime

	       The beginning will either be the same as the message send, or it will 
	       be one microsecond later. The mess packing area may therefore not be
	       connected to the message send when zoomed in.

		 */

		if(data.showPacks() && packs != null)
		{
			g.setColor(Color.pink);
			for(int p=0; p<packs.length; p++)
			{
				long packBeginTime = packs[p].BeginTime;
				long packEndTime = packs[p].EndTime;

				if(packEndTime >= data.startTime() && packBeginTime <= data.endTime())
				{

					// Compute the begin pixel coordinate relative to the containing panel
					int packBeginPanelCoordX = data.timeToScreenPixelLeft(packBeginTime);

					// Compute the begin pixel coordinate relative to the Entry method object itself
					int packBeginObjectCoordX = packBeginPanelCoordX  - leftCoord - 1;

					// Compute the end pixel coordinate relative to the containing panel
					int packEndPanelCoordX = data.timeToScreenPixelRight(packEndTime);

					// Compute the end pixel coordinate relative to the Entry method object itself
					int packEndObjectCoordX = packEndPanelCoordX  - leftCoord - 1;

					g.fillRect(packBeginObjectCoordX, verticalInset+rectHeight, (packEndObjectCoordX-packBeginObjectCoordX+1), data.messagePackHeight());

				}
			}
		}

		// Show the message sends. See note above for the message packing areas
		// Don't change this without changing MainPanel's paintComponent which draws message send lines
		if(data.showMsgs() == true && messages != null)
		{
			g.setColor(getForeground());
			
			Iterator<TimelineMessage> m = messages.iterator();
			while(m.hasNext()){
				TimelineMessage msg = m.next();
				long msgtime = msg.Time;
				if(msgtime >= data.startTime() && msgtime <= data.endTime())
				{
					// Compute the pixel coordinate relative to the containing panel
					int msgPanelCoordX = data.timeToScreenPixel(msgtime);

					// Compute the pixel coordinate relative to the Entry method object itself
					int msgObjectCoordX = msgPanelCoordX  - leftCoord;

					g.drawLine(msgObjectCoordX, verticalInset+rectHeight, msgObjectCoordX, verticalInset+rectHeight+data.messageSendHeight());

				}
			}
		}
	}

		
	/**  Determine the color of the object */	
	private Color determineColor() {
		// First handle the simple cases of idle, unknown and function events
		if (isIdleEvent()) { 	
			// Idle time regions are white on a dark background, or grey on a light background
			Color bg = data.getBackgroundColor();
			int brightness = bg.getRed() + bg.getGreen() + bg.getBlue();
			if(brightness > (128*3)){
				// bright background
				return bg.darker();
			} else {
				// dark background ( keep the same traditional look for the old folks ) 
				return Color.white;
			}
		} else if (entryIndex == -2) { // unknown domain
			return data.getBackgroundColor();
		} else if (isFunction) {
			return MainWindow.runObject[data.myRun].getFunctionColor(entryIndex);
		}


		// color the objects by memory usage with a nice blue - red gradient
		if(data.colorByMemoryUsage()){
			if(this.memoryUsage == null){
				return Color.darkGray;
			}else{
				// scale the memory usage to the interval [0,1]
				float normalizedValue = (float)(memoryUsage.intValue() - data.minMemBColorRange()) / (float)(data.maxMemBColorRange()-data.minMemBColorRange());
				if( normalizedValue<0.0 || normalizedValue>1.0 )
					return Color.darkGray;
				else {
					return Color.getHSBColor(0.6f-normalizedValue*0.65f, 1.0f, 1.0f); 
				}
			}
		}


		// color the objects by user supplied values with a nice blue gradient
		if(data.colorByUserSupplied() && data.colorSchemeForUserSupplied==Data.BlueGradientColors){
			if(userSuppliedData !=  null){
				long value = userSuppliedData.longValue();
				float normalizedValue = (float)(value - data.minUserSupplied) / (float)(data.maxUserSupplied-data.minUserSupplied);
				return Color.getHSBColor(0.25f-normalizedValue*0.75f, 1.0f, 1.0f); 
			} 	else {
				return Color.darkGray;
			}
		}

		
		
		

//		
//			
//		if(data.colorByUserSupplied() && data.colorSchemeForUserSupplied==Data.RandomColors){
//			if(userSuppliedData !=  null){
//				switch ((userSuppliedData.intValue()+5000)%10) {
//				 case 0: c = Color.green; break;
//				 case 1: c = Color.red; break;
//				 case 2: c = Color.blue; break;
//				 case 3: c = Color.yellow; break;
//				 case 4: c = Color.darkGray; break;
//				 case 5: c = Color.magenta; break;
//				 case 6: c = Color.cyan; break;
//				 case 7: c = Color.orange; break;
//				 case 8: c = Color.pink; break;
//				 case 9: c = Color.lightGray; break;
//				}
//			}
//		}


		// Sometimes Overrule the normal colors and use one based on the chare array index
		if( data.colorByOID() || data.colorByUserSupplied() || data.colorByEID() ){

			long color = 0;

			if(data.colorByOID()){
				// hashes of the object indices
				int h1, h2, h3, h4;
				h1 = (getTid().id[0] * 139) % 509;
				h2 = (getTid().id[1] * 101) % 1039;
				h3 = (getTid().id[2] * 67) % 1291;
				h4 = (getTid().id[3] * 2789) % 1721;
				color += h1^h3^h2^h4;
			}


			if(data.colorByEID()){
				color += (entryIndex * 251) % 5113;
			}


			if(data.colorByUserSupplied() && userSuppliedData != null){
				color += (userSuppliedData * 359) % 4903;
			}


			if(data.colorByMemoryUsage() && memoryUsage != null){
				color += (memoryUsage * 6121) % 5953;
			}

			 // Should range from 0.0 to 2.0
			 float h2 = ((color+512) % 512) / 256.0f;
			 // Should range from 0.0 to 1.0
			 float h = ((color+512) % 512) / 512.0f;

			
			
			float s = 1.0f;   // Should be 1.0

			float b = 1.0f;   // Should be 0.5 or 1.0

			if(h2 > 1.0)
				b = 0.6f;

			return Color.getHSBColor(h, s, b);

		} else {
			return data.entryColor()[entryIndex];
		}


	}

	
	
	public void setLocationAndSize(int actualDisplayWidth)
	{
		
		if(data.entryIsHiddenID(entry)){
			setBounds( 0, 0, 0, 0 );			
			return;
		}	
		
		
		leftCoord = data.timeToScreenPixel(beginTime, actualDisplayWidth);
		rightCoord = data.timeToScreenPixel(endTime, actualDisplayWidth);

		if(endTime > data.endTime())
			rightCoord = data.timeToScreenPixelRight(data.endTime(), actualDisplayWidth);

		if(beginTime < data.startTime())
			leftCoord = data.timeToScreenPixelLeft(data.startTime(), actualDisplayWidth);
		
		int width = rightCoord-leftCoord+1;

		if(width < 1)
			width = 1;
		
//		int singleTimelineH = data.singleTimelineHeight();
		
//		this.setBounds(leftCoord,  whichTimelineVerticalIndex()*singleTimelineH,
//				width, singleTimelineH);
		
		this.setBounds(leftCoord,  data.entryMethodLocationTop(pCurrent),
				width, data.entryMethodLocationHeight());
	
	}   
	
//	public int whichTimelineVerticalIndex(){
//		return data.whichTimelineVerticalPosition(pCurrent);
//	}
	

	public void setPackUsage()
	{
		packtime = 0;
		if(packs != null)
		{   
			for(int p=0; p<packs.length; p++)
			{
				// packtime += packs[p].EndTime - packs[p].BeginTime + 1;
				packtime += packs[p].EndTime - packs[p].BeginTime;
				if(packs[p].BeginTime < data.startTime())
					packtime -= (data.startTime() - packs[p].BeginTime);
				if(packs[p].EndTime > data.endTime())
					packtime -= (packs[p].EndTime - data.endTime());
			}
			packusage = packtime * 100;
			packusage /= (data.endTime() - data.startTime());
		}
	}   

	public void setUsage()
	{
		//       System.out.println(beginTime + " " + endTime + " " +
		//			  data.beginTime + " " + data.endTime);
		if (entryIndex < -1) {
			// if I am not a standard entry method, I do not contribute
			// to the usage
			//
			// 2006/10/02 - **CW** changed it such that idle time gets
			//              usage accounted for.
			return;
		}

		usage = endTime - beginTime;
		//	  usage = endTime - beginTime + 1;

		//	  System.out.println("Raw usage : " + usage);

		if (beginTime < data.startTime()) {
			usage -= (data.startTime() - beginTime);
		}
		if (endTime > data.endTime()) {
			usage -= (endTime - data.endTime());
		}
		//	  System.out.println("Final usage : " + usage);
		//	  System.out.println();

		usage /= (data.endTime() - data.startTime());
		usage *= 100;
		// System.out.println(usage);
	}


	public int compareTo(Object o) {
		EntryMethodObject obj = (EntryMethodObject) o;
		if(pCreation != obj.pCreation)
			return pCreation-obj.pCreation;
		else if(pCurrent != obj.pCurrent)
			return pCurrent - obj.pCurrent;
		else
			return EventID - obj.EventID;
	}

	public ObjectId getTid() {
		return tid;
	}

	/** Shift all the times associated with this entry method by given amount */
	public void shiftTimesBy(long s){
		beginTime += s;
		endTime += s;
		recvTime += s;
		cpuBegin += s;
		cpuEnd += s;
	}


	/** Handle the right-click popup menu events */
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() instanceof JMenuItem) {
			String arg = ((JMenuItem) e.getSource()).getText();
			if (arg.equals(popupChangeColor)){
				Color old = MainWindow.runObject[data.myRun].getEntryColor(entry);
				Color c = JColorChooser.showDialog(null, "Choose new color", old); 
				if(c !=null){
					MainWindow.runObject[data.myRun].setEntryColor(entry, c);
					data.displayMustBeRepainted();
				}

			} 
			else if(arg.equals(popupShowDetails)) {
				OpenMessageWindow();
			} 
			else if(arg.equals(popupTraceSender)) {
				data.clickTraceSender(this);				
			} 
			else if(arg.equals(popupDropPEsForObject)) {	
				data.dropPEsUnrelatedToObject(this);
			} 
			else if(arg.equals(popupDropPEsForPE)) {
				data.dropPEsUnrelatedToPE(this.pCurrent);
			}

		}

	}


}
