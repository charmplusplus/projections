package projections.analysis;

import projections.misc.*;

import java.lang.*;
import java.io.*;
import java.util.*;

// this version of the summary reader only has 1 line containing utilization 
// data

public class BGSummaryReader 
{
    // private static meta-tags - used to allocate space in the data array
    // based on the number of tags.
    private static final int NUM_TAGS = 2;

    // public static tags - used to access the appropriate part of the
    // array.
    public static final int TOTAL_TIME = 0;
    public static final int NUM_MSGS = 1;

    // header values
    public int versionNum;
    public int myPE;
    public int numPE;
    public int numIntervals;
    public int numTracedPEs;
    public int numEPs;  // bizzare ... already found in .sts file
    public double intervalSize;
    public int numPhases;

    // Data values

    // processor utilization data
    public int processorUtil[];

    // private miscellaneous data
    private double version;
    private BufferedReader reader;
    private ParseTokenizer tokenizer;
    private int tokenType;

    public BGSummaryReader(String filename, double Nversion) 
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

    // Methods to parse the summary file

    public void read()
	throws IOException
    {
	//Set up the tokenizer
	tokenizer=new ParseTokenizer(reader);
	tokenizer.parseNumbers();
	tokenizer.eolIsSignificant(true);
	tokenizer.whitespaceChars('/','/'); 
	tokenizer.whitespaceChars(':',':');
	tokenizer.whitespaceChars('[','[');
	tokenizer.whitespaceChars(']',']');
	tokenizer.wordChars('a','z');
	tokenizer.wordChars('A','Z');

	//Read the first line (Header information)
	tokenizer.checkNextString("ver");
	versionNum = (int)tokenizer.nextNumber("Version Number");
	myPE = (int)tokenizer.nextNumber("processor number");
	numPE = (int)tokenizer.nextNumber("number of processors");
	tokenizer.checkNextString("count");
	numIntervals = (int)tokenizer.nextNumber("count");
	tokenizer.checkNextString("ep");
	numEPs = (int)tokenizer.nextNumber("number of entry methods");
	tokenizer.checkNextString("interval");
	intervalSize = 
	    tokenizer.nextScientific("processor usage sample interval"); 
	tokenizer.checkNextString("numTracedPE");
	numTracedPEs = (int)tokenizer.nextNumber("number of traced PEs");

	// Make sure we're at the end of the line
	if (StreamTokenizer.TT_EOL!=tokenizer.nextToken()) {
	    throw new IOException("extra garbage at end of line 1");
	}

	// prepare to store summary data into arrays
	processorUtil = new int[numIntervals];

	// Read the SECOND line (processor usage)
	int nUsageRead=0;
	while (StreamTokenizer.TT_NUMBER==(tokenType=tokenizer.nextToken())) {
	    processorUtil[nUsageRead++] = (int)tokenizer.nval;
	}
        if (numIntervals != nUsageRead) 
            throw new IOException("numIntervals not agree!");
	tokenizer = null;
    }
}

