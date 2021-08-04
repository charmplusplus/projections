package projections.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.List;

import projections.gui.U;

public class RangeHistory
{
    public static final int MAX_ENTRIES = 10;

    private String filename;
    private int numEntries;
    private List<Long> rangeSet;
    private List<String> rangeName;
    private List<String> rangeProcs;

    private List<String> historyStringVector;

    public RangeHistory(String logDirectory) 
    {
	this.filename = logDirectory + "ranges.hst";
	if (!(new File(this.filename)).exists()) {
	    rangeSet = new ArrayList<Long>();
	    rangeName = new ArrayList<String>();
	    rangeProcs = new ArrayList<String>();
	    historyStringVector = new ArrayList<String>();
	} else {
	    try {
		loadRanges();
		historyStringVector = new ArrayList<String>();
		for (int i=0; i<rangeSet.size()/2; i++) {
			String historyString =
			U.humanReadableString(rangeSet.get(i*2)) +
			" to " + 
			U.humanReadableString(rangeSet.get(i*2+1));
			if (rangeProcs != null && rangeProcs.size() !=0 && rangeProcs.size() > i)
			{
				String temp = rangeProcs.get(i);
				if (temp.length() > 10) historyString += " Proc(s):" + temp.substring(0,10)+"...";
				else historyString += " Proc(s):" + temp;
			}
			if (rangeName != null && rangeName.size() !=0 && !rangeName.get(i).equals(null) && !rangeName.get(i).equals("cancel"))
			{
				String temp = rangeName.get(i);
				if (temp.length() > 10) historyString += " (" + temp.substring(0,10)+"...)";
				else historyString += " (" + rangeName.get(i) + ")";
			}
			historyStringVector.add(historyString);
		}
	    } catch (IOException e) {
		System.err.println("Error: " + e);
	    }
	}
    }

    private void loadRanges() 
	throws IOException
    {
	rangeSet = new ArrayList<Long>();
	rangeName = new ArrayList<String>();
	rangeProcs = new ArrayList<String>();
	String line;
	StringTokenizer st;
	BufferedReader reader = 
	    new BufferedReader(new FileReader(filename));

	numEntries = 0;
	
	while ((line = reader.readLine()) != null) {
	    st = new StringTokenizer(line);
	    String s1 = st.nextToken();
	    if (s1.equals("ENTRY"))
		{
			if (numEntries >= MAX_ENTRIES)
			{
				throw new IOException("Range history overflow!");
			}
		rangeSet.add(Long.valueOf(st.nextToken()));
		rangeSet.add(Long.valueOf(st.nextToken()));
		numEntries++;
	    }
		else if (s1.equals("NAMEENTRY"))
		{
			if (st.hasMoreTokens()) rangeName.add(st.nextToken());
			// creates a fake name, cancel, to preserve alignment and prevent issues
			else rangeName.add("cancel");
		}
		else if (s1.equals("PROCENTRY"))
		{
			String procs = "";
			while (true)
			{
				if (st.hasMoreTokens()) procs+= st.nextToken();
				else break;
			}
			rangeProcs.add(procs);
		}
	}
	reader.close();
    }

    public void save() 
	throws IOException
    {
	PrintWriter writer =
	    new PrintWriter(new FileWriter(filename), true);

	// Data and File is stored in proper order (latest on top)
		for (int i=0; i<numEntries; i++)
		{
			writer.print("ENTRY ");
			writer.print(rangeSet.get(i*2));
			writer.print(" ");
			writer.println(rangeSet.get(i*2+1));
			writer.print("NAMEENTRY ");
			if (rangeName.size() > i) writer.println(rangeName.get(i));
			else writer.println("cancel");
			writer.print("PROCENTRY ");
			if (rangeProcs.size() > i) writer.println(rangeProcs.get(i));
		}
	}

    // adds a new entry and displaces any old entry beyond MAX_ENTRIES
    public void add(long start, long end, String name, String procs) {
	if (numEntries == MAX_ENTRIES) {
	    // history is full, take off the rear entry.
	    rangeSet.remove((MAX_ENTRIES*2)-1);
	    rangeSet.remove((MAX_ENTRIES*2)-2);
	    if (rangeName.size() == MAX_ENTRIES) rangeName.remove(MAX_ENTRIES-1);
	    if (rangeProcs.size() == MAX_ENTRIES) rangeProcs.remove(MAX_ENTRIES-1);
	    numEntries--;
	}
	// added in reverse order starting from the front of the vector.
	if (rangeName == null) rangeName = new ArrayList<String>();
	rangeName.add(0, name);
	rangeSet.add(0, end);
	rangeSet.add(0, start);
	rangeProcs.add(0, procs);
	numEntries++;
    }

    public void update(int index, long start, long end, String name, String procs) {
		if ((index < 0) ||
				(index >= numEntries)) {
			System.err.println("Internal Error: Attempt to update " +
					"invalid index " +
					index + ". Max number of " +
					"histories is " + numEntries +
					". Please report to developers!");
			System.exit(-1);
		}

		// remove the "same" index twice because the first remove
		// has the side effect of changing the index.
		rangeSet.remove(index * 2);
		rangeSet.remove(index * 2);
		rangeSet.add(index * 2, end);
		rangeSet.add(index * 2, start);

		if(name != null && rangeName != null && rangeName.size() > index) {
			rangeName.remove(index);
			rangeName.add(index, name);
		}

		if(procs != null && rangeProcs != null && rangeProcs.size() > index) {
			rangeProcs.remove(index);
			rangeProcs.add(index, procs);
		}
	}

    public void remove(int index) {
	if ((index < 0) ||
	    (index >= numEntries)) {
	    System.err.println("Internal Error: Attempt to remove " +
			       "invalid index " + 
			       index + ". Max number of " +
			       "histories is " + numEntries +
			       ". Please report to developers!");
	    System.exit(-1);
	}
	// remove the "same" index twice because the first remove
	// has the side effect of changing the index.
	rangeSet.remove(index*2);
	rangeSet.remove(index*2);
	if (rangeName != null && rangeName.size() != 0)
	{
		rangeName.remove(index);
	}
	if (rangeProcs != null && rangeProcs.size() > index)
	{
		rangeProcs.remove(index);
	}
	 numEntries--;
    }

    // NOTE: history string is only used at the start of initializing
    // the history list in the GUI. Any further updates to this string
    // is quite meaningless.
    public List<String> getHistoryStrings() {
	return historyStringVector;
    }

    public String getProcRange(int index) {
	if (rangeProcs == null || rangeProcs.size() <= index || index < 0) return null;
	return rangeProcs.get(index);
    }

    public String getName(int index) {
    	if(rangeName == null || rangeName.size() <= index || index < 0)
    		return null;
    	return rangeName.get(index);
	}

    public long getStartValue(int index) {
	if ((index < 0) ||
	    (index >= numEntries)) {
	    System.err.println("Internal Error: Requested history index " + 
			       index + " is invalid. Max number of " +
			       "histories is " + numEntries +
			       ". Please report to developers!");
	    System.exit(-1);
	}
	return rangeSet.get(index*2);
    }

    public long getEndValue(int index) {
	if ((index < 0) ||
	    (index >= numEntries)) {
	    System.err.println("Internal Error: Requested history index " + 
			       index + " is invalid. Max number of " +
			       "histories is " + numEntries +
			       ". Please report to developers!");
	    System.exit(-1);
	}
	return rangeSet.get(index*2+1);
    }
}
