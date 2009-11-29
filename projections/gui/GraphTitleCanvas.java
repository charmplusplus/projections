package projections.gui;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class GraphTitleCanvas extends Canvas 
{

	// Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;

   private GraphData data;
   private FontMetrics fm;
   
   public GraphTitleCanvas()
   {
	  setBackground(MainWindow.runObject[myRun].background);
	  setForeground(MainWindow.runObject[myRun].foreground);
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
		 h = fm.getHeight() + 20;
	  
	  return h;
   }   
   public void paint(Graphics g)
   {
	  if(data == null)
		 return;
			
	  if(fm == null)
		 fm = g.getFontMetrics(g.getFont());
		 
	  String title;
	  if(data.xmode == GraphData.PROCESSOR)
	  {
		 title = "Interval";
		 if(data.interval.list.size() > 1)
			title += "s";
		 title += " " + data.interval.string;   
	  }           
	  else
	  {   
		 title = "Processor";
		 if(data.processor.list.size() > 1)
			title += "s";
		 title += " " + data.processor.string;  
	  } 
	  
	  int x = (getSize().width - fm.stringWidth(title))/2;
	  int y = (getSize().height + fm.getHeight())/2;

	  g.setColor(MainWindow.runObject[myRun].background);
	  g.fillRect(0, 0, getSize().width, getSize().height);
	  g.setColor(MainWindow.runObject[myRun].foreground);
	  g.drawString(title, x, y);    
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
   public void setData(GraphData data)
   {
	  this.data = data;
   }   
}
