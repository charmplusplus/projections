package projections.gui;

import java.awt.*;
import java.awt.event.*;
import java.text.*;


public class ProfileObject extends Component
   implements MouseListener
{
   private float usage;
   private String name;
   private int processor;
   private String[] bubbletext;
   private Bubble bubble;

   private boolean inside = false;
   
   public ProfileObject(float u, String s, int p)
   {
      usage = u;
      name = s;
      processor = p;
      bubbletext = new String[3];
      bubbletext[0] = s;
      NumberFormat df = NumberFormat.getInstance();
      bubbletext[1] = "USAGE: " + df.format(u) + "%";
      // Made Change below for the average data
      if (p == -1)
	
	  bubbletext[2] = "Average"; 
      else
	  bubbletext[2] = "Processor " + p;
      addMouseListener(this);
   }   
   
   public float getUsage()
   {
      return usage;
   }   
   
   public String getName()
   {
      return name;
   }
   
   public void mouseEntered(MouseEvent evt)
   {
      if(!inside)
      {
         inside = true;
         ProfileObject po = ProfileObject.this;
         Point scrnloc = po.getLocationOnScreen();
         Dimension size = getSize();
      
         if(bubble == null)
            bubble = new Bubble(this, bubbletext);
      
         bubble.setLocation(scrnloc.x + size.width+5, scrnloc.y + evt.getY());
         bubble.setVisible(true);
      }     
   }      
   
   public void mouseExited(MouseEvent evt)
   {
      if(inside)
      {
         if(bubble != null)
         {
            bubble.dispose();
         }   
         inside = false;
      }      
   }
        
   public void mouseClicked(MouseEvent evt)
   {}           
   
   public void mouseReleased(MouseEvent evt)
   {}
   
   public void mousePressed(MouseEvent evt)
   {}
   
   
   public void paint(Graphics g)
   {
      int w = getSize().width;
      int h = getSize().height;
      
      Color c;
      if(name.equals("MESSAGE PACKING"))
         c = Color.pink;
      else if(name.equals("OVERHEAD"))
         c = Color.white;
      else if(name.equals("IDLE"))
         c = Color.white;
      else         
         c = getForeground();
      
      g.setColor(c);
      
      g.fillRect(0, 0, w, h);
      
      if(name.equals("OVERHEAD"))
      {
         g.setColor(Color.black);
         for(int i=0; i<h + w; i+=4)
         {
            g.drawLine(i, 0, 0, i);
            g.drawLine(i+1, 0, 0, i+1);
         }
      }      
      /*
      g.setColor(c.brighter());
      g.drawLine(0, 0, w, 0);
      g.drawLine(0, 0, 0, h);
      
      g.setColor(c.darker());
      g.drawLine(0, h-1, w, h-1);
      g.drawLine(w-1, 0, w-1, h-1);
      */
   }
}
         


