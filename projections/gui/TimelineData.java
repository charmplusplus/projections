package projections.gui;

import java.util.Vector;
import java.awt.*;
import javax.swing.*;

import projections.analysis.*;
import projections.misc.*;

public class TimelineData
{
    int vpw, vph;
    int tlw, tlh;
    int lcw;
    int ath, abh;
    int sbw, sbh;
    int mpw, mph;
    public int tluh;
    public int barheight;
    int numPs;
    
    float scale;
    public int   offset;
    
    public OrderedIntList processorList;
    OrderedIntList oldplist;
    String         oldpstring;
    
    // boolean for testing if entries are to be colored by Object ID
    public boolean colorbyObjectId;
    

    public double         pixelIncrement;
    public int            timeIncrement;
    int            labelIncrement;
    int            numIntervals;
    

    int[]          entries;
    Color[]        entryColor;
    
    public TimelineObject[][] tloArray;
    public Vector [] mesgVector;	  
    public Vector [] oldmesgVector;
    
    UserEvent[][] userEventsArray = null;
    
    TimelineDisplayCanvas displayCanvas;
    
    int xmin, xmax;
    long xmintime, xmaxtime;
    int  xminpixel, xmaxpixel;
    
    float[] processorUsage;
    float[] idleUsage;
    float[] packUsage;
    OrderedUsageList[] entryUsageList;
    
    public long beginTime, endTime, totalTime;
    long oldBT, oldET;
    
    boolean showPacks, showIdle, showMsgs;
    
    public TimelineWindow timelineWindow;
   
    // points for line joining the creation of a message 
    // and its beginning of execution
    public Vector mesgCreateExecVector;

   
    public TimelineData(TimelineWindow timelineWindow)
    {
	showPacks = false;
	showMsgs  = true;
	showIdle  = true;
	
	oldBT = -1;
	oldET = -1;
	oldplist = null;
	oldpstring = null;
	
	this.timelineWindow = timelineWindow;
	displayCanvas = timelineWindow.displayCanvas;
	lcw = 100;
	sbw = 20;
	sbh = 20;   
	barheight = 20;
	tluh = barheight + 20;
	numPs = 0;
	ath = 50;
	scale = 1;
	processorUsage = null;
	entryUsageList = null;
	
	offset = 10;
	pixelIncrement = 5.0;
	timeIncrement  = 100;
	labelIncrement = 5;
	numIntervals = 1;
	beginTime = 0;
	totalTime = Analysis.getTotalTime();
	endTime = totalTime;
	xmin = 0;
	xmax = numIntervals;
	xmintime = 0;
	xmaxtime = 1;
	xminpixel = 0;
	xmaxpixel = 1;
	
	mesgCreateExecVector = new Vector();
	
	tloArray = null;
	mesgVector = null;
	entries = new int[Analysis.getNumUserEntries()];
	entryColor = Analysis.getColorMap();
	/*
	entryColor = new Color[Analysis.getNumUserEntries()];
	float H = (float)1.0;
	float S = (float)1.0;
	float B = (float)1.0;
	float delta = (float)(1.0/Analysis.getNumUserEntries());
	if (new File("bin/color.map").exists()) {
	    try {
		Util.restoreColors(entryColor, "Timeline Graph", null);
	    } catch (IOException e) {
		System.err.println("unable to load color.map");
	    } 
	} else {
	    for (int i=0; i<Analysis.getNumUserEntries(); i++) {
		entries[i] = 0;
		entryColor[i] = Analysis.getEntryColor(i);
	    }   
	}
	*/
    }   
    
    public void createTLOArray()
    {
	TimelineObject[][] oldtloArray = tloArray;
	UserEvent[][] oldUserEventsArray = userEventsArray;
	oldmesgVector = mesgVector;
	mesgVector = new Vector[Analysis.getNumProcessors()];
	for(int i=0;i < Analysis.getNumProcessors();i++){
	    mesgVector[i] = null;
	}
	
	tloArray = new TimelineObject[processorList.size()][];
	userEventsArray = new UserEvent[processorList.size()][];
	
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
			userEventsArray[newpindex] = 
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
			tloArray[newpindex] = new TimelineObject[newNumItems];
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
			    userEventsArray[newpindex] = 
				new UserEvent[newNumItems];
			    for (n=0; n<newNumItems; n++) {
				userEventsArray[newpindex][n] = 
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
	   new ProgressMonitor(Analysis.guiRoot, "Reading timeline data",
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
       for (int e=0; e<Analysis.getNumUserEntries(); e++) {
	   entries[e] = 0;
       }
       processorUsage = new float[tloArray.length];
       entryUsageList = new OrderedUsageList[tloArray.length];
       float[] entryUsageArray = new float[Analysis.getNumUserEntries()];
       idleUsage  = new float[tloArray.length];
       packUsage  = new float[tloArray.length];
	  
       for (int p=0; p<tloArray.length; p++) {
	   processorUsage[p] = 0;
	   idleUsage[p] = 0;
	   packUsage[p] = 0;
	   for (int i=0; i<Analysis.getNumUserEntries(); i++) {
	       entryUsageArray[i] = 0;
	   }
	   for (int n=0; n<tloArray[p].length; n++) {
	       float usage = tloArray[p][n].getUsage();
	       int entrynum = tloArray[p][n].getEntry();
	       /*
	       System.out.println("Usage for PE " + p + ", Event " +
				  n + " is " + usage + " | cumulative = " +
				  processorUsage[p] + " with EP = " +
				  entrynum);
	       */
	       if (entrynum >=0) {
		   entries[entrynum]++;
		   processorUsage[p] += usage;
		   packUsage[p] += tloArray[p][n].getPackUsage();
		   entryUsageArray[entrynum] += tloArray[p][n].getNetUsage();
	       } else {
		   idleUsage[p] += usage;
	       }
	   }
	   /*
	   System.out.println("Processor " + p + " Usage: " + 
			      processorUsage[p]);
	   */
	   entryUsageList[p] = new OrderedUsageList();
	   for (int i=0; i<Analysis.getNumUserEntries(); i++) {
	       if (entryUsageArray[i] > 0) {
		   entryUsageList[p].insert(entryUsageArray[i], i);
	       }
	   }      
       } 
   }   

    // index into userEventArray
    private TimelineObject[] getData(int pnum, int index)  
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
	    userEventsArray[index] = new UserEvent[numUserEvents];
	    for (int i=0; i<numUserEvents; i++) {
		userEventsArray[index][i] = 
		    (UserEvent) userEvents.elementAt(i);
	    }
	} else { 
	    // probably already numm
	    userEventsArray[index] = null; 
	} 
	
	// proc timeline events
	numItems = tl.size();   
	TimelineObject[] tlo = new TimelineObject[numItems];
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
		packs[p] = (PackTime)packlist.elementAt(p);
	    }
	    tlo[i] = new TimelineObject(this, tle, msgs, packs, pnum);
	}
	return tlo;
    }   

   public int getNumUserEvents() {
     if (userEventsArray == null) { return 0; }
     int num = 0;
     for (int i=0; i<userEventsArray.length; i++) {
       if (userEventsArray[i] != null) { num += userEventsArray[i].length; }
     }
     return num;
   }


    public void drawConnectingLine(int pCreation,long creationtime,
				   int pCurrent,long executiontime,
				   TimelineObject objCurrent,
				   int drawordelete) {
	Dimension dim = displayCanvas.getSize();
	processorList.reset();
	int count =0;
	TimelineLine line;
	
	if (drawordelete == 2) {
	    int flag  = 0;
	    int i;
	    for (i=0;i<mesgCreateExecVector.size();i++) {
		line = (TimelineLine ) mesgCreateExecVector.elementAt(i);
		if (line.pCurrent == pCurrent && 
		    line.executiontime == executiontime) {
		    flag = 1;
		    break;
		}
	    }
	    if (flag == 1) { 
		mesgCreateExecVector.remove(i);
	    }
	    displayCanvas.repaint();
	    
	    return;
	}
	
	for (int i =0;i < processorList.size();i++) {
	    int pe = processorList.nextElement();
	    if (pe == pCreation) {
	    }
	    if (pe == pCurrent) {
	    }
	    count++;	
	}
	processorList.reset();
	line = new TimelineLine(pCreation,pCurrent,objCurrent,
				creationtime,executiontime);
	mesgCreateExecVector.add(line);
	displayCanvas.repaint();
    }

    public void drawAllLines(){
   	Graphics g = displayCanvas.getGraphics();
   	if (!mesgCreateExecVector.isEmpty()) {
	    g.setColor(Analysis.foreground);
	    
	    Dimension dim = displayCanvas.getSize();
	    double calc_xscale = (double )(pixelIncrement/timeIncrement);
	    double yscale = (double )dim.height/
		(double)(processorList.size());

	    for (int i=0;i<mesgCreateExecVector.size();i++) {
		TimelineLine lineElement = 
		    (TimelineLine)mesgCreateExecVector.elementAt(i);
		int startpe_position=0;
		int endpe_position=0;
		processorList.reset();
		for (int j=0;j<processorList.size();j++) {
		    int pe = processorList.nextElement();
		    if (pe == lineElement.pCreation) {
			startpe_position = j;
		    }
		    if (pe == lineElement.pCurrent) {
			endpe_position = j;
		    }
		}
		int x1 = 
		    (int)((double)(lineElement.creationtime - beginTime)*
			  calc_xscale+offset);
		int x2 = 
		    (int)((double)(lineElement.executiontime - beginTime)*
			  calc_xscale+offset);
		int y1 = (int)(yscale * (double)startpe_position + 
			       lineElement.obj.h+lineElement.obj.startY+5+5);
		int y2 = (int)(yscale * (double)endpe_position +
			       lineElement.obj.h);
		g.drawLine(x1,y1,x2,y2);
	    }
	}
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

    public void addProcessor(int pCreation){
	oldplist = processorList.copyOf();
	processorList.insert(pCreation);
	numPs = processorList.size();
	timelineWindow.validPEs = processorList;
	timelineWindow.procRangeDialog(false);
    }

    /****************** Timeline ******************/
    public Vector createTL(int p, long bt, long et, 
			   Vector timelineEvents, Vector userEvents) {
	try {
	    if (Analysis.hasLogData()) {
		return Analysis.logLoader.createtimeline(p, bt, et, 
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


}
