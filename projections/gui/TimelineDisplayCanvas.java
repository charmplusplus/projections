package projections.gui;

import java.awt.*;

public class TimelineDisplayCanvas extends Container
{
   private TimelineData data;
   private Image offscreen;
   
   public TimelineDisplayCanvas(TimelineData data)
   {
      this.data = data;
      offscreen = null;
   }
      
   public void makeNewImage()
   {
      if(data.tlh > 0 && data.tlw > 0)
      {
         try
         {
            offscreen = createImage(data.tlw, data.tlh);
         }
         catch(OutOfMemoryError e)
         {
            System.out.println("NOT ENOUGH MEMORY!");
            return;
         }      
         if(offscreen == null)
            return;
            
         Graphics og = offscreen.getGraphics();

         og.setClip(0, 0, data.tlw, data.tlh);
         int maxx = data.offset + (int)((data.endTime-data.beginTime)*data.pixelIncrement/data.timeIncrement);
         og.setColor(Color.gray);
         for(int i=0; i<data.numPs; i++)
         {
            int y = data.tluh/2 + i*data.tluh;
            og.drawLine(data.offset, y, maxx, y);
         }    
        
         super.paint(og);
         og.dispose();  
         repaint(); 
      }   
   }                             

   public void updateColors()
   {
      if(offscreen != null)
      {
         Graphics og = offscreen.getGraphics();
         super.paint(og);
         og.dispose();
         repaint();
      }   
   }
   
   public void update(Graphics g)
   {
      paint(g);
   }
   
   public void paint(Graphics g)
   {
      if(offscreen != null)
      {
         int x = data.timelineWindow.getHSBValue();
         int y = data.timelineWindow.getVSBValue();
      
         g.drawImage(offscreen, x,y, x + data.vpw, y + data.vph, 
                                x,y, x + data.vpw, y + data.vph, null);
      }                          
   }                     
}
   




