package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class AnimationDisplayPanel extends Panel
   implements MouseMotionListener , MouseListener
{
   private float MAXHUE = (float)0.65;
   private int numPs = -1;
   private int numIs = -1;
   private int pwidth;
   private int pheight;
   private int numrows;
   private int numcols;
   private int pold = -1;
   private int phoffset;
   private int pvoffset;
   private int hoffset;
   private int voffset;
   private float psize = (float)0.75;
   
   private int curI = 0;
   private int curP = -1;
   private int Isize = 0; //Interval length, microseconds
   private int[][] data;
   private Image offscreen;
   
   private Color[] colors;
   
   private int w, h;
   
   private AnimationWindow animationWindow;
   
   public AnimationDisplayPanel(AnimationWindow animationWindow)
   {
	  this.animationWindow = animationWindow;
	  setBackground(Color.black);
	  setNumPsIsize(Analysis.getNumProcessors(), 100*1000);
	  
	  addComponentListener(new ComponentAdapter()
	  {
		 public void componentResized(ComponentEvent evt)
		 {
			w = getSize().width;
			h = getSize().height;
			if(w > 0 && h > 0)
			{
			   offscreen = createImage(w, h); 
			   numcols = (int)Math.ceil(Math.sqrt((double)(w*numPs)/h));
			   if(numcols > numPs) 
				  numcols = numPs;
			   numrows = (int)Math.ceil((double)numPs/numcols);
			   
			   pwidth  = Math.min(w/numcols, h/numrows);
			   pheight = pwidth;
	  
			   hoffset  = (w-numcols*pwidth)/2;
			   voffset  = (h-numrows*pheight)/2;
			   phoffset = (int)((1-psize)*pwidth)/2;
			   pvoffset = (int)((1-psize)*pheight)/2;
			   clearScreen();
			}   
		 }
	  }); 
	  
	  addMouseMotionListener(this);
	  addMouseListener(this);  
	  
	  colors = new Color[101];
	  for(int i=0; i<=100; i++)
		 colors[i] = Color.getHSBColor((float)((100-i)/100.0)*MAXHUE,1,1);
   }   
   private void clearScreen()
   {
	  if(offscreen == null)
		 return;
	  
	  Graphics og = offscreen.getGraphics();
	  if(og != null)
		 og.clearRect(0, 0, w, h);
	  repaint();   
   }   
   public int getCurI()
   {
	  return curI;
   }   
   public int getIsize()
   {
	  return Isize;
   }   
   //Make sure we aren't made too tiny
   public Dimension getMinimumSize() {return new Dimension(150,100);}   
   public int getNumIs()
   {
	  return numIs;
   }   
   public Dimension getPreferredSize() {return new Dimension(550,400);}   
   public void makeNextImage(Graphics g, int I)
   {
	  int tothoffset = phoffset + hoffset;
	  int totvoffset = pvoffset + voffset;
	  int pw = (int)(pwidth*psize);
	  int ph = (int)(pheight*psize);
	  
	  g.translate(tothoffset, totvoffset);

	  int p = 0;
	  for(int r=0; r<numrows; r++)
	  {
		 for(int c=0; c<numcols; c++)
		 {
			int usage = data[p++][I];
			if(usage >=0 && usage <=100)
			{
			   g.setColor(colors[usage]);
			   g.fillRect(c*pwidth, r*pheight, pw, ph);           
			} 
			if(p >= numPs) break;             
		 }
	  }
   }   
   public void mouseClicked(MouseEvent evt)
   {}   
   public void mouseDragged(MouseEvent evt)
   {}   
   public void mouseEntered(MouseEvent evt)
   {}   
   public void mouseExited(MouseEvent evt)
   {
	  pold = -1;
	  animationWindow.setStatusInfo(-1, -1, -1);
   }   
   public void mouseMoved(MouseEvent evt)
   {
	  if(pwidth <=0 || pheight <=0)
		 return;
	  
	  int row = (evt.getY() - voffset) / pheight;
	  int col = (evt.getX() - hoffset) / pwidth;
	  
	  curP = row * numcols + col;
	  
	  if(curP >= numPs || curP < 0 || row < 0 || col < 0 || row >= numrows || col >= numcols)
		 curP = -1;
	  
	  if(curP != pold)
	  {
		 pold = curP;
		 if(curP >= 0)
			animationWindow.setStatusInfo(curP, curI, data[curP][curI]);
		 else
			animationWindow.setStatusInfo(-1, -1, -1);   
	  }   
   }   
   public void mousePressed(MouseEvent evt)
   {}   
   public void mouseReleased(MouseEvent evt)
   {}   
   public void paint(Graphics g)
   {
	  if(offscreen == null)
		 return;
		 
	  makeNextImage(offscreen.getGraphics(), curI);
	  g.drawImage(offscreen, 0, 0, null);
   }   
   public void setCurI(int i)
   {
	  curI = (i % numIs);
	  if(curI < 0) curI += numIs;
	 
	  if(curP >= 0 && curP < numPs)
		 animationWindow.setStatusInfo(curP, curI, data[curP][curI]);
	  repaint();        
   }   
   public void setIsize(int i)
   {
	  if(i > 0)
		 setNumPsIsize(numPs, i);
   }   
   public void setNumPs(int p)
   {
	  if(p > 0)
		 setNumPsIsize(p, Isize);
   }   
   private void setNumPsIsize(int p, int i)
   {
	  if(p != numPs || i != Isize)
	  {
		 numPs = p;
		 Isize = i;
		 data = Analysis.getAnimationData(numPs, Isize);
		 numIs = data[0].length;
		  
		 w = getSize().width;
		 h = getSize().height;
		 if(w>0 && h>0)
		 {
			numcols = (int)Math.ceil(Math.sqrt((double)(w*numPs)/h));
			if(numcols > numPs)
			   numcols = numPs;
			numrows = (int)Math.ceil((double)numPs/numcols);
	  
			pwidth  = Math.min(w/numcols, h/numrows);
			pheight = pwidth;
	  
			hoffset  = (w-numcols*pwidth)/2;
			voffset  = (h-numrows*pheight)/2;
			phoffset = (int)((1-psize)*pwidth)/2;
			pvoffset = (int)((1-psize)*pheight)/2;
		 
			clearScreen();
		 }   
	  }   
   }   
   public void update(Graphics g)
   {
	  paint(g);
   }   
}