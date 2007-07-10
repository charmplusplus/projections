package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class CallTableTextArea extends Panel
   implements AdjustmentListener
{
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    int myRun = 0;

   private Scrollbar VSB;
   private String[][] text;
   private FontMetrics fm;
   private int lineheight;
   private int titleheight;
   private Image offscreen;
   private int linenumwidth;
   private int timewidth;
   
   public CallTableTextArea()
   {
	  addComponentListener(new ComponentAdapter()
	  {
		 public void componentResized(ComponentEvent e)
		 {
			setBounds();  
		 }
	  });
	  
	  setLayout(null);
	  setBackground(Color.black);
	  
	  VSB = new Scrollbar(Scrollbar.VERTICAL, 0, 1, 0, 1);
	  VSB.setVisible(false);
	  VSB.setBackground(Color.lightGray);
	  VSB.addAdjustmentListener(this);
	  add(VSB);
   }   
   public void adjustmentValueChanged(AdjustmentEvent evt)
   {
	  repaint();
   }   
   //Make sure we aren't made too tiny
   public Dimension getMinimumSize() {return new Dimension(150,100);}   
   public Dimension getPreferredSize() {return new Dimension(500,300);}   
   public void paint(Graphics g)
   {
	  if(fm == null)
		 setBounds();
		 
	  if(text == null)
		 return; 
	  
	  Graphics og = g;
	  
	  if(offscreen != null) {
	      g = offscreen.getGraphics();
	  }
	      
	  int vsb = VSB.getValue();  
	  g.translate(0, -vsb);     
	  
	  int w = getSize().width - 20;
	  int h = getSize().height - titleheight;   
	  
	  int minline = (vsb - lineheight)/lineheight;
	  int maxline = (vsb - lineheight + h)/lineheight + 1;
	  
	  if(minline < 0) minline = 0;
	  if(maxline < minline) maxline = minline;
	  if(maxline > text.length-1) maxline = text.length-1;
 
	  g.setColor(Color.white);
	  String s;
	  int xpos, ypos;
	  for(int i=minline; i<=maxline; i++)
	  {
		 s = text[i][0];
		 if (s != "") {
		     char[] str = s.toCharArray();
		     if (str[0] != ' ')  //line is sourceEP
		         g.setColor(Color.green);
		     else if (str[8] != ' ')  //line is destEP
		         g.setColor(Color.yellow);
		     else  //line is stats
		         g.setColor(Color.white);
		 }
		 else
		     g.setColor(Color.white);
		 xpos = 10;
		 ypos = (i) * lineheight + titleheight;
		 g.drawString(s, xpos, ypos);
	  }
	  
/*
	  g.translate(0, vsb); 
	  g.clearRect(0, 0, w, titleheight);
	  g.setColor(Color.white);
	  s = "LINE";
	  xpos = (linenumwidth - fm.stringWidth(s))/2 + 10;
	  ypos = (titleheight + fm.getHeight())/2;
	  
	  g.drawString(s, xpos, ypos);
	  g.drawLine(xpos, ypos+2, xpos + fm.stringWidth(s), ypos+2);
	  
	  s = "TIME";
	  xpos = 10 + linenumwidth + 10 + (timewidth - fm.stringWidth(s))/2; 
	  g.drawString(s, xpos, ypos);
	  g.drawLine(xpos, ypos+2, xpos + fm.stringWidth(s), ypos+2);
	  
	  s = "EVENT";
	  xpos = 10 + linenumwidth + 10 + timewidth + 10;
	  g.drawString(s, xpos, ypos); 
	  g.drawLine(xpos, ypos+2, xpos + fm.stringWidth(s), ypos+2);
*/
	  
	  og.drawImage(offscreen, 0,0,w,h+titleheight, 0,0,w,h+titleheight, null);    
   }   
   public void setBounds()
   {
	 
	  if(fm == null)
	  {
		 Graphics g = getGraphics();
		 if(g != null)
		 {
			fm = g.getFontMetrics(g.getFont());
			lineheight = fm.getHeight() + 2;
		 }   
	  }
	  
	  if(fm == null || text == null)
		 return;
	  
	  int numlines = text.length;
	  int totalheight = lineheight * (numlines + 1);
	  titleheight = lineheight + 20;
	  linenumwidth = fm.stringWidth("" + numlines);
	  if(linenumwidth < fm.stringWidth("LINE")) linenumwidth = fm.stringWidth("LINE");
	  timewidth = fm.stringWidth("" + MainWindow.runObject[myRun].getTotalTime());
	  if(timewidth < fm.stringWidth("TIME")) timewidth = fm.stringWidth("TIME");
	  
	  int w = getSize().width - 20;
	  int h = getSize().height - titleheight;
	  

	  VSB.setBounds(w, titleheight, 20, h);
	  
	  if(totalheight > h)
	  {
		 VSB.setMaximum(totalheight);
		 VSB.setVisibleAmount(h);
		 VSB.setBlockIncrement(h);
		 VSB.setUnitIncrement(lineheight);
		 VSB.setVisible(true);
	  }
	  else
		 VSB.setVisible(false);
		 
	  try
	  {
		 offscreen = createImage(w, h+titleheight);
	  }
	  catch(OutOfMemoryError e)
	  {
		 System.out.println("NOT ENOUGH MEMORY!");  
	  }
	  
	  repaint();   
   }   
   public void setText(String[][] s)
   {
	  text = s;    
	  
	  if(s == null)
	  {
		 text = new String[1][2];
		 text[0][0] = "";
		 text[0][1] = "THIS LOG FILE IS EMPTY";
	  }

	  setBounds();
   }   
   public void update(Graphics g)
   {
	  int w = getSize().width - 20;
	  int h = getSize().height;
	  
	  if(offscreen != null)
	  {
		 Graphics og = offscreen.getGraphics();
		 og.clearRect(0, 0, w, h);
	  }
	  
	  paint(g);
   }   
}
