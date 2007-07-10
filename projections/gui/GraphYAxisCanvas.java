package projections.gui;

import java.awt.*;

public class GraphYAxisCanvas extends Canvas 
{
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;
   
   private GraphData data;
   private int textheight;
   private int labelincrement;
   private int tickincrement;
   private int numintervals;
   private int maxvalue;
   
   private double pixelincrement;
   private FontMetrics fm;
   
   public GraphYAxisCanvas()
   {
	  textheight = 0;
	  tickincrement = 0;
	  numintervals = 0;
	  pixelincrement = 0;
	  maxvalue = 0;
	  labelincrement = 0;
	  setBackground(MainWindow.runObject[myRun].background);
	  setForeground(MainWindow.runObject[myRun].foreground);
   }   
   public int getPreferredWidth()
   {
	  int w = 0;
	  
	  if(data == null)
		 return w;
		 
	  if(fm == null)
	  {
		 Graphics g = getGraphics();
		 fm = g.getFontMetrics(g.getFont());
		 g.dispose();
	  }
	  
	  int max = 0;
	  String s;
	  if(data.ymode == GraphData.TIME)
	  {
		 s = "(usec)";
		 if(data.xmode == GraphData.PROCESSOR)
			max = data.processor.maxTime;
		 else
			max = data.interval.maxTime;
	  }
	  else
	  {
		 s = "Msgs";
		 if(data.xmode == GraphData.PROCESSOR)
			max = data.processor.maxMsgs;
		 else
			max = data.interval.maxMsgs;
	  }                 
		 
	  if(fm != null)
		 w = fm.stringWidth("" + max) + fm.stringWidth(s) + 25;
	  
	  return w;
   }   
   public void paint(Graphics g)
   {
	  if(data == null)
		 return;
		 
	  int w = getSize().width;
	  int h = getSize().height - data.offset2;

	  if(fm == null)
		 fm = g.getFontMetrics(g.getFont());
	  
	 
	  g.setColor(MainWindow.runObject[myRun].background);
	  g.fillRect(0, 0, getSize().width, getSize().height);
	  g.setColor(MainWindow.runObject[myRun].foreground);
	  String s;
	  int y = h / 2;
	  if(data.ymode == GraphData.TIME)
	  {
		 s = "Time";
		 g.drawString(s, 5, y);
		 s = "(usec)";
		 g.drawString(s, 5, y + fm.getHeight() + 5);
	  }
	  else
	  {
		 s = "Msgs";
		 g.drawString(s, 5, y);
	  } 
	  
	  g.drawLine(w-5, data.offset, w-5, h - 1);                      
	  
	  int cury;
	  for(int i=0; i<=numintervals; i++)
	  {
		 cury = (int)(h - i*pixelincrement) - 1;
		 
		 if(i % labelincrement == 0)
		 {
			g.drawLine(w-10, cury, w, cury);
			s = "" + i*tickincrement;
			
			g.drawString(s, w-15-fm.stringWidth(s), cury + fm.getHeight()/2);
		 
		 }
		 else
		 {
			g.drawLine(w-7, cury, w-3, cury);
		 }
	  }         
 
   }   
   public void print(Graphics pg)
   {
	  ((Graphics2D)pg).setBackground(Color.white);
	  int w = getSize().width;
	  int h = getSize().height;
	  pg.clearRect(0, 0, w, h);
	  paint(pg);
	  ((Graphics2D)pg).setBackground(Color.black);
   }   
   public void setBounds(int x, int y, int w, int h)
   {
	  if(textheight == 0)
	  {
		 Graphics g = getGraphics();
		 FontMetrics fm = g.getFontMetrics(g.getFont());
		 textheight = fm.getHeight();
		 g.dispose();
	  } 
	  
	  if(data.ymode == GraphData.TIME)
	  {
		 if(data.xmode == GraphData.PROCESSOR)
			maxvalue = data.processor.maxTime;
		 else
			maxvalue = data.interval.maxTime;
	  }
	  else
	  {
		 if(data.xmode == GraphData.PROCESSOR)
			maxvalue = data.processor.maxMsgs;
		 else
			maxvalue = data.interval.maxMsgs;
	  }                    
	  
	  if(maxvalue <= 0)
	  {
		 tickincrement  = 1;
		 numintervals   = 0;
		 pixelincrement = 1;
		 labelincrement = 1;
	  }   
	  else
	  {
		 tickincrement = (int)Math.ceil(5/((double)(h - data.offset - data.offset2)/maxvalue));
		 tickincrement = Util.getBestIncrement(tickincrement);
		 numintervals = (int)Math.ceil((double)maxvalue/tickincrement);
		 pixelincrement = (double)(h - data.offset - data.offset2) / numintervals;
		 labelincrement = (int)Math.ceil((textheight + 10) / pixelincrement);
		 labelincrement = Util.getBestIncrement(labelincrement);   
	  }   
   
	  data.yscale = pixelincrement / tickincrement;
   
	  super.setBounds(x, y, w, h);
   }   
   public void setData(GraphData data)
   {
	  this.data = data;
   }   
}
