package projections.gui;

import java.awt.*;

public class TimelineAxisCanvas extends Canvas
{
   private TimelineData data;
   private int axispos, textpos;
   private String type;
   private FontMetrics fm;
   private Image offscreen;
   
   public TimelineAxisCanvas(TimelineData data, String type)
   {
	  this.data = data;
	  this.type = type;
	  setBackground(Color.black);
	  setForeground(Color.white);
   }   
   public void makeNewImage()
   {
	  offscreen = null;
	  
	  if(data.tlw > 0 && data.ath > 0)
	  {
		 offscreen = createImage(data.tlw, data.ath);

		 if(offscreen == null)
			return;

		 Graphics og = offscreen.getGraphics();
		 og.setClip(0, 0, data.tlw, data.ath);
	  
		 if(fm == null)
		 {
			fm = og.getFontMetrics(og.getFont());

			if(type.equals("top"))
			{
			   axispos = data.ath - 10;
			   textpos = axispos  - 10;
			}
			else
			{
			   axispos = 10;
			   textpos = axispos + 10 + fm.getHeight();
			} 
		 }        
		 
		 if((type.equals("top") && data.numPs > 1) || (type.equals("bot") && data.numPs > 0))
		 { 
			
			long labeloffset = data.timeIncrement - (data.beginTime % data.timeIncrement);
			if(labeloffset == data.timeIncrement) labeloffset = 0;
			
			
			int maxx = data.offset + (int)((data.endTime-data.beginTime)*data.pixelIncrement/data.timeIncrement);
			og.setColor(getForeground());
			og.drawLine(data.offset, axispos, maxx, axispos);
		 
			int curx;
			String tmp;
			for(int x=0; x<data.numIntervals; x++)
			{
			   curx = data.offset + (int)(x*data.pixelIncrement) 
					  + (int)(labeloffset * data.pixelIncrement/data.timeIncrement);
			
			   if(curx > maxx) break;
			   
			   if(x % data.labelIncrement == 0)
			   {  
				  tmp = "" + (long)(data.beginTime + (long)labeloffset + (long)x*data.timeIncrement);
				  og.drawLine(curx, axispos-5, curx, axispos + 5);
				  og.drawString(tmp, curx - fm.stringWidth(tmp)/2, textpos);
			   }
			   else
			   {
				  og.drawLine(curx, axispos-2, curx, axispos+2);
			   }
			}
		 }
		 og.dispose();
		 repaint();
	  }   
   }   
   public void paint(Graphics g)
   {
	  if(offscreen != null)
	  {
		 int x = data.timelineWindow.getHSBValue();
	  
		 g.drawImage(offscreen, 0,0,     data.vpw, data.ath, 
								x,0, x + data.vpw, data.ath, null);
	  }                       
   }   
   public void update(Graphics g)
   {
	  paint(g);
   }   
}
