package projections.gui;

import java.awt.*;

public class ProfileDisplayCanvas extends Container
{
   private ProfileData data;
   private Image offscreen;
   
   public ProfileDisplayCanvas(ProfileData data)
   {
	  this.data = data;
	  offscreen = null;
   }   
   //Make sure we aren't made too tiny
   public Dimension getMinimumSize() {return new Dimension(150,100);}   
   public Dimension getPreferredSize() {return new Dimension(550,400);}   
   public void makeNewImage()
   {
	   if(data.plist == null)
		 return;
	  int w = getSize().width;
	  int h = getSize().height;
	  if(w > 0 && h > 0)
	  {
		 try
		 {
			offscreen = createImage(w, h);
		 }
		 catch(OutOfMemoryError e)
		 {
			System.out.println("NOT ENOUGH MEMORY!");
			return;
		 }      
		 
		 if(offscreen == null)
			return;
			
		 Graphics og = offscreen.getGraphics();

		 og.setClip(0, 0, w, h);
		
		 super.paint(og);
		 og.dispose();  
		 repaint(); 
	  }   
   }   
   public void paint(Graphics g)
   {
	  if(offscreen != null)
	  {
		 int x = data.profileWindow.getHSBValue();
		 int y = data.profileWindow.getVSBValue();
	  
		 g.drawImage(offscreen, x,y, x + data.vpw, y + data.vph, 
								x,y, x + data.vpw, y + data.vph, null);                  
	  }                          
   }   
   public void update(Graphics g)
   {
	  paint(g);
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
}