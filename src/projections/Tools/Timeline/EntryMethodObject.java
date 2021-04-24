package projections.Tools.Timeline;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JColorChooser;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import projections.Tools.Timeline.RangeQueries.Range1D;
import projections.analysis.Analysis;
import projections.analysis.ObjectId;
import projections.analysis.PackTime;
import projections.analysis.TimelineEvent;
import projections.gui.MainWindow;
import projections.gui.U;
import projections.misc.MiscUtil;

class EntryMethodObject implements Comparable, Range1D, ActionListener, MainPanel.SpecialMouseHandler
{

	private long beginTime;
	private int elapsedTime, recvTimeOffset;
	private long cpuBegin;
	private int cpuElapsed;
	// entryPoint is an unsigned short on the tracing side, so use short here to
	// save space. Take care to convert to an unsigned int when using, as Java
	// has no unsigned short type.
	private short entryPoint;
	private int msglen;
	int EventID;
	private ObjectId tid; 
	int pe;
	int pCreation;
	
	private final static String popupChangeColor = "Change Entry Point Color";
	private final static String popupShowDetails = "Show details";
	private final static String popupTraceSender = "Trace message to sender";
	private final static String popupTracePath = "Trace message path";
	private final static String popupDropPEsForObject = "Drop all PEs unrelated to this entry method";
	private final static String popupDropPEsForPE = "Drop all PEs unrelated to entry methods on this PE";
    private final static String loadNeighbors = "Load neighbors";

	private class Extra
	{
		final Integer userSuppliedData;
		final String tleUserEventName;
		final long papiCounts[];
		final long memoryUsage;

		public Extra(TimelineEvent tle) {
			userSuppliedData = tle.UserSpecifiedData;
			memoryUsage = tle.memoryUsage;
			tleUserEventName = tle.userEventName;

			if (tle.numPapiCounts > 0)
				papiCounts = tle.papiCounts;
			else
				papiCounts = null;
		}
	}

	private Extra extraFields;

	/** Total time spent packing in this event */
	private int packtime;

	private Data data = null;
	
	/** A set of TimelineMessage's */
	protected ArrayList<TimelineMessage> messages;
	
	private ArrayList<PackTime> packs;

	private static DecimalFormat format_ = new DecimalFormat();

	private final byte epFlags;
	private static final byte COMM_THD_RECV_MASK = 0x1;
	private static final byte IDLE_EP_MASK = 0x2;
	private static final byte OVERHEAD_EP_MASK = 0x4;

	protected EntryMethodObject(Data data,  TimelineEvent tle, 
			ArrayList<TimelineMessage> msgs, ArrayList<PackTime> packs,
			int p1)
	{
	
		this.data = data;

		pe = p1;
		pCreation = tle.SrcPe;

		byte flags = 0;
		if(data.isCommThd(pe)) {
			final int myNode = data.getNodeID(pe);
			final int creationNode = data.getNodeID(pCreation);
			if (myNode != creationNode)
				flags |= COMM_THD_RECV_MASK;
		}

		switch (tle.EntryPoint) {
			case Analysis.IDLE_ENTRY_POINT:
				flags |= IDLE_EP_MASK;
				break;
			case Analysis.OVERHEAD_ENTRY_POINT:
				flags |= OVERHEAD_EP_MASK;
				break;
			default:
				entryPoint = (short)tle.EntryPoint;
		}

		epFlags = flags;

		beginTime = tle.BeginTime;
		elapsedTime = (int)(tle.EndTime - tle.BeginTime);
		// If the incoming RecvTime is 0, then it is invalid, so use MIN_VALUE to represent it in the offset
		recvTimeOffset = (tle.RecvTime == 0) ? Integer.MIN_VALUE : (int)(tle.RecvTime - tle.BeginTime);
		cpuBegin = tle.cpuBegin;
		cpuElapsed = (int)(tle.cpuEnd - tle.cpuBegin);
		messages  = msgs; // Set of TimelineMessage
		if (messages != null) {
			for (TimelineMessage msg : messages) {
				msg.setSender(this);
			}
		}
		this.packs= packs;

		EventID = tle.EventID;
		msglen = tle.MsgLen;
		if (tle.id != null) {
			tid = new ObjectId(tle.id);
		} else {
			tid = new ObjectId();
		}
		
		if (tle.UserSpecifiedData != null || tle.memoryUsage > 0 ||
				tle.userEventName != null || tle.numPapiCounts > 0) {
			extraFields = new Extra(tle);
		}

		format_.setGroupingUsed(true);

		setPackUsage();		
	} 
	
	/** Dynamically generate the tooltip mouseover text when needed */
	public String getToolTipText(){

		// Construct a nice informative html formatted string about this entry method object. 

		StringBuilder infoString = new StringBuilder(5000);

		final int entry = getEntry();
		if (entry >= 0) {

			infoString.append("<b>" + MainWindow.runObject[data.myRun].getEntryFullNameByID(entry, true) + "</b><br><br>"); 

			if(msglen > 0) {
				infoString.append("<i>Msg Len</i>: " + msglen + "<br>");
			}

			
			infoString.append("<i>Begin Time</i>: " + format_.format(beginTime));
			if (cpuElapsed > 0)
				infoString.append(" (" + format_.format(cpuBegin) + ")");
			infoString.append("<br>");
			
			infoString.append("<i>End Time</i>: " + format_.format(beginTime + elapsedTime) );
			if (cpuElapsed > 0)
				infoString.append(" (" + format_.format(cpuBegin + cpuElapsed) + ")");
			infoString.append("<br>");
			
			infoString.append("<i>Total Time</i>: " + U.humanReadableString(elapsedTime));
			if (cpuElapsed > 0)
				infoString.append(" (" + U.humanReadableString(cpuElapsed) + ")");
			infoString.append("<br>");
			
			infoString.append("<i>Packing</i>: " + U.humanReadableString(packtime));
			if (packtime > 0)
				infoString.append(" (" + (100*(float)packtime/(elapsedTime+1)) + "%)");
			infoString.append("<br>");
			
			if(messages!=null)
				infoString.append("<i>Msgs created</i>: " + messages.size() + "<br>");
			else 
				infoString.append("<i>Msgs created</i>: 0<br>");

			TimelineMessage created_message = this.creationMessage();
			boolean usedCommThreadSender = false;
			if(created_message != null)
			{
				infoString.append("<i>Msg latency is </i>: " + (beginTime - created_message.Time) + "<br>");

				// If the message came from a comm thread, trace back to the actual creator if possible
				if (data.isCommThd(pCreation)) {
					final EntryMethodObject commThreadSender = created_message.getSender();
					if (commThreadSender != null) {
						TimelineMessage origMsg = commThreadSender.creationMessage();
						if (origMsg != null) {
							EntryMethodObject origSender = origMsg.getSender();
							if (origSender != null) {
								infoString.append("<i>Created by </i>: " + data.getPEString(origSender.pe) + " via " + data.getPEString(pCreation) + "<br>");
								usedCommThreadSender = true;
							}
						}
					}
				}
			}
			if (!usedCommThreadSender) {
				infoString.append("<i>Created by </i>: " + data.getPEString(pCreation) + "<br>");
			}
			int dimension = MainWindow.runObject[data.myRun].getSts().getEntryChareDimensionsByID(entry);
			if (dimension < 0) {
				infoString.append("<i>Id</i>: " + tid.id[0] + ":" + tid.id[1] + ":" + tid.id[2] + "<br>");
			} else {
				infoString.append("<i>Id [" + dimension + "D]</i>");
				for (int i = 0; i < dimension; i++) {
					infoString.append(":" + tid.id[i]);
				}
				infoString.append("<br>");
			}
			if(extraFields != null && extraFields.tleUserEventName!=null)
				infoString.append("<i>Associated User Event</i>: "+extraFields.tleUserEventName+ "<br>");
			
			if(recvTimeOffset != Integer.MIN_VALUE){
				infoString.append("<i>Recv Time</i>: " + format_.format(beginTime + recvTimeOffset) + "<br>");
			}	
			
			if (extraFields != null && extraFields.papiCounts != null) {
				infoString.append("<i>*** PAPI counts ***</i>" + "<br>");
				for (int i=0; i<extraFields.papiCounts.length; i++) {
					infoString.append(MainWindow.runObject[data.myRun].getPerfCountNames()[i] + " = " + format_.format(extraFields.papiCounts[i]) + "<br>");
				}
			}
		} else if (isIdleEvent()) {
			infoString.append("<b>Idle Time</b><br><br>");
			infoString.append("<i>Begin Time</i>: " + format_.format(beginTime)+ "<br>");
			infoString.append("<i>End Time</i>: " + format_.format(beginTime + elapsedTime) + "<br>");
			infoString.append("<i>Total Time</i>: " + U.humanReadableString(elapsedTime) + "<br>");
		} else if (isUnaccountedTime()) {
			infoString.append("<i>Unaccounted Time</i>" + "<br>");
			
			infoString.append("<i>Begin Time</i>: " + format_.format(beginTime));

			if (cpuElapsed > 0)
				infoString.append(" (" + format_.format(cpuBegin) + ")");
			infoString.append("<br>");
			
			infoString.append("<i>End Time</i>: " + format_.format(beginTime + elapsedTime));
			if (cpuElapsed > 0)
				infoString.append( " (" + format_.format(cpuBegin + cpuElapsed) + ")");
			infoString.append( "<br>");
			
			infoString.append( "<i>Total Time</i>: " + U.humanReadableString(elapsedTime));
			if (cpuElapsed > 0)
				infoString.append( " (" + (cpuElapsed) + ")");
			infoString.append( "<br>");
			
			infoString.append( "<i>Packing</i>: " + U.humanReadableString(packtime));
			if (packtime > 0) 
				infoString.append( " (" + (100*(float)packtime/(elapsedTime+1)) + "%)");
			infoString.append("<br>");
			
			
			int numMsgs = 0;
			if(messages!=null)
				numMsgs = messages.size();
			infoString.append("<i>Num Msgs created</i>: " + numMsgs + "<br>");
		}

		if(getUserSuppliedData() != null){
			infoString.append("<i>User Supplied Parameter(timestep):</i> " + getUserSuppliedData().intValue() + "<br>");
		}
			
		if(extraFields != null && extraFields.memoryUsage != 0){
			infoString.append("<i>Memory Usage:</i> " + extraFields.memoryUsage/1024/1024 + " MB<br>");
		}
			
		return "<html><body>" + infoString.toString() + "</html></body>";
	}
	
	

	/** paint an entry method that tapers to a point at its left side */
	private void drawLeftArrow(Graphics2D g, Paint c, int startY, int leftCoord, int h)
	{
		int[] xpts = {leftCoord+5, leftCoord+0, leftCoord+5};
		int[] ypts = {startY, startY+h/2, startY+h-1};

		g.setPaint(c);
		g.fillPolygon(xpts, ypts, 3);


		g.setPaint(makeMoreLikeForeground(c));
		g.drawLine(xpts[0], ypts[0], xpts[1], ypts[1]);

		g.setPaint(makeMoreLikeBackground(c));
		g.drawLine(xpts[1], ypts[1], xpts[2], ypts[2]);   
	}   
	
	/** paint an entry method that tapers to a point at its right side */
	private void drawRightArrow(Graphics2D g, Paint c, int startY, int leftCoord, int h, int right)
	{
		int[] xpts = {leftCoord+right-6, leftCoord+right, leftCoord+right-6};
		int[] ypts = {startY, startY+h/2, startY+h-1};

		g.setPaint(c);
		g.fillPolygon(xpts, ypts, 3);

		g.setPaint(makeMoreLikeForeground(c));
		g.drawLine(xpts[0], ypts[0], xpts[1], ypts[1]);

		g.setPaint(makeMoreLikeBackground(c));
		g.drawLine(xpts[1], ypts[1], xpts[2], ypts[2]);
	}
	
	
	private Paint makeMoreLikeBackground(Paint c){
		Color other = data.getBackgroundColor();
		if(c instanceof Color)
			return mixColors((Color)c, other, 0.8f);
		else
			return c;		
	}
	
	private Paint makeMoreLikeForeground(Paint c){
		Color other = data.getForegroundColor();
		if(c instanceof Color)
			return mixColors((Color)c, other, 0.8f);
		else
			return c;
	}
	
	private Color mixColors(Color a, Color b, float ratio){
		float red = (float) (ratio*a.getRed()+(1.0-ratio)*b.getRed());
		float green = (float) (ratio*a.getGreen()+(1.0-ratio)*b.getGreen());
		float blue = (float) (ratio*a.getBlue()+(1.0-ratio)*b.getBlue());	

		return new Color(red/256.0f, green/256.0f, blue/256.0f);		
	}
	
	

	public long getBeginTime()
	{
		return beginTime;
	}   

	public long getEndTime()
	{
		return beginTime + elapsedTime;
	}   

	public int getEntry()
	{
		if (isIdleEvent()) return Analysis.IDLE_ENTRY_POINT;
		else if (isUnaccountedTime()) return Analysis.OVERHEAD_ENTRY_POINT;
		else return Short.toUnsignedInt(entryPoint);
	}   

	public int getEntryIndex()
	{
		return MainWindow.runObject[data.myRun].getEntryIndex(getEntry());
	}
	
	/** Return a set of messages for this entry method */
	public ArrayList<TimelineMessage> getMessages()
	{
		return messages;
	}   
	
//
//	public Dimension getMinimumSize()
//	{
//		return new Dimension(getSize().width, getSize().height);
//	}   

	
	public float getNonPackUsage()
	{
		return getUsage() - getPackUsage();
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
		return packtime * 100.0f / (data.endTime() - data.startTime());
	}   

	public int getPCreation()
	{
		return pCreation;
	}   

	public int getPCurrent()
	{
		return pe;
	}   

	public float getUsage()
	{
		if (isUnaccountedTime()) {
			// if I am not idle or a standard entry method, I do not contribute
			// to the usage
			return 0;
		}

		float usage = elapsedTime;

		if (beginTime < data.startTime()) {
			usage -= (data.startTime() - beginTime);
		}
		if (beginTime + elapsedTime > data.endTime()) {
			usage -= (beginTime + elapsedTime - data.endTime());
		}

		usage /= (data.endTime() - data.startTime());
		usage *= 100;

		return usage;
	}

	
	public void mouseClicked(MouseEvent evt, JPanel parent, Data data)
	{
		if (!isUnaccountedTime() && !isIdleEvent()) {
			if (evt.getModifiers()==MouseEvent.BUTTON1_MASK) {
				// Left Click
            if(data.traceMessagesBackOnHover() || data.traceMessagesForwardOnHover()  || data.traceCriticalPathOnHover()){
					
                System.out.println("mouse left clicked ");
                Set<EntryMethodObject>fwd = traceForwardDependencies(); // this function acts differently depending on data.traceMessagesForwardOnHover()
                Set<EntryMethodObject> back = traceBackwardDependencies();// this function acts differently depending on data.traceMessagesBackOnHover()
                Set<EntryMethodObject> criticalpath = traceCriticalPathDependencies();// this function acts differently depending on data.traceMessagesBackOnHover()

                HashSet<Object> fwdGeneric = new HashSet<Object>();
                HashSet<Object> backGeneric =  new HashSet<Object>();
                HashSet<Object> criticalpathGeneric =  new HashSet<Object>();
                fwdGeneric.addAll(fwd); // this function acts differently depending on data.traceMessagesForwardOnHover()
                backGeneric.addAll(back); // this function acts differently depending on data.traceMessagesBackOnHover()
                criticalpathGeneric.addAll(criticalpath); // this function acts differently depending on data.traceCriticalPathOnHover()

                // Highlight the forward and backward messages
                //data.clearMessageSendLines();
                data.addMessageSendLine(back);
                data.addMessageSendLineAlt(fwd);
                data.addMessageSendLine(criticalpath);
			
                // highlight the objects as well
                data.highlightObjects(fwdGeneric);
                data.highlightObjects(backGeneric);
                data.highlightObjects(criticalpathGeneric);
			
                data.displayMustBeRepainted();
			//needRepaint=true;
		}else{
	
				data.clickTraceSender(this);}	
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
		        
		        menuItem = new JMenuItem(popupTracePath);
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
		            
				menuItem = new JMenuItem(loadNeighbors);
				menuItem.addActionListener(this);
				popup.add(menuItem);

		        popup.show(parent, evt.getX(), evt.getY());			
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
	protected Set<EntryMethodObject> traceBackwardDependencies(){
		synchronized(data.messageStructures){
            int length = 0;
			HashSet<EntryMethodObject> v = new HashSet<EntryMethodObject>();
			if(data.traceMessagesBackOnHover()){
				EntryMethodObject obj = this;
				boolean done;
                long max_time = 0;
                long begin_max = 0;
				do{
                    length++;
					done = true;
					v.add(obj);
                    System.out.println("backward pe " + obj.pe + ", msg time=" + obj.beginTime + ", entry=" + obj.getEntry());
					if (!obj.isIdleEvent() && obj.pCreation <= data.numPEs() && obj.beginTime + obj.elapsedTime > data.leftSelectionTime()  ){
						// Find message that created the object
                        data.addProcessor(obj.pCreation);
						TimelineMessage created_message = obj.creationMessage();
						if(created_message != null){
							if ( obj.beginTime - created_message.Time > max_time) { max_time = obj.beginTime - created_message.Time; begin_max = created_message.Time;}
                            // Find object that created the message
							obj = created_message.getSender();
							if(obj != null){
								done = false;
							}else
                                System.out.println(" create object null");

						}else 
                        {
                            System.out.println(" pcreation create_msg=null");
                            obj = data.getPreviousEntry(obj, obj.pe);
							if(obj != null){
								done = false;
							}
                        }
					}
				}while(!done && length < 8);
			}
			return v;
		}
	}


	/** Trace one level of message sends forward from this object */
	protected Set<EntryMethodObject> traceForwardDependencies(){
		synchronized(data.messageStructures){
			HashSet<EntryMethodObject> v = new HashSet<EntryMethodObject>();
			if(data.traceMessagesForwardOnHover()){
				EntryMethodObject obj = this;
				ArrayList<TimelineMessage> tleMsg = messages;
				
				boolean done = false;
                v.add(obj); //add this object to the set that is returned
				do{
					done = true;
                    System.out.println(" forward pe " + obj.pe + ", msg time=" + obj.beginTime + ", entry=" + obj.getEntry() + "forwarding msgs:" + tleMsg.size());
					if (!obj.isIdleEvent() && obj.pCreation <= data.numPEs() && tleMsg != null && !tleMsg.isEmpty()){
						for(int j=0; j<tleMsg.size(); j++)
                        {
                        // Find messages called by current entry method
						TimelineMessage msgToCalledEntryMethod = tleMsg.get(j);
						if(msgToCalledEntryMethod != null){
							//if there is a mapping for this message, find objects that are called by this message.
							//if this object isn't null or equal to this, go through while loop again
                            //data.addProcessor( mm); 
							List<EntryMethodObject> objset = msgToCalledEntryMethod.getRecipients();
                            //System.out.println("fowarding  " + j + "; obj ");
                            //msgToCalledEntryMethod.printMe();

							if (objset!=null && !objset.isEmpty()) {
								Iterator<EntryMethodObject> i = objset.iterator();
								obj = i.next();
                                //System.out.println("not empty fowarding  " + j );
								if(obj != null && obj!=this){
									//done = false;
									//tleMsg=obj.TLmsgs;
                                    v.add(obj);
                                    //System.out.println("finally not empty fowarding  " + j );
								}
							}
						}
                        }
                    }
				}while(!done);
			}
			v.remove(this);
			return v;
		}
	}

	
    /** Iteratively trace the critical path that 
	 * led to this entry method, without loading any 
	 * additional processor timelines 
	 * 
	 * return a set of entry method objects and 
	 * TimelineMessage objects associated with the 
	 * trace
	 * 
	 */
	protected Set<EntryMethodObject> traceCriticalPathDependencies(){
		synchronized(data.messageStructures){
            int length = 0;
			HashSet<EntryMethodObject> v = new HashSet<EntryMethodObject>();
			if(data.traceCriticalPathOnHover()){
				EntryMethodObject obj = this;
				EntryMethodObject previous_obj = null;
				boolean done;
				do{
                    length++;
					done = true;
					v.add(obj);
                    System.out.println(" pe " + obj.pe + ", msg time=" + obj.beginTime + ", entry=" + obj.getEntry());
					if (!obj.isIdleEvent() && obj.pe <= data.numPEs() && obj.beginTime + obj.elapsedTime > data.leftSelectionTime()  ){
						// Find message that created the object
                        previous_obj = data.getPreviousEntry(obj, obj.pe);
                        if( previous_obj!= null && !previous_obj.isIdleEvent()){
                            System.out.println("It has previous entry");
                            obj = previous_obj; 
                            done = false;
                        }else
                        {
                            data.addProcessor(obj.pCreation);
						    TimelineMessage created_message = obj.creationMessage();
                            if(created_message != null) {
                                obj = created_message.getSender();
                                if(obj != null){
                                    System.out.println("Switch to other processor");
                                    done = false;
                                }
                            }
                        }
                    }
				}while(!done && length < 20);
			}
			return v;
		}
	}


	
	/** Return the message that caused the entry method to execute. Complexity=O(1) time */
	protected TimelineMessage creationMessage(){
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
				return (TimelineMessage) data.messageStructures.getEventIDToMessageMap()[pCreation].get(Integer.valueOf(EventID));

		}
	}
	
	protected TimelineMessage currentMessage() {
		synchronized(data.messageStructures){
			if(data == null)
				return null;
			else if(pe<0)
				return null;
			else if(data.messageStructures.getEventIDToMessageMap() == null)
				return null;
			else if(pe >= data.messageStructures.getEventIDToMessageMap().length)
				return null;
			else if(data.messageStructures.getEventIDToMessageMap()[pe] == null)
				return null;
			else
				return (TimelineMessage) data.messageStructures.getEventIDToMessageMap()[pe].get(Integer.valueOf(EventID));

		}
	}
	
	

	public void mouseEntered(MouseEvent evt)
	{   
		boolean needRepaint = false;
		
		// Highlight the messages linked to this object
		/*if(data.traceMessagesBackOnHover() || data.traceMessagesForwardOnHover()){
						
			fwd = traceForwardDependencies(); // this function acts differently depending on data.traceMessagesForwardOnHover()
			back = traceBackwardDependencies();// this function acts differently depending on data.traceMessagesBackOnHover()
				
		//	HashSet<Object> fwdGeneric = new HashSet<Object>();
		//	HashSet<Object> backGeneric =  new HashSet<Object>();
			fwdGeneric.addAll(fwd); // this function acts differently depending on data.traceMessagesForwardOnHover()
			backGeneric.addAll(back); // this function acts differently depending on data.traceMessagesBackOnHover()
						
			// Highlight the forward and backward messages
			data.clearMessageSendLines();
			data.addMessageSendLine(back);
			data.addMessageSendLineAlt(fwd);
			
			// highlight the objects as well
			data.highlightObjects(fwdGeneric);
			data.highlightObjects(backGeneric);
			
			needRepaint=true;
		}
			
		*/
		// Highlight any Entry Method invocations for the same chare array element
		if(data.traceOIDOnHover()){
			synchronized(data.messageStructures){
			Set allWithSameId = (Set) data.messageStructures.getOidToEntryMethodObjectsMap().get(tid);
			data.highlightObjects(allWithSameId);
			needRepaint=true;
			}
		}	

		
		if(needRepaint)
			data.displayMustBeRepainted();
		
	}   


	public void mouseExited(MouseEvent evt)
	{
        /*
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
		*/
	}   

//
//	public void mousePressed(MouseEvent evt)
//	{
//		// ignore 	
//	}   
//
//	public void mouseReleased(MouseEvent evt)
//	{
//		// ignore 	
//	}   

	private void OpenMessageWindow()
	{
		MessageWindow msgwindow = new MessageWindow(this);
		Dimension d = msgwindow.getPreferredSize();
		msgwindow.setSize(480, d.width);
		msgwindow.setVisible(true);
	} 
	
	/** Is this an idle event */
	public boolean isIdleEvent(){
		return getFlag(IDLE_EP_MASK);
	}
	
	public boolean isUnaccountedTime(){
		return getFlag(OVERHEAD_EP_MASK);
	}
	
	
/** Whether this object is displayed or hidden (for example when idle's are not displayed this might be false) */
	public boolean isDisplayed() {
		// If it is hidden, we may not display it
		if(data.entryIsHiddenID(getEntry())) {
			return false;
		}
		
		// If this is an idle time region, we may not display it
		if (isIdleEvent() && (!data.showIdle() || MainWindow.IGNORE_IDLE)) {
			return false;
		}

		return true;
	}
	
	
	public boolean paintMe(Graphics2D g2d, int actualDisplayWidth, MainPanel.MaxFilledX maxFilledX){
		boolean paintedEP = false;
		// If it is hidden, we may not display it
		if(!isDisplayed()){
			return paintedEP;
		}
		
		final long endTime = beginTime + elapsedTime;
		int leftCoord = data.timeToScreenPixel(beginTime, actualDisplayWidth);
		int rightCoord = data.timeToScreenPixel(endTime, actualDisplayWidth);

		if(endTime > data.endTime())
			rightCoord = data.timeToScreenPixelRight(data.endTime(), actualDisplayWidth);

		if(beginTime < data.startTime())
			leftCoord = data.timeToScreenPixelLeft(data.startTime(), actualDisplayWidth);

		int topCoord = data.entryMethodLocationTop(pe);
//		int height = data.entryMethodLocationHeight();

		// Determine the coordinates and sizes of the components of the graphical representation of the object
		int rectWidth = Math.max(1, rightCoord - leftCoord + 1);
		int rectHeight = data.barheight();

		int left  = leftCoord+0;
		int right = leftCoord+rectWidth-1;

		// The distance from the top or bottom to the rectangle
		int verticalInset = 0;

		final boolean isIdle = isIdleEvent();
		// Idle regions are thinner vertically
		if (isIdle && data.getViewType() != Data.ViewType.VIEW_SUPERCOMPACT) {
			rectHeight -= 7;
			verticalInset += 3;
		}

		// Only draw this EMO if it covers some pixel that hasn't been filled yet
		if (right > maxFilledX.ep) {
			maxFilledX.ep = right;
			paintedEP = true;

			// Determine the base color
			Paint c = determineColor();


			// Dim this object if we want to focus on some objects (for some reason or another)
			if (data.isObjectDimmed(this)) {
				if (isIdle) c = Color.lightGray;
				else c = makeMoreLikeBackground(c);
			}


			if (beginTime < data.startTime()) {
				drawLeftArrow(g2d, c, topCoord + verticalInset, leftCoord, rectHeight);
				rectWidth -= 5;
				left += 5;
			}

			if (endTime > data.endTime()) {
				drawRightArrow(g2d, c, topCoord + verticalInset, leftCoord, rectHeight, rectWidth);
				rectWidth -= 5;
				right -= 5;
			}

			// Paint the main rectangle for the object, as long as it is not a skinny idle event
			g2d.setPaint(c);
			if (rectWidth > 1 || !isIdle) {
				g2d.fillRect(left, topCoord + verticalInset, rectWidth, rectHeight);
//			System.out.println("Entry method painting at (" + left + "," + (topCoord+verticalInset) + "," +  rectWidth + "," + rectHeight + ")");
				if (isCommThreadMsgRecv()) {
					g2d.setColor(data.getForegroundColor());
					g2d.fillRect(left, topCoord + verticalInset + rectHeight, rectWidth, data.smpMessageRecvBarHeight());
				}
			}

			// Paint the edges of the rectangle lighter/darker to give an embossed look
			if (rectWidth > 2 && !data.colorByMemoryUsage() && rectHeight > 1) {
				g2d.setPaint(makeMoreLikeForeground(c));
				g2d.drawLine(left, topCoord + verticalInset, right, topCoord + verticalInset);
				if (left == leftCoord)
					g2d.drawLine(left, topCoord + verticalInset, left, topCoord + verticalInset + rectHeight - 1);

				g2d.setPaint(makeMoreLikeBackground(c));
				g2d.drawLine(left, topCoord + verticalInset + rectHeight - 1, right, topCoord + verticalInset + rectHeight - 1);
				if (right == rectWidth - 1)
					g2d.drawLine(right, topCoord + verticalInset, right, topCoord + verticalInset + rectHeight - 1);
			}
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
			g2d.setColor(Color.pink);
			for(PackTime pt : packs){
				long packBeginTime = pt.BeginTime;
				long packEndTime = pt.EndTime;

				if(packEndTime >= data.startTime() && packBeginTime <= data.endTime())
				{

					// Compute the begin pixel coordinate relative to the containing panel
					int packBeginCoordX = data.timeToScreenPixelLeft(packBeginTime);

					// Compute the end pixel coordinate relative to the containing panel
					int packEndCoordX = data.timeToScreenPixelRight(packEndTime);

					if (packEndCoordX > maxFilledX.pack) {
						maxFilledX.pack = packEndCoordX;
						g2d.fillRect(packBeginCoordX, topCoord + verticalInset + rectHeight, (packEndCoordX - packBeginCoordX + 1), data.messagePackHeight());
					}

				}
			}
		}

		// Show the message sends. See note above for the message packing areas
		// Don't change this without changing MainPanel's paintComponent which draws message send lines
		if(data.showMsgs() == true && messages != null)
		{
			g2d.setColor(data.getForegroundColor());
			
			Iterator<TimelineMessage> m = messages.iterator();
			while(m.hasNext()){
				TimelineMessage msg = m.next();
				long msgtime = msg.Time;
				if(msgtime >= data.startTime() && msgtime <= data.endTime())
				{
					// Compute the pixel coordinate relative to the containing panel
					int msgCoordX = data.timeToScreenPixel(msgtime);
					if (msgCoordX > maxFilledX.msg) {
						maxFilledX.msg = msgCoordX;
						g2d.drawLine(msgCoordX, topCoord + verticalInset + rectHeight, msgCoordX, topCoord + verticalInset + rectHeight + data.messageSendHeight());
					}
				}
			}
		}

		return paintedEP;
	}

		
	/**  Determine the color of the object */	
	private Paint determineColor() {
		Color colToSave = null;
		
		// First handle the simple cases of idle, unknown and function events
		if (isIdleEvent()) { 	
			return MainWindow.runObject[data.myRun].getIdleColor();
		} else if (isUnaccountedTime()) { // unknown domain
			return MainWindow.runObject[data.myRun].getOverheadColor();
		}
		
		// color the objects by memory usage with a nice blue - red gradient
		if(data.colorByMemoryUsage()){
			if(extraFields == null || extraFields.memoryUsage == 0){
				colToSave = Color.darkGray;
			}else{
				// scale the memory usage to the interval [0,1]
				float normalizedValue = (float)(extraFields.memoryUsage - data.minMemBColorRange()) / (float)(data.maxMemBColorRange()-data.minMemBColorRange());
				if( normalizedValue<0.0 || normalizedValue>1.0 )
					colToSave = Color.darkGray;
				else {
					colToSave = Color.getHSBColor(0.6f-normalizedValue*0.65f, 1.0f, 1.0f); 
				}
			}
		}


		// color the objects by user supplied values with a nice blue gradient
		if(data.colorByUserSupplied() && data.colorSchemeForUserSupplied==Data.ColorScheme.BlueGradientColors){
			if(getUserSuppliedData() !=  null){
				long value = getUserSuppliedData().longValue();
				float normalizedValue = (float)(value - data.minUserSupplied) / (float)(data.maxUserSupplied-data.minUserSupplied);
				colToSave = Color.getHSBColor(0.25f-normalizedValue*0.75f, 1.0f, 1.0f); 
			} 	else {
				colToSave = Color.darkGray;
			}
		}

		// Sometimes Overrule the normal colors and use one based on the chare array index
		if( data.colorByOID() || data.colorByUserSupplied() || data.colorByEID()){

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
				color += (getEntry() * 251) % 5113;
			}


			if(data.colorByUserSupplied() && getUserSuppliedData() != null){
				color += (getUserSuppliedData() * 359) % 4903;
			}


			if(data.colorByMemoryUsage() && extraFields != null && extraFields.memoryUsage != 0){
				color += (extraFields.memoryUsage * 6121) % 5953;
			}

			 // Should range from 0.0 to 2.0
			 float h2 = ((color+512) % 512) / 256.0f;
			 // Should range from 0.0 to 1.0
			 float h = ((color+512) % 512) / 512.0f;

			
			
			float s = 1.0f;   // Should be 1.0

			float b = 1.0f;   // Should be 0.5 or 1.0

			if(h2 > 1.0)
				b = 0.6f;

			colToSave = Color.getHSBColor(h, s, b);

		}
		if (colToSave == null) {
			return MainWindow.runObject[data.myRun].entryColors[getEntryIndex()];
		}
		else {
			MainWindow.runObject[data.myRun].entryColors[getEntryIndex()] = colToSave;
			return colToSave;
		}


	}

	
//	
//	public void setLocationAndSize(int actualDisplayWidth)
//	{
//		
//		if(data.entryIsHiddenID(entry)){
//			setBounds( 0, 0, 0, 0 );			
//			return;
//		}	
//		
//		
//		leftCoord = data.timeToScreenPixel(beginTime, actualDisplayWidth);
//		rightCoord = data.timeToScreenPixel(endTime, actualDisplayWidth);
//
//		if(endTime > data.endTime())
//			rightCoord = data.timeToScreenPixelRight(data.endTime(), actualDisplayWidth);
//
//		if(beginTime < data.startTime())
//			leftCoord = data.timeToScreenPixelLeft(data.startTime(), actualDisplayWidth);
//		
//		int width = rightCoord-leftCoord+1;
//
//		if(width < 1)
//			width = 1;
//		
////		int singleTimelineH = data.singleTimelineHeight();
//		
////		this.setBounds(leftCoord,  whichTimelineVerticalIndex()*singleTimelineH,
////				width, singleTimelineH);
//		
//		this.setBounds(leftCoord,  data.entryMethodLocationTop(pCurrent),
//				width, data.entryMethodLocationHeight());
//	
//	}  
//	
//	
	
//	public int whichTimelineVerticalIndex(){
//		return data.whichTimelineVerticalPosition(pCurrent);
//	}
	

	private void setPackUsage()
	{
		packtime = 0;
		if(packs != null)
		{   
			for(PackTime pt : packs){
				// packtime += packs[p].EndTime - packs[p].BeginTime + 1;
				packtime += pt.EndTime - pt.BeginTime;
				if(pt.BeginTime < data.startTime())
					packtime -= (data.startTime() - pt.BeginTime);
				if(pt.EndTime > data.endTime())
					packtime -= (pt.EndTime - data.endTime());
			}
		}
	}   
	
	@Override
	public int compareTo(Object o) {
		EntryMethodObject obj = (EntryMethodObject) o;
		if(pCreation != obj.pCreation)
			return MiscUtil.sign(pCreation-obj.pCreation);
		else if(pe != obj.pe)
			return MiscUtil.sign(pe - obj.pe);
		else
			return MiscUtil.sign(EventID - obj.EventID);
	}

	public ObjectId getTid() {
		return tid;
	}

	public boolean isCommThreadMsgRecv(){
		return getFlag(COMM_THD_RECV_MASK);
	}

	/** Shift all the times associated with this entry method by given amount */
	@Override
	public void shiftTimesBy(long s){
		beginTime += s;
		cpuBegin += s;

		if(messages != null){
			for(TimelineMessage msg : messages){
				msg.shiftTimesBy(s);
			}
		}
		
	}


	/** Handle the right-click popup menu events */
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() instanceof JMenuItem) {
			String arg = ((JMenuItem) e.getSource()).getText();
			if (arg.equals(popupChangeColor)){
				final int entry = getEntry();
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
			else if(arg.equals(popupTracePath)) {
				data.clickTraceSender(this);				
			} 
			else if(arg.equals(popupDropPEsForObject)) {	
				data.dropPEsUnrelatedToObject(this);
			} 
			else if(arg.equals(popupDropPEsForPE)) {
				data.dropPEsUnrelatedToPE(this.pe);
			}
            else if (arg.equals(loadNeighbors))
            {
                data.addNeighbors(this.pe);
            }

		}

	}

	public ArrayList<TimelineMessage> getTLmsgs() {
		return messages;
	}

	@Override
	public long lowerBound() {
		return beginTime;
	}	

	@Override
	public long upperBound() {
		return beginTime + elapsedTime;
	}

	@Override
	public void mouseMoved(MouseEvent evt) {
		// TODO Auto-generated method stub
		
	}

	/** Data specified by the user, likely a timestep. Null if nonspecified */
	public Integer getUserSuppliedData() {
		return (extraFields == null) ? null :  extraFields.userSuppliedData;
	}

	private boolean getFlag(final byte mask)
	{
		return (epFlags & mask) != 0;
	}
}
