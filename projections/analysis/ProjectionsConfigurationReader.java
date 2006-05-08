package projections.analysis;

import java.io.*;
import java.util.*;
import java.lang.*;

import projections.misc.*;

/** 
 *  ProjectionsConfigurationReader.java
 *  by Chee Wai Lee.
 *  10/24/2005
 *
 *  ProjectionsConfigurationReader reads a simple line-based data file
 *  which describes visualization configurations (like total run time).
 *  **CW** This is still kind of a hack which needs cleaning up.
 *
 */
public class ProjectionsConfigurationReader
{
    private String baseName;
    private String logDirectory;
    private String configurationName;

    // Variables recognized and stored in a hash table when a 
    // configuration file is read.
    private Hashtable configData;
    
    // Private status variables
    private boolean dirty;

    public ProjectionsConfigurationReader(String baseName,
					  String logDirectory) 
    {
	configData = new Hashtable();
	this.baseName = baseName;
	this.logDirectory = logDirectory;
	configurationName = baseName + ".projrc";
	dirty = false;
	try {
	    read();
	} catch (LogLoadException e) {
	    System.err.println(e.toString());
	    System.exit(-1);
	}
    }

    public void read() 
	throws LogLoadException
    {
	try {
	    BufferedReader InFile = 
		new BufferedReader(new InputStreamReader(new FileInputStream(configurationName)));
	    String Line,Type,Name;
	    while ((Line = InFile.readLine()) != null) {
		StringTokenizer st = new StringTokenizer(Line);
		String s1 = st.nextToken();
		// look up against known variables, read their corresponding
		// data type and return an Object "value" for each.
		Object value = readValue(s1,st);
		if (value != null) {
		    configData.put(s1,value);
		} else {
		    System.err.println("Internal Error: Unknown projections" +
				       " configuration key [" +
				       s1 + "]! Please inform developers.");
		    System.exit(-1);
		}
	    }
	    InFile.close();
	} catch (FileNotFoundException e) {
	    // no previous rc file. Create new file.
	    try {
		File newRc = new File(configurationName);
		newRc.createNewFile();
	    } catch (IOException ioException) {
		System.err.println("WARNING: Unable to write to rc file [" +
				   configurationName + "]. Reason: ");
		System.err.println(ioException.toString());
	    }
	} catch (IOException e) {
	    throw new LogLoadException (configurationName, 
					LogLoadException.READ);
	}
    }

    public void add(String key, Object value)
    {
	configData.put(key, value);
	dirty = true;
    }

    // *********************
    // * Public interface to the configuration reader
    // * whichever tool of projections uses this is expected
    // * to know the data type returned and cast it accordingly.
    // * otherwise, there is an internal error and the system
    // * should be shut down.
    // *********************
    public Object getValue(String key) {
	return configData.get(key);
    }

    public void close() 
    {
	try {
	    flush();
	} catch (LogLoadException e) {
	    System.err.println(e.toString());
	    System.exit(-1);
	}
    }

    // *********************
    // * This method uses a different read method for each known
    // * key.
    // *********************
    private Object readValue(String key, StringTokenizer st) {
	if (key.equals("REAL_TOTAL_TIME")) {
	    return Long.valueOf(st.nextToken());
	} else if (key.equals("POSE_REAL_TOTAL_TIME")) {
	    return Double.valueOf(st.nextToken());
	} else if (key.equals("POSE_VIRT_TOTAL_TIME")) {
	    return Long.valueOf(st.nextToken());
	} else {
	    return null;
	}
    }

    private void writeValue(String key, Object value, PrintWriter writer) {
	if (key.equals("REAL_TOTAL_TIME")) {
	    writer.print(((Long)value).longValue());
	} else if (key.equals("POSE_REAL_TOTAL_TIME")) {
	    writer.print(((Double)value).doubleValue());
	} else if (key.equals("POSE_VIRT_TOTAL_TIME")) {
	    writer.print(((Long)value).longValue());
	} else {
	    System.err.println("Internal Error: Unknown key [" +
			       key + "]! Please report to developers!");
	    System.exit(-1);
	}
    }

    private void flush() 
	throws LogLoadException
    {
	if (dirty) {
	    try {
		PrintWriter writer =
		    new PrintWriter(new FileWriter(configurationName));
		Iterator dataIterator = 
		    configData.entrySet().iterator();
		Map.Entry entry;
		while (dataIterator.hasNext()) {
		    entry = (Map.Entry)dataIterator.next();
		    writer.print((String)(entry.getKey()) + " ");
		    writeValue((String)entry.getKey(), entry.getValue(),
			       writer);
		    writer.println();
		}
		writer.close();
	    } catch (FileNotFoundException e) {
		throw new LogLoadException (configurationName, 
					    LogLoadException.OPEN);
	    } catch (IOException e) {
		throw new LogLoadException (configurationName, 
					    LogLoadException.WRITE);
	    }
	} else {
	    return;
	}
    }
}

