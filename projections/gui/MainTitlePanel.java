package projections.gui;

import java.awt.*;

public class MainTitlePanel extends Component
{ 
   private FontMetrics fm;
   private int sw, sh;
   private String s;
   
   public MainTitlePanel(MainWindow mainWindow)
   {
	  setBackground(Color.black);
	  s = "PROJECTIONS";
	  sw = 0;
	  sh = 0;
	  Font f = new Font("SansSerif", Font.BOLD+Font.ITALIC, 40);
	  setFont(f);
   }   
   public Dimension getMinimumSize() 
   {
	  return getPreferredSize();
   }   
   public Dimension getPreferredSize() 
   {
	  if(fm == null)
	  {
		 Graphics g = getGraphics();
		 if(g != null)
		 {
			fm = g.getFontMetrics(getFont());
			sw = fm.stringWidth(s);
			sh = fm.getAscent();
			g.dispose();
		 }   
	  }
		
	  return new Dimension(sw+20, sh+20);
   }   
   public void paint(Graphics g)
   {
	  int w = getSize().width;
	  int h = getSize().height;
	  
	  if(fm == null)
	  {
		 fm = g.getFontMetrics(getFont());
		 sw = fm.stringWidth(s);
		 sh = fm.getAscent();
	  }   
	  
	  
	  int xpos = (w - sw)/2;
	  int ypos = (h + sh)/2;
	  
	  /*  
	  g.setColor(new Color(155, 50, 50));
	  g.drawString(s, xpos+4, ypos-10);
	  
	  g.setColor(new Color(50, 155, 50));
	  g.drawString(s, xpos+10, ypos+4);
	  
   //  g.setColor(new Color(155, 155, 50));
   //   g.drawString(s, xpos-8, ypos-10);
									  
	  g.setColor(new Color(50, 50, 155));
	  g.drawString(s, xpos-10, ypos-4);
	  
	  g.setColor(Color.gray);
	  g.drawString(s, xpos-2, ypos-2);
	  g.drawString(s, xpos+2, ypos+2);
	  */
	 
	  g.setColor(Color.white);
	  g.drawString(s, xpos, ypos);
	  
	  super.paint(g);
	  
   }   
}