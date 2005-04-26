package projections.gui;

import java.awt.*;

public class GraphTitleCanvas extends Canvas 
{
   private GraphData data;
   private FontMetrics fm;
   
   public GraphTitleCanvas()
   {
	  setBackground(Analysis.background);
	  setForeground(Analysis.foreground);
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

	  g.setColor(Analysis.background);
	  g.fillRect(0, 0, getSize().width, getSize().height);
	  g.setColor(Analysis.foreground);
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
