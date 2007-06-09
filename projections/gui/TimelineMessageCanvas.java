package projections.gui;

import java.awt.*;
import projections.analysis.*;


public class TimelineMessageCanvas extends Canvas
{
   private String[] sTitles;
   private int[]    width;
   private TimelineObject obj;
   private FontMetrics fm;
   private TimelineMessage[] msgs;
   private String[][] names;
   private int w,h;
   private int numTitles = 17; // index of sTitle with last actual title
   private int numColumns = 5; // number of columns that will be drawn
   private int[] maxColWidth = null;  // store max col width for each col
   
   public TimelineMessageCanvas(TimelineObject obj)
   {
	  this.obj  = obj;
	  w = 0;
	  h = 0;
	  msgs = obj.getMessages();
	  names = Analysis.getEntryNames();

          sTitles = new String[numTitles];
	  width = new int[numTitles];
	  
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
	  sTitles[15]= "TIME SINCE LAST SEND";
	  sTitles[16]= "TO ENTRY:";
	  //	  sTitles[17]= "DEST PE:";
	  
	  setBackground(Color.black);
	  setForeground(Color.lightGray);
   }   
   public Dimension getMinimumSize()
   {
	  return getPreferredSize();
   }   
   public Dimension getPreferredSize()
   {
          int i;
	  if(fm == null)
	  {
		 Graphics g = getGraphics();
		 if(g != null)
		 {
			fm = g.getFontMetrics(g.getFont());
			h = (fm.getHeight() + 5) * (6 + obj.getNumMsgs());
	 
			for(i=0; i<numTitles; i++)
			{
			   width[i] = fm.stringWidth(sTitles[i]);
			}

			// figure out the maximum width of each each column
			if (maxColWidth==null) { 
			    maxColWidth = new int[numColumns];
			}
			for(int m=0; m<obj.getNumMsgs(); m++)
			{
			   int w1 = fm.stringWidth("" + m);
			   int w2 = fm.stringWidth("" + msgs[m].MsgLen);
			   int w3 = fm.stringWidth("" + msgs[m].Time);
			   int w4 = (m>0) ? 
			       fm.stringWidth(""+(msgs[m].Time-msgs[m-1].Time)) : 0;
			   int w5 = fm.stringWidth(names[msgs[m].Entry][0]);

			   if(w1 > maxColWidth[0]) maxColWidth[0] = w1;
			   if(w2 > maxColWidth[1]) maxColWidth[1] = w2;
			   if(w3 > maxColWidth[2]) maxColWidth[2] = w3;
			   if(w4 > maxColWidth[3]) maxColWidth[3] = w4;
			   if(w5 > maxColWidth[4]) maxColWidth[4] = w5;
			}
			for (i=0; i<maxColWidth.length; i++) {
			    if (width[12+i]>maxColWidth[i]) { maxColWidth[i] = width[12+i]; }
			}
			w = width[0] + width[1];
			int wtmp = 0;
			for(i=2; i<=7; i++) wtmp += width[i];
			if(wtmp > w) w = wtmp;
			wtmp = 0;
			for(i=8; i<=11; i++) wtmp += width[i];
			if(wtmp > w) w = wtmp;
			wtmp = 18;
			for(i=12; i<numTitles; i++) wtmp += width[i];
			if(wtmp > w) w = wtmp;
			wtmp = 18;
			for(i=0; i<maxColWidth.length; i++) 
			    wtmp += maxColWidth[i];
			if(wtmp > w) w = wtmp;

			g.dispose();
		 }
	  }        
	  
	  return new Dimension(w, h);
   }   
   public void paint(Graphics g)
   {
          int i;
	  int wi = getSize().width;
	  int ht = getSize().height;
		 
	  int space0 = (wi-width[0]-width[1])/2;
	  int space1 = (wi-width[2]-width[3]-width[4]-width[5]-width[6]-width[7])/2;
	  int space2 = (wi-width[8]-width[9]-width[10]-width[11])/2;
	  int totalColWidth = 0;
	  for (i=0; i<maxColWidth.length; i++) { 
	      totalColWidth += maxColWidth[i]; 
	  }
	  int space3 = (wi-totalColWidth)/(1+maxColWidth.length);
	  
	  if(space0 < 0) space0 = 0;
	  if(space1 < 0) space1 = 0;
	  if(space2 < 0) space2 = 0;
	  if(space3 < 0) space3 = 0;   

	  int[] colWidth = new int[numColumns];
	  int[] colStart = new int[numColumns];
	  colWidth[0] = maxColWidth[0] + (int)(space3 * 1.3);
	  colWidth[1] = maxColWidth[1] + space3;
	  colWidth[2] = maxColWidth[2] + (int)(space3 * 1.3);
	  colWidth[3] = maxColWidth[3] + (int)(space3 * 1.3);
	  colWidth[4] = maxColWidth[4] + space3;
	  colStart[0] = 0;
	  for (i=1; i<colStart.length; i++) {
	      colStart[i] = colStart[i-1]+colWidth[i-1];
	  }
	  
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
	  
	  // draw headers for columns
	  y+=dy;
	  g.setColor(Color.white);
	  for (i=0; i<numColumns; i++) {
	      g.drawString(sTitles[12+i], (colWidth[i]-width[12+i])/2+colStart[i], y);
	  }

	  // draw columns
	  g.setColor(Color.lightGray);
	  String[] s = new String[numColumns];
	  for(int m=0; m<obj.getNumMsgs(); m++)
	  {
		s[0] = new String("" + m);
		s[1] = new String("" + msgs[m].MsgLen);
		s[2] = new String("" + msgs[m].Time);
		s[3] = (m>0) ? new String("" + (int)(msgs[m].Time-msgs[m-1].Time)) : "-";
		s[4] = new String(names[msgs[m].Entry][0]);
		 
		y+=dy;
		for (i=0; i<numColumns; i++) {
		    g.drawString(s[i], (colWidth[i]-fm.stringWidth(s[i]))/2+colStart[i], y);
		}
	  }     
   }   
   public void update(Graphics g)
   {
	  paint(g);
   }   
}

