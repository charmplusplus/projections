package projections.gui;

class AmpiProfileData
{
//   AmpiProfileWindow profileWindow;
   
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
	private int myRun = 0;

   int numPs;
   OrderedIntList plist;
   long begintime, endtime;
   
   protected AmpiProfileData()
   {
	  
//	  this.profileWindow = profileWindow;
	  numPs     = MainWindow.runObject[myRun].getNumProcessors();
	  
	  if(MainWindow.runObject[myRun].checkJTimeAvailable() == true) { 
		begintime = MainWindow.runObject[myRun].getJStart();
		endtime = MainWindow.runObject[myRun].getJEnd();
		MainWindow.runObject[myRun].setJTimeAvailable(false);}
	  else {
	  	begintime = 0;
	  	endtime   = MainWindow.runObject[myRun].getTotalTime();}
	  
	  plist     = null;
   }   
}
