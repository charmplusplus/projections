package projections.analysis;

import projections.misc.*;

import java.io.*;
import java.util.*;

/** 
 *  Adapted by Chee Wai Lee
 *  (the StsReader class from the projections.analysis package)
 *  3/27/2002
 *
 *  MRStsReader reads the .sts or .sum.sts files and parses out the
 *  entry point names and numbers, message names, etc.
 *
 *  The read data are henceforth publically available.
 *  
 */
public class GenericStsReader
{
    private double version;
    private BufferedReader reader;

    // Sts data
    public int numPe;
    public String machineName;

    // various counts
    public int entryCount;
    public int totalChares;
    public int totalMsgs;

    // data items
    public ChareData chareList[];
    public EntryTypeData entryList[];
    public long msgSizeList[];

    public GenericStsReader(String filename, double Nversion) 
	throws IOException
    {
	try {
	    reader = new BufferedReader(new FileReader(filename));
	    version = Nversion;
	    read();
	    reader.close();
	    reader = null;
	} catch (IOException e) {
	    throw new IOException("Error reading file " + filename);
	}
    }

    public void read()
	throws IOException
    {
	String line;

	while ((line = reader.readLine()) != null) {
	    StringTokenizer st = new StringTokenizer(line);
	    String s1 = st.nextToken();
	    if (s1.equals("MACHINE")) {
		machineName = st.nextToken();
	    } else if (s1.equals("PROCESSORS")) {
		numPe = Integer.parseInt(st.nextToken());
	    } else if (s1.equals("TOTAL_CHARES")) {
		totalChares = Integer.parseInt(st.nextToken());
		chareList = new ChareData[totalChares];
	    } else if (s1.equals("TOTAL_EPS")) {
		entryCount = Integer.parseInt(st.nextToken());
		entryList = new EntryTypeData[entryCount];
	    } else if (s1.equals("TOTAL_MSGS")) {
		totalMsgs = Integer.parseInt(st.nextToken());
		msgSizeList = new long[totalMsgs];
	    } else if (s1.equals("CHARE") || s1.equals("BOC")) {
		int ID = Integer.parseInt(st.nextToken());
		chareList[ID] = new ChareData();
		chareList[ID].chareID = ID;
		chareList[ID].numEntries = 0;
		chareList[ID].name = st.nextToken();
		chareList[ID].type = new String(s1);
	    } else if (s1.equals("ENTRY")) {
		String type = st.nextToken();  // when is this used?
		int ID = Integer.parseInt(st.nextToken());
		entryList[ID] = new EntryTypeData();
		StringBuffer nameBuf=new StringBuffer(st.nextToken());
		String name = nameBuf.toString();
		// if found open-paren but not close-paren
		// **CW** What if there are nested paren or other
		//        special token characters? This implementation
		//        seems a little too simplistic.
		if (-1!=name.indexOf('(') && -1==name.indexOf(')')) {
		    //Parse strings until we find the close-paren
		    while (true) {
			String tmp=st.nextToken();
			nameBuf.append(" ");
			nameBuf.append(tmp);
			if (tmp.endsWith(")"))
			    break;
		    }
		}
		entryList[ID].name = nameBuf.toString();
		entryList[ID].chareID = Integer.parseInt(st.nextToken());
		entryList[ID].msgID   = Integer.parseInt(st.nextToken());
	    } else if (s1.equals("MESSAGE")) {
		int ID  = Integer.parseInt(st.nextToken());
		msgSizeList[ID] = Long.parseLong(st.nextToken());
	    } else if (s1.equals ("END")) {
		break;
	    }
	}
    }
}
