package projections.gui;

import java.awt.*;

public class ZItem
{
   String  name;
   String  type;
   String  parent;
   Color   color;
   boolean state;
   int[][] data;
   int[]   curPData;
   int[]   curIData;
   int     ymode;

   public ZItem()
   {
      name   = null;
      type   = null;
      parent = null;
      color  = null;
      state  = false;
      data   = null;
      curPData = null;
      curIData = null;
      ymode  = -1;
   }   
}   
   
