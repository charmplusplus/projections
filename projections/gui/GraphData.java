package projections.gui;

import java.awt.*;

/**
  GraphData holds the data for Graph
  It doesn't have to hold all data for all processors, the processor.list
  has the ordered list of all precoessors.
*/
public class GraphData
{
   static final int PROCESSOR = 0;
   static final int INTERVAL  = 1;
   
   static final int TIME      = 10;
   static final int MSGS      = 11;
   static final int BOTH      = 12;
   
   static final int LINE      = 20;
   static final int BAR       = 21;
   

   protected int               numUserEntries;
   protected int               graphtype;
   protected int               xmode;     
   protected int               ymode;
   protected int               minx;
   protected int               maxx;  
   protected int               offset;
   protected int               offset2;
   protected int               offset3;   
   protected float             scale;
   protected double            yscale;
   protected double            wscale;
   protected double            xscale;
   private   float             colorindex;
   private   float             colorvalue;
   private   boolean           colorflag = false;
   protected ZItem[]           systemUsage;
   protected ZItem[][]         systemMsgs;
   protected ZItem[][]         userEntry;
   protected ZItem[]           onGraph;
   protected BItem             processor;
   protected BItem             interval;
   protected GraphControlPanel controlPanel;
   protected GraphLegendPanel  legendPanel;
   protected GraphDisplayPanel displayPanel;
   protected GraphWindow       graphWindow;

   // save the original processor list
   protected OrderedIntList    origProcList;
   
   public GraphData(int numIs, long intsize, OrderedIntList procList)
   {
	  yscale  = 1;
	  wscale  = 1;
	  offset  = 10;
	  offset2 = 0;
	  offset3 = 0;
	  numUserEntries = Analysis.getNumUserEntries();
	  
	  colorvalue = (float)0.0;
	  colorindex = (float)1.0 / (numUserEntries + 7);
	  
	  graphtype = BAR;
	  xmode     = INTERVAL;
	  ymode     = MSGS;
	  
	  // Initialize processor info
	  processor         = new BItem();
/*  gzheng
  GraphData doesn't neccessarily have all processors data any more
	  processor.num     = Analysis.getNumProcessors();
	  processor.list    = new OrderedIntList();
	  for(int p=0; p<processor.num; p++)
		 processor.list.insert(p);
*/
          origProcList      = procList;
          processor.list    = procList;
	  processor.string  = processor.list.listToString();   
	  processor.num     = procList.size();
	  
	  // Initialize systemUsage data  
	  String s1[] = {"Queue Size", "Processor Usage(%)", "Idle Time(%)"};
	  systemUsage = new ZItem[3];
	  for(int a=0; a<3; a++)
	  {
		 systemUsage[a]       = new ZItem();
		 systemUsage[a].name  = s1[a];
		 systemUsage[a].exists=(null!=Analysis.getSystemUsageData(a));
	         float gray=(float)(1.0-a*0.3);
		 systemUsage[a].color = new Color(gray,gray,gray);
		 if (a==1) //Enable system CPU utilization by default
			systemUsage[a].state = true;
		 
		 if(a==0)
		 {
			systemUsage[a].type = "Msgs";
			systemUsage[a].ymode = MSGS;
		 }   
		 else
		 {
			systemUsage[a].type = "%"; 
			systemUsage[a].ymode = BOTH;
		 }      
	  }
	  onGraph    = new ZItem[1];
	  onGraph[0] = systemUsage[1];

	  // Initialize systemMsgs data
	  String s2[] = {"New Chare", "For Chare", "New Group", "Load Balancing", "Quiescence"};  
	  String s3[] = {"Creation", "Processing", "Time"};
	  systemMsgs = new ZItem[5][3];
	  for(int a=0; a<5; a++)
		 for(int t=0; t<3; t++)
		 {
			systemMsgs[a][t] = new ZItem();
			systemMsgs[a][t].name = s2[a];
			systemMsgs[a][t].type = s3[t];
			systemMsgs[a][t].exists=Analysis.hasSystemMsgsData(a,t);
			if(t==0) 
			{
			   systemMsgs[a][t].color = nextColor();
			   systemMsgs[a][t].ymode = MSGS;
			}   
			else if(t==1) 
			{
			   systemMsgs[a][t].color = systemMsgs[a][0].color.darker();
			   systemMsgs[a][t].ymode = MSGS;
			}   
			else if(t==2)
			{
			   systemMsgs[a][t].color = systemMsgs[a][0].color;
			   systemMsgs[a][t].ymode = TIME;
			}   

		 }  
			
	  // Initialize userEntry data
	  String[][] s4 = Analysis.getUserEntryNames();
	  userEntry = new ZItem[numUserEntries][3];
	  for(int a=0; a<numUserEntries; a++)
		 for(int t=0; t<3; t++)
		 {
			userEntry[a][t] = new ZItem();
			userEntry[a][t].name = s4[a][0];
			userEntry[a][t].type = s3[t];
			userEntry[a][t].parent = s4[a][1];
			userEntry[a][t].exists=Analysis.hasUserEntryData(a,t);
			if(t==0) 
			{
			   userEntry[a][t].color = nextColor();
			   userEntry[a][t].ymode = MSGS;
			}   
			else if(t==1) 
			{
			   userEntry[a][t].color = userEntry[a][0].color.darker();
			   userEntry[a][t].ymode = MSGS;
			}   
			else if(t==2)
			{
			   userEntry[a][t].color = userEntry[a][0].color;
			   userEntry[a][t].ymode = TIME;
			}   
		 }
	  
	  initData(numIs, intsize);
   }   

   public void initData(int numIs, long intsize)
	{
	scale       = (float)1.0;
	 
	  // Initialize interval info
	  interval      = new BItem();
	  interval.num  = numIs;
	  interval.size = intsize;
	  interval.list = new OrderedIntList();
	  for(int i=0; i<interval.num; i++)
		 interval.list.insert(i);
	  interval.string   = interval.list.listToString();
		 
	  int numProcessors = Analysis.getNumProcessors();
	  // Initialize systemUsage data 
	  for(int a=0; a<3; a++)
	  if (null!=Analysis.getSystemUsageData(a))
	  {
		 systemUsage[a].data = Analysis.getSystemUsageData(a);
//		 systemUsage[a].curPData = new int[processor.num];
		 systemUsage[a].curPData = new int[numProcessors];
		 systemUsage[a].curIData = new int[interval.num];
	  }    
	  
	  
	      // Initialize systemMsgs data
	      for(int a=0; a<5; a++)
		  {
		      for(int t=0; t<3; t++)
		      if (Analysis.hasSystemMsgsData(a,t))
			  {
			      systemMsgs[a][t].data = Analysis.getSystemMsgsData(a,t);
//			      systemMsgs[a][t].curPData = new int[processor.num];
			      systemMsgs[a][t].curPData = new int[numProcessors];
			      systemMsgs[a][t].curIData = new int[interval.num];
			  }
		  }  
	      
	      // Initialize userEntry data
	      for(int a=0; a<numUserEntries; a++)
		  {
		      for(int t=0; t<3; t++)
		      if (Analysis.hasUserEntryData(a,t))
			  {
			      userEntry[a][t].data = Analysis.getUserEntryData(a,t);
//			      userEntry[a][t].curPData = new int[processor.num];
			      userEntry[a][t].curPData = new int[numProcessors];
			      userEntry[a][t].curIData = new int[interval.num];
			  }
		  } 
	      
	  setData();
   }   

   private Color nextColor()
   {
	  Color tmp = Color.getHSBColor((float)((colorvalue*0.173)%1),
				   (float)(1-0.6*((colorvalue*0.729)%1)), 
		   (float)1.0);
	  colorvalue+=1.0;
	  
	  return tmp;
   }   

   private int setCurIData(ZItem item)
   {
	  int element, count;
	  int max = 0;
	  for(int i=0; i<interval.num; i++)
	  {
	    item.curIData[i] = 0;
	    // gzheng
	    // need to check if these i is in interval.list
	    if (!interval.list.contains(i)) continue;
	    count = 0;
	    processor.list.reset();
	    while((element = processor.list.nextElement()) >= 0)
	    {
		item.curIData[i] += item.data[element][i];
		count++;
	    }
	    if(item.ymode == BOTH)
		item.curIData[i] /= count;
	    else
		max = Math.max(item.curIData[i], max);     
	  }
	  return max;
   }   
   private int setCurPData(ZItem item)
   {
	  int element, count;
	  int max = 0;
	  int numProcessors = Analysis.getNumProcessors();
	  for(int p=0; p<numProcessors; p++)
	  {
//  for (int p=processor.list.nextElement(); p!=-1; p=processor.list.nextElement())
	    item.curPData[p] = 0;
	    if (!processor.list.contains(p)) {
//		System.out.println("Warning: Proc: "+p+" is not in the list.");
		continue;
	    }
	    count = 0;
	    interval.list.reset();
	    while((element = interval.list.nextElement()) >= 0)
	    {
		item.curPData[p] += item.data[p][element];
		count++;
	    }
	    if(item.ymode == BOTH)
		item.curPData[p] /= count;
	    else
		max = Math.max(item.curPData[p], max);   
	  }
	  return max;
   }   
   public void setData()
   {
	  int maxMP = 0;
	  int maxTP = 0;
	  int maxMI = 0;
	  int maxTI = 0;
	  
	  for(int a=0; a<onGraph.length; a++)
	  {
		 if (onGraph[a] == null) {
		   System.out.println("onGraph["+a+"/"+onGraph.length+"]==null");
		   continue;
		 }
		 if(onGraph[a].ymode == BOTH)
		 {
			setCurPData(onGraph[a]);
			setCurIData(onGraph[a]);
		 }   
		 else if(onGraph[a].ymode == TIME)
		 {
			maxTP = Math.max(setCurPData(onGraph[a]), maxTP);
			maxTI = Math.max(setCurIData(onGraph[a]), maxTI);
		 }   
		 else
		 {
			maxMP = Math.max(setCurPData(onGraph[a]), maxMP);
			maxMI = Math.max(setCurIData(onGraph[a]), maxMI);
		 }   
	  }

	  processor.maxTime = maxTP;
	  processor.maxMsgs = maxMP;
	  interval.maxTime  = maxTI;
	  interval.maxMsgs  = maxMI;     
   }   
}
