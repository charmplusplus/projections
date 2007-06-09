package projections.gui;

public class ProfileData
{
   ProfileWindow profileWindow;
   
   int vpw, vph;
   int dcw, dch;
   int numPs;
   int offset;
   OrderedIntList plist;
   OrderedIntList phaselist;
   String         pstring;
   long begintime, endtime;
   
   public ProfileData(ProfileWindow profileWindow)
   {
	  
	  this.profileWindow = profileWindow;
	  numPs     = Analysis.getNumProcessors();
	  
	  if(Analysis.checkJTimeAvailable() == true) { 
		begintime = Analysis.getJStart();
		endtime = Analysis.getJEnd();
		Analysis.setJTimeAvailable(false);}
	  else {
	  	begintime = 0;
	  	endtime   = Analysis.getTotalTime();}
	  
	  pstring   = "0";
	  offset    = 10;
	  plist     = null;
	  phaselist = null;
   }   
}