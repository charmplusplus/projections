package projections.analysis;

import java.lang.*;
import java.io.*;
import projections.misc.*;

/** This class contains the core logic behind the analysis of the data amd
 *  exports various functions by which to obtain information about part of
 *  the data as well as summary statistics.
 */
public class LogAnalyzer extends ProjDefs
{
   private int       NumIntervals;
   private String    FileName;
   private LogLoader LL;
   private Program   Prog;

   /** The default constructor.
    *  @param ArgV command line given to Projections when it is run
    */
   public LogAnalyzer (String ArgV) throws IOException
   {
       FileName = ArgV;
      LL       = new LogLoader(FileName);
      Prog     = LL.getprogram();
   }

  /* ********************** Graph functions ************************ */

   public long totaltime()
   {
      return (Prog.EndTime - Prog.BeginTime);
   }

   public int numpe()
   {
      return Prog.NumPe;
   }

   public int numuserentries ()
   {
       return LL.NumUserAttributes;
   }

   /** Gives the user entry points as read in from the .sts file as an array of
    *  two strings:  one for the name of the entry point with BOC or CHARE
    *  prepended to the front and a second containing the name of its parent
    *  BOC or chare.
    *  @return a two-dimensional array of Strings containing these records
    */
   public String[][] getuserentries()
   {
      return LL.GetUserAttributes();
   }
 
   /* ******************** Timeline functions ********************* */

   public MyLinkedList createtimeline (int PeNum, long Begin, long End)
      throws LogLoadException
   {
      LL.createtimeline (PeNum, Begin + Prog.BeginTime, End + Prog.BeginTime, true);
      return Prog.Timeline;
   }

   public long searchtimeline (int PeNum, int Entry, int Num)
      throws LogLoadException, EntryNotFoundException
   {
      return LL.searchtimeline (PeNum, Entry, Num);
   }

   /* ****************** Log file viewer functions **************** */

   public MyLinkedList viewlog (int Pe) throws LogLoadException
   {
      return LL.view (Pe);
   }
}








