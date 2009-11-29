package projections.gui;

import java.awt.AWTEventMulticaster;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


public class ColorPanel extends Canvas
   implements MouseListener
{

	private int type;
    private int index;

   private int w, h;
   private Color background;
   private Color foreground;
   private int wDefault = 40;
   private int hDefault = 20;
   private boolean highlighted = false;
   
   private ActionListener actionListener = null;

   public ColorPanel(int type, int index, Color f)
   {
       this.type = type;
       this.index = index;
	  background = Color.black;
	  foreground = f;
	  w = wDefault;
	  h = hDefault;
	  addMouseListener(this);
   }   

   public ColorPanel(Color f)
   {
          background = Color.black;
          foreground = f;
          w = wDefault;
          h = hDefault;
          addMouseListener(this);
   }

   public void addActionListener(ActionListener l)
   {
	  actionListener = AWTEventMulticaster.add(actionListener, l);
   }   
   public Color getColor()
   {
	  return foreground;
   }   
   public Dimension getMinimumSize()
   {
	  return new Dimension(w, h);
   }   
   public Dimension getPreferredSize()
   {
	  return new Dimension(w, h);
   }   
   public void mouseClicked(MouseEvent evt)
   {
	  processActionEvent();
   }   
   public void mouseEntered(MouseEvent evt)
   {
	  if(!highlighted)
	  {
		 highlighted = true;
		 repaint();
	  }   
   }   
   public void mouseExited(MouseEvent evt)
   {
	  if(highlighted)
	  {
		 highlighted = false;
		 repaint();
	  }   
   }   
   public void mousePressed(MouseEvent evt)
   {
   }   
   public void mouseReleased(MouseEvent evt)
   {
   }   
   public void paint(Graphics g)
   {
	  g.setColor(background);
	  g.fillRect(0, 0, w, h);
	  
	  g.setColor(Color.gray);
	  g.drawLine(0, 0, w, 0);
	  g.drawLine(0, 1, w, 1);
	  g.drawLine(0, 0, 0, h);
	  g.drawLine(1, 0, 1, h);
	  
	  g.setColor(Color.white);
	  g.drawLine(0, h, w, h);
	  g.drawLine(0, h-1, w, h-1);
	  g.drawLine(w, h, w, 0);
	  g.drawLine(w-1, h, w-1, 0);
	  
	  g.setColor(foreground);
	  g.fillRect(5, 5, w-10, h-10);
	  
	  if(highlighted)
	  {
		 g.setColor(foreground.brighter().brighter());
		 g.drawRect(5, 5, w-10, h-10);
	  }   
   }   
   public void processActionEvent()
   {
	  if(actionListener != null)
	  {
		 ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "ColorPanel Action");
		 actionListener.actionPerformed(ae);
	  }
   }   

   public void setColor(Color c)
   {
	  foreground = c;
	  repaint();
   }   

    public int getType() {
	return type;
    }

    public int getIndex() {
	return index;
    }
}
