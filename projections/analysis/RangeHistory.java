package projections.analysis;

import java.io.*;
import java.util.*;

public class RangeHistory
{
    public static final int MAX_ENTRIES = 10;

    private boolean noSource = false;
    private String filename;
    private int numEntries;
    private Vector rangeSet;

    public RangeHistory(String logDirectory) 
    {
	this.filename = logDirectory + "ranges.hst";
	if (!(new File(this.filename)).exists()) {
	    noSource = true;
	}
    }

    public Vector loadRanges() 
	throws IOException
    {
	rangeSet = new Vector();
	if (noSource) {
	    return rangeSet;
	}
	String line;
	StringTokenizer st;
	BufferedReader reader = 
	    new BufferedReader(new FileReader(filename));

	numEntries = 0;
	
	while ((line = reader.readLine()) != null) {
	    st = new StringTokenizer(line);
	    String s1 = st.nextToken();
	    if (s1.equals("ENTRY")) {
		if (numEntries >= MAX_ENTRIES) {
		    throw new IOException("Range history overflow!");
		}
		rangeSet.add(Long.valueOf(st.nextToken()));
		rangeSet.add(Long.valueOf(st.nextToken()));
		numEntries++;
	    }
	}
	reader.close();
	return rangeSet;
    }

    public void save() 
	throws IOException
    {
	PrintWriter writer =
	    new PrintWriter(new FileWriter(filename), true);

	// Data and File is stored in proper order (latest on top)
	for (int i=0; i<numEntries; i++) {
	    writer.print("ENTRY ");
	    writer.print(((Long)rangeSet.elementAt(i*2)).longValue());
	    writer.print(" ");
	    writer.println(((Long)rangeSet.elementAt(i*2+1)).longValue());
	}
    }

    // adds a new entry and displaces any old entry beyond MAX_ENTRIES
    public void add(long start, long end) {
	if (numEntries == MAX_ENTRIES) {
	    // history is full, take off the rear entry.
	    rangeSet.remove(MAX_ENTRIES);
	    rangeSet.remove(MAX_ENTRIES-1);
	    numEntries--;
	}
	// added in reverse order starting from the front of the vector.
	rangeSet.add(0, new Long(end));
	rangeSet.add(0, new Long(start));
	numEntries++;
    }
}
