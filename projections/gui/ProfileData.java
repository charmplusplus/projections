package projections.gui;

import java.awt.*;

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
	  begintime = 0;
	  pstring   = "0";
	  endtime   = Analysis.getTotalTime();
	  offset    = 10;
	  plist     = null;
	  phaselist = null;
   }   
}