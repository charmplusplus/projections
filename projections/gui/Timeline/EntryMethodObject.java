package projections.gui.Timeline;

import java.awt.*;
import java.awt.event.*;
import projections.analysis.*;
import projections.gui.MainWindow;
import projections.gui.U;

import java.util.Vector;
import java.text.DecimalFormat;
import javax.swing.*;

public class EntryMethodObject extends JComponent
implements MouseListener
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
	private int EventID;
	private ObjectId tid; 
	private int pCurrent; // I assume this is which displayed timeline the event is assocated with
	private int pCreation;
	private double  usage;
	private float packusage;
	private long packtime;
	private int ylocation;
	
	private String tleUserEventName;


	private Data data = null;
	public TimelineMessage[] messages;
	private PackTime[] packs;
	// private UserEventObject[] userEvents;

	private int numPapiCounts = 0;
	private long papiCounts[];

	private boolean isFunction = false;

	private static DecimalFormat format_ = new DecimalFormat();


	public EntryMethodObject(Data data,  TimelineEvent tle, 
			TimelineMessage[] msgs, PackTime[] packs,
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
		messages  = msgs;
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
			infoString += "<i>Msgs created</i>: " + msgs.length + "<br>";
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
			infoString += "<i>Msg Len</i>: " + msglen + "<br>";
			
			
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
			
			infoString += "<i>Msgs created</i>: " + msgs.length + "<br>";
			infoString += "<i>Created by processor</i>: " + pCreation + "<br>";
			infoString += "<i>Id</i>: " + tid.id[0] + ":" + tid.id[1] + ":" + tid.id[2] + "<br>";
			if(tleUserEventName!=null)
				infoString += "<i>Associated User Event</i>: "+tleUserEventName+ "<br>";
			
			infoString += "<i>Recv Time</i>: " + recvTime + "<br>";
			
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
			
			infoString += "<i>Msgs created</i>: " + msgs.length + "<br>";
		}
		addMouseListener(this);
		
//		infoString += traceAvailableDependencies();
		
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

	public TimelineMessage[] getMessages()
	{
		return messages;
	}   

	public Dimension getMinimumSize()
	{
		return new Dimension(getSize().width, getSize().height);
	}   

	public float getNetUsage()
	{
		return (float)usage - packusage;
	}   

	public int getNumMsgs()
	{
		if(messages == null)
			return 0;
		else
			return messages.length;
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
//				System.out.println("))))) Left Click");
				OpenMessageWindow();
			} else {	
//				System.out.println("))))) Non-Left Click");
	
				// we may need to load this processor
				data.addProcessor(pCreation);
								

				TimelineMessage created_message = searchMesg(data.mesgVector[pCreation],EventID);

				if(created_message != null){
					data.toggleConnectingLine(pCreation,created_message.Time, pCurrent,beginTime,this);
				}		
			}
		}
	} 
	
	
	/** This will eventually trace the upstream messages that led to this entry method, without loading any additional processor timelines */
	public String traceAvailableDependencies(){
		String s = "";

		if (entry != -1){
			s += "<br><hr><br><b>Tracing Available Dependencies</b>:";

			s += "This event is EventID="+EventID+" from creation processor="+pCreation+"<br>";
//
//			if(pCreation <= data.maxPs()){
//				
//				if (data.mesgVector[pCreation] != null){
//
//					s += "caller timeline is loaded<br>";
////				TimelineMessage created_message = searchMesg(data.mesgVector[pCreation],EventID);
////			} else {
////				s += "caller timeline is not loaded<br>";
//
//				}
//			}
			
		}
		
		return s;
	}
	
	
	
	
	/** Search for specified event in vector v using a binary search. Returns null if not found, or eventid=-1. */
	public TimelineMessage searchMesg(Vector v,int eventid){
		TimelineMessage returnItem = null;

		// the binary search should deal with indices and not absolute
		// values, hence size-1.
		//
		// if eventid = -1, the event has no source. Link to the end of
		// the last event. (No spontaneous event creation is allowed in
		// charm++) **CW** VERIFY THIS!
		if (eventid == -1) {
			// still trying to find a way to make everything work together
			// while linking to the previous event.
			return null;  
		}

		// Try binary search first. If that fails, try sequential search.
		// This is because stuff like bigsim logs may not have eventID
		// stored in sorted order.

		returnItem = binarySearch(v,eventid,0,v.size()-1);
		if (returnItem == null) {
			return seqSearch(v,eventid);
		} else {
			return null;
		}
	}

	public TimelineMessage seqSearch(Vector v, int eventid) {
		TimelineMessage item;
		for (int i=0; i<v.size()-1; i++) {
			item = (TimelineMessage)v.elementAt(i);
			if (item.EventID == eventid) {
				return item;
			}
		}
		return null;
	}

	public TimelineMessage binarySearch(Vector v,int eventid,
			int start,int end) {
		int mid = (start + end)/2;
		TimelineMessage middle = (TimelineMessage)v.elementAt(mid);
		if(middle.EventID == eventid){
			return middle;
		}
		if(start==end){
			return null;
		}
		if(middle.EventID > eventid){
			return binarySearch(v,eventid,start,mid);
		}else{
			return binarySearch(v,eventid,mid+1,end);
		}
	}

	public void mouseEntered(MouseEvent evt)
	{
//		if ((entry == -1 && data.showIdle == false) ||
//				(entry == -1 && MainWindow.IGNORE_IDLE)) {
//			return;
//		}
//
//		if(!inside)
//		{
//			inside = true;
//			EntryMethodObject to = EntryMethodObject.this;
//			Point scrnloc = to.getLocationOnScreen();
//			Dimension size = getSize();
//
//			if(bubble == null)
//				bubble = new Bubble(this, bubbletext);
//			bubble.setLocation(scrnloc.x + evt.getX(), scrnloc.y + size.height + 2);
//			bubble.setVisible(true);
//		}     
	}   

	public void mouseExited(MouseEvent evt)
	{
//		if(inside)
//		{
//			if(bubble != null)
//			{
//				bubble.dispose();
//				bubble = null;
//			}   
//			inside = false;
//		}      
	}   

	public void mousePressed(MouseEvent evt)
	{
		// ignore 	
	}   

	public void mouseReleased(MouseEvent evt)
	{
		// ignore cause problems if the window containing the container is resized.
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

	public void paintComponent(Graphics g)
	{     
		super.paintComponent(g);

		// If this is an idle time region, we may not display it
		if ((entry == -1 && data.showIdle == false) ||
				(entry == -1 && MainWindow.IGNORE_IDLE)) {
			return;
		}

		Color c;

		
		// Set the colors of the object
		if (entry == -1) { 
			
			// Idle time regions are white on a dark background, or grey on a light background
			
//			Color fg = data.getForegroundColor();
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
		
		
		// Determine the coordinates and sizes of the components of the graphical representation of the object
		int rectWidth = getSize().width;
		int rectHeight = data.barheight();
		
		// Idle regions are thinner vertically
		if(entry==-1){
			rectHeight -= 6;
		}

		// The distance from the top or bottom to the rectangle
		int verticalInset = (getHeight()-rectHeight)/2;
		
//		System.out.println("rectWidth="+rectWidth+" rectHeight="+rectHeight+"verticalInset="+verticalInset);
		
		int left  = 0;
		int right = rectWidth-1;

		long viewbt = beginTime;
		long viewet = endTime;

		if(beginTime < data.beginTime())
		{
			drawLeftArrow(g, c, verticalInset, rectHeight);
			rectWidth -= 5;
			left = 5;
			viewbt = data.beginTime();
		}

		if(endTime > data.endTime())
		{
			drawRightArrow(g, c, verticalInset, rectHeight, rectWidth);
			rectWidth -= 5; // the rectangle is only a portion of the area
			right = rectWidth-6;
			viewet = data.endTime();
		}

		// Paint the main rectangle for the object 
		g.setColor(c);
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

		// Show the message sends
		if(data.showMsgs == true && messages != null)
		{
			g.setColor(getForeground());
			for(int m=0; m<messages.length; m++)
			{
				long msgtime = messages[m].Time;
				if(msgtime >= data.beginTime() && msgtime <= data.endTime())
				{
					double scale= rectWidth /((double)(viewet - viewbt + 1));
					int xPos = (int)((msgtime - viewbt+1.0) * scale);
					g.drawLine(xPos, verticalInset+rectHeight, xPos, verticalInset+rectHeight+data.messageSendHeight());
//					System.out.println("Displaying a message send from "+xPos+","+(verticalInset+rectHeight)+" to "+ xPos+","+ (verticalInset+rectHeight+data.messageSendHeight()));
				}
			}
		}               

		// Paint the message packing area
		if(data.showPacks == true && packs != null)
		{
			g.setColor(Color.pink);
			for(int p=0; p<packs.length; p++)
			{
				long pbt = packs[p].BeginTime;
				long pet = packs[p].EndTime;

				if(pet >= data.beginTime() && pbt <= data.endTime())
				{
					double scale= rectWidth /((double)(viewet - viewbt + 1));
					int xPos = (int)((pbt - viewbt) * scale);
					g.fillRect(xPos, verticalInset+rectHeight, (int)(pet-pbt+1), data.messagePackHeight());
				}
			}
		}               

	}   


	public void setLocationAndSize(int actualDisplayWidth)
	{

		long BT, ET;

		if(endTime > data.endTime())
			ET = data.endTime() - data.beginTime();
		else
			ET = endTime - data.beginTime();

		if(beginTime < data.beginTime()) 
			BT = 0;
		else BT = beginTime - data.beginTime();

		
		int BTS  = data.offset() + (int)(BT*data.pixelIncrement(actualDisplayWidth)/data.timeIncrement(actualDisplayWidth));
		int ETS  = data.offset() + (int)(ET*data.pixelIncrement(actualDisplayWidth)/data.timeIncrement(actualDisplayWidth));
		int LENS = ETS - BTS + 1; 
		if(LENS < 1) LENS = 1;

		if(endTime > data.endTime()) LENS += 5;
		if(beginTime < data.beginTime())
		{
			BTS  -= 5;
			LENS += 5;
		}

		this.setBounds(BTS,  ylocation*data.singleTimelineHeight(),
				LENS, data.singleTimelineHeight());
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
		ylocation = p;
	}   


}
