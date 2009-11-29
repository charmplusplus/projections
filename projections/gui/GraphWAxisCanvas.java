package projections.gui;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class GraphWAxisCanvas extends Canvas 
{

	// Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;

   private GraphData data;
   private int width;
   private int textheight;
   private int labelwidth;
   private int labelincrement;
   private double deltay;
   
   public GraphWAxisCanvas()
   {
	  width = 0;
	  textheight = 0;
	  labelwidth = 0;
	  deltay = 0;
	  labelincrement = 0;
	  setBackground(MainWindow.runObject[myRun].background);
	  setForeground(MainWindow.runObject[myRun].foreground);
   }   
   public int getPreferredWidth()
   {
	  if(width == 0)
	  {
		 Graphics g = getGraphics();
		 if(g != null)
		 {
			FontMetrics fm = g.getFontMetrics(g.getFont());
			width = fm.stringWidth("" + 100) + fm.stringWidth("%") + 20;
			g.dispose();
		 }   
	  }
	  
	  return width;
   }   
   public void paint(Graphics g)
   {
	  if(data == null)
		 return;
		 
	  int w = getSize().width;
	  int h = getSize().height - data.offset2;

	  g.setColor(MainWindow.runObject[myRun].background);
	  g.fillRect(0, 0, getSize().width, getSize().height);

	  if(textheight == 0)
	  {
		 FontMetrics fm = g.getFontMetrics(g.getFont());
		 textheight = fm.getHeight();
		 labelwidth = fm.stringWidth("%");
	  }   
	 
	  g.setColor(MainWindow.runObject[myRun].foreground);
	  
	  g.drawString("%", w - 5 - labelwidth, h/2);
	  g.drawLine(5, data.offset, 5, h-1); 
	  
	  int cury;
	  for(int y=0; y<=100; y++)
	  {
		 cury = h - (int)(y * deltay)-1; 
			
		 if(y % labelincrement == 0)
		 {  
			g.drawLine(0, cury, 10, cury);
			cury += (int)(0.5*textheight); 
			g.drawString("" + y, 15, cury);
		 }
		 else
		 {
			g.drawLine(3, cury, 7, cury);
		 }
	  }                  
   }   
   public void print(Graphics pg)
   {
	  ((Graphics2D)pg).setBackground(Color.white);
	  setForeground(Color.black);
	  int w = getSize().width;
	  int h = getSize().height;
	  pg.clearRect(0, 0, w, h);
	  paint(pg);
	  ((Graphics2D)pg).setBackground(Color.black);
	  setForeground(Color.white);
   }   
   public void setBounds(int x, int y, int w, int h)
   {
	  if(textheight == 0)
	  {
		 Graphics g = getGraphics();
		 FontMetrics fm = g.getFontMetrics(g.getFont());
		 textheight = fm.getHeight();
		 labelwidth = fm.stringWidth("%");
		 g.dispose();
	  }    
	  
	  deltay = ((h - data.offset - data.offset2) / 100.0);
	  labelincrement = (int)(Math.ceil((textheight + 10) / deltay));
	  labelincrement = Util.getBestIncrement(labelincrement);
   
	  data.wscale = deltay;
	  
	  super.setBounds(x, y, w, h);
   }   
   public void setData(GraphData data)
   {
	  this.data = data;
   }   
}
