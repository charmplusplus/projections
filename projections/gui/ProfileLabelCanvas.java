package projections.gui;

import java.awt.*;

public class ProfileLabelCanvas extends Canvas
{  
   private ProfileData data;
   private Image offscreen;
   private int maxwidth;
   private FontMetrics fm;
   private int labelincrement;
   
   public ProfileLabelCanvas(ProfileData data)
   {
	  this.data = data;
	  offscreen = null;
	  maxwidth = 0;
	  setForeground(Analysis.foreground);
	  setBackground(Analysis.background);
   }   
   private void drawLabels(Graphics g)
   {    
	  g.setColor(getForeground());
	  
	  int h = getSize().height;
	  
	  float width = (float)(data.dcw - 2*data.offset)/data.numPs;

	  if(fm == null)
	  {
		 fm = g.getFontMetrics(g.getFont());
		 maxwidth = fm.stringWidth("" + Analysis.getNumProcessors()) + 20;
	  }   
	  
	  labelincrement = (int)(Math.ceil((double)maxwidth/width));
	  labelincrement = Util.getBestIncrement(labelincrement);
	  int textheight = fm.getHeight();
	  int longlineht  = h - textheight - 2;
	  int shortlineht = longlineht/2;

	  data.plist.reset();
	  
	  String tmp1 = "Avg";
	  int xloc1 = (int)((0+0.5)*width) + data.offset;
	  g.drawLine(xloc1, 0, xloc1, longlineht);
	  xloc1 -= (int)(0.5 * fm.stringWidth(tmp1));
	  g.drawString(tmp1, xloc1, h);
	  
	  for (int p=1; p<(data.numPs); p++)
	  {
		 String tmp = "" + data.plist.nextElement();
		 int xloc = (int)((p+0.5)*width) + data.offset;
		 if((p % labelincrement) == 0)
		 {
			g.drawLine(xloc, 0, xloc, longlineht);
			xloc -= (int)(0.5 * fm.stringWidth(tmp));
			g.drawString(tmp, xloc, h);
		 }
		 else
		 {
			g.drawLine(xloc, 0, xloc, shortlineht);
		 }      
	  }  
   }   
   public void makeNewImage()
   {
	  offscreen = null;
	  
	  if(data.plist == null)
		 return;
	  
	  int h = getSize().height;

	  if(data.dcw > 0 && h > 0)
	  {
		 offscreen = createImage(data.dcw, h);
	  
		 if(offscreen == null)
			return;
	  
		 Graphics og = offscreen.getGraphics();
		 og.setClip(0, 0, data.dcw, h);
				  
		 drawLabels(og);     
		 og.dispose();
		 repaint();
	  }  
   }   
   public void paint(Graphics g)
   {
	  if(offscreen != null)
	  {
		 int x = data.profileWindow.getHSBValue();
		 int h = getSize().height;
	  
		 g.drawImage(offscreen, 0,0, data.vpw,   h, 
								x,0, data.vpw+x, h, null);
	  }                        
   }   
   public void update(Graphics g)
   {
	  paint(g);
   }   
}
