package projections.gui;

import java.awt.*;
import java.awt.event.*;

public class ProfileColorWindow extends Dialog 
{
   private ProfileData data;
   
   public ProfileColorWindow(Frame parent,ProfileData data)
   {
	  super(parent);
	  this.data = data;
   }   
   public void applyNewColor(Color c)
   {
   }   
}
