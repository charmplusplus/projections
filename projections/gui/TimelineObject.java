package projections.gui;

import java.awt.*;
import java.awt.event.*;
import projections.analysis.*;

public class TimelineObject extends Component
   implements MouseListener
{
   private String[]  bubbletext;
   private Bubble  bubble;
   private TimelineMessageWindow msgwindow;
   private long    beginTime, endTime;
   private int     entry;
   private boolean inside = false; 
   private int pCurrent, pCreation;
   private double  usage;
   private float packusage;
   private long packtime;
   
   private TimelineData data;
   private Frame f;
   
   private TimelineMessage[] messages;
   private PackTime[] packs;

   public TimelineObject(TimelineData data, long bt, long et, int n, 
						 TimelineMessage[] msgs, PackTime[] packs,
						 int p1, int p2)
   {
	  setBackground(Color.black);
	  setForeground(Color.white);
	  
	  this.data = data;
	  beginTime = bt;
	  endTime   = et;
	  entry     = n;
	  messages  = msgs;
	  this.packs= packs;
	  pCurrent  = p1;
	  pCreation = p2;
	  f = (Frame)data.timelineWindow;
	  
	  setUsage();
	  setPackUsage();
	  
	  if(n != -1)
	  {
		 bubbletext  = new String[7];
		 int ecount = Analysis.getUserEntryCount();
		 if (n >= ecount) {
		   System.out.println("Fatal error: invalid entry "+n+"!");
		   System.exit(1) ;
		 }
		 bubbletext[0] = (Analysis.getUserEntryNames())[n][1] + "::" + 
					  (Analysis.getUserEntryNames())[n][0]; 
		 bubbletext[1] = "Begin Time: " + bt;
		 bubbletext[2] = "End Time: " + et;
		 bubbletext[3] = "Total Time: " + U.t(et-bt);
		 bubbletext[4] = "Packing: " + U.t(packtime);
		 if (packtime>0)
			bubbletext[4]+=" (" + (100*(float)packtime/(et-bt+1)) + "%)";
		 bubbletext[5] = "Msgs created: " + msgs.length;
		 bubbletext[6] = "Created by processor " + pCreation;
	  }
	  else
	  {
		 bubbletext = new String[4];
		 bubbletext[0] = "IDLE TIME";
		 bubbletext[1] = "Begin Time: " + bt;
		 bubbletext[2] = "End Time: " + et;
		 bubbletext[3] = "Total Time: " + U.t(et-bt);
	  }
	  
	  
	  addMouseListener(this);
   }   
   public void CloseMessageWindow()
   {
	  msgwindow = null;
   }   
   private void drawLeftArrow(Graphics g, Color c, int h)
   {
	  int[] xpts = {5, 0, 5};
	  int[] ypts = {0, h/2, h-1};
	  
	  g.setColor(c);
	  g.fillPolygon(xpts, ypts, 3);
	  
	  g.setColor(c.brighter());
	  g.drawLine(xpts[0], ypts[0], xpts[1], ypts[1]);
	  
	  g.setColor(c.darker());
	  g.drawLine(xpts[1], ypts[1], xpts[2], ypts[2]);   
   }   
   private void drawRightArrow(Graphics g, Color c, int h, int w)
   {
	  int[] xpts = {w-6, w, w-6};
	  int[] ypts = {0, h/2, h-1};
	  
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
	  if(entry >= 0)
		 OpenMessageWindow();
   }   
   public void mouseEntered(MouseEvent evt)
   {
	  if(entry == -1 && data.showIdle == false)
		 return;

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
   {}   
   public void mouseReleased(MouseEvent evt)
   {}   
   private void OpenMessageWindow()
   {
	  if(msgwindow == null)
		 msgwindow = new TimelineMessageWindow(this);
	  msgwindow.setVisible(true);
   }   
   public void paint(Graphics g)
   {     
	  if(entry == -1 && data.showIdle == false)
		 return;

	  Color c;
	  
	  if(entry == -1)
		 c = getForeground();
	  else
		 c = data.entryColor[entry];
	  

	  int w   = getSize().width;
	  int h   = getSize().height - 5;
	  
	  int left  = 0;
	  int right = w-1;
	  
	  long viewbt = beginTime;
	  long viewet = endTime;
		 
	  if(beginTime < data.beginTime)
	  {
		 drawLeftArrow(g, c, h);
		 left = 5;
		 viewbt = data.beginTime;
	  }
		 
	  if(endTime > data.endTime)
	  {
		 drawRightArrow(g, c, h, w);
		 right = w-6;
		 viewet = data.endTime;
	  }
		 
	  g.setColor(c);
	  
	  int pixelwidth = right-left+1;
	  g.fillRect(left, 0, pixelwidth, h);
	  
	  if(entry == -1)
	  {
		 g.setColor(getBackground());
		 for(int x=0; x<w+h-2; x += 4)
		 {
			g.drawLine(x, 0, x-h, h);
			g.drawLine(x+1, 0, x-h+1, h);
		 }
	  }

	  if(w > 2)
	  {
		 g.setColor(c.brighter());
		 g.drawLine(left, 0, right, 0);
		 if(left == 0)
			g.drawLine(0, 0, 0, h-1);
		 
		 g.setColor(c.darker());
		 g.drawLine(left, h-1, right, h-1);
		 if(right == w-1)
			g.drawLine(w-1, 0, w-1, h-1);
	  }
	  
	  
	  double scale= pixelwidth /((double)(viewet - viewbt + 1)); 

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
			   g.drawLine(pos, h, pos, h+5);
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
			   g.fillRect(pos, h, (int)(pet-pbt+1), 3);
			}
		 }
	  }               
   }   
   public void print(Graphics pg, long minx, long maxx, double pixelIncrement, int timeIncrement)
   {
	  if(entry == -1 && data.showIdle == false)
		 return;

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
		 drawLeftArrow(pg, c, h);
		 left = 5;
		 viewbt = minx;
	  }
		 
	  if(endTime > maxx)
	  {
		 drawRightArrow(pg, c, h, w);
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
	  long LEN, BT, ET;
	  
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
			packtime += packs[p].EndTime - packs[p].BeginTime + 1;  
			if(packs[p].BeginTime < data.beginTime)
			   packtime -= (data.beginTime - packs[p].BeginTime);
			if(packs[p].EndTime > data.endTime)
			   packtime -= (packs[p].EndTime - data.endTime);
		 }
		 packusage = packtime * 100;
		 packusage /= (data.endTime - data.beginTime + 1);
	  }
   }   
   public void setUsage()
   {
	  usage = endTime - beginTime + 1;
	  if(beginTime < data.beginTime)
		 usage -= (data.beginTime - beginTime);
	  if(endTime   > data.endTime)
		 usage -= (endTime - data.endTime);
	
	  usage /= (data.endTime - data.beginTime + 1);
	  usage *= 100;
   }   
   public void update(Graphics g)
   {
	  paint(g);
   }   
}
