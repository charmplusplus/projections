package projections.gui.Timeline;


import java.awt.*;
import java.awt.event.*;
import projections.analysis.*;
import projections.gui.MainWindow;
import projections.gui.U;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.*;

public class EntryMethodObject extends JComponent implements Comparable, MouseListener
{

	private static final long serialVersionUID = 1L;


	private MessageWindow msgwindow;
	private long    beginTime, endTime, recvTime;
	private long cpuTime;
	private long cpuBegin, cpuEnd;
	private int     entry;
	private int     msglen;
	int EventID;
	private ObjectId tid; 
	int pCurrent; // I assume this is which displayed timeline the event is assocated with
	int pCreation;
	
	/** Data specified by the user, likely a timestep. Null if nonspecified */
	Integer userSuppliedData;
	
	/** Memory usage at some point in this entry method. Null if nonspecified */
	Integer memoryUsage;
	
	/** Stores the creationMessage after it has been found by creationMessage() */
	private TimelineMessage creationMessage;
	
	/** The duration of the visible portion of this event */
	private double  usage;
	private float packusage;
	private long packtime;
	
	/** Vertical index that shows which displayed timeline this event lives on */
	private int whichTimelineVerticalIndex;
	
	/** Pixel coordinate of left side of object */
	private int leftCoord=0;

	/** Pixel coordinate of right side of object */
	private int rightCoord=0;
	
	
	private String tleUserEventName;


	private Data data = null;
	public TreeSet messages; // Set of TimelineMessage's
	private PackTime[] packs;

	private int numPapiCounts = 0;
	private long papiCounts[];

	private boolean isFunction = false;

	private static DecimalFormat format_ = new DecimalFormat();
	private AmpiFunctionData funcData[];

	public EntryMethodObject(Data data,  TimelineEvent tle, 
			TreeSet msgs, PackTime[] packs,
			int p1)
	{
		format_.setGroupingUsed(true);

		setVisible(true);
		
		setBackground(MainWindow.runObject[data.myRun].background);
		setForeground(MainWindow.runObject[data.myRun].foreground);

		this.data = data;
		beginTime = tle.BeginTime;
		endTime   = tle.EndTime;
		cpuBegin  = tle.cpuBegin;
		cpuEnd    = tle.cpuEnd;
		cpuTime   = cpuEnd - cpuBegin;
		entry     = tle.EntryPoint;
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

		setUsage();
		setPackUsage();

		if (isFunction) {
			// copy the callstack	
			funcData = new AmpiFunctionData[tle.callStack.size()];
			tle.callStack.copyInto(funcData);
					
		} else if (tle.EntryPoint >= 0) {
			int ecount = MainWindow.runObject[data.myRun].getNumUserEntries();
			if (tle.EntryPoint >= ecount) {
				System.out.println("<b>Fatal error: invalid entry " + tle.EntryPoint +
						" on processor " + pCurrent + "</b>!");
				System.exit(1) ;
			}
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
			infoString += "<i>Total Time</i>: " + U.t(endTime-beginTime) + "<br>";
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

			infoString += "<b>"+(MainWindow.runObject[data.myRun].getEntryNames())[entry][1] + "::" + (MainWindow.runObject[data.myRun].getEntryNames())[entry][0] + "</b><br><br>"; 

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
			
			infoString +=  "<i>Total Time</i>: " + U.t(endTime-beginTime);
			if (cpuTime > 0)
				infoString +=  " (" + U.t(cpuTime) + ")";
			infoString += "<br>";
			
			infoString +=  "<i>Packing</i>: " + U.t(packtime);
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
			infoString += "<i>Total Time</i>: " + U.t(endTime-beginTime) + "<br>";
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
			
			infoString +=  "<i>Total Time</i>: " + U.t(endTime-beginTime);
			if (cpuTime > 0) 
				infoString +=  " (" + (cpuTime) + ")";
			infoString += "<br>";
			
			infoString +=  "<i>Packing</i>: " + U.t(packtime);
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
	
	
	
	
	public void CloseMessageWindow()
	{
		msgwindow = null;
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

	public int getEntry()
	{
		return entry;
	}   

	/** Return a set of messages for this entry method */
	public Set getMessages()
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
				OpenMessageWindow();
			} else {	
				// non-left click
				data.entryMethodObjectRightClick(this);				
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
	public HashSet traceBackwardDependencies(){
		EntryMethodObject obj = this;
		HashSet v = new HashSet();
		
		boolean done = false;
		while(!done){
			done = true;
			v.add(obj);
			
			if (obj.entry != -1 && obj.pCreation <= data.numPEs() && data.mesgVector[obj.pCreation] != null ){
				// Find message that created the object
				TimelineMessage created_message = obj.creationMessage();
				if(created_message != null){
					// Find object that created the message
					obj = (EntryMethodObject) data.messageToSendingObjectsMap.get(created_message);
					if(obj != null){
						done = false;
					}

				}
			}
		}
		return v;
	}


	/** Trace one level of message sends forward from this object
	 *
	 *  @note This uses an inefficient algorithm which could be sped up by using more suitable data structures
	 *
	 */
	public HashSet traceForwardDependencies(){
		HashSet v = new HashSet();

		// For all loaded EntryMethodObjects, see if they match any of the sends from this object
		for(int i=0;i<data.tloArray.length;i++){
			for(int j=0;j<data.tloArray[i].length;j++){
				EntryMethodObject obj = data.tloArray[i][j];
				
				// If any of the messages sent by this object created the EntryMethodObject obj
				TimelineMessage m = obj.creationMessage();
				// a message found on pe=obj.pCreation with eventID==obj.EventID
				
				if(m!=null && messages.contains(m))
					v.add(obj);
						
			}

		}
		
		return v;
	}

	
	
	/** Return the message that caused the entry method to execute. Complexity=O(1) time */
	public TimelineMessage creationMessage(){
		if(data != null && pCreation>=0 && data.eventIDToMessageMap != null && data.eventIDToMessageMap[pCreation] != null && pCreation<data.eventIDToMessageMap.length)
			return (TimelineMessage) data.eventIDToMessageMap[pCreation].get(EventID);
		else
			return null;
	}
	
	

	public void mouseEntered(MouseEvent evt)
	{   
		boolean needRepaint = false;
		
		// Highlight the messages linked to this object
		if(data.traceMessagesOnHover()){
			Set fwd = traceForwardDependencies();
			Set back = traceBackwardDependencies();
			
			// Highlight the forward and backward messages
			data.clearMessageSendLines();
			data.addMessageSendLine(back);
			data.addMessageSendLineAlt(fwd);
			
			// highlight the objects as well
			data.HighlightObjects(fwd);
			data.HighlightObjects(back);
			
			needRepaint=true;
		}
		
		
		// Highlight any Entry Method invocations for the same chare array element
		if(data.traceOIDOnHover()){
			Set allWithSameId = (Set) data.oidToEntryMethonObjectsMap.get(tid);
			data.HighlightObjects(allWithSameId);
			needRepaint=true;
		}

		
		if(needRepaint)
			data.displayMustBeRepainted();
		
	}   


	public void mouseExited(MouseEvent evt)
	{
		boolean needRepaint = false;
		
		if(data.traceMessagesOnHover()){
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
	

	public void paintComponent(Graphics g)
	{     
		super.paintComponent(g);

		// If this is an idle time region, we may not display it
		if ((isIdleEvent() && data.showIdle == false) ||
				(isIdleEvent() && MainWindow.IGNORE_IDLE)) {
			return;
		}

		Color c;

		// Set the colors of the object
		if (isIdleEvent()) { 

			// Idle time regions are white on a dark background, or grey on a light background

			Color bg = data.getBackgroundColor();

			int brightness = bg.getRed() + bg.getGreen() + bg.getBlue();

			if(brightness > (128*3)){
				// bright background
				c = bg.darker();
			} else {
				// dark background ( keep the same traditional look for the old folks ) 
				c = Color.white;
			}

		} else if (entry == -2) { // unknown domain
			c = getBackground();
		}  else {
			c = data.entryColor()[entry];
			if (isFunction) {
				c = MainWindow.runObject[data.myRun].getFunctionColor(entry);
			}
		}
		
		// Sometimes Overrule the normal colors and use one based on the chare array index
		if( ! isIdleEvent() && data.colorbyObjectId())
			c = colorFromOID();
	
		// Dim this object if we want to focus on some objects(for some reason or another)
		if(data.isObjectDimmed(this))
			c = c.darker().darker();
		
		
		// grey out the objects with odd userSuppliedData value 
		if(data.colorByUserSupplied()){
			if(userSuppliedData !=  null && (((userSuppliedData.intValue()+4096)%2)==1)){
				c = Color.darkGray;
			} else {
			}
		}
		
		// color the objects by memory usage 
		if(data.colorByMemoryUsage()){
			if(this.memoryUsage == null){
				c = Color.darkGray;
			}else{
				// scale the memory usage to the interval [0,1]
				float m = (float)(memoryUsage.intValue() - data.minMem) / (float)(data.maxMem-data.minMem);
				
				if( m<0.0 || m>1.0 )
					c = Color.darkGray;
				else {
//					System.out.println("memoryUsage="+memoryUsage.intValue()+"  m="+m);
					c = Color.getHSBColor(0.2f-m*0.25f, 1.0f, 1.0f); 
				}
			}
			
		}
		
			
		// Determine the coordinates and sizes of the components of the graphical representation of the object
		int rectWidth = getWidth();
		int rectHeight = data.barheight();

		// Idle regions are thinner vertically
		if(entry==-1){
			rectHeight -= 6;
		}

		// The distance from the top or bottom to the rectangle
		int verticalInset = (getHeight()-rectHeight)/2;

		int left  = 0;
		int right = rectWidth-1;


		if(beginTime < data.beginTime())
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
		if(rectWidth > 1 || entry!=-1)
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

		if(data.showPacks == true && packs != null)
		{
			g.setColor(Color.pink);
			for(int p=0; p<packs.length; p++)
			{
				long packBeginTime = packs[p].BeginTime;
				long packEndTime = packs[p].EndTime;

				if(packEndTime >= data.beginTime() && packBeginTime <= data.endTime())
				{

					// Compute the begin pixel coordinate relative to the containing panel
					int packBeginPanelCoordX = data.timeToScreenPixelLeft(packBeginTime);

					// Compute the begin pixel coordinate relative to the Entry method object itself
					int packBeginObjectCoordX = packBeginPanelCoordX  - leftCoord;

					// Compute the end pixel coordinate relative to the containing panel
					int packEndPanelCoordX = data.timeToScreenPixelRight(packEndTime);

					// Compute the end pixel coordinate relative to the Entry method object itself
					int packEndObjectCoordX = packEndPanelCoordX  - leftCoord;

					g.fillRect(packBeginObjectCoordX, verticalInset+rectHeight, (int)(packEndObjectCoordX-packBeginObjectCoordX+1), data.messagePackHeight());

				}
			}
		}

		// Show the message sends. See note above for the message packing areas
		// Don't change this without changing MainPanel's paintComponent which draws message send lines
		if(data.showMsgs == true && messages != null)
		{
			g.setColor(getForeground());
			
			Iterator m = messages.iterator();
			while(m.hasNext()){
				TimelineMessage msg = (TimelineMessage) m.next();
				long msgtime = msg.Time;
				if(msgtime >= data.beginTime() && msgtime <= data.endTime())
				{
					// Compute the pixel coordinate relative to the containing panel
					int msgPanelCoordX = data.timeToScreenPixelLeft(msgtime);

					// Compute the pixel coordinate relative to the Entry method object itself
					int msgObjectCoordX = msgPanelCoordX  - leftCoord;

					g.drawLine(msgObjectCoordX, verticalInset+rectHeight, msgObjectCoordX, verticalInset+rectHeight+data.messageSendHeight());

				}
			}
		}
	}

	/** Create a color based on the chare array index for the object executing this entry method */
	private Color colorFromOID() {

		// hashes of the object indices
		int h1, h2, h3, h4;
		
		h1 = ((getTid().id[0]+2) * 7841) % 223;
		h2 = ((getTid().id[1]+3) * 7841) % 223;
		h3 = ((getTid().id[2]+5) * 7841) % 223;
		h4 = ((getTid().id[3]+7) * 7841) % 223;

		// This will give us a pretty crazy random bitpattern
		int h5 = h1^h3^h2^h4;
		h5 = h5 ^ (h5/1024);
		
		float h;   // Should range from 0.0 to 1.0
		h = (h5 % 512) / 512.0f;
		
		float s = 1.0f;   // Should be 1.0
				
		float b = 1.0f;   // Should be 0.5 or 1.0
		if((h5%2) == 0)
			b = 1.0f;
		else
			b = 0.5f;
		
		return Color.getHSBColor(h, s, b);
	}

	public void setLocationAndSize(int actualDisplayWidth)
	{
		leftCoord = data.timeToScreenPixel(beginTime, actualDisplayWidth);
		rightCoord = data.timeToScreenPixel(endTime, actualDisplayWidth);

		if(endTime > data.endTime())
			rightCoord = data.timeToScreenPixelRight(data.endTime(), actualDisplayWidth);

		if(beginTime < data.beginTime())
			leftCoord = data.timeToScreenPixelLeft(data.beginTime(), actualDisplayWidth);
		
		int width = rightCoord-leftCoord+1;
		
		int singleTimelineH = data.singleTimelineHeight();
		this.setBounds(leftCoord,  whichTimelineVerticalIndex*singleTimelineH,
				width, singleTimelineH);
	
	}   

	public void setPackUsage()
	{
		packtime = 0;
		if(packs != null)
		{   
			for(int p=0; p<packs.length; p++)
			{
				// packtime += packs[p].EndTime - packs[p].BeginTime + 1;
				packtime += packs[p].EndTime - packs[p].BeginTime;
				if(packs[p].BeginTime < data.beginTime())
					packtime -= (data.beginTime() - packs[p].BeginTime);
				if(packs[p].EndTime > data.endTime())
					packtime -= (packs[p].EndTime - data.endTime());
			}
			packusage = packtime * 100;
			packusage /= (data.endTime() - data.beginTime());
		}
	}   

	public void setUsage()
	{
		//       System.out.println(beginTime + " " + endTime + " " +
		//			  data.beginTime + " " + data.endTime);
		if (entry < -1) {
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

		if (beginTime < data.beginTime()) {
			usage -= (data.beginTime() - beginTime);
		}
		if (endTime > data.endTime()) {
			usage -= (endTime - data.endTime());
		}
		//	  System.out.println("Final usage : " + usage);
		//	  System.out.println();

		usage /= (data.endTime() - data.beginTime());
		usage *= 100;
		// System.out.println(usage);
	}

	public void setWhichTimeline(int p) {
		whichTimelineVerticalIndex = p;
	}

	public int getEventID() {
		return EventID;
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


}
