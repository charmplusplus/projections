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
   private int[][] data;
   private Image offscreen;
   
   private Color[] colors;
   
   private int w, h;
   
   private AnimationWindow animationWindow;
   
   public AnimationDisplayPanel(AnimationWindow animationWindow)
   {
      this.animationWindow = animationWindow;
      setBackground(Color.black);
      setNumPsIs(Analysis.getNumProcessors(), 10);
      
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
   
   public void mouseEntered(MouseEvent evt)
   {}
   public void mouseExited(MouseEvent evt)
   {
      pold = -1;
      animationWindow.setStatusInfo(-1, -1, -1);
   }
   public void mouseClicked(MouseEvent evt)
   {}
   public void mousePressed(MouseEvent evt)
   {}
   public void mouseReleased(MouseEvent evt)
   {}   
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
   
   public void mouseDragged(MouseEvent evt)
   {}
   
   public void setCurI(int i)
   {
      curI = (i % numIs);
      if(curI < 0) curI += numIs;
     
      if(curP >= 0 && curP < numPs)
         animationWindow.setStatusInfo(curP, curI, data[curP][curI]);
      repaint();        
   }
   
   public int getCurI()
   {
      return curI;
   }         
   
   public void setNumPs(int p)
   {
      if(p > 0)
         setNumPsIs(p, numIs);
   }
   
   public void setNumIs(int i)
   {
      if(i > 0)
         setNumPsIs(numPs, i);
   }   
         
   private void setNumPsIs(int p, int i)
   {
      if(p != numPs || i != numIs)
      {
         numPs = p;
         numIs = i;
         data = Analysis.getAnimationData(numPs, numIs);
          
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
   
   private void clearScreen()
   {
      if(offscreen == null)
         return;
      
      Graphics og = offscreen.getGraphics();
      if(og != null)
         og.clearRect(0, 0, w, h);
      repaint();   
   }      
   
   public void update(Graphics g)
   {
      paint(g);
   }
      
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
   
   public void paint(Graphics g)
   {
      if(offscreen == null)
         return;
         
      makeNextImage(offscreen.getGraphics(), curI);
      g.drawImage(offscreen, 0, 0, null);
   }   
}                       
                      
