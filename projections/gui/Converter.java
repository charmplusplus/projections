package projections.gui;


import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

class Converter 
{
   public native void runLog2QD(int[][][][] data, String name, int intervalSize);
   
   static 
   {
      System.loadLibrary("Converter"); 
   }
            
   public Converter()
   {
   }
   
   public void Run(int[][][][] d, String n, int is)
   {
      runLog2QD(d, n, is);
   }   
}



