package projections.analysis;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;

import javax.swing.ProgressMonitor;

import projections.gui.MainWindow;


/** This thread's run() method will lookup the endtime for an input log file */
public class LogLoaderEndTimeThread  extends Thread {

	public String logName;
	public Integer result;

	public LogLoaderEndTimeThread(String  _logName) {
		result = new Integer(0);
		logName = _logName;
	}

	public void run() {

		String           Line;
		RandomAccessFile InFile;
		StringTokenizer  st;

		// Find the begin and end time for the given logfile

		int dummyInt = 0;
		int type = 0;
		try {
			InFile =  new RandomAccessFile(logName, "r");

			long back = InFile.length()-80*3; //Seek to the end of the file
			if (back < 0) back = 0;
			InFile.seek(back);
			while (InFile.readByte() != '\n'){}
			while (true) {
				Line = InFile.readLine();
				// incomplete files can end on a proper previous
				// line but not on END_COMPUTATION. This statement
				// takes care of that.
				if (Line == null) {
					throw new EOFException();
				}
				st = new StringTokenizer(Line);
				if ((type=Integer.parseInt(st.nextToken())) == ProjDefs.END_COMPUTATION) {
					long time = Long.parseLong(st.nextToken());
					if (time > result)
						result = (int) time;
					break; // while loop
				} else {
					switch (type) {
					case ProjDefs.CREATION:
					case ProjDefs.CREATION_BCAST:
					case ProjDefs.CREATION_MULTICAST:
					case ProjDefs.BEGIN_PROCESSING:
					case ProjDefs.END_PROCESSING:
						dummyInt = Integer.parseInt(st.nextToken());
						dummyInt = Integer.parseInt(st.nextToken());
						break;
					case ProjDefs.USER_SUPPLIED:
					case ProjDefs.MEMORY_USAGE:
					case ProjDefs.USER_EVENT:
					case ProjDefs.USER_EVENT_PAIR:
					case ProjDefs.MESSAGE_RECV: 
					case ProjDefs.ENQUEUE: 
					case ProjDefs.DEQUEUE:
						dummyInt = 	Integer.parseInt(st.nextToken());
						break;
					case ProjDefs.BEGIN_IDLE:
					case ProjDefs.END_IDLE:
					case ProjDefs.BEGIN_PACK: 
					case ProjDefs.END_PACK:
					case ProjDefs.BEGIN_UNPACK: 
					case ProjDefs.END_UNPACK:
					case ProjDefs.BEGIN_TRACE: 
					case ProjDefs.END_TRACE:
					case ProjDefs.BEGIN_COMPUTATION:
					case ProjDefs.BEGIN_FUNC:
					case ProjDefs.END_FUNC:
					case ProjDefs.BEGIN_INTERRUPT: 
					case ProjDefs.END_INTERRUPT:
						break;

					}
				}
			}
			InFile.close ();
		} catch (IOException E) {
			System.err.println("Couldn't read log file " + logName);
		}
		
	}

}
