package projections.gui;

import java.awt.*;

public class GraphXAxisCanvas extends Canvas 
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;

   private GraphData data;
   private FontMetrics fm;
   
   private int maxvalue;
   private int tickincrement, numintervals, labelincrement;
   private double pixelincrement;
   
   private Image offscreen;
   
   public GraphXAxisCanvas()
   {
	  setBackground(MainWindow.runObject[myRun].background);
	  setForeground(MainWindow.runObject[myRun].foreground);
   }   
   private void drawAxis(Graphics g)
   {
	  if(data == null)
		 return;
	  
	  int w  = getSize().width;
	  int h  = getSize().height;
	  
	  g.setColor(MainWindow.runObject[myRun].background);
	  g.fillRect(0, 0, w, h);
	
	  int hsbval = data.displayPanel.getHSBValue();
	  g.translate(-hsbval, 0);
	  
	  int mini = (int)Math.floor((hsbval - data.offset3)/pixelincrement);
	  int maxi = (int)Math.ceil((hsbval - data.offset3 + w)/pixelincrement);

	  data.minx = mini * tickincrement;
	  data.maxx = maxi * tickincrement;
	  
	  if(data.minx < 0) data.minx = 0;
	  if(data.maxx > maxvalue) data.maxx = maxvalue;
	  
	  if(mini < 0) mini = 0;
	  if(maxi > numintervals) maxi = numintervals;
	  
	  int linemin = data.offset3 + (int)(mini*pixelincrement);
	  int linemax = data.offset3 + (int)(maxi*pixelincrement);
	  if(data.graphtype == GraphData.BAR)
		 linemax += (int)pixelincrement;

	  g.setColor(MainWindow.runObject[myRun].foreground);
	  g.drawLine(linemin, 5, linemax, 5); 
	  
	  if(fm == null)
		 fm = g.getFontMetrics(g.getFont());
		 
	  String label;
	  if(data.xmode == GraphData.PROCESSOR)
		 label = "Processor";          
	  else  
		 label = "Time (in s)"; 
	  
	  int x = hsbval + (w - fm.stringWidth(label))/2;
	  int y = h - 5;

	  g.drawString(label, x, y); 
	  
	
	  
	  int curx;
	  String s;
	  for(int i=mini; i<=maxi; i++)
	  {
		 curx = data.offset3 + (int)(i*pixelincrement);
		 
		 if(data.graphtype == GraphData.BAR)
			curx += (int)(pixelincrement / 2);
		 
		 if(i % labelincrement == 0)
		 {
			g.drawLine(curx, 0, curx, 10);
			// gzheng
			// now the processor can be any subset of processors
			// not necessarily from minx to maxx
	  		if(data.xmode == GraphData.PROCESSOR)
			{
			  data.origProcList.reset();
			  int pe = data.origProcList.nextElement();
			  for (int j=0; j<i*tickincrement; j++)
			    pe = data.origProcList.nextElement();
			  s = "" + pe;
			}
			else{
			  double tickVal = ((double )(i*tickincrement+data.intervalStart)*data.interval.size)/(double )1000000;
			  s = "" + tickVal;
			 }
			
			g.drawString(s, curx-fm.stringWidth(s)/2, 15 + fm.getHeight());
		 
		 }
		 else
		 {
			g.drawLine(curx, 3, curx, 7);
		 }
	  } 
   }   
   public int getPreferredHeight()
   {
	  int h = 0;
	  
	  if(fm == null)
	  {
		 Graphics g = getGraphics();
		 if(g != null)
			fm = g.getFontMetrics(g.getFont());
	  }
	  
	  if(fm != null)
		 h = fm.getHeight() * 2 + 20;
	  
	  return h;
   }   
   public void paint(Graphics g)
   {
	  if(offscreen == null)
		 return;
	  
	  Graphics og = offscreen.getGraphics();
	
	  drawAxis(og);
	 
	  int w = getSize().width;
	  int h = getSize().height;
	  g.drawImage(offscreen, 0,0,w,h, 0,0,w,h, null);                  
   }   
   public void print(Graphics pg)
   {
	  ((Graphics2D)pg).setBackground(Color.white);
	  setForeground(Color.black);
	  
	  drawAxis(pg);
	  
	  ((Graphics2D)pg).setBackground(MainWindow.runObject[myRun].background);
	  setForeground(MainWindow.runObject[myRun].foreground);
   }   
   public void setBounds(int x, int y, int w, int h)
   {
	  if(fm == null)
	  {
		 Graphics g = getGraphics();
		 fm = g.getFontMetrics(g.getFont());
		 g.dispose();
	  } 
	  
	  if(data.xmode == GraphData.PROCESSOR)
		 maxvalue = data.processor.num - 1;
	  else
		 maxvalue = data.interval.num - 1;
	   
	  int sw = fm.stringWidth("" + maxvalue);  
	  data.offset3 = sw / 2;
	  
	  if(data.graphtype == GraphData.BAR)
		 maxvalue++;

	  int width = (int)(w * data.scale) - 2 * data.offset3;   
	 
	  tickincrement = (int)Math.ceil(5/((double)width/maxvalue));
	  tickincrement = Util.getBestIncrement(tickincrement);
	  numintervals = (int)Math.ceil((double)maxvalue/tickincrement);
	  pixelincrement = (double)width / numintervals;
	  labelincrement = (int)Math.ceil((sw + 20) / pixelincrement);
	  labelincrement = Util.getBestIncrement(labelincrement);    
   
	  data.xscale = pixelincrement/tickincrement;
	  
	  if(data.graphtype == GraphData.BAR)
	  {
		 numintervals--; 
		 maxvalue--;
	  }   
	  
	  // make offscreen image   
	  if(getSize().width != w || getSize().height != h)
	  {
		 try
		 {
			offscreen = createImage(w, h);
		 }
		 catch(OutOfMemoryError e)
		 {
			System.out.println("NOT ENOUGH MEMORY!");  
		 }   
	  }
	  super.setBounds(x, y, w, h);
   }   
   public void setData(GraphData data)
   {
	  this.data = data;
	  maxvalue = 0;
   }   
   public void update(Graphics g)
   {
	  paint(g);
   }   
}
