package projections.analysis;

/** This class reads in .log files and turns them into a timeline.
 *  @author Sid Cammeresi
 *  @version 1.0
 */

import java.io.*;
import java.util.*;
import java.lang.*;

import javax.swing.*;

import projections.gui.*;
import projections.misc.*;

public class LogLoader extends ProjDefs
{
    private long BeginTime, EndTime;
    private String validPEString;
    
    private int basePE, upperPE;
    private boolean validPERange;
    private StringBuffer validPEStringBuffer;

    // **CW** register previous event timestamp to support delta encoding.
    private long prevTime = 0;
    private boolean deltaEncoded = false;
    private int tokenExpected = 2;

    private boolean isProcessing = false; // **bloody hack**
    
    public LogLoader() 
	throws LogLoadException
    {
	int              Type;
	long              Time;
	int              Len;
	long 	       back;
	String           Line;
	File testFile;
	RandomAccessFile InFile;
	StringTokenizer  st;

	//Find the begin and end time across the parallel machine
	BeginTime = 0;
	EndTime   = Integer.MIN_VALUE;
	int nPe=Analysis.getNumProcessors();

	ProgressMonitor progressBar =
	    new ProgressMonitor(Analysis.guiRoot, "Determining end time",
				"", 0, nPe);

	validPEStringBuffer = new StringBuffer();
	validPERange = false;
	basePE = -1;
	upperPE = -1;
	for (int i=0; i<nPe; i++) {
	    if (!progressBar.isCanceled()) {
		progressBar.setNote(i + " of " + nPe);
		progressBar.setProgress(i);
	    } else {
		System.err.println("Fatal error - Projections cannot" +
				   " function without proper end time!");
		System.exit(-1);
	    }
	    try {
		// test the file to see if it exists ...
		testFile = new File(Analysis.getLogName(i));
		if (testFile.exists() == false) {
		    System.out.println(Analysis.getLogName(i) +
				       " does not exist, ignoring.");
		    updatePEStringBuffer();
		    validPERange = false;
		} else {
		    InFile = new RandomAccessFile (testFile, "r");

		    // success, so register processor as valid.
		    registerPE(i);
		    back = InFile.length()-80*3; //Seek to the end of the file
		    if (back < 0) back = 0;
		    InFile.seek(back);
		    while(InFile.readByte() != '\n');
		    //Throws EOFException at end of file
		    while (true) {
			Line = InFile.readLine();
			st   = new StringTokenizer(Line);
			if (Integer.parseInt(st.nextToken()) == 
			    END_COMPUTATION) {
			    Time = Long.parseLong(st.nextToken());
			    if (Time > EndTime)
				EndTime = Time;
			    break;
			}   
		    }
		    InFile.close ();
		}
	    } catch (IOException E) {
		System.out.println("Couldn't read log file " + 
				   Analysis.getLogName(i));
	    }
	}
	updatePEStringBuffer();
	validPEString = validPEStringBuffer.toString();
	
	progressBar.close();
	Analysis.setTotalTime(EndTime-BeginTime);
    }    
        
    public Vector createtimeline(int PeNum, long Begin, long End, 
				 Vector Timeline, Vector userEventVector)
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
	
	String logHeader;

	System.gc ();

	// open the file
	try {
	    log = 
		new AsciiIntegerReader(new BufferedReader(new FileReader(Analysis.getLogName(PeNum))));

	    /*
	    log.nextLine(); //First line contains junk
	    */

	    // **CW** first line is no longer junk.
	    // With the advent of the delta-encoding format, it should
	    // contain an additional field which specifies if the log file
	    // is a delta-encoded file.
	    logHeader = log.readLine();
	    StringTokenizer headerTokenizer = new StringTokenizer(logHeader);
	    // **CW** a hack to avoid parsing the string - simply count
	    // the number of tokens.
	    if (Analysis.getVersion() >= 6.0)  
		tokenExpected = 3;
	    if (headerTokenizer.countTokens() >= tokenExpected) {
		deltaEncoded = true;
                System.out.println("DELTA format found.");
	    } else {
		deltaEncoded = false;
	    }

	    // **CW** each time we open the file, we need to reset the
	    // previous event timestamp to 0 to support delta encoding.
	    prevTime = 0;

	    isProcessing = false; // *hack* - use global
	    while (true) { //Seek to time Begin
		LE = readlogentry(log);
		if (LE.Entry == -1) {
		    continue;
		}
		// This is still not ideal. There are cases which may cause
		// a rogue begin event to have data dropped at the beginning.
		if ((LE.TransactionType == BEGIN_PROCESSING) && 
		    (LE.Entry != -1)) {
		    isProcessing = true;
		    Time       = LE.Time - BeginTime;
		    Entry      = LE.Entry;
		} else if ((LE.TransactionType == END_PROCESSING) &&
			   (LE.Entry != -1)) {
		    isProcessing = false;
		    Time       = LE.Time - BeginTime;
		    Entry      = LE.Entry;
		} else if (LE.TransactionType == BEGIN_IDLE) {
		    Time = LE.Time - BeginTime;
		}
		if (LE.Time >= Begin) {
		    break;
		}
	    }
	    if (Time == Long.MIN_VALUE) {
		Time = Begin;
	    }
	    if (LE.Time > End) {
		switch (LE.TransactionType) {
		case BEGIN_PROCESSING:
		    System.out.println("finished empty timeline for " + PeNum);
		    log.close();
		    return Timeline;                              
		case END_PROCESSING:
		default:
		    //	  System.out.println ("overlaid, end");
		    Timeline.addElement(TE=new TimelineEvent(Begin-BeginTime,
							     End-BeginTime,
							     LE.Entry,LE.Pe));
		    log.close();
		    return Timeline;
		}
	    }
	    //Throws EOFException at end of file; break if past endTime
	    while(true) {
		if (LE.Entry != -1) {
		    switch (LE.TransactionType) {
		    case BEGIN_PROCESSING:
			if (isProcessing) {
			    // bad. We add a "pretend" end event to accomodate
			    // the prior begin processing event.
			    if (TE != null) {
				TE.EndTime = LE.Time - BeginTime;
			    }
			    TE = null;
			}
			isProcessing = true;
			TE = new TimelineEvent(LE.Time-BeginTime, 
					       LE.Time-BeginTime,
					       LE.Entry, LE.Pe,
					       LE.MsgLen, LE.recvTime, 
					       LE.id,LE.EventID,
					       LE.cpuBegin, LE.cpuEnd);
			Timeline.addElement(TE);
			break;
		    case END_PROCESSING:
			// this code automatically drops end events that
			// duplicated, which is the intended behavior.
			if (TE != null) {
			    TE.EndTime = LE.Time - BeginTime;
			    TE.cpuEnd = LE.cpuEnd;
			}
			TE = null;
			break;
		    case CREATION:
			tempte = false;
			//Start a new dummy event
			if (TE == null) { 
			    TE = new TimelineEvent(LE.Time-BeginTime,
						   LE.Time-BeginTime,
						   Entry,LE.Pe,LE.MsgLen);
			    Timeline.addElement(TE);
			    tempte = true;
			}
			TM = new TimelineMessage(LE.Time - BeginTime,
						 LE.Entry, LE.MsgLen,
						 LE.EventID);
			TE.addMessage(TM);
			if (tempte) {
			    TE = null;
			}
			break;
		    case CREATION_MULTICAST:
			tempte = false;
			if (TE == null) {
			    TE = new TimelineEvent(LE.Time-BeginTime,
						   LE.Time-BeginTime,
						   Entry, LE.Pe, LE.MsgLen);
			    Timeline.addElement(TE);
			    tempte = true;
			}
			TM = new TimelineMessage(LE.Time - BeginTime,
						 LE.Entry, LE.MsgLen,
						 LE.EventID, LE.destPEs);
			TE.addMessage(TM);
			if (tempte) {
			    TE = null;
			}
			break;
		    case USER_EVENT:
			// don't mess with TE, that's just for EPs
			UserEvent event = new UserEvent(LE.Time-BeginTime,
							LE.MsgType, LE.EventID,
							UserEvent.SINGLE);
			userEventVector.addElement(event);
			break;
		    case USER_EVENT_PAIR:
			Integer key = new Integer(LE.EventID);
			userEvent = (UserEvent)userEvents.get(key);
			if (userEvent != null) {
			    // the next is a bit confusing
			    // basically there is a CharmEventID and 
			    // an UserEventID (the id of the userEvent)
			    // but the log entry calls the CharmEventID 
			    // just EventID and the UserEventID
			    if (userEvent.CharmEventID != LE.EventID || 
				userEvent.UserEventID != LE.MsgType) {
				System.out.println("WARN: LogLoader.createtimeline() USER_EVENT_PAIR does not match same EventID");
			    }
			    userEvent.EndTime = LE.Time-BeginTime;
			    userEvents.remove(key);
			    userEventVector.addElement(userEvent);
			} else { 
			    userEvent = 
				new UserEvent(LE.Time-BeginTime,
					      LE.MsgType,LE.EventID,
					      UserEvent.PAIR); 
			    userEvents.put(key, userEvent);
			}
			break;
		    case BEGIN_PACK:
			// Start a new dummy event
			if (TE == null) {
			    TE = new TimelineEvent(LE.Time-BeginTime,
						   LE.Time-BeginTime,-1,
						   LE.Pe);
			    Timeline.addElement(TE);
			}
			TE.addPack (PT=new PackTime(LE.Time-BeginTime));
			break;
		    case END_PACK:
			if (PT!=null) {
			    PT.EndTime = LE.Time-BeginTime;
			}
			PT=null;
			if (TE.EntryPoint == -1) {
			    TE=null;
			}
			break;
		    case BEGIN_IDLE:
			if (MainWindow.IGNORE_IDLE) {
			    break;
			}
			TE = new TimelineEvent(LE.Time - BeginTime,
					       Long.MAX_VALUE,
					       -1,-1); 
			Timeline.addElement(TE);
			break;
		    case END_IDLE:
			if (MainWindow.IGNORE_IDLE) {
			    break;
			}
			if (TE != null) {   
			    TE.EndTime = LE.Time - BeginTime;
			}
			TE=null;
			break;
		    }
		}
		LE = readlogentry(log);
		// this will still eventually end because of the 
		// END COMPUTATION event.
		if (LE.Entry != -1) {
		    if ((LE.Time - BeginTime) > End) {
			break;
		    }
		}
	    }

	    // check to see if we are stopping in the middle of a message.
	    // if so, we need to keep reading to get its end time
	    while (TE != null) {
		if (LE.Entry != -1) {
		    if (LE.TransactionType == END_PROCESSING) {
			TE.EndTime = LE.Time - BeginTime;
			TE=null;
		    }
		}
		LE = readlogentry (log);
	    }
	    log.close ();
	} catch (EOFException e) { 
	    /*ignore*/ 
	} catch (FileNotFoundException E) {
	    System.out.println("ERROR: couldn't open file " + 
			       Analysis.getLogName(PeNum));
	} catch (IOException E) {
	    throw new LogLoadException(Analysis.getLogName(PeNum), 
				       LogLoadException.READ);
	}
	return Timeline;
    }

    private ViewerEvent entrytotext(LogEntry LE)
    {
	ViewerEvent VE = new ViewerEvent();
	VE.Time        = LE.Time - BeginTime;
	VE.EventType   = LE.TransactionType;

	if (LE.Entry == -1) {
	    return null;
	}

	switch (LE.TransactionType) {
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
	    String e2desc[][] = Analysis.getEntryNames();
	    VE.Dest = new String(e2desc[LE.Entry][1] + 
				 "::" + e2desc[LE.Entry][0]);     
	    if (LE.TransactionType != CREATION) {
		VE.SrcPe = LE.Pe;
	    }
	    return VE;
	case USER_EVENT:
	case USER_EVENT_PAIR:
	case DEQUEUE:
	case BEGIN_TRACE:
	case END_TRACE:
	case MESSAGE_RECV:
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
    LogEntry readlogentry(AsciiIntegerReader log) 
	throws IOException
    {   
	// **CW** prevTime (object variable) holds the previous time-stamp. 

	LogEntry Temp = new LogEntry();   
	  
	Temp.TransactionType = log.nextInt();
   
	switch (Temp.TransactionType) {
	case USER_EVENT:
	    Temp.MsgType = log.nextInt();
	    if (deltaEncoded) {
		prevTime += log.nextLong();
		Temp.Time    = prevTime;
	    } else {
		Temp.Time    = log.nextLong();
	    }
	    Temp.EventID = log.nextInt();
	    Temp.Pe      = log.nextInt();
	    log.nextLine();  // ignore rest of this line
	    break;
	case USER_EVENT_PAIR:
	    Temp.MsgType = log.nextInt();
	    if (deltaEncoded) {
		prevTime += log.nextLong();
		Temp.Time    = prevTime;
	    } else {
		Temp.Time    = log.nextLong();
	    }
	    Temp.EventID = log.nextInt();
	    Temp.Pe      = log.nextInt();
	    log.nextLine();  // ignore rest of this line
	    break;
	case BEGIN_IDLE:
	case END_IDLE:
	case BEGIN_PACK:
	case END_PACK:
	case BEGIN_UNPACK:
	case END_UNPACK:
	    if (deltaEncoded) {
		prevTime += log.nextLong();
		Temp.Time    = prevTime;
	    } else {
		Temp.Time    = log.nextLong();
	    }
	    Temp.Pe      = log.nextInt();
	    log.nextLine();  // ignore rest of this line
	    break;
	case BEGIN_PROCESSING:
	    /* We no longer ignore lines at the reading level. 
	     * The calling method will have
	     * to process this and act accordingly.
	    if (isProcessing) {
		// bad, ignore and clear rest of line.
		log.nextLine();  // ignore rest of this line
		break;
	    }
	    */
	    Temp.MsgType = log.nextInt();
	    Temp.Entry   = log.nextInt();
	    if (deltaEncoded) {
		prevTime += log.nextLong();
		Temp.Time    = prevTime;
	    } else {
		Temp.Time    = log.nextLong();
	    }
	    Temp.EventID = log.nextInt();
	    Temp.Pe      = log.nextInt();
	    if (Analysis.getVersion() > 1.0) {
		Temp.MsgLen  = log.nextInt();
	    } else {
		Temp.MsgLen  = -1;
	    }
	    if (Analysis.getVersion() >= 4.0) {
		Temp.recvTime  = log.nextLong();
		Temp.id = new ObjectId(log.nextInt(), log.nextInt(), 
				       log.nextInt());;
	    }
	    if (Analysis.getVersion() >= 6.5) {
		Temp.cpuBegin = log.nextLong();
	    }
	    isProcessing = true;
	    log.nextLine();  // ignore rest of this line
	    break;
	case END_PROCESSING:
	    /* We no longer ignore lines at the reading level.
	     * The calling method will have to process this and act
	     * accordingly.
	    if (!isProcessing) {
		// **CW** This is still a hack. A sequence number check is
		// actually the correct thing to do.
		// bad, ignore and clear rest of line.
		log.nextLine();  // ignore rest of this line
		break;
	    }
	    */
	    Temp.MsgType = log.nextInt();
	    Temp.Entry   = log.nextInt();
	    if (deltaEncoded) {
		prevTime += log.nextLong();
		Temp.Time    = prevTime;
	    } else {
		Temp.Time    = log.nextLong();
	    }
	    Temp.EventID = log.nextInt();
	    Temp.Pe      = log.nextInt();
	    if (Analysis.getVersion() > 1.0) {
		Temp.MsgLen  = log.nextInt();
	    } else {
		Temp.MsgLen  = -1;
	    }
	    if (Analysis.getVersion() >= 5.0 && 
		Temp.TransactionType == CREATION) {
		Temp.sendTime = log.nextLong();
	    }
	    if (Analysis.getVersion() >= 6.5) {
		Temp.cpuEnd = log.nextLong();
	    }
	    isProcessing = false;
	    log.nextLine();  // ignore rest of this line
	    break;
	case BEGIN_TRACE:
	    // ignore for now
	    log.nextLine();
	    break;
	case END_TRACE:
	    // ignore for now
	    log.nextLine();
	    break;
	case MESSAGE_RECV:
	    // ignore for now
	    log.nextLine();
	    break;
	case CREATION:
	    Temp.MsgType = log.nextInt();
	    Temp.Entry   = log.nextInt();
	    if (deltaEncoded) {
		prevTime += log.nextLong();
		Temp.Time    = prevTime;
	    } else {
		Temp.Time    = log.nextLong();
	    }
	    Temp.EventID = log.nextInt();
	    Temp.Pe      = log.nextInt();
	    if (Analysis.getVersion() > 1.0) {
		Temp.MsgLen  = log.nextInt();
	    } else {
		Temp.MsgLen  = -1;
	    }
	    if (Analysis.getVersion() >= 5.0 && 
		Temp.TransactionType == CREATION) {
		Temp.sendTime = log.nextLong();
	    }
	    log.nextLine();  // ignore rest of this line
	    break;
	case CREATION_MULTICAST:
	    Temp.MsgType = log.nextInt();
	    Temp.Entry   = log.nextInt();
	    if (deltaEncoded) {
		prevTime += log.nextLong();
		Temp.Time    = prevTime;
	    } else {
		Temp.Time    = log.nextLong();
	    }
	    Temp.EventID = log.nextInt();
	    Temp.Pe      = log.nextInt();
	    Temp.MsgLen  = log.nextInt();
	    Temp.sendTime = log.nextLong();
	    Temp.destPEs = new int[log.nextInt()];
	    for (int i=0; i<Temp.destPEs.length; i++) {
		Temp.destPEs[i] = log.nextInt();
	    }
	    log.nextLine();  // ignore rest of this line
	    break;
	case ENQUEUE:
	case DEQUEUE:
	    Temp.MsgType = log.nextInt();
	    if (deltaEncoded) {
		prevTime += log.nextLong();
		Temp.Time    = prevTime;
	    } else {
		Temp.Time    = log.nextLong();
	    }
	    Temp.EventID = log.nextInt();
	    Temp.Pe      = log.nextInt();
	    log.nextLine();  // ignore rest of this line
	    break;
	case BEGIN_INTERRUPT:
	case END_INTERRUPT:
	    if (deltaEncoded) {
		prevTime += log.nextLong();
		Temp.Time    = prevTime;
	    } else {
		Temp.Time    = log.nextLong();
	    }
	    Temp.EventID = log.nextInt();
	    Temp.Pe      = log.nextInt();
	    log.nextLine();  // ignore rest of this line
	    break;
	case BEGIN_COMPUTATION:
	    // begin computation's timestamp is not delta encoded.
	    Temp.Time    = log.nextLong();
	    if (deltaEncoded) {
		prevTime += Temp.Time;
	    }
	    log.nextLine();  // ignore rest of this line
	    break;
	case END_COMPUTATION:
	    // end computation's timestamp is not delta encoded.
	    Temp.Time    = log.nextLong();
	    log.nextLine();  // ignore rest of this line
	    break;
	default:
	    System.out.println ("ERROR: weird event type " + 
				Temp.TransactionType);
	    log.nextLine();  // ignore rest of this line
	}
	return Temp;
    }   
    
    public long searchtimeline(int PeNum, int Entry, int Num)
	throws LogLoadException, EntryNotFoundException
    {
	long           Count = 0;
	LogEntry       LE     = null;
	AsciiIntegerReader log = null;

	String logHeader;

	System.out.println("looking through log for processor " + PeNum);
	
	// open the file
	try {
	    System.gc();
	    log = new AsciiIntegerReader(new BufferedReader(new FileReader(Analysis.getLogName(PeNum))));

	    /*
	    log.nextLine(); //First line is junk
	    */

	    // **CW** first line is no longer junk.
	    // With the advent of the delta-encoding format, it should
	    // contain an additional field which specifies if the log file
	    // is a delta-encoded file.
	    logHeader = log.readLine();
	    StringTokenizer headerTokenizer = new StringTokenizer(logHeader);
	    // **CW** a hack to avoid parsing the string - simply count
	    // the number of tokens.
	    if (Analysis.getVersion() >= 6.0) tokenExpected = 3;
	    if (headerTokenizer.countTokens() > tokenExpected) {
		deltaEncoded = true;
	    } else {
		deltaEncoded = false;
	    }

	    // **CW** each time we open the file, we need to reset the
	    // previous event timestamp to 0 to support delta encoding.
	    prevTime = 0;

	    //Throws EOFException at end of file
	    while(true) {
		LE = readlogentry (log);
		if (LE.Entry == -1) {
		    continue;
		}
		if ((LE.Entry == Entry) && 
		    (LE.TransactionType == BEGIN_PROCESSING)) {
		    Count++;
		}
		if (Count > Num) {
		    break;
		}
	    }
	} catch (FileNotFoundException E) {
	    System.out.println("ERROR: couldn't open file " + 
			       Analysis.getLogName(PeNum));
	} catch (EOFException E) {
	    /*ignore*/
	} catch (IOException E) {
	    throw new LogLoadException(Analysis.getLogName(PeNum), 
				       LogLoadException.READ);
	}  
	return LE.Time - BeginTime;
    }   

    public Vector view(int PeNum) 
	throws LogLoadException
    {
	AsciiIntegerReader log = null;
	ViewerEvent    VE;
	Vector ret = null;
	String         Line;

	String logHeader;

	try {	  
	    ret = new Vector ();
	    log = new AsciiIntegerReader(new BufferedReader(new FileReader(Analysis.getLogName(PeNum))));

	    /*
	    log.nextLine();//First line is junk
	    */

	    // **CW** first line is no longer junk.
	    // With the advent of the delta-encoding format, it should
	    // contain an additional field which specifies if the log file
	    // is a delta-encoded file.
	    logHeader = log.readLine();
	    StringTokenizer headerTokenizer = new StringTokenizer(logHeader);
	    // **CW** a hack to avoid parsing the string - simply count
	    // the number of tokens.
	    if (Analysis.getVersion() >= 6.0)  tokenExpected = 3;
	    if (headerTokenizer.countTokens() > tokenExpected) {
		deltaEncoded = true;
	    } else {
		deltaEncoded = false;
	    }

	    // **CW** each time we open the file, we need to reset the
	    // previous event timestamp to 0 to support delta encoding.
	    prevTime = 0;

	    //Throws EOFException at end of file
	    while (true) {
		VE = entrytotext(readlogentry(log));
		if (VE != null) {
		    ret.addElement (VE);
		}
	    }
	} catch (FileNotFoundException E) {
	    System.out.println("ERROR: couldn't open file " + 
			       Analysis.getLogName(PeNum));
	} catch (EOFException E) {
	} catch (IOException E) {
	    System.out.println("throwing....2");
	    throw new LogLoadException(Analysis.getLogName(PeNum), 
				       LogLoadException.READ);
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
