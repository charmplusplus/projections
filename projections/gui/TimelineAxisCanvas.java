package projections.gui;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;

public class TimelineAxisCanvas extends Canvas
{
   private TimelineData  data;
   private int           axispos, textpos;
   private String        type;
   private FontMetrics   fm;
   private Image         offscreen;
   private Component     component_;
   private long          beginCoord_;
   private long          endCoord_;
   private DecimalFormat format_= new DecimalFormat();
   
   public TimelineAxisCanvas(TimelineData data, String type)
   {
	  this.data = data;
	  this.type = type;
	  component_ = this;
	  setBackground(Color.black);
	  setForeground(Color.white);
	  this.addMouseListener(new MouseAdapter() {
	      public void mouseEntered(MouseEvent e) {
		component_.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	      }
	      public void mouseExited(MouseEvent e) {
		component_.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	      }
	  });
	  format_.setGroupingUsed(true);
   }   
  // given a coordinate in coordinates from beginning of TimelineAxisCanvas, 
  // return the time in us
  public double canvasToTime(int x) {
    return (x-beginCoord_)/(double)(endCoord_-beginCoord_)*(data.endTime-data.beginTime)+data.beginTime;
  }
  // given a coordinte in screen coords, return coodinate in scaled axis
  public Point screenToCanvas(Point p) {
    int x = p.x;
    int startX = data.timelineWindow.getHSBValue();
    int newX = startX+x;
    p.x = newX;
    return p;
  }
  // return the int value of the HSB to make the image start at the requested
  // time
  public int calcHSBOffset(double startTime) {
    double percentOffset = 
      (startTime-data.beginTime)/(data.endTime-data.beginTime);
    double actualOffset = percentOffset*(endCoord_-beginCoord_);
    return (int)(actualOffset + beginCoord_ + 0.5);
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
			beginCoord_ = data.offset;
			endCoord_ = maxx;
	  
			int curx;
			String tmp;
			for(int x=0; x<data.numIntervals; x++)
			{
			   curx = data.offset + (int)(x*data.pixelIncrement) 
					  + (int)(labeloffset * data.pixelIncrement/data.timeIncrement);
			
			   if(curx > maxx) break;
			   
			   if(x % data.labelIncrement == 0)
			   {  
				  tmp = format_.format((long)(data.beginTime + (long)labeloffset + (long)x*data.timeIncrement));
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
		 g.drawImage(offscreen, 0, 0, data.vpw, data.ath, 
			     x, 0, x + data.vpw, data.ath, null);
	  }                       
   }   
   public void update(Graphics g)
   {
	  paint(g);
   }   
}
