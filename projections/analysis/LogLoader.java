package projections.analysis;

/** This class reads in .log files and turns them into a timeline.
 *  @author Sid Cammeresi
 *  @version 1.0
 */

import java.io.*;
import java.util.*;
import java.lang.*;
import projections.misc.*;
import projections.gui.*;

public class LogLoader extends ProjDefs
{
   private StsReader sts;
   private long    BeginTime, EndTime;
   private String validPEString;

    private int basePE, upperPE;
    private boolean validPERange;
    private StringBuffer validPEStringBuffer;
	  

   public LogLoader (StsReader Nsts) throws LogLoadException
   {
	  int              I;
	  int              Type;
	  long              Time;
	  int              Len;
	  // long              Begin;
	  long 	       back;
	  String           Line;
	  File testFile;
	  RandomAccessFile InFile;
	  StringTokenizer  st;

	  sts=Nsts;
	  ProgressDialog bar=new ProgressDialog("Finding end time...");

	  //Find the begin and end time across the parallel machine
	  BeginTime = 0;
	  EndTime   = Integer.MIN_VALUE;
	  int nPe=sts.getProcessorCount();

	  validPEStringBuffer = new StringBuffer();
	  validPERange = false;
	  basePE = -1;
	  upperPE = -1;
	  for(I = 0; I<nPe; I++)
	  {
		 bar.progress(I,nPe,I+" of "+nPe);
		 try
		 {
		     // test the file to see if it exists ...
		     testFile = new File(sts.getLogName(I));
		     if (testFile.exists() == false) {
			 System.out.println(sts.getLogName(I) +
					    " does not exist, ignoring.");
			 updatePEStringBuffer();
			 validPERange = false;
		     } else {

			InFile = new RandomAccessFile (testFile, "r");

			// success, so register processor as valid.
			registerPE(I);
			/*
			if (validPEStringBuffer.length() > 0) {
			    validPEStringBuffer.append(",");
			}
			validPEStringBuffer.append(String.valueOf(I));
			*/
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
				  Time = Long.parseLong(st.nextToken());
				  if (Time > EndTime)
		      			EndTime = Time;
		  		  break;
			   }   
			}
			
			InFile.close ();
		     }
		 }
		 catch (IOException E)
		 {
			System.out.println ("Couldn't read log file " + sts.getLogName(I));
		 }
	  }
	  updatePEStringBuffer();
	  validPEString = validPEStringBuffer.toString();

	  bar.done();
	  sts.setTotalTime(EndTime-BeginTime);
   }            
   public Vector createtimeline (int PeNum, long Begin, long End, Vector Timeline, Vector userEventVector)
	  throws LogLoadException
   {
	  int               Entry       = 0;
	  long              Time        = Long.MIN_VALUE;
	  boolean		LogMsgs     = true;
	  AsciiIntegerReader log        = null;
	  LogEntry          LE          = null;
	  TimelineEvent     TE          = null;
	  Hashtable         userEvents  = new Hashtable();  // store unfinished userEvents
	  UserEvent         userEvent   = null;  // jsut for temp purposes
	  TimelineMessage   TM          = null;
	  PackTime          PT          = null;
	  boolean tempte;
   
	  System.gc ();

	  // open the file
	  try
	  {
		log = new AsciiIntegerReader(
			new BufferedReader (new FileReader (sts.getLogName(PeNum))));

		log.nextLine();//First line contains junk

	 	while (true) { //Seek to time Begin
			LE = readlogentry(log);
			if (LE.Entry == -1) continue;

			if ((LE.TransactionType == BEGIN_PROCESSING) && (LE.Entry != -1))
			{
			   Time       = LE.Time - BeginTime;
			   Entry      = LE.Entry;
			}
			else if (LE.TransactionType == BEGIN_IDLE)
			   Time = LE.Time - BeginTime;
	  
			if(LE.Time >= Begin) break;
		 }
		 if (Time == Long.MIN_VALUE) Time = Begin;
		 if(LE.Time > End)
		 {
			switch (LE.TransactionType)
			{
			   case BEGIN_PROCESSING:
				  System.out.println ("finished empty timeline for " + PeNum);
				  log.close();
				  return Timeline;                              
			   case END_PROCESSING:
			   default:
				  System.out.println ("overlaid, end");
				  Timeline.addElement(TE=new TimelineEvent(
					  Begin - BeginTime,End   - BeginTime,
					  LE.Entry,LE.Pe));
				  log.close();
				  return Timeline;
			}
		 }

		 while(true)  //Throws EOFException at end of file; break if past endTime
		 {
			   if (LE.Entry != -1)
			   {
				  switch(LE.TransactionType)
				  {
					 case BEGIN_PROCESSING:
					//	Timeline.addElement(TE=new TimelineEvent(LE.Time - BeginTime,LE.Time - BeginTime,LE.Entry,LE.Pe,LE.MsgLen, LE.recvTime, LE.id));
						Timeline.addElement(TE=new TimelineEvent(LE.Time - BeginTime,LE.Time - BeginTime,LE.Entry,LE.Pe,LE.MsgLen, LE.recvTime, LE.id,LE.EventID));
						break;
					 case END_PROCESSING:
						if(TE!=null)
						   TE.EndTime = LE.Time - BeginTime;
						TE=null;
						break;
			   
					 case CREATION:
						tempte = false;
						if(TE==null) { //Start a new dummy event
						   Timeline.addElement(TE=new TimelineEvent(LE.Time-BeginTime,LE.Time-BeginTime,Entry,LE.Pe,LE.MsgLen));
						   tempte = true;
						}
						//TE.addMessage (TM=new TimelineMessage (LE.Time - BeginTime,LE.Entry,LE.MsgLen));
						TE.addMessage (TM=new TimelineMessage (LE.Time - BeginTime,LE.Entry,LE.MsgLen,LE.EventID));
						if (tempte) TE=null;
						break;
					 case USER_EVENT:
					        // don't mess with TE, that's just for EPs
					        userEventVector.addElement(new UserEvent(LE.Time-BeginTime,LE.MsgType,LE.EventID,UserEvent.SINGLE));
						break;
				         case USER_EVENT_PAIR:
						Integer key = new Integer(LE.EventID);
					        userEvent = (UserEvent) userEvents.get(key);
					        if (userEvent != null) {
						  // the next is a bit confusing
						  // basically there is a CharmEventID and an UserEventID (the id of the userEvent)
						  // but the log entry calls the CharmEventID just EventID and the UserEventID
						  if (userEvent.CharmEventID != LE.EventID || userEvent.UserEventID != LE.MsgType) {
						    System.out.println("WARN: LogLoader.createtimeline() USER_EVENT_PAIR does not match same EventID");
						  }
						  userEvent.EndTime = LE.Time-BeginTime;
						  userEvents.remove(key);
						  userEventVector.addElement(userEvent);
						}
						else { 
						  userEvent = new UserEvent(LE.Time-BeginTime,LE.MsgType,LE.EventID,UserEvent.PAIR); 
						  userEvents.put(key, userEvent);
						}
					        break;
					 case BEGIN_PACK:
						if(TE==null) //Start a new dummy event
						   Timeline.addElement(TE=new TimelineEvent(LE.Time-BeginTime,LE.Time-BeginTime,-1,LE.Pe));
						TE.addPack (PT=new PackTime(LE.Time-BeginTime));
						break;

					 case END_PACK:
						if(PT!=null)
						  PT.EndTime = LE.Time-BeginTime;
						PT=null;
						if (TE.EntryPoint == -1)
						   TE=null;
						break;

					 case BEGIN_IDLE:
						Timeline.addElement(TE=new TimelineEvent(
							LE.Time - BeginTime,Long.MAX_VALUE,
							-1,-1)); 
						break;
				  
					 case END_IDLE:
						if(TE!=null)   
						   TE.EndTime = LE.Time - BeginTime;
						TE=null;
						break;
				  }
			   }
			   LE = readlogentry (log);
			   if ((LE.Time - BeginTime) > End) break;
		 }
		 
		 // check to see if we are stopping in the middle of a message.  if so, we
		 // need to keep reading to get its end time
		 while(TE != null) {
			if (LE.TransactionType == END_PROCESSING)
			{
			   TE.EndTime = LE.Time - BeginTime;
			   TE=null;
			}
			LE = readlogentry (log);
		 }
		 log.close ();
	  }   
	  catch (EOFException e) { /*ignore*/ }   
	  catch (FileNotFoundException E)
	  {
		 System.out.println ("ERROR: couldn't open file " + sts.getLogName(PeNum));
	  }
	  catch (IOException E)
	  {
		 throw new LogLoadException (sts.getLogName(PeNum), LogLoadException.READ);
	  }
	  
	  return Timeline;
   }   
   private ViewerEvent entrytotext (LogEntry LE,StsReader sts)
   {
	  ViewerEvent VE = new ViewerEvent();
	  VE.Time        = LE.Time - BeginTime;
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
		   if ((LE.Entry != -1) && (LE.Entry != -1))
			{
			   String e2desc[][]=sts.getEntryNames();
			   VE.Dest = new String (e2desc[LE.Entry][1]
							+ "::" + e2desc[LE.Entry][0]);     
			   if(LE.TransactionType != CREATION)
				  VE.SrcPe = LE.Pe;
			   return VE;
			}
			else
			   return null;

		 case USER_EVENT:
	         case USER_EVENT_PAIR:
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
			Temp.Time    = log.nextLong();
			Temp.EventID = log.nextInt();
			Temp.Pe      = log.nextInt();
			return Temp;
	         case USER_EVENT_PAIR:
		        Temp.MsgType = log.nextInt();
			Temp.Time    = log.nextLong();
			Temp.EventID = log.nextInt();
			Temp.Pe      = log.nextInt();
			return Temp;
		 case BEGIN_IDLE:
		 case END_IDLE:
		 case BEGIN_PACK:
		 case END_PACK:
		 case BEGIN_UNPACK:
		 case END_UNPACK:
			Temp.Time    = log.nextLong();
			Temp.Pe      = log.nextInt();
			return Temp;
		 case BEGIN_PROCESSING:
			Temp.MsgType = log.nextInt();
			Temp.Entry   = log.nextInt();
			Temp.Time    = log.nextLong();
			Temp.EventID = log.nextInt();
			Temp.Pe      = log.nextInt();
			if (Analysis.getVersion() > 1.0)
			  Temp.MsgLen  = log.nextInt();
			else
			  Temp.MsgLen  = -1;
			if (Analysis.getVersion() >= 4.0) {
			  Temp.recvTime  = log.nextLong();
			  Temp.id = new ObjectId(log.nextInt(), log.nextInt(), log.nextInt());;
                        }
			return Temp;
		 case CREATION:
		 case END_PROCESSING:
			Temp.MsgType = log.nextInt();
			Temp.Entry   = log.nextInt();
			Temp.Time    = log.nextLong();
			Temp.EventID = log.nextInt();
			Temp.Pe      = log.nextInt();
			if (Analysis.getVersion() > 1.0)
			  Temp.MsgLen  = log.nextInt();
			else
			  Temp.MsgLen  = -1;
			return Temp;
		 case ENQUEUE:
		 case DEQUEUE:
			Temp.MsgType = log.nextInt();
			Temp.Time    = log.nextLong();
			Temp.EventID = log.nextInt();
			Temp.Pe      = log.nextInt();
			return Temp;
		 case INSERT:
		 case FIND:
		 case DELETE:
			Temp.MsgType = log.nextInt();
			Temp.Time    = log.nextLong();
			Temp.Time    = log.nextLong();
			Temp.Pe      = log.nextInt();
			return Temp;
		 case BEGIN_INTERRUPT:
		 case END_INTERRUPT:
			Temp.Time    = log.nextLong();
			Temp.EventID = log.nextInt();
			Temp.Pe      = log.nextInt();
			return Temp;
		 case BEGIN_COMPUTATION:
			Temp.Time    = log.nextLong();
			return Temp;
		 case END_COMPUTATION:
			Temp.Time    = log.nextLong();
			return Temp;
		 default:
			System.out.println ("ERROR: weird event type " + 
					    Temp.TransactionType);
			log.nextLine();  // ignore rest of this line
			return Temp;
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
			new BufferedReader(new FileReader (sts.getLogName(PeNum))));
	 log.nextLine();//First line is junk
	  
		 while(true)  //Throws EOFException at end of file
		 {
			LE = readlogentry (log);

			if (LE.Entry == -1) continue;
 
			if ((LE.Entry == Entry) && (LE.TransactionType == BEGIN_PROCESSING))
			   Count++;
		 
			if(Count > Num) break;
		 }
	  }   
	  catch (FileNotFoundException E)
	  {
		 System.out.println ("ERROR: couldn't open file " + sts.getLogName(PeNum));
	  }
	  catch (EOFException E) {/*ignore*/}
	  catch (IOException E)
	  {
		 throw new LogLoadException (sts.getLogName(PeNum), LogLoadException.READ);
	  }  
   
	  return LE.Time - BeginTime;
   }   
   public Vector view (int PeNum) throws LogLoadException
   {
	  AsciiIntegerReader log = null;
	  ViewerEvent    VE;
	  Vector ret = null;
	  String         Line;

	  try
	  {	  
		 ret = new Vector ();
		 log = new AsciiIntegerReader (
			new BufferedReader (new FileReader (sts.getLogName(PeNum))));
	 log.nextLine();//First line is junk
		 while(true) //Throws EOFException at end of file
		 {
			VE = entrytotext(readlogentry(log),sts);
			if (VE != null)
			   ret.addElement (VE);
		 }
	  }  
	  catch (FileNotFoundException E)
	  {
		 System.out.println ("ERROR: couldn't open file " + sts.getLogName(PeNum));
	  } 
	  catch (EOFException E)
	  {}
	  catch (IOException E)
	  {
		 System.out.println ("throwing....2");
		 throw new LogLoadException (sts.getLogName(PeNum), LogLoadException.READ);
	  }
		
	  return ret;
   }   

    public String getValidProcessorString() {
	return validPEString;
    }

    private void registerPE(int peIdx) {
	if (validPERange == false) {
	    basePE = peIdx;
	}
	upperPE = peIdx;
	validPERange = true;
    }

    private void updatePEStringBuffer() {
	if (!validPERange) {
	    return;
	}
	if (validPEStringBuffer.length() > 0) {
	    validPEStringBuffer.append(",");
	}
	if (upperPE > basePE) {
	    validPEStringBuffer.append(String.valueOf(basePE));
	    validPEStringBuffer.append("-");
	    validPEStringBuffer.append(String.valueOf(upperPE));
	} else if (upperPE == basePE) {
	    validPEStringBuffer.append(String.valueOf(basePE));
	} else {
	    // error. Should never happen.
	}
    }
}
