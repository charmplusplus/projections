/** This class reads in the files from the .sts and the various .log files
 *  so that they can be analyzed by the LogAnalyzer.
 *  @author Sid Cammeresi
 *  @version 1.0
 */

package projections.analysis;

import java.io.*;
import java.util.*;
import java.lang.*;
import projections.misc.*;

public class LogLoader extends ProjDefs
{
   public  int     NumBOCs            = 0;
   public  int     NumEntryPoints     = 0;
   public  int     NumCharesCreated   = 0;
   public  int     NumCharesProcessed = 0;
   public  int     NumMsgsCreated     = 0;
   public  int     NumMsgsProcessed   = 0;
   public  int     NumUserAttributes  = 0;
   private int     NumClasses         = 0;
   private int     EntryToAttribute[];
   private String  FileName;
   private String  ClassNames[];
   private String  UserAttributes[][];
   private Program Prog;

   public String[][] GetUserAttributes()
   {
      return UserAttributes;
   }   

   /** Constructor which reads in all of the data associated with the given path
    *  and program name.
    *  @param Name base of filename of logs to read in
    *  @exception IOException if an error occurs while loading the logs that
    *      prevented opening them
    */
   public LogLoader (String Name) throws LogLoadException
   {
      int              I;
      int              Type;
      int              Time;
      int              BeginTime; 
      int              EndTime; 
      int              Len;
      int              Begin;
      long 	       back;
      String           Line;
      RandomAccessFile InFile;
      StringTokenizer  st;

      FileName = new String(Name);
   
      readsts ();  // read in state file

      BeginTime = Integer.MAX_VALUE;
      EndTime   = Integer.MIN_VALUE;
      for(I = 0; I<Prog.NumPe; I++)
      {
         try
         {
            InFile = new RandomAccessFile (FileName + "." + I + ".log", "r");  
			//Look for the computation start time at the beginning of the file
            Line   = InFile.readLine();
            while(true)  //Throws EOFException at end of file
            {
               Line = InFile.readLine();
               st = new StringTokenizer(Line);
               if(Integer.parseInt(st.nextToken()) == BEGIN_COMPUTATION)
               {
                  Time = Integer.parseInt(st.nextToken());
                  if(Time < BeginTime)
		      BeginTime = Time;
		  break;
               }
            }         
	    
	    back = InFile.length()-80*3; //Seek to the end of the file
	    if (back < 0) back = 0;
            InFile.seek(back);
            while(InFile.readByte() != '\n');
            while(true)  //Throws EOFException at end of file
            {
               Line = InFile.readLine();
               st   = new StringTokenizer(Line);
               if(Integer.parseInt(st.nextToken()) == END_COMPUTATION)
               {
                  Time = Integer.parseInt(st.nextToken());
                  if (Time > EndTime)
		      EndTime = Time;
		  break;
               }   
            }
            
            InFile.close ();
         }
         catch (IOException E)
         {
            System.out.println ("Couldn't read log file " + FileName + "." + I + ".log");
         }
      }   
      Prog.BeginTime = BeginTime;
      Prog.EndTime   = EndTime;
   }

   /** Read in the .sts file and fill in our instance of the Program class.
   *  @exception LogLoadException if an error occurs while reading in the
   *      the state file
   */
   void readsts () throws LogLoadException   
   {
      int            I;
      int            ID;
      int            ChareID;
      int            MsgID;
      int            MsgIndex;
      int            Size;
      int            PseudoIndex;
      int            PseudoType;
      String         Line;
      String         Type;
      String         Name;
      BufferedReader InFile;
      Entry          TempEntry;
  
      try
      {

         InFile = new BufferedReader(new InputStreamReader(new FileInputStream(FileName + ".sts")));

         Prog = new Program();

         while((Line = InFile.readLine()) != null)
         {
            StringTokenizer st = new StringTokenizer(Line);
            String s1 = st.nextToken();
      
            if(s1.equals("MACHINE"))
               Prog.Machine = st.nextToken();
            else if(s1.equals("PROCESSORS"))
            {
               Prog.NumPe = Integer.parseInt(st.nextToken());
            }
            else if(s1.equals("TOTAL_CHARES"))
            {
               Prog.TotalChares = Integer.parseInt(st.nextToken());
               Prog.ChareList   = new Chare[Prog.TotalChares];
               ClassNames       = new String[Prog.TotalChares];
            }
            else if(s1.equals("TOTAL_EPS"))
            {
               Prog.TotalEPS    = Integer.parseInt(st.nextToken());
               UserAttributes   = new String[Prog.TotalEPS][2];
               EntryToAttribute = new int[Prog.TotalEPS];
               for (I=0; I<Prog.TotalEPS; I++)
                  EntryToAttribute[I] = -1;
            }
            else if(s1.equals("TOTAL_MSGS"))
            {
               Prog.TotalMsgs = Integer.parseInt(st.nextToken());
               Prog.MsgTable  = new long[Prog.TotalMsgs];
            }
            else if(s1.equals("TOTAL_PSEUDOS"))
            {
               Prog.TotalPseudos = Integer.parseInt (st.nextToken());
               Prog.PseudoTable  = new Pseudo [Prog.TotalPseudos];
            }
            else if(s1.equals("TOTAL_EVENTS"))
            {
               Prog.TotalEvents = Integer.parseInt(st.nextToken());
            }
            else if(s1.equals("CHARE") || Line.equals("BOC"))
            {
               ID = Integer.parseInt(st.nextToken());
               Prog.ChareList[ID]            = new Chare();
               Prog.ChareList[ID].ChareID    = ID;
               Prog.ChareList[ID].NumEntries = 0;
               Prog.ChareList[ID].Name       = st.nextToken();
               Prog.ChareList[ID].Type       = new String(s1);
               Prog.ChareList[ID].EntryList  = null;
               Prog.NumChares++;
               ClassNames[NumClasses++]      = Prog.ChareList[ID].Name;
            }
            else if(s1.equals("ENTRY"))
            {
               Type    = st.nextToken();
               ID      = Integer.parseInt(st.nextToken());
               Name    = st.nextToken();
               ChareID = Integer.parseInt(st.nextToken());
               MsgID   = Integer.parseInt(st.nextToken());
      
               TempEntry         = new Entry ();
               TempEntry.Number  = ID;
               TempEntry.ChareID = ChareID;
               TempEntry.Name    = Name; // need to strip some stuff off
               TempEntry.Index   = Prog.NumEntries;

               Prog.NumEntries++;

               UserAttributes[NumUserAttributes][0] = TempEntry.Name;
               UserAttributes[NumUserAttributes][1] = new String (Type + " " + ClassNames [ChareID]);

               EntryToAttribute[ID] = NumUserAttributes++;
            }
            else if(s1.equals("MESSAGE"))
            {  
               MsgIndex                = Integer.parseInt(st.nextToken());
               Size                    = Integer.parseInt(st.nextToken());
               Prog.MsgTable[MsgIndex] = Size;
            }
            else if(s1.equals("PSEUDO"))
            {
               PseudoIndex = Integer.parseInt(st.nextToken());
               PseudoType  = Integer.parseInt(st.nextToken());
               Name        = st.nextToken();
               Prog.PseudoTable[Prog.NumPseudos]       = new Pseudo ();
               Prog.PseudoTable[Prog.NumPseudos].Index = PseudoIndex;
               Prog.PseudoTable[Prog.NumPseudos].Type  = PseudoType;
               Prog.PseudoTable[Prog.NumPseudos].Name  = Name;
               Prog.NumPseudos++;
            }
            else if (s1.equals ("END"))
               break;
         }
         InFile.close();
      }
      catch (FileNotFoundException E)
      {
         throw new LogLoadException (FileName + ".sts", LogLoadException.OPEN);
      }
      catch (IOException E)
      {
         throw new LogLoadException (FileName + ".sts", LogLoadException.READ);
      }
   }

   /** Read in one event from the currently open log, create an instance of
    *  LogEntry to hold it, and fill in the fields appropriate to the type of
    *  event that is indicated by Temp.TransactionType.
    *  @return a reference to the entry read in
    *  @exception EOFException if it encounters the end of the file
    */
   LogEntry readlogentry (AsciiIntegerReader log) throws IOException
   {   
      LogEntry Temp = new LogEntry();   
      
      Temp.TransactionType = log.nextInt();
   
      switch(Temp.TransactionType)
      {
         case USER_EVENT:
            Temp.MsgType = log.nextInt();
            Temp.Time    = log.nextInt();
            Temp.EventID = log.nextInt();
            Temp.Pe      = log.nextInt();
            return Temp;
         case BEGIN_IDLE:
         case END_IDLE:
         case BEGIN_PACK:
         case END_PACK:
         case BEGIN_UNPACK:
         case END_UNPACK:
            Temp.Time    = log.nextInt();
            Temp.Pe      = log.nextInt();
            return Temp;
         case CREATION:
         case BEGIN_PROCESSING:
         case END_PROCESSING:
            Temp.MsgType = log.nextInt();
            Temp.Entry   = log.nextInt();
            Temp.Time    = log.nextInt();
            Temp.EventID = log.nextInt();
            Temp.Pe      = log.nextInt();

            if(Temp.TransactionType == CREATION)
            {
               if(Temp.Entry != -1)
                  NumMsgsCreated++; 
            
               if(Temp.MsgType == NEW_CHARE_MSG)
               {
                  if(Temp.Entry != -1)
                     NumCharesCreated++;
               }
               else if(Temp.MsgType == BOC_INIT_MSG)
               {
                  if(Temp.Pe == 0)
                     NumBOCs++;
               }      
            }
            else if(Temp.TransactionType == BEGIN_PROCESSING)
            {
               if(Temp.Entry != -1)
               {
                  NumMsgsProcessed++;
                  if(Temp.MsgType == NEW_CHARE_MSG)
                     NumCharesProcessed++;
               }
            } 
            return Temp;
         case ENQUEUE:
         case DEQUEUE:
            Temp.MsgType = log.nextInt();
            Temp.Time    = log.nextInt();
            Temp.EventID = log.nextInt();
            Temp.Pe      = log.nextInt();
            return Temp;
         case INSERT:
         case FIND:
         case DELETE:
            Temp.MsgType = log.nextInt();
            Temp.Time    = log.nextInt();
            Temp.Time    = log.nextInt();
            Temp.Pe      = log.nextInt();
            return Temp;
         case BEGIN_INTERRUPT:
         case END_INTERRUPT:
            Temp.Time    = log.nextInt();
            Temp.EventID = log.nextInt();
            Temp.Pe      = log.nextInt();
            return Temp;
         case BEGIN_COMPUTATION:
            Temp.Time    = log.nextInt();
            return Temp;
         case END_COMPUTATION:
            Temp.Time    = log.nextInt();
            return Temp;
         default:
            System.out.println ("ERROR: weird event type " + Temp.TransactionType);
            return Temp;
      }
   }
   
   /** Method to obtain summary information about the program's execution.
    *  @return an instance of Program
    */
   public Program getprogram ()
   {
      return Prog;
   }

   public void createtimeline (int PeNum, long Begin, long End, boolean LogMsgs)
      throws LogLoadException
   {
      int               NumLevels   = 0;
      int               Entry       = 0;
      long              Time        = Long.MIN_VALUE;
      boolean           Started     = false; 
      boolean           Processing  = false;
      boolean           Overlap     = false;
      AsciiIntegerReader log        = null;
      LogEntry          LE          = null;
      TimelineEvent     TE          = null;
      TimelineMessage   TM          = null;
      PackTime          PT          = null;
   
      Prog.Timeline = null;
      System.gc ();

      // open the file
      try
      {
         Prog.Timeline = new MyLinkedList();
         log = new AsciiIntegerReader(
			new BufferedReader (new FileReader (FileName + "." + PeNum + ".log")));

         log.nextLine();//First line contains junk

	 while (true) { //Seek to time Begin
            LE = readlogentry(log);
            if (LE.Entry == -1) continue;

            if ((LE.TransactionType == BEGIN_PROCESSING) && (EntryToAttribute [LE.Entry] != -1))
            {
               Processing = true;
               Time       = LE.Time - Prog.BeginTime;
               Entry      = EntryToAttribute [LE.Entry];
            }
            else if ((LE.TransactionType == END_PROCESSING) && (EntryToAttribute [LE.Entry] != -1))
               Processing = false;
            else if (LE.TransactionType == BEGIN_IDLE)
               Time = LE.Time - Prog.BeginTime;
      
            if(LE.Time >= Begin) break;
         }
         if(LE.Time > End)
         {
            switch (LE.TransactionType)
            {
               case BEGIN_PROCESSING:
                  System.out.println ("finished empty timeline for " + PeNum);
                  return;                              
               case END_PROCESSING:
               default:
                  System.out.println ("overlaid, end");
                  Overlap = true;

                  TE            = new TimelineEvent ();
                  TE.BeginTime  = Begin - Prog.BeginTime;
                  TE.EndTime    = End   - Prog.BeginTime;
                  TE.EntryPoint = EntryToAttribute[LE.Entry];
                  TE.SrcPe      = LE.Pe;
                  TE.MsgsSent   = new MyLinkedList ();
                  TE.PackTimes = new MyLinkedList ();
                  Prog.Timeline.pushrear (TE);
                  break;
            }
         }   

         if (!Overlap)
         {
            while(true)  //Throws EOFException at end of file
            {
               if (LE.Entry != -1)
               {
                  switch(LE.TransactionType)
                  {
                     case BEGIN_PROCESSING:
                        if(EntryToAttribute [LE.Entry] == -1) break;
                  
                        if(!Processing)
                        {
                           TE            = new TimelineEvent ();
                           TE.BeginTime  = LE.Time - Prog.BeginTime;
                           TE.EndTime    = Long.MAX_VALUE;
                           TE.EntryPoint = EntryToAttribute [LE.Entry];
                           TE.SrcPe      = LE.Pe;
                           TE.MsgsSent   = new MyLinkedList();
                           TE.PackTimes  = new MyLinkedList();

                           Prog.Timeline.pushrear (TE);
                           Started    = true;
                           Processing = true;
                        }
                        else if(Started)
                        {  
                           NumLevels++;
                        }
                        break;
                     case END_PROCESSING:
                        if(Processing)
                        {   
                           if(Started)
                           {
                              if(NumLevels == 0)
                              {
                                 TE.EndTime = LE.Time - Prog.BeginTime;
                                 Processing = false;
                              }  
                              else if(NumLevels > 0) 
                              {
                                 NumLevels--;
                              }
                           }
                           else 
                           {
                              TE            = new TimelineEvent();
                              TE.BeginTime  = Time;
                              TE.EndTime    = LE.Time - Prog.BeginTime;
                              TE.EntryPoint = EntryToAttribute[LE.Entry];
                              TE.SrcPe      = LE.Pe;
                              TE.MsgsSent   = new MyLinkedList();
                              TE.PackTimes  = new MyLinkedList();

                              Prog.Timeline.pushrear (TE);
                              Started    = true;
                              Processing = false;
                           }
                        }
                        break;
               
                     case CREATION:
                        if(EntryToAttribute[LE.Entry] == -1) break;
                        if(Processing)
                        {
                           if(Started)
                           {
                              TM       = new TimelineMessage ();
                              TM.Time  = LE.Time - Prog.BeginTime;
                              TM.Entry = EntryToAttribute [LE.Entry];
                              TE.MsgsSent.pushrear (TM);
                           }
                           else   
                           {
                              TE            = new TimelineEvent();
                              TE.BeginTime  = Time;
                              TE.EndTime    = Long.MAX_VALUE;
                              TE.EntryPoint = Entry;
                              TE.SrcPe      = LE.Pe;
                              TE.MsgsSent   = new MyLinkedList();
                              TE.PackTimes  = new MyLinkedList();

                              Prog.Timeline.pushrear (TE);
                              Started = true;

                              TM       = new TimelineMessage ();
                              TM.Time  = LE.Time - Prog.BeginTime;
                              TM.Entry = EntryToAttribute[LE.Entry];

                              TE.MsgsSent.pushrear (TM);
                           }   
                        }
                        break;
					
                     case USER_EVENT:
                        if(Processing)
                        {
                           if(Started)
                           {
                              TM       = new TimelineMessage ();
                              TM.Time  = LE.Time - Prog.BeginTime;
                              TM.Entry = -LE.MsgType;
                              TE.MsgsSent.pushrear (TM);
                           }
                           else   
                           {
                              TE            = new TimelineEvent();
                              TE.BeginTime  = Time;
                              TE.EndTime    = Long.MAX_VALUE;
                              TE.EntryPoint = Entry;
                              TE.SrcPe      = LE.Pe;
                              TE.MsgsSent   = new MyLinkedList();
                              TE.PackTimes  = new MyLinkedList();

                              Prog.Timeline.pushrear (TE);
                              Started = true;

                              TM       = new TimelineMessage ();
                              TM.Time  = LE.Time - Prog.BeginTime;
                              TM.Entry = -LE.MsgType;

                              TE.MsgsSent.pushrear (TM);
                           }   
                        }
                        break;

                     case BEGIN_PACK:
                        if(Processing && Started)
                        {
                           PT           = new PackTime();
                           PT.BeginTime = LE.Time;
                           TE.PackTimes.pushrear (PT);
                        }
                        break;

                     case END_PACK:
                        if(Processing && Started)   
                           PT.EndTime = LE.Time;
                        break;

                     case BEGIN_IDLE:   
                        TE            = new TimelineEvent ();
                        TE.BeginTime  = LE.Time - Prog.BeginTime;
                        TE.EndTime    = Long.MAX_VALUE;
                        TE.EntryPoint = -1;
                        TE.SrcPe      = -1;
                        TE.MsgsSent   = TE.PackTimes = null;
                        Prog.Timeline.pushrear (TE);
                        Started = true;
                        break;
                  
                     case END_IDLE:
                        if(Started)   
                           TE.EndTime = LE.Time - Prog.BeginTime;
                        else   
                        {
                           TE            = new TimelineEvent ();
                           TE.BeginTime  = Time;
                           TE.EndTime    = LE.Time - Prog.BeginTime;
                           TE.EntryPoint = -1;
                           TE.SrcPe      = -1;
                           TE.MsgsSent   = TE.PackTimes = null;
                           Prog.Timeline.pushrear (TE);
                           Started = true;
                        }
                        break;
                  }
               }
               LE = readlogentry (log);
               if ((LE.Time - Prog.BeginTime) > End) break;
            }
         }  
      }      
      catch (FileNotFoundException E)
      {
         System.out.println ("ERROR: couldn't open file " + FileName + "." + PeNum + ".log");
      }
	  catch (EOFException e) {
	    /*ignore*/
	  }
      catch (IOException E)
      {
         throw new LogLoadException (FileName + "." + PeNum + ".log", LogLoadException.READ);
      }

      // check to see if we are stopping in the middle of a message.  if so, we
      // need to keep reading to get its end time
      if (TE != null)
      {
         if (TE.EndTime == Long.MAX_VALUE)
         {
            if (LE.TransactionType == END_PROCESSING)
               TE.EndTime = LE.Time - Prog.BeginTime;
            else
            {
               try
               {
                  //System.out.println("stopped in middle");
                  while(true)
                  {
                     LE = readlogentry (log);
                       
                     if (LE.TransactionType == END_PROCESSING)
                     {
                        TE.EndTime = LE.Time - Prog.BeginTime;
                        break;
                     }
                  }
               }
               catch (EOFException E)
               {}
               catch (IOException E)
               {}
            }      
         }
      }   
      else
         System.out.println ("TE is null");

      // close the file
      try
      {
         log.close ();
      }
      catch (IOException E)
      {
         System.out.println ("ERROR: couldn't close file");
         System.exit (1);
      }
   }

   public long searchtimeline (int PeNum, int Entry, int Num)
      throws LogLoadException, EntryNotFoundException
   {
      long           Count = 0;
      LogEntry       LE     = null;
      AsciiIntegerReader log = null;

      System.out.println ("looking through log for processor " + PeNum);

      // open the file
      try
      {
         System.gc ();
         log = new AsciiIntegerReader(
			new BufferedReader(new FileReader (FileName + "." + PeNum + ".log")));
	 log.nextLine();//First line is junk
      
         while(true)  //Throws EOFException at end of file
         {
            LE = readlogentry (log);

            if (LE.Entry == -1) continue;
 
            if ((EntryToAttribute [LE.Entry] == Entry) && (LE.TransactionType == BEGIN_PROCESSING))
               Count++;
         
            if(Count > Num) break;
         }
      }   
      catch (FileNotFoundException E)
      {
         System.out.println ("ERROR: couldn't open file " + FileName + "." + PeNum + ".log");
      }
	  catch (EOFException E) {/*ignore*/}
      catch (IOException E)
      {
         throw new LogLoadException (FileName + "." + PeNum + ".log", LogLoadException.READ);
      }  
   
      return LE.Time - Prog.BeginTime;
   }

   private ViewerEvent entrytotext (LogEntry LE)
   {
      ViewerEvent VE = new ViewerEvent();
      VE.Time        = LE.Time - Prog.BeginTime;
      VE.EventType   = LE.TransactionType;

      switch (LE.TransactionType)
      {
         case BEGIN_IDLE:
         case END_IDLE:
         case BEGIN_PACK:
         case END_PACK:
         case BEGIN_UNPACK:
         case END_UNPACK:
            return VE;
         
         case CREATION:
         case BEGIN_PROCESSING:
         case END_PROCESSING:
         case ENQUEUE:
           if ((LE.Entry != -1) && (EntryToAttribute[LE.Entry] != -1))
            {
               VE.Dest = new String (UserAttributes[EntryToAttribute[LE.Entry]]
                       [1] + "::" + UserAttributes[EntryToAttribute[LE.Entry]][0]);     
               if(LE.TransactionType != CREATION)
                  VE.SrcPe = LE.Pe;
               return VE;
            }
            else
               return null;

         case USER_EVENT:
         case DEQUEUE:
         case INSERT:
         case FIND:
         case DELETE:
         case BEGIN_INTERRUPT:
         case END_INTERRUPT:
         default:
            return null;
      }
   }

   public MyLinkedList view (int PeNum) throws LogLoadException
   {
      AsciiIntegerReader log = null;
      ViewerEvent    VE;
      MyLinkedList     List = null;
      String         Line;

      try
      {	  
         List = new MyLinkedList ();
         log = new AsciiIntegerReader (
			new BufferedReader (new FileReader (FileName + "." + PeNum + ".log")));
	 log.nextLine();//First line is junk
         while(true) //Throws EOFException at end of file
         {
            VE = entrytotext(readlogentry(log));
            if (VE != null)
               List.pushrear (VE);
         }
      }  
      catch (FileNotFoundException E)
      {
         System.out.println ("ERROR: couldn't open file " + FileName + "." + PeNum + ".log");
      } 
      catch (EOFException E)
      {}
      catch (IOException E)
      {
         System.out.println ("throwing....2");
         throw new LogLoadException (FileName + "." + PeNum + ".log", LogLoadException.READ);
      }
		
      return List;
   }
}








