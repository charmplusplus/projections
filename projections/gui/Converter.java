package projections.gui;

class Converter 
{
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
   public native void runLog2QD(int[][][][] data, String name, int intervalSize);   
}