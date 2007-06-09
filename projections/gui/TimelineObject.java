package projections.gui;

import java.awt.*;
import java.awt.event.*;
import projections.analysis.*;
import java.util.Vector;
import java.text.DecimalFormat;

public class TimelineObject extends Component
   implements MouseListener
{
    private String[]  bubbletext;
    private Bubble  bubble;
    private TimelineMessageWindow msgwindow;
    private long    beginTime, endTime, recvTime;
    private long cpuTime;
    private long cpuBegin, cpuEnd;
    private int     entry;
    private int     msglen;
    private int EventID;
    private ObjectId tid;
    private boolean inside = false; 
    private int pCurrent, pCreation;
    private double  usage;
    private float packusage;
    private long packtime;
    
    private TimelineData data;
    public TimelineMessage[] messages;
    private PackTime[] packs;
    // private UserEvent[] userEvents;

    private int numPapiCounts = 0;
    private long papiCounts[];

    private boolean isFunction = false;
    
  private static DecimalFormat format_ = new DecimalFormat();
  double scale;
  int left;
  int h,startY;
  // if line from message creation already exists
  private int creationLine;
  
  private TimelineMessage created_message;
    public TimelineObject(TimelineData data, TimelineEvent tle, 
			  TimelineMessage[] msgs, PackTime[] packs,
			  int p1)
    {
	format_.setGroupingUsed(true);

	setBackground(Analysis.background);
	setForeground(Analysis.foreground);
	
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
	numPapiCounts = tle.numPapiCounts;
	papiCounts    = tle.papiCounts;

	int n = tle.EntryPoint;
	isFunction = tle.isFunction;

	setUsage();
	setPackUsage();
	  
	// **CW** special treatment for functions. There really should
	// be a general way of dealing with this.
	if (isFunction) {
	    int textIndex = 0;
	    int textSize = 7+(tle.callStack.size()*2);

	    bubbletext = new String[textSize];
	    bubbletext[textIndex++] = "Function: " +
		Analysis.getFunctionName(entry);
	    bubbletext[textIndex++] = "Begin Time: " + 
		format_.format(beginTime);
	    bubbletext[textIndex++] = "End Time: " +
		format_.format(endTime);
	    bubbletext[textIndex++] = "Total Time: " +
		U.t(endTime-beginTime);
	    bubbletext[textIndex++] = "Msgs created: " + msgs.length;
	    bubbletext[textIndex++] = "Id: " + tid.id[0] + ":" + tid.id[1] + 
		":" + tid.id[2];
	    bubbletext[textIndex++] = "--------- Function Callstack --------";

	    // consume the call stack
	    while (!tle.callStack.empty()) {
		AmpiFunctionData functionData = 
		    (AmpiFunctionData)tle.callStack.pop();
		bubbletext[textIndex++] = 
		    "[Func]: " + Analysis.getFunctionName(functionData.FunctionID);
		bubbletext[textIndex++] =
		    "   line:" + functionData.LineNo + " file: " +
		    functionData.sourceFileName;
	    }
	} else if (n >= 0) {
	    int textIndex = 0;
	    int textSize = 10;
	    if (numPapiCounts > 0) {
		bubbletext = new String[textSize+numPapiCounts+1];
	    } else {
		bubbletext = new String[textSize];
	    }
	    int ecount = Analysis.getNumUserEntries();
	    if (n >= ecount) {
		System.out.println("Fatal error: invalid entry " + n +
				   " on processor " + pCurrent + "!");
		System.exit(1) ;
	    }
	    bubbletext[textIndex++] = (Analysis.getEntryNames())[n][1] + 
		"::" + (Analysis.getEntryNames())[n][0]; 
	    bubbletext[textIndex++] = "Msg Len: " + msglen;
	    bubbletext[textIndex] = "Begin Time: " + format_.format(beginTime);
	    if (cpuTime > 0) {
		bubbletext[textIndex++] += " (" + 
		    format_.format(cpuBegin) + ")";
	    } else {
		textIndex++;
	    }
	    bubbletext[textIndex] = "End Time: " + format_.format(endTime);
	    if (cpuTime > 0) {
		bubbletext[textIndex++] += " (" + 
		    format_.format(cpuEnd) + ")";
	    } else {
		textIndex++;
	    }
	    bubbletext[textIndex] = "Total Time: " + U.t(endTime-beginTime);
	    if (cpuTime > 0) {
		bubbletext[textIndex++] += " (" + 
		    U.t(cpuTime) + ")";
	    } else {
		textIndex++;
	    }
	    bubbletext[textIndex] = "Packing: " + U.t(packtime);
	    if (packtime > 0) {
		bubbletext[textIndex++] += " (" + 
		    (100*(float)packtime/(endTime-beginTime+1)) + "%)";
	    } else {
		textIndex++;
	    }
	    bubbletext[textIndex++] = "Msgs created: " + msgs.length;
	    bubbletext[textIndex++] = "Created by processor " + pCreation;
	    bubbletext[textIndex++] = "Id: " + tid.id[0] + ":" + tid.id[1] + 
		":" + tid.id[2];
	    bubbletext[textIndex++] = "Recv Time: " + recvTime;
	    if (numPapiCounts > 0) {
		bubbletext[textIndex++] = "*** PAPI counts ***";
		for (int i=0; i<numPapiCounts; i++) {
		    bubbletext[textIndex++] = Analysis.getPerfCountNames()[i] +
			" = " + format_.format(papiCounts[i]);
		    /* Dropping the hack - some more general way of
		       intelligently performing derived analysis of raw
		       counters should be developed
		    // hack for now
		    bubbletext[textIndex] = Analysis.getPerfCountNames()[i] +
			" = " + format_.format(papiCounts[i]);
		    if (i == 0) {
			// processor count
			bubbletext[textIndex++] += "   FLOPS = " +
			    format_.format((long)(papiCounts[i]/
						  ((endTime-beginTime)/1000000.0)));
		    }
		    if (i == 1) {
			// cache miss
			100*(float)(papiCounts[i]/(papiCounts[i-1]*1.0)) +
			"%";
		    }
		    */
		}
	    }
	} else if (n == -1) {
	    int textIndex = 0;
	    int textSize = 4;
	    bubbletext = new String[textSize];
	    bubbletext[textIndex++] = "IDLE TIME";
	    bubbletext[textIndex++] = "Begin Time: " + 
		format_.format(beginTime);
	    bubbletext[textIndex++] = "End Time: " + format_.format(endTime);
	    bubbletext[textIndex++] = "Total Time: " + U.t(endTime-beginTime);
	} else if (n == -2) {
	    int textIndex = 0;
	    int textSize = 6;
	    bubbletext = new String[textSize];
	    bubbletext[textIndex++] = "Unaccounted Time";
	    bubbletext[textIndex] = "Begin Time: " + format_.format(beginTime);
	    if (cpuTime > 0) {
		bubbletext[textIndex++] += " (" + 
		    format_.format(cpuBegin) + ")";
	    } else {
		textIndex++;
	    }
	    bubbletext[textIndex] = "End Time: " + format_.format(endTime);
	    if (cpuTime > 0) {
		bubbletext[textIndex++] += " (" + 
		    format_.format(cpuEnd) + ")";
	    } else {
		textIndex++;
	    }
	    bubbletext[textIndex] = "Total Time: " + U.t(endTime-beginTime);
	    if (cpuTime > 0) {
		bubbletext[textIndex++] += " (" + 
		    U.t(cpuTime) + ")";
	    } else {
		textIndex++;
	    }
	    bubbletext[textIndex] = "Packing: " + U.t(packtime);
	    if (packtime > 0) {
		bubbletext[textIndex++] += " (" + 
		    (100*(float)packtime/(endTime-beginTime+1)) + "%)";
	    } else {
		textIndex++;
	    }
	    bubbletext[textIndex++] = "Msgs created: " + msgs.length;
	}
	addMouseListener(this);
    } 

   public void CloseMessageWindow()
   {
	  msgwindow = null;
   }   

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

   private void drawRightArrow(Graphics g, Color c, int startY, int h, int w)
   {
	  int[] xpts = {w-6, w, w-6};
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

   public Dimension getPreferredSize()
   {
	  return new Dimension(getSize().width, getSize().height);
   }   

   public float getUsage()
   {
	  return (float)usage;
   }   

    public void mouseClicked(MouseEvent evt)
    {
	if (entry >= 0) {
	    if (evt.getModifiers()==MouseEvent.BUTTON1_MASK) {
		OpenMessageWindow();
	    } else {	
		if (creationLine<2) {
		    if (data.mesgVector[pCreation] == null || 
			data.mesgVector[pCreation].isEmpty()) {
			// if the processor at which this message was 
			// not created load it. 
			if (data.mesgVector[pCreation]==null) {
			    data.addProcessor(pCreation);
			    // if the message send is within the time 
			    // interval draw the blue line
			    created_message = 
				searchMesg(data.mesgVector[pCreation],EventID);
			    if (created_message != null) {
				data.drawConnectingLine(pCreation,
							created_message.Time,
							pCurrent,beginTime,
							this,creationLine);
				creationLine = 2;
			    }
			}
		    } else {
			if (creationLine == 0) {
			    created_message = 
				searchMesg(data.mesgVector[pCreation],EventID);
			}
			if (created_message != null) {
			    data.drawConnectingLine(pCreation,
						    created_message.Time,
						    pCurrent,beginTime,this,
						    creationLine);
			    creationLine = 2;
			}
		    }
		} else {
		    data.drawConnectingLine(pCreation,created_message.Time,
					    pCurrent,beginTime,this,
					    creationLine);
		    creationLine = 1;
		}
	    }	
	}
    } 
    
    public void clearCreationLine() {
	creationLine = 0;
	created_message = null;
    }
    
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
	    return returnItem;
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
	  if ((entry == -1 && data.showIdle == false) ||
	      (entry == -1 && MainWindow.IGNORE_IDLE)) {
		 return;
	  }

	  if(!inside)
	  {
		 inside = true;
		 TimelineObject to = TimelineObject.this;
		 Point scrnloc = to.getLocationOnScreen();
		 Dimension size = getSize();
	  
		 if(bubble == null)
			bubble = new Bubble(this, bubbletext);
		 bubble.setLocation(scrnloc.x + evt.getX(), scrnloc.y + size.height + 2);
		 bubble.setVisible(true);
	  }     
   }   

   public void mouseExited(MouseEvent evt)
   {
	  if(inside)
	  {
		 if(bubble != null)
		 {
			bubble.dispose();
			bubble = null;
		 }   
		 inside = false;
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
		 msgwindow = new TimelineMessageWindow(this);
		 Dimension d = msgwindow.getPreferredSize();
		 msgwindow.setSize(480, d.width);
	  }
	  
	  msgwindow.setVisible(true);
   } 
	
   private Color getObjectColor(ObjectId tid){
   	int r;
	int g;
	int b;
	int nPE = data.processorList.size();
	int d = (int )(255/(double )nPE);
	r = ((tid.id[0])*d) % 255;
	g = ((tid.id[1])*23) % 255;
	b = 255-g;
	//b = 56;
	return new Color(r, g,b );
   }
   
   public void paint(Graphics g)
   {     
	  if ((entry == -1 && data.showIdle == false) ||
	      (entry == -1 && MainWindow.IGNORE_IDLE)) {
		 return;
	  }

	  Color c;
	  
	  if (entry == -1) {
	      c = Color.white;
	      // c = getForeground();
	      // System.out.println("entry is -1 ");
	  } else if (entry == -2) { // unknown domain
	      c = getBackground();
	  }  else {
		if(data.colorbyObjectId){
			//System.out.println("Should be colored by ObjectId");
			c = getObjectColor(tid);
		}else{
			c = data.entryColor[entry];
			if (isFunction) {
			    c = Analysis.getFunctionColor(entry);
			}
		}	
	  }
	  // leave 5 pixels above and below
	  startY = 5;
	  int w      = getSize().width;
	  h      = getSize().height - 10;
	  
	  left  = 0;
	  int right = w-1;
	  
	  long viewbt = beginTime;
	  long viewet = endTime;
		 
	  if(beginTime < data.beginTime)
	  {
		 drawLeftArrow(g, c, startY, h);
		 left = 5;
		 viewbt = data.beginTime;
	  }
		 
	  if(endTime > data.endTime)
	  {
		 drawRightArrow(g, c, startY, h, w);
		 right = w-6;
		 viewet = data.endTime;
	  }
		 
	  int pixelwidth = right-left+1;
	  /*
	  if (isFunction) {
	      g.setColor(c);
	      for (int x=0; x<w+h-2; x += 4) {
		  g.drawLine(x, startY, x-h, startY+h);
		  g.drawLine(x+1, startY, x-h+1, startY+h);
	      }
	  } else {
	      g.setColor(c);
	      g.fillRect(left, startY, pixelwidth, h);
	  }
	  */

	  g.setColor(c);
	  g.fillRect(left, startY, pixelwidth, h);

	  /*
	  if(entry == -1)
	  {
		 g.setColor(getBackground());
		 for(int x=0; x<w+h-2; x += 4)
		 {
			g.drawLine(x, startY, x-h, startY+h);
			g.drawLine(x+1, startY, x-h+1, startY+h);
		 }
	  }
	  */

	  if(w > 2)
	  {
		 g.setColor(c.brighter());
		 g.drawLine(left, startY, right, startY);
		 if(left == 0)
			g.drawLine(0, startY, 0, startY+h-1);
		 
		 g.setColor(c.darker());
		 g.drawLine(left, startY+h-1, right, startY+h-1);
		 if(right == w-1)
			g.drawLine(w-1, startY, w-1, startY+h-1);
	  }
	  
	  scale= pixelwidth /((double)(viewet - viewbt + 1)); 

	  if(data.showMsgs == true && messages != null)
	  {
		 g.setColor(getForeground());
		 for(int m=0; m<messages.length; m++)
		 {
			long msgtime = messages[m].Time;
			if(msgtime >= data.beginTime && msgtime <= data.endTime)
			{
			   int pos = (int)((msgtime - viewbt) * scale);
			   if(beginTime < data.beginTime)
				  pos += 5;
			   g.drawLine(pos, startY+h, pos, startY+h+5);
			}
		 }
	  }               

	  if(data.showPacks == true && packs != null)
	  {
		 g.setColor(Color.pink);
		 for(int p=0; p<packs.length; p++)
		 {
			long pbt = packs[p].BeginTime;
			long pet = packs[p].EndTime;
			
			if(pet >= data.beginTime && pbt <= data.endTime)
			{
			   int pos = (int)((pbt - viewbt) * scale);
			   if(beginTime < data.beginTime)
				  pos += 5;
			   g.fillRect(pos, startY+h, (int)(pet-pbt+1), 3);
			}
		 }
	  }               

   }   

   public void print(Graphics pg, long minx, long maxx, double pixelIncrement, int timeIncrement)
   {
	  if ((entry == -1 && data.showIdle == false) ||
	      (entry == -1 && MainWindow.IGNORE_IDLE)) {
		 return;
	  }

	  Color c;
	  
	  if(entry == -1)
		 c = Color.black;
	  else
		 c = data.entryColor[entry];
	  
	  int w = (int)((endTime - beginTime + 1) * pixelIncrement / timeIncrement);
	  if(w < 1) w = 1;
	  if(beginTime < minx)
		 w -= (int)((minx - beginTime) * pixelIncrement / timeIncrement) - 5;
	  if(endTime > maxx)
		 w -= (int)((endTime - maxx) * pixelIncrement / timeIncrement) - 5; 
	 
	  int h = 20;
	  Rectangle r = pg.getClipBounds();
	  pg.setClip(0, 0, w, 25);
	  
	  int left  = 0;
	  int right = w-1;
	  
	  long viewbt = beginTime;
	  long viewet = endTime;
		 
	  if(beginTime < minx)
	  {
		 drawLeftArrow(pg, c, 0, h);
		 left = 5;
		 viewbt = minx;
	  }
		 
	  if(endTime > maxx)
	  {
		 drawRightArrow(pg, c, 0, h, w);
		 right = w-6;
		 viewet = maxx;
	  }
		 
	  pg.setColor(c);
	  
	  int pixelwidth = right-left+1;
	  pg.fillRect(left, 0, pixelwidth, h);
	  
	  if(entry == -1)
	  {
		 pg.setColor(Color.white);
		 for(int x=0; x<w+h-2; x += 4)
		 {
			pg.drawLine(x, 0, x-h, h);
			pg.drawLine(x+1, 0, x-h+1, h);
		 }
	  }

	  if(w > 2)
	  {
		 pg.setColor(c.brighter());
		 pg.drawLine(left, 0, right, 0);
		 if(left == 0)
			pg.drawLine(0, 0, 0, h-1);
		 
		 pg.setColor(c.darker());
		 pg.drawLine(left, h-1, right, h-1);
		 if(right == w-1)
			pg.drawLine(w-1, 0, w-1, h-1);
	  }
	  
	  
	  double scale= pixelwidth /((double)(viewet - viewbt + 1)); 

	  if(data.showMsgs == true && messages != null)
	  {
		 pg.setColor(Color.black);
		 for(int m=0; m<messages.length; m++)
		 {
			long msgtime = messages[m].Time;
			if(msgtime >= minx && msgtime <= maxx)
			{
			   int pos = (int)((msgtime - viewbt) * scale);
			   if(beginTime < minx)
				  pos += 5;
			   pg.drawLine(pos, h, pos, h+5);
			}
		 }
	  }               

	  if(data.showPacks == true && packs != null)
	  {
		 pg.setColor(Color.pink);
		 for(int p=0; p<packs.length; p++)
		 {
			long pbt = packs[p].BeginTime;
			long pet = packs[p].EndTime;
			
			if(pet >= minx && pbt <= maxx)
			{
			   int pos = (int)((pbt - viewbt) * scale);
			   if(beginTime < minx)
				  pos += 5;
			   pg.fillRect(pos, h, (int)(pet-pbt+1), 3);
			}
		 }
	  }

	  pg.setClip(r);               
   }   

   public void setBounds(int ylocation)
   {
	  long BT, ET;
	  
	  if(endTime > data.endTime)
		 ET = data.endTime - data.beginTime;
	  else
		 ET = endTime - data.beginTime;

	  if(beginTime < data.beginTime) 
		 BT = 0;
	  else BT = beginTime - data.beginTime;

	  int BTS  = data.offset + (int)(BT*data.pixelIncrement/data.timeIncrement);
	  int ETS  = data.offset + (int)(ET*data.pixelIncrement/data.timeIncrement);
	  int LENS = ETS - BTS + 1; 
	  if(LENS < 1) LENS = 1;
				  
	  if(endTime > data.endTime) LENS += 5;
	  if(beginTime < data.beginTime)
	  {
		 BTS  -= 5;
		 LENS += 5;
	  }
				  
	  super.setBounds(BTS,  data.tluh/2 + ylocation*data.tluh - data.barheight/2,
					  LENS, data.barheight + 5);
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
			if(packs[p].BeginTime < data.beginTime)
			    packtime -= (data.beginTime - packs[p].BeginTime);
			if(packs[p].EndTime > data.endTime)
			   packtime -= (packs[p].EndTime - data.endTime);
		 }
		 packusage = packtime * 100;
		 packusage /= (data.endTime - data.beginTime);
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

	  if (beginTime < data.beginTime) {
	      usage -= (data.beginTime - beginTime);
	  }
	  if (endTime > data.endTime) {
	      usage -= (endTime - data.endTime);
	  }
	  //	  System.out.println("Final usage : " + usage);
	  //	  System.out.println();
	
	  usage /= (data.endTime - data.beginTime);
	  usage *= 100;
	  // System.out.println(usage);
   }   

   public void update(Graphics g)
   {
	  paint(g);
   }   
}
