package projections.gui;

import java.awt.*;
import projections.analysis.*;


public class TimelineMessageCanvas extends Canvas
{
   private String[] sTitles;
   private int[]    width;
   private TimelineObject obj;;
   private FontMetrics fm;
   private TimelineMessage[] msgs;
   private String[][] names;
   private int w,h;
   
   public TimelineMessageCanvas(TimelineObject obj)
   {
	  this.obj  = obj;
	  w = 0;
	  h = 0;
	  msgs = obj.getMessages();
	  names = Analysis.getUserEntryNames();

	  sTitles = new String[20];
	  width = new int[20];
	  
	  int entry = obj.getEntry();
	  
	  sTitles[0] = "DETAILS FOR ENTRY: ";
	  sTitles[1] = names[entry][1] + " -- " + names[entry][0];
	  sTitles[2] = "BEGIN TIME: ";
	  sTitles[3] = "" + obj.getBeginTime();
	  sTitles[4] = "     END TIME: ";
	  sTitles[5] = "" + obj.getEndTime();
	  sTitles[6] = "     MSGS: ";
	  sTitles[7] = "" + obj.getNumMsgs();
	  sTitles[8] = "CREATED BY: ";
	  sTitles[9] = "Processor " + obj.getPCreation();
	  sTitles[10]= "      EXECUTED ON: ";
	  sTitles[11]= "Processor " + obj.getPCurrent();
	  sTitles[12]= "MSG#";
	  sTitles[13]= "MSG SIZE:";
	  sTitles[14]= "TIME SENT";
	  sTitles[15]= "TO ENTRY:";
	  
	  setBackground(Color.black);
	  setForeground(Color.lightGray);
   }   
   public Dimension getMinimumSize()
   {
	  return getPreferredSize();
   }   
   public Dimension getPreferredSize()
   {
	  if(fm == null)
	  {
		 Graphics g = getGraphics();
		 if(g != null)
		 {
			fm = g.getFontMetrics(g.getFont());
			h = (fm.getHeight() + 5) * (6 + obj.getNumMsgs());
	 
			for(int i=0; i<=15; i++)
			{
			   width[i] = fm.stringWidth(sTitles[i]);
			}

			width[16] = 0;
			width[17] = 0;
			width[18] = 0;
			width[19] = 0;
			for(int m=0; m<obj.getNumMsgs(); m++)
			{
			   int w1 = fm.stringWidth("" + m);
			   int w2 = fm.stringWidth("" + msgs[m].MsgLen);
			   int w3 = fm.stringWidth("" + msgs[m].Time);
			   int w4 = fm.stringWidth(names[msgs[m].Entry][0]);
			
			   if(w1 > width[16]) width[16] = w1;
			   if(w2 > width[17]) width[17] = w2;
			   if(w3 > width[18]) width[18] = w3;
			   if(w4 > width[19]) width[19] = w4;
			}
			w = width[0] + width[1];
			int wtmp = 0;
			for(int i=2; i<=7; i++) wtmp += width[i];
			if(wtmp > w) w = wtmp;
			wtmp = 0;
			for(int i=8; i<=11; i++) wtmp += width[i];
			if(wtmp > w) w = wtmp;
			wtmp = 18;
			for(int i=12; i<=15; i++) wtmp += width[i];
			if(wtmp > w) w = wtmp;
			wtmp = 18;
			for(int i=16; i<=19; i++) wtmp += width[i];
			if(wtmp > w) w = wtmp;

			g.dispose();
		 }
	  }        
	  
	  return new Dimension(w, h);
   }   
   public void paint(Graphics g)
   {
	  int w1, w2, w3, w4;
	  
	  int wi = getSize().width;
	  int ht = getSize().height;
		 
	  int space0 = (wi-width[0]-width[1])/2;
	  int space1 = (wi-width[2]-width[3]-width[4]-width[5]-width[6]-width[7])/2;
	  int space2 = (wi-width[8]-width[9]-width[10]-width[11])/2;
	  int space3 = (wi-width[16]-width[17]-width[18]-width[19])/5;
	  
	  if(space0 < 0) space0 = 0;
	  if(space1 < 0) space1 = 0;
	  if(space2 < 0) space2 = 0;
	  if(space3 < 0) space3 = 0;   

	  w1 = width[16] + (int)(space3 * 1.3);
	  w2 = width[17] + space3;
	  w3 = width[18] + (int)(space3 * 1.3);
	  w4 = width[19] + (int)(space3);
	  
	  int dy = fm.getHeight() + 5;
	  int y  = dy;
	  
	  int wtmp = 0;
	 
	  //g.setColor(Color.white);
	  //g.drawString(sTitles[0], wtmp=space0,    y);      
	  g.setColor(Color.white);
	  g.drawString(sTitles[1], (wi-width[1])/2, y);
	  
	  g.setColor(Color.red.darker());
	  g.drawString(sTitles[2], wtmp=space1,             y+=dy);
	  g.drawString(sTitles[4], wtmp+=width[2]+width[3], y);
	  g.drawString(sTitles[6], wtmp+=width[4]+width[5], y);
	  
	  g.setColor(Color.lightGray);
	  g.drawString(sTitles[3], wtmp=space1+width[2],    y);
	  g.drawString(sTitles[5], wtmp+=width[3]+width[4], y);
	  g.drawString(sTitles[7], wtmp+=width[5]+width[6], y);
	  
	  g.setColor(Color.green.darker());
	  g.drawString(sTitles[8] , wtmp=space2,             y+=dy);
	  g.drawString(sTitles[10], wtmp+=width[8]+width[9], y);
	  
	  g.setColor(Color.lightGray);
	  g.drawString(sTitles[9] , wtmp=space2+width[8],     y);
	  g.drawString(sTitles[11], wtmp+=width[9]+width[10], y);
	  
	  g.setColor(Color.white);
	  g.drawString(sTitles[12], (w1-width[12])/2,       y+=(2*dy));
	  g.drawString(sTitles[13], (w2-width[13])/2+w1,    y);
	  g.drawString(sTitles[14], (w3-width[14])/2+w1+w2, y); 
	  g.drawString(sTitles[15], (w4-width[15])/2+w1+w2+w3, y); 
	 
	  g.setColor(Color.lightGray);
	  for(int m=0; m<obj.getNumMsgs(); m++)
	  {
		 String sNum   = new String("" + m);
		 String sMLen = new String("" + msgs[m].MsgLen);
		 String sTime  = new String("" + msgs[m].Time);
		 String sEntry = new String(names[msgs[m].Entry][0]);
		 
		 g.drawString(sNum,   (w1-fm.stringWidth(sNum  ))/2,       y+=dy);
		 g.drawString(sMLen,  (w2-fm.stringWidth(sMLen ))/2+w1,    y);
		 g.drawString(sTime,  (w3-fm.stringWidth(sTime ))/2+w1+w2,  y);
		 g.drawString(sEntry, (w4-fm.stringWidth(sEntry))/2+w1+w2+w3, y);
	  }     
   }   
   public void update(Graphics g)
   {
	  paint(g);
   }   
}
