package projections.gui;

import java.util.Vector;
import projections.analysis.*;
import java.awt.*;

public class TimelineData
{
   int vpw, vph;
   int tlw, tlh;
   int lcw;
   int ath, abh;
   int sbw, sbh;
   int mpw, mph;
   int tluh;
   int barheight;
   int numPs;
   
   float scale;
   
   int offset;
   
   OrderedIntList processorList;
   OrderedIntList oldplist;
   String processorString;
   String oldpstring;
   
   double pixelIncrement;
   int timeIncrement;
   int labelIncrement;
   int numIntervals;
   
   int[] entries;
   Color[] entryColor;
   
   TimelineObject[][] tloArray;
   
   TimelineDisplayCanvas displayCanvas;
   
   int xmin, xmax;
   long xmintime, xmaxtime;
   int  xminpixel, xmaxpixel;
   
   float[] processorUsage;
   float[] idleUsage;
   float[] packUsage;
   OrderedUsageList[] entryUsageList;
   
   long beginTime, endTime, totalTime;
   long oldBT, oldET;
   
   boolean showPacks, showIdle, showMsgs;

   TimelineWindow timelineWindow;
   
   public TimelineData(TimelineWindow timelineWindow)
   {
	  showPacks = false;
	  showMsgs  = true;
	  showIdle  = false;

	  oldBT = -1;
	  oldET = -1;
	  oldplist = null;
	  oldpstring = null;
	  
	  this.timelineWindow = timelineWindow;
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
   
	  processorString = "0";
	  
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
	  
	  tloArray = null;
	  entries = new int[Analysis.getNumUserEntries()];
	  entryColor = new Color[Analysis.getNumUserEntries()];
	  float H = (float)1.0;
	  float S = (float)1.0;
	  float B = (float)1.0;
	  float delta = (float)(1.0/Analysis.getNumUserEntries());
	  for(int i=0; i<Analysis.getNumUserEntries(); i++)
	  {
		 entries[i] = 0;
		 entryColor[i] = Color.getHSBColor(H, S, B);
		 H -= delta;
		 if(H < 0.0)
			H = (float)1.0;
	  }   
		 
   }   
   public void createTLOArray()
   {
	  TimelineObject[][] oldtloArray = tloArray;
	  
	  tloArray = new TimelineObject[processorList.size()][];
	  
	  if(oldtloArray != null && beginTime >= oldBT && endTime <= oldET)
	  {
		 int oldp, newp;
		 int oldpindex=0, newpindex=0;
		 
		 processorList.reset();
		 oldplist.reset();
		 
		 newp = processorList.nextElement();
		 oldp = oldplist.nextElement();
		 while(newp != -1)
		 {
			while(oldp != -1 && oldp < newp)
			{
			   oldp = oldplist.nextElement();
			   oldpindex++;
			}   
			if(oldp == -1)
			   break;
			if(oldp == newp)
			{
			   if(beginTime == oldBT && endTime == oldET)
				  tloArray[newpindex] = oldtloArray[oldpindex];
			   else
			   {
				  int oldnumitems = oldtloArray[oldpindex].length;
				  int newnumitems = 0;
				  int startindex  = 0;
				  int endindex    = oldnumitems - 1;
				  
				  for(int n=0; n<oldnumitems; n++)
				  {
					 if(oldtloArray[oldpindex][n].getEndTime() < beginTime)
						startindex++;
					 else
						break;
				  }
				  
				  for(int n=oldnumitems-1; n>=0; n--)
				  {
					 if(oldtloArray[oldpindex][n].getBeginTime() > endTime)
						endindex--;
					 else
						break;
				  }
				  
				  newnumitems = endindex - startindex + 1;
				  
				  tloArray[newpindex] = new TimelineObject[newnumitems];
				  
				  for(int n=0; n<newnumitems; n++)
				  {
					 tloArray[newpindex][n] = oldtloArray[oldpindex][n+startindex];
					 tloArray[newpindex][n].setUsage();
					 tloArray[newpindex][n].setPackUsage();
				  }
			   }
			}                                       
		 
			newp = processorList.nextElement();
			newpindex++;
		 }   
		 oldtloArray = null;
	  }
	  
	  int pnum;
	  processorList.reset();
	  for(int p=0; p<processorList.size(); p++)
	  {
		 pnum = processorList.nextElement();
		 if(tloArray[p] == null)
			tloArray[p] = getData(pnum);
	  }
	  
	  for(int e=0; e<Analysis.getNumUserEntries(); e++)
		 entries[e] = 0;
	  
	  processorUsage = new float[tloArray.length];
	  entryUsageList = new OrderedUsageList[tloArray.length];
	  float[] entryUsageArray = new float[Analysis.getNumUserEntries()];
	  idleUsage  = new float[tloArray.length];
	  packUsage  = new float[tloArray.length];
	  
	  for(int p=0; p<tloArray.length; p++)
	  {
		 processorUsage[p] = 0;
		 idleUsage[p] = 0;
		 packUsage[p] = 0;
		 for(int i=0; i<Analysis.getNumUserEntries(); i++)
			entryUsageArray[i] = 0;
			
		 for(int n=0; n<tloArray[p].length; n++)
		 {
			float usage = tloArray[p][n].getUsage();
			int entrynum = tloArray[p][n].getEntry();
			if(entrynum >=0)
			{
			   entries[entrynum]++;
			   processorUsage[p] += usage;
			   packUsage[p] += tloArray[p][n].getPackUsage();
			   entryUsageArray[entrynum] += tloArray[p][n].getNetUsage();
			}
			else
			   idleUsage[p] += usage;
		 }
		 
		 entryUsageList[p] = new OrderedUsageList();
		 for(int i=0; i<Analysis.getNumUserEntries(); i++)
		 {
			if(entryUsageArray[i] > 0)
			   entryUsageList[p].insert(entryUsageArray[i], i);
		 }      
		 
	  } 
   }   
   private TimelineObject[] getData(int pnum)
   {
	  Vector tl, msglist, packlist;
	  TimelineEvent tle;

	  int numItems;
	  long btime, etime;
	  int entry, pSrc, numMsgs, numpacks;
		
	  tl = Analysis.createTL(pnum, beginTime, endTime);
	  numItems = tl.size();   
	  
	  TimelineObject[] tlo = new TimelineObject[numItems];
	  for(int i=0; i<numItems; i++)
	  {
		 tle   = (TimelineEvent)tl.elementAt(i);
		 btime = tle.BeginTime;
		 etime = tle.EndTime; 
		 entry = tle.EntryPoint; 
		 pSrc  = tle.SrcPe;
			
		 msglist = tle.MsgsSent;
		 if(msglist == null)
			numMsgs = 0;
		 else
			numMsgs = msglist.size();
			
		 TimelineMessage[] msgs = new TimelineMessage[numMsgs];
		 for(int m=0; m<numMsgs; m++)
			msgs[m] = (TimelineMessage)msglist.elementAt(m);

		 packlist = tle.PackTimes;
		 if(packlist == null)
			numpacks = 0;
		 else
			numpacks = packlist.size();
		 
		 PackTime[] packs = new PackTime[numpacks];
		 for(int p=0; p<numpacks; p++)
			packs[p] = (PackTime)packlist.elementAt(p);
		 
		 tlo[i] = new TimelineObject(this, btime, etime, entry, msgs, packs, pnum, pSrc);
	  }
	  
	  return tlo;
   }   
}