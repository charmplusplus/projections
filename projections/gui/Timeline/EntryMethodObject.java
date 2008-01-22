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

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;

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


	public EntryMethodObject(Data data,  TimelineEvent tle, 
			TreeSet msgs, PackTime[] packs,
			int p1)
	{
		format_.setGroupingUsed(true);

		setVisible(true);

		setBackground(MainWindow.runObject[myRun].background);
		setForeground(MainWindow.runObject[myRun].foreground);

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

		tleUserEventName = tle.userEventName;

		numPapiCounts = tle.numPapiCounts;
		papiCounts    = tle.papiCounts;

		int n = tle.EntryPoint;
		isFunction = tle.isFunction;

		setUsage();
		setPackUsage();
		
		// Construct a nice informative html formatted string about this entry method object. 
		// This string is displayed on mouseover(by setting it as this component's tooltip)
		String infoString = "";

		// **CW** special treatment for functions. There really should
		// be a general way of dealing with this.
		if (isFunction) {
			
			infoString += "<i>Function</i>: " + MainWindow.runObject[myRun].getFunctionName(entry) + "<br>";
			infoString += "<i>Begin Time</i>: " + format_.format(beginTime) + "<br>";
			infoString += "<i>End Time</i>: " + format_.format(endTime) + "<br>";
			infoString += "<i>Total Time</i>: " + U.t(endTime-beginTime) + "<br>";
			infoString += "<i>Msgs created</i>: " + msgs.size() + "<br>";
			infoString += "<i>Id</i>: " + tid.id[0] + ":" + tid.id[1] + ":" + tid.id[2] + "<br>";
			infoString += "<hr><br><i>Function Callstack</i>:<br>";

			// consume the call stack
			while (!tle.callStack.empty()) {
				AmpiFunctionData functionData = (AmpiFunctionData)tle.callStack.pop();
				infoString += "<i>[Func]</i>: " + MainWindow.runObject[myRun].getFunctionName(functionData.FunctionID) + "<br>";
				infoString += "&nbsp&nbps&nbsp&nbps<i>line</i>:" + functionData.LineNo + " <i>file</i>: " + functionData.sourceFileName + "<br>";
			}
		} else if (n >= 0) {

			int ecount = MainWindow.runObject[myRun].getNumUserEntries();
			if (n >= ecount) {
				System.out.println("<b>Fatal error: invalid entry " + n +
						" on processor " + pCurrent + "</b>!");
				System.exit(1) ;
			}
			infoString += "<b>"+(MainWindow.runObject[myRun].getEntryNames())[n][1] + "::" + (MainWindow.runObject[myRun].getEntryNames())[n][0] + "</b><br><br>"; 

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
			
			infoString += "<i>Msgs created</i>: " + msgs.size() + "<br>";
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
					infoString += MainWindow.runObject[myRun].getPerfCountNames()[i] + " = " + format_.format(papiCounts[i]) + "<br>";
				}
			}
		} else if (n == -1) {
			infoString += "<b>Idle Time</b><br><br>";
			infoString += "<i>Begin Time</i>: " + format_.format(beginTime)+ "<br>";
			infoString += "<i>End Time</i>: " + format_.format(endTime) + "<br>";
			infoString += "<i>Total Time</i>: " + U.t(endTime-beginTime) + "<br>";
		} else if (n == -2) {
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
			
			infoString += "<i>Msgs created</i>: " + msgs.size() + "<br>";
		}
		
		if(!isIdleEvent()){
			addMouseListener(this);
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
			
			if (obj.entry != -1 && obj.pCreation <= data.maxPs() && data.mesgVector[obj.pCreation] != null ){
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
		if(data != null && pCreation>=0 && data.eventIDToMessageMap != null && pCreation<data.eventIDToMessageMap.length)
			return (TimelineMessage) data.eventIDToMessageMap[pCreation].get(EventID);
		else
			return null;
	}
	
	

	public void mouseEntered(MouseEvent evt)
	{   
		// Highlight the dependencies of this object
		if(data.showDependenciesOnHover()){
			Set s = new HashSet();
			s.addAll(traceBackwardDependencies());
			s.addAll(traceForwardDependencies());
			data.clearMessageSendLines();
			data.addMessageSendLine(s);
			data.HighlightObjects(s);
			data.displayMustBeRepainted();
		}
	}   


	public void mouseExited(MouseEvent evt)
	{
		if(data.showDependenciesOnHover()){
			data.clearObjectHighlights();
			data.clearMessageSendLines();
			data.displayMustBeRepainted();
		}
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

	private Color getObjectColor(ObjectId tid){
		int r;
		int g;
		int b;
		int nPE = data.processorList().size();
		int d = (int )(255/(double )nPE);
		r = ((tid.id[0])*d) % 255;
		g = ((tid.id[1])*23) % 255;
		b = 255-g;
		//b = 56;
		return new Color(r, g,b );
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
			if(data.colorbyObjectId()){
				c = getObjectColor(tid);
			}else{
				c = data.entryColor()[entry];
				if (isFunction) {
					c = MainWindow.runObject[myRun].getFunctionColor(entry);
				}
			}	
		}
		
		
		if(data.isObjectDimmed(this))
			c = c.darker().darker();
		


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
		if(rectWidth > 2)
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


	public void setLocationAndSize(int actualDisplayWidth)
	{
		leftCoord = data.timeToScreenPixel(beginTime, actualDisplayWidth);
		rightCoord = data.timeToScreenPixel(endTime, actualDisplayWidth);

		if(endTime > data.endTime())
			rightCoord = data.timeToScreenPixelRight(data.endTime(), actualDisplayWidth);

		if(beginTime < data.beginTime())
			leftCoord = data.timeToScreenPixelLeft(data.beginTime(), actualDisplayWidth);
		
		int width = rightCoord-leftCoord+1;
		
		this.setBounds(leftCoord,  whichTimelineVerticalIndex*data.singleTimelineHeight(),
				width, data.singleTimelineHeight());
	
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


}
