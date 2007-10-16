package projections.gui;

import java.awt.*;
import java.awt.event.*;

class MyButton extends Component
   implements MouseListener
{
   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
private String text;
   private FontMetrics fm;
   private boolean highlighted = false;
   private boolean pressed = false;
   private boolean enabled = true;
   private ActionListener actionListener = null;
   private Color border;
   
   public MyButton(String s, Color c)
   {
	  text = s;
	  border = c;
	  addMouseListener(this);
   }   
   public void addActionListener(ActionListener l)
   {
	  actionListener = AWTEventMulticaster.add(actionListener, l);
   }   
   public Dimension getMinimumSize()
   {
	  int w = 20;
	  int h = 20;
	  
	  if(fm == null)
	  {
		 Graphics g = getGraphics();
		 if(g != null)
			fm = g.getFontMetrics();
	  }
	  
	  if(fm != null)
	  {
		 w = fm.stringWidth(text)+20;
		 h = fm.getHeight()+10;
	  }
			   
	  return new Dimension(w, h);
   }   
   public Dimension getPreferredSize()
   {
	  return getMinimumSize();
   }   
   public void mouseClicked(MouseEvent evt)
   {
	  if(enabled)
		 processActionEvent();
   }   
   public void mouseEntered(MouseEvent evt)
   {
	  if(enabled && !highlighted)
	  {
		 highlighted = true;
		 repaint();
	  }   
   }   
   public void mouseExited(MouseEvent evt)
   {
	  if(enabled && (highlighted || pressed))
	  {
		 highlighted = false;
		 pressed     = false;
		 repaint();
	  } 
   }   
   public void mousePressed(MouseEvent evt)
   {
	  if(enabled)
	  {
		 pressed = true;
		 repaint();
	  }
   }   
   public void mouseReleased(MouseEvent evt)
   {
	  if(enabled)
	  {
		 pressed = false;
		 repaint();
	  }
   }   
   public void paint(Graphics g)
   {
	  int w = getSize().width;
	  int h = getSize().height;
	  
	  if(fm == null)
		 fm = g.getFontMetrics();
	  
	  g.setColor(border);      
	  g.drawRect(0,0,w-1,h-1);
	  
	  if(!enabled)
		 g.setColor(Color.gray);
	  else if(pressed)
		 g.setColor(Color.red);
	  else if(highlighted)
		 g.setColor(Color.yellow);
	  else 
		 g.setColor(Color.white);   

	  g.drawString(text, 5, (h+fm.getHeight())/2-3);  
	  super.paint(g);
   }   
   public void processActionEvent()
   {
	  if(enabled && actionListener != null)
	  {
		 ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "MyButton Action");
		 actionListener.actionPerformed(ae);
	  }
   }   
   public void removeActionListener(ActionListener l)
   {
	  actionListener = AWTEventMulticaster.remove(actionListener, l);
   }   
   public void setEnabled(boolean b)
   {
	  enabled = b;
	  repaint();
   }   
   public void update(Graphics g)
   {
	  paint(g);
   }   
}