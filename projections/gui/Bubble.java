package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class Bubble extends Window
implements MouseListener
{
   private FontMetrics fm;
   private String[] text;
	//private JPanel textPanel;
   //private Button closeButton;
   public Bubble(Component c, String[] s)
   {
	  super(getFrame(c));
	  
	  text = s;
	  
	  GridBagConstraints gbc = new GridBagConstraints();
	  GridBagLayout gbl = new GridBagLayout();
	  
	  gbc.fill = GridBagConstraints.BOTH;
	  
	  addMouseListener(this);
	  
	  setBackground(new Color(255, 255, 200));
	  setForeground(Color.black);
	  setSize(getPreferredSize());
    }   
   static Frame getFrame(Component c)
   {
	  Frame f = null;
	  
	  while((c = c.getParent()) != null)
		 if(c instanceof Frame)
			f = (Frame)c;
	  
	  return f;
   }   
	
	public void mouseClicked(MouseEvent e) {
		this.setVisible(false);
	}
	
	public void mouseEntered(MouseEvent e) {
	}
	
	public void mouseExited(MouseEvent e) {
	}
	
	public void mousePressed(MouseEvent e) {
	}
	
	public void mouseReleased(MouseEvent e){
	}
	
   public Dimension getMinimumSize()
   {
	  return getPreferredSize();
   }   
   public Dimension getPreferredSize()
   {
	  int w = 10;
	  int h = 10;
	  
	  if (fm == null) {
	      Graphics g = getGraphics();
	      if (g != null) {
		  //fm = g.getFontMetrics(g.getFont());
		  fm = g.getFontMetrics();
		  g.dispose();
	      }   
	  }
	  
	  if (text == null){
		return new Dimension(w, h);
	  }	 
	
	  if (fm != null) {
	      int sh = fm.getHeight()+2;
	      
	      h = sh * text.length + 6;
	      
	      w = fm.stringWidth(text[0]);
	      for (int i=1; i<text.length; i++)
		  w = Math.max(fm.stringWidth(text[i]), w);
	  
	      w += 16;
	  } else {
	      w = text[0].length();
	      for(int i=1; i<text.length; i++)
		  w = Math.max(text[i].length(), w);
	      w *= 8;	 
	      w += 8;
	      h = text.length * 15;
	      h += 8;
	  }
	  
	  return new Dimension(w, h);
   }   
   public void paint(Graphics g)
   {
	  if(text == null) 
		 return;
		 
	  int w = getSize().width;
	  int h = getSize().height;
	  
	  if(fm == null)
		 fm = g.getFontMetrics(g.getFont());
	  
	  int sh = fm.getHeight() + 2;   

	  
	  g.setColor(Color.black);
	  g.drawRect(0, 0, w-1, h-1);
	  
	  g.setColor(getForeground());
	  
	  for(int i=0; i<text.length; i++)
		 g.drawString(text[i], 4, (i+1)*sh );    
   }   
   public void setBubbleText(String[] s)
   {
	  text = s;
   }   
   public void setVisible(boolean state)
   {
	  pack();
	  super.setVisible(state);
   }   
}
