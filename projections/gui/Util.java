package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class Util 
{
   public static void gblAdd(Container target, Component c, GridBagConstraints gbc, 
	  int x, int y, int dx, int dy, int wx, int wy)
   {
	  gbc.gridx = x;
	  gbc.gridy = y;
	  gbc.gridwidth = dx;
	  gbc.gridheight = dy;
	  gbc.weightx = wx;
	  gbc.weighty = wy;
	  target.add(c, gbc);
   }   
   public static void gblAdd(Container target, Component c, GridBagConstraints gbc, 
	  int x, int y, int dx, int dy, int wx, int wy, int I1, int I2, int I3, int I4)
   {
	  Insets oldInsets = gbc.insets;
	  gbc.gridx = x;
	  gbc.gridy = y;
	  gbc.gridwidth = dx;
	  gbc.gridheight = dy;
	  gbc.weightx = wx;
	  gbc.weighty = wy;
	  gbc.insets = new Insets(I1, I2, I3, I4);
	  target.add(c, gbc);
	  gbc.insets = oldInsets;
   }   
   public static int getBestIncrement(int initialIncrement)
   {
	  int[] indices = {1, 2, 5, 25};
	  int best = -1;
	  for(int i=0; i<indices.length; i++)
	  {
		 int t=0;
		 int sum=0;
		 while((sum = (int)(indices[i] * Math.pow(10,t))) < initialIncrement)
			t++;
		 if((sum-initialIncrement) < (best-initialIncrement) || best < 0)
			best = sum;
	  }
	  return best;
   }   
   public static Menu makeMenu(Object parent, Object[] items, Object target)
   {  
	  Menu m = null;
	  if (parent instanceof Menu)
		 m = (Menu)parent;
	  else if (parent instanceof String)
		 m = new Menu((String)parent);
	  else
		 return null;

	  for (int i = 0; i < items.length; i++)
	  {  
		 if (items[i] instanceof String)
		 {  
			MenuItem mi = new MenuItem((String)items[i]);
			if (target instanceof ActionListener)
			   mi.addActionListener((ActionListener)target);
			m.add(mi);
		 }
		 else if (items[i] instanceof CheckboxMenuItem && target instanceof ItemListener)
		 {  
			CheckboxMenuItem cmi = (CheckboxMenuItem)items[i];
			cmi.addItemListener((ItemListener)target);
			m.add(cmi);
		 }
		 else if (items[i] instanceof MenuItem)
		 {  
			MenuItem mi = (MenuItem)items[i];
			if (target instanceof ActionListener)
			   mi.addActionListener((ActionListener)target);
			m.add(mi);
		 }
		 else if (items[i] == null) 
			m.addSeparator();
	  }

	  return m;
   }   
   public static void waitForImage(Component component, Image image) 
   {
	  MediaTracker tracker = new MediaTracker(component);
	  try 
	  {
		 tracker.addImage(image, 0);
		 tracker.waitForID(0);
	  }
	  catch(InterruptedException e) 
	  { 
		 e.printStackTrace(); 
	  }
   }   
   public static void wallPaper(Component component, Graphics  g, Image image) 
   {
	  Dimension compsize = component.getSize();
	  Util.waitForImage(component, image);

	  int patchW = image.getWidth(component);
	  int patchH = image.getHeight(component);
	  
	  for(int r=0; r < compsize.width; r += patchW) 
	  {
		 for(int c=0; c < compsize.height; c += patchH)
			g.drawImage(image, r, c, component);
	  }
   }   
}