package projections.gui;


import java.awt.*;
import java.text.*;

public class TimelineLabelCanvas extends Canvas
{  
   private TimelineData data;
   private FontMetrics fm;
   private Image offscreen;
   
   public TimelineLabelCanvas(TimelineData data)
   {
	  this.data = data;
	  setBackground(Color.black);
	  setForeground(Color.white);
   }   
   public void makeNewImage()
   {
	  offscreen = null;
	  
	  if(data.lcw > 0 && data.tlh > 0)
	  {
		 offscreen = createImage(data.lcw, data.tlh);
	  
		 if(offscreen == null)
			return;
	  
		 Graphics og = offscreen.getGraphics();
		 og.setClip(0, 0, data.lcw, data.tlh);
	  
		 if(fm == null)
		 {
			fm = og.getFontMetrics(og.getFont());
			data.lcw = fm.stringWidth("Processor 999") + 20;
		 }
		 
		 og.setColor(getForeground());
		 
		 data.processorList.reset();
		 NumberFormat df = NumberFormat.getInstance();
		 for(int p=0; p<data.numPs; p++)
		 {
			og.setColor(getForeground());
			String tmp = "Processor " + data.processorList.nextElement();
			og.drawString(tmp, 10, data.tluh/2 + p*data.tluh);
			
			og.setColor(Color.lightGray);
			tmp = "(" + df.format(data.processorUsage[p]) + "%)";
			og.drawString(tmp, 20, data.tluh/2 + p*data.tluh + fm.getHeight() + 2);
		 }
					   
		 og.dispose();
		 repaint();
	  }   
   }   
   public void paint(Graphics g)
   {
	  if(offscreen != null)
	  {
		 int y = data.timelineWindow.getVSBValue();
	  
		 g.drawImage(offscreen, 0,0, data.lcw,     data.vph, 
								0,y, data.lcw, y + data.vph, null);
	  }                        
   }   
   public void update(Graphics g)
   {
	  paint(g);
   }   
}